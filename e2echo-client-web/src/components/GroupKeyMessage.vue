<script setup lang="ts">
import { computed, watch } from 'vue'
import { useConversationStore } from '@/stores/conversation'
import { formatFullTime, shortKey } from '@/utils/display'

/**
 * 群聊密钥消息的正文。
 *
 * <p>群主把群密钥逐条私发给成员时发的就是这类消息。密钥本身是敏感数据、界面上不展示，这里只说明
 * 是哪个群、哪个版本：群优先显示会话别名，取不到时退回群标识末几位；版本用完整时间。</p>
 */

const props = defineProps<{
  /** 群标识。 */
  group: string
  /** 密钥签发时间（毫秒）。 */
  publishTime: number
}>()

const conversationStore = useConversationStore()

// 别名要查一次（会话列表里没有时会单独请求），取到后 computed 会自动换成别名
watch(() => props.group, (value) => {
  if (value.length > 0) {
    void conversationStore.resolveAlias(value)
  }
}, { immediate: true })

/** 群展示名称。 */
const groupLabel = computed(() =>
  conversationStore.aliasOf(props.group) ?? shortKey(props.group, 8))
</script>

<template>
  <div class="key-message">
    <div class="key-message__title">群聊密钥</div>
    <div class="key-message__line">
      群：<span :title="group">{{ groupLabel }}</span>
    </div>
    <div class="key-message__line">版本：{{ formatFullTime(publishTime) }}</div>
  </div>
</template>

<style scoped>
.key-message__title {
  margin-bottom: 2px;
  font-weight: 600;
}

.key-message__line {
  font-size: 13px;
  line-height: 1.6;
  word-break: break-all;
}
</style>
