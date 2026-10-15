<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import type { BroadcastFileVo, BroadcastMessageRespVo } from '@/api'
import BroadcastComposer from '@/components/BroadcastComposer.vue'
import BroadcastPostCard from '@/components/BroadcastPostCard.vue'
import { useBroadcastStore } from '@/stores/broadcast'
import { useSubscribeStore } from '@/stores/subscribe'
import { useAnchorScroll } from '@/utils/anchorScroll'
import { showError } from '@/utils/feedback'

/**
 * 帖子详情：帖子本身 + 评论（含二级回复）。
 *
 * <p>评论是单级的，回复某条评论只是把正文里的 {@code replyToId} 指向它，所以这里在评论卡片上标出
 * "回复某某"，回复时也只需要记住被回复的那条评论。评论输入框固定在底部：点评论上的"回复"直接聚焦
 * 它，不用再滚回去找输入框。</p>
 */

const route = useRoute()
const router = useRouter()
const broadcastStore = useBroadcastStore()
const subscribeStore = useSubscribeStore()

/** 帖子 ID。 */
const postId = computed(() => String(route.params.id ?? ''))

/** 发布框，发布成功后清空它。 */
const composer = ref<InstanceType<typeof BroadcastComposer>>()

/** 正在回复的评论，为空表示直接评论帖子。 */
const replyTarget = ref<BroadcastMessageRespVo | null>(null)

/** 是否正在发布评论。 */
const publishing = ref(false)

/** 评论 ID -> 被回复的评论，用来显示"回复某某"。 */
const replyTargets = ref(new Map<string, BroadcastMessageRespVo>())

/** 内容滚动容器。 */
const listEl = ref<HTMLDivElement>()

/** 内容容器，用来观察内容高度变化。 */
const contentEl = ref<HTMLDivElement>()

/** 待定位的内容 ID：从评论跳走再回来时，把那条评论滚回可视区中间。 */
const pendingAnchor = ref('')

const anchor = useAnchorScroll({
  container: listEl,
  content: contentEl,
  anchorId: pendingAnchor,
})

/** 正在回复的评论作者展示名。 */
const replyToName = computed(() => {
  const target = replyTarget.value
  if (!target) {
    return ''
  }
  // 名字和卡片保持一致：订阅别名优先，其次资料昵称
  return subscribeStore.aliasOf(target.from) || broadcastStore.profileOf(target.from)?.nickname || ''
})

watch(postId, async (value) => {
  if (value.length === 0) {
    return
  }
  replyTarget.value = null
  replyTargets.value = new Map()
  try {
    if (broadcastStore.post?.id === value) {
      // 已经加载过这个帖子（例如从用户主页返回）：不重新拉，直接用现有内容
    } else {
      broadcastStore.postAnchorId = ''
      // 帖子与评论一起加载：进帖前会把上一个帖子的内容清掉
      await broadcastStore.openPost(value)
    }
    await resolveReplies()
    // 把上次跳走时那条评论放回可视区中间；取走即清，避免重复跳
    pendingAnchor.value = broadcastStore.consumePostAnchor()
    await anchor.start()
  } catch (error) {
    showError(error, '加载帖子失败')
  }
}, { immediate: true })

/**
 * 用户自己滚动：不要再把视图拽回锚点。
 */
function onUserScroll(): void {
  anchor.stop()
}

/**
 * 刷新：帖子与评论都按第一次打开那样重新拉一遍，然后回到顶部。
 */
async function onRefresh(): Promise<void> {
  const id = postId.value
  if (id.length === 0) {
    return
  }
  try {
    // openPost 会清掉旧内容与旧评论，再从第一页拉
    await broadcastStore.openPost(id)
    await resolveReplies()
    anchor.reset()
  } catch (error) {
    showError(error, '刷新失败')
  }
}

/**
 * 把评论里引用到的"被回复评论"补齐：已加载的评论里找不到时单独查一次。
 */
async function resolveReplies(): Promise<void> {
  for (const comment of broadcastStore.comments) {
    const id = comment.message.replyToId
    if (!id || replyTargets.value.has(id)) {
      continue
    }
    try {
      const target = await broadcastStore.findComment(id)
      if (target) {
        replyTargets.value.set(id, target)
      }
    } catch {
      // 被回复的评论查不到只是少一行提示，不影响评论本身展示
    }
  }
}

/**
 * 返回上一页；直接打开（没有历史）时回时间线。
 */
function onBack(): void {
  if (window.history.state?.back) {
    router.back()
    return
  }
  void router.push({ name: 'broadcast-feed' })
}

/**
 * 回复某条评论：记下目标，并顺手把它的作者资料查出来（提示里要显示昵称）。
 *
 * @param item 被回复的评论
 */
function startReply(item: BroadcastMessageRespVo): void {
  replyTarget.value = item
  void broadcastStore.loadProfile(item.from).catch(() => {})
  composer.value?.focus()
}

/**
 * 取消回复（直接评论帖子），仍然聚焦输入框。
 */
function cancelReply(): void {
  replyTarget.value = null
  composer.value?.focus()
}

/**
 * 打开某个用户的主页。
 *
 * @param uid    用户公钥
 * @param itemId 这条内容的 ID，回来时用它定位
 */
function openUser(uid: string, itemId: string): void {
  broadcastStore.postAnchorId = itemId
  void router.push({ name: 'broadcast-profile', params: { uid } })
}

/**
 * 发表评论或回复。
 *
 * @param text  正文
 * @param files 附件
 */
async function onComment(text: string, files: BroadcastFileVo[]): Promise<void> {
  // 帖子没加载出来（不存在、正在加载或加载失败）就没有可评论的对象
  if (!broadcastStore.post) {
    ElMessage.warning('帖子不存在，无法评论')
    return
  }
  publishing.value = true
  try {
    await broadcastStore.createComment(postId.value, text, files, replyTarget.value?.id)
    composer.value?.clear()
    replyTarget.value = null
    await resolveReplies()
    ElMessage.success('已发布')
  } catch (error) {
    showError(error, '发布失败')
  } finally {
    publishing.value = false
  }
}

/**
 * 切换评论排序：重新从头加载（游标和排序方向要一起换，不能接着旧游标翻）。
 */
async function onOrderChange(): Promise<void> {
  try {
    await broadcastStore.loadComments(postId.value, true)
  } catch (error) {
    showError(error, '加载评论失败')
  }
}

/**
 * 加载更多评论。
 */
async function onLoadMore(): Promise<void> {
  try {
    await broadcastStore.loadComments(postId.value, false)
    await resolveReplies()
  } catch (error) {
    showError(error, '加载更多评论失败')
  }
}
</script>

<template>
  <section class="post-view">
    <header class="post-view__header">
      <el-button
        :icon="ArrowLeft"
        circle
        size="small"
        title="返回"
        @click="onBack"
      />
      <span class="post-view__title">帖子</span>
      <el-button
        class="post-view__refresh"
        :icon="Refresh"
        circle
        size="small"
        title="刷新"
        :loading="broadcastStore.postLoading || broadcastStore.commentsLoading"
        @click="onRefresh"
      />
    </header>

    <div
      ref="listEl"
      class="post-view__body scroll-y"
      @wheel.passive="onUserScroll"
      @touchstart.passive="onUserScroll"
    >
      <div ref="contentEl">
      <el-skeleton v-if="broadcastStore.postLoading" :rows="5" animated />

      <el-empty v-else-if="!broadcastStore.post" description="帖子不存在" :image-size="80" />

      <template v-else>
        <BroadcastPostCard
          :item="broadcastStore.post"
          @open-user="openUser"
          @reply="cancelReply"
        />

        <div class="post-view__section">
          <span>评论</span>
          <el-radio-group
            v-model="broadcastStore.commentsOrder"
            size="small"
            @change="onOrderChange"
          >
            <el-radio-button value="DESC">最新在前</el-radio-button>
            <el-radio-button value="ASC">最早在前</el-radio-button>
          </el-radio-group>
        </div>

        <el-skeleton v-if="broadcastStore.commentsLoading && broadcastStore.comments.length === 0" :rows="3" animated />

        <el-empty
          v-else-if="broadcastStore.comments.length === 0"
          description="还没有评论"
          :image-size="60"
        />

        <template v-else>
          <BroadcastPostCard
            v-for="item in broadcastStore.comments"
            :key="item.id"
            :item="item"
            :reply-to="item.message.replyToId ? (replyTargets.get(item.message.replyToId) ?? null) : null"
            @open-user="openUser"
            @reply="startReply(item)"
          />

          <div class="post-view__more">
            <el-button
              v-if="broadcastStore.commentsHasMore"
              size="small"
              :loading="broadcastStore.commentsLoading"
              @click="onLoadMore"
            >
              加载更多评论
            </el-button>
            <span v-else class="post-view__end">没有更多评论了</span>
          </div>
        </template>
      </template>
      </div>
    </div>

    <!--
      评论输入框固定在底部：回复某条评论时不用滚回去找它。
      只有帖子确实加载出来才显示——帖子不存在时没有可评论的对象，让输入框消失比让人白写一遍好。
    -->
    <BroadcastComposer
      v-if="broadcastStore.post"
      ref="composer"
      class="post-view__composer"
      placeholder="说点什么…"
      submit-text="评论"
      :submitting="publishing"
      :reply-to-name="replyToName"
      @submit="onComment"
      @cancel-reply="cancelReply"
    />
  </section>
</template>

<style scoped>
.post-view {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  background: #f5f5f5;
}

.post-view__header {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.post-view__title {
  font-size: 15px;
  font-weight: 600;
}

.post-view__refresh {
  margin-left: auto;
}

.post-view__body {
  flex: 1;
  min-height: 0;
  background: #fff;
}

.post-view__composer {
  flex: none;
  border-top: 1px solid #e4e7ed;
}

.post-view__section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-top: 8px solid #f5f5f5;
  border-bottom: 1px solid #f0f2f5;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.post-view__more {
  padding: 14px 0 20px;
  text-align: center;
}

.post-view__end {
  font-size: 12px;
  color: #a8abb2;
}
</style>
