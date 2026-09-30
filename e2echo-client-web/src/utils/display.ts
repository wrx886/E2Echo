import type { ConversationDto } from '@/api'

/**
 * 展示相关的格式化工具。
 */

/**
 * 从消息 ID 中取出消息的产生时间。
 *
 * <p>client 的 ID 由 {@code IdUtil} 生成：前 16 位是十六进制的毫秒时间戳，后面是随机 UUID。
 * 会话消息接口返回的 {@code MessageVo} 里没有时间字段，需要展示时间时按这个约定取。</p>
 *
 * @param id 消息 ID
 * @returns 毫秒时间戳，ID 结构不符合约定时返回 {@code NaN}
 */
export function timestampFromId(id: string): number {
  const value = Number.parseInt(id.slice(0, 16), 16)
  return Number.isNaN(value) ? Number.NaN : value
}

/**
 * 把 LocalDateTime 字符串、时间戳或 Date 解析成 Date。
 *
 * @param value 待解析的值
 * @returns 解析结果，无法解析时返回 Invalid Date
 */
function toDate(value: string | number | Date): Date {
  if (value instanceof Date) {
    return value
  }
  if (typeof value === 'number') {
    return new Date(value)
  }
  // LocalDateTime 形如 2026-09-30T20:35:19.845611，毫秒部分可能超过 3 位，先截断再交给 Date
  return new Date(value.replace(/(\.\d{3})\d+/, '$1'))
}

/**
 * 补零到两位。
 *
 * @param value 待补零的数字
 * @returns 补零后的字符串
 */
function pad(value: number): string {
  return value.toString().padStart(2, '0')
}

/**
 * 取某天的零点。
 *
 * @param date 日期
 * @returns 当天零点的毫秒时间戳
 */
function startOfDay(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
}

/**
 * 格式化时间：今天只显示时分，昨天加“昨天”，本年加月日，更早的加年份。
 *
 * @param value 时间值，可以是 LocalDateTime 字符串、毫秒时间戳或 Date
 * @returns 格式化后的时间；无法解析时返回空串
 */
export function formatTime(value: string | number | Date): string {
  const date = toDate(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }

  const now = new Date()
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`

  // 按“天”比较，避免用毫秒差计算时跨天但不足 24 小时被算成今天
  const dayDiff = Math.round((startOfDay(now) - startOfDay(date)) / (24 * 60 * 60 * 1000))
  if (dayDiff === 0) {
    return time
  }
  if (dayDiff === 1) {
    return `昨天 ${time}`
  }
  if (date.getFullYear() === now.getFullYear()) {
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${time}`
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${time}`
}

/**
 * 取公钥等长标识的末尾若干位，用于在没有联系人资料时展示一个可读的名字。
 *
 * @param value  完整标识
 * @param length 保留的位数
 * @returns 末尾若干位；标识比要求的位数还短时原样返回
 */
export function shortKey(value: string, length = 6): string {
  return value.length <= length ? value : value.slice(value.length - length)
}

/**
 * 取会话的展示名称：优先用别名，没有别名时退回会话对方标识的末几位。
 *
 * @param conversation 会话，可能为空
 * @param peer         会话对方，会话为空时用它兜底
 * @returns 展示名称
 */
export function conversationTitle(conversation: ConversationDto | null | undefined, peer: string): string {
  const alias = conversation?.alias
  return alias && alias.trim().length > 0 ? alias : shortKey(peer)
}

/**
 * 取当前用户的展示名称。
 *
 * <p>优先用“与自己”那条会话的别名：用户可以建一个会话对方等于自己公钥的会话，用它的别名当
 * 自己的名称。没有这样的会话、或者别名为空时，退回公钥末 5 位——与 client 自动建会话时取对方
 * 末 5 位当别名的规则一致。</p>
 *
 * @param publicKey 当前用户的公钥，尚未取到时为空串
 * @param alias     与自己那条会话的别名，可能为空
 * @returns 展示名称
 */
export function resolveSelfName(publicKey: string, alias: string | null | undefined): string {
  if (publicKey.length === 0) {
    return ''
  }
  const name = alias?.trim() ?? ''
  return name.length > 0 ? name : shortKey(publicKey, 5)
}

/**
 * 取头像上的文字。
 *
 * <p>没有头像图片，用名称的首字符；名称为空时退回公钥末 2 位。</p>
 *
 * @param name      展示名称
 * @param publicKey 当前用户的公钥
 * @returns 头像文字
 */
export function resolveAvatarText(name: string, publicKey: string): string {
  const value = name.trim()
  if (value.length > 0) {
    return value.slice(0, 1)
  }
  return publicKey.length > 0 ? shortKey(publicKey, 2) : ''
}
