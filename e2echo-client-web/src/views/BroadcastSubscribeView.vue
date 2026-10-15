<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Setting } from '@element-plus/icons-vue'
import type { BroadcastMessageRespVo } from '@/api'
import BroadcastPostCard from '@/components/BroadcastPostCard.vue'
import { useSubscribeStore } from '@/stores/subscribe'
import { useAnchorScroll } from '@/utils/anchorScroll'
import { showError } from '@/utils/feedback'

/**
 * 订阅流：显示当前用户订阅的这些作者的帖子，外加自己发的帖子。
 *
 * <p>订阅关系存在 client 本地库，服务端不保存，所以这个流是 client 把订阅的作者列表当过滤条件查出来
 * 的；作者列表里 client 会补上当前用户自己（空的作者列表等于不过滤），所以自己的帖子也在里面。广播
 * 没有推送，进入页面拉一次，之后靠刷新按钮手动刷新。</p>
 */

const router = useRouter()
const subscribeStore = useSubscribeStore()

/** 列表滚动容器。 */
const listEl = ref<HTMLDivElement>()

/** 列表内容容器，用来观察内容高度变化。 */
const contentEl = ref<HTMLDivElement>()

/** 待定位的帖子 ID：从详情/主页返回时把它滚回可视区中间。 */
const pendingAnchor = ref('')

const anchor = useAnchorScroll({
  container: listEl,
  content: contentEl,
  anchorId: pendingAnchor,
})

onMounted(async () => {
  // 订阅关系变了（stale）或还没拉过才重新拉：返回列表时不打断已经加载的页
  if (subscribeStore.stale || subscribeStore.posts.length === 0) {
    try {
      await subscribeStore.refresh()
    } catch (error) {
      showError(error, '加载订阅流失败')
    }
  }
  // 取走即清：不然切到别的功能再回来，还会按旧锚点跳一次
  pendingAnchor.value = subscribeStore.consumeAnchor()
  await anchor.start()
})

/**
 * 用户自己滚动：不要再把视图拽回锚点。
 */
function onUserScroll(): void {
  anchor.stop()
}

/**
 * 手动刷新订阅流。
 */
async function onRefresh(): Promise<void> {
  try {
    await subscribeStore.refresh()
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
    await subscribeStore.loadMore()
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
  subscribeStore.anchorId = item.id
  void router.push({ name: 'broadcast-post', params: { id: item.id } })
}

/**
 * 打开某个用户的主页。
 *
 * @param uid    用户公钥
 * @param itemId 触发跳转的那条帖子 ID，返回时用它定位
 */
function openUser(uid: string, itemId: string): void {
  subscribeStore.anchorId = itemId
  void router.push({ name: 'broadcast-profile', params: { uid } })
}
</script>

<template>
  <section class="subscribe">
    <header class="subscribe__header">
      <span class="subscribe__title">订阅</span>
      <el-button
        class="subscribe__manage"
        :icon="Setting"
        size="small"
        text
        @click="router.push({ name: 'broadcast-subscribe-manage' })"
      >
        订阅管理
      </el-button>
      <el-button
        class="subscribe__refresh"
        :icon="Refresh"
        circle
        size="small"
        title="刷新"
        :loading="subscribeStore.loading"
        @click="onRefresh"
      />
    </header>

    <div
      ref="listEl"
      class="subscribe__body scroll-y"
      @wheel.passive="onUserScroll"
      @touchstart.passive="onUserScroll"
    >
      <div ref="contentEl">
        <el-skeleton
          v-if="subscribeStore.loading && subscribeStore.posts.length === 0"
          :rows="6"
          animated
        />

        <el-empty
          v-else-if="subscribeStore.posts.length === 0"
          description="订阅流里还没有帖子"
          :image-size="80"
        >
          <div class="subscribe__empty-tip">
            这里放的是你订阅的人和你自己的帖子
          </div>
          <el-button
            type="primary"
            size="small"
            @click="router.push({ name: 'broadcast-subscribe-manage' })"
          >
            去订阅
          </el-button>
        </el-empty>

        <template v-else>
          <BroadcastPostCard
            v-for="item in subscribeStore.posts"
            :key="item.id"
            :item="item"
            clickable
            @open="openPost(item)"
            @open-user="openUser"
            @reply="openPost(item)"
          />

          <div class="subscribe__more">
            <el-button
              v-if="subscribeStore.hasMore"
              size="small"
              :loading="subscribeStore.loading"
              @click="onLoadMore"
            >
              加载更多
            </el-button>
            <span v-else class="subscribe__end">没有更多了</span>
          </div>
        </template>
      </div>
    </div>
  </section>
</template>

<style scoped>
.subscribe {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  background: #f5f5f5;
}

.subscribe__header {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.subscribe__title {
  font-size: 15px;
  font-weight: 600;
}

.subscribe__manage {
  margin-left: auto;
}

.subscribe__body {
  flex: 1;
  min-height: 0;
}

.subscribe__empty-tip {
  margin-bottom: 12px;
  font-size: 12px;
  color: #909399;
}

.subscribe__more {
  padding: 12px 0 20px;
  text-align: center;
}

.subscribe__end {
  font-size: 12px;
  color: #a8abb2;
}
</style>
