from typing import Dict, Any, Optional, Generator, Callable
from agent.state import AgentState, AgentStatus, StepType, TerminationCondition
from agent.planner import Planner, IntentType
from agent.executor import Executor
from agent.events import EventBus, Event, RunStartedEvent, RunCompletedEvent, RunFailedEvent, StepStartedEvent, StepCompletedEvent, StepFailedEvent
from agent.policies import policies
import time
import logging
import uuid
import json

logger = logging.getLogger(__name__)


class Orchestrator:
    """Agent编排器 - 负责协调整个Agent执行流程"""

    def __init__(self):
        self.planner = Planner()
        self.executor = Executor()
        self.event_bus = EventBus()
        self.policies = policies

    def create_state(self, input_text: str, conversation_id: Optional[str] = None,
                    user_id: Optional[str] = None, goal: Optional[str] = None,
                    run_id: Optional[str] = None, trace_id: Optional[str] = None) -> AgentState:
        """创建Agent状态"""
        state = AgentState(
            run_id=run_id or str(uuid.uuid4()),
            trace_id=trace_id or str(uuid.uuid4()),
            conversation_id=conversation_id,
            user_id=user_id,
            goal=goal or f"回答用户问题: {input_text[:50]}...",
            original_input=input_text,
            status=AgentStatus.PENDING
        )
        return state

    def run(self, input_text: str, conversation_id: Optional[str] = None,
            user_id: Optional[str] = None, context: str = "",
            goal: Optional[str] = None, run_id: Optional[str] = None,
            trace_id: Optional[str] = None, **kwargs) -> Dict[str, Any]:
        """同步执行Agent"""
        state = self.create_state(input_text, conversation_id, user_id, goal, run_id, trace_id)
        state.context = context

        is_valid, error_msg = self.policies.validate_input(input_text)
        if not is_valid:
            state.fail(error_msg or "输入验证失败", "INPUT_VALIDATION_ERROR")
            return self._build_error_response(state)

        state.start()
        self.event_bus.publish(RunStartedEvent(
            run_id=state.run_id,
            goal=state.goal,
            input_data=input_text
        ))

        try:
            planned_steps = self.planner.plan_steps(state)
            state.planned_steps = planned_steps

            for step_name in planned_steps:
                step_type = self._get_step_type(step_name)
                step = state.add_step(step_type, step_name, {"input": input_text})

                self.event_bus.publish(StepStartedEvent(
                    run_id=state.run_id,
                    step_id=step.step_id,
                    step_name=step_name,
                    step_type=step_type.value
                ))

                try:
                    self.executor.execute_step(state, step)

                    self.event_bus.publish(StepCompletedEvent(
                        run_id=state.run_id,
                        step_id=step.step_id,
                        step_name=step_name,
                        step_type=step_type.value,
                        output=step.output_data,
                        duration_ms=step.duration_ms or 0
                    ))

                    if state.status == AgentStatus.WAITING:
                        clarification_output = self._handle_clarification(state)
                        state.complete(clarification_output)
                        break

                    if step_type == StepType.ANSWER_GENERATION:
                        final_output = self._build_success_response(state)
                        state.complete(final_output)
                        break

                except Exception as e:
                    logger.error(f"[{state.run_id}] Step {step_name} failed: {str(e)}")
                    self.event_bus.publish(StepFailedEvent(
                        run_id=state.run_id,
                        step_id=step.step_id,
                        step_name=step_name,
                        step_type=step_type.value,
                        error=str(e)
                    ))

                    if self.policies.should_retry(0, e):
                        continue
                    else:
                        state.fail(str(e), "STEP_EXECUTION_ERROR")
                        break

                should_terminate, reason = self.planner.should_terminate(state)
                if should_terminate:
                    logger.info(f"[{state.run_id}] Terminating: {reason}")
                    if state.status == AgentStatus.PENDING:
                        state.complete(self._build_success_response(state))
                    break

            if state.status == AgentStatus.RUNNING:
                state.complete(self._build_success_response(state))

        except Exception as e:
            logger.error(f"[{state.run_id}] Orchestrator run failed: {str(e)}")
            state.fail(str(e), "ORCHESTRATOR_ERROR")
            self.event_bus.publish(RunFailedEvent(
                run_id=state.run_id,
                error=str(e),
                error_code="ORCHESTRATOR_ERROR"
            ))
            return self._build_error_response(state)

        if state.status == AgentStatus.COMPLETED:
            self.event_bus.publish(RunCompletedEvent(
                run_id=state.run_id,
                output=state.final_output
            ))
        elif state.status == AgentStatus.FAILED:
            self.event_bus.publish(RunFailedEvent(
                run_id=state.run_id,
                error=state.error_message,
                error_code=state.error_code
            ))

        return state.final_output if state.final_output else self._build_error_response(state)

    def run_stream(self, input_text: str, conversation_id: Optional[str] = None,
                  user_id: Optional[str] = None, context: str = "",
                  goal: Optional[str] = None, run_id: Optional[str] = None,
                  trace_id: Optional[str] = None, **kwargs) -> Generator[str, None, None]:
        """流式执行Agent"""
        state = self.create_state(input_text, conversation_id, user_id, goal, run_id, trace_id)
        state.context = context

        is_valid, error_msg = self.policies.validate_input(input_text)
        if not is_valid:
            state.fail(error_msg or "输入验证失败", "INPUT_VALIDATION_ERROR")
            yield json.dumps({
                "type": "error",
                "content": error_msg
            })
            return

        state.start()
        self.event_bus.publish(RunStartedEvent(
            run_id=state.run_id,
            goal=state.goal,
            input_data=input_text
        ))

        try:
            planned_steps = self.planner.plan_steps(state)
            state.planned_steps = planned_steps

            for step_name in planned_steps:
                step_type = self._get_step_type(step_name)
                step = state.add_step(step_type, step_name, {"input": input_text})

                yield json.dumps({
                    "type": "step_started",
                    "step_name": step_name,
                    "step_type": step_type.value
                })

                try:
                    self.executor.execute_step(state, step)

                    yield json.dumps({
                        "type": "step_completed",
                        "step_name": step_name,
                        "output": step.output_data
                    })

                    if state.status == AgentStatus.WAITING:
                        clarification_output = self._handle_clarification(state)
                        state.complete(clarification_output)
                        yield json.dumps({
                            "type": "clarification",
                            "content": clarification_output
                        })
                        break

                    if step_type == StepType.ANSWER_GENERATION:
                        answer = step.output_data.get("answer", "")
                        sources = step.output_data.get("sources", [])

                        yield json.dumps({
                            "type": "sources",
                            "content": sources
                        })

                        for char in answer:
                            yield json.dumps({
                                "type": "token",
                                "content": char
                            })

                        yield json.dumps({
                            "type": "end",
                            "content": {
                                "answer": answer,
                                "sources": sources
                            }
                        })

                        state.complete({
                            "answer": answer,
                            "sources": sources
                        })
                        break

                except Exception as e:
                    logger.error(f"[{state.run_id}] Step {step_name} failed: {str(e)}")
                    yield json.dumps({
                        "type": "step_failed",
                        "step_name": step_name,
                        "error": str(e)
                    })
                    state.fail(str(e), "STEP_EXECUTION_ERROR")
                    break

                should_terminate, reason = self.planner.should_terminate(state)
                if should_terminate:
                    break

        except Exception as e:
            logger.error(f"[{state.run_id}] Orchestrator stream run failed: {str(e)}")
            yield json.dumps({
                "type": "error",
                "content": str(e)
            })
            state.fail(str(e), "ORCHESTRATOR_ERROR")

    def _get_step_type(self, step_name: str) -> StepType:
        """获取步骤类型"""
        step_mapping = {
            "intent_recognition": StepType.INTENT_RECOGNITION,
            "question_classification": StepType.QUESTION_CLASSIFICATION,
            "clarification": StepType.CLARIFICATION,
            "question_rewrite": StepType.QUESTION_REWRITE,
            "knowledge_search": StepType.KNOWLEDGE_SEARCH,
            "result_evaluation": StepType.RESULT_EVALUATION,
            "answer_generation": StepType.ANSWER_GENERATION,
            "memory_write": StepType.MEMORY_WRITE,
            "identity_answer": StepType.ANSWER_GENERATION,
            "admin_operation": StepType.TOOL_CALL
        }
        return step_mapping.get(step_name, StepType.TOOL_CALL)

    def _handle_clarification(self, state: AgentState) -> Dict[str, Any]:
        """处理澄清请求"""
        clarification_step = None
        for step in reversed(state.steps):
            if step.step_type == StepType.CLARIFICATION:
                clarification_step = step
                break

        if clarification_step and clarification_step.output_data.get("needs_clarification"):
            prompt = clarification_step.output_data.get("prompt", "请提供更多信息")
            return {
                "type": "clarification",
                "content": prompt,
                "requires_input": True
            }

        return {
            "type": "clarification",
            "content": "请详细描述您的问题",
            "requires_input": True
        }

    def _build_success_response(self, state: AgentState) -> Dict[str, Any]:
        """构建成功响应"""
        answer = None
        sources = []

        for step in reversed(state.steps):
            if step.step_type == StepType.ANSWER_GENERATION and step.output_data:
                answer = step.output_data.get("answer", "")
                sources = step.output_data.get("sources", [])
                break

        if answer is None:
            answer = "抱歉，我无法生成回答。"

        return self.policies.format_response(answer, sources)

    def _build_error_response(self, state: AgentState) -> Dict[str, Any]:
        """构建错误响应"""
        return {
            "answer": state.error_message or "服务暂时不可用，请稍后再试。",
            "sources": [],
            "has_sources": False,
            "error": True,
            "error_code": state.error_code
        }

    def get_state(self, run_id: str) -> Optional[AgentState]:
        """获取Agent状态（如果实现了状态存储）"""
        return None

    def interrupt(self, run_id: str) -> bool:
        """中断执行"""
        return False
