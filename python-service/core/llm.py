import os
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
            self.llm = Tongyi(model_name="qwen-plus", api_key=api_key)

        # 优化后的 Prompt 模板
        # 允许在没有上下文的情况下进行通用回答
        self.prompt = PromptTemplate.from_template(
            """
            你是一个专业的AI知识库助手。请回答用户的问题。
            
            如果提供了以下上下文信息，请优先基于上下文回答。
            如果上下文信息不足或与问题无关，请利用你的通用知识库进行回答，但请尽量简洁准确。
            
            上下文信息：
            {context}
            
            用户问题：
            {question}
            
            回答要求：
            1. 如果上下文包含答案，请优先使用上下文信息，并用通俗易懂的语言回答。
            2. 如果上下文不相关或为空，请忽略上下文，直接回答用户问题。
            3. 回答时保持客观、中立。
            
            回答：
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

    def get_answer(self, question: str, context_docs: list) -> str:
        if not self.llm:
            return "Error: LLM API Key is missing. Please configure DASHSCOPE_API_KEY environment variable."

        # 即使没有检索到文档，也尝试回答 (通用问答)
        if not context_docs:
            context_text = "（无相关上下文信息）"
        else:
            context_text = "\n\n".join([doc.page_content for doc in context_docs])
        
        # 构建处理链
        chain = (
            self.prompt
            | self.llm
            | StrOutputParser()
        )
        
        try:
            return chain.invoke({
                "context": context_text,
                "question": question
            })
        except Exception as e:
            print(f"LLM Error: {e}")
            return f"抱歉，AI服务暂时不可用。错误信息: {str(e)}"

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
