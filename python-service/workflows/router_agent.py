from typing import Dict, Any, Optional, Generator
from enum import Enum
from workflows.knowledge_qa_agent import KnowledgeQAAgent
from workflows.chitchat_agent import ChitChatAgent
from workflows.admin_copilot_agent import AdminCopilotAgent
from workflows.inspection_agent import InspectionAgent
from workflows.retrieval_agent import RetrievalAgent
from intent.classifier import IntentClassifier, IntentType
import logging
import json

logger = logging.getLogger(__name__)


class TaskType(Enum):
    """任务类型枚举"""
    CHITCHAT = "chitchat"
    KNOWLEDGE_QA = "knowledge_qa"
    ADMIN_COPILOT = "admin_copilot"
    KNOWLEDGE_INSPECTION = "knowledge_inspection"
    UNKNOWN = "unknown"


class RouterAgent:
    """路由Agent - 负责将用户请求路由到合适的工作流"""

    def __init__(self):
        self.knowledge_qa_agent = KnowledgeQAAgent()
        self.chitchat_agent = ChitChatAgent()
        self.admin_copilot_agent = AdminCopilotAgent()
        self.inspection_agent = InspectionAgent()
        self.retrieval_agent = RetrievalAgent()
        self.classifier = IntentClassifier()

    def route(self, input_text: str, conversation_id: Optional[str] = None,
              user_id: Optional[str] = None, context: str = "",
              is_admin: bool = False, **kwargs) -> Dict[str, Any]:
        """
        路由并执行任务

        Args:
            input_text: 用户输入
            conversation_id: 会话ID
            user_id: 用户ID
            context: 对话上下文
            is_admin: 是否为管理员
            **kwargs: 其他参数

        Returns:
            执行结果
        """
        task_type = self.classify_task(input_text, is_admin)
        logger.info(f"[RouterAgent] Routing to: {task_type.value} for input: {input_text[:50]}...")

        try:
            if task_type == TaskType.CHITCHAT:
                return self.chitchat_agent.chat(
                    input_text, conversation_id, user_id, context, **kwargs
                )

            elif task_type == TaskType.KNOWLEDGE_QA:
                return self.knowledge_qa_agent.ask(
                    input_text, conversation_id, user_id, context, **kwargs
                )

            elif task_type == TaskType.ADMIN_COPILOT:
                return self.admin_copilot_agent.handle(
                    input_text, conversation_id, user_id, context, **kwargs
                )

            elif task_type == TaskType.KNOWLEDGE_INSPECTION:
                # 从输入中解析巡检类型
                inspection_type = self._parse_inspection_type(input_text)
                return self.inspection_agent.inspect(
                    inspection_type, conversation_id, user_id, context, **kwargs
                )

            else:
                return self.knowledge_qa_agent.ask(
                    input_text, conversation_id, user_id, context, **kwargs
                )
        except Exception as e:
            logger.error(f"[RouterAgent] Route error: {str(e)}")
            return {
                "answer": "抱歉，服务暂时不可用，请稍后再试。",
                "sources": [],
                "has_sources": False,
                "task_type": task_type.value,
                "error": True,
                "error_message": str(e)
            }

    def route_stream(self, input_text: str, conversation_id: Optional[str] = None,
                     user_id: Optional[str] = None, context: str = "",
                     is_admin: bool = False, **kwargs) -> Generator[str, None, None]:
        """
        流式路由并执行任务

        Args:
            input_text: 用户输入
            conversation_id: 会话ID
            user_id: 用户ID
            context: 对话上下文
            is_admin: 是否为管理员
            **kwargs: 其他参数

        Yields:
            JSON格式的事件流
        """
        task_type = self.classify_task(input_text, is_admin)
        logger.info(f"[RouterAgent] Streaming route to: {task_type.value}")

        try:
            yield json.dumps({
                "type": "routed",
                "task_type": task_type.value
            })

            if task_type == TaskType.CHITCHAT:
                for event in self.chitchat_agent.chat_stream(
                    input_text, conversation_id, user_id, context, **kwargs
                ):
                    yield event

            elif task_type == TaskType.KNOWLEDGE_QA:
                for event in self.knowledge_qa_agent.ask_stream(
                    input_text, conversation_id, user_id, context, **kwargs
                ):
                    yield event

            elif task_type == TaskType.ADMIN_COPILOT:
                for event in self.admin_copilot_agent.handle_stream(
                    input_text, conversation_id, user_id, context, **kwargs
                ):
                    yield event

            elif task_type == TaskType.KNOWLEDGE_INSPECTION:
                inspection_type = self._parse_inspection_type(input_text)
                for event in self.inspection_agent.inspect_stream(
                    inspection_type, conversation_id, user_id, context, **kwargs
                ):
                    yield event

            else:
                for event in self.knowledge_qa_agent.ask_stream(
                    input_text, conversation_id, user_id, context, **kwargs
                ):
                    yield event

        except Exception as e:
            logger.error(f"[RouterAgent] Stream route error: {str(e)}")
            yield json.dumps({
                "type": "error",
                "content": str(e)
            })

    def classify_task(self, input_text: str, is_admin: bool = False) -> TaskType:
        """
        分类任务类型 - 委托给统一分类器

        Args:
            input_text: 用户输入
            is_admin: 是否为管理员

        Returns:
            任务类型
        """
        result = self.classifier.classify(input_text, is_admin)

        # 将IntentType映射到TaskType
        intent_to_task = {
            IntentType.CHITCHAT: TaskType.CHITCHAT,
            IntentType.KNOWLEDGE_QA: TaskType.KNOWLEDGE_QA,
            IntentType.ADMIN_OPERATION: TaskType.ADMIN_COPILOT,
            IntentType.KNOWLEDGE_INSPECTION: TaskType.KNOWLEDGE_INSPECTION,
            IntentType.IDENTITY_QUERY: TaskType.CHITCHAT,
            IntentType.UNKNOWN: TaskType.KNOWLEDGE_QA,
        }

        return intent_to_task.get(result.intent, TaskType.KNOWLEDGE_QA)

    def _parse_inspection_type(self, input_text: str) -> str:
        """从输入中解析巡检类型"""
        lower_text = input_text.lower()

        if "重复" in lower_text:
            return "duplicate"
        elif "低质量" in lower_text or "质量" in lower_text or "片段" in lower_text:
            return "low_quality"
        elif "过期" in lower_text or "陈旧" in lower_text:
            return "stale"
        elif "无人访问" in lower_text or "没人看" in lower_text or "访问" in lower_text:
            return "unpopular"
        else:
            return "full"

    def get_agent(self, task_type: TaskType):
        """获取对应的Agent"""
        agent_map = {
            TaskType.CHITCHAT: self.chitchat_agent,
            TaskType.KNOWLEDGE_QA: self.knowledge_qa_agent,
            TaskType.ADMIN_COPILOT: self.admin_copilot_agent,
            TaskType.KNOWLEDGE_INSPECTION: self.inspection_agent,
        }
        return agent_map.get(task_type, self.knowledge_qa_agent)

    def get_task_stats(self) -> Dict[str, int]:
        """获取各类关键词数量统计（用于调试和分析）"""
        return self.classifier.get_keyword_stats()
