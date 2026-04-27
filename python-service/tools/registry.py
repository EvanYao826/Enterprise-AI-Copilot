from typing import Dict, Any, Optional
import time
import threading
from tools.base import Tool
from tools.execution import tool_execution_tracker
from core.config import config


class ToolRegistry:
    """工具注册器"""
    
    _instance = None
    _lock = threading.Lock()
    
    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._tools = {}
        return cls._instance
    
    def register_tool(self, tool: Tool):
        """注册工具"""
        self._tools[tool.name] = tool
        config.logger.info(f"Tool registered: {tool.name}")
    
    def get_tool(self, name: str) -> Optional[Tool]:
        """获取工具"""
        return self._tools.get(name)
    
    def get_all_tools(self) -> Dict[str, Tool]:
        """获取所有工具"""
        return self._tools
    
    def has_tool(self, name: str) -> bool:
        """检查工具是否存在"""
        return name in self._tools
    
    def invoke_tool(self, tool_name: str, parameters: Dict[str, Any], run_id: Optional[str] = None) -> Dict[str, Any]:
        """调用工具"""
        tool = self.get_tool(tool_name)
        if not tool:
            raise ValueError(f"Tool not found: {tool_name}")
        
        # 验证输入参数
        if not tool.validate_input(parameters):
            raise ValueError(f"Invalid input parameters for tool: {tool_name}")
        
        # 生成 run_id 如果没有提供
        if run_id is None:
            run_id = str(time.time())
        
        # 开始工具调用跟踪
        tool_call_id = tool_execution_tracker.start_tool_call(run_id, tool_name, parameters)
        
        # 执行工具（带超时和重试）
        retries = 0
        max_retries = tool.metadata.max_retries
        timeout_ms = tool.metadata.timeout_ms
        
        while retries <= max_retries:
            try:
                start_time = time.time()
                
                # 执行工具
                result = tool.execute(parameters)
                
                execution_time = (time.time() - start_time) * 1000
                config.logger.info(f"Tool {tool_name} executed in {execution_time:.2f}ms")
                
                # 完成工具调用跟踪
                tool_execution_tracker.complete_tool_call(tool_call_id, result, execution_time)
                
                return result
                
            except Exception as e:
                execution_time = (time.time() - start_time) * 1000 if 'start_time' in locals() else 0
                retries += 1
                if retries > max_retries:
                    config.logger.error(f"Tool {tool_name} failed after {max_retries} retries: {e}")
                    # 失败工具调用跟踪
                    tool_execution_tracker.fail_tool_call(tool_call_id, str(e), execution_time)
                    raise
                config.logger.warning(f"Tool {tool_name} failed (attempt {retries}/{max_retries}): {e}")
                time.sleep(0.5)  # 重试间隔
    
    def get_tool_info(self, tool_name: str) -> Dict[str, Any]:
        """获取工具信息"""
        tool = self.get_tool(tool_name)
        if not tool:
            return None
        
        return {
            "name": tool.name,
            "description": tool.description,
            "input_schema": {
                "type": tool.input_schema.type,
                "properties": {k: {
                    "type": v.type,
                    "description": v.description,
                    "required": v.required
                } for k, v in tool.input_schema.properties.items()}
            },
            "output_schema": {
                "type": tool.output_schema.type,
                "properties": {k: {
                    "type": v.type,
                    "description": v.description,
                    "required": v.required
                } for k, v in tool.output_schema.properties.items()}
            },
            "metadata": {
                "timeout_ms": tool.metadata.timeout_ms,
                "max_retries": tool.metadata.max_retries,
                "permission": tool.metadata.permission
            }
        }
    
    def get_tool_calls_by_run_id(self, run_id: str) -> list:
        """按 runId 查询工具调用轨迹"""
        tool_calls = tool_execution_tracker.get_tool_calls_by_run_id(run_id)
        return [{
            "tool_call_id": call.tool_call_id,
            "tool_name": call.tool_name,
            "input_params": call.input_params,
            "output": call.output,
            "status": call.status,
            "duration_ms": call.duration_ms,
            "error_message": call.error_message,
            "timestamp": call.timestamp
        } for call in tool_calls]


# 全局工具注册器实例
tool_registry = ToolRegistry()
