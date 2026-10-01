import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    outDir: '../e2echo-client/src/main/resources/static', // 改成你想要的目录，默认是 dist
    // 可选：输出到项目根目录之外
    emptyOutDir: true, // 输出到 root 外时，通常需要显式开启清空目录

    // 默认阈值 500 kB 是给“经网络分发”的页面定的，这里网页由客户端在 localhost 上提供，不经过
    // 网络，所以不再按它告警。入口 chunk 里绝大部分是 Element Plus 全量引入（约 700 kB，全量构建
    // 893 kB）——只用到 19 个组件，但这是有意的取舍：本地加载毫秒级，换取按需引入的配置与样式风险。
    // 阈值压在入口 chunk 之上，将来真有异常膨胀的 chunk 仍然会报出来。
    chunkSizeWarningLimit: 1000,
  }
})
