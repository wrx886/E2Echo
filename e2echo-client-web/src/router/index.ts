import { createRouter, createWebHashHistory } from 'vue-router'

/**
 * 前端路由。
 *
 * <p>用 hash 模式：客户端容器没有单页应用转发，子路径直接刷新会被当成静态资源请求而返回错误 JSON，
 * hash 变化不会产生新的请求，刷新任意子页面都能正常回到页面。</p>
 */
const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      redirect: '/chat',
    },
    {
      path: '/chat',
      name: 'chat-empty',
      component: () => import('@/views/ChatEmptyView.vue'),
    },
    {
      // 必须排在 /chat/:peer 之前，否则 new 会被当成会话对方
      path: '/chat/new',
      name: 'conversation-create',
      component: () => import('@/views/ConversationSettingsView.vue'),
    },
    {
      path: '/chat/:peer',
      name: 'chat',
      component: () => import('@/views/ChatView.vue'),
    },
    {
      path: '/chat/:peer/settings',
      name: 'conversation-settings',
      component: () => import('@/views/ConversationSettingsView.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/chat',
    },
  ],
})

export default router
