<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Edit, Refresh, Search } from '@element-plus/icons-vue'
import { MESSAGE_ID_LENGTH, PUBLIC_KEY_LENGTH } from '@/api'
import type { BroadcastMessageRespVo } from '@/api'
import BroadcastPostCard from '@/components/BroadcastPostCard.vue'
import { useBroadcastStore } from '@/stores/broadcast'
import { useAnchorScroll } from '@/utils/anchorScroll'
import { showError } from '@/utils/feedback'

/**
 * 广播时间线：所有人的帖子，右下角按钮进入发帖页，顶部可以按公钥或帖子 ID 检索。
 *
 * <p>广播没有推送，进入页面拉一次，之后靠刷新按钮手动刷新。想看订阅的人发的帖子去「订阅」流。</p>
 */

const router = useRouter()
const broadcastStore = useBroadcastStore()

/** 检索关键字：用户公钥或帖子 ID。 */
const keyword = ref('')

/** 列表滚动容器。 */
const listEl = ref<HTMLDivElement>()

/** 列表内容容器，用来观察内容高度变化。 */
const contentEl = ref<HTMLDivElement>()

/** 待定位的帖子 ID：返回列表后把它滚到可视区中间。 */
const pendingAnchor = ref('')

const anchor = useAnchorScroll({
  container: listEl,
  content: contentEl,
  anchorId: pendingAnchor,
})

onMounted(async () => {
  // 从帖子详情返回时列表还在 store 里：不重新拉，保住已经加载的页
  if (broadcastStore.posts.length === 0) {
    try {
      await broadcastStore.refreshPosts()
    } catch (error) {
      showError(error, '加载广播失败')
    }
  }
  // 把上次点开的那条帖子放回可视区中间；取走即清，避免以后重新进这个页面又跳一次
  pendingAnchor.value = broadcastStore.consumeFeedAnchor()
  await anchor.start()
})

/**
 * 用户自己滚动：不要再把视图拽回锚点。
 */
function onUserScroll(): void {
  anchor.stop()
}

/**
 * 检索：按长度区分是用户公钥还是帖子 ID，分别跳到用户主页或帖子详情。
 */
function onSearch(): void {
  const value = keyword.value.trim()
  if (value.length === 0) {
    ElMessage.warning('请输入用户公钥或帖子 ID')
    return
  }
  if (!/^[0-9a-fA-F]+$/.test(value)) {
    ElMessage.warning('只能输入十六进制字符')
    return
  }
  if (value.length === PUBLIC_KEY_LENGTH) {
    keyword.value = ''
    void router.push({ name: 'broadcast-profile', params: { uid: value } })
    return
  }
  if (value.length === MESSAGE_ID_LENGTH) {
    keyword.value = ''
    void router.push({ name: 'broadcast-post', params: { id: value } })
    return
  }
  ElMessage.warning(`长度不对：用户公钥是 ${PUBLIC_KEY_LENGTH} 位，帖子 ID 是 ${MESSAGE_ID_LENGTH} 位`)
}

/**
 * 手动刷新时间线。
 */
async function onRefresh(): Promise<void> {
  try {
    await broadcastStore.refreshPosts()
    // 刷新就是重看最新的：放弃定位、回到顶部
    anchor.reset()
  } catch (error) {
    showError(error, '刷新失败')
  }
}

/**
 * 加载更早的帖子。
 */
async function onLoadMore(): Promise<void> {
  try {
    await broadcastStore.loadPosts(false)
  } catch (error) {
    showError(error, '加载更多失败')
  }
}

/**
 * 打开帖子详情。
 *
 * @param item 帖子
 */
function openPost(item: BroadcastMessageRespVo): void {
  // 记下点开的是哪条，返回时把它放回可视区中间
  broadcastStore.feedAnchorId = item.id
  void router.push({ name: 'broadcast-post', params: { id: item.id } })
}

/**
 * 打开某个用户的主页。
 *
 * @param uid 用户公钥
 */
function openUser(uid: string, itemId: string): void {
  broadcastStore.feedAnchorId = itemId
  void router.push({ name: 'broadcast-profile', params: { uid } })
}
</script>

<template>
  <section class="feed">
    <header class="feed__header">
      <span class="feed__title">全部帖子</span>
      <el-button
        class="feed__refresh"
        :icon="Refresh"
        circle
        size="small"
        title="刷新"
        :loading="broadcastStore.postsLoading"
        @click="onRefresh"
      />
    </header>

    <!-- 检索：公钥（130 位）查用户，帖子 ID（48 位）查帖子 -->
    <div class="feed__search">
      <el-input
        v-model="keyword"
        size="small"
        placeholder="输入用户公钥（130 位）或帖子 ID（48 位）查找"
        clearable
        @keydown.enter.exact="onSearch"
      />
      <el-button size="small" :icon="Search" @click="onSearch">查找</el-button>
    </div>

    <div
      ref="listEl"
      class="feed__body scroll-y"
      @wheel.passive="onUserScroll"
      @touchstart.passive="onUserScroll"
    >
      <div ref="contentEl">
      <el-skeleton v-if="broadcastStore.postsLoading && broadcastStore.posts.length === 0" :rows="6" animated />

      <el-empty
        v-else-if="broadcastStore.posts.length === 0"
        description="还没有人发帖"
        :image-size="80"
      />

      <template v-else>
        <BroadcastPostCard
          v-for="item in broadcastStore.posts"
          :key="item.id"
          :item="item"
          clickable
          @open="openPost(item)"
          @open-user="openUser"
          @reply="openPost(item)"
        />

        <div class="feed__more">
          <el-button
            v-if="broadcastStore.postsHasMore"
            size="small"
            :loading="broadcastStore.postsLoading"
            @click="onLoadMore"
          >
            加载更多
          </el-button>
          <span v-else class="feed__end">没有更多了</span>
        </div>
      </template>
      </div>
    </div>

    <!-- 发帖入口：悬浮在右下角，点开进入发帖页 -->
    <el-button
      class="feed__fab"
      type="primary"
      :icon="Edit"
      round
      @click="router.push({ name: 'broadcast-create' })"
    >
      发帖
    </el-button>
  </section>
</template>

<style scoped>
.feed {
  position: relative;
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  background: #f5f5f5;
}

.feed__header {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.feed__title {
  font-size: 15px;
  font-weight: 600;
}

.feed__refresh {
  margin-left: auto;
}

.feed__search {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.feed__body {
  flex: 1;
  min-height: 0;
  background: #fff;
}

.feed__more {
  padding: 14px 0 20px;
  text-align: center;
}

.feed__end {
  font-size: 12px;
  color: #a8abb2;
}

/* 发帖入口：固定在整个内容区右下角，列表滚动时不动 */
.feed__fab {
  position: absolute;
  right: 24px;
  bottom: 24px;
  z-index: 10;
}
</style>
