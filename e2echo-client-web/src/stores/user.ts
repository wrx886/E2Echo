import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getConversation, getCurrentUser } from '@/api'
import { resolveAvatarText, resolveSelfName } from '@/utils/display'
import { useConversationStore } from './conversation'

/**
 * 当前登入用户。
 *
 * <p>保存自己的公钥，并据此得出展示名称与头像文字：前端靠公钥判断一条消息是不是自己发的，
 * 侧边栏的个人信息、自己消息的头像用的都是这里的名称。</p>
 */
export const useUserStore = defineStore('user', () => {

  /** 当前登入用户的公钥，空串表示还没有取到。 */
  const publicKey = ref('')

  /** 正在进行的请求，用于把并发调用合并成一次。 */
  let pending: Promise<void> | null = null

  /**
   * 直接查到的“与自己”会话的别名。
   *
   * <p>会话列表是分页的，自己那条会话不一定在第一页，所以除了看列表还单独查一次。</p>
   */
  const selfAlias = ref<string | null>(null)

  /**
   * 与自己那条会话：用户可以建一个会话对方等于自己公钥的会话，用它的别名当自己的名称。
   *
   * <p>会话列表由会话 store 持有，这里跟着它变化：列表还没加载完时先按公钥算一个名字，
   * 加载后如果存在这样一条会话就自动换成它的别名。</p>
   */
  const selfConversation = computed(() => {
    const key = publicKey.value
    if (key.length === 0) {
      return null
    }
    return useConversationStore().list.find((item) => item.peer === key) ?? null
  })

  /**
   * 查询“与自己”会话的别名。
   *
   * @param key 当前用户公钥
   */
  async function loadSelfAlias(key: string): Promise<void> {
    try {
      const conversation = await getConversation(key)
      const alias = conversation?.alias?.trim() ?? ''
      selfAlias.value = alias.length > 0 ? alias : null
    } catch {
      // 取不到就用算出来的名称，不影响其他功能
    }
  }

  /** 展示名称：优先用与自己那条会话的别名，没有就按公钥算一个。 */
  const displayName = computed(() =>
    resolveSelfName(publicKey.value, selfConversation.value?.alias ?? selfAlias.value))

  /** 头像上的文字。 */
  const avatarText = computed(() => resolveAvatarText(displayName.value, publicKey.value))

  /**
   * 确保已经取到当前用户公钥。
   *
   * <p>幂等：已经取到或正在取时直接返回；失败时清掉在途标记，下次调用会重试。</p>
   */
  async function ensureLoaded(): Promise<void> {
    if (publicKey.value) {
      return
    }
    if (pending) {
      return pending
    }
    pending = (async () => {
      try {
        publicKey.value = await getCurrentUser()
        // 名称随后异步补上，先按公钥算一个
        void loadSelfAlias(publicKey.value)
      } finally {
        pending = null
      }
    })()
    return pending
  }

  return { publicKey, selfConversation, selfAlias, displayName, avatarText, ensureLoaded }
})
