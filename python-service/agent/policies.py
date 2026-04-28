from typing import Dict, Any, Optional, List
from agent.state import AgentState, AgentStatus, TerminationCondition
from agent.planner import Planner, IntentType
import logging

logger = logging.getLogger(__name__)


class RetryPolicy:
    """重试策略"""

    def __init__(self, max_retries: int = 3, base_delay: float = 1.0, max_delay: float = 60.0):
        self.max_retries = max_retries
        self.base_delay = base_delay
        self.max_delay = max_delay

    def should_retry(self, attempt: int, error: Exception) -> bool:
        """判断是否应该重试"""
        if attempt >= self.max_retries:
            return False

        retryable_errors = [
            "timeout",
            "connection",
            "network",
            "rate_limit"
        ]

        error_str = str(error).lower()
        return any(e in error_str for e in retryable_errors)

    def get_delay(self, attempt: int) -> float:
        """计算重试延迟（指数退避）"""
        delay = min(self.base_delay * (2 ** attempt), self.max_delay)
        return delay


class FallbackPolicy:
    """降级策略"""

    def __init__(self):
        self.fallback_responses = {
            IntentType.CHITCHAT: "抱歉，我现在无法回应您的问题，让我们换个话题吧。",
            IntentType.KNOWLEDGE_QA: "抱歉，我暂时无法找到相关的知识来回答您的问题。",
            IntentType.UNKNOWN: "抱歉，我无法理解您的问题，请尝试重新描述。"
        }

    def get_fallback(self, intent: str) -> str:
        """获取降级响应"""
        return self.fallback_responses.get(intent, "抱歉，服务暂时不可用，请稍后再试。")


class GuardrailsPolicy:
    """安全守卫策略"""

    def __init__(self):
        self.dangerous_keywords = [
            "hack", "exploit", "病毒", "木马",
            "攻击", "入侵", "破解", "作弊"
        ]

        self.sensitive_topics = [
            "政治", "宗教", "色情", "暴力",
            "赌博", "毒品", "犯罪"
        ]

    def check_input(self, text: str) -> tuple[bool, Optional[str]]:
        """检查输入是否安全"""
        if not text:
            return True, None

        lower_text = text.lower()

        for keyword in self.dangerous_keywords:
            if keyword in lower_text:
                return False, f"输入包含敏感关键词: {keyword}"

        for topic in self.sensitive_topics:
            if topic in text:
                return False, f"输入涉及敏感话题: {topic}"

        return True, None

    def check_output(self, text: str) -> tuple[bool, Optional[str]]:
        """检查输出是否安全"""
        if not text:
            return True, None

        return True, None


class TimeoutPolicy:
    """超时策略"""

    def __init__(self, step_timeout: int = 30, total_timeout: int = 300):
        self.step_timeout = step_timeout
        self.total_timeout = total_timeout

    def should_timeout_step(self, state: AgentState, step_index: int) -> bool:
        """判断步骤是否超时"""
        step = state.steps[step_index] if step_index < len(state.steps) else None
        if step and step.start_time:
            import time
            elapsed = time.time() - step.start_time
            return elapsed > self.step_timeout
        return False

    def should_timeout_total(self, state: AgentState) -> bool:
        """判断总执行是否超时"""
        return state.elapsed_time > self.total_timeout


class ResponsePolicy:
    """响应策略"""

    def __init__(self):
        self.max_answer_length = 2000
        self.max_sources = 5

    def format_response(self, answer: str, sources: List[Dict[str, Any]],
                        include_sources: bool = True) -> Dict[str, Any]:
        """格式化响应"""
        truncated_answer = answer[:self.max_answer_length] if len(answer) > self.max_answer_length else answer

        formatted_sources = sources[:self.max_sources] if sources else []

        return {
            "answer": truncated_answer,
            "sources": formatted_sources if include_sources else [],
            "has_sources": len(formatted_sources) > 0 if include_sources else False
        }


class AgentPolicies:
    """Agent策略管理器"""

    def __init__(self):
        self.retry_policy = RetryPolicy()
        self.fallback_policy = FallbackPolicy()
        self.guardrails_policy = GuardrailsPolicy()
        self.timeout_policy = TimeoutPolicy()
        self.response_policy = ResponsePolicy()

    def validate_input(self, text: str) -> tuple[bool, Optional[str]]:
        """验证输入"""
        return self.guardrails_policy.check_input(text)

    def validate_output(self, text: str) -> tuple[bool, Optional[str]]:
        """验证输出"""
        return self.guardrails_policy.check_output(text)

    def get_fallback(self, intent: str) -> str:
        """获取降级响应"""
        return self.fallback_policy.get_fallback(intent)

    def should_retry(self, attempt: int, error: Exception) -> bool:
        """判断是否应该重试"""
        return self.retry_policy.should_retry(attempt, error)

    def get_retry_delay(self, attempt: int) -> float:
        """获取重试延迟"""
        return self.retry_policy.get_delay(attempt)

    def format_response(self, answer: str, sources: List[Dict[str, Any]],
                       include_sources: bool = True) -> Dict[str, Any]:
        """格式化响应"""
        return self.response_policy.format_response(answer, sources, include_sources)


policies = AgentPolicies()
