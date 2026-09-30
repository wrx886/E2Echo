import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { listConversations, saveConversation } from '@/api'
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
    loadIfNeeded, refresh, loadMore, save, select,
  }
})
