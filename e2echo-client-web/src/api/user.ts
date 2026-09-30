import { get } from './http'

/**
 * 用户接口，对应 {@code UserController}。
 */

/**
 * 查询当前登入用户的公钥。
 *
 * <p>前端用它判断一条消息是不是自己发的：私聊里可以靠“发送者是不是会话对方”推断，群聊里没有
 * 这个接口就无法区分。</p>
 *
 * @returns 当前登入用户的 secp256k1 公钥（RAW HEX 格式）
 */
export function getCurrentUser(): Promise<string> {
  return get<string>('/api/user/current')
}
