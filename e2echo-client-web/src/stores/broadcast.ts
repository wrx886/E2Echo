import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  BROADCAST_POST_TO,
  getBroadcastMessage,
  getUserinfo,
  listBroadcastMessages,
  publishBroadcastMessage,
  publishUserinfo,
} from '@/api'
import type { BroadcastFileVo, BroadcastMessageRespVo, BroadcastUserinfo } from '@/api'

/** 帖子与评论每页条数。 */
const PAGE_SIZE = 20

/**
 * 判断一条广播消息是否可用。
 *
 * <p>两种坏数据都要丢掉：验签失败的条目在列表里是 {@code null} 占位；正文整体缺失的（别的客户端
 * 发的、或正文解析成了 null）也没有可展示的内容。</p>
 *
 * @param item 列表里的一条
 * @returns 可用返回 true
 */
function isUsable(item: BroadcastMessageRespVo | null): item is BroadcastMessageRespVo {
  return item !== null && item.message !== null && item.message !== undefined
}

/**
 * 广播。
 *
 * <p>广播只存服务端、不落本地库，所以这里只做缓存与拼装：时间线、帖子详情与评论、用户资料、以及
 * 某个用户的帖子列表。资料按公钥缓存，避免同一个人被反复查。</p>
 */
export const useBroadcastStore = defineStore('broadcast', () => {

  // ---------------- 用户资料 ----------------

  /** 用户公钥 -> 资料；null 表示查过但没有。 */
  const profiles = ref(new Map<string, BroadcastUserinfo | null>())

  /** 正在进行的资料查询，避免同一个人并发查多次。 */
  const profileRequests = new Map<string, Promise<BroadcastUserinfo | null>>()

  /**
   * 取用户资料：缓存里有就直接用。
   *
   * @param uid   用户公钥
   * @param force 是否强制重新查询
   * @returns 用户资料，没有资料时为 null
   */
  async function loadProfile(uid: string, force = false): Promise<BroadcastUserinfo | null> {
    if (!force && profiles.value.has(uid)) {
      return profiles.value.get(uid) ?? null
    }
    const running = profileRequests.get(uid)
    if (running && !force) {
      return running
    }

    const task = (async () => {
      // 响应里外层是消息信息，资料本身在 userinfo 字段里
      const info = (await getUserinfo(uid))?.userinfo ?? null
      profiles.value.set(uid, info)
      return info
    })()

    profileRequests.set(uid, task)
    try {
      return await task
    } finally {
      profileRequests.delete(uid)
    }
  }

  /**
   * 同步取缓存里的用户资料。
   *
   * @param uid 用户公钥
   * @returns 用户资料，没查过或没有资料时为 null
   */
  function profileOf(uid: string): BroadcastUserinfo | null {
    return profiles.value.get(uid) ?? null
  }

  /**
   * 发布当前用户的资料，并更新缓存。
   *
   * @param uid      资料所属用户（当前登入用户）的公钥
   * @param userinfo 用户资料
   */
  async function saveProfile(uid: string, userinfo: BroadcastUserinfo): Promise<void> {
    await publishUserinfo(userinfo)
    profiles.value.set(uid, userinfo)
  }

  // ---------------- 帖子列表 ----------------

  /**
   * 取一页帖子：按更新时间倒序，游标用上一页最后一条的 ID。
   *
   * @param author 只看这个作者的帖子，空串表示不限作者
   * @param endId  游标：只取 ID 小于它的帖子，空串表示从头开始
   * @returns 帖子与是否还有更多
   */
  async function fetchPostPage(
    author: string,
    endId: string,
  ): Promise<{ items: BroadcastMessageRespVo[]; hasMore: boolean }> {
    const page = await listBroadcastMessages({
      from: author.length > 0 ? [author] : [],
      to: [BROADCAST_POST_TO],
      startTimestamp: '',
      endTimestamp: '',
      startId: '',
      endId,
      order: 'DESC',
      pageNum: 1,
      pageSize: PAGE_SIZE,
    })
    return {
      items: page.content.filter(isUsable),
      hasMore: page.content.length >= PAGE_SIZE,
    }
  }

  /** 时间线上的帖子。 */
  const posts = ref<BroadcastMessageRespVo[]>([])

  /** 时间线是否正在加载。 */
  const postsLoading = ref(false)

  /** 时间线是否还有更多。 */
  const postsHasMore = ref(true)

  /**
   * 时间线的定位锚点：用户点开的那条帖子的 ID。
   *
   * <p>记 ID 而不是像素位置：附件是浏览器异步加载的，内容高度随时会变，像素位置很容易漂；按元素
   * 定位则不受影响——回来时把这条帖子滚到可视区中间即可。</p>
   */
  const feedAnchorId = ref('')

  /**
   * 加载时间线。
   *
   * @param reset 是否重新从头加载（刷新），否则接着往后翻
   */
  async function loadPosts(reset: boolean): Promise<void> {
    if (postsLoading.value) {
      return
    }
    postsLoading.value = true
    try {
      const last = posts.value[posts.value.length - 1]
      const endId = reset ? '' : (last?.id ?? '')
      if (!reset && endId.length === 0) {
        postsHasMore.value = false
        return
      }
      const page = await fetchPostPage('', endId)
      posts.value = reset ? page.items : [...posts.value, ...page.items]
      postsHasMore.value = page.hasMore
    } finally {
      postsLoading.value = false
    }
  }

  /**
   * 刷新时间线。
   */
  async function refreshPosts(): Promise<void> {
    await loadPosts(true)
  }

  /**
   * 发帖，随后刷新时间线。
   *
   * @param text  正文
   * @param files 附件
   */
  async function createPost(text: string, files: BroadcastFileVo[]): Promise<void> {
    await publishBroadcastMessage('', { text, files })
    await refreshPosts()
  }

  // ---------------- 帖子详情与评论 ----------------

  /** 当前打开的帖子。 */
  const post = ref<BroadcastMessageRespVo | null>(null)

  /** 帖子是否正在加载。 */
  const postLoading = ref(false)

  /** 当前帖子的评论。 */
  const comments = ref<BroadcastMessageRespVo[]>([])

  /** 评论是否正在加载。 */
  const commentsLoading = ref(false)

  /** 评论是否还有更多。 */
  const commentsHasMore = ref(true)

  /**
   * 评论排序：{@code DESC} 新评论在前（默认），{@code ASC} 最早在前。
   *
   * <p>由页面上的切换控件直接改，改完重新从头加载。</p>
   */
  const commentsOrder = ref<'DESC' | 'ASC'>('DESC')

  /** 评论 ID -> 评论，用来显示"回复某某"。 */
  const commentIndex = ref(new Map<string, BroadcastMessageRespVo>())

  /** 帖子详情页的定位锚点：从评论跳走时记下那条评论，回来时把它滚到中间。 */
  const postAnchorId = ref('')

  /**
   * 用户主页的定位锚点：从主页里点开某条帖子再返回时，把那条帖子滚回中间。
   *
   * <p>锚点带上是哪个人的主页（见 {@link profileAnchorUid}）：换一个人看时，上一个主页的锚点
   * 不能套过来。</p>
   */
  const profileAnchorId = ref('')

  /** 主页锚点所属的用户公钥。 */
  const profileAnchorUid = ref('')

  /**
   * 加载帖子。
   *
   * <p>先把上一次的内容清掉：查不到（服务端返回失败会抛异常）或正文有问题时，赋值不会发生，
   * 不清空就会继续显示上一个帖子的内容。</p>
   *
   * @param id 帖子消息 ID
   */
  async function loadPost(id: string): Promise<void> {
    postLoading.value = true
    post.value = null
    try {
      post.value = await getBroadcastMessage(id)
    } finally {
      postLoading.value = false
    }
  }

  /**
   * 打开一个帖子：清掉上一个帖子的内容，再加载帖子本身与它的评论。
   *
   * @param id 帖子消息 ID
   */
  async function openPost(id: string): Promise<void> {
    comments.value = []
    commentsHasMore.value = true
    commentIndex.value = new Map()
    await loadPost(id)
    await loadComments(id, true)
  }

  /**
   * 加载某个帖子的评论，游标用上一页最后一条的 ID。
   *
   * <p>排序方向看 {@link commentsOrder}：倒序用 {@code endId}（只取 ID 更小的），正序用
   * {@code startId}（只取 ID 更大的）——方向和游标必须配套，否则翻页会一直停在第一页。</p>
   *
   * @param postId 帖子消息 ID
   * @param reset  是否重新从头加载
   */
  async function loadComments(postId: string, reset: boolean): Promise<void> {
    if (commentsLoading.value) {
      return
    }
    commentsLoading.value = true
    try {
      const last = comments.value[comments.value.length - 1]
      const cursor = reset ? '' : (last?.id ?? '')
      if (!reset && cursor.length === 0) {
        commentsHasMore.value = false
        return
      }
      const order = commentsOrder.value

      const page = await listBroadcastMessages({
        from: [],
        to: [postId],
        startTimestamp: '',
        endTimestamp: '',
        startId: order === 'ASC' ? cursor : '',
        endId: order === 'DESC' ? cursor : '',
        order,
        pageNum: 1,
        pageSize: PAGE_SIZE,
      })

      const items = page.content.filter(isUsable)
      comments.value = reset ? items : [...comments.value, ...items]
      for (const item of items) {
        commentIndex.value.set(item.id, item)
      }
      commentsHasMore.value = page.content.length >= PAGE_SIZE
    } finally {
      commentsLoading.value = false
    }
  }

  /**
   * 发表评论或回复，随后重新加载评论。
   *
   * @param postId    帖子消息 ID
   * @param text      正文
   * @param files     附件
   * @param replyToId 被回复评论的 ID，直接评论帖子时为空
   */
  async function createComment(
    postId: string,
    text: string,
    files: BroadcastFileVo[],
    replyToId?: string,
  ): Promise<void> {
    await publishBroadcastMessage(postId, { text, files, replyToId: replyToId ?? null })
    await loadComments(postId, true)
    // 评论数变了，顺带刷新时间线里的那条帖子没有接口支持，刷新靠用户手动
  }

  /**
   * 取被回复的评论：已加载的评论里没有时单独查一次。
   *
   * @param id 评论消息 ID
   * @returns 评论，不存在时为 null
   */
  async function findComment(id: string): Promise<BroadcastMessageRespVo | null> {
    const cached = commentIndex.value.get(id)
    if (cached) {
      return cached
    }
    const item = await getBroadcastMessage(id)
    if (item) {
      commentIndex.value.set(item.id, item)
    }
    return item
  }

  // ---------------- 某个用户的帖子 ----------------

  /** 用户主页上的帖子。 */
  const userPosts = ref<BroadcastMessageRespVo[]>([])

  /** 用户主页上的帖子是否正在加载。 */
  const userPostsLoading = ref(false)

  /** 用户主页上的帖子是否还有更多。 */
  const userPostsHasMore = ref(true)

  /**
   * 加载某个用户的帖子。
   *
   * @param uid   用户公钥
   * @param reset 是否重新从头加载
   */
  async function loadUserPosts(uid: string, reset: boolean): Promise<void> {
    if (userPostsLoading.value) {
      return
    }
    userPostsLoading.value = true
    try {
      const last = userPosts.value[userPosts.value.length - 1]
      const endId = reset ? '' : (last?.id ?? '')
      if (!reset && endId.length === 0) {
        userPostsHasMore.value = false
        return
      }
      const page = await fetchPostPage(uid, endId)
      userPosts.value = reset ? page.items : [...userPosts.value, ...page.items]
      userPostsHasMore.value = page.hasMore
    } finally {
      userPostsLoading.value = false
    }
  }

  return {
    profiles, loadProfile, profileOf, saveProfile,
    posts, postsLoading, postsHasMore, loadPosts, refreshPosts, createPost,
    feedAnchorId,
    post, postLoading, comments, commentsLoading, commentsHasMore,
    commentsOrder, postAnchorId, profileAnchorId, profileAnchorUid,
    loadPost, openPost, loadComments, createComment, findComment,
    userPosts, userPostsLoading, userPostsHasMore, loadUserPosts,
  }
})
