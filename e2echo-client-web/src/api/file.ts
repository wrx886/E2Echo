import { request } from './http'
import type { ChatFileMessageVo } from './types'

/**
 * 文件接口，对应 {@code FileController}。
 *
 * <p>只用来取回聊天文件：把文件消息正文原样提交给 client，client 从对象存储下载密文、解密后把
 * 文件流返回。前端拿到的就是解密后的内容，不再做任何处理。</p>
 */

/**
 * 下载聊天文件。
 *
 * <p>参数用请求体传，所以是 POST；响应是二进制流（失败时仍是统一响应 JSON，由响应拦截器区分）。</p>
 *
 * @param file 文件消息正文（文件名、密钥、对象键）
 * @returns 文件内容
 */
export function downloadChatFile(file: ChatFileMessageVo): Promise<Blob> {
  return request<Blob>({
    method: 'POST',
    url: '/api/file/chat',
    data: file,
    responseType: 'blob',
  })
}
