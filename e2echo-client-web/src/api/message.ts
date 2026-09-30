import { get, post } from './http'
import { MESSAGE_TYPE_CHAT_TEXT } from './types'
import type { ChatTextMessageVo, MessageVo, Page, SendTextMessageReq } from './types'

/**
 * 消息接口，对应 {@code MessageController}。
 */

/**
 * 分页查询某个会话的消息（收 + 发），最新的在前。
 *
 * <p>倒序翻页：把上一页最后一条消息的 {@code seq} 作为 {@code endSeq} 传回来，只取序号更小的
 * 消息；首页不传 {@code endSeq}。</p>
 *
 * @param peer      会话对方：私聊时为对方公钥、群聊时为群聊标识
 * @param pageNum   页码，从 1 开始
 * @param pageSize  每页条数
 * @param endSeq    倒序翻页的游标（上一页最后一条消息的本地序号），首页不传
 * @returns 消息分页结果，最新的在前
 */
export function findConversationMessages(
  peer: string,
  pageNum: number,
  pageSize: number,
  endSeq?: number | string,
): Promise<Page<MessageVo>> {
  return get<Page<MessageVo>>('/api/message/findConversation', {
    peer,
    pageNum,
    pageSize,
    endSeq: endSeq === undefined ? undefined : String(endSeq),
  })
}

/**
 * 发送文字聊天消息。
 *
 * <p>消息由 client 负责加密、签名与落库，前端只需要给出接收者与正文。私聊消息发送时立即在本地
 * 落库，群聊消息等拉取回来，两种情况最终都会由 notice 通道通知前端刷新。</p>
 *
 * @param req 接收者、是否群聊与消息正文
 */
export function sendTextMessage(req: SendTextMessageReq): Promise<void> {
  return post<void>('/api/message/sendTextMessage', req)
}

/**
 * 取文字聊天消息的正文。
 *
 * @param message 会话消息
 * @returns 正文；不是文字聊天消息或正文结构异常时返回 null
 */
export function chatTextOf(message: MessageVo): string | null {
  if (message.type !== MESSAGE_TYPE_CHAT_TEXT) {
    return null
  }
  const body = message.message as ChatTextMessageVo | null
  return body && typeof body.text === 'string' ? body.text : null
}
