from typing import Dict, Any, List
from tools.base import Tool, ToolSchema, SchemaProperty, ToolMetadata
from core.config import config


class ConversationMemoryReadTool(Tool):
    """对话记忆读取工具"""
    
    def __init__(self):
        input_schema = ToolSchema(
            properties={
                "conversation_id": SchemaProperty(
                    type="string",
                    description="对话ID",
                    required=True
                ),
                "limit": SchemaProperty(
                    type="number",
                    description="返回消息数量限制",
                    required=False,
                    default=10
                ),
                "offset": SchemaProperty(
                    type="number",
                    description="消息偏移量",
                    required=False,
                    default=0
                )
            },
            type="object"
        )
        
        output_schema = ToolSchema(
            properties={
                "messages": SchemaProperty(
                    type="array",
                    description="对话消息列表",
                    required=True
                ),
                "conversation_id": SchemaProperty(
                    type="string",
                    description="对话ID",
                    required=True
                ),
                "total_count": SchemaProperty(
                    type="number",
                    description="总消息数量",
                    required=True
                )
            },
            type="object"
        )
        
        metadata = ToolMetadata(
            timeout_ms=5000,
            max_retries=1,
            permission="user",
            description="读取对话记忆"
        )
        
        super().__init__(
            name="conversation_memory_read",
            description="读取对话记忆",
            input_schema=input_schema,
            output_schema=output_schema,
            metadata=metadata
        )
    
    def execute(self, parameters: Dict[str, Any]) -> Dict[str, Any]:
        """执行对话记忆读取"""
        conversation_id = parameters.get("conversation_id")
        limit = int(parameters.get("limit", 10))
        offset = int(parameters.get("offset", 0))
        
        config.logger.info(f"Reading conversation memory for ID: {conversation_id}, limit: {limit}, offset: {offset}")
        
        # 这里是一个示例实现，实际应该从数据库或缓存中读取
        # 后续需要接入真实的对话存储
        
        # 模拟对话消息
        mock_messages = [
            {
                "id": "1",
                "role": "user",
                "content": "你好，我想了解AI知识系统",
                "timestamp": "2026-04-27T10:00:00Z"
            },
            {
                "id": "2",
                "role": "assistant",
                "content": "您好！AI知识系统是一个基于知识库的智能问答系统，能够帮助您快速获取相关信息。",
                "timestamp": "2026-04-27T10:00:05Z"
            },
            {
                "id": "3",
                "role": "user",
                "content": "它有哪些功能？",
                "timestamp": "2026-04-27T10:01:00Z"
            },
            {
                "id": "4",
                "role": "assistant",
                "content": "AI知识系统具有文档解析、向量检索、智能问答、对话记忆等功能。",
                "timestamp": "2026-04-27T10:01:05Z"
            }
        ]
        
        # 模拟分页
        total_count = len(mock_messages)
        end_index = min(offset + limit, total_count)
        paginated_messages = mock_messages[offset:end_index]
        
        return {
            "messages": paginated_messages,
            "conversation_id": conversation_id,
            "total_count": total_count
        }
