<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { Microphone, Paperclip, Picture, VideoCamera } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { InputInstance } from 'element-plus'
import { FILE_MAX_SIZE_BYTE, FILE_MAX_SIZE_MESSAGE } from '@/api'
import EmojiPicker from '@/components/EmojiPicker.vue'

/**
 * 消息输入区。
 *
 * <p>Enter 发送、Shift + Enter 换行；正文由父组件持有（v-model），发送成功后由父组件清空。文件也
 * 在这里选：选中后交给父组件上传发送，本组件只负责选文件与显示进度。表情同样是本组件负责插到
 * 输入框里。</p>
 */

/** 输入框允许的最大长度，与模板上的 maxlength 保持一致。 */
const MAX_LENGTH = 2000

/** 输入框长度达到上限时的提示。 */
const MAX_LENGTH_MESSAGE = '消息长度已达上限'

const props = defineProps<{
  /** 输入框内容。 */
  modelValue: string
  /** 是否正在发送。 */
  sending: boolean
  /** 是否正在发送文件。 */
  sendingFile: boolean
  /** 文件上传进度，0-100。 */
  filePercent: number
}>()

/** 输入框实例：插表情时要拿它里面的原生输入框取光标位置。 */
const inputRef = ref<InputInstance>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'submit': []
  'submitFile': [file: File]
  'submitImage': [file: File]
  'submitVideo': [file: File]
  'submitAudio': [file: File]
}>()

/** 隐藏的文件选择框（任意文件）。 */
const fileInput = ref<HTMLInputElement>()

/** 隐藏的图片选择框。 */
const imageInput = ref<HTMLInputElement>()

/** 隐藏的视频选择框。 */
const videoInput = ref<HTMLInputElement>()

/** 隐藏的音频选择框。 */
const audioInput = ref<HTMLInputElement>()

/** 正在发送的文件名，用于进度提示。 */
const pendingName = ref('')

/**
 * 打开文件选择框。
 */
function onPickFile(): void {
  fileInput.value?.click()
}

/**
 * 打开图片选择框。
 */
function onPickImage(): void {
  imageInput.value?.click()
}

/**
 * 打开视频选择框。
 */
function onPickVideo(): void {
  videoInput.value?.click()
}

/**
 * 打开音频选择框。
 */
function onPickAudio(): void {
  audioInput.value?.click()
}

/**
 * 选中表情：插到光标处，光标停在表情后面。
 *
 * @param emoji 表情
 */
function onPickEmoji(emoji: string): void {
  const textarea = inputRef.value?.textarea
  if (!textarea) {
    // 拿不到原生输入框（实际不会发生）就退化成追加到末尾
    appendEmoji(emoji)
    return
  }

  const start = textarea.selectionStart ?? props.modelValue.length
  const end = textarea.selectionEnd ?? start
  const value = props.modelValue.slice(0, start) + emoji + props.modelValue.slice(end)
  if (value.length > MAX_LENGTH) {
    ElMessage.warning(MAX_LENGTH_MESSAGE)
    return
  }

  emit('update:modelValue', value)
  // 等父组件把新值写回输入框之后，再把光标放到插入内容之后并保持焦点
  void nextTick(() => {
    const caret = start + emoji.length
    textarea.setSelectionRange(caret, caret)
    textarea.focus()
  })
}

/**
 * 追加表情，用于拿不到原生输入框时兜底。
 *
 * @param emoji 表情
 */
function appendEmoji(emoji: string): void {
  if (props.modelValue.length + emoji.length > MAX_LENGTH) {
    ElMessage.warning(MAX_LENGTH_MESSAGE)
    return
  }
  emit('update:modelValue', props.modelValue + emoji)
}

/**
 * 选中文件后交给父组件发送。
 *
 * <p>选完把输入框清空，这样连续选同一个文件也能再次触发 change。</p>
 *
 * @param event 选择框的 change 事件
 */
function onFileChange(event: Event): void {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  submitAttachment(file, 'submitFile')
}

/**
 * 选中图片后交给父组件发送。
 *
 * @param event 选择框的 change 事件
 */
function onImageChange(event: Event): void {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  submitAttachment(file, 'submitImage')
}

/**
 * 选中视频后交给父组件发送。
 *
 * @param event 选择框的 change 事件
 */
function onVideoChange(event: Event): void {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  submitAttachment(file, 'submitVideo')
}

/**
 * 选中音频后交给父组件发送。
 *
 * @param event 选择框的 change 事件
 */
function onAudioChange(event: Event): void {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  submitAttachment(file, 'submitAudio')
}

/**
 * 附件发送前的统一处理：超过大小上限的直接拦下来，不进入上传流程。
 *
 * @param file  选中的文件，可能为空（用户取消选择）
 * @param event 交给父组件的哪个事件
 */
type AttachmentEvent = 'submitFile' | 'submitImage' | 'submitVideo' | 'submitAudio'

function submitAttachment(file: File | undefined, event: AttachmentEvent): void {
  if (!file) {
    return
  }
  if (file.size > FILE_MAX_SIZE_BYTE) {
    ElMessage.warning(FILE_MAX_SIZE_MESSAGE)
    return
  }
  pendingName.value = file.name
  if (event === 'submitImage') {
    emit('submitImage', file)
  } else if (event === 'submitVideo') {
    emit('submitVideo', file)
  } else if (event === 'submitAudio') {
    emit('submitAudio', file)
  } else {
    emit('submitFile', file)
  }
}

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
      ref="inputRef"
      :model-value="modelValue"
      type="textarea"
      :rows="3"
      resize="none"
      :maxlength="MAX_LENGTH"
      placeholder="输入消息，Enter 发送，Shift + Enter 换行"
      @update:model-value="emit('update:modelValue', $event)"
      @keydown.enter.exact="onEnter"
    />

    <div class="composer__bar">
      <!-- 特殊类型内容的发送按钮都放左边：文件、后续的图片等 -->
      <div class="composer__tools">
        <el-button
          :icon="Picture"
          :loading="sendingFile"
          :disabled="sendingFile"
          title="发送图片"
          @click="onPickImage"
        />
        <el-button
          :icon="VideoCamera"
          :loading="sendingFile"
          :disabled="sendingFile"
          title="发送视频"
          @click="onPickVideo"
        />
        <el-button
          :icon="Microphone"
          :loading="sendingFile"
          :disabled="sendingFile"
          title="发送音频"
          @click="onPickAudio"
        />
        <el-button
          :icon="Paperclip"
          :loading="sendingFile"
          :disabled="sendingFile"
          title="发送文件"
          @click="onPickFile"
        />
      </div>

      <!-- 提示与发送按钮放右边 -->
      <div class="composer__actions">
        <span class="composer__hint">
          <template v-if="sendingFile">
            正在发送 {{ pendingName }}<template v-if="filePercent > 0">（{{ filePercent }}%）</template>
          </template>
          <template v-else>Enter 发送 · Shift + Enter 换行</template>
        </span>
        <EmojiPicker @pick="onPickEmoji" />
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

    <input ref="fileInput" class="composer__file" type="file" @change="onFileChange" />
    <input
      ref="imageInput"
      class="composer__file"
      type="file"
      accept="image/*"
      @change="onImageChange"
    />
    <input
      ref="videoInput"
      class="composer__file"
      type="file"
      accept="video/*"
      @change="onVideoChange"
    />
    <input
      ref="audioInput"
      class="composer__file"
      type="file"
      accept="audio/*"
      @change="onAudioChange"
    />
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

.composer__tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.composer__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.composer__hint {
  font-size: 12px;
  color: #a8abb2;
}

.composer__file {
  display: none;
}

</style>
