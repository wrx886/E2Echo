<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, DocumentCopy, Share } from '@element-plus/icons-vue'
import { BROADCAST_POST_TO, avatarSrc } from '@/api'
import type { BroadcastMessageRespVo } from '@/api'
import BroadcastAttachments from '@/components/BroadcastAttachments.vue'
import BroadcastText from '@/components/BroadcastText.vue'
import FloatingUserCard from '@/components/FloatingUserCard.vue'
import { useBroadcastStore } from '@/stores/broadcast'
import { useSubscribeStore } from '@/stores/subscribe'
import { copyText } from '@/utils/clipboard'
import { formatTime, shortKey, timestampFromId } from '@/utils/display'

/**
 * 广播内容卡片：帖子与评论共用。
 *
 * <p>作者名字优先用订阅别名（订阅时给自己起的本机名字），其次用用户资料的昵称，都没有就退回公钥末
 * 几位；头像取名字首字。时间是消息 ID 前 16 位里的时间戳。二级回复会在正文上方标出回复了谁。
 * 帖子会带出它的 ID（48 位），便于复制给别人用 ID 检索；
 * 评论不带，因为评论的 {@code to} 指向帖子，展示 ID 容易和帖子混淆。</p>
 */

const props = defineProps<{
  /** 帖子或评论。 */
  item: BroadcastMessageRespVo
  /** 被回复的评论，二级回复时传。 */
  replyTo?: BroadcastMessageRespVo | null
  /** 是否可以点开详情。 */
  clickable?: boolean
}>()

const emit = defineEmits<{
  /** 点开详情。 */
  'open': []
  /** 回复这条内容。 */
  'reply': []
  /** 转发这条内容：始终创建一个新的一级帖子。 */
  'forward': [item: BroadcastMessageRespVo]
  /** 打开当前帖子转发的那条原广播。 */
  'openForward': [target: BroadcastMessageRespVo, sourceId: string]
  /** 快速 @ 卡片作者。 */
  'mentionUser': [uid: string]
  /** 打开某人的主页：带上这条内容的 ID，调用方返回时用它定位。 */
  'openUser': [uid: string, itemId: string]
}>()

const broadcastStore = useBroadcastStore()
const subscribeStore = useSubscribeStore()

/** 作者资料。 */
const author = computed(() => broadcastStore.profileOf(props.item.from))

/** 作者展示名：订阅别名优先，其次是资料昵称，最后公钥末几位。 */
const authorName = computed(() =>
  subscribeStore.aliasOf(props.item.from)
  || author.value?.nickname
  || shortKey(props.item.from, 5))

/** 这条内容的作者有没有被当前用户订阅：名字右边标一下。 */
const subscribed = computed(() => subscribeStore.isSubscribed(props.item.from))

/** 被回复者的展示名。 */
const replyName = computed(() => {
  const target = props.replyTo
  if (!target) {
    return ''
  }
  return subscribeStore.aliasOf(target.from)
    || broadcastStore.profileOf(target.from)?.nickname
    || shortKey(target.from, 5)
})

/** 发布时间。 */
const time = computed(() => {
  const timestamp = timestampFromId(props.item.id)
  return Number.isNaN(timestamp) ? '' : formatTime(timestamp)
})

/** 是不是帖子：评论的 {@code to} 是帖子 ID。 */
const isPost = computed(() => props.item.to === BROADCAST_POST_TO)

/** 一级帖子转发时所引用的原广播 ID。 */
const forwardId = computed(() =>
  isPost.value ? props.item.message?.replyToId ?? '' : '')

/** 被转发的原广播。 */
const forwardTarget = ref<BroadcastMessageRespVo | null>(null)

/** 原广播是否正在加载。 */
const forwardLoading = ref(false)

/** 原广播是否不存在或读取失败。 */
const forwardMissing = ref(false)

/** 头像用户卡片的锚点。 */
const avatarCardAnchor = ref<HTMLElement | null>(null)

/** 正文 @ 的用户卡片目标。 */
const mentionCardTarget = ref<{ uid: string; anchor: HTMLElement } | null>(null)

/** 被转发内容的作者展示名。 */
const forwardName = computed(() => {
  const target = forwardTarget.value
  if (!target) {
    return ''
  }
  return subscribeStore.aliasOf(target.from)
    || broadcastStore.profileOf(target.from)?.nickname
    || shortKey(target.from, 5)
})

/**
 * 复制帖子 ID。
 */
async function onCopyId(): Promise<void> {
  const ok = await copyText(props.item.id)
  if (ok) {
    ElMessage.success('已复制帖子 ID')
  } else {
    ElMessage.error('复制失败，请手动选中复制')
  }
}

/**
 * 加载作者（以及被回复者）的资料，查不到不影响展示。
 */
function loadProfiles(): void {
  void broadcastStore.loadProfile(props.item.from).catch(() => {})
  if (props.replyTo) {
    void broadcastStore.loadProfile(props.replyTo.from).catch(() => {})
  }
  if (forwardTarget.value) {
    void broadcastStore.loadProfile(forwardTarget.value.from).catch(() => {})
  }
}

/**
 * 加载一级帖子转发的原广播。
 */
async function loadForward(): Promise<void> {
  const id = forwardId.value
  forwardTarget.value = null
  forwardMissing.value = false
  if (!id) {
    return
  }
  forwardLoading.value = true
  try {
    const target = await broadcastStore.findMessage(id)
    const post = target?.to === BROADCAST_POST_TO ? target : null
    forwardTarget.value = post
    forwardMissing.value = post === null
    if (post) {
      void broadcastStore.loadProfile(post.from).catch(() => {})
    }
  } catch {
    forwardMissing.value = true
  } finally {
    forwardLoading.value = false
  }
}

/**
 * 点开被转发的原广播。
 */
async function onOpenForward(): Promise<void> {
  let target = forwardTarget.value
  if (!target && forwardId.value) {
    await loadForward()
    target = forwardTarget.value
  }
  if (target) {
    emit('openForward', target, props.item.id)
  }
}

/**
 * 点击正文里的 @ 名称，打开对应用户主页。
 *
 * @param uid    被提及者公钥
 * @param anchor 点击的 @ 元素
 */
function onMentionClick(uid: string, anchor: HTMLElement): void {
  mentionCardTarget.value = { uid, anchor }
}

/**
 * 点击正文区域打开当前帖子，头像区域交给用户卡片处理。
 *
 * @param event 点击事件
 */
function onCardOpen(event: MouseEvent): void {
  const target = event.target as HTMLElement
  if (target.closest('.post__avatar-wrap')) {
    return
  }
  emit('open')
}

/**
 * 第一次点头像时挂载唯一一张用户卡片。
 */
function openAvatarCard(event: MouseEvent): void {
  avatarCardAnchor.value = event.currentTarget as HTMLElement
}

/**
 * 从用户卡片快速 @ 作者。
 *
 * @param uid 作者公钥
 */
function mentionCardUser(uid: string): void {
  avatarCardAnchor.value = null
  emit('mentionUser', uid)
}

/**
 * 从正文 @ 用户卡片再次执行 @。
 *
 * @param uid 用户公钥
 */
function mentionFromCard(uid: string): void {
  mentionCardTarget.value = null
  emit('mentionUser', uid)
}

watch(() => [props.item.from, props.replyTo?.from, forwardTarget.value?.from], loadProfiles, { immediate: true })
watch(forwardId, loadForward, { immediate: true })
</script>

<template>
  <div
    class="post"
    :class="{ 'post--clickable': clickable }"
    :data-item-id="item.id"
    @click="onCardOpen"
  >
    <div class="post__avatar-wrap">
      <el-avatar
        :size="36"
        class="post__avatar"
        :src="avatarSrc(author?.avatarUrl)"
        title="查看用户"
        @click.stop="openAvatarCard"
      >
        {{ authorName.charAt(0) }}
      </el-avatar>
    </div>

    <div class="post__main">
      <div class="post__head">
        <span class="post__name" @click.stop="emit('openUser', item.from, item.id)">{{ authorName }}</span>
        <!-- 作者是当前用户订阅的人：标一下，和自己发的、没订阅的内容区分开 -->
        <el-tag v-if="subscribed" size="small" type="success" effect="plain">已订阅</el-tag>
        <span class="post__time">{{ time }}</span>
      </div>

      <div v-if="replyTo" class="post__reply">
        回复 @{{ replyName }}：<BroadcastText
          :text="replyTo.message?.text ?? ''"
          @mention-click="onMentionClick"
        />
      </div>

      <BroadcastText
        class="post__text"
        :text="item.message?.text ?? ''"
        @mention-click="onMentionClick"
      />

      <!-- 附件区不整体拦截点击：空白处仍然可以点开帖子，只有图片、播放器、下载链接自己拦 -->
      <BroadcastAttachments :files="item.message?.files ?? []" />

      <div
        v-if="forwardId"
        class="post__forward"
        role="button"
        tabindex="0"
        @click.stop="onOpenForward"
        @keydown.enter.prevent="onOpenForward"
        @keydown.space.prevent="onOpenForward"
      >
        <span class="post__forward-head">
          转发 @{{ forwardName || '原广播' }}
        </span>
        <span class="post__forward-text">
          <template v-if="forwardLoading">正在加载转发内容…</template>
          <template v-else-if="forwardMissing">原帖子不存在、不是一级帖子或暂时无法读取</template>
          <BroadcastText
            v-else
            :text="forwardTarget?.message?.text ?? ''"
            @mention-click="onMentionClick"
          />
        </span>
      </div>

      <div class="post__actions">
        <el-button text size="small" :icon="ChatDotRound" @click.stop="emit('reply')">
          回复
        </el-button>
        <el-button
          v-if="isPost"
          text
          size="small"
          :icon="Share"
          @click.stop="emit('forward', item)"
        >
          转发
        </el-button>

        <template v-if="isPost">
          <span class="post__id" :title="item.id">{{ item.id }}</span>
          <el-button
            text
            size="small"
            :icon="DocumentCopy"
            title="复制帖子 ID"
            @click.stop="onCopyId"
          />
        </template>
      </div>
    </div>

    <FloatingUserCard
      v-if="mentionCardTarget"
      :uid="mentionCardTarget.uid"
      :anchor="mentionCardTarget.anchor"
      :preferred-name="subscribeStore.aliasOf(mentionCardTarget.uid)
        || broadcastStore.profileOf(mentionCardTarget.uid)?.nickname
        || shortKey(mentionCardTarget.uid, 5)"
      mentionable
      show-profile
      :conversation-actions="false"
      @mention="mentionFromCard"
      @close="mentionCardTarget = null"
    />

    <FloatingUserCard
      v-if="avatarCardAnchor"
      :uid="item.from"
      :anchor="avatarCardAnchor"
      :preferred-name="authorName"
      mentionable
      show-profile
      :conversation-actions="false"
      @mention="mentionCardUser"
      @close="avatarCardAnchor = null"
    />
  </div>
</template>

<style scoped>
.post {
  display: flex;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f2f5;
  background: #fff;
}

.post--clickable {
  cursor: pointer;
}

.post--clickable:hover {
  background: #fafafa;
}

.post__avatar {
  flex: none;
  background: #07c160;
  color: #fff;
  cursor: pointer;
}

.post__avatar-wrap {
  position: relative;
  flex: none;
}

.post__main {
  flex: 1;
  min-width: 0;
}

.post__head {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.post__name {
  font-size: 14px;
  font-weight: 600;
}

.post__name:hover {
  color: #409eff;
  cursor: pointer;
}

.post__time {
  font-size: 12px;
  color: #a8abb2;
}

.post__reply {
  margin-top: 4px;
  padding: 4px 8px;
  border-left: 2px solid #dcdfe6;
  border-radius: 2px;
  background: #f7f8fa;
  font-size: 12px;
  line-height: 1.6;
  color: #909399;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.post__forward {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 3px;
  margin-top: 6px;
  padding: 7px 9px;
  overflow: hidden;
  border: 0;
  border-left: 3px solid #a0cfff;
  border-radius: 3px;
  background: #f5f7fa;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.post__forward:hover {
  background: #edf2f8;
}

.post__forward-head {
  overflow: hidden;
  font-size: 12px;
  color: #409eff;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.post__forward-text {
  overflow: hidden;
  font-size: 12px;
  line-height: 1.6;
  color: #909399;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.post__forward-text :deep(.broadcast-text) {
  white-space: nowrap;
}

.post__text {
  display: block;
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.post__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
}

.post__id {
  overflow: hidden;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  color: #c0c4cc;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
