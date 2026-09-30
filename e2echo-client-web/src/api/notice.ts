/**
 * 通知接口，对应 {@code NoticeController}。
 *
 * <p>前端用 SSE 连上客户端，之后客户端拉到新消息、会话发生变化时推一条 {@link NOTICE_REFLASH}，
 * 收到后重新拉取会话与消息即可。推送内容不代表具体数据，只表示“数据可能变了”。</p>
 */

/** 服务端推送的内容：数据可能变了，需要重新拉取。 */
export const NOTICE_REFLASH = 'reflash'

/** 通知连接地址。 */
const NOTICE_URL = '/notice'

/**
 * 通知回调。
 */
export interface NoticeHandlers {
  /**
   * 收到通知时调用。
   *
   * @param data 服务端推送的内容，通常为 {@link NOTICE_REFLASH}
   */
  onReflash?: (data: string) => void
  /** 连接建立时调用，可用于清除“连接断开”的提示。 */
  onOpen?: () => void
  /**
   * 连接出错时调用。
   *
   * <p>会话失效时客户端返回的是统一响应 JSON（HTTP 200），SSE 会当作错误处理，因此这里也是
   * 发现会话失效的时机之一；调用方可以借此再发一次普通请求确认。</p>
   */
  onError?: (event: Event) => void
}

/**
 * 建立通知连接。
 *
 * <p>直接使用浏览器的 {@code EventSource}：它会在连接断开（客户端主动断开超过 30 分钟的连接、
 * 进程重启等）后自动重连，不需要前端自己维护重连逻辑。连接与页面同源，会话 Cookie 由浏览器
 * 自动携带，前端不需要处理身份信息。</p>
 *
 * @param handlers 通知回调
 * @returns 关闭连接的方法
 */
export function connectNotice(handlers: NoticeHandlers = {}): () => void {

  const source = new EventSource(NOTICE_URL)

  source.onopen = () => {
    handlers.onOpen?.()
  }

  source.onmessage = (event: MessageEvent<string>) => {
    if (event.data) {
      handlers.onReflash?.(event.data)
    }
  }

  source.onerror = (event: Event) => {
    handlers.onError?.(event)
  }

  return () => source.close()
}
