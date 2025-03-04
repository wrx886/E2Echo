# E2Echo Client

**简介**:E2Echo 前端项目以 ECC（采用 secp256k1 曲线）加密技术为核心，遵循后端 API 进行开发，实现消息收发、群聊管理、会话管理、文件收发等功能。

**Version**:1.0-SNAPSHOT

**备注**:发送手机验证码的逻辑（com.github.wrx886.e2echo.server.web.service.SmsService）没有实现，而是将信息打印到服务端的控制台。

# 构建

- 环境：

  - JDK 17
  - Maven 3.9

- 使用 maven 完成构建

  ```bash
  # 转到 server 目录下
  cd client
  # 清空已构建目标
  mvn clean
  # 开始构建
  mvn package
  ```

- 构建完毕后，可在 client\target 中找到对应的 jar 包

