<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { DocumentCopy, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listConversations } from '@/api'
import type { ConversationDto } from '@/api'
import { useUserStore } from '@/stores/user'
import { copyText } from '@/utils/clipboard'
import { shortKey } from '@/utils/display'
import { showError } from '@/utils/feedback'

/**
 * 会话选取弹窗。
 *
 * <p>列出全部会话，但群聊会话和「与自己」的会话灰掉不可选：这两类只看会话自己的属性（是不是群聊、
 * 是不是自己），判断可靠，也能提前挡住没意义的操作。已经在群里的会话不做任何标记——成员和会话都是
 * 分页查询，前端拿不到全量名单，判错了反而误导，成员是否已存在一律交给后端判断。</p>
 *
 * <p>会话列表接口没有搜索和按类型过滤，只能分页取回来在前端筛，所以这里自己维护分页：打开时取
 * 第一页，取满一页时才显示「加载更多」。</p>
 */

/** 每次拉取的会话条数。 */
const PAGE_SIZE = 100

const props = defineProps<{
  /** 是否显示。 */
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  /** 选中一个会话：回填公钥，并带上展示名称供调用方提示。 */
  'pick': [payload: { peer: string; label: string }]
}>()

const userStore = useUserStore()

/** 已加载的会话，含私聊与群聊，展示前才过滤。 */
const conversations = ref<ConversationDto[]>([])

/** 已经加载到第几页。 */
const pageNum = ref(0)

/** 是否还有更多会话可以加载。 */
const hasMore = ref(false)

/** 是否正在加载。 */
const loading = ref(false)

/** 搜索词，按会话名称或公钥过滤。 */
const keyword = ref('')

/** 候选会话：全部会话按搜索词过滤。 */
const candidates = computed(() => {
  const word = keyword.value.trim().toLowerCase()
  return conversations.value.filter((item) => {
    if (word.length === 0) {
      return true
    }
    return item.alias.toLowerCase().includes(word) || item.peer.toLowerCase().includes(word)
  })
})

/**
 * 判断一条会话能不能作为群成员。
 *
 * @param item 会话
 * @returns 可选返回 true；群聊会话和「与自己」的会话不能选
 */
function selectable(item: ConversationDto): boolean {
  return !item.group && item.peer !== userStore.publicKey
}

/**
 * 取不可选的原因，用作悬浮提示。
 *
 * @param item 会话
 * @returns 不可选的原因
 */
function disabledReason(item: ConversationDto): string {
  return item.group ? '群聊会话不能作为群成员' : '不能把自己加为群成员'
}

/**
 * 取会话展示名称。
 *
 * @param item 会话
 * @returns 会话名称
 */
function titleOf(item: ConversationDto): string {
  return item.alias && item.alias.trim().length > 0 ? item.alias : shortKey(item.peer)
}

/**
 * 加载某一页会话。
 *
 * @param page 页码，从 1 开始
 */
async function fetchPage(page: number): Promise<void> {
  loading.value = true
  try {
    const result = await listConversations(page, PAGE_SIZE)
    if (page === 1) {
      conversations.value = result.content
    } else {
      // 翻页期间列表可能变化，按 peer 去重，避免同一条会话出现两次
      const known = new Set(conversations.value.map((item) => item.peer))
      conversations.value = [...conversations.value, ...result.content.filter((item) => !known.has(item.peer))]
    }
    pageNum.value = page
    hasMore.value = result.content.length === PAGE_SIZE
  } catch (error) {
    showError(error, '加载会话失败')
  } finally {
    loading.value = false
  }
}

/**
 * 选中一个会话：抛出选择结果并关闭弹窗（是否立即添加由调用方决定）。
 *
 * @param item 选中的会话
 */
function onPick(item: ConversationDto): void {
  if (!selectable(item)) {
    return
  }
  emit('pick', { peer: item.peer, label: titleOf(item) })
  emit('update:visible', false)
}

/**
 * 复制公钥。
 *
 * @param peer 会话对方的公钥
 */
async function onCopy(peer: string): Promise<void> {
  const ok = await copyText(peer)
  if (ok) {
    ElMessage.success('已复制公钥')
  } else {
    ElMessage.error('复制失败，请手动选中复制')
  }
}

// 每次打开都重新加载：成员名单和会话都可能在别处变过
watch(() => props.visible, (value) => {
  if (!value) {
    return
  }
  keyword.value = ''
  conversations.value = []
  pageNum.value = 0
  hasMore.value = false
  void fetchPage(1)
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="选择成员"
    width="480px"
    @update:model-value="emit('update:visible', $event)"
  >
    <el-input
      v-model="keyword"
      placeholder="搜索会话名称或公钥"
      clearable
      :prefix-icon="Search"
    />

    <div class="picker__list scroll-y">
      <el-skeleton v-if="loading && conversations.length === 0" :rows="4" animated />

      <el-empty
        v-else-if="candidates.length === 0"
        :image-size="60"
        :description="conversations.length === 0 ? '还没有会话，可以直接粘贴对方公钥添加' : '没有匹配的会话'"
      />

      <template v-else>
        <div
          v-for="item in candidates"
          :key="item.peer"
          class="picker__item"
          :class="{ 'picker__item--disabled': !selectable(item) }"
          :title="selectable(item) ? '' : disabledReason(item)"
          @click="onPick(item)"
        >
          <el-avatar :size="32" class="picker__avatar">
            {{ titleOf(item).charAt(0) }}
          </el-avatar>

          <div class="picker__main">
            <div class="picker__row">
              <span class="picker__name">{{ titleOf(item) }}</span>
            </div>
            <div class="picker__key" :title="item.peer">{{ item.peer }}</div>
          </div>

          <el-button
            :icon="DocumentCopy"
            text
            size="small"
            title="复制公钥"
            @click.stop="onCopy(item.peer)"
          />
        </div>

        <div v-if="hasMore" class="picker__more">
          <el-button size="small" :loading="loading" @click="fetchPage(pageNum + 1)">
            加载更多
          </el-button>
        </div>
      </template>
    </div>
  </el-dialog>
</template>

<style scoped>
.picker__list {
  max-height: 360px;
  margin-top: 12px;
}

.picker__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 6px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.picker__item:hover {
  background: #f5f7fa;
}

.picker__item--disabled {
  cursor: not-allowed;
  color: #b7b7b7;
}

.picker__item--disabled:hover {
  background: transparent;
}

.picker__avatar {
  flex: none;
  background: #07c160;
  color: #fff;
}

.picker__item--disabled .picker__avatar {
  background: #c0c4cc;
}

.picker__main {
  flex: 1;
  min-width: 0;
}

.picker__row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.picker__name {
  overflow: hidden;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.picker__key {
  overflow: hidden;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.picker__more {
  padding: 10px 0 4px;
  text-align: center;
}
</style>
