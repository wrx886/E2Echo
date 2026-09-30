<script setup lang="ts">
/**
 * 消息输入区。
 *
 * <p>Enter 发送、Shift + Enter 换行；正文由父组件持有（v-model），发送成功后由父组件清空。</p>
 */

defineProps<{
  /** 输入框内容。 */
  modelValue: string
  /** 是否正在发送。 */
  sending: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'submit': []
}>()

/**
 * 回车发送。
 *
 * <p>正在用输入法拼字时，回车是确认候选词而不是发送：此时不能阻止默认行为，也不能发送，
 * 否则中文输入会被打断、还会把没写完的内容发出去。{@code keyCode === 229} 是一些输入法在
 * {@code isComposing} 不可靠时的兜底判断。</p>
 *
 * @param event 键盘事件
 */
function onEnter(event: KeyboardEvent): void {
  if (event.isComposing || event.keyCode === 229) {
    return
  }
  event.preventDefault()
  emit('submit')
}
</script>

<template>
  <div class="composer">
    <el-input
      :model-value="modelValue"
      type="textarea"
      :rows="3"
      resize="none"
      maxlength="2000"
      placeholder="输入消息，Enter 发送，Shift + Enter 换行"
      @update:model-value="emit('update:modelValue', $event)"
      @keydown.enter.exact="onEnter"
    />

    <div class="composer__bar">
      <span class="composer__hint">Enter 发送 · Shift + Enter 换行</span>
      <el-button
        type="primary"
        :loading="sending"
        :disabled="modelValue.trim().length === 0"
        @click="emit('submit')"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.composer {
  flex: none;
  padding: 10px 16px 14px;
  border-top: 1px solid #e4e7ed;
  background: #fafafa;
}

.composer__bar {
  display: flex;
  align-items: center;
  margin-top: 8px;
}

.composer__hint {
  font-size: 12px;
  color: #a8abb2;
}

.composer__bar .el-button {
  margin-left: auto;
}
</style>
