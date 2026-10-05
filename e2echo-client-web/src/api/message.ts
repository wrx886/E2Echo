import { get, post, request } from './http'
import {
  CHANNEL_CHAT_PRIVATE_ECC,
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
  MessageVo,
  Page,
  SendTextMessageReq,
} from './types'

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
 * 发送文件的大小上限（字节）。
 *
 * <p>与 client 的 {@code Const.FILE_MAX_SIZE_BYTE} 保持一致：超限的文件在 client 侧本来就会被拒收，
 * 前端先拦一道，省得白白把文件传一遍。client 调整上限时这里要跟着改。</p>
 */
export const FILE_MAX_SIZE_BYTE = 25 * 1024 * 1024

/** 文件超限时的提示，与 client 的 {@code Const.FILE_MAX_SIZE_MESSAGE} 文案一致。 */
export const FILE_MAX_SIZE_MESSAGE = `文件大小应小于 ${FILE_MAX_SIZE_BYTE / 1024 / 1024}M`

/**
 * 发送文件消息。
 *
 * <p>文件内容用 multipart 上传给 client，由 client 负责加密、上传对象存储，再把带对象键与密钥的
 * 消息发出去；群聊没有密钥、文件为空、超过大小上限等失败原因都由 client 返回。</p>
 *
 * @param to         接收者：私聊时为对方公钥、群聊时为群聊标识
 * @param group      是否群聊
 * @param file       待发送的文件
 * @param onProgress 上传进度回调，参数是 0-100 的百分比
 */
export function sendFileMessage(
  to: string,
  group: boolean,
  file: File,
  onProgress?: (percent: number) => void,
): Promise<void> {
  return sendFileForm('/api/message/sendFileMessage', to, group, file, onProgress)
}

/**
 * 发送图片消息。
 *
 * <p>和文件消息走同一套流程，只是消息类型不同：client 会把类型标成图片消息，前端据此直接渲染。</p>
 *
 * @param to         接收者：私聊时为对方公钥、群聊时为群聊标识
 * @param group      是否群聊
 * @param file       待发送的图片
 * @param onProgress 上传进度回调，参数是 0-100 的百分比
 */
export function sendImageMessage(
  to: string,
  group: boolean,
  file: File,
  onProgress?: (percent: number) => void,
): Promise<void> {
  return sendFileForm('/api/message/sendImageMessage', to, group, file, onProgress)
}

/**
 * 发送视频消息。
 *
 * <p>和文件消息走同一套流程，只是消息类型不同：client 会把类型标成视频消息，前端用播放器展示。</p>
 *
 * @param to         接收者：私聊时为对方公钥、群聊时为群聊标识
 * @param group      是否群聊
 * @param file       待发送的视频
 * @param onProgress 上传进度回调，参数是 0-100 的百分比
 */
export function sendVideoMessage(
  to: string,
  group: boolean,
  file: File,
  onProgress?: (percent: number) => void,
): Promise<void> {
  return sendFileForm('/api/message/sendVideoMessage', to, group, file, onProgress)
}

/**
 * 发送音频消息。
 *
 * <p>和文件消息走同一套流程，只是消息类型不同：client 会把类型标成音频消息，前端用播放器展示。</p>
 *
 * @param to         接收者：私聊时为对方公钥、群聊时为群聊标识
 * @param group      是否群聊
 * @param file       待发送的音频
 * @param onProgress 上传进度回调，参数是 0-100 的百分比
 */
export function sendAudioMessage(
  to: string,
  group: boolean,
  file: File,
  onProgress?: (percent: number) => void,
): Promise<void> {
  return sendFileForm('/api/message/sendAudioMessage', to, group, file, onProgress)
}

/**
 * 用 multipart 把文件内容提交给 client（文件与图片共用）。
 *
 * @param url        接口地址
 * @param to         接收者
 * @param group      是否群聊
 * @param file       待发送的文件
 * @param onProgress 上传进度回调
 */
function sendFileForm(
  url: string,
  to: string,
  group: boolean,
  file: File,
  onProgress?: (percent: number) => void,
): Promise<void> {
  const form = new FormData()
  form.append('to', to)
  form.append('group', String(group))
  form.append('file', file, file.name)

  return request<void>({
    method: 'POST',
    url,
    data: form,
    onUploadProgress: (event) => {
      if (!onProgress || !event.total) {
        return
      }
      onProgress(Math.round((event.loaded / event.total) * 100))
    },
  })
}

/**
 * 把消息正文规整成对象。
 *
 * <p>client 是按消息类型注册处理器、再用处理器的类型反序列化正文的；没有注册处理器的类型会原样
 * 返回 JSON 字符串（文件消息目前就是这样），所以两种形态都要接。</p>
 *
 * @param value 消息正文
 * @returns 正文对象，不是对象或字符串也解析不出来时返回 null
 */
function toMessageBody(value: unknown): Record<string, unknown> | null {
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value) as unknown
      return parsed !== null && typeof parsed === 'object' ? parsed as Record<string, unknown> : null
    } catch {
      return null
    }
  }
  return value !== null && typeof value === 'object' ? value as Record<string, unknown> : null
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
  const body = toMessageBody(message.message) as ChatTextMessageVo | null
  return body && typeof body.text === 'string' ? body.text : null
}

/**
 * 取群聊密钥消息的正文。
 *
 * @param message 会话消息
 * @returns 正文；不是群聊密钥消息或正文结构异常时返回 null
 */
export function groupKeyOf(message: MessageVo): ChatGroupKeyMessageVo | null {
  if (message.type !== MESSAGE_TYPE_CHAT_GROUP_KEY) {
    return null
  }
  const body = toMessageBody(message.message) as ChatGroupKeyMessageVo | null
  return body && typeof body.group === 'string' ? body : null
}

/**
 * 取聊天文件消息的正文。
 *
 * @param message 会话消息
 * @returns 文件名、密钥与对象键；不是文件消息或正文结构异常时返回 null
 */
export function fileMessageOf(message: MessageVo): ChatFileMessageVo | null {
  return fileBodyOf(message, MESSAGE_TYPE_CHAT_FILE)
}

/**
 * 取聊天图片消息的正文。
 *
 * @param message 会话消息
 * @returns 文件名、密钥与对象键；不是图片消息或正文结构异常时返回 null
 */
export function imageMessageOf(message: MessageVo): ChatFileMessageVo | null {
  return fileBodyOf(message, MESSAGE_TYPE_CHAT_FILE_IMAGE)
}

/**
 * 取聊天视频消息的正文。
 *
 * @param message 会话消息
 * @returns 文件名、密钥与对象键；不是视频消息或正文结构异常时返回 null
 */
export function videoMessageOf(message: MessageVo): ChatFileMessageVo | null {
  return fileBodyOf(message, MESSAGE_TYPE_CHAT_FILE_VIDEO)
}

/**
 * 取聊天音频消息的正文。
 *
 * @param message 会话消息
 * @returns 文件名、密钥与对象键；不是音频消息或正文结构异常时返回 null
 */
export function audioMessageOf(message: MessageVo): ChatFileMessageVo | null {
  return fileBodyOf(message, MESSAGE_TYPE_CHAT_FILE_AUDIO)
}

/**
 * 取文件类消息（文件、图片）的正文：两种类型的正文结构相同，只按类型区分。
 *
 * @param message 会话消息
 * @param type    期望的消息类型
 * @returns 正文，类型不符或结构异常时返回 null
 */
function fileBodyOf(message: MessageVo, type: string): ChatFileMessageVo | null {
  if (message.type !== type) {
    return null
  }
  const body = toMessageBody(message.message)
  if (!body
    || typeof body.filename !== 'string'
    || typeof body.aesKey !== 'string'
    || typeof body.objectKey !== 'string') {
    return null
  }
  return { filename: body.filename, aesKey: body.aesKey, objectKey: body.objectKey }
}

/**
 * 判断一条群聊密钥消息是否可信。
 *
 * <p>群标识由“群主公钥 + 随机 ID”拼成，所以群标识以发送者公钥开头，才说明这条密钥确实是群主发的
 * （消息本身有签名，发送者伪造不了）；同时群密钥只允许走私聊通道分发。两个条件有任意一条不满足，
 * 就可能是别人伪造的密钥消息。</p>
 *
 * <p>这条判定要在界面上再做一次：client 拒绝一条密钥消息时它已经入库了，所以伪造的消息仍会出现在
 * 聊天记录里，只能靠这里的标记提醒用户。</p>
 *
 * @param group   消息正文里的群标识
 * @param from    消息发送者的公钥
 * @param channel 消息通道
 * @returns 可信返回 true
 */
export function isTrustedGroupKey(group: string, from: string, channel: string): boolean {
  return group.startsWith(from) && channel === CHANNEL_CHAT_PRIVATE_ECC
}
