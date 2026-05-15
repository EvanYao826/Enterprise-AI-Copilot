"""pytest 配置文件"""

import pytest
import sys
import os

# 添加项目根目录到 Python 路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


@pytest.fixture
def sample_state():
    """创建示例 AgentState"""
    from agent.state import AgentState
    return AgentState(
        run_id="test-run-001",
        trace_id="test-trace-001",
        conversation_id="conv-001",
        user_id="user-001",
        original_input="什么是 RAG？"
    )


@pytest.fixture
def tool_registry():
    """创建干净的 ToolRegistry 实例"""
    from tools.registry import ToolRegistry
    # 每次测试创建新实例，避免污染
    registry = ToolRegistry()
    registry._tools.clear()
    return registry


@pytest.fixture
def mock_tool():
    """创建模拟工具"""
    from tools.base import Tool, ToolSchema, SchemaProperty, ToolMetadata

    class MockTool(Tool):
        def __init__(self):
            input_schema = ToolSchema(
                properties={
                    "query": SchemaProperty(type="string", description="查询内容", required=True)
                }
            )
            output_schema = ToolSchema(
                properties={
                    "result": SchemaProperty(type="string", description="结果", required=True)
                }
            )
            metadata = ToolMetadata(timeout_ms=5000, max_retries=2)
            super().__init__("mock_tool", "模拟工具", input_schema, output_schema, metadata)

        def execute(self, parameters):
            return {"result": f"处理: {parameters.get('query', '')}"}

    return MockTool()
