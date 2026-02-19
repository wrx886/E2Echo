# E2Echo - End-to-End Encrypted Echo

E2Echo 是一个端到端加密的聊天软件，旨在为用户提供安全、私密的通信体验。

## 项目背景

E2Echo 诞生于对隐私保护日益增长的需求背景下，旨在为个人和团队提供安全可靠的即时通讯解决方案。传统的聊天工具往往存在数据泄露风险，而 E2Echo 通过端到端加密技术，确保用户通信内容完全私密可控。

## 特性

- **不信任服务器**：服务器被视为潜在的攻击者或数据窥探者。服务器仅负责路由加密后的数据包，无法获取任何消息的明文内容。
- **客户端持有密钥**：所有加密密钥均在用户设备上生成、使用和存储，绝不将私钥或对称密钥明文上传至服务器。
- **公钥即身份**：用户的唯一身份标识（ID）就是其 secp256k1 公钥。这确保了身份由密码学算法生成，无法被伪造或篡改，从根本上杜绝了身份冒用，也简化了密钥交换过程。
- **私聊端到端加密**: 使用 secp256k1 椭圆曲线加密算法，确保消息在传输过程中无法被第三方窃听或篡改，保障通信的绝对隐私。
- **群聊对称加密**：使用 AES256 算法，由群主生成随机会话密钥，并通过成员的公钥进行端到端加密分发。群内所有消息通过该共享会话密钥进行加密传输，确保群聊通信的安全与效率。
- **安全的公钥交换**：服务器不直接参与公钥交换，用户可通过软件内置的加密私聊功能，选择可信第三方（如中间人）传递或使用其他安全的方案传递公钥，确保密钥传递的安全性。
- **群成员密钥共享机制**：“堵不如疏”，群成员分享密钥是无法完全禁止的，所以该机制允许群成员将密钥分享给非群成员，使得其可以参与群聊。

## 技术栈

- **服务端**：
  - **框架**：Spring Boot 3 Web
  - **数据库**：MySQL + MyBatis-Plus
  - **加解密**：Bouncy Castle
  - **文档**：Knife4j
- **客户端**：
  - **框架**：Spring Boot 3
  - **数据库**：H2 数据库
  - **图形化界面**：JavaFX
  - **通信**：Feign + Java-WebSocket
  - **加密**：Bouncy Castle

## 快速开始

### 客户端

从 releases 下载 e2echo-client ，解压后直接运行即可

### 服务端

- **初始化**：
  - 从 releases 下载 e2echo-server ，解压
  - 修改解压后文件夹中的 application.yml 配置，配置MySQL数据库
  - 在 MySQL 中运行 `init.sql` 文件，初始化数据库
- **运行**：在jar文件所在目录中运行：`java -jar e2echo-server.jar` 即可

## 构建

在开始之前，请确保您的系统满足以下要求：

- **Java**: 17 或更高版本
- **Maven**: 3.9 或更高版本
- **MySQL**: 8.4 或更高版本（服务端运行时必需）
- **Git**: 用于克隆代码库

### 获取代码

```bash
# 克隆项目
git clone https://github.com/wrx886/E2Echo.git
cd e2echo
```

### 客户端

```bash
cd e2echo-client
mvn clean package
```

构建结果为 `e2echo-client-starter/target` 下的 jar 文件

### 服务端

```bash
cd e2echo-server
mvn clean package
```

构建结果为 `e2echo-server/target` 下的 jar 文件

## 贡献指南

欢迎任何形式的贡献！请遵循以下步骤：
**1.** Fork 本仓库
**2.** 创建新分支：**`git checkout -b feature/your-feature-name`**
**3.** 提交更改并编写清晰的 Commit Message
**4.** 推送到远程分支并发起 Pull Request

## 许可证

本项目采用 [GNU General Public License v3.0 (GPLv3)](https://www.gnu.org/licenses/gpl-3.0.html) 开源协议，具体包括：

- 你可以自由地运行、研究、分享和修改本软件。
- 如果你分发修改后的版本，必须同样以 GPLv3 协议开源你的代码。
- 任何基于本项目的衍生作品也必须遵守相同的许可条款。

更多信息请参阅 [LICENSE](LICENSE) 文件。

---

**E2Echo** - 安全通信，从端到端加密开始。
