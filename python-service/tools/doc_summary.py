from typing import Dict, Any
from tools.base import Tool, ToolSchema, SchemaProperty, ToolMetadata
from core.llm import LLMService
from core.config import config


class DocSummaryTool(Tool):
    """文档摘要工具"""
    
    def __init__(self):
        input_schema = ToolSchema(
            properties={
                "content": SchemaProperty(
                    type="string",
                    description="文档内容",
                    required=True
                ),
                "max_length": SchemaProperty(
                    type="number",
                    description="摘要最大长度",
                    required=False,
                    default=200
                ),
                "focus": SchemaProperty(
                    type="string",
                    description="摘要重点（可选）",
                    required=False
                )
            },
            type="object"
        )
        
        output_schema = ToolSchema(
            properties={
                "summary": SchemaProperty(
                    type="string",
                    description="文档摘要",
                    required=True
                ),
                "original_length": SchemaProperty(
                    type="number",
                    description="原始文档长度",
                    required=True
                ),
                "summary_length": SchemaProperty(
                    type="number",
                    description="摘要长度",
                    required=True
                )
            },
            type="object"
        )
        
        metadata = ToolMetadata(
            timeout_ms=20000,
            max_retries=2,
            permission="user",
            description="生成文档摘要"
        )
        
        super().__init__(
            name="doc_summary",
            description="生成文档摘要",
            input_schema=input_schema,
            output_schema=output_schema,
            metadata=metadata
        )
        
        # 初始化 LLM 服务
        self.llm_service = LLMService()
    
    def execute(self, parameters: Dict[str, Any]) -> Dict[str, Any]:
        """执行文档摘要生成"""
        content = parameters.get("content")
        max_length = int(parameters.get("max_length", 200))
        focus = parameters.get("focus", "")
        
        original_length = len(content)
        
        config.logger.info(f"Generating summary for document of length {original_length}, max summary length: {max_length}")
        
        # 构建摘要提示
        summary_prompt = f"""
        请为以下文档生成一个简洁的摘要：
        
        {content}
        
        要求：
        1. 摘要长度不超过 {max_length} 字
        2. 包含文档的核心内容和主要观点
        3. 语言简洁明了
        4. {'重点关注：' + focus if focus else ''}
        5. 直接返回摘要，不要添加任何前缀或解释
        """
        
        try:
            # 使用 LLM 生成摘要
            summary = self.llm_service.get_answer(
                question=summary_prompt,
                context_docs=[],
                conversation_context=""
            )
            
            # 清理摘要
            summary = summary.strip()
            
            # 确保摘要长度不超过限制
            if len(summary) > max_length:
                summary = summary[:max_length] + "..."
            
            summary_length = len(summary)
            config.logger.info(f"Summary generated: {summary_length} characters")
            
            return {
                "summary": summary,
                "original_length": original_length,
                "summary_length": summary_length
            }
            
        except Exception as e:
            config.logger.error(f"Document summary failed: {e}")
            return {
                "summary": f"生成摘要失败: {str(e)}",
                "original_length": original_length,
                "summary_length": 0
            }
