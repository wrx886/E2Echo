import { get, post, request } from './http'
import type { GroupMemberDto, Page } from './types'

/**
 * 群聊管理接口，对应 {@code GroupController}。
 *
 * <p>群标识由群主创建时生成，以群主公钥开头，所以从群标识前缀就能判断自己是不是群主；群密钥的
 * 轮换与分发只有群主能操作。</p>
 */

/**
 * 分页查询某个群的成员，按主键升序。
 *
 * @param group    群标识
 * @param pageNum  页码，从 1 开始
 * @param pageSize 每页条数
 * @returns 成员分页结果
 */
export function listGroupMembers(
  group: string,
  pageNum: number,
  pageSize: number,
): Promise<Page<GroupMemberDto>> {
  return get<Page<GroupMemberDto>>('/api/group/member/list', { group, pageNum, pageSize })
}

/**
 * 新增群成员。
 *
 * <p>只有群主能加人；加完会立刻把当前最新的群密钥私聊发给该成员。成员已在名单里、自己不是群主、
 * 群还没有密钥都会失败，失败时成员不会写入（新增与密钥分发在同一个事务里）；群还没有密钥时要先
 * 生成或轮换一次密钥再加人。</p>
 *
 * @param group  群标识
 * @param member 成员的公钥
 */
export function addGroupMember(group: string, member: string): Promise<void> {
  return post<void>('/api/group/member', { group, member })
}

/**
 * 按主键删除群成员。
 *
 * <p>删除只是把成员从分发名单里去掉，对方手里还有当前群密钥，所以删除后必须轮换一次群密钥。</p>
 *
 * @param id 群成员记录的主键
 */
export function deleteGroupMember(id: string): Promise<void> {
  return request<void>({ method: 'DELETE', url: `/api/group/member/${encodeURIComponent(id)}` })
}

/**
 * 生成一个新的群标识。
 *
 * <p>群标识以当前用户公钥开头，即当前用户是这个群的群主。</p>
 *
 * @returns 群标识
 */
export function generateGroupId(): Promise<string> {
  return get<string>('/api/group/generateGroupId')
}

/**
 * 轮换群密钥：生成新密钥并分发给全部成员。
 *
 * @param group 群标识
 */
export function updateGroupKey(group: string): Promise<void> {
  return request<void>({ method: 'POST', url: '/api/group/updateGroupKey', params: { group } })
}

/**
 * 重发群密钥：把当前最新的群密钥再分发给全部成员，用于成员漏收。
 *
 * @param group 群标识
 */
export function resendGroupKey(group: string): Promise<void> {
  return request<void>({ method: 'POST', url: '/api/group/resendGroupKey', params: { group } })
}
