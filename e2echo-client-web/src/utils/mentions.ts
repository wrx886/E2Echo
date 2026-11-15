import { PUBLIC_KEY_LENGTH } from '@/api'

/**
 * 消息正文里的一小段内容：普通文字，或一个 @ 公钥。
 */
export interface MentionSegment {
  /** 片段类型。 */
  kind: 'text' | 'mention'
  /** 原始文字；mention 片段这里是包含 @ 的完整公钥。 */
  text: string
  /** mention 片段对应的公钥。 */
  publicKey?: string
}

/** 完整公钥前面紧跟一个 @，最后一个字符后不能再接十六进制字符。 */
const MENTION_PATTERN = new RegExp(
  `@([0-9a-fA-F]{${PUBLIC_KEY_LENGTH}})(?![0-9a-fA-F])`,
  'g',
)

/**
 * 把消息正文拆成普通文字与 @ 公钥片段。
 *
 * <p>只识别完整长度的公钥，避免把用户手打的普通 @ 文字误当成提及。拆分结果保留原文，
 * 消息实体与发送请求都不会被改写。</p>
 *
 * @param text 消息正文
 * @returns 按出现顺序排列的片段；空正文返回空数组
 */
export function splitMentions(text: string): MentionSegment[] {
  const segments: MentionSegment[] = []
  let cursor = 0

  for (const match of text.matchAll(MENTION_PATTERN)) {
    const index = match.index
    if (index === undefined) {
      continue
    }
    if (index > cursor) {
      segments.push({ kind: 'text', text: text.slice(cursor, index) })
    }
    segments.push({
      kind: 'mention',
      text: match[0],
      publicKey: match[1],
    })
    cursor = index + match[0].length
  }

  if (cursor < text.length) {
    segments.push({ kind: 'text', text: text.slice(cursor) })
  }
  return segments
}

/**
 * 取正文里出现过的全部公钥，按首次出现顺序去重。
 *
 * @param text 消息正文
 * @returns 公钥列表
 */
export function mentionPublicKeys(text: string): string[] {
  const keys = new Set<string>()
  for (const segment of splitMentions(text)) {
    if (segment.publicKey) {
      keys.add(segment.publicKey)
    }
  }
  return [...keys]
}

/**
 * 删除光标前面的 @ 标签。
 *
 * <p>@ 标签本身是 {@code contenteditable=false}，不同浏览器在标签后的空格处按退格时处理不一致。
 * 这里统一把标签及其后最多一个分隔空格整体删除。</p>
 *
 * @param root 编辑区
 * @returns 确实删除了标签时返回 true
 */
export function deleteMentionBeforeCaret(root: HTMLElement): boolean {
  const selection = window.getSelection()
  if (!selection || selection.rangeCount === 0 || !selection.isCollapsed) {
    return false
  }
  const range = selection.getRangeAt(0)
  if (!root.contains(range.startContainer)) {
    return false
  }

  const startElement = range.startContainer.nodeType === Node.ELEMENT_NODE
    ? range.startContainer as HTMLElement
    : range.startContainer.parentElement
  const inside = startElement?.closest<HTMLElement>('[data-public-key]')
  let mention = inside && root.contains(inside) ? inside : null

  if (!mention) {
    let previous: Node | null = null
    if (range.startContainer.nodeType === Node.TEXT_NODE) {
      const text = range.startContainer.nodeValue ?? ''
      const before = text.slice(0, range.startOffset)
      if (before.trim().length > 0) {
        return false
      }
      previous = range.startContainer.previousSibling
    } else {
      previous = range.startContainer.childNodes[range.startOffset - 1] ?? null
      if (previous?.nodeType === Node.TEXT_NODE && (previous.nodeValue ?? '').trim().length === 0) {
        previous = previous.previousSibling
      }
    }
    while (previous?.nodeType === Node.TEXT_NODE && (previous.nodeValue ?? '').length === 0) {
      previous = previous.previousSibling
    }
    if (previous?.nodeType === Node.ELEMENT_NODE
      && (previous as HTMLElement).dataset.publicKey) {
      mention = previous as HTMLElement
    }
  }

  if (!mention?.parentNode) {
    return false
  }

  const parent = mention.parentNode
  const index = Array.from(parent.childNodes).indexOf(mention)
  let next = mention.nextSibling
  parent.removeChild(mention)
  if (next?.nodeType === Node.TEXT_NODE) {
    const text = next.nodeValue ?? ''
    if (text.startsWith(' ') || text.startsWith('\n') || text.startsWith('\t')) {
      if (text.length === 1) {
        parent.removeChild(next)
        next = null
      } else {
        next.nodeValue = text.slice(1)
      }
    }
  }

  const caret = document.createRange()
  caret.setStart(parent, Math.min(index, parent.childNodes.length))
  caret.collapse(true)
  selection.removeAllRanges()
  selection.addRange(caret)
  return true
}
