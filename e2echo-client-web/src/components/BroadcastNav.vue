<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

/**
 * 广播功能的中间栏：在时间线和自己的资料之间切换。
 *
 * <p>和会话列表占同一个位置——选中广播功能时显示它，选中聊天时显示会话列表。</p>
 */

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 导航项。"我的帖子"在自己的资料页里，所以这里只有两项。 */
const items = computed(() => [
  { key: 'feed', label: '全部帖子', path: '/broadcast' },
  { key: 'me', label: '我的资料', path: `/broadcast/user/${userStore.publicKey}` },
])

/**
 * 当前选中的导航项。
 *
 * <p>"我的资料"只在自己主页上高亮：看别人的主页时两边都不选中，免得看起来像在看自己的资料。
 * 帖子详情算时间线的一部分，仍然高亮"全部帖子"。</p>
 */
const activeKey = computed(() => {
  if (route.name === 'broadcast-profile') {
    const uid = String(route.params.uid ?? '')
    return uid.length > 0 && uid === userStore.publicKey ? 'me' : ''
  }
  return 'feed'
})

onMounted(() => {
  // 自己的资料页要用公钥拼路径
  void userStore.ensureLoaded().catch(() => {})
})

/**
 * 切换导航项。
 *
 * @param item 导航项
 */
function open(item: { key: string; path: string }): void {
  if (item.key === 'me' && userStore.publicKey.length === 0) {
    ElMessage.warning('还没有取到自己的公钥，请稍后再试')
    return
  }
  void router.push(item.path)
}

</script>

<template>
  <aside class="broadcast-nav">
    <header class="broadcast-nav__header">
      <span class="broadcast-nav__title">广播</span>
    </header>

    <div class="broadcast-nav__list scroll-y">
      <div
        v-for="item in items"
        :key="item.key"
        class="broadcast-nav__item"
        :class="{ 'broadcast-nav__item--active': item.key === activeKey }"
        @click="open(item)"
      >
        {{ item.label }}
      </div>
    </div>
  </aside>
</template>

<style scoped>
.broadcast-nav {
  flex: none;
  display: flex;
  flex-direction: column;
  width: 280px;
  border-right: 1px solid #e4e7ed;
  background: #f7f7f7;
}

.broadcast-nav__header {
  padding: 14px 16px;
  border-bottom: 1px solid #e4e7ed;
}

.broadcast-nav__title {
  font-size: 16px;
  font-weight: 600;
}

.broadcast-nav__list {
  flex: 1;
  min-height: 0;
  padding: 8px 0;
}

.broadcast-nav__item {
  padding: 10px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.broadcast-nav__item:hover {
  background: #ededed;
}

.broadcast-nav__item--active {
  background: #e4e4e4;
  font-weight: 500;
}
</style>
