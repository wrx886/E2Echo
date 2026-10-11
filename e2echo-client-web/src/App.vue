<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import BroadcastNav from '@/components/BroadcastNav.vue'
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
const route = useRoute()

/** 中间栏跟着功能走：聊天用会话列表，广播用广播导航。 */
const isBroadcast = computed(() => route.path.startsWith('/broadcast'))

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
    <BroadcastNav v-if="isBroadcast" />
    <ConversationList v-else />
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
