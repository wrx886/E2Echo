<script setup lang="ts">
import { computed, inject, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { Headset, Loading, RefreshLeft } from '@element-plus/icons-vue'
import { chatFileUrl } from '@/api'
import type { ChatFileMessageVo } from '@/api'
import { CHAT_PIN_TO_BOTTOM } from '@/utils/chatScroll'

/**
 * 音频消息。
 *
 * <p>和视频一样：进入视野才把地址交给播放器（懒加载），播放器自己取、自己按区间请求。下载直接用
 * 播放器自带的能力，前端不再单独放按钮。</p>
 */

const props = defineProps<{
  /** 音频消息正文（文件名、密钥、对象键）。 */
  file: ChatFileMessageVo
}>()

/** 让聊天页把视图贴回底部的能力。 */
const pinToBottom = inject(CHAT_PIN_TO_BOTTOM, null)

/** 是否已经进入视野：进入后才把地址交给播放器，实现懒加载。 */
const revealed = ref(false)

/** 加载失败（或取回来但播放器解不出来）时的提示。 */
const failed = ref('')

/** 音频地址：空串表示还没进入视野。 */
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
 * 拿到元数据：让聊天页贴回底部（气泡高度在这一刻定下来）。
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
 * 字节拿到了但播放器解不出来（例如发送方给的根本不是音频）。
 */
function onAudioError(): void {
  failed.value = '音频无法播放'
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
  <div ref="root" class="audio">
    <template v-if="revealed && failed.length === 0">
      <div class="audio__head">
        <el-icon class="audio__icon" :size="16"><Headset /></el-icon>
        <span class="audio__name" :title="file.filename">{{ file.filename }}</span>
      </div>
      <audio
        class="audio__player"
        :src="src"
        controls
        preload="metadata"
        @loadedmetadata="onLoadedMetadata"
        @error="onAudioError"
      />
    </template>

    <!-- 还没进入视野：留个占位，等懒加载触发 -->
    <div v-if="!revealed && failed.length === 0" class="audio__placeholder">
      <el-icon class="is-loading" :size="18"><Loading /></el-icon>
    </div>

    <div v-if="failed.length > 0" class="audio__failed">
      <span>{{ failed }}</span>
      <el-button size="small" text :icon="RefreshLeft" @click="onRetry">重试</el-button>
    </div>
  </div>
</template>

<style scoped>
.audio {
  /* 标题一行、播放器一行；fit-content 让气泡贴合内容，max-width 保证不超出聊天区 */
  display: block;
  width: fit-content;
  min-width: 0;
  max-width: 100%;
}

.audio__head {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.audio__icon {
  flex: none;
  color: #409eff;
}

.audio__name {
  overflow: hidden;
  font-size: 13px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.audio__player {
  display: block;
  /* 给足宽度：浏览器原生控件的进度条与按钮挤在窄宽度里会很难用 */
  width: 360px;
  max-width: 100%;
  margin-top: 6px;
}

.audio__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 200px;
  height: 56px;
  border-radius: 4px;
  background: #f0f2f5;
  color: #c0c4cc;
}

.audio__failed {
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
