# E2Echo 加解密插件

## 项目概述

**E2Echo Plugin** 是一个基于Java开发的插件项目，主要提供了加密通信功能。该项目集成了图形界面和API接口，支持ECC（secp256k1）算法进行消息加密传输。

## 主要特性

- **ECC 加密**：基于椭圆曲线算法（secp256k1）对消息进行加密、解密、签名、验证
- **图形化界面**：提供友好的用户交互界面
- **API 接口**：支持 REST API 调用
- **文档集成**：集成 Knife4j 在线 API 文档

## 技术栈

- **后端框架**：SpringBoot3
- **加密算法**：椭圆曲线算法（secp256k1）
- **API 文档**：Knife4j
- **GUI**：JavaFX

## 快速开始

### 环境要求

- JDK 17
- Maven 3.9.12

### 构造运行

```bash
# 克隆项目
git clone https://github.com/wrx886/E2Echo.git

# 进入项目目录
cd e2echo-plugin

# 编译打包
mvn clean package

# 运行项目
java -jar target/e2echo-plugin-1.0.jar

```

### 访问地址

- **API 文档**：`http://localhost:35010/doc.html`
- **REST API**：`http://localhost:35010`
- **注**：端口号可能发生变化，以图形化界面中使用端口为准

## 贡献

欢迎提交Issue和Pull Request来改进项目。
