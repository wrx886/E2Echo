/**
 * 复制文本到剪贴板。
 *
 * <p>优先用异步剪贴板接口；网页由客户端在 localhost 上提供，属于安全上下文，正常情况走的就是它。
 * 取不到时退回“临时输入框 + execCommand”的老办法，保证在内嵌浏览器里也能用。</p>
 *
 * @param text 待复制的文本
 * @returns 复制成功返回 true
 */
export async function copyText(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard) {
      await navigator.clipboard.writeText(text)
      return true
    }
  } catch {
    // 落到下面的兜底方案
  }

  try {
    const area = document.createElement('textarea')
    area.value = text
    // 固定定位 + 透明，避免复制时页面滚动或闪烁
    area.style.position = 'fixed'
    area.style.top = '0'
    area.style.opacity = '0'
    document.body.appendChild(area)
    area.select()
    const copied = document.execCommand('copy')
    document.body.removeChild(area)
    return copied
  } catch {
    return false
  }
}
