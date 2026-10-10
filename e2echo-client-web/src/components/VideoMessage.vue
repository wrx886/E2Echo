<script setup lang="ts">
import { computed, inject, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { Loading, RefreshLeft } from '@element-plus/icons-vue'
import { chatFileUrl } from '@/api'
import type { ChatFileMessageVo } from '@/api'
import { CHAT_PIN_TO_BOTTOM } from '@/utils/chatScroll'

/**
 * 视频消息。
 *
 * <p>文件接口支持 GET + Range，地址可以直接写进 {@code src}：这里在将要进入视野时才挂上地址（懒加载），
 * 播放器自己取、自己按区间请求。放大、全屏、画中画、下载都用播放器自带的能力。</p>
 */

const props = defineProps<{
  /** 视频消息正文（文件名、密钥、对象键）。 */
  file: ChatFileMessageVo
}>()

/**
 * 让聊天页把视图贴回底部的能力（视频撑开气泡后通知它）。
 */
const pinToBottom = inject(CHAT_PIN_TO_BOTTOM, null)

/** 是否已经进入视野：进入后才把地址交给播放器，实现懒加载。 */
const revealed = ref(false)

/** 加载失败（或取回来但播放器解不出来）时的提示。 */
const failed = ref('')

/** 视频地址：空串表示还没进入视野。 */
const src = computed(() => (revealed.value ? chatFileUrl(props.file) : ''))

/** 懒加载是否已经触发过，只触发一次。 */
let started = false

/** 视野观察器。 */
let observer: IntersectionObserver | null = null

/** 组件根节点，用来观察是否进入视野。 */
const root = ref<HTMLDivElement>()

/**
 * 让播放器开始加载：进入视野时调用一次。
 */
function reveal(): void {
  revealed.value = true
}

/**
 * 拿到元数据：尺寸在这一刻确定，让聊天页贴回底部（播放器会撑高气泡）。
 */
function onLoadedMetadata(): void {
  pinToBottom?.()
}

/**
 * 重试：把播放器重新挂一次，触发一次新的请求。
 */
async function onRetry(): Promise<void> {
  failed.value = ''
  revealed.value = false
  await nextTick()
  revealed.value = true
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
</script>

<template>
  <div ref="root" class="video">
    <video
      v-if="revealed && failed.length === 0"
      class="video__player"
      :src="src"
      controls
      preload="metadata"
      @loadedmetadata="onLoadedMetadata"
      @error="onVideoError"
    />

    <!-- 还没进入视野：留个占位，等懒加载触发 -->
    <div v-if="!revealed && failed.length === 0" class="video__placeholder">
      <el-icon class="is-loading" :size="18"><Loading /></el-icon>
    </div>

    <div v-if="failed.length > 0" class="video__failed">
      <span>{{ failed }}</span>
      <el-button size="small" text :icon="RefreshLeft" @click="onRetry">重试</el-button>
    </div>
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
  /* 播放器最大边 320，窄窗口下按可用宽度收缩 */
  max-width: min(320px, 100%);
  max-height: 320px;
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
