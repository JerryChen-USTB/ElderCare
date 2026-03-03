# ElderCare

ElderCare 是一个面向养老陪护场景的前后端分离项目，围绕老年用户、监护人和志愿者三类角色，提供日程管理、用药提醒、预约服务、紧急求助、远程协助、健康信息管理以及 AI 问答等能力。

## 项目结构

- `ElderCare-backend`：Spring Boot 后端服务，提供业务 API、WebSocket、AI 能力接入、文件上传等。
- `ElderCare-frontend`：uni-app 前端，包含老人端、监护人端、志愿者端页面。
- `database_table_design`：数据库建表脚本与示例数据。
- `data`：用户数据及运行期数据目录。
- `documents`：项目相关设计和第三方接入说明。
- `report`：阶段性报告材料。

## 主要功能

- 多角色登录与身份选择：老人、监护人、志愿者。
- 老人端：日程管理、用药提醒、紧急呼叫、预约服务、AI 聊天、远程协助。
- 监护人端：绑定老人、查看健康记录、管理用药提醒、紧急情况处理、视频通话。
- 志愿者端：任务管理、服务记录、日程安排、远程服务。
- AI 能力：接入 LangChain4j，支持聊天、知识检索、摘要、健康教育等。
- 音视频与语音：集成腾讯云 TRTC、语音转写/语音合成相关能力。

## 技术栈

### 后端

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL
- Redis
- WebSocket
- LangChain4j
- Tencent Cloud TRTC SDK

### 前端

- uni-app
- Vue 生态（uni-app 运行时）
- `@dcloudio/uni-ui`
- uv-ui 相关组件

## 环境要求

- JDK 17
- Maven（或直接使用仓库内 `mvnw` / `mvnw.cmd`）
- MySQL 8.x
- Redis 6.x 或更高版本
- HBuilderX（推荐，用于运行 uni-app 前端）

## 快速开始

### 1. 初始化数据库

1. 在 MySQL 中创建数据库：`eldercare_project`。
2. 导入主建表脚本：`database_table_design/eldercare_project.sql`。
3. 如需示例日程数据，可额外导入：`database_table_design/schedules_data_insert.sql`。

### 2. 配置后端

后端默认启用 `dev` 配置文件，配置入口如下：

- 主配置：`ElderCare-backend/src/main/resources/application.yml`
- 开发环境示例：`ElderCare-backend/src/main/resources/application-dev.yml.example`
- 本地开发配置：`ElderCare-backend/src/main/resources/application-dev.yml`

建议流程：

1. 参考 `application-dev.yml.example` 补齐本地 `application-dev.yml`。
2. 至少确认以下配置可用：
   - MySQL 连接信息
   - Redis 连接信息
   - AI 模型接口地址、API Key、模型名
3. 如需启用音视频或第三方能力，补齐以下配置或环境变量：
   - 腾讯云：`TENCENTCLOUD_SECRET_ID`、`TENCENTCLOUD_SECRET_KEY`、`TRTC_SDK_APP_ID`、`TRTC_SECRET_KEY`
   - 百度灵医：`BAIDU_LINGYI_AK`、`BAIDU_LINGYI_SK`
   - AI 接口：`AI_BASE_URL`、`AI_API_KEY`、`AI_MODEL`

注意：`application-dev.yml` 包含敏感信息，应仅保留本地使用，不要提交到版本库。

### 3. 启动后端

在项目根目录执行：

```powershell
cd ElderCare-backend
.\mvnw.cmd spring-boot:run
```

如果使用类 Unix 环境：

```bash
cd ElderCare-backend
./mvnw spring-boot:run
```

默认情况下，后端服务运行在 `http://localhost:8080`。

### 4. 启动前端

当前前端目录已包含 uni-app 项目结构，但未提供完整的 `npm scripts`。建议使用 HBuilderX：

1. 使用 HBuilderX 导入 `ElderCare-frontend` 目录。
2. 根据运行平台检查并调整接口地址：`ElderCare-frontend/utils/config.js`。
3. 选择目标平台运行（H5、App、小程序）。

当前前端接口配置中：

- H5 开发环境默认指向 `http://localhost:8080`
- App / 小程序环境需要改成你本机可访问的局域网 IP 或部署域名

## 测试

后端可执行：

```powershell
cd ElderCare-backend
.\mvnw.cmd test
```

## 说明

- 后端资源目录包含知识库素材、系统提示词、天气行政区划数据等运行所需文件，请勿随意删除。
- 根目录下的 `hs_err_pid*.log`、`replay_pid*.log` 是 JVM 崩溃/诊断日志，排查完成后可按需清理。
- 如果需要补充接口文档、部署说明或前端页面截图，建议在此 README 基础上继续扩展。