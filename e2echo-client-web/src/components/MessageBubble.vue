<script setup lang="ts">
import { computed } from 'vue'
import { chatTextOf } from '@/api'
import type { MessageVo } from '@/api'
import { shortKey } from '@/utils/display'

/**
 * 单条消息气泡。
 *
 * <p>自己发的靠右、绿色气泡；对方发的靠左、白色气泡，群聊里在气泡上方标出发送者（没有用户资料
 * 接口，只能取公钥末几位）。</p>
 */

const props = defineProps<{
  /** 消息。 */
  message: MessageVo
  /** 是否是自己发的。 */
  mine: boolean
  /** 是否群聊会话。 */
  group: boolean
  /** 自己的头像文字；为空时按发送者公钥算。 */
  mineAvatar?: string
}>()

/** 文字正文，非文字消息为空。 */
const text = computed(() => chatTextOf(props.message))

/** 头像文字：自己的消息用自己的名称，对方的按公钥末几位。 */
const avatar = computed(() =>
  props.mine && props.mineAvatar ? props.mineAvatar : shortKey(props.message.from, 2))
</script>

<template>
  <div class="bubble-row" :class="mine ? 'bubble-row--mine' : 'bubble-row--other'">
    <el-avatar
      :size="32"
      class="bubble-row__avatar"
      :class="{ 'bubble-row__avatar--mine': mine }"
    >
      {{ avatar }}
    </el-avatar>

    <div class="bubble-row__body">
      <div v-if="group && !mine" class="bubble-row__name">{{ shortKey(message.from) }}</div>
      <div class="bubble">{{ text ?? '[暂不支持的消息类型]' }}</div>
    </div>
  </div>
</template>

<style scoped>
.bubble-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 4px 16px;
}

.bubble-row--mine {
  flex-direction: row-reverse;
}

.bubble-row__avatar {
  flex: none;
  background: #d8d8d8;
  color: #555;
}

.bubble-row__avatar--mine {
  background: #07c160;
  color: #fff;
}

.bubble-row__body {
  display: flex;
  flex-direction: column;
  max-width: min(70%, 560px);
}

.bubble-row--mine .bubble-row__body {
  align-items: flex-end;
}

.bubble-row__name {
  margin-bottom: 2px;
  font-size: 12px;
  color: #909399;
}

.bubble {
  padding: 8px 12px;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-row--mine .bubble {
  background: #95ec69;
}
</style>
