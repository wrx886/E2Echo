import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getConversationAlias, listConversations, saveConversation } from '@/api'
import type { ConversationDto } from '@/api'

/** 会话列表每页条数。 */
const PAGE_SIZE = 50

/**
 * 会话列表。
 *
 * <p>数据来自会话列表接口，按更新时间倒序；翻页是触底追加，刷新则回到第一页。</p>
 */
export const useConversationStore = defineStore('conversation', () => {

  /** 已经加载的会话，按更新时间倒序。 */
  const list = ref<ConversationDto[]>([])

  /** 是否正在请求。 */
  const loading = ref(false)

  /** 是否还有更多会话。 */
  const hasMore = ref(false)

  /** 当前已加载到第几页，从 1 开始。 */
  const pageNum = ref(0)

  /** 是否已经加载过（用于区分“还没加载”和“加载完是空的”）。 */
  const loaded = ref(false)

  /** 当前选中的会话对方。 */
  const currentPeer = ref('')

  /** 当前选中的会话。 */
  const current = computed(() =>
    list.value.find((item) => item.peer === currentPeer.value) ?? null)

  /**
   * 会话对方 -> 别名缓存。
   *
   * <p>别名存在会话记录上，展示消息里的群名、群成员名称时都要用它。值为 null 表示查过但没有，
   * 免得反复请求；请求失败不缓存，下次还能重试。</p>
   */
  const aliasCache = ref(new Map<string, string | null>())

  /**
   * 取会话对方的别名：先看缓存，再看已经加载的会话列表，最后按公钥单独查一次。
   *
   * @param peer 会话对方：私聊时为对方公钥、群聊时为群聊标识
   * @returns 别名，没有对应会话或没有别名时返回 null
   */
  async function resolveAlias(peer: string): Promise<string | null> {
    if (aliasCache.value.has(peer)) {
      return aliasCache.value.get(peer) ?? null
    }
    const conversation = list.value.find((item) => item.peer === peer)
    if (conversation) {
      aliasCache.value.set(peer, conversation.alias)
      return conversation.alias
    }
    try {
      const alias = await getConversationAlias(peer)
      aliasCache.value.set(peer, alias)
      return alias
    } catch {
      return null
    }
  }

  /**
   * 同步取会话对方的别名，供模板直接使用。
   *
   * @param peer 会话对方
   * @returns 别名，缓存与已加载的会话里都没有时返回 null
   */
  function aliasOf(peer: string): string | null {
    const cached = aliasCache.value.get(peer)
    if (cached) {
      return cached
    }
    return list.value.find((item) => item.peer === peer)?.alias ?? null
  }

  /**
   * 取某一页并写入列表。
   *
   * @param page   页码，从 1 开始
   * @param append 是否追加到现有列表（否则替换）
   */
  async function fetchPage(page: number, append: boolean): Promise<void> {
    loading.value = true
    try {
      const result = await listConversations(page, PAGE_SIZE)
      if (append) {
        // 翻页期间列表可能已经变化，按 peer 去重，避免同一条会话出现两次
        const known = new Set(list.value.map((item) => item.peer))
        list.value = [...list.value, ...result.content.filter((item) => !known.has(item.peer))]
      } else {
        list.value = result.content
      }
      pageNum.value = page
      hasMore.value = result.content.length === PAGE_SIZE
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  /**
   * 加载第一页，已经有数据时不重复加载。
   */
  async function loadIfNeeded(): Promise<void> {
    if (loaded.value || loading.value) {
      return
    }
    await fetchPage(1, false)
  }

  /**
   * 重新加载：回到第一页。
   */
  async function refresh(): Promise<void> {
    await fetchPage(1, false)
  }

  /**
   * 加载下一页（触底时调用）。
   */
  async function loadMore(): Promise<void> {
    if (loading.value || !hasMore.value) {
      return
    }
    await fetchPage(pageNum.value + 1, true)
  }

  /**
   * 保存会话，并刷新列表。
   *
   * @param conversation 待保存的会话
   */
  async function save(conversation: ConversationDto): Promise<void> {
    await saveConversation(conversation)
    // 别名可能改了，缓存跟着更新，免得消息里的群名、成员名还是旧的
    aliasCache.value.set(conversation.peer, conversation.alias)
    await refresh()
  }

  /**
   * 设置当前选中的会话。
   *
   * @param peer 会话对方
   */
  function select(peer: string): void {
    currentPeer.value = peer
  }

  return {
    list, loading, hasMore, loaded, currentPeer, current,
    aliasCache, aliasOf, resolveAlias,
    loadIfNeeded, refresh, loadMore, save, select,
  }
})
