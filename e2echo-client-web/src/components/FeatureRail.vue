<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound } from '@element-plus/icons-vue'
import UserProfile from '@/components/UserProfile.vue'

/**
 * 功能项。新增功能时在这里追加一项即可，侧边栏按数组渲染。
 */
interface Feature {
  /** 功能标识。 */
  key: string
  /** 展示名称。 */
  label: string
  /** 功能入口路径。 */
  path: string
  /** 图标组件。 */
  icon: Component
}

/**
 * 左侧功能栏。
 *
 * <p>目前只有「聊天」一个功能，高亮按当前路由是否在该功能的路径下判断。</p>
 */
const features: Feature[] = [
  { key: 'chat', label: '聊天', path: '/chat', icon: ChatDotRound },
]

const route = useRoute()
const router = useRouter()

/** 当前高亮的功能。 */
const activeKey = computed(() =>
  features.find((item) => route.path.startsWith(item.path))?.key ?? '')
</script>

<template>
  <nav class="feature-rail">
    <button
      v-for="item in features"
      :key="item.key"
      type="button"
      class="feature"
      :class="{ 'feature--active': item.key === activeKey }"
      @click="router.push(item.path)"
    >
      <el-icon :size="22">
        <component :is="item.icon" />
      </el-icon>
      <span class="feature__label">{{ item.label }}</span>
    </button>

    <!-- 底部个人信息：固定在功能栏下方 -->
    <div class="feature-rail__bottom">
      <UserProfile />
    </div>
  </nav>
</template>

<style scoped>
.feature-rail {
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 64px;
  padding: 16px 0;
  background: #2b2b2b;
}

.feature {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 52px;
  padding: 8px 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #a3a3a3;
  cursor: pointer;
  transition: background-color 0.2s, color 0.2s;
}

.feature:hover {
  background: #3a3a3a;
  color: #e8e8e8;
}

.feature--active {
  color: #07c160;
}

.feature__label {
  font-size: 12px;
}

.feature-rail__bottom {
  margin-top: auto;
}
</style>
