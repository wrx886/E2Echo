<script setup lang="ts">
import { onUnmounted, ref, watch } from 'vue'
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
 * <p>只列私聊会话：群聊不能作为群成员，直接让接口按 {@code group=false} 过滤，比取回来在前端筛更准
 * 也更省——以前是按会话分页取回来再筛，私聊占比低时用户得点很多次「加载更多」。搜索同样交给接口
 * （按对方或别名模糊匹配），这样没加载到的那几页也能搜到。</p>
 *
 * <p>「与自己」的会话仍然列出来但灰掉：它只是一条普通会话，判起来可靠，也能提前挡住没意义的操作。
 * 已经在群里的会话不做任何标记——成员是分页查询的，前端拿不到全量名单，判错了反而误导，成员是否
 * 已存在一律交给后端判断。</p>
 */

/** 每次拉取的会话条数。 */
const PAGE_SIZE = 50

/** 输入停止多久后才去查。 */
const SEARCH_DELAY = 300

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

/** 搜索框里的文字。 */
const keyword = ref('')

/** 已经发给接口的搜索词：接口返回的是它的结果，不能用输入框当前值判断空态。 */
const appliedKeyword = ref('')

/** 已加载的私聊会话。 */
const conversations = ref<ConversationDto[]>([])

/** 已经加载到第几页。 */
const pageNum = ref(0)

/** 是否还有更多会话可以加载。 */
const hasMore = ref(false)

/** 是否正在加载。 */
const loading = ref(false)

/** 搜索防抖定时器。 */
let searchTimer: ReturnType<typeof setTimeout> | null = null

onUnmounted(() => {
  if (searchTimer !== null) {
    clearTimeout(searchTimer)
  }
})

/**
 * 判断一条会话能不能作为群成员。
 *
 * @param item 会话
 * @returns 可选返回 true；「与自己」的会话不能选（群聊已经由接口过滤掉）
 */
function selectable(item: ConversationDto): boolean {
  return item.peer !== userStore.publicKey
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
    // group=false：只要私聊会话
    const result = await listConversations(page, PAGE_SIZE, appliedKeyword.value, false)
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
 * 搜索框内容变化：防抖后交给接口重新查第一页。
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
    void fetchPage(1)
  }, SEARCH_DELAY)
}

/**
 * 清空搜索：不用等防抖，直接回到全部私聊会话。
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
  void fetchPage(1)
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
  appliedKeyword.value = ''
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
      @input="onSearchInput"
      @clear="onSearchClear"
    />

    <div class="picker__list scroll-y">
      <el-skeleton v-if="loading && conversations.length === 0" :rows="4" animated />

      <el-empty
        v-else-if="conversations.length === 0"
        :image-size="60"
        :description="appliedKeyword.length > 0 ? '没有匹配的会话' : '还没有私聊会话，可以直接粘贴对方公钥添加'"
      />

      <template v-else>
        <div
          v-for="item in conversations"
          :key="item.peer"
          class="picker__item"
          :class="{ 'picker__item--disabled': !selectable(item) }"
          :title="selectable(item) ? '' : '不能把自己加为群成员'"
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
