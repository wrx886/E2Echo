/**
 * 与 e2echo-client 后端接口对应的数据结构。
 *
 * <p>这里的字段与 client 侧的 {@code Result}、{@code PageData}、{@code ConversationDto}、
 * {@code MessageVo} 一一对应，接口地址见 {@code com.github.wrx886.e2echo.client.controller} 下的
 * 各控制器。</p>
 */

/**
 * 统一响应结果。
 *
 * <p>client 的所有接口（含认证失败）都以该结构返回，HTTP 状态码统一为 200，
 * 调用方通过 {@link code} 判断成功与否。</p>
 */
export interface Result<T> {
  /** 状态码，{@link RESULT_CODE_OK} 表示成功。 */
  code: string
  /** 提示信息，失败时为失败原因。 */
  message: string
  /** 业务数据，可能为 null。 */
  data: T
}

/** 成功状态码。 */
export const RESULT_CODE_OK = '0'

/** 失败状态码。 */
export const RESULT_CODE_FAIL = '-1'

/**
 * 认证失败时后端返回的提示信息。
 *
 * <p>会话失效时由 {@code AuthInterceptor} 直接写出该提示，前端据此识别“需要重新从客户端打开网页”，
 * 与普通业务失败区分开。</p>
 */
export const AUTH_FAILED_MESSAGE = '认证失败'

/** 消息通道：私聊，正文用 ECC 加解密。 */
export const CHANNEL_CHAT_PRIVATE_ECC = 'CHAT_PRIVATE_ECC'

/** 消息通道：群聊，正文用 AES 加解密。 */
export const CHANNEL_CHAT_GROUP_AES = 'CHAT_GROUP_AES'

/** 消息类型：文字聊天消息。 */
export const MESSAGE_TYPE_CHAT_TEXT = 'CHAT_TEXT'

/** 消息类型：群聊密钥消息，正文是 {@link ChatGroupKeyMessageVo}。 */
export const MESSAGE_TYPE_CHAT_GROUP_KEY = 'CHAT_GROUP_KEY'

/** 消息类型：聊天文件消息，正文是 {@link ChatFileMessageVo}。 */
export const MESSAGE_TYPE_CHAT_FILE = 'CHAT_FILE'

/**
 * 消息类型：聊天图片消息，正文同样是 {@link ChatFileMessageVo}。
 *
 * <p>正文结构与文件消息完全一样，单独分一个类型是为了让前端把图片直接渲染出来，而不是显示成文件。</p>
 */
export const MESSAGE_TYPE_CHAT_FILE_IMAGE = 'CHAT_FILE_IMAGE'

/**
 * 消息类型：聊天视频消息，正文同样是 {@link ChatFileMessageVo}。
 *
 * <p>正文结构与文件消息完全一样，单独分一个类型是为了让前端用播放器展示，而不是显示成文件。</p>
 */
export const MESSAGE_TYPE_CHAT_FILE_VIDEO = 'CHAT_FILE_VIDEO'

/**
 * 消息类型：聊天音频消息，正文同样是 {@link ChatFileMessageVo}。
 *
 * <p>正文结构与文件消息完全一样，单独分一个类型是为了让前端用播放器展示，而不是显示成文件。</p>
 */
export const MESSAGE_TYPE_CHAT_FILE_AUDIO = 'CHAT_FILE_AUDIO'

/**
 * 分页结果，对应 Spring Data 的 {@code Page} 序列化后的 JSON。
 *
 * @param T 当前页的数据类型
 */
export interface Page<T> {
  /** 当前页的数据。 */
  content: T[]
  /** 当前页码，从 0 开始（注意与接口入参的 pageNum 从 1 开始不同）。 */
  number: number
  /** 每页条数。 */
  size: number
  /** 总条数。 */
  totalElements: number
  /** 总页数。 */
  totalPages: number
  /** 是否为第一页。 */
  first: boolean
  /** 是否为最后一页。 */
  last: boolean
  /** 当前页是否为空。 */
  empty: boolean
}

/**
 * 文字聊天消息的正文，对应 {@code ChatTextMessageVo}。
 */
export interface ChatTextMessageVo {
  /** 消息正文。 */
  text: string
}

/**
 * 群聊密钥消息的正文，对应 {@code ChatGroupKeyMessageVo}。
 *
 * <p>群主把群密钥通过私聊逐条发给成员时用的消息类型：收到后密钥会写入本地，之后就能收发该群的
 * 消息。密钥本身不在界面上展示。</p>
 */
export interface ChatGroupKeyMessageVo {
  /** 群标识。 */
  group: string
  /** 密钥签发时间（毫秒），同时充当密钥版本。 */
  publishTime: number
  /** AES 密钥（HEX 格式）。 */
  aesKey: string
}

/**
 * 聊天文件消息的正文，对应 {@code ChatFileMessageVo}。
 *
 * <p>文件本体加密后放在对象存储里，正文只带取回并解密它所需的信息；下载时把这份正文原样提交给
 * client，由 client 下载密文、解密后返回文件流。</p>
 */
export interface ChatFileMessageVo {
  /** 文件名，用于展示与下载时命名。 */
  filename: string
  /** 文件加密用的一次性 AES 密钥（HEX 格式）。 */
  aesKey: string
  /** 对象存储里的对象键，形如 {@code default/文件 ID}。 */
  objectKey: string
}

/**
 * 会话消息，对应 {@code MessageVo}。
 *
 * <p>消息正文已由 client 解密并按类型反序列化：{@code message} 在文字聊天消息下是
 * {@link ChatTextMessageVo}，其余类型可能是字符串或别的结构。</p>
 */
export interface MessageVo {
  /** 服务端消息 ID。 */
  id: string
  /** 发送者公钥。 */
  from: string
  /** 接收者：私聊时为对方公钥、群聊时为群聊标识。 */
  to: string
  /** 消息正文，已反序列化。 */
  message: unknown
  /** 消息类型，如 {@link MESSAGE_TYPE_CHAT_TEXT}。 */
  type: string
  /** 消息通道，如 {@link CHANNEL_CHAT_PRIVATE_ECC}。 */
  channel: string
  /** 消息附加信息。 */
  info: string
  /** 本地序号，倒序翻页时作为 endSeq 游标。 */
  seq: number
}

/**
 * 本地消息实体，对应 {@code Message}。
 *
 * <p>会话列表里的 latestMessage 就是这个结构，其中的 {@code message} 是未反序列化的正文 JSON
 * 字符串。</p>
 */
export interface Message {
  /** 本地记录 ID。 */
  id: string
  /** 数据所有者，即登入用户的公钥。 */
  owner: string
  /** 创建时间。 */
  createTime: string
  /** 修改时间。 */
  updateTime: string
  /** 服务端消息 ID。 */
  messageId: string
  /** 发送者公钥。 */
  from: string
  /** 接收者：私聊时为对方公钥、群聊时为群聊标识。 */
  to: string
  /** 本地序号，用户内唯一、严格递增。 */
  seq: number
  /** 消息正文（明文 JSON 字符串）。 */
  message: string
  /** 消息类型。 */
  type: string
  /** 消息通道。 */
  channel: string
  /** 消息附加信息。 */
  info: string
}

/**
 * 会话，对应 {@code ConversationDto}。
 */
export interface ConversationDto {
  /** 主键 ID，为空表示新建。 */
  id?: string | null
  /** 数据所有者，即登入用户的公钥。 */
  owner?: string
  /** 创建时间。 */
  createTime?: string
  /** 修改时间。 */
  updateTime?: string
  /** 会话对方：私聊时为对方公钥、群聊时为群聊标识。 */
  peer: string
  /** 会话别名。 */
  alias: string
  /** 是否群聊会话。 */
  group: boolean
  /** 会话是否启用。 */
  enabled: boolean
  /** 最新一条消息，还没有消息时为 null。 */
  latestMessage?: Message | null
  /** 未读消息数：收到对方的消息时加一，查看该会话时清零。 */
  unread?: number
}

/**
 * 群成员，对应 {@code GroupMemberDto}。
 *
 * <p>群主用它决定把群密钥分发给谁；成员按登入用户隔离，同一用户的同一个群、同一个成员只有一条
 * 记录。</p>
 */
export interface GroupMemberDto {
  /** 主键 ID，删除成员时用它。 */
  id?: string
  /** 数据所有者，即登入用户的公钥。 */
  owner?: string
  /** 创建时间（成员加入时间）。 */
  createTime?: string
  /** 修改时间。 */
  updateTime?: string
  /** 群标识。 */
  group: string
  /** 成员的公钥。 */
  member: string
}

/**
 * 发送文字聊天消息的请求体，对应 {@code SendTestMessageReqVo}。
 */
export interface SendTextMessageReq {
  /** 接收者：私聊时为对方公钥、群聊时为群聊标识。 */
  to: string
  /** 是否群聊。 */
  group: boolean
  /** 消息正文。 */
  text: string
}
