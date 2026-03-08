# AI Knowledge System - Python Service

这是一个基于 FastAPI 和 LangChain 的 AI 服务，负责文档解析、向量存储和问答。

## 目录结构

*   `main.py`: 服务入口
*   `api/`: API 接口定义
*   `core/`: 核心逻辑 (解析、向量库、LLM)
*   `requirements.txt`: 依赖列表

## 环境准备

1.  确保已安装 Python 3.10+。
2.  推荐使用虚拟环境。

## 在 PyCharm 中运行

1.  **打开项目**:
    *   启动 PyCharm。
    *   选择 "Open"，然后选择 `ai-knowledge-system/python-service` 文件夹（**注意是选中 python-service 文件夹，而不是整个项目根目录，这样 PyCharm 会将其识别为 Python 项目**）。

2.  **配置解释器**:
    *   PyCharm 通常会自动检测并提示创建虚拟环境。
    *   如果没有，请进入 `Settings` -> `Project: python-service` -> `Python Interpreter`。
    *   点击 "Add Interpreter"，选择 "New" (Virtualenv)。
    *   点击 "OK" 创建。

3.  **安装依赖**:
    *   打开 `requirements.txt` 文件。
    *   PyCharm 通常会提示 "Install requirements"。点击安装即可。
    *   或者在 PyCharm 的 Terminal 中运行: `pip install -r requirements.txt`

4.  **配置环境变量**:
    *   在 `python-service` 目录下创建一个名为 `.env` 的文件。
    *   复制 `.env.example` 的内容到 `.env`。
    *   填入您的阿里云 DashScope API Key: `DASHSCOPE_API_KEY=sk-xxxxxxxx`

5.  **运行服务**:
    *   右键点击 `main.py` 文件。
    *   选择 **"Run 'main'"**。
    *   控制台应显示 `Uvicorn running on http://0.0.0.0:8000`。

## 命令行运行

```bash
# 进入目录
cd python-service

# 创建虚拟环境
python -m venv venv
# 激活虚拟环境 (Windows)
.\venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 运行
python main.py
```

## API 文档

服务启动后，访问: `http://localhost:8000/docs` 查看 Swagger UI。
