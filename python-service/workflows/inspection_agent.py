from typing import Dict, Any, Optional, Generator, List
from core.mysql_client import mysql_client
import logging
import json

logger = logging.getLogger(__name__)


class InspectionAgent:
    """知识巡检Agent - 专门处理知识库质量检查的工作流"""

    def __init__(self):
        self.inspection_types = {
            "duplicate": "重复文档检测",
            "low_quality": "低质量片段检测",
            "stale": "过期知识检测",
            "unpopular": "无人访问文档检测",
        }

    def inspect(self, inspection_type: str, conversation_id: Optional[str] = None,
               user_id: Optional[str] = None, context: str = "",
               **kwargs) -> Dict[str, Any]:
        """
        执行知识巡检

        Args:
            inspection_type: 巡检类型 (duplicate/low_quality/stale/unpopular)
            conversation_id: 会话ID
            user_id: 用户ID
            context: 对话上下文
            **kwargs: 其他参数

        Returns:
            包含巡检结果的字典
        """
        logger.info(f"[InspectionAgent] Processing inspection: {inspection_type}")

        try:
            if inspection_type == "duplicate":
                return self._check_duplicate_docs()

            elif inspection_type == "low_quality":
                return self._check_low_quality_chunks()

            elif inspection_type == "stale":
                return self._check_stale_knowledge()

            elif inspection_type == "unpopular":
                return self._check_unpopular_docs()

            else:
                return self._run_full_inspection()

        except Exception as e:
            logger.error(f"[InspectionAgent] Inspection error: {str(e)}")
            return {
                "answer": f"执行巡检时出错：{str(e)}",
                "sources": [],
                "has_sources": False,
                "task_type": "knowledge_inspection",
                "error": True
            }

    def inspect_stream(self, inspection_type: str, conversation_id: Optional[str] = None,
                      user_id: Optional[str] = None, context: str = "",
                      **kwargs) -> Generator[str, None, None]:
        """
        流式执行知识巡检

        Args:
            inspection_type: 巡检类型
            conversation_id: 会话ID
            user_id: 用户ID
            context: 对话上下文
            **kwargs: 其他参数

        Yields:
            JSON格式的事件流
        """
        logger.info(f"[InspectionAgent] Stream inspection: {inspection_type}")

        try:
            result = self.inspect(inspection_type, conversation_id, user_id, context, **kwargs)
            answer = result.get("answer", "")

            yield json.dumps({
                "type": "inspection_started",
                "inspection_type": inspection_type
            })

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
            logger.error(f"[InspectionAgent] Stream error: {str(e)}")
            yield json.dumps({
                "type": "error",
                "content": str(e)
            })

    def _check_duplicate_docs(self) -> Dict[str, Any]:
        """检测重复文档"""
        try:
            # 查询可能有重复的文档（相同标题或相似内容）
            query = """
                SELECT d1.id as doc1_id, d1.title as doc1_title,
                       d2.id as doc2_id, d2.title as doc2_title,
                       d1.created_at
                FROM knowledge_docs d1
                JOIN knowledge_docs d2 ON d1.title = d2.title AND d1.id < d2.id
                WHERE d1.is_deleted = 0 AND d2.is_deleted = 0
                LIMIT 20
            """
            duplicates = mysql_client.fetch_all(query) or []

            if not duplicates:
                return {
                    "answer": "✅ 重复文档检测完成\n\n未发现明显的重复文档，知识库文档唯一性良好。",
                    "sources": [],
                    "has_sources": False,
                    "task_type": "knowledge_inspection",
                    "inspection_type": "duplicate",
                    "data": {"duplicates": [], "count": 0}
                }

            duplicate_list = []
            for dup in duplicates:
                duplicate_list.append({
                    "doc1_id": dup.get("doc1_id"),
                    "doc1_title": dup.get("doc1_title"),
                    "doc2_id": dup.get("doc2_id"),
                    "doc2_title": dup.get("doc2_title"),
                })

            answer = f"""⚠️ 发现 {len(duplicates)} 对可能重复的文档：

"""
            for i, dup in enumerate(duplicate_list[:5], 1):
                answer += f"{i}. 《{dup['doc1_title']}》 与 《{dup['doc2_title']}》\n"

            if len(duplicate_list) > 5:
                answer += f"\n... 还有 {len(duplicate_list) - 5} 对重复文档未显示"

            answer += "\n\n建议：请管理员核实这些文档是否真的重复，决定是否合并或删除。"

            return {
                "answer": answer,
                "sources": [],
                "has_sources": False,
                "task_type": "knowledge_inspection",
                "inspection_type": "duplicate",
                "data": {"duplicates": duplicate_list, "count": len(duplicate_list)}
            }
        except Exception as e:
            logger.error(f"[InspectionAgent] Duplicate check error: {str(e)}")
            return {
                "answer": "重复文档检测失败，请稍后重试。",
                "sources": [],
                "has_sources": False,
                "task_type": "knowledge_inspection",
                "inspection_type": "duplicate",
                "error": True
            }

    def _check_low_quality_chunks(self) -> Dict[str, Any]:
        """检测低质量知识片段"""
        try:
            # 查询可能低质量的片段（内容过短或过长）
            query = """
                SELECT id, doc_id, content, LENGTH(content) as content_length
                FROM knowledge_chunks
                WHERE is_deleted = 0
                  AND (LENGTH(content) < 50 OR LENGTH(content) > 5000)
                ORDER BY content_length ASC
                LIMIT 20
            """
            low_quality = mysql_client.fetch_all(query) or []

            if not low_quality:
                return {
                    "answer": "✅ 低质量片段检测完成\n\n未发现明显的低质量知识片段，所有片段长度都在合理范围内。",
                    "sources": [],
                    "has_sources": False,
                    "task_type": "knowledge_inspection",
                    "inspection_type": "low_quality",
                    "data": {"low_quality_chunks": [], "count": 0}
                }

            chunk_list = []
            for chunk in low_quality:
                chunk_list.append({
                    "chunk_id": chunk.get("id"),
                    "doc_id": chunk.get("doc_id"),
                    "content_length": chunk.get("content_length"),
                })

            too_short = sum(1 for c in chunk_list if c["content_length"] < 50)
            too_long = sum(1 for c in chunk_list if c["content_length"] > 5000)

            answer = f"""⚠️ 发现 {len(low_quality)} 个可能低质量的知识片段：

📊 统计：
- 内容过短（<50字）：{too_short} 个
- 内容过长（>5000字）：{too_long} 个

"""
            for i, chunk in enumerate(chunk_list[:5], 1):
                if chunk["content_length"] < 50:
                    answer += f"{i}. 片段ID {chunk['chunk_id']}：内容过短（{chunk['content_length']}字）\n"
                else:
                    answer += f"{i}. 片段ID {chunk['chunk_id']}：内容过长（{chunk['content_length']}字）\n"

            if len(chunk_list) > 5:
                answer += f"\n... 还有 {len(chunk_list) - 5} 个片段未显示"

            answer += "\n\n建议：请管理员审核这些片段，过短的考虑合并，过长的考虑拆分。"

            return {
                "answer": answer,
                "sources": [],
                "has_sources": False,
                "task_type": "knowledge_inspection",
                "inspection_type": "low_quality",
                "data": {"low_quality_chunks": chunk_list, "count": len(chunk_list)}
            }
        except Exception as e:
            logger.error(f"[InspectionAgent] Low quality check error: {str(e)}")
            return {
                "answer": "低质量片段检测失败，请稍后重试。",
                "sources": [],
                "has_sources": False,
                "task_type": "knowledge_inspection",
                "inspection_type": "low_quality",
                "error": True
            }

    def _check_stale_knowledge(self) -> Dict[str, Any]:
        """检测过期知识"""
        try:
            # 查询30天以上未更新的文档
            query = """
                SELECT id, title, updated_at, created_at
                FROM knowledge_docs
                WHERE is_deleted = 0
                  AND updated_at < DATE_SUB(NOW(), INTERVAL 30 DAY)
                ORDER BY updated_at ASC
                LIMIT 20
            """
            stale_docs = mysql_client.fetch_all(query) or []

            if not stale_docs:
                return {
                    "answer": "✅ 过期知识检测完成\n\n所有文档都在30天内更新过，知识库内容较为新鲜。",
                    "sources": [],
                    "has_sources": False,
                    "task_type": "knowledge_inspection",
                    "inspection_type": "stale",
                    "data": {"stale_docs": [], "count": 0}
                }

            doc_list = []
            for doc in stale_docs:
                doc_list.append({
                    "doc_id": doc.get("id"),
                    "title": doc.get("title"),
                    "updated_at": str(doc.get("updated_at")),
                })

            answer = f"""⚠️ 发现 {len(stale_docs)} 个可能过期的知识文档（30天以上未更新）：

"""
            for i, doc in enumerate(doc_list[:5], 1):
                answer += f"{i}. 《{doc['title']}》 - 最后更新：{doc['updated_at']}\n"

            if len(doc_list) > 5:
                answer += f"\n... 还有 {len(doc_list) - 5} 个文档未显示"

            answer += "\n\n建议：请管理员审核这些文档，确认内容是否仍然有效，必要时进行更新。"

            return {
                "answer": answer,
                "sources": [],
                "has_sources": False,
                "task_type": "knowledge_inspection",
                "inspection_type": "stale",
                "data": {"stale_docs": doc_list, "count": len(doc_list)}
            }
        except Exception as e:
            logger.error(f"[InspectionAgent] Stale knowledge check error: {str(e)}")
            return {
                "answer": "过期知识检测失败，请稍后重试。",
                "sources": [],
                "has_sources": False,
                "task_type": "knowledge_inspection",
                "inspection_type": "stale",
                "error": True
            }

    def _check_unpopular_docs(self) -> Dict[str, Any]:
        """检测无人访问的文档"""
        try:
            # 查询从创建至今没有被访问过的文档
            query = """
                SELECT d.id, d.title, d.created_at,
                       (SELECT COUNT(*) FROM doc_view_logs WHERE doc_id = d.id) as view_count
                FROM knowledge_docs d
                WHERE d.is_deleted = 0
                  AND d.created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)
                HAVING view_count = 0
                ORDER BY d.created_at ASC
                LIMIT 20
            """
            unpopular = mysql_client.fetch_all(query) or []

            if not unpopular:
                return {
                    "answer": "✅ 无人访问文档检测完成\n\n所有文档在近7天内都有访问记录，知识库使用率良好。",
                    "sources": [],
                    "has_sources": False,
                    "task_type": "knowledge_inspection",
                    "inspection_type": "unpopular",
                    "data": {"unpopular_docs": [], "count": 0}
                }

            doc_list = []
            for doc in unpopular:
                doc_list.append({
                    "doc_id": doc.get("id"),
                    "title": doc.get("title"),
                    "created_at": str(doc.get("created_at")),
                    "view_count": doc.get("view_count", 0),
                })

            answer = f"""⚠️ 发现 {len(unpopular)} 个近7天无人访问的文档：

"""
            for i, doc in enumerate(doc_list[:5], 1):
                answer += f"{i}. 《{doc['title']}》 - 创建于：{doc['created_at']}\n"

            if len(doc_list) > 5:
                answer += f"\n... 还有 {len(doc_list) - 5} 个文档未显示"

            answer += "\n\n建议：请管理员审核这些文档，确认是否需要更新内容或从知识库移除。"

            return {
                "answer": answer,
                "sources": [],
                "has_sources": False,
                "task_type": "knowledge_inspection",
                "inspection_type": "unpopular",
                "data": {"unpopular_docs": doc_list, "count": len(doc_list)}
            }
        except Exception as e:
            logger.error(f"[InspectionAgent] Unpopular docs check error: {str(e)}")
            return {
                "answer": "无人访问文档检测失败，请稍后重试。",
                "sources": [],
                "has_sources": False,
                "task_type": "knowledge_inspection",
                "inspection_type": "unpopular",
                "error": True
            }

    def _run_full_inspection(self) -> Dict[str, Any]:
        """执行完整巡检"""
        try:
            # 获取各项巡检结果
            duplicate_result = self._check_duplicate_docs()
            low_quality_result = self._check_low_quality_chunks()
            stale_result = self._check_stale_knowledge()
            unpopular_result = self._check_unpopular_docs()

            # 汇总问题数量
            total_issues = (
                duplicate_result.get("data", {}).get("count", 0) +
                low_quality_result.get("data", {}).get("count", 0) +
                stale_result.get("data", {}).get("count", 0) +
                unpopular_result.get("data", {}).get("count", 0)
            )

            answer = f"""🔍 知识库完整巡检报告

━━━━━━━━━━━━━━━━━━

📋 巡检项目概览：

1️⃣ 重复文档检测：{duplicate_result.get("data", {}).get("count", 0)} 个问题
2️⃣ 低质量片段检测：{low_quality_result.get("data", {}).get("count", 0)} 个问题
3️⃣ 过期知识检测：{stale_result.get("data", {}).get("count", 0)} 个问题
4️⃣ 无人访问文档：{unpopular_result.get("data", {}).get("count", 0)} 个问题

━━━━━━━━━━━━━━━━━━

📊 问题总计：{total_issues} 个

"""

            if total_issues == 0:
                answer += "🎉 恭喜！知识库质量良好，未发现明显问题。"
            else:
                answer += "⚠️ 建议及时处理以上问题，以保持知识库质量。\n\n如需详细查看某一类问题，请单独执行该类巡检。"

            return {
                "answer": answer,
                "sources": [],
                "has_sources": False,
                "task_type": "knowledge_inspection",
                "inspection_type": "full",
                "data": {
                    "duplicate_count": duplicate_result.get("data", {}).get("count", 0),
                    "low_quality_count": low_quality_result.get("data", {}).get("count", 0),
                    "stale_count": stale_result.get("data", {}).get("count", 0),
                    "unpopular_count": unpopular_result.get("data", {}).get("count", 0),
                    "total_issues": total_issues
                }
            }
        except Exception as e:
            logger.error(f"[InspectionAgent] Full inspection error: {str(e)}")
            return {
                "answer": f"完整巡检失败：{str(e)}",
                "sources": [],
                "has_sources": False,
                "task_type": "knowledge_inspection",
                "inspection_type": "full",
                "error": True
            }

    def get_inspection_summary(self) -> Dict[str, Any]:
        """获取巡检摘要（不执行详细检测）"""
        return {
            "available_inspections": self.inspection_types,
            "usage": {
                "duplicate": "检测标题相同的重复文档",
                "low_quality": "检测内容过短或过长的片段",
                "stale": "检测30天以上未更新的文档",
                "unpopular": "检测7天内无人访问的文档",
                "full": "执行完整巡检（包含以上四项）"
            }
        }
