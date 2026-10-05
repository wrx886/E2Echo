<script setup lang="ts">
import { inject, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { Loading, RefreshLeft } from '@element-plus/icons-vue'
import type { ChatFileMessageVo } from '@/api'
import { CHAT_PIN_TO_BOTTOM } from '@/utils/chatScroll'
import { showError } from '@/utils/feedback'
import { loadMediaUrl } from '@/utils/media'

/**
 * 视频消息。
 *
 * <p>视频字节要 POST 给 client 才能拿到，所以不能直接写进 {@code src}：这里在将要进入视野时才去取
 * （懒加载），取回后转成 object URL 用播放器内嵌播放。放大、全屏、画中画都用播放器自带的能力，
 * 下载同理，前端不再另外放按钮或弹窗。</p>
 */

/** 内嵌播放器的最大边长。 */
const THUMB_MAX_SIZE = 320

const props = defineProps<{
  /** 视频消息正文（文件名、密钥、对象键）。 */
  file: ChatFileMessageVo
}>()

/**
 * 让聊天页把视图贴回底部的能力（视频撑开气泡后通知它）。
 */
const pinToBottom = inject(CHAT_PIN_TO_BOTTOM, null)

/** 视频地址，空串表示还没取到。 */
const url = ref('')

/** 按最长边缩过的显示尺寸；量不出来时为 0，交给 CSS 约束。 */
const width = ref(0)
const height = ref(0)

/** 是否正在取视频。 */
const loading = ref(false)

/** 取视频失败（或取回来但没法播放）时的提示。 */
const failed = ref('')

/** 懒加载是否已经触发过，只触发一次。 */
let started = false

/** 视野观察器。 */
let observer: IntersectionObserver | null = null

/** 组件根节点，用来观察是否进入视野。 */
const root = ref<HTMLDivElement>()

/**
 * 取回视频。
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
    // 等播放器进入布局，再把视图贴回底部（用户已经往上翻的话聊天页会忽略）
    await nextTick()
    pinToBottom?.()
  } catch (error) {
    failed.value = '视频加载失败'
    showError(error, '视频加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * 先量出视频尺寸，再决定显示尺寸（和图片一样，避免气泡先塌后涨）。
 *
 * @param address 视频地址
 */
async function measure(address: string): Promise<void> {
  try {
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.src = address
    await new Promise<void>((resolve, reject) => {
      const timer = window.setTimeout(() => reject(new Error('读取超时')), 3000)
      video.onloadedmetadata = () => {
        window.clearTimeout(timer)
        resolve()
      }
      video.onerror = () => {
        window.clearTimeout(timer)
        reject(new Error('读取失败'))
      }
    })
    if (video.videoWidth === 0 || video.videoHeight === 0) {
      width.value = 0
      height.value = 0
      return
    }
    const scale = Math.min(
      1,
      THUMB_MAX_SIZE / video.videoWidth,
      THUMB_MAX_SIZE / video.videoHeight,
    )
    width.value = Math.round(video.videoWidth * scale)
    height.value = Math.round(video.videoHeight * scale)
  } catch {
    width.value = 0
    height.value = 0
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
 * 字节拿到了但播放器解不出来（例如发送方给的根本不是视频）。
 */
function onVideoError(): void {
  failed.value = '视频无法播放'
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
</script>

<template>
  <div ref="root" class="video">
    <video
      v-if="url && failed.length === 0"
      class="video__player"
      :src="url"
      :width="width > 0 ? width : undefined"
      :height="height > 0 ? height : undefined"
      controls
      preload="metadata"
      @loadedmetadata="pinToBottom?.()"
      @error="onVideoError"
    />

    <div v-else-if="loading" class="video__placeholder">
      <el-icon class="is-loading" :size="18"><Loading /></el-icon>
    </div>

    <div v-else-if="failed.length > 0" class="video__failed">
      <span>{{ failed }}</span>
      <el-button size="small" text :icon="RefreshLeft" @click="onRetry">重试</el-button>
    </div>

    <!-- 还没进入视野：留个占位，等懒加载触发 -->
    <div v-else class="video__placeholder" />

  </div>
</template>

<style scoped>
.video {
  display: block;
  width: fit-content;
  max-width: 100%;
}

.video__player {
  display: block;
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  background: #000;
}

.video__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 200px;
  height: 130px;
  border-radius: 4px;
  background: #f0f2f5;
  color: #c0c4cc;
}

.video__failed {
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

</style>
