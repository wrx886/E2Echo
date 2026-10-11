import { get, post, request } from './http'
import type {
  BroadcastFileVo,
  BroadcastMessageListReq,
  BroadcastMessageRespVo,
  BroadcastMessageVo,
  BroadcastUserinfo,
  BroadcastUserinfoRespVo,
  PageData,
} from './types'

/**
 * 广播接口，对应 {@code BroadcastController}。
 *
 * <p>广播只存服务端、不落本地库，所以这里都是直接和服务端交互。数据分成三类：用户资料、帖子
 * （{@code to} 为占位符 {@code *}）、评论（{@code to} 为帖子 ID）。</p>
 */

/**
 * 查询某个用户的资料。
 *
 * @param uid 用户公钥
 * @returns 用户资料与消息信息（作者看 {@code from}），没有资料或签名校验不通过时为 null
 */
export function getUserinfo(uid: string): Promise<BroadcastUserinfoRespVo | null> {
  return get<BroadcastUserinfoRespVo | null>(`/api/broadcast/userinfo/${encodeURIComponent(uid)}`)
}

/**
 * 发布当前用户的资料（会覆盖旧资料，读取时取最新一条）。
 *
 * <p>作者固定是当前登入用户，正文里不带公钥。</p>
 *
 * @param userinfo 用户资料
 */
export function publishUserinfo(userinfo: BroadcastUserinfo): Promise<void> {
  return post<void>('/api/broadcast/userinfo', userinfo)
}

/**
 * 发布一条广播消息：帖子或评论。
 *
 * @param to      发布目标：空串是发帖，帖子 ID 是在该帖子下评论
 * @param message 广播正文（文本 + 附件）
 */
export function publishBroadcastMessage(to: string, message: BroadcastMessageVo): Promise<void> {
  return post<void>('/api/broadcast/message', { to, message })
}

/**
 * 按消息 ID 查询一条广播消息。
 *
 * @param id 消息 ID
 * @returns 广播消息，不存在或签名校验不通过时为 null
 */
export function getBroadcastMessage(id: string): Promise<BroadcastMessageRespVo | null> {
  return get<BroadcastMessageRespVo | null>(`/api/broadcast/message/${encodeURIComponent(id)}`)
}

/**
 * 分页查询广播消息列表。
 *
 * <p>验签失败的条目在结果里是 {@code null} 占位，调用方需要跳过。</p>
 *
 * @param req 查询条件
 * @returns 广播消息分页结果
 */
export function listBroadcastMessages(
  req: BroadcastMessageListReq,
): Promise<PageData<BroadcastMessageRespVo | null>> {
  return post<PageData<BroadcastMessageRespVo | null>>('/api/broadcast/message/list', req)
}

/**
 * 上传广播附件。
 *
 * <p>上传与发消息分开：先把文件传上去拿到附件信息，再把它放进正文的 {@code files} 里发帖；一条
 * 广播可以带多个附件，逐个上传即可。</p>
 *
 * @param file       待上传的文件
 * @param onProgress 上传进度回调，参数是 0-100 的百分比
 * @returns 附件信息（对象键、文件名、内容类型、大小）
 */
export function uploadBroadcastFile(
  file: File,
  onProgress?: (percent: number) => void,
): Promise<BroadcastFileVo> {
  const form = new FormData()
  form.append('file', file, file.name)

  return request<BroadcastFileVo>({
    method: 'POST',
    url: '/api/file/broadcast',
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
 * 拼出广播附件的地址。
 *
 * <p>广播文件不加密、直接透传，地址可以直接交给图片、播放器或下载链接；接口还支持 Range，视频、
 * 音频能拖进度条。</p>
 *
 * @param file 附件信息
 * @returns 可以直接给标签或下载链接用的地址
 */
export function broadcastFileUrl(file: BroadcastFileVo): string {
  const params = new URLSearchParams({
    objectKey: file.objectKey,
    filename: file.filename,
  })
  if (file.contentType.length > 0) {
    params.set('contentType', file.contentType)
  }
  return `/api/file/broadcast?${params.toString()}`
}

/**
 * 解析资料里的头像地址。
 *
 * <p>头像只走广播文件通道：资料里存的是上传后拿到的文件 ID（对象键），这里把它拼成广播文件地址。
 * 不支持外部链接，所以不按前缀区分。</p>
 *
 * @param value 资料里的头像地址
 * @returns 可以直接给 img 用的地址，没有设置头像时返回 undefined
 */
export function avatarSrc(value?: string | null): string | undefined {
  const text = value?.trim() ?? ''
  if (text.length === 0) {
    return undefined
  }
  return `/api/file/broadcast?objectKey=${encodeURIComponent(text)}`
}
