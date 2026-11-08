<script setup lang="ts">
import { computed, nextTick, onUnmounted, provide, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Setting } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { chatTextBodyOf, getConversation } from '@/api'
import type { ConversationDto, MessageVo } from '@/api'
import ChatUserCard from '@/components/ChatUserCard.vue'
import MessageBubble from '@/components/MessageBubble.vue'
import MessageComposer from '@/components/MessageComposer.vue'
import { useConversationStore } from '@/stores/conversation'
import { useMessageStore } from '@/stores/message'
import { useUserStore } from '@/stores/user'
import { conversationTitle, formatTime, timestampFromId } from '@/utils/display'
import { CHAT_PIN_TO_BOTTOM } from '@/utils/chatScroll'
import { showError } from '@/utils/feedback'

/**
 * 聊天页面。
 *
 * <p>消息按本地序号升序展示，向上滚动加载更早的消息；发送成功后重新拉取最新一页并合并。</p>
 */

/** 距底部小于该距离时视为“贴着底部”，新消息到达才自动滚动。 */
const NEAR_BOTTOM = 80

/** 距顶部小于该距离时加载更早的消息。 */
const NEAR_TOP = 24

/** 相邻消息间隔超过该值时插入时间分隔。 */
const TIME_GAP = 5 * 60 * 1000

const route = useRoute()
const router = useRouter()
const conversationStore = useConversationStore()
const messageStore = useMessageStore()
const userStore = useUserStore()

/** 当前会话对方。 */
const peer = computed(() => String(route.params.peer ?? ''))

/** 消息滚动容器。 */
const listEl = ref<HTMLDivElement>()

/** 消息内容容器，用来观察内容高度变化。 */
const contentEl = ref<HTMLDivElement>()

/** 输入框内容。 */
const draft = ref('')

/** 输入区实例，选中引用后把焦点放回输入框。 */
const composerRef = ref<InstanceType<typeof MessageComposer>>()

/** 正在引用的消息；为空表示不引用。 */
const quoteTarget = ref<MessageVo | null>(null)

/** 刚从引用块定位到的消息 ID，用于短暂高亮。 */
const highlightMessageId = ref('')

/** 高亮自动消失的定时器。 */
let highlightTimer: ReturnType<typeof setTimeout> | null = null

/** 会话列表里还没有该会话时单独查到的会话。 */
const fallbackConversation = ref<ConversationDto | null>(null)

/** 会话不存在。 */
const missing = ref(false)

/**
 * 用户小卡片的目标人员与锚点。
 *
 * <p>整页只创建一个 popover，头像和 @ 别名都复用它，避免每条消息各挂一套定位组件。</p>
 */
const cardTarget = ref<{ uid: string; anchor: HTMLElement } | null>(null)

/** 用户小卡片是否显示。 */
const cardVisible = ref(false)

/** 正在恢复滚动位置，期间不触发自动滚到底部。 */
const restoring = ref(false)

/**
 * 是否贴着底部。
 *
 * <p>贴底时内容变高要跟着滚到底——最典型的就是图片：图片是进入视野后才去取的，取回前只有占位，
 * 取回后气泡变高，不处理的话刚打开的会话就会停在中途，而不是最新一条消息。</p>
 */
let stickyBottom = true

/** 内容高度观察器。 */
let contentObserver: ResizeObserver | null = null

/** 当前会话：优先用列表里的，列表里没有时用单独查到的。 */
const conversation = computed(() =>
  conversationStore.list.find((item) => item.peer === peer.value) ?? fallbackConversation.value)

/** 会话展示名称。 */
const title = computed(() => conversationTitle(conversation.value, peer.value))

/** 是否群聊会话。 */
const group = computed(() => conversation.value?.group === true)

/** 已加载消息按 ID 建立索引，供引用摘要与定位使用。 */
const messageLookup = computed(() =>
  new Map(messageStore.messages.map((message) => [message.id, message])))

/**
 * 展示条目：消息与时间分隔。
 */
interface TimeItem {
  kind: 'time'
  key: string
  text: string
}

interface MessageItem {
  kind: 'message'
  key: string
  message: MessageVo
}

type ChatItem = TimeItem | MessageItem

/** 按消息间隔插入时间分隔后的展示条目。 */
const items = computed<ChatItem[]>(() => {
  const result: ChatItem[] = []
  let previous = 0
  for (const message of messageStore.messages) {
    // 消息 ID 前 16 位是生成时间，MessageVo 本身没有时间字段
    const timestamp = timestampFromId(message.id)
    if (!Number.isNaN(timestamp)) {
      if (previous === 0 || timestamp - previous > TIME_GAP) {
        result.push({ kind: 'time', key: `time-${message.id}`, text: formatTime(timestamp) })
      }
      previous = timestamp
    }
    result.push({ kind: 'message', key: message.id, message })
  }
  return result
})

/**
 * 判断一条消息是不是自己发的。
 *
 * @param message 消息
 * @returns 是自己发的返回 true
 */
function isMine(message: MessageVo): boolean {
  return userStore.publicKey.length > 0 && message.from === userStore.publicKey
}

/**
 * 取消息滚动容器。
 *
 * @returns 滚动容器，尚未渲染时返回 null
 */
function scroller(): HTMLDivElement | null {
  return listEl.value ?? null
}

/**
 * 判断是否贴着底部。
 *
 * @returns 贴着底部（或容器还没渲染）返回 true
 */
function isNearBottom(): boolean {
  const el = scroller()
  return el === null || el.scrollHeight - el.scrollTop - el.clientHeight < NEAR_BOTTOM
}

/**
 * 滚动到底部。
 */
async function scrollToBottom(): Promise<void> {
  stickyBottom = true
  await nextTick()
  const el = scroller()
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

/**
 * 加载会话：列表里没有时单独查一次。
 *
 * @param value 会话对方
 */
async function loadConversation(value: string): Promise<void> {
  if (conversationStore.list.some((item) => item.peer === value)) {
    return
  }
  try {
    const result = await getConversation(value)
    fallbackConversation.value = result
    missing.value = result === null
  } catch (error) {
    showError(error, '加载会话失败')
  }
}

/**
 * 加载更早的消息，并保持滚动位置不变。
 */
async function loadOlder(): Promise<void> {
  const el = scroller()
  if (el === null || messageStore.loading || messageStore.loadingOlder || !messageStore.hasMore) {
    return
  }

  const previousHeight = el.scrollHeight
  const previousTop = el.scrollTop
  restoring.value = true

  try {
    await messageStore.loadOlder()
  } catch (error) {
    showError(error, '加载更早的消息失败')
  } finally {
    await nextTick()
    // 插入到顶部后内容变高，把滚动位置同步往后挪，视觉上停在原来的消息上
    el.scrollTop = previousTop + (el.scrollHeight - previousHeight)
    restoring.value = false
  }
}

/**
 * 滚动到顶部时加载更早的消息。
 */
function onScroll(): void {
  const el = scroller()
  if (!el) {
    return
  }
  stickyBottom = isNearBottom()
  if (el.scrollTop <= NEAR_TOP) {
    void loadOlder()
  }
}

/**
 * 观察消息内容的高度：内容变高（图片取回、文字换行等）且此前贴着底部时，继续贴到底部。
 *
 * <p>向上翻历史时 {@link stickyBottom} 已经是 false，prepend 更早的消息不会把视图拽回底部。</p>
 *
 * @param content 内容容器
 */
function observeContent(content: HTMLDivElement): void {
  if (typeof ResizeObserver === 'undefined') {
    return
  }
  contentObserver = new ResizeObserver(() => {
    if (restoring.value) {
      return
    }
    pinToBottomIfSticky()
  })
  contentObserver.observe(content)
}

/**
 * 把视图贴回底部：只在用户没有主动往上翻的时候动。
 *
 * <p>提供给下层的内容组件（例如消息里的图片）在布局完成后调用，作为 ResizeObserver 之外的第二条
 * 通路——图片来源不同、取回时机也不同，只靠观察内容高度容易漏掉「最后一次长高」。</p>
 */
function pinToBottomIfSticky(): void {
  if (!stickyBottom) {
    return
  }
  const el = scroller()
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

provide(CHAT_PIN_TO_BOTTOM, pinToBottomIfSticky)

/**
 * 打开用户小卡片：头像传消息发送者，@ 别名传被提及者。
 *
 * @param target 目标人员与点击锚点
 */
function openUserCard(target: { uid: string; anchor: HTMLElement }): void {
  cardTarget.value = target
  cardVisible.value = true
}

/**
 * 同步 popover 的显隐状态。
 *
 * @param visible 是否显示
 */
function onUserCardVisibleChange(visible: boolean): void {
  cardVisible.value = visible
  if (!visible) {
    cardTarget.value = null
  }
}

/**
 * 主动关闭用户小卡片。
 */
function closeUserCard(): void {
  cardVisible.value = false
  cardTarget.value = null
}

/**
 * 取一条消息引用的原消息。
 *
 * @param message 当前消息
 * @returns 原消息；没有引用或原消息还没加载时返回 null
 */
function quotedMessageOf(message: MessageVo): MessageVo | null {
  const id = chatTextBodyOf(message)?.quote
  return id ? messageLookup.value.get(id) ?? null : null
}

/**
 * 引用一条文字消息，并把焦点放回输入框。
 *
 * @param message 被引用的消息
 */
function startQuote(message: MessageVo): void {
  quoteTarget.value = message
  void nextTick(() => composerRef.value?.focus())
}

/**
 * 取消当前引用。
 */
function cancelQuote(): void {
  quoteTarget.value = null
}

/**
 * 短暂高亮一条消息。
 *
 * @param messageId 消息 ID
 */
function flashMessage(messageId: string): void {
  if (highlightTimer !== null) {
    clearTimeout(highlightTimer)
  }
  highlightMessageId.value = messageId
  highlightTimer = setTimeout(() => {
    highlightTimer = null
    highlightMessageId.value = ''
  }, 1600)
}

/**
 * 定位被引用的原消息；当前页之后的历史还没加载时，逐页向前补齐。
 *
 * @param messageId 被引用消息的 ID
 */
async function openQuotedMessage(messageId: string): Promise<void> {
  let target = messageLookup.value.get(messageId) ?? null
  try {
    while (!target && messageStore.hasMore && !messageStore.loadingOlder) {
      const previousLength = messageStore.messages.length
      await messageStore.loadOlder()
      target = messageLookup.value.get(messageId) ?? null
      if (messageStore.messages.length === previousLength) {
        break
      }
    }
  } catch (error) {
    showError(error, '加载被引用的消息失败')
    return
  }

  if (!target) {
    ElMessage.warning('没有找到被引用的消息')
    return
  }

  await nextTick()
  const element = scroller()?.querySelector<HTMLElement>(`[data-message-id="${messageId}"]`)
  if (!element) {
    return
  }
  flashMessage(messageId)
  element.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

// 用 ref 的 watch 而不是 onMounted：会话从「不存在」切到正常会话时，列表容器是后出现的
watch(contentEl, (element) => {
  contentObserver?.disconnect()
  contentObserver = null
  if (element) {
    observeContent(element)
  }
}, { flush: 'post' })

onUnmounted(() => {
  contentObserver?.disconnect()
  contentObserver = null
  if (highlightTimer !== null) {
    clearTimeout(highlightTimer)
  }
  // 离开聊天页后不能再把最后打开的会话当成“正在查看”：否则通知刷新会继续拉取并清零它的未读。
  messageStore.reset()
})

/**
 * 发送输入框中的消息。
 */
async function onSend(): Promise<void> {
  const text = draft.value.trim()
  if (text.length === 0 || messageStore.sending || peer.value.length === 0) {
    return
  }
  try {
    await messageStore.send(text, group.value, quoteTarget.value?.id)
    draft.value = ''
    quoteTarget.value = null
    await scrollToBottom()
  } catch (error) {
    // 发送失败保留输入内容，方便直接重试
    showError(error, '发送失败')
  }
}

/**
 * 发送文件：交给 client 加密、上传并发出，成功后刷新当前会话。
 *
 * @param file 待发送的文件
 */
async function onSendFile(file: File): Promise<void> {
  try {
    await messageStore.sendFile(file, group.value)
    await scrollToBottom()
  } catch (error) {
    showError(error, '文件发送失败')
  }
}

/**
 * 发送图片：与文件同一套流程，成功后刷新当前会话。
 *
 * @param file 待发送的图片
 */
async function onSendImage(file: File): Promise<void> {
  try {
    await messageStore.sendImage(file, group.value)
    await scrollToBottom()
  } catch (error) {
    showError(error, '图片发送失败')
  }
}

/**
 * 发送视频：与文件同一套流程，成功后刷新当前会话。
 *
 * @param file 待发送的视频
 */
async function onSendVideo(file: File): Promise<void> {
  try {
    await messageStore.sendVideo(file, group.value)
    await scrollToBottom()
  } catch (error) {
    showError(error, '视频发送失败')
  }
}

/**
 * 发送音频：与文件同一套流程，成功后刷新当前会话。
 *
 * @param file 待发送的音频
 */
async function onSendAudio(file: File): Promise<void> {
  try {
    await messageStore.sendAudio(file, group.value)
    await scrollToBottom()
  } catch (error) {
    showError(error, '音频发送失败')
  }
}

// 消息变化（收到通知、发送成功）时，贴着底部才自动滚到底
watch(() => messageStore.messages, () => {
  if (restoring.value) {
    return
  }
  const near = isNearBottom()
  void nextTick(() => {
    const el = scroller()
    if (near && el) {
      el.scrollTop = el.scrollHeight
    }
  })
})

// 会话变化：切换消息列表并滚到底部
watch(peer, async (value) => {
  // 先把当前这条会话记下来：搜索会话时列表里可能就查不到它了，标题和「群 / 已停用」标记不能跟着丢
  fallbackConversation.value =
    conversationStore.list.find((item) => item.peer === value) ?? null
  missing.value = false
  // 换会话时把上一会话开着的小卡片收掉
  closeUserCard()
  quoteTarget.value = null
  highlightMessageId.value = ''
  if (value.length === 0) {
    messageStore.reset()
    return
  }
  conversationStore.select(value)
  await loadConversation(value)
  try {
    if (messageStore.peer !== value) {
      await messageStore.open(value)
    } else {
      // 回到同一个会话：可能是从别的页面回来的，期间收到过消息（例如群密钥分发会私聊发密钥），
      // 补拉一次最新消息，免得看到的是离开前的旧内容
      await messageStore.refreshLatest()
    }
    await scrollToBottom()
    // 拉消息时客户端会顺手把该会话的未读清零，列表里的红点与总数要跟着更新
    await conversationStore.markRead(value)
  } catch (error) {
    showError(error, '加载消息失败')
  }
}, { immediate: true })
</script>

<template>
  <section class="chat">
    <el-empty v-if="missing" description="会话不存在">
      <el-button @click="router.push({ name: 'chat-empty' })">返回</el-button>
    </el-empty>

    <template v-else>
      <header class="chat__header">
        <span class="chat__title">{{ title }}</span>
        <el-tag v-if="group" size="small" type="success" effect="plain">群</el-tag>
        <el-tag v-if="conversation && !conversation.enabled" size="small" type="info">已停用</el-tag>
        <el-button
          class="chat__setting"
          :icon="Setting"
          circle
          size="small"
          title="会话设置"
          @click="router.push({ name: 'conversation-settings', params: { peer } })"
        />
      </header>

      <div ref="listEl" class="chat__messages scroll-y" @scroll="onScroll">
        <div ref="contentEl" class="chat__list">
          <el-skeleton v-if="messageStore.loading" :rows="6" animated />

          <template v-else>
            <div v-if="messageStore.loadingOlder" class="chat__tip">加载中…</div>
            <div v-else-if="!messageStore.hasMore && messageStore.messages.length > 0" class="chat__tip">
              没有更早的消息了
            </div>

            <div v-if="messageStore.messages.length === 0" class="chat__tip">还没有消息，打个招呼吧</div>

            <template v-for="item in items" :key="item.key">
              <div v-if="item.kind === 'time'" class="chat__time">{{ item.text }}</div>
              <MessageBubble
                v-else-if="item.message"
                :message="item.message"
                :mine="isMine(item.message)"
                :group="group"
                :mine-avatar="userStore.avatarText"
                :quoted-message="quotedMessageOf(item.message)"
                :highlighted="item.message.id === highlightMessageId"
                @user-card-click="openUserCard"
                @quote="startQuote"
                @open-quote="openQuotedMessage"
              />
            </template>
          </template>
        </div>
      </div>

      <MessageComposer
        ref="composerRef"
        v-model="draft"
        :sending="messageStore.sending"
        :sending-file="messageStore.sendingFile"
        :file-percent="messageStore.fileProgress"
        :peer="peer"
        :peer-name="title"
        :mention-enabled="conversation?.group === false"
        :quoted-message="quoteTarget"
        @mention-click="openUserCard"
        @cancel-quote="cancelQuote"
        @submit="onSend"
        @submit-file="onSendFile"
        @submit-image="onSendImage"
        @submit-video="onSendVideo"
        @submit-audio="onSendAudio"
      />

      <el-popover
        v-if="cardTarget"
        :visible="cardVisible"
        :virtual-ref="cardTarget.anchor"
        virtual-triggering
        :width="300"
        placement="top-start"
        @update:visible="onUserCardVisibleChange"
      >
        <ChatUserCard :uid="cardTarget.uid" @close="closeUserCard" />
      </el-popover>
    </template>
  </section>
</template>

<style scoped>
.chat {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  background: #f5f5f5;
}

.chat__header {
  flex: none;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.chat__title {
  font-size: 15px;
  font-weight: 600;
}

.chat__setting {
  margin-left: auto;
}

.chat__messages {
  flex: 1;
  min-height: 0;
}

.chat__list {
  display: flex;
  flex-direction: column;
  padding: 12px 0;
}

.chat__tip {
  padding: 6px 0;
  font-size: 12px;
  color: #a8abb2;
  text-align: center;
}

.chat__time {
  padding: 8px 0;
  font-size: 12px;
  color: #a8abb2;
  text-align: center;
}
</style>
