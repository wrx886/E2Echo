<script setup lang="ts">
import { computed, inject, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { Loading, RefreshLeft } from '@element-plus/icons-vue'
import type { ChatFileMessageVo } from '@/api'
import { chatFileUrl } from '@/api'
import ImagePreviewDialog from '@/components/ImagePreviewDialog.vue'
import { CHAT_PIN_TO_BOTTOM } from '@/utils/chatScroll'

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

    <!-- 放大看原图：查看器和广播里的图片附件共用同一个组件 -->
    <ImagePreviewDialog
      v-model:visible="previewVisible"
      :src="src"
      :title="file.filename"
    />
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

</style>
