<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, DocumentCopy, Edit, Refresh, Search } from '@element-plus/icons-vue'
import { avatarSrc, listBroadcastSubscribes } from '@/api'
import type { BroadcastSubscribeDto, BroadcastUserinfo } from '@/api'
import { useBroadcastStore } from '@/stores/broadcast'
import { useSubscribeStore } from '@/stores/subscribe'
import { copyText } from '@/utils/clipboard'
import { isBackNavigation, useAnchorScroll } from '@/utils/anchorScroll'
import { formatTime, shortKey } from '@/utils/display'
import { showError } from '@/utils/feedback'

/**
 * 订阅管理：查看已订阅的人、改别名、取消订阅，以及按公钥或别名搜索。
 *
 * <p>订阅只存在 client 本地库，服务端不知道谁订阅了谁。别名只在本机展示用——订阅流、时间线、用户
 * 主页显示作者名字时都用它（优先于对方自己填的昵称）。这里不提供"新增订阅"：订阅是在用户主页上
 * 看人时顺手的操作，管理页只管已有的订阅。</p>
 *
 * <p>行里的头像与昵称来自对方的 userinfo（订阅本身只存了公钥和别名），所以每页加载完还要把这一页
 * 作者的资料查一遍；查不到就退回公钥末几位。</p>
 */

/** 每页条数。 */
const PAGE_SIZE = 20

/** 搜索输入停止多久后才真正去查。 */
const SEARCH_DELAY = 300

const router = useRouter()
const subscribeStore = useSubscribeStore()
const broadcastStore = useBroadcastStore()

/** 搜索框里的文字。 */
const keyword = ref('')

/** 已经发给接口的搜索词。 */
const appliedKeyword = ref('')

/** 当前页的订阅。 */
const items = ref<BroadcastSubscribeDto[]>([])

/** 订阅总数。 */
const total = ref(0)

/** 当前页码，从 1 开始。 */
const pageNum = ref(1)

/** 是否正在加载。 */
const loading = ref(false)

/** 搜索防抖定时器。 */
let searchTimer: ReturnType<typeof setTimeout> | null = null

/** 列表容器：行数多时可以滚动，返回时用它定位。 */
const listEl = ref<HTMLDivElement>()

/** 内容容器，用来观察内容高度变化。 */
const contentEl = ref<HTMLDivElement>()

/** 待定位的行 ID：从用户主页返回时把那一行滚回可视区中间。 */
const pendingAnchor = ref('')

const anchor = useAnchorScroll({
  container: listEl,
  content: contentEl,
  anchorId: pendingAnchor,
})

/** 有没有关键词：用来区分"还没订阅"和"没搜到"。 */
const searching = computed(() => appliedKeyword.value.length > 0)

onMounted(async () => {
  // 从用户主页返回时恢复原来的页码与位置；其它入口进来就当作第一次打开
  const back = isBackNavigation()
  const saved = subscribeStore.consumeManageAnchor()
  await loadPage(back && saved.id.length > 0 && saved.page >= 1 ? saved.page : 1)
  pendingAnchor.value = back ? saved.id : ''
  await anchor.start()
})

onUnmounted(() => {
  if (searchTimer !== null) {
    clearTimeout(searchTimer)
  }
})

/**
 * 取一页订阅。
 *
 * @param page 页码，从 1 开始
 */
async function loadPage(page: number): Promise<void> {
  loading.value = true
  try {
    const result = await listBroadcastSubscribes({
      pageNum: page,
      pageSize: PAGE_SIZE,
      publicKeyOrAlias: appliedKeyword.value,
    })
    items.value = result.content
    // 分页信息收在 page 对象里（client 开了 VIA_DTO 序列化）
    total.value = result.page.totalElements
    pageNum.value = page
    // 资料单独异步补：订阅里只有公钥和别名，昵称与头像要去 userinfo 里取（按公钥缓存、并发去重）
    void resolveProfiles(result.content)
  } catch (error) {
    showError(error, '加载订阅失败')
  } finally {
    loading.value = false
  }
}

/**
 * 把这一页订阅对应的 userinfo 拉回来。
 *
 * <p>取不到资料（对方没填过，或请求失败）就不显示昵称与头像，名字退回公钥末几位——不能因为某个人
 * 的资料失败就让整行报错。</p>
 *
 * @param list 当前页的订阅
 */
async function resolveProfiles(list: BroadcastSubscribeDto[]): Promise<void> {
  await Promise.all(list.map((item) =>
    broadcastStore.loadProfile(item.publicKey).catch(() => null)))
}

/**
 * 取某个订阅对应的用户资料。
 *
 * @param item 订阅
 * @returns 用户资料，没查到时为 null
 */
function profileOf(item: BroadcastSubscribeDto): BroadcastUserinfo | null {
  return broadcastStore.profileOf(item.publicKey)
}

/**
 * 头像地址：资料里的头像文件 ID 转成广播文件地址；没有头像时返回 undefined，由头像文字兜底。
 *
 * @param item 订阅
 * @returns 头像地址
 */
function avatarUrlOf(item: BroadcastSubscribeDto): string | undefined {
  return avatarSrc(profileOf(item)?.avatarUrl)
}

/**
 * 重新加载当前页，并同步订阅索引（卡片上显示的名字要用它）。
 */
async function reload(): Promise<void> {
  await loadPage(pageNum.value)
  // 订阅可能增删过，索引跟着重新拉一次才能反映到卡片上
  await subscribeStore.loadIndex(true).catch(() => {})
}

/**
 * 搜索：防抖后按关键词重新查第一页。
 */
function onSearchInput(): void {
  if (searchTimer !== null) {
    clearTimeout(searchTimer)
  }
  searchTimer = setTimeout(() => {
    searchTimer = null
    const value = keyword.value.trim()
    if (value === appliedKeyword.value) {
      return
    }
    appliedKeyword.value = value
    void loadPage(1)
  }, SEARCH_DELAY)
}

/**
 * 清空搜索：不用等防抖，直接回到全部订阅。
 */
function onSearchClear(): void {
  if (searchTimer !== null) {
    clearTimeout(searchTimer)
    searchTimer = null
  }
  keyword.value = ''
  if (appliedKeyword.value.length === 0) {
    return
  }
  appliedKeyword.value = ''
  void loadPage(1)
}

/**
 * 修改订阅别名：留空表示清除。
 *
 * @param item 订阅
 */
async function onEditAlias(item: BroadcastSubscribeDto): Promise<void> {
  let value = ''
  try {
    const result = await ElMessageBox.prompt(
      '别名只在本机展示，订阅流、时间线和 TA 的主页都会优先显示这个名字；留空表示清除。',
      `给「${titleOf(item)}」起个别名`,
      {
        inputValue: item.alias ?? '',
        inputPlaceholder: '留空表示清除别名',
        confirmButtonText: '保存',
        cancelButtonText: '取消',
      },
    )
    value = result.value ?? ''
  } catch {
    // 取消或关闭弹窗，什么都不做
    return
  }
  try {
    await subscribeStore.setAlias(item.publicKey, value)
    ElMessage.success(value.trim().length > 0 ? '已保存别名' : '已清除别名')
    await reload()
  } catch (error) {
    showError(error, '保存别名失败')
  }
}

/**
 * 取消订阅。
 *
 * @param item 订阅
 */
async function onUnsubscribe(item: BroadcastSubscribeDto): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `取消订阅后「${titleOf(item)}」的帖子不再出现在订阅流里，别名也会一起删掉。`,
      '取消订阅',
      { type: 'warning', confirmButtonText: '取消订阅', cancelButtonText: '再想想' },
    )
  } catch {
    return
  }
  try {
    await subscribeStore.unsubscribe(item.publicKey)
    ElMessage.success('已取消订阅')
    await reload()
  } catch (error) {
    showError(error, '取消订阅失败')
  }
}

/**
 * 复制公钥。
 *
 * @param publicKey 被订阅者公钥
 */
async function onCopy(publicKey: string): Promise<void> {
  const ok = await copyText(publicKey)
  if (ok) {
    ElMessage.success('已复制公钥')
  } else {
    ElMessage.error('复制失败，请手动选中复制')
  }
}

/**
 * 刷新：按第一次打开那样重新拉第一页，然后回到顶部。
 */
async function onRefresh(): Promise<void> {
  await loadPage(1)
  anchor.reset()
}

/**
 * 打开某个订阅者的主页。
 *
 * @param item 订阅
 */
function openProfile(item: BroadcastSubscribeDto): void {
  // 记下当前页码与这一行，返回时恢复位置
  subscribeStore.managePageNum = pageNum.value
  subscribeStore.manageAnchorId = item.id
  void router.push({ name: 'broadcast-profile', params: { uid: item.publicKey } })
}

/**
 * 取展示名：订阅别名优先，其次资料昵称，最后公钥末 8 位（和时间线、订阅流、用户主页一致）。
 *
 * @param item 订阅
 * @returns 展示名
 */
function titleOf(item: BroadcastSubscribeDto): string {
  const alias = item.alias?.trim() ?? ''
  if (alias.length > 0) {
    return alias
  }
  const nickname = profileOf(item)?.nickname?.trim() ?? ''
  return nickname.length > 0 ? nickname : shortKey(item.publicKey, 8)
}

/**
 * 展示名用的是别名、而且和对方自己的昵称不一样时，名字后面补一条昵称。
 *
 * @param item 订阅
 * @returns 要补的昵称，不需要补时为空串
 */
function nicknameOf(item: BroadcastSubscribeDto): string {
  const alias = item.alias?.trim() ?? ''
  const nickname = profileOf(item)?.nickname?.trim() ?? ''
  return alias.length > 0 && nickname.length > 0 && nickname !== alias ? nickname : ''
}

/**
 * 头像文字：展示名首字，没有别名时就是公钥最后一位。
 *
 * @param item 订阅
 * @returns 头像文字
 */
function avatarOf(item: BroadcastSubscribeDto): string {
  return titleOf(item).slice(0, 1)
}
</script>

<template>
  <section class="manage">
    <header class="manage__header">
      <el-button
        class="manage__back"
        :icon="ArrowLeft"
        circle
        size="small"
        title="返回订阅流"
        @click="router.push({ name: 'broadcast-subscribe' })"
      />
      <span class="manage__title">订阅管理</span>
      <el-button
        class="manage__refresh"
        :icon="Refresh"
        circle
        size="small"
        title="刷新"
        :loading="loading"
        @click="onRefresh"
      />
    </header>

    <div class="manage__search">
      <el-input
        v-model="keyword"
        size="small"
        placeholder="搜索已订阅的公钥或别名"
        clearable
        :prefix-icon="Search"
        @input="onSearchInput"
        @clear="onSearchClear"
      />
    </div>

    <div ref="listEl" class="manage__body scroll-y">
      <div ref="contentEl">
      <el-skeleton v-if="loading && items.length === 0" :rows="6" animated />

      <el-empty
        v-else-if="items.length === 0"
        :image-size="80"
        :description="searching ? '没有匹配的订阅' : '还没有订阅任何人'"
      >
        <div v-if="!searching" class="manage__empty-tip">
          在用户主页点「订阅」后，TA 会出现在这里
        </div>
      </el-empty>

      <template v-else>
        <div v-for="item in items" :key="item.id" class="manage__item" :data-item-id="item.id">
          <!-- 点头像/名字去 TA 的主页：订阅、发消息这些都在那边做 -->
          <el-avatar
            :size="36"
            class="manage__avatar"
            :src="avatarUrlOf(item)"
            :title="`去 ${titleOf(item)} 的主页`"
            @click="openProfile(item)"
          >
            {{ avatarOf(item) }}
          </el-avatar>

          <div class="manage__main">
            <div class="manage__row">
              <span class="manage__name" :title="item.publicKey" @click="openProfile(item)">
                {{ titleOf(item) }}
              </span>
              <span v-if="nicknameOf(item)" class="manage__nickname">昵称：{{ nicknameOf(item) }}</span>
              <el-button
                class="manage__edit"
                :icon="Edit"
                text
                circle
                size="small"
                title="设置别名"
                @click="onEditAlias(item)"
              />
              <el-tag v-if="item.alias && item.alias.trim().length > 0" size="small" effect="plain">
                别名
              </el-tag>
            </div>
            <div class="manage__key" :title="item.publicKey">
              {{ item.publicKey }}
              <el-button
                :icon="DocumentCopy"
                text
                size="small"
                title="复制公钥"
                @click="onCopy(item.publicKey)"
              />
            </div>
            <div class="manage__time">订阅于 {{ formatTime(item.createTime) }}</div>
          </div>

          <div class="manage__actions">
            <el-button size="small" text type="danger" @click="onUnsubscribe(item)">
              取消订阅
            </el-button>
          </div>
        </div>

        <el-pagination
          v-if="total > PAGE_SIZE"
          class="manage__pagination"
          layout="prev, pager, next"
          :current-page="pageNum"
          :page-size="PAGE_SIZE"
          :total="total"
          @current-change="loadPage"
        />
      </template>
      </div>
    </div>
  </section>
</template>

<style scoped>
.manage {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  background: #f5f5f5;
}

.manage__header {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.manage__title {
  font-size: 15px;
  font-weight: 600;
}

.manage__back {
  margin-right: 2px;
}

.manage__refresh {
  margin-left: auto;
}

.manage__search {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
}

.manage__body {
  flex: 1;
  min-height: 0;
  padding: 0 16px 16px;
}

.manage__empty-tip {
  font-size: 12px;
  color: #909399;
}

.manage__item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 8px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.manage__avatar {
  flex: none;
  background: #07c160;
  color: #fff;
  cursor: pointer;
}

.manage__main {
  flex: 1;
  min-width: 0;
}

.manage__row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.manage__name {
  overflow: hidden;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  text-overflow: ellipsis;
  cursor: pointer;
}

.manage__name:hover {
  color: #409eff;
}

.manage__nickname {
  flex: none;
  font-size: 12px;
  color: #a8abb2;
}

.manage__edit {
  flex: none;
  color: #909399;
}

.manage__edit:hover {
  color: #409eff;
}

.manage__key {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.manage__time {
  margin-top: 2px;
  font-size: 12px;
  color: #a8abb2;
}

.manage__actions {
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.manage__pagination {
  margin-top: 12px;
  justify-content: center;
}
</style>
