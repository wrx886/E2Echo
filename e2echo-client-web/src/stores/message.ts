import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  findConversationMessages,
  sendFileMessage,
  sendImageMessage,
  sendTextMessage,
} from '@/api'
import type { MessageVo } from '@/api'

/** 每次加载的消息条数。 */
const PAGE_SIZE = 20

/**
 * 当前会话的消息。
 *
 * <p>一次只维护一个会话：消息按本地序号 seq 升序保存，向上翻页用最旧一条的 seq 作为游标。</p>
 */
export const useMessageStore = defineStore('message', () => {

  /** 当前会话对方。 */
  const peer = ref('')

  /** 消息，按 seq 升序。 */
  const messages = ref<MessageVo[]>([])

  /** 是否正在加载首屏。 */
  const loading = ref(false)

  /** 是否正在加载更早的消息。 */
  const loadingOlder = ref(false)

  /** 是否还有更早的消息。 */
  const hasMore = ref(false)

  /** 是否正在发送。 */
  const sending = ref(false)

  /** 是否正在发送文件。 */
  const sendingFile = ref(false)

  /** 文件上传进度，0-100。 */
  const fileProgress = ref(0)

  /**
   * 会话切换标记：异步请求返回时用它判断结果是否还属于当前会话，避免旧请求写回新会话的消息列表。
   */
  let token = 0

  /**
   * 把一批消息合并进当前列表。
   *
   * <p>按消息 ID 去重（拉取会重复拿到已经存在的消息），再按 seq 升序排列。</p>
   *
   * @param incoming 新拿到的消息
   */
  function merge(incoming: MessageVo[]): void {
    const map = new Map<string, MessageVo>()
    for (const message of messages.value) {
      map.set(message.id, message)
    }
    for (const message of incoming) {
      map.set(message.id, message)
    }
    messages.value = [...map.values()].sort((a, b) => a.seq - b.seq)
  }

  /**
   * 打开一个会话：切换对方并加载最新一页消息。
   *
   * <p>已经是当前会话时不重复加载，避免每次路由变化都重新拉取。</p>
   *
   * @param newPeer 会话对方
   */
  async function open(newPeer: string): Promise<void> {
    if (!newPeer || peer.value === newPeer) {
      return
    }

    const myToken = ++token
    peer.value = newPeer
    messages.value = []
    hasMore.value = false
    loading.value = true

    try {
      const page = await findConversationMessages(newPeer, 1, PAGE_SIZE)
      if (myToken !== token) {
        return
      }
      // 接口返回的是最新的在前，展示要的是最早的在前
      messages.value = [...page.content].reverse()
      hasMore.value = page.content.length === PAGE_SIZE
    } finally {
      if (myToken === token) {
        loading.value = false
      }
    }
  }

  /**
   * 加载更早的消息。
   */
  async function loadOlder(): Promise<void> {
    const oldest = messages.value[0]
    if (!peer.value || !oldest || loadingOlder.value || !hasMore.value) {
      return
    }

    const myToken = token
    loadingOlder.value = true

    try {
      const page = await findConversationMessages(peer.value, 1, PAGE_SIZE, oldest.seq)
      if (myToken !== token) {
        return
      }
      const known = new Set(messages.value.map((message) => message.id))
      const older = page.content.filter((message) => !known.has(message.id)).reverse()
      messages.value = [...older, ...messages.value]
      hasMore.value = page.content.length === PAGE_SIZE
    } finally {
      if (myToken === token) {
        loadingOlder.value = false
      }
    }
  }

  /**
   * 重新拉取最新一页并合并进当前列表。
   *
   * <p>收到通知、发送消息后调用：新消息合并进来，已有消息按 ID 覆盖，不打断已经翻上去的历史。</p>
   */
  async function refreshLatest(): Promise<void> {
    if (!peer.value) {
      return
    }
    const myToken = token
    const page = await findConversationMessages(peer.value, 1, PAGE_SIZE)
    if (myToken !== token) {
      return
    }
    merge(page.content)
  }

  /**
   * 发送文字消息，并把结果合并回列表。
   *
   * @param text  正文
   * @param group 是否群聊会话
   */
  async function send(text: string, group: boolean): Promise<void> {
    const to = peer.value
    if (!to) {
      return
    }
    sending.value = true
    try {
      await sendTextMessage({ to, group, text })
      await refreshLatest()
    } finally {
      sending.value = false
    }
  }

  /**
   * 发送带附件的消息：文件与图片的上传流程相同，只是调用的接口不同。
   *
   * <p>内容先上传给 client，由 client 加密、上传对象存储后发消息；私聊发送时 client 会落本地，
   * 群聊要等通知触发拉取后才出现，和文字消息一致。</p>
   *
   * @param file    待发送的文件
   * @param group   是否群聊会话
   * @param request 实际调用的发送接口
   */
  async function sendAttachment(
    file: File,
    group: boolean,
    request: (
      to: string,
      group: boolean,
      file: File,
      onProgress: (percent: number) => void,
    ) => Promise<void>,
  ): Promise<void> {
    const to = peer.value
    if (!to) {
      return
    }
    sendingFile.value = true
    fileProgress.value = 0
    try {
      await request(to, group, file, (percent) => {
        fileProgress.value = percent
      })
      await refreshLatest()
    } finally {
      sendingFile.value = false
      fileProgress.value = 0
    }
  }

  /**
   * 发送文件消息。
   *
   * @param file  待发送的文件
   * @param group 是否群聊会话
   */
  async function sendFile(file: File, group: boolean): Promise<void> {
    await sendAttachment(file, group, sendFileMessage)
  }

  /**
   * 发送图片消息：流程与文件相同，client 会把类型标成图片消息，前端直接渲染。
   *
   * @param file  待发送的图片
   * @param group 是否群聊会话
   */
  async function sendImage(file: File, group: boolean): Promise<void> {
    await sendAttachment(file, group, sendImageMessage)
  }

  /**
   * 清空当前会话的消息。
   */
  function reset(): void {
    token += 1
    peer.value = ''
    messages.value = []
    hasMore.value = false
    loading.value = false
    loadingOlder.value = false
  }

  return {
    peer, messages, loading, loadingOlder, hasMore, sending, sendingFile, fileProgress,
    open, loadOlder, refreshLatest, send, sendFile, sendImage, reset,
  }
})
