"""测试 ToolRegistry 工具注册表"""

import pytest
from tools.registry import ToolRegistry
from tools.base import Tool, ToolSchema, SchemaProperty, ToolMetadata


class TestToolRegistry:
    """测试工具注册表"""

    def test_register_tool(self, tool_registry, mock_tool):
        """测试注册工具"""
        tool_registry.register_tool(mock_tool)
        assert tool_registry.has_tool("mock_tool")
        assert tool_registry.get_tool("mock_tool") is not None

    def test_get_nonexistent_tool(self, tool_registry):
        """测试获取不存在的工具"""
        assert tool_registry.get_tool("nonexistent") is None
        assert tool_registry.has_tool("nonexistent") is False

    def test_get_all_tools(self, tool_registry, mock_tool):
        """测试获取所有工具"""
        tool_registry.register_tool(mock_tool)
        tools = tool_registry.get_all_tools()
        assert len(tools) == 1
        assert "mock_tool" in tools

    def test_invoke_tool(self, tool_registry, mock_tool):
        """测试调用工具"""
        tool_registry.register_tool(mock_tool)
        result = tool_registry.invoke_tool("mock_tool", {"query": "test"})
        assert result["result"] == "处理: test"

    def test_invoke_nonexistent_tool(self, tool_registry):
        """测试调用不存在的工具"""
        with pytest.raises(ValueError, match="Tool not found"):
            tool_registry.invoke_tool("nonexistent", {})

    def test_invoke_with_invalid_params(self, tool_registry, mock_tool):
        """测试调用工具时参数无效"""
        tool_registry.register_tool(mock_tool)
        # 缺少必需的 query 参数
        with pytest.raises(ValueError, match="Invalid input"):
            tool_registry.invoke_tool("mock_tool", {})

    def test_get_tool_info(self, tool_registry, mock_tool):
        """测试获取工具信息"""
        tool_registry.register_tool(mock_tool)
        info = tool_registry.get_tool_info("mock_tool")
        assert info["name"] == "mock_tool"
        assert info["description"] == "模拟工具"
        assert "input_schema" in info
        assert "output_schema" in info
        assert "metadata" in info
        assert info["metadata"]["timeout_ms"] == 5000

    def test_singleton_pattern(self):
        """测试单例模式"""
        registry1 = ToolRegistry()
        registry2 = ToolRegistry()
        assert registry1 is registry2


class TestToolBase:
    """测试 Tool 基类"""

    def test_tool_creation(self):
        """测试创建工具"""
        input_schema = ToolSchema(
            properties={
                "param1": SchemaProperty(type="string", description="参数1", required=True)
            }
        )
        output_schema = ToolSchema(
            properties={
                "result": SchemaProperty(type="string", description="结果")
            }
        )
        metadata = ToolMetadata(timeout_ms=10000, max_retries=5, permission="admin")

        class TestTool(Tool):
            def execute(self, parameters):
                return {"result": "ok"}

        tool = TestTool("test", "测试工具", input_schema, output_schema, metadata)
        assert tool.name == "test"
        assert tool.description == "测试工具"
        assert tool.metadata.timeout_ms == 10000
        assert tool.metadata.permission == "admin"

    def test_validate_input_success(self, mock_tool):
        """测试输入验证成功"""
        assert mock_tool.validate_input({"query": "test"}) is True

    def test_validate_input_missing_required(self, mock_tool):
        """测试缺少必需参数"""
        assert mock_tool.validate_input({}) is False

    def test_validate_input_wrong_type(self, mock_tool):
        """测试参数类型错误"""
        assert mock_tool.validate_input({"query": 123}) is False
