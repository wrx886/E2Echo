<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  Download,
  Refresh,
  RefreshLeft,
  RefreshRight,
  ZoomIn,
  ZoomOut,
} from '@element-plus/icons-vue'
import { safeFileName } from '@/utils/filename'

/**
 * 图片放大查看器。
 *
 * <p>聊天里的图片消息和广播里的图片附件共用这一个弹窗：工具栏、滚轮缩放、拖动、旋转、复位、下载都
 * 一样。以前广播那边是自己写的一个"img + 弹窗"，少了整排按钮，所以抽出来共用。</p>
 */

const props = defineProps<{
  /** 是否显示。 */
  visible: boolean
  /** 图片地址。 */
  src: string
  /** 标题（一般是文件名），同时作为下载时的文件名。 */
  title?: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

/** 当前缩放倍数。 */
const scale = ref(1)

/** 当前旋转角度（度，顺时针）。 */
const rotate = ref(0)

/** 拖动后的位移。 */
const offsetX = ref(0)
const offsetY = ref(0)

/** 缩放下限与上限。 */
const MIN_SCALE = 0.25
const MAX_SCALE = 5

/** 每次放大/缩小的倍率。 */
const SCALE_STEP = 1.25

/** 是否正在拖动。 */
let dragging = false
let dragStartX = 0
let dragStartY = 0
let dragStartOffsetX = 0
let dragStartOffsetY = 0

/** 当前的变换。 */
const transformStyle = ref('')

/**
 * 刷新变换：先平移，再缩放，最后旋转。
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
 * 开始拖动。
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
 * 拖动：拉不动时不用管，复位可以一键回到原位。
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

/**
 * 下载原图：直接用图片地址触发一次浏览器下载。
 */
function onDownload(): void {
  const anchor = document.createElement('a')
  anchor.href = props.src
  anchor.download = safeFileName(props.title ?? '')
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
}

// 每次打开都从初始状态开始，免得上次的缩放旋转带到下一张图上
watch(() => props.visible, (value) => {
  if (value) {
    resetTransform()
  }
}, { immediate: true })
</script>

<template>
  <!-- append-to-body：弹窗默认渲染在原地，会被可滚动容器或"整卡可点"的帖子夹住 -->
  <el-dialog
    :model-value="visible"
    :title="title"
    width="80%"
    align-center
    append-to-body
    @update:model-value="emit('update:visible', $event)"
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
        :alt="title"
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
</template>

<style scoped>
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
