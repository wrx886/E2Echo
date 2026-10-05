<script setup lang="ts">
import { computed, ref } from 'vue'
import { EMOJI_GROUPS, loadRecentEmojis, pushRecentEmoji } from '@/utils/emoji'

/**
 * 表情选择器。
 *
 * <p>组件自带触发按钮与面板：面板顶部是「最近使用」，下面一行切换分类，再下面是表情网格。选中后
 * 面板不关闭，方便连着挑几个；关闭方式为点击面板外或按 Esc。</p>
 */

const emit = defineEmits<{
  /** 选中一个表情。 */
  'pick': [emoji: string]
}>()

/** 「最近使用」那一栏的标识，避免和分类名撞车。 */
const RECENT_KEY = 'recent'

/** 最近使用的表情（本机记录）。 */
const recent = ref<string[]>(loadRecentEmojis())

/** 当前选中的分类。 */
const activeKey = ref(RECENT_KEY)

/** 分类栏：先是最近使用，再是各个分类。 */
const tabs = computed(() => [
  { key: RECENT_KEY, name: '最近使用' },
  ...EMOJI_GROUPS.map((group) => ({ key: group.name, name: group.name })),
])

/** 当前分类下的表情。 */
const current = computed(() => {
  if (activeKey.value === RECENT_KEY) {
    return recent.value
  }
  return EMOJI_GROUPS.find((group) => group.name === activeKey.value)?.emojis ?? []
})

/**
 * 选中一个表情：抛出给调用方，同时记进最近使用。
 *
 * @param emoji 表情
 */
function onPick(emoji: string): void {
  recent.value = pushRecentEmoji(emoji)
  emit('pick', emoji)
}
</script>

<template>
  <el-popover :width="360" placement="top-end" trigger="click" :show-after="80">
    <template #reference>
      <el-button class="emoji__trigger" title="表情">
        <!--
          自己画的线性笑脸：Element Plus 没有笑脸图标，直接用 emoji 当图标会和旁边几个线性图标
          风格不一致。描边用 currentColor，尺寸跟随按钮字号，和别的图标按钮保持一致。
        -->
        <el-icon>
          <svg
            viewBox="0 0 24 24"
            width="1em"
            height="1em"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
          >
            <circle cx="12" cy="12" r="9" />
            <path d="M8.4 14.2a4.6 4.6 0 0 0 7.2 0" />
            <circle cx="9.2" cy="9.6" r="1" fill="currentColor" stroke="none" />
            <circle cx="14.8" cy="9.6" r="1" fill="currentColor" stroke="none" />
          </svg>
        </el-icon>
      </el-button>
    </template>

    <div class="emoji">
      <div class="emoji__tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          class="emoji__tab"
          :class="{ 'emoji__tab--active': tab.key === activeKey }"
          @click="activeKey = tab.key"
        >
          {{ tab.name }}
        </button>
      </div>

      <!-- 空状态单独一块，和网格一样高：切换分类时面板高度不跟着内容变 -->
      <div v-if="current.length === 0" class="emoji__empty">还没有用过的表情</div>

      <div v-else class="emoji__grid scroll-y">
        <button
          v-for="(emoji, index) in current"
          :key="`${emoji}-${index}`"
          type="button"
          class="emoji__item"
          @click="onPick(emoji)"
        >
          {{ emoji }}
        </button>
      </div>
    </div>
  </el-popover>
</template>

<style scoped>
.emoji__tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.emoji__tab {
  padding: 3px 8px;
  border: none;
  border-radius: 4px;
  background: transparent;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
}

.emoji__tab:hover {
  background: #f5f7fa;
}

.emoji__tab--active {
  background: #ecf5ff;
  color: #409eff;
}

.emoji__grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  /* 每行固定高度、从顶部排列：容器高度是固定的，但行不能被拉伸填满 */
  grid-auto-rows: 36px;
  align-content: start;
  gap: 2px;
  /* 固定高度：分类之间表情多少差别很大，跟着内容变会让面板忽高忽低 */
  height: 220px;
  margin-top: 8px;
}

.emoji__item {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  border: none;
  border-radius: 4px;
  background: transparent;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.emoji__item:hover {
  background: #f5f7fa;
}

.emoji__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 220px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
