<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { latestMessagePreview } from '@/api'
import type { ConversationDto } from '@/api'
import { useConversationStore } from '@/stores/conversation'
import { conversationTitle, formatTime } from '@/utils/display'
import { showError } from '@/utils/feedback'
import { afterPaint } from '@/utils/paint'

/**
 * 会话区。
 *
 * <p>列表按更新时间倒序，触底加载更多；点击某条会话进入聊天页，右上角按钮进入新增会话页。</p>
 */

const conversationStore = useConversationStore()
const route = useRoute()
const router = useRouter()

/** 列表滚动容器。 */
const listEl = ref<HTMLDivElement>()

/** 刚点下、还没真正跳过去的会话对方，用来先画出选中效果。 */
const pendingPeer = ref('')

/** 当前选中的会话对方：刚点的那个优先，其次看路由，都只在聊天页里算选中。 */
const activePeer = computed(() =>
  pendingPeer.value || (route.name === 'chat' ? String(route.params.peer ?? '') : ''))

onMounted(async () => {
  try {
    await conversationStore.loadIfNeeded()
  } catch (error) {
    showError(error, '加载会话列表失败')
  }
})

/**
 * 打开会话。
 *
 * @param conversation 会话
 */
function openConversation(conversation: ConversationDto): void {
  // 先把点击反馈画出来：这一步只改选中样式，浏览器能在当前帧直接绘制，
  // 切会话（重建消息区）放到绘制之后再开始，否则反馈会和重活一起被推迟到下一帧
  pendingPeer.value = conversation.peer
  afterPaint(() => {
    pendingPeer.value = ''
    void router.push({ name: 'chat', params: { peer: conversation.peer } })
  })
}

/**
 * 触底加载更多会话。
 */
async function onScroll(): Promise<void> {
  const el = listEl.value
  if (!el || conversationStore.loading || !conversationStore.hasMore) {
    return
  }
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 24) {
    try {
      await conversationStore.loadMore()
    } catch (error) {
      showError(error, '加载更多会话失败')
    }
  }
}

/**
 * 取会话的展示名称。
 *
 * @param conversation 会话
 * @returns 展示名称
 */
function titleOf(conversation: ConversationDto): string {
  return conversationTitle(conversation, conversation.peer)
}

/**
 * 取会话最新消息的预览文本。
 *
 * @param conversation 会话
 * @returns 预览文本
 */
function previewOf(conversation: ConversationDto): string {
  if (!conversation.latestMessage) {
    return '暂无消息'
  }
  return latestMessagePreview(conversation) || '[消息]'
}

/**
 * 取会话最新消息的时间。
 *
 * @param conversation 会话
 * @returns 时间文本
 */
function timeOf(conversation: ConversationDto): string {
  const createTime = conversation.latestMessage?.createTime
  return createTime ? formatTime(createTime) : ''
}
</script>

<template>
  <aside class="conversations">
    <header class="conversations__header">
      <span class="conversations__title">聊天</span>
      <el-button
        class="conversations__add"
        :icon="Plus"
        circle
        size="small"
        title="新增会话"
        @click="router.push({ name: 'conversation-create' })"
      />
    </header>

    <div ref="listEl" class="conversations__list scroll-y" @scroll="onScroll">
      <el-skeleton v-if="conversationStore.loading && conversationStore.list.length === 0" :rows="6" animated />

      <el-empty
        v-else-if="conversationStore.list.length === 0"
        description="还没有会话"
        :image-size="80"
      >
        <el-button type="primary" size="small" @click="router.push({ name: 'conversation-create' })">
          新增会话
        </el-button>
      </el-empty>

      <template v-else>
        <div
          v-for="item in conversationStore.list"
          :key="item.peer"
          class="conversation"
          :class="{
            'conversation--active': item.peer === activePeer,
            'conversation--disabled': !item.enabled,
          }"
          @click="openConversation(item)"
        >
          <el-badge
            class="conversation__badge"
            :value="item.unread ?? 0"
            :max="99"
            :hidden="!item.unread"
          >
            <el-avatar :size="36" class="conversation__avatar">
              {{ titleOf(item).charAt(0) }}
            </el-avatar>
          </el-badge>

          <div class="conversation__main">
            <div class="conversation__row">
              <span class="conversation__title">{{ titleOf(item) }}</span>
              <el-tag v-if="item.group" size="small" type="success" effect="plain">群</el-tag>
              <el-tag v-if="!item.enabled" size="small" type="info">已停用</el-tag>
              <span class="conversation__time">{{ timeOf(item) }}</span>
            </div>
            <div class="conversation__row">
              <span class="conversation__preview">{{ previewOf(item) }}</span>
            </div>
          </div>
        </div>

        <div v-if="conversationStore.loading" class="conversations__more">加载中…</div>
        <div v-else-if="!conversationStore.hasMore" class="conversations__more">没有更多了</div>
      </template>
    </div>
  </aside>
</template>

<style scoped>
.conversations {
  flex: none;
  display: flex;
  flex-direction: column;
  width: 280px;
  border-right: 1px solid #e4e7ed;
  background: #f7f7f7;
}

.conversations__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  border-bottom: 1px solid #e4e7ed;
}

.conversations__title {
  font-size: 16px;
  font-weight: 600;
}

.conversations__add {
  margin-left: auto;
}

.conversations__list {
  flex: 1;
  min-height: 0;
  padding: 8px 0;
}

.conversation {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.conversation:hover {
  background: #ededed;
}

/*
  按下就变色：切换偶尔要等一下才有反应，有了这个即时反馈，按下的一瞬间就能看到「点到了」，
  不会觉得是没点中。纯 CSS，不依赖脚本。
*/
.conversation:active {
  background: #e4e4e4;
}

.conversation--active {
  background: #e4e4e4;
}

.conversation--disabled {
  color: #b7b7b7;
}

.conversation--disabled .conversation__title,
.conversation--disabled .conversation__preview,
.conversation--disabled .conversation__time {
  color: #b7b7b7;
}

.conversation__avatar {
  flex: none;
  background: #07c160;
  color: #fff;
}

/* 未读红点挂在头像右上角 */
.conversation__badge {
  flex: none;
  line-height: 0;
}

.conversation__badge :deep(.el-badge__content) {
  border: none;
}

.conversation--disabled .conversation__avatar {
  background: #c0c4cc;
}

.conversation__main {
  flex: 1;
  min-width: 0;
}

.conversation__row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.conversation__title {
  overflow: hidden;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.conversation__time {
  margin-left: auto;
  flex: none;
  font-size: 12px;
  color: #a8abb2;
}

.conversation__preview {
  overflow: hidden;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.conversations__more {
  padding: 10px 0;
  font-size: 12px;
  color: #a8abb2;
  text-align: center;
}
</style>
