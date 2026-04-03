# 🚀 AI Knowledge System - 企业级智能知识库问答平台

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![SpringBoot](https://img.shields.io/badge/SpringBoot-3.2.3-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2-61dafb.svg)](https://reactjs.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.109-009688.svg)](https://fastapi.tiangolo.com/)
[![Milvus](https://img.shields.io/badge/Milvus-2.4-00B4D8.svg)](https://milvus.io/)
[![Redis](https://img.shields.io/badge/Redis-7.0-DC382D.svg)](https://redis.io/)

## 📖 项目简介

**AI Knowledge System** 是一个基于 **RAG（检索增强生成）** 架构的企业级智能知识库问答平台。系统采用 **微服务架构**，整合了 Spring Boot 后端、React 前端和 Python AI 服务，为企业提供完整的知识管理和智能问答解决方案。

### ✨ 核心价值
- **智能文档处理**：支持多格式文档（PDF/Word/TXT等）自动解析、向量化存储
- **精准语义检索**：基于 Milvus 向量数据库的语义相似度搜索
- **实时流式对话**：SSE 流式输出提供流畅的 AI 对话体验
- **上下文感知**：智能对话历史管理和多轮上下文理解
- **高性能架构**：Redis + Caffeine 多级缓存系统，毫秒级响应
- **生产级部署**：完整的监控、安全和数据一致性保障

---

## 项目状态 (Project Status)

**当前状态**：✅ **核心功能已完整实现**，项目已具备生产级能力

### ✅ 已完成的重大更新（2026-04-03）
1. **向量数据库迁移**：已从FAISS成功迁移到**Milvus**，支持高效向量检索和文档删除
2. **多级缓存系统**：实现**Redis + Caffeine**两级缓存架构，大幅提升系统性能
3. **对话上下文记忆**：完整的上下文记忆系统，支持短期/长期记忆和智能摘要
4. **SSE流式输出框架**：完整的流式输出框架，支持实时AI响应

### 🚀 核心功能状态
- ✅ **用户认证系统**：JWT + 手机验证码登录
- ✅ **知识库管理**：文档上传/解析/向量化
- ✅ **智能问答**：RAG检索增强生成 + 来源追溯
- ✅ **对话管理**：完整的对话历史管理
- ✅ **性能优化**：多级缓存 + 异步处理
- ✅ **向量数据库**：Milvus生产级部署
- ⚠️ **SSE流式输出**：框架完整，需配置AI API密钥测试

### 📋 近期优化计划
- 完善SSE流式输出的实际测试
- 添加API文档（Swagger/OpenAPI）
- 部署优化和容器化
- 性能监控和告警系统

**贡献与反馈**：欢迎提出建议和贡献代码，共同完善系统功能。

---

## 项目亮点 (Highlights)
- RAG 链路打通：上传 -> 解析 -> 向量索引 -> 检索 -> 生成 -> 来源追溯
- 云存储集成：文档上传至七牛云 Kodo，AI 服务支持从 URL 拉取并解析
- 端到端可观测：管理端仪表盘展示用户数、文档数、提问趋势、AI 命中率、热点与未命中问题
- 来源体验优化：来源只展示真实文件名（去除 UUID 前缀），并按后缀展示不同图标
- 数据一致性治理：删除文档时同步清理数据库记录、云端对象和向量索引，避免“幽灵文档”

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        React 前端 (User/Admin)                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  • 用户端：智能对话界面、文档预览、历史管理                   │  │
│  │  • 管理端：仪表盘、知识库管理、缓存监控、系统配置             │  │
│  └──────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ HTTP/SSE (JWT 认证)
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Spring Boot 后端 (API Gateway)                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  • 用户认证：JWT + 手机验证码                                │  │
│  │  • 业务逻辑：对话管理、文档管理、缓存服务                    │  │
│  │  • 多级缓存：Caffeine(本地) + Redis(分布式)                  │  │
│  │  • 异步处理：@Async + ExecutorService                        │  │
│  └──────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ REST API / SSE Stream
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   Python AI 服务 (RAG Engine)                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  • 文档处理：多格式解析、文本切分、向量化                    │  │
│  │  • 向量检索：Milvus 向量数据库 (生产级)                      │  │
│  │  • AI 模型：通义千问 + DashScope Embeddings                  │  │
│  │  • 流式输出：SSE 实时响应、上下文记忆                        │  │
│  └──────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ 七牛云 Kodo
                                ▼
                        ┌───────────────┐
                        │  文档存储     │
                        │  (对象存储)   │
                        └───────────────┘
```

### 🔄 数据流程
1. **文档上传** → 七牛云存储 → Python 解析 → Milvus 向量化
2. **用户提问** → Spring Boot 路由 → Python RAG 检索 → AI 生成 → 流式返回
3. **缓存策略** → 本地缓存优先 → Redis 缓存 → 数据库持久化

---

## 🎯 核心功能

### 👤 用户端功能
| 功能模块 | 描述 | 状态 |
|---------|------|------|
| **用户认证** | 手机验证码注册/登录 + JWT 鉴权 | ✅ 已实现 |
| **智能问答** | RAG 检索增强生成，返回答案 + 参考来源 | ✅ 已实现 |
| **流式对话** | SSE 实时流式响应，流畅的 AI 对话体验 | ⚠️ 框架完整，需API密钥 |
| **上下文记忆** | 智能对话历史管理，支持多轮上下文理解 | ✅ 已实现 |
| **文档预览** | 来源文档在线预览/下载（七牛云链接） | ✅ 已实现 |
| **对话管理** | 对话历史查看、置顶、删除、搜索 | ✅ 已实现 |

### 👨‍💼 管理端功能
| 功能模块 | 描述 | 状态 |
|---------|------|------|
| **数据仪表盘** | 用户数、文档数、提问趋势、AI 命中率统计 | ✅ 已实现 |
| **知识库管理** | 文档上传/删除、自动解析向量化、分类管理 | ✅ 已实现 |
| **缓存监控** | Redis + Caffeine 多级缓存命中率监控 | ✅ 已实现 |
| **性能分析** | 响应时间统计、热点问题分析、未命中问题追踪 | ✅ 已实现 |
| **系统配置** | 通知管理、系统参数配置、用户权限管理 | ✅ 已实现 |

### 🏗️ 系统特性
| 特性 | 描述 | 优势 |
|------|------|------|
| **🔍 智能检索** | 基于 Milvus 的语义向量相似度搜索 | 精准匹配，支持复杂查询 |
| **⚡ 高性能缓存** | Redis + Caffeine 多级缓存架构 | 毫秒级响应，高并发支持 |
| **🧠 上下文感知** | 短期记忆(Redis) + 长期记忆(数据库) | 智能对话，多轮理解 |
| **🌊 流式输出** | SSE 实时流式响应框架 | 流畅体验，实时反馈 |
| **🔒 数据一致性** | 文档删除同步清理数据库、云存储、向量索引 | 避免”幽灵文档”，数据一致 |
| **📊 可观测性** | 完整的监控指标和性能分析 | 系统健康可视，问题快速定位 |

---

## 🛠️ 技术栈

### 🟢 Java 后端
| 技术组件 | 版本 | 用途 |
|---------|------|------|
| **Spring Boot** | 3.2.x | 后端主框架，REST API 开发 |
| **MyBatis-Plus** | 3.5.x | ORM 框架，数据库操作增强 |
| **MySQL** | 8.0 | 关系型数据库，业务数据存储 |
| **Redis** | 7.0 | 分布式缓存，会话和热点数据 |
| **Caffeine** | 3.x | 本地缓存，毫秒级响应优化 |
| **Spring Security** | 6.2.x | 安全认证，JWT + 权限控制 |
| **七牛云 Kodo** | - | 对象存储，文档文件管理 |
| **Spring @Async** | - | 异步处理，提升系统吞吐量 |

### 🐍 Python AI 服务
| 技术组件 | 版本 | 用途 |
|---------|------|------|
| **FastAPI** | 0.109+ | 高性能 Python Web 框架 |
| **LangChain** | 0.1.x | AI 应用开发框架，RAG 流程管理 |
| **Milvus** | 2.4+ | 生产级向量数据库，语义检索 |
| **通义千问** | qwen-plus | 大语言模型，智能问答生成 |
| **DashScope** | - | 阿里云 Embeddings 服务 |
| **PyMuPDF** | - | PDF 文档解析 |
| **python-docx** | - | Word 文档解析 |
| **SSE (Server-Sent Events)** | - | 流式输出，实时 AI 响应 |

### ⚛️ 前端
| 技术组件 | 版本 | 用途 |
|---------|------|------|
| **React** | 18.2 | 前端主框架，组件化开发 |
| **Vite** | 5.x | 构建工具，开发服务器 |
| **React Router** | 6.x | 路由管理，SPA 导航 |
| **Axios** | 1.x | HTTP 客户端，API 调用 |
| **ECharts** | 5.x | 数据可视化，管理端图表 |
| **Element Plus** | 2.x | UI 组件库，界面组件 |
| **原生 ReadableStream** | - | SSE 流式数据处理 |
| **React Context + Hooks** | - | 状态管理，全局状态共享 |

### 🗄️ 基础设施
| 组件 | 用途 | 部署方式 |
|------|------|----------|
| **Docker** | 容器化部署 | 可选 |
| **Nginx** | 反向代理，负载均衡 | 生产环境推荐 |
| **Prometheus** | 监控指标收集 | 可选 |
| **Grafana** | 监控数据可视化 | 可选 |

---

## 🚀 快速启动

### 📋 环境要求
- **Java**: JDK 17+
- **Python**: 3.9+
- **Node.js**: 18+
- **MySQL**: 8.0+
- **Redis**: 7.0+
- **Milvus**: 2.4+ (可选，推荐生产环境)

### 🗄️ 步骤 1：数据库准备
```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE ai_knowledge_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 执行初始化脚本
mysql -u root -p ai_knowledge_db < sql/init.sql
mysql -u root -p ai_knowledge_db < sql/update_admin.sql
```

### ☕ 步骤 2：Java 后端启动
```bash
# 1. 配置数据库连接
# 编辑 src/main/resources/application.yml
# 修改 MySQL 和 Redis 连接信息

# 2. 配置七牛云（必需）
# 在 application.yml 中配置：
# qiniu:
#   accessKey: your-access-key
#   secretKey: your-secret-key
#   bucket: your-bucket-name
#   domain: your-domain.com

# 3. 启动服务
# 方式一：IDE 运行 AiKnowledgeSystemApplication.java
# 方式二：Maven 打包运行
mvn clean package
java -jar target/ai-knowledge-system-*.jar

# 默认端口：8080
```

### 🐍 步骤 3：Python AI 服务启动
```bash
# 1. 进入 Python 服务目录
cd python-service

# 2. 安装依赖
pip install -r requirements.txt

# 3. 配置环境变量
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，配置以下关键项：
# MILVUS_HOST=192.168.150.102    # Milvus 服务地址
# MILVUS_PORT=19530              # Milvus 端口
# USE_MILVUS=true                # 启用 Milvus（推荐）
# DASHSCOPE_API_KEY=sk-your-key  # 阿里云 DashScope API 密钥（必需）

# 4. 启动服务
python main.py

# 默认端口：8000
# 访问 http://localhost:8000/docs 查看 API 文档
```

#### 🔄 Milvus 数据迁移（可选）
如果从 FAISS 迁移到 Milvus：
```bash
# 调用迁移接口
curl -X POST http://localhost:8000/api/vector-store/migrate

# 或使用 FAISS 作为 fallback
# 在 .env 中设置：USE_MILVUS=false
```

### ⚛️ 步骤 4：前端启动
```bash
# 1. 进入前端目录
cd frontend

# 2. 安装依赖
npm install

# 3. 配置 API 地址（可选）
# 编辑 .env 文件，修改后端地址：
# VITE_API_BASE_URL=http://localhost:8080

# 4. 启动开发服务器
npm run dev

# 默认端口：3000
# 访问 http://localhost:3000 查看前端
```

### 👥 默认账号
- **用户端**：手机验证码注册/登录
- **管理端**：`http://localhost:3000/admin/login`
  - 用户名：`admin`
  - 密码：`admin123`

---

## ⚙️ 配置说明

### 🔑 必需配置
| 服务 | 配置项 | 说明 | 获取方式 |
|------|--------|------|----------|
| **七牛云 Kodo** | `qiniu.accessKey` | 七牛云 Access Key | 七牛云控制台 |
| | `qiniu.secretKey` | 七牛云 Secret Key | 七牛云控制台 |
| | `qiniu.bucket` | 存储空间名称 | 七牛云控制台创建 |
| | `qiniu.domain` | 绑定域名 | 七牛云域名管理 |
| **阿里云 DashScope** | `DASHSCOPE_API_KEY` | AI 模型 API 密钥 | 阿里云 DashScope 控制台 |
| **Milvus** | `MILVUS_HOST` | Milvus 服务地址 | 本地部署或云服务 |
| | `MILVUS_PORT` | Milvus 服务端口 | 默认 19530 |

### 🔧 可选配置
| 服务 | 配置项 | 说明 | 默认值 |
|------|--------|------|--------|
| **阿里云短信** | `aliyun.sms.*` | 短信验证码服务 | 模拟模式 |
| **Redis** | `spring.redis.*` | Redis 连接配置 | localhost:6379 |
| **MySQL** | `spring.datasource.*` | 数据库连接配置 | localhost:3306 |
| **缓存策略** | `cache.*` | 缓存 TTL 和大小 | 见 CacheConfig |

### 🚨 安全注意事项
1. **密钥管理**：所有敏感密钥（七牛云 AK/SK、API 密钥）必须通过环境变量配置
2. **生产环境**：禁用模拟模式，启用真实的短信和存储服务
3. **访问控制**：配置防火墙规则，限制数据库和缓存服务的访问
4. **日志脱敏**：确保日志中不输出敏感信息

---

## 🚢 部署架构

### 🐳 Docker 容器化部署（推荐）
```yaml
# docker-compose.yml 示例
version: '3.8'

services:
  # MySQL 数据库
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: ai_knowledge_db
    volumes:
      - mysql_data:/var/lib/mysql
    ports:
      - "3306:3306"

  # Redis 缓存
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  # Milvus 向量数据库
  milvus:
    image: milvusdb/milvus:v2.4.0
    environment:
      ETCD_ENDPOINTS: etcd:2379
      MINIO_ADDRESS: minio:9000
    ports:
      - "19530:19530"
      - "9091:9091"

  # Java 后端服务
  backend:
    build: .
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: mysql
      REDIS_HOST: redis
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
      - milvus

  # Python AI 服务
  ai-service:
    build: ./python-service
    environment:
      MILVUS_HOST: milvus
      MILVUS_PORT: 19530
    ports:
      - "8000:8000"
    depends_on:
      - milvus

  # 前端服务
  frontend:
    build: ./frontend
    ports:
      - "3000:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

### ☁️ 云原生部署建议
1. **Kubernetes 部署**：使用 Helm Charts 管理各服务
2. **服务网格**：Istio 实现流量管理和监控
3. **CI/CD**：GitHub Actions 或 GitLab CI 自动化部署
4. **监控告警**：Prometheus + Grafana + AlertManager
5. **日志收集**：ELK Stack 或 Loki + Grafana

### 📊 性能指标
| 指标 | 目标值 | 监控方式 |
|------|--------|----------|
| **API 响应时间** | < 200ms (P95) | Prometheus + Grafana |
| **缓存命中率** | > 95% | Redis 监控 |
| **向量检索延迟** | < 100ms | Milvus 监控 |
| **系统可用性** | 99.9% | 健康检查端点 |
| **并发用户数** | 1000+ | 压力测试 |

## 👥 默认账号
- **用户端**：`http://localhost:3000` - 手机验证码注册/登录
- **管理端**：`http://localhost:3000/admin/login`
  - 用户名：`admin`
  - 密码：`admin123`

---

## 已知限制和注意事项

### ✅ 已解决的问题
1. **向量数据库限制**：已从FAISS迁移到Milvus，支持高效的文档删除和过滤
2. **缓存性能**：已实现多级缓存系统，大幅提升响应速度
3. **对话上下文**：已实现完整的上下文记忆系统

### ⚠️ 当前注意事项
1. **AI API密钥**：需要配置 `DASHSCOPE_API_KEY` 环境变量才能使用AI问答功能
2. **流式输出测试**：SSE流式输出框架已实现，但需要AI API密钥进行实际测试
3. **Milvus连接**：确保Milvus服务正常运行，连接地址正确配置在 `.env` 文件中
4. **缓存一致性**：多级缓存系统需要确保数据一致性，系统已实现缓存穿透/雪崩防护

### 🔧 生产环境建议
1. **监控系统**：建议添加应用性能监控（APM）和业务日志收集
2. **API限流**：生产环境建议实现接口限流防止滥用
3. **密钥管理**：生产环境使用环境变量或密钥管理服务存储敏感信息
4. **备份策略**：定期备份Milvus向量数据和MySQL数据库

---

## 更新日志 (Changelog)

### v1.0.0 (2026-04-03) - 生产级功能完善
**重大更新**：
- ✅ **向量数据库迁移**：从FAISS成功迁移到Milvus生产级向量数据库
- ✅ **多级缓存系统**：实现Redis + Caffeine两级缓存架构，性能大幅提升
- ✅ **对话上下文记忆**：完整的上下文记忆系统，支持智能对话管理
- ✅ **SSE流式输出框架**：完整的流式响应框架，支持实时AI输出

**技术架构升级**：
- 支持Milvus高效向量检索和文档删除
- 实现缓存穿透/雪崩防护机制
- 优化异步处理和线程池管理
- 完善安全认证和权限控制

**性能优化**：
- 毫秒级本地缓存响应
- 分布式缓存支持高并发
- 向量检索性能提升
- 内存使用优化

**下一步计划**：
- 完善SSE流式输出的实际测试
- 添加API文档和测试用例
- 部署优化和容器化
- 性能监控系统集成
