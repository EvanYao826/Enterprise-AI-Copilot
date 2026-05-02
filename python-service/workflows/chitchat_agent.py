from typing import Dict, Any, Optional, Generator
from core.llm import llm
import logging
import json

logger = logging.getLogger(__name__)


class ChitChatAgent:
    """闲聊Agent - 专门处理日常对话和闲聊的工作流"""

    def __init__(self):
        self.chitchat_prompts = {
            "greeting": [
                "你好！很高兴见到你，有什么我可以帮助你的吗？",
                "您好！今天过得怎么样？有什么想聊的吗？",
                "Hi！有什么可以为你效劳的？",
            ],
            "thanks": [
                "不客气！能帮到你我很开心！",
                "不用谢，这是我应该做的！",
                "不客气，随时为你服务！",
            ],
            "identity": [
                "我是你的AI知识助手，可以帮你回答问题、查阅资料，也可以陪你聊聊天。你想了解什么？",
                "我是AI知识库助手，专注于知识问答，同时也可以陪你聊聊日常。",
            ],
            "weather": [
                "很抱歉，我无法实时获取天气信息。不过你可以查看天气预报APP来了解天气情况。",
            ],
            "time": [
                f"很抱歉，我无法告诉你当前时间，请查看你的设备时钟。",
            ],
            "joke": [
                "程序员最讨厌的季节是什么？是秋天！因为秋高气爽（bug少），但也会有很多落叶（bug）！",
                "你知道为什么程序员总是分不清万圣节和圣诞节吗？因为Oct 31 = Dec 25！",
                "为什么程序员喜欢黑暗模式？因为 Light attracts bugs！",
            ],
            "default": [
                "这是个有趣的话题！不过我主要专注于知识问答，你有什么具体的问题吗？",
                "嗯嗯，有意思！你想了解一些知识相关的内容吗？",
                "好的，不过我更擅长回答知识类问题，你有什么想了解的吗？",
            ]
        }

    def chat(self, question: str, conversation_id: Optional[str] = None,
             user_id: Optional[str] = None, context: str = "",
             **kwargs) -> Dict[str, Any]:
        """
        处理闲聊

        Args:
            question: 用户问题
            conversation_id: 会话ID
            user_id: 用户ID
            context: 对话上下文
            **kwargs: 其他参数

        Returns:
            包含answer的字典
        """
        logger.info(f"[ChitChatAgent] Processing chitchat: {question[:50]}...")

        try:
            answer = self._generate_chitchat_response(question)

            return {
                "answer": answer,
                "sources": [],
                "has_sources": False,
                "task_type": "chitchat"
            }
        except Exception as e:
            logger.error(f"[ChitChatAgent] Error: {str(e)}")
            return {
                "answer": "抱歉，我现在状态不太好，稍后再聊吧。",
                "sources": [],
                "has_sources": False,
                "task_type": "chitchat",
                "error": True
            }

    def chat_stream(self, question: str, conversation_id: Optional[str] = None,
                    user_id: Optional[str] = None, context: str = "",
                    **kwargs) -> Generator[str, None, None]:
        """
        流式处理闲聊

        Args:
            question: 用户问题
            conversation_id: 会话ID
            user_id: 用户ID
            context: 对话上下文
            **kwargs: 其他参数

        Yields:
            JSON格式的事件流
        """
        logger.info(f"[ChitChatAgent] Stream chitchat: {question[:50]}...")

        try:
            answer = self._generate_chitchat_response(question)

            for char in answer:
                yield json.dumps({
                    "type": "token",
                    "content": char
                })

            yield json.dumps({
                "type": "end",
                "content": {
                    "answer": answer,
                    "sources": [],
                    "task_type": "chitchat"
                }
            })
        except Exception as e:
            logger.error(f"[ChitChatAgent] Stream error: {str(e)}")
            yield json.dumps({
                "type": "error",
                "content": str(e)
            })

    def _generate_chitchat_response(self, question: str) -> str:
        """
        生成闲聊回复（基于规则和关键词匹配）

        Args:
            question: 用户问题

        Returns:
            回复内容
        """
        lower_question = question.lower()

        # 问候类
        if any(kw in lower_question for kw in ["你好", "您好", "hello", "hi", "早上好", "下午好", "晚上好"]):
            return self.chitchat_prompts["greeting"][0]

        # 感谢类
        if any(kw in lower_question for kw in ["谢谢", "感谢", "多谢", "thanks", "thank you"]):
            return self.chitchat_prompts["thanks"][0]

        # 身份询问类
        if any(kw in lower_question for kw in ["你叫什么", "你是谁", "你是什么", "你的名字"]):
            return self.chitchat_prompts["identity"][0]

        # 天气类
        if any(kw in lower_question for kw in ["天气", "下雨", "晴天", "温度"]):
            return self.chitchat_prompts["weather"][0]

        # 时间类
        if any(kw in lower_question for kw in ["几点", "时间", "日期", "今天是"]):
            return self.chitchat_prompts["time"][0]

        # 笑话类
        if any(kw in lower_question for kw in ["笑话", "讲个笑话", "笑", "搞笑"]):
            return self.chitchat_prompts["joke"][0]

        # 日常闲聊类 - 使用LLM生成更自然的回复
        try:
            prompt = f"""你是一个友好的AI助手。用户说："{question}"
请用友好、自然的方式回复，100字以内。如果用户想了解知识，可以引导他们问具体问题。"""

            response = llm.generate(prompt, temperature=0.7, max_tokens=150)
            return response.strip()
        except:
            return self.chitchat_prompts["default"][0]
