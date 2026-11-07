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
