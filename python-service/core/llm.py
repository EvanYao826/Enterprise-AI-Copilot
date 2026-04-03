import os
import json
from typing import AsyncGenerator, Generator
from langchain_community.llms import Tongyi
from langchain_core.prompts import PromptTemplate
from langchain_core.output_parsers import StrOutputParser

class LLMService:
    def __init__(self):
        # 默认使用阿里云通义千问 (需要设置 DASHSCOPE_API_KEY 环境变量)
        api_key = os.getenv("DASHSCOPE_API_KEY")
        
        if not api_key:
            print("Warning: DASHSCOPE_API_KEY not found. LLM features will not work properly.")
            self.llm = None
        else:
            # 使用 qwen-plus 模型，效果比 turbo 好，适合知识库问答
            # 如果需要更强的推理能力，可以使用 qwen-max
            # 启用流式输出
            self.llm = Tongyi(
                model_name="qwen-plus",
                api_key=api_key,
                streaming=True  # 启用流式输出
            )

        # 优化后的 Prompt 模板
        # 支持对话上下文和知识库上下文
        self.prompt = PromptTemplate.from_template(
            """
            你是一个专业的AI知识库助手。请根据以下信息直接回答问题，不要展示思考过程。

            对话历史：
            {conversation_context}

            相关知识库：
            {knowledge_context}

            用户当前问题：
            {question}

            请直接给出简洁、准确的回答：
            """
        )

        # 标题生成模板
        self.summary_prompt = PromptTemplate.from_template(
            """
            请为以下用户问题生成一个简短的标题（Summary）。
            
            用户问题：
            {question}
            
            要求：
            1. 标题应概括问题的主要内容。
            2. 长度控制在10个字以内。
            3. 不需要任何前缀或后缀，直接返回标题文本。
            
            标题：
            """
        )

    """
     * 获取 LLM 的回答
     * @param question 用户问题
     * @param context_docs 上下文文档列表
     * @param conversation_context 对话上下文（可选）
     * @return LLM 的回答
     * """
    def get_answer(self, question: str, context_docs: list, conversation_context: str = "") -> str:
        if not self.llm:
            # 当没有API密钥时，返回一个友好的默认响应
            return "我是AI知识库助手，很高兴为您服务。由于系统未配置API密钥，我暂时无法提供详细回答。请联系管理员配置DASHSCOPE_API_KEY环境变量以启用完整功能。"

        # 处理知识库上下文
        if not context_docs:
            knowledge_context = "（无相关知识库信息）"
        else:
            knowledge_context = "\n\n".join([doc.page_content for doc in context_docs])

        # 处理对话上下文
        if not conversation_context or conversation_context.strip() == "":
            conversation_context = "（无对话历史）"

        # 构建处理链
        chain = (
            self.prompt
            | self.llm
            | StrOutputParser()
        )

        try:
            return chain.invoke({
                "conversation_context": conversation_context,
                "knowledge_context": knowledge_context,
                "question": question
            })
        except Exception as e:
            print(f"LLM Error: {e}")
            return "抱歉，AI服务暂时不可用，请稍后再试。"

    """
     * 流式获取 LLM 的回答
     * @param question 用户问题
     * @param context_docs 上下文文档列表
     * @param conversation_context 对话上下文（可选）
     * @return 流式生成器，逐个token返回
     * """
    def get_answer_stream(self, question: str, context_docs: list, conversation_context: str = "") -> Generator[str, None, None]:
        if not self.llm:
            # 当没有API密钥时，返回错误信息
            yield json.dumps({"type": "error", "content": "未配置API密钥"})
            return

        # 处理知识库上下文
        if not context_docs:
            knowledge_context = "（无相关知识库信息）"
        else:
            knowledge_context = "\n\n".join([doc.page_content for doc in context_docs])

        # 处理对话上下文
        if not conversation_context or conversation_context.strip() == "":
            conversation_context = "（无对话历史）"

        # 构建处理链
        chain = (
            self.prompt
            | self.llm
            | StrOutputParser()
        )

        try:
            # 发送开始信号
            yield json.dumps({"type": "start", "content": ""})

            # 流式调用
            full_response = ""
            for chunk in chain.stream({
                "conversation_context": conversation_context,
                "knowledge_context": knowledge_context,
                "question": question
            }):
                full_response += chunk
                yield json.dumps({"type": "token", "content": chunk})

            # 发送结束信号
            yield json.dumps({"type": "end", "content": full_response})

        except Exception as e:
            print(f"LLM Stream Error: {e}")
            yield json.dumps({"type": "error", "content": "AI服务暂时不可用"})

    def generate_title(self, question: str) -> str:
        if not self.llm:
            return "New Chat"

        chain = (
            self.summary_prompt
            | self.llm
            | StrOutputParser()
        )
        
        try:
            title = chain.invoke({"question": question})
            # 清理可能的额外空白或引号
            return title.strip().strip('"').strip("'")
        except Exception as e:
            print(f"LLM Title Generation Error: {e}")
            return "New Chat"
