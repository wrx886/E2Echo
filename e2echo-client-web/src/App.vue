<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { RouterView } from 'vue-router'
import ConversationList from '@/components/ConversationList.vue'
import FeatureRail from '@/components/FeatureRail.vue'
import { useNoticeStore } from '@/stores/notice'
import { useUserStore } from '@/stores/user'

/**
 * 应用外壳：左侧功能栏 + 会话区 + 内容区。
 *
 * <p>通知连接与当前用户身份在这里初始化：一次建立后对所有页面生效，页面自己不再关心。</p>
 */

const noticeStore = useNoticeStore()
const userStore = useUserStore()

onMounted(() => {
  // 身份只用于区分消息气泡，取不到不影响其他功能，所以这里不打断
  void userStore.ensureLoaded().catch(() => {})
  noticeStore.start()
})

onUnmounted(() => {
  noticeStore.stop()
})
</script>

<template>
  <div class="app-shell">
    <FeatureRail />
    <ConversationList />
    <main class="app-shell__content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  height: 100%;
}

.app-shell__content {
  display: flex;
  flex: 1;
  min-width: 0;
}
</style>
