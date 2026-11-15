<script setup lang="ts">
import { computed, watch } from 'vue'
import { useBroadcastStore } from '@/stores/broadcast'
import { useSubscribeStore } from '@/stores/subscribe'
import { shortKey } from '@/utils/display'
import { splitMentions } from '@/utils/mentions'

/**
 * 广播正文中的 @ 公钥渲染。
 *
 * <p>实际正文保存的是 {@code @公钥}，这里按关注别名、资料昵称、公钥末位的顺序换成易读名称。</p>
 */

const props = withDefaults(defineProps<{
  /** 原始正文。 */
  text: string
  /** @ 名称是否可以点击打开用户主页。 */
  interactive?: boolean
}>(), {
  interactive: true,
})

const emit = defineEmits<{
  /** 点击 @ 名称，查看对应用户。 */
  'mentionClick': [uid: string, anchor: HTMLElement]
}>()

const broadcastStore = useBroadcastStore()
const subscribeStore = useSubscribeStore()

/** 正文按普通文字与 @ 公钥拆分。 */
const segments = computed(() => splitMentions(props.text))

/** 正文中出现的被提及者公钥。 */
const mentionedKeys = computed(() => [
  ...new Set(segments.value
    .filter((segment) => segment.kind === 'mention' && segment.publicKey)
    .map((segment) => segment.publicKey as string)),
])

/**
 * 取 @ 的展示名称。
 *
 * @param uid 被提及者公钥
 * @returns 关注别名优先，其次资料昵称，最后公钥末位
 */
function mentionName(uid: string): string {
  return subscribeStore.aliasOf(uid)
    || broadcastStore.profileOf(uid)?.nickname
    || shortKey(uid, 5)
}

// 正文含 @ 时保证关注别名索引可用
watch(() => props.text, (value) => {
  if (value.includes('@')) {
    void subscribeStore.loadIndex().catch(() => {})
  }
}, { immediate: true })

// 补齐被提及者资料；关注索引或资料更新后模板会自动重绘
watch(mentionedKeys, (keys) => {
  if (keys.length === 0) {
    return
  }
  for (const uid of keys) {
    if (!broadcastStore.profileOf(uid)) {
      void broadcastStore.loadProfile(uid).catch(() => {})
    }
  }
}, { immediate: true })
</script>

<template>
  <span class="broadcast-text">
    <template v-for="(segment, index) in segments" :key="index">
      <button
        v-if="segment.kind === 'mention' && segment.publicKey && interactive !== false"
        type="button"
        class="broadcast-text__mention"
        :title="segment.text"
        @click.stop="emit('mentionClick', segment.publicKey, $event.currentTarget as HTMLElement)"
      >
        @{{ mentionName(segment.publicKey) }}
      </button>
      <span
        v-else-if="segment.kind === 'mention' && segment.publicKey"
        class="broadcast-text__mention broadcast-text__mention--static"
        :title="segment.text"
      >
        @{{ mentionName(segment.publicKey) }}
      </span>
      <template v-else>{{ segment.text }}</template>
    </template>
  </span>
</template>

<style scoped>
.broadcast-text {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.broadcast-text__mention {
  display: inline;
  padding: 0;
  border: 0;
  background: transparent;
  color: #409eff;
  cursor: pointer;
  font: inherit;
}

.broadcast-text__mention:hover,
.broadcast-text__mention:focus-visible {
  text-decoration: underline;
}

.broadcast-text__mention--static,
.broadcast-text__mention--static:hover {
  cursor: inherit;
  text-decoration: none;
}
</style>
