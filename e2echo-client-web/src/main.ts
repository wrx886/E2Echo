import './assets/main.css'
import 'element-plus/dist/index.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus, { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'
import { setAuthFailedHandler } from './api'

// 会话失效时网页自己无法重新登录（登录票据只能由客户端生成），提示用户回到客户端重新打开网页。
// 接口层保证同一次失效只触发一次，避免并发请求弹出多条提示
setAuthFailedHandler(() => {
  ElMessage.warning('登录状态已失效，请在客户端中重新打开网页')
})

createApp(App)
  .use(createPinia())
  .use(router)
  .use(ElementPlus, { locale: zhCn })
  .mount('#app')
