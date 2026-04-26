---
name: rag-debug
description: 排查当前 RAG 链路中的文档解析、切分、向量检索、重排序、提示词拼接和答案引用问题。
allowed-tools: Read Grep Glob Bash(python *) Bash(pip *) Bash(git diff *) Bash(git status)
---

排查当前项目 RAG / AI 问答链路的问题，重点检查：

1. 文档解析是否成功落库、落向量库
2. text splitter 参数是否合理，是否导致切分过碎或过大
3. reranker 是否正确接入，是否错误过滤高价值片段
4. vector store 检索阈值、k 值、元数据过滤是否合理
5. prompt 拼接是否丢上下文，是否把无关内容传给模型
6. sources 引用是否和最终回答一致
7. fallback 逻辑是否把失败状态误标为成功
8. Windows 路径、模型配置、OCR 路径等硬编码问题

输出要求：
- 给出从“最可能根因”到“次要问题”的排序
- 每条结论都带 file_path:line_number
- 能复现的给出最短复现路径
- 不要泛泛而谈，只说与当前仓库相关的问题
