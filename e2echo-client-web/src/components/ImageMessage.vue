<script setup lang="ts">
import { inject, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
import { CHAT_PIN_TO_BOTTOM } from '@/utils/chatScroll'
import { saveBlob } from '@/utils/download'
import { loadMediaBlob, loadMediaUrl } from '@/utils/media'
import { showError } from '@/utils/feedback'

/**
 * 图片消息。
 *
 * <p>图片字节要 POST 给 client 才能拿到，所以不能直接写进 {@code src}：这里在图片将要进入视野时才
 * 去取（懒加载），取回后转成 object URL 渲染缩略图，点击弹窗看原图。</p>
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

/** 缩略图的最大边长。 */
const THUMB_MAX_SIZE = 320

/** 图片地址，空串表示还没取到。 */
const url = ref('')

/** 按最大边缩过的显示尺寸；量不出来时为 0，交给 CSS 约束。 */
const width = ref(0)
const height = ref(0)

/** 是否正在取图片。 */
const loading = ref(false)

/** 取图片失败（或取回来但不是能渲染的图片）时的提示。 */
const failed = ref('')

/** 是否正在看大图。 */
const previewVisible = ref(false)

/** 是否正在下载原图。 */
const downloading = ref(false)

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
 * 取回图片。
 */
async function load(): Promise<void> {
  if (loading.value) {
    return
  }
  loading.value = true
  failed.value = ''
  try {
    const address = await loadMediaUrl(props.file)
    await measure(address)
    url.value = address
    // 等图片进入布局，再把视图贴回底部（用户已经往上翻的话聊天页会忽略）
    await nextTick()
    pinToBottom?.()
  } catch (error) {
    failed.value = '图片加载失败'
    showError(error, '图片加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * 先量出图片的真实尺寸，再决定显示尺寸。
 *
 * <p>直接插入未解码的 {@code <img>} 会让气泡先塌成 0 再涨回来，高度来回变两次，滚动很难跟准；
 * 先把宽高算好写进去，气泡只会在插入时变高一次。量不出来（解码失败）就保持 0，退回由 CSS 约束。</p>
 *
 * @param address 图片地址
 */
async function measure(address: string): Promise<void> {
  try {
    const image = new Image()
    image.src = address
    await image.decode()
    const scale = Math.min(
      1,
      THUMB_MAX_SIZE / image.naturalWidth,
      THUMB_MAX_SIZE / image.naturalHeight,
    )
    width.value = Math.round(image.naturalWidth * scale)
    height.value = Math.round(image.naturalHeight * scale)
  } catch {
    width.value = 0
    height.value = 0
  }
}

/**
 * 图片渲染完成后贴一次底部：量尺寸失败时，尺寸要到这一刻才知道。
 */
function onImageLoad(): void {
  pinToBottom?.()
}

/**
 * 下载原图：字节已经在缓存里（看图时取过），直接交给浏览器保存。
 */
async function onDownload(): Promise<void> {
  downloading.value = true
  try {
    const blob = await loadMediaBlob(props.file)
    saveBlob(blob, props.file.filename)
  } catch (error) {
    showError(error, '图片下载失败')
  } finally {
    downloading.value = false
  }
}

/**
 * 重试：先清掉地址再取一次。
 */
async function onRetry(): Promise<void> {
  url.value = ''
  width.value = 0
  height.value = 0
  await load()
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
    void load()
    return
  }
  observer = new IntersectionObserver((entries) => {
    if (started || !entries.some((entry) => entry.isIntersecting)) {
      return
    }
    started = true
    observer?.disconnect()
    observer = null
    void load()
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
    <div v-if="url && failed.length === 0" class="image__box" @click="previewVisible = true">
      <img
        class="image__img"
        :src="url"
        :width="width > 0 ? width : undefined"
        :height="height > 0 ? height : undefined"
        :alt="file.filename"
        @load="onImageLoad"
        @error="onImageError"
      />
    </div>

    <div v-else-if="loading" class="image__placeholder">
      <el-icon class="is-loading" :size="18"><Loading /></el-icon>
    </div>

    <div v-else-if="failed.length > 0" class="image__failed">
      <span>{{ failed }}</span>
      <el-button size="small" text :icon="RefreshLeft" @click="onRetry">重试</el-button>
    </div>

    <!-- 还没进入视野：留个占位，等懒加载触发 -->
    <div v-else class="image__placeholder" />

    <el-dialog v-model="previewVisible" :title="file.filename" width="80%" align-center>
      <div
        class="viewer"
        @wheel.prevent="onWheel"
        @pointerdown="onPointerDown"
        @pointermove="onPointerMove"
        @pointerup="onPointerUp"
        @pointerleave="onPointerUp"
      >
        <img
          v-if="url"
          class="viewer__img"
          :src="url"
          :style="{ transform: transformStyle }"
          :alt="file.filename"
          draggable="false"
        />
      </div>

      <div class="viewer__bar">
        <el-button-group>
          <el-button :icon="Download" :loading="downloading" title="下载原图" @click="onDownload" />
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
}

.image__img {
  display: block;
  max-width: 100%;
  /* 宽高由组件按真实比例写好，窄窗口下只收缩宽度、高度按同比例跟着变 */
  height: auto;
  border-radius: 4px;
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
