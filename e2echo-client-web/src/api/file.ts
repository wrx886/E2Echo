import type { ChatFileMessageVo } from './types'

/**
 * 文件接口，对应 {@code FileController}。
 *
 * <p>取回聊天文件走 GET，参数放在查询串里：图片、视频、音频可以直接把地址交给对应的标签，浏览器
 * 自己取、自己缓存，播放器还能靠 Range 请求拖进度条。前端不再需要手动取回 Blob 再转 object URL。</p>
 */

/**
 * 常见扩展名对应的内容类型。
 *
 * <p>文件消息正文里只有文件名、密钥与对象键，没有 MIME；接口的 {@code contentType} 又是可选的，
 * 所以这里按扩展名猜一个——猜不到就不传，服务端按二进制流处理（浏览器一般也能靠内容嗅探渲染）。</p>
 */
const CONTENT_TYPES: Record<string, string> = {
  png: 'image/png',
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  gif: 'image/gif',
  webp: 'image/webp',
  bmp: 'image/bmp',
  svg: 'image/svg+xml',
  avif: 'image/avif',
  mp4: 'video/mp4',
  webm: 'video/webm',
  mkv: 'video/x-matroska',
  mov: 'video/quicktime',
  avi: 'video/x-msvideo',
  mp3: 'audio/mpeg',
  wav: 'audio/wav',
  ogg: 'audio/ogg',
  oga: 'audio/ogg',
  m4a: 'audio/mp4',
  aac: 'audio/aac',
  flac: 'audio/flac',
  pdf: 'application/pdf',
  txt: 'text/plain',
}

/**
 * 按文件名猜内容类型。
 *
 * @param filename 文件名
 * @returns 内容类型，猜不到时返回空串
 */
function contentTypeOf(filename: string): string {
  const index = filename.lastIndexOf('.')
  if (index < 0 || index === filename.length - 1) {
    return ''
  }
  return CONTENT_TYPES[filename.slice(index + 1).toLowerCase()] ?? ''
}

/**
 * 拼出取回聊天文件的地址。
 *
 * @param file 文件消息正文（文件名、密钥、对象键）
 * @returns 可以直接交给 {@code <img>}、{@code <video>}、{@code <audio>} 或下载链接的地址
 */
export function chatFileUrl(file: ChatFileMessageVo): string {
  const params = new URLSearchParams({
    objectKey: file.objectKey,
    aesKey: file.aesKey,
    filename: file.filename,
  })
  const contentType = contentTypeOf(file.filename)
  if (contentType.length > 0) {
    params.set('contentType', contentType)
  }
  return `/api/file/chat?${params.toString()}`
}
