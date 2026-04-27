from typing import Dict, Any
from tools.base import Tool, ToolSchema, SchemaProperty, ToolMetadata
from core.llm import LLMService
from core.config import config


class QuestionRewriteTool(Tool):
    """问题重写工具"""
    
    def __init__(self):
        input_schema = ToolSchema(
            properties={
                "question": SchemaProperty(
                    type="string",
                    description="原始问题",
                    required=True
                ),
                "conversation_context": SchemaProperty(
                    type="string",
                    description="对话上下文（可选）",
                    required=False,
                    default=""
                )
            },
            type="object"
        )
        
        output_schema = ToolSchema(
            properties={
                "rewritten_question": SchemaProperty(
                    type="string",
                    description="重写后的问题",
                    required=True
                ),
                "original_question": SchemaProperty(
                    type="string",
                    description="原始问题",
                    required=True
                )
            },
            type="object"
        )
        
        metadata = ToolMetadata(
            timeout_ms=15000,
            max_retries=2,
            permission="user",
            description="重写用户问题以提高检索效果"
        )
        
        super().__init__(
            name="question_rewrite",
            description="重写用户问题以提高检索效果",
            input_schema=input_schema,
            output_schema=output_schema,
            metadata=metadata
        )
        
        # 初始化 LLM 服务
        self.llm_service = LLMService()
    
    def execute(self, parameters: Dict[str, Any]) -> Dict[str, Any]:
        """执行问题重写"""
        question = parameters.get("question")
        conversation_context = parameters.get("conversation_context", "")
        
        # 构建重写提示
        rewrite_prompt = f"""
        你是一个问题重写专家。请将用户的原始问题重写为更适合知识库检索的形式。
        
        原始问题：{question}
        
        对话上下文：
        {conversation_context if conversation_context else "无"}
        
        要求：
        1. 保持问题的核心意图不变
        2. 使用更正式、明确的语言
        3. 补充可能缺失的关键信息
        4. 优化关键词，使其更适合向量检索
        5. 直接返回重写后的问题，不要添加任何前缀或解释
        """
        
        # 使用 LLM 重写问题
        try:
            # 使用 LLM 服务生成重写后的问题
            # 这里使用 get_answer 方法，因为我们需要的是 LLM 的生成能力
            rewritten_question = self.llm_service.get_answer(
                question=rewrite_prompt,
                context_docs=[],  # 不需要上下文文档
                conversation_context=""
            )
            
            # 清理结果
            rewritten_question = rewritten_question.strip()
            
            config.logger.info(f"Question rewritten: '{question}' -> '{rewritten_question}'")
            
            return {
                "rewritten_question": rewritten_question,
                "original_question": question
            }
            
        except Exception as e:
            config.logger.error(f"Question rewrite failed: {e}")
            # 如果重写失败，返回原始问题
            return {
                "rewritten_question": question,
                "original_question": question
            }
