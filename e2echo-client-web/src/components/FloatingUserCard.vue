<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { CSSProperties } from 'vue'
import ChatUserCard from '@/components/ChatUserCard.vue'

/**
 * 按点击元素的位置显示用户卡片。
 *
 * <p>正文里的 @ 是运行时生成的 DOM，使用 Element Plus 的虚拟锚点时，打开它的原点击事件也可能
 * 同时触发点击外部关闭。这里直接用固定定位和实际元素坐标，避免锚点挂载与关闭时序冲突。</p>
 */

const props = withDefaults(defineProps<{
  /** 用户公钥。 */
  uid: string
  /** 点击的锚点。 */
  anchor: HTMLElement
  /** 优先展示名称。 */
  preferredName?: string
  /** 是否显示 @ 操作。 */
  mentionable?: boolean
  /** 是否显示广播用户主页入口。 */
  showProfile?: boolean
  /** 是否显示私聊会话相关操作。 */
  conversationActions?: boolean
}>(), {
  conversationActions: true,
})

const emit = defineEmits<{
  /** 关闭卡片。 */
  'close': []
  /** 在当前输入框 @ 这位用户。 */
  'mention': [uid: string]
}>()

/** 浮层元素。 */
const panelRef = ref<HTMLDivElement>()

/** 浮层定位样式。 */
const style = ref<CSSProperties>({
  position: 'fixed',
  width: '300px',
  visibility: 'hidden',
  zIndex: 3000,
})

/** 浮层尺寸观察器。 */
let observer: ResizeObserver | null = null

/**
 * 根据锚点更新浮层位置。
 */
function updatePosition(): void {
  const panel = panelRef.value
  if (!panel) {
    return
  }
  const margin = 8
  const gap = 8
  const anchor = props.anchor.getBoundingClientRect()
  const width = Math.min(300, Math.max(180, window.innerWidth - margin * 2))
  const height = panel.offsetHeight || 240
  const left = Math.min(
    Math.max(anchor.left, margin),
    Math.max(margin, window.innerWidth - width - margin),
  )
  const below = anchor.bottom + gap
  const above = anchor.top - height - gap
  const top = below + height <= window.innerHeight - margin
    ? below
    : Math.max(margin, above)
  style.value = {
    position: 'fixed',
    left: `${left}px`,
    top: `${top}px`,
    width: `${width}px`,
    visibility: 'visible',
    zIndex: 3000,
  }
}

/**
 * 点击浮层或锚点外时关闭。
 *
 * @param event 指针事件
 */
function onPointerDown(event: PointerEvent): void {
  const target = event.target as Node | null
  if (!target || panelRef.value?.contains(target) || props.anchor.contains(target)) {
    return
  }
  emit('close')
}

/**
 * Esc 关闭卡片。
 *
 * @param event 键盘事件
 */
function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') {
    emit('close')
  }
}

onMounted(async () => {
  await nextTick()
  updatePosition()
  requestAnimationFrame(updatePosition)
  if (panelRef.value && typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(updatePosition)
    observer.observe(panelRef.value)
  }
  window.addEventListener('resize', updatePosition)
  window.addEventListener('scroll', updatePosition, true)
  document.addEventListener('pointerdown', onPointerDown)
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  observer?.disconnect()
  observer = null
  window.removeEventListener('resize', updatePosition)
  window.removeEventListener('scroll', updatePosition, true)
  document.removeEventListener('pointerdown', onPointerDown)
  document.removeEventListener('keydown', onKeydown)
})

watch(() => props.anchor, () => {
  void nextTick(updatePosition)
})
</script>

<template>
  <Teleport to="body">
    <div ref="panelRef" class="floating-user-card" :style="style" @pointerdown.stop>
      <ChatUserCard
        :uid="uid"
        :preferred-name="preferredName"
        :mentionable="mentionable"
        :show-profile="showProfile"
        :conversation-actions="conversationActions"
        @mention="emit('mention', $event)"
        @close="emit('close')"
      />
    </div>
  </Teleport>
</template>

<style scoped>
.floating-user-card {
  padding: 12px;
  max-height: min(360px, calc(100vh - 16px));
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.18);
}
</style>
