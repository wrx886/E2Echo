import { downloadChatFile } from '@/api'
import type { ChatFileMessageVo } from '@/api'

/**
 * 聊天图片的取回与缓存。
 *
 * <p>图片字节要 POST 文件消息正文给 client 才能拿到，不能直接当 URL 用，所以取回后转成 object URL
 * 给 {@code <img>} 使用。字节本身也缓存：下载原图时直接复用，不用再取一次。同一个对象键只取一次：
 * 取过的缓存起来复用，正在取的复用同一个 Promise，避免同一张图被并发取多遍。</p>
 *
 * <p>缓存不做淘汰：只缓存用户实际看到过的图片，页面刷新即释放；翻历史来回滚动时不会反复取回。</p>
 */

/** 对象键 -> 图片字节。 */
const blobCache = new Map<string, Blob>()

/** 对象键 -> 图片地址。 */
const urlCache = new Map<string, string>()

/** 对象键 -> 正在进行的请求。 */
const pending = new Map<string, Promise<Blob>>()

/**
 * 取图片字节：缓存里有就直接用，正在取就等同一个请求，都没有才去 client 取。
 *
 * @param file 图片消息正文（文件名、密钥、对象键）
 * @returns 图片字节
 */
export async function loadImageBlob(file: ChatFileMessageVo): Promise<Blob> {
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
 * 取图片地址：同一个对象键只建一次 object URL。
 *
 * @param file 图片消息正文（文件名、密钥、对象键）
 * @returns 可用于 {@code <img src>} 的地址
 */
export async function loadImageUrl(file: ChatFileMessageVo): Promise<string> {
  const cached = urlCache.get(file.objectKey)
  if (cached) {
    return cached
  }

  const blob = await loadImageBlob(file)
  // 并发取同一张图时，这里可能已经有人建好了地址，直接复用
  const created = urlCache.get(file.objectKey)
  if (created) {
    return created
  }

  const url = URL.createObjectURL(blob)
  urlCache.set(file.objectKey, url)
  return url
}
