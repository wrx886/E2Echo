import { downloadChatFile } from '@/api'
import type { ChatFileMessageVo } from '@/api'

/**
 * 聊天媒体（图片、视频、音频）的取回与缓存。
 *
 * <p>媒体字节要 POST 文件消息正文给 client 才能拿到，不能直接当 URL 用，所以取回后转成 object URL
 * 给 {@code <img>}、{@code <video>}、{@code <audio>} 使用。字节本身也缓存：下载原件时直接复用，不用
 * 再取一次。同一个对象键只取一次：取过的缓存起来复用，正在取的复用同一个 Promise，避免同一份内容
 * 被并发取多遍。</p>
 *
 * <p>缓存不做淘汰：只缓存用户实际看到过的内容，页面刷新即释放；翻历史来回滚动时不会反复取回。</p>
 */

/** 对象键 -> 媒体字节。 */
const blobCache = new Map<string, Blob>()

/** 对象键 -> 媒体地址。 */
const urlCache = new Map<string, string>()

/** 对象键 -> 正在进行的请求。 */
const pending = new Map<string, Promise<Blob>>()

/**
 * 取媒体字节：缓存里有就直接用，正在取就等同一个请求，都没有才去 client 取。
 *
 * @param file 媒体消息正文（文件名、密钥、对象键）
 * @returns 媒体字节
 */
export async function loadMediaBlob(file: ChatFileMessageVo): Promise<Blob> {
  const cached = blobCache.get(file.objectKey)
  if (cached) {
    return cached
  }

  const running = pending.get(file.objectKey)
  if (running) {
    return running
  }

  const task = (async () => {
    const blob = await downloadChatFile(file)
    blobCache.set(file.objectKey, blob)
    return blob
  })()

  pending.set(file.objectKey, task)
  try {
    return await task
  } finally {
    pending.delete(file.objectKey)
  }
}

/**
 * 取媒体地址：同一个对象键只建一次 object URL。
 *
 * @param file 媒体消息正文（文件名、密钥、对象键）
 * @returns 可用于 {@code <img src>}、{@code <video src>}、{@code <audio src>} 的地址
 */
export async function loadMediaUrl(file: ChatFileMessageVo): Promise<string> {
  const cached = urlCache.get(file.objectKey)
  if (cached) {
    return cached
  }

  const blob = await loadMediaBlob(file)
  // 并发取同一份内容时，这里可能已经有人建好了地址，直接复用
  const created = urlCache.get(file.objectKey)
  if (created) {
    return created
  }

  const url = URL.createObjectURL(blob)
  urlCache.set(file.objectKey, url)
  return url
}
