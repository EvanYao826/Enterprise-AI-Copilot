from typing import Dict, Any
from tools.base import Tool, ToolSchema, SchemaProperty, ToolMetadata
from core.config import config
import uuid
import datetime


class ConversationMemoryWriteTool(Tool):
    """对话记忆写入工具"""
    
    def __init__(self):
        input_schema = ToolSchema(
            properties={
                "conversation_id": SchemaProperty(
                    type="string",
                    description="对话ID",
                    required=True
                ),
                "role": SchemaProperty(
                    type="string",
                    description="角色 (user 或 assistant)",
                    required=True
                ),
                "content": SchemaProperty(
                    type="string",
                    description="消息内容",
                    required=True
                ),
                "timestamp": SchemaProperty(
                    type="string",
                    description="时间戳 (ISO格式，可选)",
                    required=False
                )
            },
            type="object"
        )
        
        output_schema = ToolSchema(
            properties={
                "success": SchemaProperty(
                    type="boolean",
                    description="是否成功",
                    required=True
                ),
                "message_id": SchemaProperty(
                    type="string",
                    description="消息ID",
                    required=True
                ),
                "conversation_id": SchemaProperty(
                    type="string",
                    description="对话ID",
                    required=True
                )
            },
            type="object"
        )
        
        metadata = ToolMetadata(
            timeout_ms=5000,
            max_retries=1,
            permission="user",
            description="写入对话记忆"
        )
        
        super().__init__(
            name="conversation_memory_write",
            description="写入对话记忆",
            input_schema=input_schema,
            output_schema=output_schema,
            metadata=metadata
        )
    
    def execute(self, parameters: Dict[str, Any]) -> Dict[str, Any]:
        """执行对话记忆写入"""
        conversation_id = parameters.get("conversation_id")
        role = parameters.get("role")
        content = parameters.get("content")
        timestamp = parameters.get("timestamp", datetime.datetime.utcnow().isoformat() + "Z")
        
        # 验证角色
        if role not in ["user", "assistant"]:
            raise ValueError(f"Invalid role: {role}, must be 'user' or 'assistant'")
        
        # 生成消息ID
        message_id = str(uuid.uuid4())
        
        config.logger.info(f"Writing message to conversation {conversation_id}: role={role}, content={content[:50]}...")
        
        # 这里是一个示例实现，实际应该写入数据库或缓存
        # 后续需要接入真实的对话存储
        
        # 模拟写入操作
        # 在实际实现中，这里应该调用数据库接口保存消息
        
        return {
            "success": True,
            "message_id": message_id,
            "conversation_id": conversation_id
        }
