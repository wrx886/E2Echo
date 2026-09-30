import { ref } from 'vue'
import { defineStore } from 'pinia'
import { connectNotice } from '@/api'
import { useConversationStore } from './conversation'
import { useMessageStore } from './message'
import { showError } from '@/utils/feedback'

/** 收到通知后合并刷新的等待时间（毫秒）。 */
const REFRESH_DELAY = 300

/**
 * 通知通道。
 *
 * <p>客户端拉到新消息后会通过 SSE 推一条 {@code reflash}，这里收到后重新拉取会话列表与当前会话的
 * 消息。通知可能连着来，所以合并成一次刷新。</p>
 */
export const useNoticeStore = defineStore('notice', () => {

  /** 是否已连接。 */
  const connected = ref(false)

  /** 关闭连接的方法，为空表示尚未连接。 */
  let dispose: (() => void) | null = null

  /** 待执行的刷新定时器。 */
  let timer: ReturnType<typeof setTimeout> | null = null

  /**
   * 收到通知：合并成一次刷新。
   */
  function onReflash(): void {
    if (timer !== null) {
      return
    }
    timer = setTimeout(() => {
      timer = null
      void refresh()
    }, REFRESH_DELAY)
  }

  /**
   * 刷新会话列表与当前会话的消息。
   */
  async function refresh(): Promise<void> {
    try {
      await useConversationStore().refresh()
    } catch (error) {
      showError(error, '刷新会话列表失败')
    }
    try {
      await useMessageStore().refreshLatest()
    } catch (error) {
      showError(error, '刷新消息失败')
    }
  }

  /**
   * 建立通知连接，重复调用只生效一次。
   */
  function start(): void {
    if (dispose) {
      return
    }
    dispose = connectNotice({
      onOpen: () => {
        connected.value = true
      },
      onError: () => {
        connected.value = false
      },
      onReflash,
    })
  }

  /**
   * 断开通知连接并清掉待执行的刷新。
   */
  function stop(): void {
    dispose?.()
    dispose = null
    connected.value = false
    if (timer !== null) {
      clearTimeout(timer)
      timer = null
    }
  }

  return { connected, start, stop }
})
