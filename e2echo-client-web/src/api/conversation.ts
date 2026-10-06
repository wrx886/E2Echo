import { get, post } from './http'
import { isTrustedGroupKey } from './message'
import {
  MESSAGE_TYPE_CHAT_FILE,
  MESSAGE_TYPE_CHAT_FILE_AUDIO,
  MESSAGE_TYPE_CHAT_FILE_IMAGE,
  MESSAGE_TYPE_CHAT_FILE_VIDEO,
  MESSAGE_TYPE_CHAT_GROUP_KEY,
  MESSAGE_TYPE_CHAT_TEXT,
} from './types'
import type {
  ChatFileMessageVo,
  ChatGroupKeyMessageVo,
  ChatTextMessageVo,
  ConversationDto,
  Page,
} from './types'

/**
 * 会话接口，对应 {@code ConversationController}。
 */

/**
 * 分页列出当前用户的会话，最近有消息的排在前面。
 *
 * @param pageNum  页码，从 1 开始
 * @param pageSize 每页条数
 * @returns 会话分页结果
 */
export function listConversations(pageNum: number, pageSize: number): Promise<Page<ConversationDto>> {
  return get<Page<ConversationDto>>('/api/conversation/list', { pageNum, pageSize })
}

/**
 * 按会话对方查询会话。
 *
 * @param peer 会话对方：私聊时为对方公钥、群聊时为群聊标识
 * @returns 会话，不存在时返回 null
 */
export function getConversation(peer: string): Promise<ConversationDto | null> {
  return get<ConversationDto | null>(`/api/conversation/${encodeURIComponent(peer)}`)
}

/**
 * 保存会话：新建会话，或修改已有会话的别名、是否启用等。
 *
 * <p>新建时不要带 id；修改已有会话时 id、peer、alias、group、enabled 都需要带上。</p>
 *
 * @param conversation 待保存的会话
 */
export function saveConversation(conversation: ConversationDto): Promise<void> {
  return post<void>('/api/conversation', conversation)
}

/**
 * 查询会话别名，展示会话名时使用。
 *
 * @param peer 会话对方：私聊时为对方公钥、群聊时为群聊标识
 * @returns 别名，没有对应会话或没有设置别名时返回 null
 */
export function getConversationAlias(peer: string): Promise<string | null> {
  return get<string | null>(`/api/conversation/alias/${encodeURIComponent(peer)}`)
}

/**
 * 统计当前用户的未读消息总数（所有会话的未读数之和）。
 *
 * @returns 未读总数，没有未读时是 0
 */
export function countUnread(): Promise<number> {
  return get<number>('/api/conversation/countUnread')
}

/**
 * 取会话最新消息的预览文本，供会话列表展示。
 *
 * <p>会话里的最新消息是实体，正文还是未反序列化的 JSON 字符串，这里按消息类型取出文字。</p>
 *
 * @param conversation 会话
 * @returns 预览文本，没有消息或不是文字消息时返回空串
 */
export function latestMessagePreview(conversation: ConversationDto): string {
  const latest = conversation.latestMessage
  if (!latest) {
    return ''
  }
  if (latest.type === MESSAGE_TYPE_CHAT_GROUP_KEY) {
    // 密钥消息也按“群标识必须是发送者（群主）开的”校验一次，不可信的在预览里就标出来
    try {
      const body = JSON.parse(latest.message) as ChatGroupKeyMessageVo
      const trusted = typeof body.group === 'string'
        && isTrustedGroupKey(body.group, latest.from, latest.channel)
      return trusted ? '[群聊密钥]' : '[可疑的群聊密钥]'
    } catch {
      return '[可疑的群聊密钥]'
    }
  }
  if (latest.type === MESSAGE_TYPE_CHAT_FILE) {
    // 会话里的最新消息是实体，正文还是 JSON 字符串，取文件名时解析一次
    try {
      const body = JSON.parse(latest.message) as ChatFileMessageVo
      const name = typeof body.filename === 'string' ? body.filename.trim() : ''
      return name.length > 0 ? `[文件] ${name}` : '[文件]'
    } catch {
      return '[文件]'
    }
  }
  if (latest.type === MESSAGE_TYPE_CHAT_FILE_IMAGE) {
    return '[图片]'
  }
  if (latest.type === MESSAGE_TYPE_CHAT_FILE_VIDEO) {
    return '[视频]'
  }
  if (latest.type === MESSAGE_TYPE_CHAT_FILE_AUDIO) {
    return '[音频]'
  }
  if (latest.type !== MESSAGE_TYPE_CHAT_TEXT) {
    return ''
  }
  try {
    const body = JSON.parse(latest.message) as ChatTextMessageVo
    return typeof body.text === 'string' ? body.text : ''
  } catch {
    return ''
  }
}
