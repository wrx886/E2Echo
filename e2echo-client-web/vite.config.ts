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
  }
})
