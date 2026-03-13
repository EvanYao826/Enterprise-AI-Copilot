# AI Knowledge System (RAG 知识库问答系统)

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![SpringBoot](https://img.shields.io/badge/SpringBoot-3.2.3-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2-61dafb.svg)](https://reactjs.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.109-009688.svg)](https://fastapi.tiangolo.com/)

面向面试官的项目介绍：这是一个“前后端 + AI 微服务”的全栈 RAG 知识库系统。管理端上传多格式文档后，Python 服务完成解析、切分、向量化入库；用户端以对话方式检索并生成答案，同时展示来源并支持在线预览/下载。

---

## 项目亮点 (Highlights)
- RAG 链路打通：上传 -> 解析 -> 向量索引 -> 检索 -> 生成 -> 来源追溯
- 云存储集成：文档上传至七牛云 Kodo，AI 服务支持从 URL 拉取并解析
- 端到端可观测：管理端仪表盘展示用户数、文档数、提问趋势、AI 命中率、热点与未命中问题
- 来源体验优化：来源只展示真实文件名（去除 UUID 前缀），并按后缀展示不同图标
- 数据一致性治理：删除文档时同步清理数据库记录、云端对象和向量索引，避免“幽灵文档”

---

## 系统架构
```
React (User/Admin)
    |
    |  HTTP
    v
Spring Boot (API, Auth, Admin)
    |                         \
    |  parse(file_url, doc_id) \ ask(question)
    v                           v
FastAPI (RAG Service) ------> FAISS (local index)
    |
    |  download file from Qiniu URL
    v
Parser + Splitter + Embeddings
```

---

## 核心功能
**用户端**
- 手机号验证码注册/登录 + JWT 鉴权
- 对话式问答（RAG）：返回答案 + 参考来源
- 来源支持“在线预览/下载”（直接打开七牛云链接）

**管理端**
- 仪表盘：用户数、文档数、提问趋势、AI 命中率、热门问题/文档、未命中问题
- 知识库管理：文档上传/删除、自动解析向量化
- 通知管理：发布与维护系统通知

---

## 技术栈
**Java 后端**
- Spring Boot 3.2.x, MyBatis-Plus, MySQL, Redis
- 七牛云 Kodo（对象存储）

**Python AI 服务**
- FastAPI + LangChain
- 向量库：FAISS（本地持久化索引）
- Embedding：DashScope（可选）/ HuggingFace（兜底）

**前端**
- React 18 + Vite + React Router
- ECharts（管理端数据可视化）

---

## 快速启动 (Quick Start)
**1) 数据库**
- 创建数据库：`ai_knowledge_db`
- 执行初始化脚本：`sql/init.sql`、`sql/update_admin.sql`

**2) Java 后端**
- 修改配置：`src/main/resources/application.yml`
- 启动：`AiKnowledgeSystemApplication.java`（默认端口 8080）

**3) Python AI 服务**
- 安装依赖：
  ```bash
  cd python-service
  pip install -r requirements.txt
  ```
- 配置环境变量（推荐 `.env`，或在系统环境变量中配置）：
  ```env
  DASHSCOPE_API_KEY=sk-your-key
  ```
- 启动：
  ```bash
  python main.py
  ```
  默认端口：8000

**4) 前端**
- 启动：
  ```bash
  cd frontend
  npm install
  npm run dev
  ```

---

## 配置说明
**七牛云 Kodo（必选）**
- `qiniu.accessKey` / `qiniu.secretKey`：七牛云 AK/SK（不要提交到仓库）
- `qiniu.bucket`：空间名
- `qiniu.domain`：绑定域名（可不带协议；系统会自动补全为 http://）

**阿里云短信（可选）**
- `aliyun.sms.*`：不配置则走模拟模式（控制台输出验证码）

---

## 默认账号
- 管理端登录：`/admin/login`，默认 `admin / admin123`

---

## 已知限制
- FAISS 对按 doc_id 删除支持有限；当前实现会尽可能清理 docstore 中的分片，并持久化更新。生产环境建议切换到原生支持删除/过滤的向量数据库（如 Milvus/Chroma/Pinecone）。
