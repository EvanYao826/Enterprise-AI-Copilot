# 🤖 AI 知识管理系统 (AI Knowledge System)

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![SpringBoot](https://img.shields.io/badge/SpringBoot-3.2.3-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2-61dafb.svg)](https://reactjs.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.109-009688.svg)](https://fastapi.tiangolo.com/)

> 一个基于 RAG (Retrieval-Augmented Generation) 架构的全栈 AI 知识库系统。支持多格式文档上传至云存储、智能切分向量化、基于语义的混合检索以及结合大模型（LLM）的精准问答。包含完整的前台用户端和后台管理端。

---

## ✨ 核心功能

### 🧑‍💻 用户端 (Client)
*   **安全认证**: 
    *   手机号验证码注册/登录（集成阿里云短信服务/支持模拟模式）。
    *   JWT 身份鉴权。
*   **智能问答**:
    *   类似 ChatGPT 的流式对话体验。
    *   基于私有知识库的精准回答。
    *   **智能来源展示**: 回答后附带参考来源，支持文件类型图标显示，去除冗余 ID。
    *   **在线预览/下载**: 支持直接点击来源跳转至七牛云进行在线预览或下载。
    *   支持多轮对话上下文记忆。
*   **会话管理**:
    *   历史会话记录保存与查询。
    *   自动生成会话标题。

### 🛡️ 管理端 (Admin)
*   **仪表盘**: 
    *   系统数据可视化概览（ECharts 图表展示用户数、文档数、提问趋势、AI 命中率）。
    *   热门文档与未命中问题统计。
*   **用户管理**:
    *   用户列表查询与分页。
    *   用户状态管理（一键封禁/解封）。
*   **知识库管理**:
    *   **云存储支持**: 集成七牛云对象存储 (Kodo)，文档上传后自动同步至云端。
    *   **多格式支持**: 支持 PDF, TXT, Markdown, Word, Excel, PPT 等多种格式。
    *   **自动处理**: 上传即触发 Python 服务从云端下载文档进行清洗、切分 (Chunking) 和向量化 (Embedding)。
    *   **完整生命周期管理**: 文档删除时同步清理数据库记录、七牛云文件以及向量库索引，防止“幽灵文档”。
*   **通知管理**:
    *   发布系统通知，支持富文本编辑。
*   **日志审计**:
    *   全量问答日志记录（提问、回答、时间）。
    *   用于分析热点问题和优化知识库质量。

---

## 🛠️ 技术栈

### 后端 (Java)
*   **框架**: Spring Boot 3.2.3
*   **ORM**: MyBatis Plus 3.5.5
*   **数据库**: MySQL 8.0
*   **缓存**: Redis (验证码存储、会话缓存)
*   **对象存储**: 七牛云 Kodo (Qiniu Cloud)
*   **工具**: Hutool, Lombok, Aliyun SDK (SMS)

### AI 服务 (Python)
*   **框架**: FastAPI (高性能异步 Web 框架)
*   **LLM 编排**: LangChain
*   **向量库**: FAISS (本地高效向量检索)
*   **网络请求**: Requests (支持从 URL 下载文档解析)
*   **模型**: 
    *   Embedding: 阿里云通义千问 / HuggingFace (可选)
    *   Chat: 阿里云通义千问 (Qwen-Turbo/Plus)

### 前端 (React)
*   **框架**: React 18 + Vite
*   **路由**: React Router v6
*   **可视化**: ECharts
*   **UI 组件**: 原生 CSS + 响应式布局
*   **网络请求**: Axios (封装拦截器)

---

## 🚀 部署指南

### 1. 前置准备
在运行项目之前，请确保您的环境已安装以下软件：
*   **JDK**: 17 或更高版本
*   **Node.js**: 18 或更高版本
*   **Python**: 3.10 或更高版本
*   **MySQL**: 8.0 (字符集推荐 utf8mb4)
*   **Redis**: 3.0+

### 2. 数据库初始化
1.  登录 MySQL，创建一个新的数据库：`ai_knowledge_db`。
2.  执行项目根目录下的 `sql/init.sql` 脚本，初始化基础表结构。
3.  执行 `sql/update_admin.sql` 脚本，初始化管理员表及默认账号。

### 3. 配置与启动 (Java 后端)
1.  打开 `src/main/resources/application.yml`。
2.  修改数据库配置 (`spring.datasource`) 为你的 MySQL 账号密码。
3.  修改 Redis 配置 (`spring.data.redis`)。
4.  **配置七牛云**: 在 `qiniu` 下填入 `accessKey`, `secretKey`, `bucket` 和 `domain`。
5.  (可选) 配置阿里云短信 `aliyun.sms`，如果不配置，系统将默认在控制台打印验证码。
6.  启动 `AiKnowledgeSystemApplication.java`。
    *   服务端口: **8080**

### 4. 配置与启动 (Python AI 服务)
1.  进入 `python-service` 目录。
2.  创建 `.env` 文件，填入你的阿里云 API Key：
    ```env
    DASHSCOPE_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx
    ```
3.  安装依赖：
    ```bash
    pip install -r requirements.txt
    ```
4.  启动服务：
    ```bash
    python main.py
    ```
    *   服务端口: **8000**

### 5. 配置与启动 (前端)
1.  进入 `frontend` 目录。
2.  安装依赖：
    ```bash
    npm install
    ```
3.  启动开发服务器：
    ```bash
    npm run dev
    ```
    *   访问地址: `http://localhost:3000` (或 5173，视 Vite 配置而定)

---

## 🔑 默认账号

### 管理员后台
*   **登录地址**: `http://localhost:3000/admin/login`
*   **默认账号**: `admin`
*   **默认密码**: `admin123`

### 普通用户
*   **登录地址**: `http://localhost:3000/login`
*   **注册方式**: 使用手机号获取验证码注册（控制台查看验证码）。

---

## 📂 目录结构

```
ai-knowledge-system/
├── src/main/java/          # Java 后端源码 (业务逻辑、七牛云集成)
├── frontend/               # React 前端源码 (ECharts 图表、Chat 组件优化)
├── python-service/         # Python AI 微服务源码 (文档解析、向量检索)
├── sql/                    # 数据库初始化脚本
├── vector_store/           # 向量数据库索引文件
└── README.md               # 项目文档
```

## 🤝 贡献与支持
欢迎提交 Issue 和 Pull Request！
