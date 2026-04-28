from typing import Dict, Any, Optional, Callable, List
from dataclasses import dataclass, field
from datetime import datetime
import logging

logger = logging.getLogger(__name__)


class EventType:
    """事件类型枚举"""
    RUN_STARTED = "run_started"
    RUN_COMPLETED = "run_completed"
    RUN_FAILED = "run_failed"
    RUN_INTERRUPTED = "run_interrupted"
    RUN_TIMEOUT = "run_timeout"
    STEP_STARTED = "step_started"
    STEP_COMPLETED = "step_completed"
    STEP_FAILED = "step_failed"
    STEP_SKIPPED = "step_skipped"
    TOOL_CALL_STARTED = "tool_call_started"
    TOOL_CALL_COMPLETED = "tool_call_completed"
    TOOL_CALL_FAILED = "tool_call_failed"
    INTENT_RECOGNIZED = "intent_recognized"
    QUESTION_CLASSIFIED = "question_classified"
    CLARIFICATION_NEEDED = "clarification_needed"
    QUESTION_REWRITTEN = "question_rewritten"
    RETRIEVAL_COMPLETED = "retrieval_completed"
    SUFFICIENCY_EVALUATED = "sufficiency_evaluated"
    ANSWER_GENERATED = "answer_generated"
    MEMORY_WRITTEN = "memory_written"
    STATE_UPDATED = "state_updated"


@dataclass
class Event:
    """事件基类"""
    event_type: str
    run_id: str
    timestamp: datetime = field(default_factory=datetime.now)
    data: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "event_type": self.event_type,
            "run_id": self.run_id,
            "timestamp": self.timestamp.isoformat(),
            "data": self.data
        }


@dataclass
class RunStartedEvent(Event):
    """运行开始事件"""
    def __init__(self, run_id: str, goal: str, input_data: str, **kwargs):
        super().__init__(
            event_type=EventType.RUN_STARTED,
            run_id=run_id,
            data={"goal": goal, "input": input_data}
        )


@dataclass
class RunCompletedEvent(Event):
    """运行完成事件"""
    def __init__(self, run_id: str, output: Dict[str, Any], **kwargs):
        super().__init__(
            event_type=EventType.RUN_COMPLETED,
            run_id=run_id,
            data={"output": output}
        )


@dataclass
class RunFailedEvent(Event):
    """运行失败事件"""
    def __init__(self, run_id: str, error: str, error_code: Optional[str] = None, **kwargs):
        super().__init__(
            event_type=EventType.RUN_FAILED,
            run_id=run_id,
            data={"error": error, "error_code": error_code}
        )


@dataclass
class StepEvent(Event):
    """步骤事件基类"""
    step_id: str = ""
    step_name: str = ""
    step_type: str = ""

    def to_dict(self) -> Dict[str, Any]:
        result = super().to_dict()
        result["data"]["step_id"] = self.step_id
        result["data"]["step_name"] = self.step_name
        result["data"]["step_type"] = self.step_type
        return result


@dataclass
class StepStartedEvent(StepEvent):
    """步骤开始事件"""
    def __init__(self, run_id: str, step_id: str, step_name: str, step_type: str, **kwargs):
        super().__init__(
            event_type=EventType.STEP_STARTED,
            run_id=run_id,
            step_id=step_id,
            step_name=step_name,
            step_type=step_type
        )


@dataclass
class StepCompletedEvent(StepEvent):
    """步骤完成事件"""
    def __init__(self, run_id: str, step_id: str, step_name: str, step_type: str,
                 output: Dict[str, Any], duration_ms: float, **kwargs):
        super().__init__(
            event_type=EventType.STEP_COMPLETED,
            run_id=run_id,
            step_id=step_id,
            step_name=step_name,
            step_type=step_type,
            data={"output": output, "duration_ms": duration_ms}
        )


@dataclass
class StepFailedEvent(StepEvent):
    """步骤失败事件"""
    def __init__(self, run_id: str, step_id: str, step_name: str, step_type: str,
                 error: str, **kwargs):
        super().__init__(
            event_type=EventType.STEP_FAILED,
            run_id=run_id,
            step_id=step_id,
            step_name=step_name,
            step_type=step_type,
            data={"error": error}
        )


@dataclass
class ToolCallEvent(Event):
    """工具调用事件"""
    tool_call_id: str = ""
    tool_name: str = ""

    def to_dict(self) -> Dict[str, Any]:
        result = super().to_dict()
        result["data"]["tool_call_id"] = self.tool_call_id
        result["data"]["tool_name"] = self.tool_name
        return result


@dataclass
class ToolCallCompletedEvent(ToolCallEvent):
    """工具调用完成事件"""
    def __init__(self, run_id: str, tool_call_id: str, tool_name: str,
                 output: Dict[str, Any], duration_ms: float, **kwargs):
        super().__init__(
            event_type=EventType.TOOL_CALL_COMPLETED,
            run_id=run_id,
            tool_call_id=tool_call_id,
            tool_name=tool_name,
            data={"output": output, "duration_ms": duration_ms}
        )


@dataclass
class ToolCallFailedEvent(ToolCallEvent):
    """工具调用失败事件"""
    def __init__(self, run_id: str, tool_call_id: str, tool_name: str,
                 error: str, **kwargs):
        super().__init__(
            event_type=EventType.TOOL_CALL_FAILED,
            run_id=run_id,
            tool_call_id=tool_call_id,
            tool_name=tool_name,
            data={"error": error}
        )


@dataclass
class AnswerGeneratedEvent(Event):
    """答案生成事件"""
    def __init__(self, run_id: str, answer: str, sources: List[Dict[str, Any]], **kwargs):
        super().__init__(
            event_type=EventType.ANSWER_GENERATED,
            run_id=run_id,
            data={"answer": answer, "sources": sources}
        )


class EventBus:
    """事件总线 - 负责事件的发布和订阅"""

    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._handlers = {}
            cls._instance._global_handlers = []
        return cls._instance

    def subscribe(self, event_type: str, handler: Callable[[Event], None]):
        """订阅事件"""
        if event_type not in self._handlers:
            self._handlers[event_type] = []
        self._handlers[event_type].append(handler)
        logger.debug(f"Subscribed handler for event type: {event_type}")

    def subscribe_global(self, handler: Callable[[Event], None]):
        """订阅所有事件"""
        self._global_handlers.append(handler)
        logger.debug("Subscribed global event handler")

    def unsubscribe(self, event_type: str, handler: Callable[[Event], None]):
        """取消订阅"""
        if event_type in self._handlers:
            self._handlers[event_type].remove(handler)

    def publish(self, event: Event):
        """发布事件"""
        logger.debug(f"Publishing event: {event.event_type} for run: {event.run_id}")

        if event.event_type in self._handlers:
            for handler in self._handlers[event.event_type]:
                try:
                    handler(event)
                except Exception as e:
                    logger.error(f"Error handling event {event.event_type}: {e}")

        for handler in self._global_handlers:
            try:
                handler(event)
            except Exception as e:
                logger.error(f"Error in global event handler: {e}")

    def clear(self):
        """清空所有订阅"""
        self._handlers.clear()
        self._global_handlers.clear()


event_bus = EventBus()
