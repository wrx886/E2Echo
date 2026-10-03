/**
 * 把二进制内容交给浏览器保存成文件。
 *
 * @param blob     文件内容
 * @param filename 保存时的文件名
 */
export function saveBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)

  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileNameOf(filename)
  anchor.style.display = 'none'
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)

  // 立刻释放会让部分浏览器来不及取数据，延后一点再释放
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}

/**
 * 只取文件名部分。
 *
 * <p>文件名来自消息正文，可能带路径分隔符，交给 {@code download} 属性前先削掉，避免出现意外的
 * 目录结构；都取不到时用一个兜底名字。</p>
 *
 * @param filename 原始文件名
 * @returns 可以安全用于保存的文件名
 */
function fileNameOf(filename: string): string {
  const name = filename.split(/[\\/]/).pop()?.trim() ?? ''
  return name.length > 0 ? name : 'download'
}
