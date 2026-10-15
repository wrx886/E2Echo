import { get, post, request } from './http'
import type { BroadcastMessageRespVo, Page, PageData } from './types'

/**
 * 广播订阅接口，对应 {@code BroadcastSubscribeController}。
 *
 * <p>订阅关系只存在 client 的本地库：服务端并不知道谁订阅了谁，所以"订阅流"是在 client 上拼出来的
 * ——把订阅的作者列表当成过滤条件去查帖子。别名也只在本机展示用。</p>
 */

/**
 * 订阅，对应 {@code BroadcastSubscribeDto}。
 */
export interface BroadcastSubscribeDto {
  /** 主键 ID。 */
  id: string
  /** 数据所有者，即订阅方的公钥。 */
  owner: string
  /** 创建时间。 */
  createTime: string
  /** 修改时间。 */
  updateTime: string
  /** 被订阅者的公钥。 */
  publicKey: string
  /** 被订阅者的别名，只在本机展示用，可以为空。 */
  alias?: string | null
}

/**
 * 订阅列表查询条件，对应 {@code BroadcastSubscribeListReq}。
 */
export interface BroadcastSubscribeListReq {
  /** 页码，从 1 开始。 */
  pageNum: number
  /** 每页条数。 */
  pageSize: number
  /** 关键词：按被订阅者公钥或别名模糊匹配，忽略大小写；空串表示不过滤。 */
  publicKeyOrAlias: string
}

/**
 * 查询当前用户对某个作者的订阅。
 *
 * @param publicKey 被订阅者的公钥
 * @returns 订阅，没有订阅时返回 null
 */
export function getBroadcastSubscribe(publicKey: string): Promise<BroadcastSubscribeDto | null> {
  return get<BroadcastSubscribeDto | null>(`/api/broadcast/subscribe/${encodeURIComponent(publicKey)}`)
}

/**
 * 订阅某个作者。
 *
 * @param publicKey 被订阅者的公钥
 */
export function subscribeBroadcastAuthor(publicKey: string): Promise<void> {
  return post<void>(`/api/broadcast/subscribe/subscribe/${encodeURIComponent(publicKey)}`)
}

/**
 * 取消订阅某个作者。
 *
 * @param publicKey 被订阅者的公钥
 */
export function unsubscribeBroadcastAuthor(publicKey: string): Promise<void> {
  return post<void>(`/api/broadcast/subscribe/unsubscribe/${encodeURIComponent(publicKey)}`)
}

/**
 * 修改订阅别名：给已订阅的作者起一个本机展示用的名字。
 *
 * @param publicKey 被订阅者的公钥
 * @param alias     新别名，空串表示清除
 */
export function setBroadcastSubscribeAlias(publicKey: string, alias: string): Promise<void> {
  // 别名在接口上是查询参数，不是请求体
  return request<void>({
    method: 'POST',
    url: `/api/broadcast/subscribe/setAlias/${encodeURIComponent(publicKey)}`,
    params: { alias },
  })
}

/**
 * 分页查询当前用户的订阅。
 *
 * @param req 查询条件
 * @returns 订阅分页结果
 */
export function listBroadcastSubscribes(
  req: BroadcastSubscribeListReq,
): Promise<Page<BroadcastSubscribeDto>> {
  return post<Page<BroadcastSubscribeDto>>('/api/broadcast/subscribe/list', req)
}

/**
 * 拉取订阅流：当前用户订阅的这些作者的帖子。
 *
 * @param pageNum  页码，从 1 开始
 * @param pageSize 每页条数
 * @param endId    游标：只取 ID 小于它的帖子，空串表示第一页
 * @returns 帖子分页结果，验签失败的条目是 null 占位
 */
export function listSubscribeMessages(
  pageNum: number,
  pageSize: number,
  endId = '',
): Promise<PageData<BroadcastMessageRespVo | null>> {
  return request<PageData<BroadcastMessageRespVo | null>>({
    method: 'POST',
    url: '/api/broadcast/message/listSubscribe',
    params: { pageNum, pageSize, endId },
  })
}
