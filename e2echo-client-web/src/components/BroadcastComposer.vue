<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { InputInstance } from 'element-plus'
import { Close, Headset, Paperclip, Picture, VideoCamera } from '@element-plus/icons-vue'
import {
  FILE_MAX_SIZE_BYTE,
  FILE_MAX_SIZE_MESSAGE,
  uploadBroadcastFile,
} from '@/api'
import type { BroadcastFileVo } from '@/api'
import BroadcastAttachments from '@/components/BroadcastAttachments.vue'
import type { AttachmentItem } from '@/components/BroadcastAttachments.vue'
import EmojiPicker from '@/components/EmojiPicker.vue'
import { showError } from '@/utils/feedback'

/**
 * 广播发布框：发帖和评论共用。
 *
 * <p>附件一律**到提交时才上传**：选中文件只记下来并本地预览，点发布时才逐个上传拿回附件信息、再连同
 * 正文一起发出去。这样用户中途放弃就不会在对象存储里留下没人引用的文件。输入内容由本组件自己持有，
 * 发布成功后由调用方调 {@link clear} 清空；边框由调用方按位置决定（底部工具条、发帖页都不需要自己
 * 的边框）。</p>
 *
 * <p>附件预览直接用消息里那套 {@link BroadcastAttachments}：写的时候看到的排版就是发布后的样子。</p>
 */

const props = defineProps<{
  /** 输入框占位提示。 */
  placeholder: string
  /** 发布按钮文字。 */
  submitText: string
  /** 是否正在发布。 */
  submitting: boolean
  /** 正在回复谁（评论用），为空表示不显示回复提示。 */
  replyToName?: string
  /** 正文最大长度。 */
  maxLength?: number
  /** 输入框行数。 */
  rows?: number
  /** 是否随内容自动增高（到上限后内部滚动）。 */
  autosize?: boolean
}>()

const emit = defineEmits<{
  /** 点击发布：正文与附件。 */
  'submit': [text: string, files: BroadcastFileVo[]]
  /** 取消回复。 */
  'cancelReply': []
}>()

/** 正文。 */
const text = ref('')

/** 已上传的附件。 */
interface PendingAttachment {
  /** 本地文件。 */
  file: File
  /** 本地预览地址，移除或卸载时释放。 */
  localUrl: string
  /** 上传结果：传过之后记下来，发布失败重试时不再重复上传。 */
  uploaded?: BroadcastFileVo
}

/** 待上传的附件：提交时才真正上传。 */
const pending = ref<PendingAttachment[]>([])

/** 预览用的附件列表：形状和上传结果一致，图片用本地地址。 */
const previewFiles = computed<AttachmentItem[]>(() => pending.value.map((item) => ({
  objectKey: item.localUrl,
  filename: item.file.name,
  contentType: item.file.type || 'application/octet-stream',
  size: item.file.size,
  localUrl: item.localUrl,
})))

/** 是否有附件正在上传。 */
const uploading = ref(false)

/** 当前上传进度，0-100。 */
const progress = ref(0)

/** 隐藏的文件选择框。 */
const fileInput = ref<HTMLInputElement>()

/** 输入框实例：插表情时要拿它里面的原生输入框取光标位置。 */
const inputRef = ref<InputInstance>()

/** 文件选择框的 accept：按点的按钮来筛类型，空串表示不限。 */
const accept = ref('')

/**
 * 按类型打开文件选择框。
 *
 * @param kind 附件类型：image / video / audio / file
 */
function pick(kind: 'image' | 'video' | 'audio' | 'file'): void {
  accept.value = kind === 'file' ? '' : `${kind}/*`
  // 等 accept 更新到输入框之后再点，免得浏览器还按上一次的类型过滤
  void nextTick(() => fileInput.value?.click())
}

/**
 * 选中文件：只记下来并本地预览，上传留到提交时。
 *
 * @param event 选择框的 change 事件
 */
function onFilesChange(event: Event): void {
  const input = event.target as HTMLInputElement
  const picked = Array.from(input.files ?? [])
  // 选完就清空，方便连续选同一个文件
  input.value = ''

  for (const file of picked) {
    if (file.size > FILE_MAX_SIZE_BYTE) {
      ElMessage.warning(FILE_MAX_SIZE_MESSAGE)
      continue
    }
    pending.value = [...pending.value, { file, localUrl: URL.createObjectURL(file) }]
  }
}

/**
 * 移除一个待上传的附件。
 *
 * @param file 待移除的附件
 */
function removeFile(file: AttachmentItem): void {
  const target = pending.value.find((item) => item.localUrl === file.localUrl)
  if (target) {
    URL.revokeObjectURL(target.localUrl)
  }
  pending.value = pending.value.filter((item) => item.localUrl !== file.localUrl)
}

/**
 * 发布：先把附件传上去，再把正文与附件交给调用方发出去。
 */
async function onSubmit(): Promise<void> {
  const value = text.value.trim()
  if (value.length === 0) {
    ElMessage.warning('写点什么吧')
    return
  }
  if (uploading.value || props.submitting) {
    return
  }

  // 附件到提交时才上传：用户中途放弃就不会留下没人引用的文件
  const uploaded: BroadcastFileVo[] = []
  if (pending.value.length > 0) {
    uploading.value = true
    try {
      for (const item of pending.value) {
        // 已经传过的（发布失败后重试）直接用上次的结果，不再传一遍
        if (!item.uploaded) {
          progress.value = 0
          item.uploaded = await uploadBroadcastFile(item.file, (percent) => {
            progress.value = percent
          })
        }
        uploaded.push(item.uploaded)
      }
    } catch (error) {
      showError(error, '附件上传失败')
      return
    } finally {
      uploading.value = false
      progress.value = 0
    }
  }

  emit('submit', value, uploaded)
}

/**
 * 选中表情：插到光标处，光标停在表情后面。
 *
 * @param emoji 表情
 */
function onPickEmoji(emoji: string): void {
  const limit = props.maxLength ?? 500
  const textarea = inputRef.value?.textarea
  if (!textarea) {
    // 拿不到原生输入框（实际不会发生）就追加到末尾
    if (text.value.length + emoji.length <= limit) {
      text.value += emoji
    }
    return
  }

  const start = textarea.selectionStart ?? text.value.length
  const end = textarea.selectionEnd ?? start
  const value = text.value.slice(0, start) + emoji + text.value.slice(end)
  if (value.length > limit) {
    ElMessage.warning('内容已达长度上限')
    return
  }

  text.value = value
  // 等新值写回输入框之后，再把光标放到插入内容之后并保持焦点
  void nextTick(() => {
    const caret = start + emoji.length
    textarea.setSelectionRange(caret, caret)
    textarea.focus()
  })
}

/**
 * 聚焦到输入框：回复某条评论时由调用方调用，省得用户还要自己点一下。
 */
function focus(): void {
  inputRef.value?.focus()
}

/**
 * 清空输入，发布成功后由调用方调用。
 */
function clear(): void {
  text.value = ''
  releasePending()
}

/**
 * 释放待上传附件的本地预览地址。
 */
function releasePending(): void {
  for (const item of pending.value) {
    URL.revokeObjectURL(item.localUrl)
  }
  pending.value = []
}

onUnmounted(releasePending)

defineExpose({ clear, focus })
</script>

<template>
  <div class="composer">
    <div v-if="replyToName" class="composer__reply">
      回复 @{{ replyToName }}
      <el-button text size="small" :icon="Close" @click="emit('cancelReply')" />
    </div>

    <el-input
      ref="inputRef"
      v-model="text"
      type="textarea"
      :rows="rows ?? 3"
      :autosize="autosize ? { minRows: rows ?? 4, maxRows: 12 } : undefined"
      resize="none"
      :maxlength="maxLength ?? 500"
      :placeholder="placeholder"
    />

    <!-- 预览和发布后的排版一致，只是每个附件多一个移除按钮 -->
    <BroadcastAttachments :files="previewFiles" removable @remove="removeFile" />

    <div class="composer__bar">
      <el-button
        :icon="Picture"
        :loading="uploading"
        :disabled="uploading || submitting"
        title="添加图片"
        @click="pick('image')"
      />
      <el-button
        :icon="VideoCamera"
        :loading="uploading"
        :disabled="uploading || submitting"
        title="添加视频"
        @click="pick('video')"
      />
      <el-button
        :icon="Headset"
        :loading="uploading"
        :disabled="uploading || submitting"
        title="添加音频"
        @click="pick('audio')"
      />
      <el-button
        :icon="Paperclip"
        :loading="uploading"
        :disabled="uploading || submitting"
        title="添加文件"
        @click="pick('file')"
      />
      <span class="composer__hint">
        <template v-if="uploading">附件上传中<template v-if="progress > 0">（{{ progress }}%）</template></template>
      </span>
      <div class="composer__actions">
        <EmojiPicker @pick="onPickEmoji" />
        <el-button
          type="primary"
          :loading="submitting || uploading"
          :disabled="uploading || submitting"
          @click="onSubmit"
        >
          {{ submitText }}
        </el-button>
      </div>
    </div>

    <input
      ref="fileInput"
      class="composer__input"
      type="file"
      multiple
      :accept="accept"
      @change="onFilesChange"
    />
  </div>
</template>

<style scoped>
.composer {
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  background: #fff;
}

.composer__reply {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
  font-size: 12px;
  color: #909399;
}

.composer__bar {
  display: flex;
  align-items: center;
  margin-top: 8px;
}

.composer__hint {
  margin-left: 8px;
  font-size: 12px;
  color: #a8abb2;
}

.composer__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.composer__input {
  display: none;
}
</style>
