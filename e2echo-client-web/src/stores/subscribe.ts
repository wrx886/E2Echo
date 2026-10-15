import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getBroadcastSubscribe,
  listBroadcastSubscribes,
  listSubscribeMessages,
  setBroadcastSubscribeAlias,
  subscribeBroadcastAuthor,
  unsubscribeBroadcastAuthor,
} from '@/api'
import type { BroadcastMessageRespVo, BroadcastSubscribeDto } from '@/api'

/** 订阅流每页条数。 */
const PAGE_SIZE = 20

/** 别名索引一次拉取的条数：订阅一般不多，一次拉完就不用给每张卡片单独查一次。 */
const ALIAS_INDEX_SIZE = 200

/**
 * 判断一条广播消息是否可用。
 *
 * <p>验签失败的条目在列表里是 {@code null} 占位，正文缺失的也没有可展示的内容。</p>
 *
 * @param item 列表里的一条
 * @returns 可用返回 true
 */
function isUsable(item: BroadcastMessageRespVo | null): item is BroadcastMessageRespVo {
  return item !== null && item.message !== null && item.message !== undefined
}

/**
 * 广播订阅。
 *
 * <p>订阅关系只存在 client 本地库，这里做两件事：一是维护"我订阅了谁、本地给他起了什么别名"的
 * 索引，供卡片、主页展示名字（订阅别名优先于资料昵称，这就是别名的用途）；二是订阅流本身的加载。</p>
 *
 * <p>索引用一次列表请求（一页拉满）灌进去，而不是每张卡片按作者查一次——时间线一屏就有十几个作者，
 * 按作者查会把请求量放大十几倍。</p>
 */
export const useSubscribeStore = defineStore('subscribe', () => {

  // ---------------- 订阅索引 ----------------

  /** 被订阅者公钥 -> 别名（空串表示没有别名）。 */
  const aliases = ref(new Map<string, string>())

  /** 是否已经加载过索引。 */
  const indexLoaded = ref(false)

  /** 正在加载索引。 */
  let indexTask: Promise<void> | null = null

  /**
   * 加载订阅索引：把订阅列表一次拉进来，供展示与判断"有没有订阅"用。
   *
   * <p>幂等：加载过就直接返回，正在加载时复用同一个请求。订阅数量超过一页时只索引到第一页，
   * 多出来的部分不显示别名（不影响订阅本身）。</p>
   *
   * @param force 是否强制重新加载
   */
  async function loadIndex(force = false): Promise<void> {
    if (indexLoaded.value && !force) {
      return
    }
    if (indexTask && !force) {
      return indexTask
    }
    indexTask = (async () => {
      const page = await listBroadcastSubscribes({
        pageNum: 1,
        pageSize: ALIAS_INDEX_SIZE,
        publicKeyOrAlias: '',
      })
      const next = new Map<string, string>()
      for (const item of page.content) {
        next.set(item.publicKey, item.alias?.trim() ?? '')
      }
      aliases.value = next
      indexLoaded.value = true
    })()
    try {
      await indexTask
    } finally {
      indexTask = null
    }
  }

  /**
   * 取某个作者的订阅别名。
   *
   * @param publicKey 作者公钥
   * @returns 别名，没有订阅或没设别名时返回空串
   */
  function aliasOf(publicKey: string): string {
    return aliases.value.get(publicKey) ?? ''
  }

  /**
   * 有没有订阅某个作者。
   *
   * <p>索引没加载时一律返回 false，所以主页上的订阅按钮自己还会查一次（见
   * {@link getBroadcastSubscribe}），不完全依赖索引。</p>
   *
   * @param publicKey 作者公钥
   * @returns 订阅过返回 true
   */
  function isSubscribed(publicKey: string): boolean {
    return aliases.value.has(publicKey)
  }

  /**
   * 把一条订阅写进索引（订阅、改别名后调用）。
   *
   * @param subscribe 订阅
   */
  function put(subscribe: BroadcastSubscribeDto): void {
    aliases.value.set(subscribe.publicKey, subscribe.alias?.trim() ?? '')
  }

  /**
   * 从索引里去掉一条订阅（取消订阅后调用）。
   *
   * @param publicKey 被订阅者公钥
   */
  function remove(publicKey: string): void {
    aliases.value.delete(publicKey)
  }

  // ---------------- 订阅流 ----------------

  /**
   * 订阅流里的帖子，按消息 ID 倒序。
   *
   * <p>client 拉取时会把当前用户自己也补进作者列表（空的作者列表等于不过滤），所以这里既有订阅作者的
   * 帖子，也有自己发的帖子。</p>
   */
  const posts = ref<BroadcastMessageRespVo[]>([])

  /** 订阅流是否正在加载。 */
  const loading = ref(false)

  /** 订阅流是否还有更多。 */
  const hasMore = ref(true)

  /** 订阅流是否需要重新拉取：订阅关系变了以后，流的内容可能不一样了。 */
  const stale = ref(true)

  /** 订阅流的定位锚点：从帖子详情/用户主页返回时把它滚回可视区中间。 */
  const anchorId = ref('')

  /**
   * 取走订阅流的锚点：读出来即清掉。
   *
   * <p>锚点只在"从订阅流点进详情再返回"这一次有效。留着不清的话，之后（比如切到聊天再切回订阅）
   * 重新进页面还会按旧锚点跳一次。</p>
   *
   * @returns 锚点帖子 ID，没有时为空的
   */
  function consumeAnchor(): string {
    const id = anchorId.value
    anchorId.value = ''
    return id
  }

  // ---------------- 订阅管理的定位 ----------------

  /** 订阅管理跳去用户主页前记下的页码。 */
  const managePageNum = ref(1)

  /** 订阅管理跳去用户主页前记下的那一行 ID。 */
  const manageAnchorId = ref('')

  /**
   * 取走订阅管理的定位信息：读出来即清掉，避免之后重新进页面又跳。
   *
   * @returns 页码与行 ID
   */
  function consumeManageAnchor(): { page: number, id: string } {
    const value = { page: managePageNum.value, id: manageAnchorId.value }
    manageAnchorId.value = ''
    return value
  }

  /** 已经加载的帖子 ID，用来去重。 */
  const knownIds = computed(() => new Set(posts.value.map((item) => item.id)))

  /**
   * 加载订阅流。
   *
   * @param reset 是否从头加载（刷新），否则接着往后翻
   */
  async function load(reset: boolean): Promise<void> {
    if (loading.value) {
      return
    }
    loading.value = true
    try {
      const last = posts.value[posts.value.length - 1]
      const endId = reset ? '' : (last?.id ?? '')
      if (!reset && endId.length === 0) {
        hasMore.value = false
        return
      }
      const page = await listSubscribeMessages(1, PAGE_SIZE, endId)
      const items = page.content.filter(isUsable).filter((item) => reset || !knownIds.value.has(item.id))
      posts.value = reset ? items : [...posts.value, ...items]
      hasMore.value = page.content.length >= PAGE_SIZE
      if (reset) {
        stale.value = false
      }
    } finally {
      loading.value = false
    }
  }

  /**
   * 刷新订阅流。
   */
  async function refresh(): Promise<void> {
    await load(true)
  }

  /**
   * 加载更早的帖子。
   */
  async function loadMore(): Promise<void> {
    await load(false)
  }

  /**
   * 标记订阅流需要重新拉取：订阅、取消订阅、改别名后调用。
   *
   * <p>不立刻拉：改订阅多半发生在主页或管理页，当时并不在看订阅流，下次进订阅流时再拉。</p>
   */
  function markStale(): void {
    stale.value = true
  }

  // ---------------- 订阅操作 ----------------

  /**
   * 订阅一个作者。
   *
   * @param publicKey 作者公钥
   */
  async function subscribe(publicKey: string): Promise<void> {
    await subscribeBroadcastAuthor(publicKey)
    aliases.value.set(publicKey, '')
    markStale()
  }

  /**
   * 取消订阅。
   *
   * @param publicKey 作者公钥
   */
  async function unsubscribe(publicKey: string): Promise<void> {
    await unsubscribeBroadcastAuthor(publicKey)
    remove(publicKey)
    markStale()
  }

  /**
   * 修改订阅别名。
   *
   * @param publicKey 作者公钥
   * @param alias     新别名，空串表示清除
   */
  async function setAlias(publicKey: string, alias: string): Promise<void> {
    const value = alias.trim()
    await setBroadcastSubscribeAlias(publicKey, value)
    aliases.value.set(publicKey, value)
  }

  return {
    aliases, indexLoaded, loadIndex, aliasOf, isSubscribed, put, remove,
    posts, loading, hasMore, stale, anchorId,
    consumeAnchor,
    managePageNum, manageAnchorId, consumeManageAnchor,
    load, refresh, loadMore, markStale,
    subscribe, unsubscribe, setAlias,
  }
})
