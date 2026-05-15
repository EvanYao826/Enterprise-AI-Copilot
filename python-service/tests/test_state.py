"""测试 AgentState 状态管理"""

import pytest
from agent.state import (
    AgentState, AgentStatus, AgentStep, StepType, StepStatus,
    TerminationCondition, IntermediateConclusion, ToolCallRecord
)


class TestAgentState:
    """测试 AgentState 基本功能"""

    def test_create_state_with_defaults(self):
        """测试默认创建状态"""
        state = AgentState()
        assert state.status == AgentStatus.PENDING
        assert state.run_id is not None
        assert state.trace_id is not None
        assert state.steps == []
        assert state.max_steps == 20
        assert state.timeout_seconds == 300

    def test_create_state_with_custom_values(self):
        """测试自定义创建状态"""
        state = AgentState(
            run_id="custom-run",
            trace_id="custom-trace",
            conversation_id="conv-123",
            user_id="user-456",
            goal="测试目标",
            original_input="测试输入"
        )
        assert state.run_id == "custom-run"
        assert state.trace_id == "custom-trace"
        assert state.conversation_id == "conv-123"
        assert state.user_id == "user-456"
        assert state.goal == "测试目标"
        assert state.original_input == "测试输入"

    def test_start_state(self):
        """测试启动状态"""
        state = AgentState()
        state.start()
        assert state.status == AgentStatus.RUNNING
        assert state.start_time is not None

    def test_complete_state(self):
        """测试完成状态"""
        state = AgentState()
        state.start()
        state.complete({"answer": "测试答案"})
        assert state.status == AgentStatus.COMPLETED
        assert state.final_output == {"answer": "测试答案"}
        assert state.end_time is not None

    def test_fail_state(self):
        """测试失败状态"""
        state = AgentState()
        state.start()
        state.fail("测试错误", "TEST_ERROR")
        assert state.status == AgentStatus.FAILED
        assert state.error_message == "测试错误"
        assert state.error_code == "TEST_ERROR"

    def test_wait_state(self):
        """测试等待状态"""
        state = AgentState()
        state.wait()
        assert state.status == AgentStatus.WAITING

    def test_interrupt_state(self):
        """测试中断状态"""
        state = AgentState()
        state.start()
        state.interrupt()
        assert state.status == AgentStatus.INTERRUPTED


class TestAgentStep:
    """测试 AgentStep 步骤管理"""

    def test_add_step(self):
        """测试添加步骤"""
        state = AgentState()
        step = state.add_step(StepType.KNOWLEDGE_SEARCH, "knowledge_search", {"query": "test"})
        assert step.step_type == StepType.KNOWLEDGE_SEARCH
        assert step.step_name == "knowledge_search"
        assert step.status == StepStatus.PENDING
        assert len(state.steps) == 1

    def test_step_lifecycle(self):
        """测试步骤生命周期"""
        step = AgentStep(
            step_id="step-001",
            step_type=StepType.ANSWER_GENERATION,
            step_name="answer_generation"
        )
        assert step.status == StepStatus.PENDING

        step.start()
        assert step.status == StepStatus.RUNNING
        assert step.start_time is not None

        step.complete({"answer": "test"})
        assert step.status == StepStatus.COMPLETED
        assert step.output_data == {"answer": "test"}
        assert step.end_time is not None
        assert step.duration_ms is not None

    def test_step_fail(self):
        """测试步骤失败"""
        step = AgentStep(
            step_id="step-001",
            step_type=StepType.TOOL_CALL,
            step_name="tool_call"
        )
        step.fail("执行失败")
        assert step.status == StepStatus.FAILED
        assert step.error_message == "执行失败"

    def test_step_skip(self):
        """测试步骤跳过"""
        step = AgentStep(
            step_id="step-001",
            step_type=StepType.CLARIFICATION,
            step_name="clarification"
        )
        step.skip()
        assert step.status == StepStatus.SKIPPED


class TestTerminationCondition:
    """测试终止条件"""

    def test_should_terminate_on_completed(self):
        """测试完成状态应终止"""
        state = AgentState()
        state.complete({"answer": "test"})
        should_terminate, reason = TerminationCondition.should_terminate(state), TerminationCondition.get_termination_reason(state)
        assert should_terminate is True
        assert "已完成" in reason

    def test_should_terminate_on_failed(self):
        """测试失败状态应终止"""
        state = AgentState()
        state.fail("错误")
        should_terminate = TerminationCondition.should_terminate(state)
        assert should_terminate is True

    def test_should_not_terminate_on_running(self):
        """测试运行状态不应终止"""
        state = AgentState()
        state.start()
        should_terminate = TerminationCondition.should_terminate(state)
        assert should_terminate is False

    def test_should_terminate_on_timeout(self):
        """测试超时应终止"""
        state = AgentState(timeout_seconds=0)
        state.start()
        import time
        time.sleep(0.1)
        should_terminate = TerminationCondition.should_terminate(state)
        assert should_terminate is True


class TestIntermediateConclusion:
    """测试中间结论"""

    def test_add_conclusion(self):
        """测试添加中间结论"""
        state = AgentState()
        step = state.add_step(StepType.INTENT_RECOGNITION, "intent_recognition")
        state.add_intermediate_conclusion(
            step_id=step.step_id,
            conclusion_type="intent",
            content="knowledge_qa",
            confidence=0.95
        )
        assert len(state.intermediate_conclusions) == 1
        assert state.intermediate_conclusions[0].content == "knowledge_qa"


class TestToolCallRecord:
    """测试工具调用记录"""

    def test_add_tool_call(self):
        """测试添加工具调用记录"""
        state = AgentState()
        state.add_tool_call(
            tool_call_id="call-001",
            tool_name="knowledge_search",
            input_params={"query": "test"},
            output={"results": []},
            status="success",
            duration_ms=150.5
        )
        assert len(state.tool_calls) == 1
        assert state.tool_calls[0].tool_name == "knowledge_search"


class TestStateSerialization:
    """测试状态序列化"""

    def test_to_dict(self):
        """测试转换为字典"""
        state = AgentState(
            run_id="test-run",
            original_input="test input"
        )
        state.start()
        state.add_step(StepType.KNOWLEDGE_SEARCH, "knowledge_search")

        data = state.to_dict()
        assert data["run_id"] == "test-run"
        assert data["original_input"] == "test input"
        assert data["status"] == "running"
        assert len(data["steps"]) == 1
