from typing import Dict, Any, Optional, Generator
from core.mysql_client import mysql_client
import logging
import json

logger = logging.getLogger(__name__)


class AdminCopilotAgent:
    """管理助手Agent - 专门处理管理端运营分析的工作流"""

    def __init__(self):
        self.admin_operations = {
            "stats": "统计分析",
            "knowledge_inspection": "知识巡检",
            "unanswered_analysis": "未命中分析",
            "user_analysis": "用户分析",
        }

    def handle(self, question: str, conversation_id: Optional[str] = None,
               user_id: Optional[str] = None, context: str = "",
               **kwargs) -> Dict[str, Any]:
        """
        处理管理助手请求

        Args:
            question: 用户问题
            conversation_id: 会话ID
            user_id: 用户ID
            context: 对话上下文
            **kwargs: 其他参数

        Returns:
            包含answer的字典
        """
        logger.info(f"[AdminCopilotAgent] Processing admin request: {question[:50]}...")

        try:
            operation = self._parse_operation(question)
            result = self._execute_operation(operation, question)

            return {
                "answer": result["answer"],
                "sources": [],
                "has_sources": False,
                "task_type": "admin_copilot",
                "operation": operation,
                "data": result.get("data")
            }
        except Exception as e:
            logger.error(f"[AdminCopilotAgent] Error: {str(e)}")
            return {
                "answer": f"抱歉，处理管理请求时出错：{str(e)}",
                "sources": [],
                "has_sources": False,
                "task_type": "admin_copilot",
                "error": True
            }

    def handle_stream(self, question: str, conversation_id: Optional[str] = None,
                     user_id: Optional[str] = None, context: str = "",
                     **kwargs) -> Generator[str, None, None]:
        """
        流式处理管理助手请求

        Args:
            question: 用户问题
            conversation_id: 会话ID
            user_id: 用户ID
            context: 对话上下文
            **kwargs: 其他参数

        Yields:
            JSON格式的事件流
        """
        logger.info(f"[AdminCopilotAgent] Stream admin request: {question[:50]}...")

        try:
            result = self.handle(question, conversation_id, user_id, context, **kwargs)
            answer = result.get("answer", "")

            for char in answer:
                yield json.dumps({
                    "type": "token",
                    "content": char
                })

            yield json.dumps({
                "type": "end",
                "content": result
            })
        except Exception as e:
            logger.error(f"[AdminCopilotAgent] Stream error: {str(e)}")
            yield json.dumps({
                "type": "error",
                "content": str(e)
            })

    def _parse_operation(self, question: str) -> str:
        """解析操作类型"""
        lower_question = question.lower()

        if any(kw in lower_question for kw in ["统计", "报表", "数据", "分析", "多少", "数量"]):
            return "stats"

        if any(kw in lower_question for kw in ["知识", "文档", "巡检", "检查", "质量"]):
            return "knowledge_inspection"

        if any(kw in lower_question for kw in ["未命中", "没有答案", "回答不了", "未知"]):
            return "unanswered_analysis"

        if any(kw in lower_question for kw in ["用户", "会员", "注册"]):
            return "user_analysis"

        return "stats"

    def _execute_operation(self, operation: str, question: str) -> Dict[str, Any]:
        """执行管理操作"""
        try:
            if operation == "stats":
                return self._get_stats()

            elif operation == "knowledge_inspection":
                return self._knowledge_inspection()

            elif operation == "unanswered_analysis":
                return self._unanswered_analysis()

            elif operation == "user_analysis":
                return self._user_analysis()

            else:
                return {
                    "answer": "抱歉，我暂时无法处理这类管理请求。",
                    "data": None
                }
        except Exception as e:
            logger.error(f"[AdminCopilotAgent] Operation error: {str(e)}")
            return {
                "answer": f"执行操作时出错：{str(e)}",
                "data": None
            }

    def _get_stats(self) -> Dict[str, Any]:
        """获取统计数据"""
        try:
            # 获取知识库统计
            doc_count = mysql_client.fetch_one("SELECT COUNT(*) as count FROM knowledge_docs") or {}
            chunk_count = mysql_client.fetch_one("SELECT COUNT(*) as count FROM knowledge_chunks") or {}

            # 获取问答统计
            qa_count = mysql_client.fetch_one("SELECT COUNT(*) as count FROM qa_logs") or {}

            # 获取用户统计
            user_count = mysql_client.fetch_one("SELECT COUNT(*) as count FROM users") or {}

            answer = f"""📊 系统统计信息：

📚 知识库：
- 文档数量：{doc_count.get('count', 0)}
- 知识片段：{chunk_count.get('count', 0)}

💬 问答系统：
- 总问答次数：{qa_count.get('count', 0)}

👥 用户管理：
- 注册用户：{user_count.get('count', 0)}

如需更详细的统计，请访问管理后台的仪表盘页面。"""

            return {
                "answer": answer,
                "data": {
                    "doc_count": doc_count.get('count', 0),
                    "chunk_count": chunk_count.get('count', 0),
                    "qa_count": qa_count.get('count', 0),
                    "user_count": user_count.get('count', 0)
                }
            }
        except Exception as e:
            logger.error(f"[AdminCopilotAgent] Stats error: {str(e)}")
            return {
                "answer": "获取统计数据失败，请稍后重试。",
                "data": None
            }

    def _knowledge_inspection(self) -> Dict[str, Any]:
        """知识巡检"""
        answer = """🔍 知识巡检功能：

我可以帮您检查：
1. 重复文档检测
2. 低质量知识片段
3. 长期未访问文档
4. 过期知识提醒

请访问管理后台的「知识巡检」页面进行详细检查，或者告诉我您想检查哪方面的问题？"""

        return {
            "answer": answer,
            "data": None
        }

    def _unanswered_analysis(self) -> Dict[str, Any]:
        """未命中分析"""
        answer = """📉 未命中问题分析：

未命中问题是指系统无法回答的问题。分析这些问题可以帮助我们：
1. 识别知识缺口
2. 补充知识库内容
3. 优化问答效果

请访问管理后台的「未命中问题」页面查看详细列表，也可以导出分析报告。需要我帮您查看具体的数据吗？"""

        return {
            "answer": answer,
            "data": None
        }

    def _user_analysis(self) -> Dict[str, Any]:
        """用户分析"""
        answer = """👥 用户分析：

用户分析功能包括：
1. 用户注册趋势
2. 活跃度统计
3. 热门问题排行
4. 用户反馈分析

请访问管理后台的「用户管理」和「仪表盘」页面查看详细数据。"""

        return {
            "answer": answer,
            "data": None
        }
