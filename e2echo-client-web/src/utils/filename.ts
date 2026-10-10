/**
 * 只取文件名部分。
 *
 * <p>文件名来自消息正文，可能带路径分隔符，交给下载链接的 {@code download} 属性前先削掉，避免出现
 * 意外的目录结构；都取不到时用一个兜底名字。</p>
 *
 * @param filename 原始文件名
 * @returns 可以安全用于保存的文件名
 */
export function safeFileName(filename: string): string {
  const name = filename.split(/[\\/]/).pop()?.trim() ?? ''
  return name.length > 0 ? name : 'download'
}
