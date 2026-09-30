# e2echo-client-web

E2Echo 客户端的网页部分，Vue 3 + TypeScript + Vite。

## 这个模块做什么

构建产物直接输出到 `e2echo-client/src/main/resources/static/`（见 `vite.config.ts` 的
`build.outDir`），由客户端内嵌的 Spring Boot 容器作为静态资源提供，因此网页与客户端
**同端口、同源**，接口不需要跨域配置，也不需要处理 token。

登录由客户端完成：客户端在「打开网页」时生成一次性票据，用系统默认浏览器打开
`http://localhost:{端口}/auth/{票据}`，服务端校验票据并把当前会话标记为已登录，再重定向到 `/`。
网页自己无法发起登录，所以**必须由客户端打开**。

会话用 Cookie 维持。失效时接口返回的不是 401，而是 HTTP 200
加 `{"code":"-1","message":"认证失败"}`，接口层据此提示用户回到客户端重新打开网页。

## 目录结构

```
src/
  api/            接口层：axios 实例、统一响应处理、各接口封装、SSE 通知
  components/     布局与通用组件：功能栏、个人信息框、会话列表、消息气泡、输入区
  assets/         全局样式
  router/         前端路由（hash 模式）
  stores/         Pinia 状态：当前用户、会话列表、当前会话消息、通知通道
  utils/          展示格式化与错误提示
  views/          页面：聊天空状态、聊天页、会话设置页
  App.vue         根组件
  main.ts         入口：注册 Pinia、Router、Element Plus
```

接口层是页面与客户端后端之间的唯一通道，页面不直接使用 axios。

页面路由：

```
/                重定向到 /chat
/chat            未选中会话时的空状态
/chat/new        新增会话
/chat/:peer      聊天页（:peer 是会话对方，已 URL 编码）
/chat/:peer/settings  会话设置（修改）
```

路由用 hash 模式：客户端容器没有单页应用转发，直接访问或刷新子路径会被当成静态资源请求而返回
错误 JSON，hash 变化不产生新的请求，刷新任意页面都能正常回到界面。

## 开发与调试

因为登录必须由客户端发起，本模块**不能用 `npm run dev` 单独调试**（dev server 的端口与客户端
容器端口不同，会话也不共享）。调试流程为：

1. `npm run build`，产物落到 `e2echo-client/src/main/resources/static/`；
2. 启动客户端并完成登入；
3. 主界面点击「打开网页」。

改完前端后重新执行 `npm run build` 并刷新页面即可。注意客户端读取的是 classpath 上的静态资源，
如果客户端是从打包产物运行的，还需要重新打包客户端才能让改动生效。

需要 Node `^22.18.0 || >=24.12.0`（见 `package.json` 的 `engines`）。

## 命令

```sh
npm install        # 安装依赖
npm run build      # 类型检查 + 构建，产物直接进客户端静态资源目录
npm run type-check # 只做类型检查
```
