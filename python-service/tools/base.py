from abc import ABC, abstractmethod
from typing import Dict, Any, Optional
from dataclasses import dataclass, field


@dataclass
class ToolMetadata:
    """工具元数据"""
    timeout_ms: int = 30000  # 默认30秒
    max_retries: int = 3     # 默认3次重试
    permission: str = "user"  # user 或 admin
    description: str = ""


@dataclass
class SchemaProperty:
    """Schema 属性"""
    type: str  # string, number, boolean, object, array
    description: str
    required: bool = True
    default: Any = None


@dataclass
class ToolSchema:
    """工具输入/输出 schema"""
    properties: Dict[str, SchemaProperty]
    type: str = "object"
    required: list = field(default_factory=list)


class Tool(ABC):
    """工具基类"""
    
    def __init__(self, name: str, description: str, input_schema: ToolSchema, 
                 output_schema: ToolSchema, metadata: Optional[ToolMetadata] = None):
        self.name = name
        self.description = description
        self.input_schema = input_schema
        self.output_schema = output_schema
        self.metadata = metadata or ToolMetadata()
    
    @abstractmethod
    def execute(self, parameters: Dict[str, Any]) -> Dict[str, Any]:
        """执行工具逻辑"""
        pass
    
    def validate_input(self, parameters: Dict[str, Any]) -> bool:
        """验证输入参数"""
        for name, property in self.input_schema.properties.items():
            if property.required and name not in parameters:
                return False
            if name in parameters:
                # 简单类型检查
                if property.type == "number" and not isinstance(parameters[name], (int, float)):
                    return False
                if property.type == "string" and not isinstance(parameters[name], str):
                    return False
                if property.type == "boolean" and not isinstance(parameters[name], bool):
                    return False
        return True
