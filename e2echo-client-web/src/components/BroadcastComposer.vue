<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Headset, Paperclip, Picture, VideoCamera } from '@element-plus/icons-vue'
import {
  FILE_MAX_SIZE_BYTE,
  FILE_MAX_SIZE_MESSAGE,
  PUBLIC_KEY_LENGTH,
  avatarSrc,
  uploadBroadcastFile,
} from '@/api'
import type { BroadcastFileVo } from '@/api'
import BroadcastAttachments from '@/components/BroadcastAttachments.vue'
import type { AttachmentItem } from '@/components/BroadcastAttachments.vue'
import EmojiPicker from '@/components/EmojiPicker.vue'
import FloatingUserCard from '@/components/FloatingUserCard.vue'
import { useBroadcastStore } from '@/stores/broadcast'
import { useSubscribeStore } from '@/stores/subscribe'
import { shortKey } from '@/utils/display'
import { showError } from '@/utils/feedback'
import { deleteMentionBeforeCaret, splitMentions } from '@/utils/mentions'

/**
 * 广播发布框：发帖和评论共用。
 *
 * <p>输入区与私聊消息框保持一致：@ 人员在编辑器中展示为别名标签，DOM 序列化和提交值始终是
 * {@code @公钥}。@ 候选项来自关注列表；头像快捷 @ 还能提及未关注的人。</p>
 *
 * <p>附件一律到提交时才上传：选中文件只记录并本地预览，点发布时才逐个上传拿回附件信息，再连同
 * 正文发送。这样中途放弃不会留下无人引用的文件。</p>
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
  /** 从用户头像快速 @ 时预先插入的公钥。 */
  initialMention?: string
}>()

const emit = defineEmits<{
  /** 点击发布：正文与附件。 */
  'submit': [text: string, files: BroadcastFileVo[]]
  /** 取消回复。 */
  'cancelReply': []
}>()

const broadcastStore = useBroadcastStore()
const subscribeStore = useSubscribeStore()

/** 正文原始值：始终是 {@code @公钥} 形式。 */
const text = ref('')

/** 富文本编辑区。 */
const editorRef = ref<HTMLDivElement>()

/** 已上传的附件。 */
interface PendingAttachment {
  /** 本地文件。 */
  file: File
  /** 本地预览地址，移除或卸载时释放。 */
  localUrl: string
  /** 上传结果：传过之后记下来，发布失败重试时不再重复上传。 */
  uploaded?: BroadcastFileVo
}

/** 待上传的附件。 */
const pending = ref<PendingAttachment[]>([])

/** 预览用的附件列表。 */
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

/** 文件选择框的 accept。 */
const accept = ref('')

/** 关注索引是否已准备。 */
const mentionReady = ref(false)

/** 是否正在加载关注列表。 */
const mentionLoading = ref(false)

/** 正在执行的关注索引加载任务。 */
let mentionTask: Promise<void> | null = null

/** @ 列表是否显示。 */
const mentionVisible = ref(false)

/** 当前 @ 在原始正文中的位置。 */
const mentionStart = ref(-1)

/** @ 后面的搜索词。 */
const mentionQuery = ref('')

/** 当前高亮的候选。 */
const mentionIndex = ref(0)

/** 输入法拼字期间不重写 DOM。 */
let composing = false

/** 输入法状态的可视化副本。 */
const composingActive = ref(false)

/** 最近一次写回正文的有效值。 */
let lastAcceptedValue = ''

/** 已处理的初始 @，避免重复插入。 */
let initialMentionApplied = ''

/** 输入框 @ 标签对应的用户卡片。 */
const mentionCardTarget = ref<{ uid: string; anchor: HTMLElement } | null>(null)

/** 关注者候选项。 */
interface MentionCandidate {
  /** 公钥。 */
  publicKey: string
  /** 展示名称。 */
  label: string
  /** 资料头像。 */
  avatarUrl?: string
}

/** 输入框的最小高度。 */
const minHeight = computed(() => `${(props.rows ?? 3) * 24 + 14}px`)

/** 输入框的最大高度。 */
const maxHeight = computed(() => props.autosize ? '302px' : '180px')

/**
 * 取被提及者的展示名称。
 *
 * @param publicKey 公钥
 * @returns 关注别名、资料昵称或公钥末位
 */
function mentionName(publicKey: string): string {
  return subscribeStore.aliasOf(publicKey)
    || broadcastStore.profileOf(publicKey)?.nickname
    || shortKey(publicKey, 5)
}

/**
 * 关注列表中的 @ 候选。
 */
const mentionCandidates = computed<MentionCandidate[]>(() => {
  const query = mentionQuery.value.trim().toLowerCase()
  const named = [...subscribeStore.aliases.keys()].map((publicKey) => {
    const profile = broadcastStore.profileOf(publicKey)
    return {
      publicKey,
      name: subscribeStore.aliasOf(publicKey)
        || profile?.nickname
        || shortKey(publicKey, 5),
      avatarUrl: avatarSrc(profile?.avatarUrl),
    }
  })
  const counts = new Map<string, number>()
  for (const item of named) {
    counts.set(item.name, (counts.get(item.name) ?? 0) + 1)
  }
  return named
    .map((item) => ({
      publicKey: item.publicKey,
      label: (counts.get(item.name) ?? 0) > 1
        ? `${item.name}·${shortKey(item.publicKey, 4)}`
        : item.name,
      avatarUrl: item.avatarUrl,
    }))
    .filter((item) =>
      !query
      || item.label.toLowerCase().includes(query)
      || item.publicKey.toLowerCase().includes(query))
    .slice(0, 20)
})

/** 当前是否有关注者，用来区分空列表和搜索无结果。 */
const hasFollows = computed(() => subscribeStore.aliases.size > 0)

/**
 * 准备关注列表及头像资料。
 *
 * @param loadAvatars 是否异步加载关注者资料
 */
async function prepareMentions(loadAvatars: boolean): Promise<void> {
  if (mentionTask) {
    return mentionTask
  }
  mentionTask = (async () => {
    const showLoading = !mentionReady.value
    if (showLoading) {
      mentionLoading.value = true
    }
    try {
      await subscribeStore.loadIndex()
      mentionReady.value = true
      if (loadAvatars) {
        for (const uid of subscribeStore.aliases.keys()) {
          void broadcastStore.loadProfile(uid).catch(() => null)
        }
      }
    } finally {
      if (showLoading) {
        mentionLoading.value = false
      }
    }
  })()
  try {
    await mentionTask
  } finally {
    mentionTask = null
  }
}

/**
 * 取可编辑区域。
 */
function editor(): HTMLDivElement | null {
  return editorRef.value ?? null
}

/**
 * 取一个 DOM 节点在原始正文里的长度。
 *
 * @param node DOM 节点
 * @returns 原始字符数
 */
function rawNodeLength(node: Node): number {
  if (node.nodeType === Node.TEXT_NODE) {
    return node.nodeValue?.length ?? 0
  }
  if (node.nodeType !== Node.ELEMENT_NODE) {
    return 0
  }
  const element = node as HTMLElement
  if (element.dataset.publicKey) {
    return 1 + element.dataset.publicKey.length
  }
  if (element.tagName === 'BR') {
    return 1
  }
  return Array.from(element.childNodes).reduce((sum, child) => sum + rawNodeLength(child), 0)
}

/**
 * 把编辑器节点序列化为广播正文。
 *
 * @param node DOM 节点
 * @returns {@code @公钥} 形式的正文
 */
function serializeNode(node: Node): string {
  if (node.nodeType === Node.TEXT_NODE) {
    return node.nodeValue ?? ''
  }
  if (node.nodeType !== Node.ELEMENT_NODE) {
    return ''
  }
  const element = node as HTMLElement
  if (element.dataset.publicKey) {
    return `@${element.dataset.publicKey}`
  }
  if (element.tagName === 'BR') {
    return '\n'
  }
  return Array.from(element.childNodes).map(serializeNode).join('')
}

/**
 * 读取编辑器中的原始正文。
 */
function readEditorValue(): string {
  const root = editor()
  return root ? Array.from(root.childNodes).map(serializeNode).join('') : text.value
}

/**
 * 创建一个不可拆分的 @ 别名标签。
 *
 * @param publicKey 公钥
 * @returns 标签元素
 */
function createMentionElement(publicKey: string): HTMLSpanElement {
  const element = document.createElement('span')
  element.className = 'broadcast-composer__mention'
  element.contentEditable = 'false'
  element.dataset.publicKey = publicKey
  element.title = `@${publicKey}`
  element.textContent = `@${mentionName(publicKey)}`
  element.addEventListener('mousedown', (event) => {
    event.preventDefault()
  })
  element.addEventListener('click', (event) => {
    event.preventDefault()
    event.stopPropagation()
    mentionCardTarget.value = { uid: publicKey, anchor: element }
  })
  return element
}

/**
 * 刷新已有 @ 标签的显示名称。
 */
function updateMentionLabels(): void {
  const root = editor()
  if (!root) {
    return
  }
  for (const element of root.querySelectorAll<HTMLElement>('[data-public-key]')) {
    const publicKey = element.dataset.publicKey
    if (publicKey) {
      element.textContent = `@${mentionName(publicKey)}`
    }
  }
}

/**
 * 补齐 @ 标签对应的资料。
 */
function resolveEditorMentionProfiles(): void {
  const root = editor()
  if (!root) {
    return
  }
  for (const element of root.querySelectorAll<HTMLElement>('[data-public-key]')) {
    const publicKey = element.dataset.publicKey
    if (publicKey && !broadcastStore.profileOf(publicKey)) {
      void broadcastStore.loadProfile(publicKey).catch(() => null).then(updateMentionLabels)
    }
  }
}

/**
 * 按原始正文重建编辑器内容。
 *
 * @param value       原始正文
 * @param caretOffset 重建后的光标位置
 */
function renderEditor(value: string, caretOffset?: number): void {
  const root = editor()
  if (!root) {
    return
  }
  const wasFocused = document.activeElement === root
  const fragment = document.createDocumentFragment()
  for (const segment of splitMentions(value)) {
    if (segment.kind === 'mention' && segment.publicKey) {
      fragment.append(createMentionElement(segment.publicKey))
    } else {
      fragment.append(document.createTextNode(segment.text))
    }
  }
  root.replaceChildren(fragment)
  text.value = value
  lastAcceptedValue = value
  updateMentionLabels()
  resolveEditorMentionProfiles()
  if (wasFocused) {
    setCaretAtRawOffset(root, caretOffset ?? value.length)
  }
}

/**
 * 取原始正文位置对应的 DOM 坐标。
 *
 * @param root   编辑器
 * @param target 原始正文位置
 */
function pointAtRawOffset(root: HTMLElement, target: number): { node: Node; offset: number } {
  let passed = 0
  for (const [index, child] of Array.from(root.childNodes).entries()) {
    const length = rawNodeLength(child)
    if (target <= passed + length) {
      const local = target - passed
      if (child.nodeType === Node.TEXT_NODE) {
        return { node: child, offset: local }
      }
      return { node: root, offset: index + (local === 0 ? 0 : 1) }
    }
    passed += length
  }
  return { node: root, offset: root.childNodes.length }
}

/**
 * 把光标放到原始正文位置。
 *
 * @param root   编辑器
 * @param target 原始正文位置
 */
function setCaretAtRawOffset(root: HTMLElement, target: number): void {
  const point = pointAtRawOffset(root, target)
  const range = document.createRange()
  range.setStart(point.node, point.offset)
  range.collapse(true)
  const selection = window.getSelection()
  selection?.removeAllRanges()
  selection?.addRange(range)
}

/**
 * 取当前光标在原始正文里的位置。
 */
function caretRawOffset(): number {
  const root = editor()
  const selection = window.getSelection()
  if (!root || !selection || selection.rangeCount === 0) {
    return lastAcceptedValue.length
  }
  const range = selection.getRangeAt(0)
  if (!root.contains(range.endContainer)) {
    return lastAcceptedValue.length
  }
  let offset = 0
  const visit = (node: Node): boolean => {
    if (node === range.endContainer) {
      if (node.nodeType === Node.TEXT_NODE) {
        offset += range.endOffset
      } else {
        for (const child of Array.from(node.childNodes).slice(0, range.endOffset)) {
          offset += rawNodeLength(child)
        }
      }
      return true
    }
    if (node.nodeType === Node.TEXT_NODE) {
      offset += node.nodeValue?.length ?? 0
      return false
    }
    if (node.nodeType === Node.ELEMENT_NODE) {
      const element = node as HTMLElement
      if (element.dataset.publicKey || element.tagName === 'BR') {
        offset += rawNodeLength(element)
        return false
      }
    }
    return Array.from(node.childNodes).some(visit)
  }
  visit(root)
  return offset
}

/**
 * 聚焦编辑器。
 */
function focus(): void {
  const root = editor()
  if (!root) {
    return
  }
  root.focus()
  const selection = window.getSelection()
  if (!selection || selection.rangeCount === 0 || !root.contains(selection.anchorNode)) {
    setCaretAtRawOffset(root, readEditorValue().length)
  }
}

/**
 * 同步编辑器到正文；超过上限时恢复最后一次有效内容。
 */
function syncEditor(): void {
  const value = readEditorValue()
  if (value.length > (props.maxLength ?? 500)) {
    ElMessage.warning('内容已达长度上限')
    renderEditor(lastAcceptedValue, caretRawOffset())
    return
  }
  text.value = value
  lastAcceptedValue = value
}

/**
 * 在当前光标处插入纯文本。
 *
 * @param value 待插入文字
 */
function insertTextAtCaret(value: string): void {
  const root = editor()
  if (!root) {
    return
  }
  focus()
  const selection = window.getSelection()
  if (!selection || selection.rangeCount === 0) {
    return
  }
  const range = selection.getRangeAt(0)
  range.deleteContents()
  const node = document.createTextNode(value)
  range.insertNode(node)
  range.setStartAfter(node)
  range.collapse(true)
  selection.removeAllRanges()
  selection.addRange(range)
  syncEditor()
}

/**
 * 通过公钥插入 @ 标签。
 *
 * @param publicKey 被提及者公钥
 */
async function insertMention(publicKey: string): Promise<void> {
  await prepareMentions(false).catch(() => {})
  await broadcastStore.loadProfile(publicKey).catch(() => null)
  const root = editor()
  if (!root) {
    return
  }
  focus()
  const selection = window.getSelection()
  if (!selection || selection.rangeCount === 0) {
    return
  }
  const range = selection.getRangeAt(0)
  range.deleteContents()
  const mention = createMentionElement(publicKey)
  range.insertNode(mention)
  const space = document.createTextNode(' ')
  mention.parentNode?.insertBefore(space, mention.nextSibling)
  range.setStartAfter(space)
  range.collapse(true)
  selection.removeAllRanges()
  selection.addRange(range)
  syncEditor()
  void nextTick(() => root.focus())
}

/**
 * 从用户卡片再次 @ 这位用户。
 *
 * @param uid 用户公钥
 */
function mentionFromCard(uid: string): void {
  mentionCardTarget.value = null
  void insertMention(uid)
}

/**
 * 判断光标前是否正在输入 @ 搜索词。
 *
 * @param value 原始正文
 * @param caret 光标位置
 */
function activeMentionAt(value: string, caret: number): { start: number; query: string } | null {
  const before = value.slice(0, caret)
  const start = before.lastIndexOf('@')
  if (start < 0) {
    return null
  }
  const previous = before[start - 1]
  if (previous && /[A-Za-z0-9._]/.test(previous)) {
    return null
  }
  const query = before.slice(start + 1)
  if (/[\s@]/.test(query) || query.length >= PUBLIC_KEY_LENGTH) {
    return null
  }
  return { start, query }
}

/**
 * 关闭 @ 列表。
 */
function closeMentions(): void {
  mentionVisible.value = false
  mentionStart.value = -1
  mentionQuery.value = ''
  mentionIndex.value = 0
}

/**
 * 刷新 @ 列表状态。
 */
function refreshMentions(): void {
  const root = editor()
  if (!root) {
    closeMentions()
    return
  }
  const active = activeMentionAt(readEditorValue(), caretRawOffset())
  if (!active) {
    closeMentions()
    return
  }
  mentionStart.value = active.start
  mentionQuery.value = active.query
  mentionVisible.value = true
  mentionIndex.value = 0
  void prepareMentions(true).catch(() => {})
}

/**
 * 选中关注者并替换当前 @ 搜索词。
 *
 * @param candidate 关注者
 */
function pickMention(candidate: MentionCandidate): void {
  const root = editor()
  if (!root || mentionStart.value < 0) {
    return
  }
  const value = readEditorValue()
  const end = Math.min(value.length, mentionStart.value + 1 + mentionQuery.value.length)
  const startPoint = pointAtRawOffset(root, mentionStart.value)
  const endPoint = pointAtRawOffset(root, end)
  const range = document.createRange()
  range.setStart(startPoint.node, startPoint.offset)
  range.setEnd(endPoint.node, endPoint.offset)
  range.deleteContents()
  const mention = createMentionElement(candidate.publicKey)
  range.insertNode(mention)
  const space = document.createTextNode(' ')
  mention.parentNode?.insertBefore(space, mention.nextSibling)
  range.setStartAfter(space)
  range.collapse(true)
  const selection = window.getSelection()
  selection?.removeAllRanges()
  selection?.addRange(range)
  closeMentions()
  syncEditor()
  void nextTick(() => root.focus())
}

/**
 * 普通文字里出现完整 @公钥时立即转成别名标签。
 */
function renderUnformattedMentions(): void {
  const root = editor()
  if (!root) {
    return
  }
  const value = readEditorValue()
  const expected = splitMentions(value).filter((segment) => segment.kind === 'mention').length
  const existing = root.querySelectorAll('[data-public-key]').length
  if (expected > existing) {
    renderEditor(value, caretRawOffset())
  }
}

/**
 * 输入区内容变化。
 */
function onEditorInput(): void {
  if (composing) {
    return
  }
  syncEditor()
  renderUnformattedMentions()
  void nextTick(refreshMentions)
}

/**
 * 开始输入法拼字。
 */
function onCompositionStart(): void {
  composing = true
  composingActive.value = true
  closeMentions()
}

/**
 * 输入法拼字完成。
 */
function onCompositionEnd(): void {
  composing = false
  composingActive.value = false
  syncEditor()
  void nextTick(refreshMentions)
}

/**
 * 键盘操作：@ 列表打开时选择候选，否则 Enter 插入换行。
 *
 * @param event 键盘事件
 */
function onKeydown(event: KeyboardEvent): void {
  if (composing || event.isComposing || event.keyCode === 229) {
    return
  }
  const root = editor()
  if (event.key === 'Backspace' && root && deleteMentionBeforeCaret(root)) {
    event.preventDefault()
    syncEditor()
    refreshMentions()
    return
  }
  if (mentionVisible.value) {
    if (event.key === 'Escape') {
      event.preventDefault()
      closeMentions()
      return
    }
    if ((event.key === 'ArrowDown' || event.key === 'ArrowUp')
      && mentionCandidates.value.length > 0) {
      event.preventDefault()
      const offset = event.key === 'ArrowDown' ? 1 : -1
      const length = mentionCandidates.value.length
      mentionIndex.value = (mentionIndex.value + offset + length) % length
      return
    }
    if ((event.key === 'Enter' || event.key === 'Tab')
      && mentionCandidates.value.length > 0) {
      event.preventDefault()
      const candidate = mentionCandidates.value[mentionIndex.value]
      if (candidate) {
        pickMention(candidate)
      }
      return
    }
  }
  if (event.key === 'Enter') {
    event.preventDefault()
    insertTextAtCaret('\n')
  }
}

/**
 * 编辑区按键结束：清理空节点并刷新 @ 状态。
 */
function onKeyup(): void {
  const root = editor()
  if (root && document.activeElement === root && readEditorValue().length === 0) {
    root.replaceChildren()
    root.focus()
    setCaretAtRawOffset(root, 0)
  }
  refreshMentions()
}

/**
 * 通过工具栏的 @ 按钮开始提及。
 */
function onMentionButton(): void {
  insertTextAtCaret('@')
  void nextTick(() => {
    focus()
    refreshMentions()
  })
}

/**
 * 粘贴时只接收纯文本。
 *
 * @param event 粘贴事件
 */
function onPaste(event: ClipboardEvent): void {
  event.preventDefault()
  const value = event.clipboardData?.getData('text/plain') ?? ''
  if (value) {
    insertTextAtCaret(value)
    renderEditor(readEditorValue(), caretRawOffset())
    void nextTick(refreshMentions)
  }
}

/**
 * 拖入时只接收纯文本。
 *
 * @param event 拖放事件
 */
function onDrop(event: DragEvent): void {
  event.preventDefault()
  const value = event.dataTransfer?.getData('text/plain') ?? ''
  if (value) {
    insertTextAtCaret(value)
    renderEditor(readEditorValue(), caretRawOffset())
    void nextTick(refreshMentions)
  }
}

/**
 * 按类型打开文件选择框。
 *
 * @param kind 附件类型
 */
function pick(kind: 'image' | 'video' | 'audio' | 'file'): void {
  accept.value = kind === 'file' ? '' : `${kind}/*`
  void nextTick(() => fileInput.value?.click())
}

/**
 * 选中文件。
 *
 * @param event 选择框事件
 */
function onFilesChange(event: Event): void {
  const input = event.target as HTMLInputElement
  const picked = Array.from(input.files ?? [])
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
 * 移除待上传附件。
 *
 * @param file 附件
 */
function removeFile(file: AttachmentItem): void {
  const target = pending.value.find((item) => item.localUrl === file.localUrl)
  if (target) {
    URL.revokeObjectURL(target.localUrl)
  }
  pending.value = pending.value.filter((item) => item.localUrl !== file.localUrl)
}

/**
 * 发布：先上传附件，再提交原始正文。
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

  const uploaded: BroadcastFileVo[] = []
  if (pending.value.length > 0) {
    uploading.value = true
    try {
      for (const item of pending.value) {
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
 * 插入表情。
 *
 * @param emoji 表情
 */
function onPickEmoji(emoji: string): void {
  insertTextAtCaret(emoji)
}

/**
 * 清空输入。
 */
function clear(): void {
  text.value = ''
  lastAcceptedValue = ''
  editor()?.replaceChildren()
  closeMentions()
  releasePending()
}

/**
 * 释放附件预览地址。
 */
function releasePending(): void {
  for (const item of pending.value) {
    URL.revokeObjectURL(item.localUrl)
  }
  pending.value = []
}

onMounted(() => {
  renderEditor('')
  void prepareMentions(false).catch(() => {})
})

onUnmounted(releasePending)

watch(() => props.initialMention, async (uid) => {
  if (!uid || uid === initialMentionApplied) {
    return
  }
  initialMentionApplied = uid
  await nextTick()
  await insertMention(uid)
}, { immediate: true })

watch(
  [() => subscribeStore.aliases, () => broadcastStore.profiles],
  () => {
    updateMentionLabels()
    resolveEditorMentionProfiles()
  },
  { deep: true },
)

defineExpose({ clear, focus, insertMention })
</script>

<template>
  <div class="composer">
    <div v-if="replyToName" class="composer__reply">
      回复 @{{ replyToName }}
      <el-button text size="small" :icon="Close" @click="emit('cancelReply')" />
    </div>

    <div class="composer__editor-wrap">
      <div
        ref="editorRef"
        class="composer__editor"
        :style="{ minHeight, maxHeight }"
        contenteditable="true"
        role="textbox"
        aria-multiline="true"
        :aria-label="placeholder"
        @input="onEditorInput"
        @keydown="onKeydown"
        @keyup="onKeyup"
        @click="refreshMentions"
        @focus="refreshMentions"
        @blur="closeMentions"
        @compositionstart="onCompositionStart"
        @compositionend="onCompositionEnd"
        @paste="onPaste"
        @drop="onDrop"
      ></div>

      <span v-if="text.length === 0 && !composingActive" class="composer__placeholder">
        {{ placeholder }}
      </span>

      <div v-if="mentionVisible" class="composer__mention-picker">
        <div class="composer__mention-title">提及关注的人</div>

        <el-skeleton v-if="mentionLoading && mentionCandidates.length === 0" :rows="2" animated />

        <div v-else-if="mentionCandidates.length > 0" class="composer__mention-list">
          <button
            v-for="(candidate, index) in mentionCandidates"
            :key="candidate.publicKey"
            type="button"
            class="composer__mention-option"
            :class="{ 'composer__mention-option--active': index === mentionIndex }"
            @mousedown.prevent
            @mouseenter="mentionIndex = index"
            @click="pickMention(candidate)"
          >
            <el-avatar :size="24" :src="candidate.avatarUrl">
              {{ candidate.label.charAt(0) }}
            </el-avatar>
            <span class="composer__mention-name">@{{ candidate.label }}</span>
            <span class="composer__mention-key">{{ shortKey(candidate.publicKey, 8) }}</span>
          </button>
        </div>

        <div v-else class="composer__mention-empty">
          {{ mentionReady
            ? (hasFollows ? '没有匹配的关注者' : '还没有关注的人')
            : '正在加载关注列表…' }}
        </div>
      </div>
    </div>

    <BroadcastAttachments :files="previewFiles" removable @remove="removeFile" />

    <div class="composer__bar">
      <el-button
        :disabled="uploading || submitting"
        title="提及关注的人"
        @click="onMentionButton"
      >
        @
      </el-button>
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
          :disabled="uploading || submitting || text.trim().length === 0"
          @click="onSubmit"
        >
          {{ submitText }}
        </el-button>
      </div>
    </div>

    <input
      ref="fileInput"
      class="composer__file"
      type="file"
      multiple
      :accept="accept"
      @change="onFilesChange"
    />

    <FloatingUserCard
      v-if="mentionCardTarget"
      :uid="mentionCardTarget.uid"
      :anchor="mentionCardTarget.anchor"
      :preferred-name="mentionName(mentionCardTarget.uid)"
      mentionable
      show-profile
      :conversation-actions="false"
      @mention="mentionFromCard"
      @close="mentionCardTarget = null"
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

.composer__editor-wrap {
  position: relative;
}

.composer__editor {
  width: 100%;
  padding: 6px 11px;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  outline: none;
  background: #fff;
  font: inherit;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.composer__editor:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 1px #409eff inset;
}

.composer__placeholder {
  position: absolute;
  top: 7px;
  left: 12px;
  color: #a8abb2;
  font-size: 14px;
  line-height: 1.5;
  pointer-events: none;
}

:deep(.broadcast-composer__mention) {
  display: inline;
  padding: 1px 4px;
  border-radius: 3px;
  background: #ecf5ff;
  color: #337ecc;
  white-space: nowrap;
  cursor: pointer;
}

:deep(.broadcast-composer__mention:hover),
:deep(.broadcast-composer__mention:focus-visible) {
  text-decoration: underline;
}

.composer__mention-picker {
  position: absolute;
  z-index: 30;
  bottom: calc(100% + 8px);
  left: 0;
  width: min(380px, 100%);
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14);
}

.composer__mention-title {
  padding: 2px 8px 6px;
  font-size: 12px;
  color: #909399;
}

.composer__mention-list {
  max-height: 260px;
  overflow-y: auto;
}

.composer__mention-option {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #303133;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.composer__mention-option:hover,
.composer__mention-option--active {
  background: #f0f7ff;
}

.composer__mention-name {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.composer__mention-key {
  flex: none;
  margin-left: auto;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  color: #a8abb2;
}

.composer__mention-empty {
  padding: 18px 8px;
  font-size: 13px;
  color: #909399;
  text-align: center;
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

.composer__file {
  display: none;
}
</style>
