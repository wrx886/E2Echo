import { createRouter, createWebHistory } from 'vue-router'

/**
 * 前端路由。
 *
 * <p>页面由客户端容器在根路径提供，登录也是从 {@code /auth/{票据}} 重定向到 {@code /}，因此路由
 * 使用 history 模式、不做修饰的路径。</p>
 *
 * <p>注意：客户端的安全拦截器会拦住除 {@code /auth/**} 以外的全部请求，子路径直接刷新时既命不中
 * 静态资源也没有后端转发，所以目前只保留根路径一个入口。</p>
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
    },
  ],
})

export default router
