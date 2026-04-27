from typing import Dict, Any, List, Optional
from dataclasses import dataclass
import time
import json
import uuid
from core.config import config


@dataclass
class ToolCallRecord:
    """工具调用记录"""
    tool_call_id: str
    run_id: str
    tool_name: str
    input_params: Dict[str, Any]
    output: Optional[Dict[str, Any]]
    status: str  # success, failed, pending
    duration_ms: float
    error_message: Optional[str]
    timestamp: str


class ToolExecutionTracker:
    """工具执行跟踪器"""
    
    def __init__(self):
        self._tool_calls = {}
        self._run_tool_calls = {}
    
    def start_tool_call(self, run_id: str, tool_name: str, input_params: Dict[str, Any]) -> str:
        """开始工具调用"""
        tool_call_id = str(uuid.uuid4())
        timestamp = time.strftime('%Y-%m-%d %H:%M:%S')
        
        record = ToolCallRecord(
            tool_call_id=tool_call_id,
            run_id=run_id,
            tool_name=tool_name,
            input_params=input_params,
            output=None,
            status="pending",
            duration_ms=0,
            error_message=None,
            timestamp=timestamp
        )
        
        self._tool_calls[tool_call_id] = record
        
        # 按 run_id 分组
        if run_id not in self._run_tool_calls:
            self._run_tool_calls[run_id] = []
        self._run_tool_calls[run_id].append(tool_call_id)
        
        config.logger.info(f"Tool call started: {tool_name}, run_id: {run_id}, tool_call_id: {tool_call_id}")
        
        return tool_call_id
    
    def complete_tool_call(self, tool_call_id: str, output: Dict[str, Any], duration_ms: float):
        """完成工具调用"""
        if tool_call_id in self._tool_calls:
            record = self._tool_calls[tool_call_id]
            record.output = output
            record.status = "success"
            record.duration_ms = duration_ms
            
            config.logger.info(f"Tool call completed: {record.tool_name}, duration: {duration_ms:.2f}ms")
    
    def fail_tool_call(self, tool_call_id: str, error_message: str, duration_ms: float):
        """工具调用失败"""
        if tool_call_id in self._tool_calls:
            record = self._tool_calls[tool_call_id]
            record.status = "failed"
            record.error_message = error_message
            record.duration_ms = duration_ms
            
            config.logger.error(f"Tool call failed: {record.tool_name}, error: {error_message}")
    
    def get_tool_call(self, tool_call_id: str) -> Optional[ToolCallRecord]:
        """获取工具调用记录"""
        return self._tool_calls.get(tool_call_id)
    
    def get_tool_calls_by_run_id(self, run_id: str) -> List[ToolCallRecord]:
        """按 run_id 获取工具调用记录"""
        tool_call_ids = self._run_tool_calls.get(run_id, [])
        return [self._tool_calls[call_id] for call_id in tool_call_ids if call_id in self._tool_calls]
    
    def get_all_tool_calls(self) -> List[ToolCallRecord]:
        """获取所有工具调用记录"""
        return list(self._tool_calls.values())
    
    def clear_run_calls(self, run_id: str):
        """清理指定 run_id 的工具调用记录"""
        if run_id in self._run_tool_calls:
            tool_call_ids = self._run_tool_calls[run_id]
            for call_id in tool_call_ids:
                if call_id in self._tool_calls:
                    del self._tool_calls[call_id]
            del self._run_tool_calls[run_id]
            config.logger.info(f"Cleared tool calls for run_id: {run_id}")


# 全局工具执行跟踪器
tool_execution_tracker = ToolExecutionTracker()
