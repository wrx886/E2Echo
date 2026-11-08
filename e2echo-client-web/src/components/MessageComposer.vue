<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { Close, Headset, Paperclip, Picture, VideoCamera } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  FILE_MAX_SIZE_BYTE,
  FILE_MAX_SIZE_MESSAGE,
  PUBLIC_KEY_LENGTH,
  listMessageFroms,
  messagePreviewText,
} from '@/api'
import type { MessageFromDto, MessageVo } from '@/api'
import EmojiPicker from '@/components/EmojiPicker.vue'
import { useConversationStore } from '@/stores/conversation'
import { useUserStore } from '@/stores/user'
import { shortKey } from '@/utils/display'
import { splitMentions } from '@/utils/mentions'

/**
 * 消息输入区。
 *
 * <p>Enter 发送、Shift + Enter 换行；正文由父组件持有（v-model），发送成功后由父组件清空。@
 * 人员在编辑器里展示为别名标签，序列化后仍是完整公钥；点击标签可以查看对应的用户卡片。文件也在
 * 这里选：选中后交给父组件上传发送，本组件只负责选文件与显示进度。表情同样是本组件负责插到
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
  /** 当前会话对方：私聊时为对方公钥、群聊时为群聊标识。 */
  peer: string
  /** 当前会话展示名称，用于私聊里还没消息时直接提及对方。 */
  peerName: string
  /** 是否启用 @ 提及；目前只在私聊里启用。 */
  mentionEnabled: boolean
  /** 当前正在引用的消息；为空表示不引用。 */
  quotedMessage?: MessageVo | null
}>()

/** 富文本输入区：普通文字可编辑，@ 人员是不可拆分的标签。 */
const editorRef = ref<HTMLDivElement>()

const conversationStore = useConversationStore()
const userStore = useUserStore()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'submit': []
  'cancelQuote': []
  'mentionClick': [target: { uid: string; anchor: HTMLElement }]
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

/** @ 人员搜索结果最多展示几条。 */
const MAX_MENTION_OPTIONS = 8

/** @ 搜索防抖时间。 */
const MENTION_SEARCH_DELAY = 200

/** @ 人员列表每次从接口取多少条。 */
const MENTION_PAGE_SIZE = 20

/** 输入框里的 @ 人员候选项。 */
interface MentionCandidate {
  /** 写入正文的公钥。 */
  publicKey: string
  /** 列表里展示的名称。 */
  label: string
}

/** 当前正在输入的 @ 片段。 */
interface ActiveMention {
  /** @ 在正文中的位置。 */
  start: number
  /** @ 后面的搜索词。 */
  query: string
}

/** 是否显示 @ 人员列表。 */
const mentionVisible = ref(false)

/** @ 在正文中的起始位置。 */
const mentionStart = ref(-1)

/** @ 后面的搜索词。 */
const mentionQuery = ref('')

/** 接口返回的发送者。 */
const mentionSources = ref<MessageFromDto[]>([])

/** 当前高亮的候选项。 */
const mentionIndex = ref(0)

/** 是否正在查询可提及人员。 */
const mentionLoading = ref(false)

/** 查询失败时显示简短提示。 */
const mentionFailed = ref(false)

/** @ 搜索防抖定时器。 */
let mentionTimer: ReturnType<typeof setTimeout> | null = null

/** 防止旧搜索结果覆盖新关键词的结果。 */
let mentionRequestToken = 0

/** Escape 关闭列表后，忽略紧随其后的 keyup，避免同一段 @ 立刻又打开。 */
let ignoreNextMentionKeyup = false

/** 最后一次写回父组件的正文，用来区分用户输入和外部清空/替换。 */
let lastAcceptedValue = props.modelValue

/** 输入法正在拼字时，不改写编辑器 DOM。 */
let composing = false

/** 输入法状态的可视化副本，拼字期间暂时隐藏占位符。 */
const composingActive = ref(false)

/** 输入框提示文字。 */
const placeholder = computed(() => props.mentionEnabled
  ? '输入消息，输入 @ 提及对方，Enter 发送，Shift + Enter 换行'
  : '输入消息，Enter 发送，Shift + Enter 换行')

/** 被引用消息的发送者名称。 */
const quotedName = computed(() => {
  const target = props.quotedMessage
  return target ? conversationStore.aliasOf(target.from) ?? shortKey(target.from, 8) : ''
})

/** 被引用消息的一行摘要，保留 @ 公钥分段以便展示别名。 */
const quotedSegments = computed(() =>
  props.quotedMessage ? splitMentions(messagePreviewText(props.quotedMessage)) : [])

/** 引用摘要的完整悬停提示。 */
const quotedTitle = computed(() => quotedSegments.value.map((segment) => segment.text).join(''))

/**
 * 取可编辑区域。
 *
 * @returns 编辑器元素，尚未渲染时返回 null
 */
function editor(): HTMLDivElement | null {
  return editorRef.value ?? null
}

/**
 * 取 @ 标签在正文里对应的长度。
 *
 * @param element @ 标签
 * @returns {@code @公钥} 的字符数
 */
function mentionTokenLength(element: HTMLElement): number {
  return 1 + (element.dataset.publicKey?.length ?? 0)
}

/**
 * 取一个 DOM 节点在原始正文里的长度。
 *
 * @param node 节点
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
    return mentionTokenLength(element)
  }
  if (element.tagName === 'BR') {
    return 1
  }
  return Array.from(element.childNodes).reduce((sum, child) => sum + rawNodeLength(child), 0)
}

/**
 * 把编辑器 DOM 序列化成发送用的正文。
 *
 * <p>@ 标签的显示文字是别名，但这里始终读 {@code data-public-key}，所以父组件拿到的是
 * {@code @完整公钥}。</p>
 *
 * @param node 待序列化的节点
 * @returns 原始正文
 */
function serializeNode(node: Node): string {
  if (node.nodeType === Node.TEXT_NODE) {
    return node.nodeValue ?? ''
  }
  if (node.nodeType !== Node.ELEMENT_NODE) {
    return ''
  }
  const element = node as HTMLElement
  const publicKey = element.dataset.publicKey
  if (publicKey) {
    return `@${publicKey}`
  }
  if (element.tagName === 'BR') {
    return '\n'
  }
  return Array.from(element.childNodes).map(serializeNode).join('')
}

/**
 * 读取编辑器当前的原始正文。
 *
 * @returns {@code @公钥} 形式的正文
 */
function readEditorValue(): string {
  const root = editor()
  return root ? Array.from(root.childNodes).map(serializeNode).join('') : props.modelValue
}

/**
 * 取 @ 人员在编辑器里展示的名称。
 *
 * @param publicKey 公钥
 * @returns 别名；没有别名时退回公钥末位
 */
function mentionDisplayName(publicKey: string): string {
  return conversationStore.aliasOf(publicKey) ?? shortKey(publicKey, 8)
}

/**
 * 创建一个不可拆分的 @ 人员标签。
 *
 * @param publicKey 写入原始正文的公钥
 * @returns 标签元素
 */
function createMentionElement(publicKey: string): HTMLSpanElement {
  const element = document.createElement('span')
  element.className = 'composer__mention'
  element.contentEditable = 'false'
  element.dataset.publicKey = publicKey
  element.title = `@${publicKey}`
  element.tabIndex = 0
  element.setAttribute('role', 'button')
  element.textContent = `@${mentionDisplayName(publicKey)}`
  element.addEventListener('mousedown', (event) => {
    // 点击标签只打开用户卡片，不移动编辑器里的正文光标
    event.preventDefault()
  })
  element.addEventListener('click', (event) => {
    event.preventDefault()
    event.stopPropagation()
    emit('mentionClick', { uid: publicKey, anchor: element })
  })
  element.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      event.stopPropagation()
      emit('mentionClick', { uid: publicKey, anchor: element })
    }
  })
  return element
}

/**
 * 刷新已有 @ 标签的别名，不重建 DOM，因此不会移动光标。
 */
function updateMentionLabels(): void {
  const root = editor()
  if (!root) {
    return
  }
  for (const element of root.querySelectorAll<HTMLElement>('[data-public-key]')) {
    const publicKey = element.dataset.publicKey
    if (publicKey) {
      element.textContent = `@${mentionDisplayName(publicKey)}`
    }
  }
}

/**
 * 补齐正文里 @ 标签的别名。
 */
function resolveEditorMentionAliases(): void {
  const root = editor()
  if (!root) {
    return
  }
  for (const element of root.querySelectorAll<HTMLElement>('[data-public-key]')) {
    const publicKey = element.dataset.publicKey
    if (!publicKey || conversationStore.aliasOf(publicKey)) {
      continue
    }
    void conversationStore.resolveAlias(publicKey).then(updateMentionLabels)
  }
}

/**
 * 按原始正文重建编辑器内容。
 *
 * @param value 原始正文
 * @param caretOffset 重建后要恢复的原始正文光标位置；不传则保持编辑器失焦
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
  lastAcceptedValue = value
  updateMentionLabels()
  resolveEditorMentionAliases()

  if (wasFocused) {
    setCaretAtRawOffset(root, caretOffset ?? value.length)
  }
}

/**
 * 取编辑器里一个原始字符位置对应的 DOM 坐标。
 *
 * @param root 编辑器
 * @param target 原始正文位置
 * @returns 可放进 Range 的节点与偏移
 */
function pointAtRawOffset(root: HTMLElement, target: number): { node: Node; offset: number } {
  let passed = 0
  const children = Array.from(root.childNodes)
  for (const [index, child] of children.entries()) {
    const length = rawNodeLength(child)
    if (target <= passed + length) {
      const local = target - passed
      if (child.nodeType === Node.TEXT_NODE) {
        return { node: child, offset: local }
      }
      if (child.nodeType === Node.ELEMENT_NODE) {
        return { node: root, offset: index + (local === 0 ? 0 : 1) }
      }
    }
    passed += length
  }
  return { node: root, offset: root.childNodes.length }
}

/**
 * 把光标放到原始正文的指定位置。
 *
 * @param root 编辑器
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
 *
 * @returns 原始字符位置；编辑器没有选区时返回末尾
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
        const children = Array.from(node.childNodes)
        for (const child of children.slice(0, range.endOffset)) {
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
    for (const child of Array.from(node.childNodes)) {
      if (visit(child)) {
        return true
      }
    }
    return false
  }
  visit(root)
  return offset
}

/**
 * 确保编辑器获得焦点，并在没有选区时把光标放到末尾。
 */
function focusEditor(): void {
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
 * 在当前光标处插入纯文本。
 *
 * @param value 待插入文字
 */
function insertTextAtCaret(value: string): void {
  const root = editor()
  if (!root) {
    return
  }
  focusEditor()
  const selection = window.getSelection()
  if (!selection || selection.rangeCount === 0) {
    return
  }
  const range = selection.getRangeAt(0)
  range.deleteContents()
  const text = document.createTextNode(value)
  range.insertNode(text)
  range.setStartAfter(text)
  range.collapse(true)
  selection.removeAllRanges()
  selection.addRange(range)
  syncEditorToParent()
}

/**
 * 把编辑器内容写回父组件；超过长度上限时恢复到最后一次有效内容。
 */
function syncEditorToParent(): void {
  const value = readEditorValue()
  if (value.length > MAX_LENGTH) {
    ElMessage.warning(MAX_LENGTH_MESSAGE)
    renderEditor(lastAcceptedValue, caretRawOffset())
    return
  }
  lastAcceptedValue = value
  emit('update:modelValue', value)
}

/**
 * 判断候选项是否匹配当前 @ 搜索词。
 *
 * @param candidate 候选人
 * @returns 匹配时返回 true
 */
function matchesMention(candidate: MentionCandidate): boolean {
  const query = mentionQuery.value.trim().toLowerCase()
  if (query.length === 0) {
    return true
  }
  return candidate.publicKey.toLowerCase().includes(query)
    || candidate.label.toLowerCase().includes(query)
}

/**
 * 合并接口结果与当前私聊对方，生成人员列表。
 *
 * <p>私聊还没发过消息时 listFrom 可能为空，因此始终保留当前会话对方作为兜底候选；自己不会出现在
 * 可提及名单里。</p>
 */
const mentionCandidates = computed<MentionCandidate[]>(() => {
  if (!props.mentionEnabled || !props.peer) {
    return []
  }

  const aliases = new Map<string, string | null>()
  for (const item of mentionSources.value) {
    aliases.set(item.publicKey, item.alias)
  }

  const keys = [props.peer, ...mentionSources.value.map((item) => item.publicKey)]
  const seen = new Set<string>()
  const result: MentionCandidate[] = []

  for (const key of keys) {
    if (!key || key === userStore.publicKey || seen.has(key)) {
      continue
    }
    seen.add(key)
    const alias = aliases.get(key)?.trim() ?? ''
    const fallback = key === props.peer ? props.peerName.trim() : ''
    const candidate: MentionCandidate = {
      publicKey: key,
      label: alias || fallback || shortKey(key, 8),
    }
    if (matchesMention(candidate)) {
      result.push(candidate)
      if (result.length >= MAX_MENTION_OPTIONS) {
        break
      }
    }
  }
  return result
})

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
  if (!editor()) {
    // 拿不到原生输入框（实际不会发生）就退化成追加到末尾
    appendEmoji(emoji)
    return
  }

  insertTextAtCaret(emoji)
  void nextTick(refreshMentionState)
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
  const value = props.modelValue + emoji
  lastAcceptedValue = value
  emit('update:modelValue', value)
}

/**
 * 判断光标前是否正在输入一个 @ 搜索词。
 *
 * @param value 输入框正文
 * @param caret 光标位置
 * @returns @ 片段；不在 @ 输入状态时返回 null
 */
function activeMentionAt(value: string, caret: number): ActiveMention | null {
  const before = value.slice(0, caret)
  const start = before.lastIndexOf('@')
  if (start < 0) {
    return null
  }

  // 邮箱地址里的 @ 不触发人员列表
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
 * 关闭 @ 人员列表并取消尚未执行的搜索。
 */
function closeMention(): void {
  mentionVisible.value = false
  mentionStart.value = -1
  mentionQuery.value = ''
  mentionFailed.value = false
  mentionIndex.value = 0
  if (mentionTimer !== null) {
    clearTimeout(mentionTimer)
    mentionTimer = null
  }
}

/**
 * 查询当前会话里可提及的人员。
 *
 * @param query 公钥或别名搜索词
 */
async function loadMentionCandidates(query: string): Promise<void> {
  const requestToken = ++mentionRequestToken
  mentionLoading.value = true
  mentionFailed.value = false
  try {
    const page = await listMessageFroms(props.peer, query, 1, MENTION_PAGE_SIZE)
    if (requestToken !== mentionRequestToken || !mentionVisible.value) {
      return
    }
    mentionSources.value = page.content
    for (const item of page.content) {
      if (item.alias?.trim()) {
        conversationStore.rememberAlias(item.publicKey, item.alias)
      }
    }
  } catch {
    if (requestToken === mentionRequestToken && mentionVisible.value) {
      mentionSources.value = []
      mentionFailed.value = true
    }
  } finally {
    if (requestToken === mentionRequestToken) {
      mentionLoading.value = false
    }
  }
}

/**
 * 按 @ 搜索词重新拉取人员列表，连续输入时只请求最后一次。
 *
 * @param query 搜索词
 */
function scheduleMentionSearch(query: string): void {
  if (mentionTimer !== null) {
    clearTimeout(mentionTimer)
  }
  mentionTimer = setTimeout(() => {
    mentionTimer = null
    void loadMentionCandidates(query)
  }, MENTION_SEARCH_DELAY)
}

/**
 * 根据输入框当前光标更新 @ 人员列表。
 */
function refreshMentionState(): void {
  if (ignoreNextMentionKeyup) {
    ignoreNextMentionKeyup = false
    return
  }
  if (!props.mentionEnabled) {
    closeMention()
    return
  }
  const root = editor()
  if (!root) {
    closeMention()
    return
  }

  const value = readEditorValue()
  const active = activeMentionAt(value, caretRawOffset())
  if (!active) {
    closeMention()
    return
  }

  const changed = !mentionVisible.value
    || mentionStart.value !== active.start
    || mentionQuery.value !== active.query
  mentionStart.value = active.start
  mentionQuery.value = active.query
  mentionVisible.value = true
  mentionIndex.value = 0

  if (changed) {
    scheduleMentionSearch(active.query)
  }
}

/**
 * 编辑器内容变化：把 DOM 序列化为 {@code @公钥} 正文，并检查是否在输入 @ 搜索词。
 */
function onEditorInput(): void {
  if (composing) {
    return
  }
  ignoreNextMentionKeyup = false
  syncEditorToParent()
  renderUnformattedMentions()
  void nextTick(refreshMentionState)
}

/**
 * 开始输入法拼字：拼字期间保持浏览器自己的 DOM 与光标。
 */
function onCompositionStart(): void {
  composing = true
  composingActive.value = true
  closeMention()
}

/**
 * 输入法拼字完成后再同步正文。
 */
function onCompositionEnd(): void {
  composing = false
  composingActive.value = false
  syncEditorToParent()
  renderUnformattedMentions()
  void nextTick(refreshMentionState)
}

/**
 * 粘贴时只接收纯文本，避免外部网页的 HTML 把编辑器结构带进来。
 *
 * @param event 粘贴事件
 */
function onPaste(event: ClipboardEvent): void {
  event.preventDefault()
  if (composing) {
    return
  }
  const text = event.clipboardData?.getData('text/plain') ?? ''
  if (!text) {
    return
  }
  insertTextAtCaret(text)
  const caret = caretRawOffset()
  // 粘贴内容里若已有完整 @公钥，也立即转成 @别名 标签
  renderEditor(readEditorValue(), caret)
  void nextTick(refreshMentionState)
}

/**
 * 选中一个 @ 人员：正文只写入公钥，界面展示由消息气泡负责。
 *
 * @param candidate 选中的人员
 */
function onPickMention(candidate: MentionCandidate): void {
  if (mentionStart.value < 0) {
    return
  }

  const root = editor()
  if (!root) {
    return
  }
  const start = mentionStart.value
  const value = readEditorValue()
  const end = Math.min(value.length, start + 1 + mentionQuery.value.length)
  const nextCharacter = value[end] ?? ''
  const suffix = !nextCharacter || !/\s/.test(nextCharacter) ? ' ' : ''

  focusEditor()
  const startPoint = pointAtRawOffset(root, start)
  const endPoint = pointAtRawOffset(root, end)
  const range = document.createRange()
  range.setStart(startPoint.node, startPoint.offset)
  range.setEnd(endPoint.node, endPoint.offset)
  range.deleteContents()

  const mention = createMentionElement(candidate.publicKey)
  range.insertNode(mention)
  let caretNode: Node = mention
  if (suffix) {
    const space = document.createTextNode(suffix)
    mention.parentNode?.insertBefore(space, mention.nextSibling)
    caretNode = space
  }

  range.setStartAfter(caretNode)
  range.collapse(true)
  const selection = window.getSelection()
  selection?.removeAllRanges()
  selection?.addRange(range)

  closeMention()
  syncEditorToParent()
  void nextTick(() => {
    root.focus()
    updateMentionLabels()
  })
}

/**
 * 通过工具栏的 @ 按钮在当前光标处插入提及符。
 */
function onMentionButton(): void {
  if (!props.mentionEnabled || !editor()) {
    return
  }
  insertTextAtCaret('@')
  void nextTick(() => {
    focusEditor()
    refreshMentionState()
  })
}

/**
 * 编辑器失焦：关闭 @ 人员列表。
 */
function onEditorBlur(): void {
  closeMention()
}

/**
 * 判断编辑器当前是否为空。
 *
 * @returns 没有任何可发送文字时返回 true
 */
function editorIsEmpty(): boolean {
  return readEditorValue().length === 0
}

/**
 * 把当前 DOM 内容规整成普通文字与 @ 标签。
 */
function normalizeEditor(): void {
  const caret = caretRawOffset()
  renderEditor(readEditorValue(), caret)
}

/**
 * 普通文字里如果出现完整的 {@code @公钥}，立即转成 @别名 标签。
 *
 * <p>主要用于粘贴、恢复草稿或用户手动输入完整公钥的情况。</p>
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
 * 修正浏览器在回车、删除后留下的空标签，并把光标留在末尾。
 */
function cleanupEditor(): void {
  const root = editor()
  if (!root) {
    return
  }
  if (readEditorValue().length === 0) {
    root.replaceChildren()
    root.focus()
    setCaretAtRawOffset(root, 0)
  }
}

/**
 * 输入框获得焦点时保持原有选区。
 */
function onEditorFocus(): void {
  if (!editorIsEmpty()) {
    return
  }
  const root = editor()
  if (root) {
    setCaretAtRawOffset(root, 0)
  }
}

/**
 * 删除 @ 标签时同步正文；浏览器不同实现会在删除后保留空文本节点。
 */
function onEditorKeyup(): void {
  cleanupEditor()
  refreshMentionState()
}

/**
 * 阻止拖放外部 HTML，拖入的文字仍按纯文本处理。
 *
 * @param event 拖放事件
 */
function onDrop(event: DragEvent): void {
  event.preventDefault()
  const text = event.dataTransfer?.getData('text/plain') ?? ''
  if (text) {
    insertTextAtCaret(text)
    normalizeEditor()
    void nextTick(refreshMentionState)
  }
}

/**
 * 把编辑器中的纯文本复制出去时，仍保留原始 @公钥。
 *
 * @param event 复制事件
 */
function onCopy(event: ClipboardEvent): void {
  const selection = window.getSelection()
  const root = editor()
  if (!selection || !root || selection.rangeCount === 0) {
    return
  }
  const range = selection.getRangeAt(0)
  if (!root.contains(range.commonAncestorContainer)) {
    return
  }
  const clone = range.cloneContents()
  const text = Array.from(clone.childNodes).map(serializeNode).join('')
  if (text.length === 0) {
    return
  }
  event.preventDefault()
  event.clipboardData?.setData('text/plain', text)
}

/**
 * 鼠标改变光标位置后，重新判断是否处于 @ 搜索状态。
 */
function onEditorClick(): void {
  refreshMentionState()
}

/**
 * 处理浏览器的撤销/重做：DOM 已经变化，但不会触发 input。
 */
function onEditorBeforeInput(event: InputEvent): void {
  if (event.inputType === 'historyUndo' || event.inputType === 'historyRedo') {
    setTimeout(() => {
      normalizeEditor()
      syncEditorToParent()
      refreshMentionState()
    })
  }
}

/**
 * 初始化编辑器，并补齐已有草稿中的 @ 别名。
 */
onMounted(() => {
  renderEditor(props.modelValue)
})

/**
 * 聚焦输入区：选择引用后由聊天页调用。
 */
function focus(): void {
  focusEditor()
}

defineExpose({ focus })

// 引用摘要里的发送者与 @ 人员也补齐别名
watch(
  () => [
    props.quotedMessage?.from ?? '',
    ...quotedSegments.value
      .filter((segment) => segment.kind === 'mention' && segment.publicKey)
      .map((segment) => segment.publicKey as string),
  ],
  (keys) => {
    for (const key of keys) {
      if (key && !conversationStore.aliasOf(key)) {
        void conversationStore.resolveAlias(key)
      }
    }
  },
  { immediate: true },
)

/**
 * 编辑器或父组件内容变化时，确保标签文字与原始正文一致。
 */
watch(() => conversationStore.aliasCache, updateMentionLabels, { deep: true })

/**
 * 父组件替换或清空正文时重建编辑器；自己同步出去的正文不重建，以免光标跳动。
 */
watch(() => props.modelValue, (value) => {
  if (value !== lastAcceptedValue || readEditorValue() !== value) {
    renderEditor(value)
  }
})

/**
 * 剪切时把原始 @公钥 写入剪贴板，并删除当前选区。
 *
 * @param event 剪切事件
 */
function onCut(event: ClipboardEvent): void {
  const selection = window.getSelection()
  const root = editor()
  if (!selection || !root || selection.rangeCount === 0) {
    return
  }
  const range = selection.getRangeAt(0)
  if (!root.contains(range.commonAncestorContainer)) {
    return
  }
  const text = Array.from(range.cloneContents().childNodes).map(serializeNode).join('')
  if (text.length === 0) {
    return
  }
  event.preventDefault()
  event.clipboardData?.setData('text/plain', text)
  range.deleteContents()
  range.collapse(true)
  selection.removeAllRanges()
  selection.addRange(range)
  syncEditorToParent()
  void nextTick(refreshMentionState)
}

/**
 * 输入框获得焦点时恢复 @ 搜索状态。
 */
function refreshAfterFocus(): void {
  onEditorFocus()
  refreshMentionState()
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
 * 输入框键盘操作：@ 列表打开时支持上下选择、Enter/Tab 确认、Esc 关闭，其余情况 Enter 发送。
 *
 * <p>正在用输入法拼字时，回车是确认候选词而不是发送：此时不能阻止默认行为，也不能发送，
 * 否则中文输入会被打断、还会把没写完的内容发出去。{@code keyCode === 229} 是一些输入法在
 * {@code isComposing} 不可靠时的兜底判断。</p>
 *
 * @param event 键盘事件
 */
function onKeydown(event: KeyboardEvent): void {
  if (composing || event.isComposing || event.keyCode === 229) {
    return
  }

  if (mentionVisible.value) {
    if (event.key === 'Escape') {
      event.preventDefault()
      ignoreNextMentionKeyup = true
      closeMention()
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
        onPickMention(candidate)
      }
      return
    }
  }

  if (event.key === 'Enter') {
    event.preventDefault()
    if (event.shiftKey || event.ctrlKey || event.metaKey || event.altKey) {
      insertTextAtCaret('\n')
    } else {
      emit('submit')
    }
  }
}

// 切换会话时关闭上一会话的 @ 列表；正文被父组件清空时也一并收起
watch(() => props.peer, closeMention)
watch(() => props.modelValue, (value) => {
  if (value.length === 0) {
    closeMention()
  }
})

onUnmounted(() => {
  if (mentionTimer !== null) {
    clearTimeout(mentionTimer)
  }
})
</script>

<template>
  <div class="composer">
    <div v-if="quotedMessage" class="composer__quote" :title="quotedTitle">
      <div class="composer__quote-content">
        <span class="composer__quote-name">引用 @{{ quotedName }}</span>
        <span class="composer__quote-text">
          <template v-for="(segment, index) in quotedSegments" :key="index">
            <span v-if="segment.kind === 'mention' && segment.publicKey" class="composer__quote-mention">
              @{{ mentionDisplayName(segment.publicKey) }}
            </span>
            <template v-else>{{ segment.text }}</template>
          </template>
        </span>
      </div>
      <el-button
        text
        circle
        size="small"
        :icon="Close"
        title="取消引用"
        @click="emit('cancelQuote')"
      />
    </div>

    <div class="composer__input">
      <div
        ref="editorRef"
        class="composer__editor"
        contenteditable="true"
        role="textbox"
        aria-multiline="true"
        aria-label="消息正文"
        @input="onEditorInput"
        @keydown="onKeydown"
        @keyup="onEditorKeyup"
        @click="onEditorClick"
        @focus="refreshAfterFocus"
        @blur="onEditorBlur"
        @compositionstart="onCompositionStart"
        @compositionend="onCompositionEnd"
        @beforeinput="onEditorBeforeInput"
        @paste="onPaste"
        @copy="onCopy"
        @cut="onCut"
        @drop="onDrop"
      ></div>
      <span
        v-if="modelValue.length === 0 && !composingActive"
        class="composer__placeholder"
      >
        {{ placeholder }}
      </span>

      <div v-if="mentionVisible" class="mention-picker">
        <div class="mention-picker__title">提及</div>

        <el-skeleton v-if="mentionLoading && mentionCandidates.length === 0" :rows="2" animated />

        <div v-else-if="mentionCandidates.length > 0" class="mention-picker__list">
          <button
            v-for="(candidate, index) in mentionCandidates"
            :key="candidate.publicKey"
            type="button"
            class="mention-picker__item"
            :class="{ 'mention-picker__item--active': index === mentionIndex }"
            @mousedown.prevent
            @mouseenter="mentionIndex = index"
            @click="onPickMention(candidate)"
          >
            <span class="mention-picker__avatar">{{ candidate.label.slice(0, 1) }}</span>
            <span class="mention-picker__name">@{{ candidate.label }}</span>
            <span class="mention-picker__key">{{ shortKey(candidate.publicKey, 8) }}</span>
          </button>
        </div>

        <div v-else class="mention-picker__empty">
          {{ mentionFailed ? '人员加载失败，请稍后重试' : '没有匹配的人员' }}
        </div>
      </div>
    </div>

    <div class="composer__bar">
      <!-- 特殊类型内容的发送按钮都放左边：文件、后续的图片等 -->
      <div class="composer__tools">
        <el-button
          v-if="mentionEnabled"
          title="提及对方"
          @click="onMentionButton"
        >
          @
        </el-button>
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
          :icon="Headset"
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

.composer__quote {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 6px 8px 6px 10px;
  border-left: 3px solid #409eff;
  border-radius: 4px;
  background: #f0f7ff;
}

.composer__quote-content {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.composer__quote-name {
  overflow: hidden;
  font-size: 12px;
  font-weight: 500;
  color: #337ecc;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.composer__quote-text {
  overflow: hidden;
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.composer__quote-mention {
  color: #337ecc;
}

.composer__input {
  position: relative;
}

.composer__editor {
  width: 100%;
  min-height: 70px;
  max-height: 180px;
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

:deep(.composer__mention) {
  display: inline;
  padding: 1px 4px;
  border-radius: 3px;
  background: #ecf5ff;
  color: #337ecc;
  white-space: nowrap;
  user-select: all;
  -webkit-user-select: all;
  cursor: pointer;
}

:deep(.composer__mention:hover),
:deep(.composer__mention:focus-visible) {
  text-decoration: underline;
}

.mention-picker {
  position: absolute;
  z-index: 30;
  bottom: calc(100% + 8px);
  left: 0;
  width: min(360px, 100%);
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14);
}

.mention-picker__title {
  padding: 2px 8px 6px;
  font-size: 12px;
  color: #909399;
}

.mention-picker__list {
  max-height: 232px;
  overflow-y: auto;
}

.mention-picker__item {
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
  text-align: left;
}

.mention-picker__item:hover,
.mention-picker__item--active {
  background: #f0f7ff;
}

.mention-picker__avatar {
  display: inline-flex;
  width: 26px;
  height: 26px;
  flex: none;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #d8e8ff;
  color: #337ecc;
  font-size: 13px;
}

.mention-picker__name {
  min-width: 0;
  overflow: hidden;
  font-size: 13px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.mention-picker__key {
  flex: none;
  margin-left: auto;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  color: #a8abb2;
}

.mention-picker__empty {
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
