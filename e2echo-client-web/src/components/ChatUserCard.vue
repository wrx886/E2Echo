<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DocumentCopy } from '@element-plus/icons-vue'
import { getConversation } from '@/api'
import type { ConversationDto } from '@/api'
import { useConversationStore } from '@/stores/conversation'
import { copyText } from '@/utils/clipboard'
import { shortKey } from '@/utils/display'
import { showError } from '@/utils/feedback'

/**
 * 聊天和广播共用的用户小卡片。
 *
 * <p>点头像或 @ 别名弹出来：显示这个人的名称与完整公钥，并提供 @、已有会话的「私聊」「修改会话」
 * 或添加到会话；广播场景还可以进入用户主页，但会关闭会话操作，保持私聊和广播两个板块独立。</p>
 *
 * <p>群聊里除了群主，其它成员拿不到群成员名单，但每条消息都带着发送者公钥，所以点头像就是认识群成员
 * 的入口。</p>
 */

const props = withDefaults(defineProps<{
  /** 对方公钥。 */
  uid: string
  /** 是否显示 @ 操作；私聊和广播输入框可用。 */
  mentionable?: boolean
  /** 外部已经解析好的展示名称，优先于会话别名。 */
  preferredName?: string
  /** 是否显示广播用户主页入口。 */
  showProfile?: boolean
  /** 是否显示私聊、修改会话、添加到会话等会话操作。 */
  conversationActions?: boolean
}>(), {
  conversationActions: true,
})

const emit = defineEmits<{
  /** 做完操作或点外部时关掉卡片。 */
  'close': []
  /** 在当前输入框 @ 这位用户。 */
  'mention': [uid: string]
}>()

const router = useRouter()
const conversationStore = useConversationStore()

/** 与这个人的会话；null 表示确实没有。 */
const conversation = ref<ConversationDto | null>(null)

/** 是否正在查会话。 */
const loading = ref(false)

/** 展示名称：有会话就用别名，没有就退回公钥末几位。 */
const name = computed(() =>
  props.preferredName?.trim() || conversation.value?.alias || shortKey(props.uid, 5))

/**
 * 查会话：先看已加载的列表，没有再去 client 查一次。
 */
async function load(): Promise<void> {
  conversation.value = conversationStore.list.find((item) => item.peer === props.uid) ?? null
  if (conversation.value) {
    return
  }
  loading.value = true
  try {
    conversation.value = await getConversation(props.uid)
  } catch (error) {
    showError(error, '查询会话失败')
  } finally {
    loading.value = false
  }
}

watch(() => [props.uid, props.conversationActions] as const, () => {
  conversation.value = null
  loading.value = false
  if (!props.conversationActions) {
    return
  }
  void load()
}, { immediate: true })

/**
 * 进已有会话私聊。
 */
function openChat(): void {
  emit('close')
  void router.push({ name: 'chat', params: { peer: props.uid } })
}

/**
 * 修改已有会话（别名、是否启用等）。
 */
function openSettings(): void {
  emit('close')
  void router.push({ name: 'conversation-settings', params: { peer: props.uid } })
}

/**
 * 还没有会话：去新增会话页，对方公钥已经带上。
 */
function openCreate(): void {
  emit('close')
  void router.push({ name: 'conversation-create', query: { peer: props.uid } })
}

/**
 * 打开广播用户主页。
 */
function openProfile(): void {
  emit('close')
  void router.push({ name: 'broadcast-profile', params: { uid: props.uid } })
}

/**
 * 复制公钥。
 */
async function copyKey(): Promise<void> {
  const ok = await copyText(props.uid)
  if (ok) {
    ElMessage.success('已复制公钥')
  } else {
    ElMessage.error('复制失败，请手动选中复制')
  }
}

/**
 * 在当前输入框插入这个人的 @。
 */
function mentionUser(): void {
  emit('mention', props.uid)
  emit('close')
}
</script>

<template>
  <div class="user-card">
    <div class="user-card__name">{{ name }}</div>

    <div class="user-card__label">公钥</div>
    <div class="user-card__key">{{ uid }}</div>

    <div class="user-card__actions">
      <el-button
        v-if="mentionable"
        size="small"
        title="在输入框中 @ 这位用户"
        @click="mentionUser"
      >
        @
      </el-button>
      <el-button v-if="showProfile" type="primary" size="small" @click="openProfile">
        主页
      </el-button>
      <template v-if="conversationActions">
        <!-- 还没查完会话前先不给操作，免得点了一半又变成"私聊" -->
        <span v-if="loading" class="user-card__loading">查询会话中…</span>
        <template v-else-if="conversation">
          <el-button type="primary" size="small" @click="openChat">私聊</el-button>
          <el-button size="small" @click="openSettings">修改会话</el-button>
        </template>
        <el-button v-else type="primary" size="small" @click="openCreate">
          添加到会话
        </el-button>
      </template>
      <el-button
        class="user-card__copy"
        size="small"
        :icon="DocumentCopy"
        title="复制公钥"
        @click="copyKey"
      />
    </div>
  </div>
</template>

<style scoped>
.user-card__name {
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.user-card__label {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.user-card__key {
  /* 公钥固定 130 位、行数有限，直接把高度交给内容，别让最后一行被截在滚动区里 */
  margin-top: 4px;
  padding: 6px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  line-height: 1.45;
  word-break: break-all;
}

.user-card__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.user-card__loading {
  font-size: 12px;
  color: #909399;
}

.user-card__copy {
  margin-left: auto;
}
</style>
