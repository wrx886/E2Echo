<script setup lang="ts">
import { computed, inject, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  Download,
  Loading,
  Refresh,
  RefreshLeft,
  RefreshRight,
  ZoomIn,
  ZoomOut,
} from '@element-plus/icons-vue'
import type { ChatFileMessageVo } from '@/api'
import { chatFileUrl } from '@/api'
import { CHAT_PIN_TO_BOTTOM } from '@/utils/chatScroll'
import { safeFileName } from '@/utils/filename'

/**
 * 图片消息。
 *
 * <p>文件接口支持 GET + Range + inline，地址可以直接写进 {@code src}：这里在图片将要进入视野时才
 * 挂上地址（懒加载），加载完按最大边缩放显示，点击弹窗看原图。</p>
 */

const props = defineProps<{
  /** 图片消息正文（文件名、密钥、对象键）。 */
  file: ChatFileMessageVo
}>()

/**
 * 让聊天页把视图贴回底部的能力。
 *
 * <p>图片取回来会撑高气泡，如果不通知，聊天页可能停在「最后一次长高之前」的位置，看起来就是没有
 * 贴住最新消息。组件不在聊天页里时注入不到，这里按可选处理。</p>
 */
const pinToBottom = inject(CHAT_PIN_TO_BOTTOM, null)

/** 是否已经进入视野：进入后才把地址交给 img，实现懒加载。 */
const revealed = ref(false)

/** 是否已经加载完成。 */
const loaded = ref(false)

/** 加载失败（或取回来但浏览器解不出图片）时的提示。 */
const failed = ref('')

/** 图片地址：空串表示还没进入视野。 */
const src = computed(() => (revealed.value ? chatFileUrl(props.file) : ''))

/** 是否正在看大图。 */
const previewVisible = ref(false)

/** 大图当前缩放倍数。 */
const scale = ref(1)

/** 大图当前旋转角度（度，顺时针）。 */
const rotate = ref(0)

/** 大图拖动后的位移。 */
const offsetX = ref(0)
const offsetY = ref(0)

/** 缩放下限与上限。 */
const MIN_SCALE = 0.25
const MAX_SCALE = 5

/** 每次放大/缩小的倍率。 */
const SCALE_STEP = 1.25

/** 是否正在拖动大图。 */
let dragging = false
let dragStartX = 0
let dragStartY = 0
let dragStartOffsetX = 0
let dragStartOffsetY = 0

/** 大图当前的变换。 */
const transformStyle = ref('')

/**
 * 刷新大图变换：先平移，再缩放，最后旋转。
 */
function refreshTransform(): void {
  transformStyle.value = `translate(${offsetX.value}px, ${offsetY.value}px)`
    + ` scale(${scale.value}) rotate(${rotate.value}deg)`
}

/** 放大。 */
function zoomIn(): void {
  scale.value = Math.min(MAX_SCALE, scale.value * SCALE_STEP)
  refreshTransform()
}

/** 缩小。 */
function zoomOut(): void {
  scale.value = Math.max(MIN_SCALE, scale.value / SCALE_STEP)
  refreshTransform()
}

/** 左转 90 度。 */
function rotateLeft(): void {
  rotate.value -= 90
  refreshTransform()
}

/** 右转 90 度。 */
function rotateRight(): void {
  rotate.value += 90
  refreshTransform()
}

/** 复位：缩放、旋转、位移都回到初始状态。 */
function resetTransform(): void {
  scale.value = 1
  rotate.value = 0
  offsetX.value = 0
  offsetY.value = 0
  refreshTransform()
}

/**
 * 滚轮缩放。
 *
 * @param event 滚轮事件
 */
function onWheel(event: WheelEvent): void {
  if (event.deltaY < 0) {
    zoomIn()
  } else {
    zoomOut()
  }
}

/**
 * 开始拖动大图。
 *
 * @param event 指针事件
 */
function onPointerDown(event: PointerEvent): void {
  dragging = true
  dragStartX = event.clientX
  dragStartY = event.clientY
  dragStartOffsetX = offsetX.value
  dragStartOffsetY = offsetY.value
}

/**
 * 拖动大图：拉不动时不用管，复位可以一键回到原位。
 *
 * @param event 指针事件
 */
function onPointerMove(event: PointerEvent): void {
  if (!dragging) {
    return
  }
  offsetX.value = dragStartOffsetX + (event.clientX - dragStartX)
  offsetY.value = dragStartOffsetY + (event.clientY - dragStartY)
  refreshTransform()
}

/** 结束拖动。 */
function onPointerUp(): void {
  dragging = false
}

/** 懒加载是否已经触发过，只触发一次。 */
let started = false

/** 视野观察器。 */
let observer: IntersectionObserver | null = null

/** 组件根节点，用来观察是否进入视野。 */
const root = ref<HTMLDivElement>()

/**
 * 让图片开始加载：进入视野时调用一次。
 */
function reveal(): void {
  revealed.value = true
}

/**
 * 图片加载完成：标记完成，并让聊天页贴回底部（图片会撑高气泡）。
 */
function onImageLoad(): void {
  loaded.value = true
  pinToBottom?.()
}

/**
 * 打开大图：还没加载完就不打开，免得弹出一个空白查看器。
 */
function openPreview(): void {
  if (loaded.value) {
    previewVisible.value = true
  }
}

/**
 * 下载原图：直接用文件接口的地址触发一次浏览器下载，不再自己取字节再转 object URL。
 */
function onDownload(): void {
  const anchor = document.createElement('a')
  anchor.href = chatFileUrl(props.file)
  anchor.download = safeFileName(props.file.filename)
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
}

/**
 * 重试：把图片重新挂一次，触发一次新的请求。
 */
async function onRetry(): Promise<void> {
  failed.value = ''
  loaded.value = false
  revealed.value = false
  await nextTick()
  revealed.value = true
}

/**
 * 字节拿到了但浏览器解不出图片（例如发送方给的根本不是图片）。
 */
function onImageError(): void {
  failed.value = '图片无法显示'
}

onMounted(() => {
  const element = root.value
  // 拿不到观察器就直接加载，保证功能可用，只是失去懒加载
  if (!element || typeof IntersectionObserver === 'undefined') {
    started = true
    reveal()
    return
  }
  observer = new IntersectionObserver((entries) => {
    if (started || !entries.some((entry) => entry.isIntersecting)) {
      return
    }
    started = true
    observer?.disconnect()
    observer = null
    reveal()
  }, { rootMargin: '200px' })
  observer.observe(element)
})

onBeforeUnmount(() => {
  observer?.disconnect()
  observer = null
})

// 每次打开大图都从初始状态开始，免得上次的缩放旋转带到下一张图上
watch(previewVisible, (value) => {
  if (value) {
    resetTransform()
  }
})
</script>

<template>
  <div ref="root" class="image">
    <!-- 进入视野后才挂上地址；解码完成前由占位块撑住尺寸，点图片看大图 -->
    <div v-if="failed.length === 0" class="image__box" @click="openPreview">
      <img
        v-if="revealed"
        class="image__img"
        :class="{ 'image__img--loading': !loaded }"
        :src="src"
        :alt="file.filename"
        @load="onImageLoad"
        @error="onImageError"
      />
      <div v-if="!loaded" class="image__placeholder">
        <el-icon class="is-loading" :size="18"><Loading /></el-icon>
      </div>
    </div>

    <div v-else class="image__failed">
      <span>{{ failed }}</span>
      <el-button size="small" text :icon="RefreshLeft" @click="onRetry">重试</el-button>
    </div>

    <!-- append-to-body：弹窗挂到 body 上，不嵌在可滚动的消息列表里 -->
    <el-dialog
      v-model="previewVisible"
      :title="file.filename"
      width="80%"
      align-center
      append-to-body
    >
      <div
        class="viewer"
        @wheel.prevent="onWheel"
        @pointerdown="onPointerDown"
        @pointermove="onPointerMove"
        @pointerup="onPointerUp"
        @pointerleave="onPointerUp"
      >
        <img
          v-if="src"
          class="viewer__img"
          :src="src"
          :style="{ transform: transformStyle }"
          :alt="file.filename"
          draggable="false"
        />
      </div>

      <div class="viewer__bar">
        <el-button-group>
          <el-button :icon="Download" title="下载原图" @click="onDownload" />
          <el-button :icon="ZoomOut" title="缩小" @click="zoomOut" />
          <el-button :icon="ZoomIn" title="放大" @click="zoomIn" />
          <el-button :icon="RefreshLeft" title="左转 90°" @click="rotateLeft" />
          <el-button :icon="RefreshRight" title="右转 90°" @click="rotateRight" />
          <el-button :icon="Refresh" title="复位" @click="resetTransform" />
        </el-button-group>
        <span class="viewer__scale">{{ Math.round(scale * 100) }}%</span>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.image {
  /* fit-content 让气泡贴合小图，max-width 保证再大的图也不会超出聊天区 */
  display: block;
  width: fit-content;
  max-width: 100%;
}

.image__box {
  display: block;
  cursor: zoom-in;
  line-height: 0;
}

.image__img {
  display: block;
  /* 宽度上限由外层媒体气泡控制，这里只管填满并按需要收缩 */
  max-width: 100%;
  max-height: 320px;
  height: auto;
  border-radius: 4px;
}

/* 还没解码出来：不占位、不可见，由占位块撑住气泡 */
.image__img--loading {
  width: 0;
  height: 0;
  opacity: 0;
}

.image__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 200px;
  height: 130px;
  border-radius: 4px;
  background: #f0f2f5;
  color: #c0c4cc;
}

.image__failed {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 4px;
  background: #f5f7fa;
  font-size: 12px;
  line-height: 1.5;
  color: #909399;
}

.viewer {
  display: flex;
  align-items: center;
  justify-content: center;
  /*
    按视口高度留出上下各 7.5% 的余量，再减掉弹窗头部、内边距与工具栏占掉的高度，
    剩下的都给图片；min-height 兜住窗口很矮的情况。
  */
  height: calc(85vh - 140px);
  min-height: 240px;
  overflow: hidden;
  cursor: grab;
  user-select: none;
}

.viewer:active {
  cursor: grabbing;
}

.viewer__img {
  max-width: 100%;
  max-height: 100%;
  transform-origin: center center;
}

.viewer__bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 12px;
}

.viewer__scale {
  min-width: 44px;
  font-size: 12px;
  color: #909399;
}
</style>
