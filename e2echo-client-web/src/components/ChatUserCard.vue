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
 * 聊天里的用户小卡片。
 *
 * <p>点消息头像弹出来：显示这个人的名称（有会话就用会话别名）与完整公钥，并给出能做的事——已经有
 * 会话就给「私聊」「修改会话」，还没有就给「添加到会话」。</p>
 *
 * <p>群聊里除了群主，其它成员拿不到群成员名单，但每条消息都带着发送者公钥，所以点头像就是认识群成员
 * 的入口。</p>
 */

const props = defineProps<{
  /** 对方公钥。 */
  uid: string
}>()

const emit = defineEmits<{
  /** 做完操作或点外部时关掉卡片。 */
  'close': []
}>()

const router = useRouter()
const conversationStore = useConversationStore()

/** 与这个人的会话；null 表示确实没有。 */
const conversation = ref<ConversationDto | null>(null)

/** 是否正在查会话。 */
const loading = ref(false)

/** 展示名称：有会话就用别名，没有就退回公钥末几位。 */
const name = computed(() => conversation.value?.alias ?? shortKey(props.uid, 5))

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

watch(() => props.uid, () => {
  conversation.value = null
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
</script>

<template>
  <div class="user-card">
    <div class="user-card__name">{{ name }}</div>

    <div class="user-card__label">公钥</div>
    <div class="user-card__key">{{ uid }}</div>

    <div class="user-card__actions">
      <!-- 还没查完会话前先不给操作，免得点了一半又变成"私聊" -->
      <span v-if="loading" class="user-card__loading">查询会话中…</span>
      <template v-else-if="conversation">
        <el-button type="primary" size="small" @click="openChat">私聊</el-button>
        <el-button size="small" @click="openSettings">修改会话</el-button>
      </template>
      <el-button v-else type="primary" size="small" @click="openCreate">
        添加到会话
      </el-button>
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
  font-size: 14px;
  font-weight: 600;
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
  font-size: 12px;
  line-height: 1.5;
  word-break: break-all;
}

.user-card__actions {
  display: flex;
  align-items: center;
  gap: 8px;
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
