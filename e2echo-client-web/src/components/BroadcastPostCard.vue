<script setup lang="ts">
import { computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, DocumentCopy } from '@element-plus/icons-vue'
import { BROADCAST_POST_TO, avatarSrc } from '@/api'
import type { BroadcastMessageRespVo } from '@/api'
import BroadcastAttachments from '@/components/BroadcastAttachments.vue'
import { useBroadcastStore } from '@/stores/broadcast'
import { copyText } from '@/utils/clipboard'
import { formatTime, shortKey, timestampFromId } from '@/utils/display'

/**
 * 广播内容卡片：帖子与评论共用。
 *
 * <p>作者昵称、头像来自用户资料（按公钥查，查不到就退回公钥末几位）；时间是消息 ID 前 16 位里的
 * 时间戳。二级回复会在正文上方标出回复了谁。帖子会带出它的 ID（48 位），便于复制给别人用 ID 检索；
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
  /** 打开某人的主页：带上这条内容的 ID，调用方返回时用它定位。 */
  'openUser': [uid: string, itemId: string]
}>()

const broadcastStore = useBroadcastStore()

/** 作者资料。 */
const author = computed(() => broadcastStore.profileOf(props.item.from))

/** 作者展示名。 */
const authorName = computed(() => author.value?.nickname ?? shortKey(props.item.from, 5))

/** 被回复者的展示名。 */
const replyName = computed(() => {
  const target = props.replyTo
  if (!target) {
    return ''
  }
  return broadcastStore.profileOf(target.from)?.nickname ?? shortKey(target.from, 5)
})

/** 发布时间。 */
const time = computed(() => {
  const timestamp = timestampFromId(props.item.id)
  return Number.isNaN(timestamp) ? '' : formatTime(timestamp)
})

/** 被回复内容的摘要。 */
const replyExcerpt = computed(() => {
  const text = props.replyTo?.message?.text ?? ''
  return text.length > 40 ? `${text.slice(0, 40)}…` : text
})

/** 是不是帖子：评论的 {@code to} 是帖子 ID。 */
const isPost = computed(() => props.item.to === BROADCAST_POST_TO)

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
}

watch(() => [props.item.from, props.replyTo?.from], loadProfiles, { immediate: true })
</script>

<template>
  <div
    class="post"
    :class="{ 'post--clickable': clickable }"
    :data-item-id="item.id"
    @click="emit('open')"
  >
    <el-avatar
      :size="36"
      class="post__avatar"
      :src="avatarSrc(author?.avatarUrl)"
      title="查看主页"
      @click.stop="emit('openUser', item.from, item.id)"
    >
      {{ authorName.charAt(0) }}
    </el-avatar>

    <div class="post__main">
      <div class="post__head">
        <span class="post__name" @click.stop="emit('openUser', item.from, item.id)">{{ authorName }}</span>
        <span class="post__time">{{ time }}</span>
      </div>

      <div v-if="replyTo" class="post__reply">
        回复 @{{ replyName }}：{{ replyExcerpt }}
      </div>

      <div class="post__text">{{ item.message?.text }}</div>

      <!-- 附件区不整体拦截点击：空白处仍然可以点开帖子，只有图片、播放器、下载链接自己拦 -->
      <BroadcastAttachments :files="item.message?.files ?? []" />

      <div class="post__actions">
        <el-button text size="small" :icon="ChatDotRound" @click.stop="emit('reply')">
          回复
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
}

.post__text {
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
