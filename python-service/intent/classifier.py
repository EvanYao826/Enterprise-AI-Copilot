"""统一意图分类器 - 使用LLM进行意图识别，关键词匹配作为fallback"""

import re
import json
from enum import Enum
from dataclasses import dataclass
from typing import Optional
import logging

logger = logging.getLogger(__name__)


class IntentType(Enum):
    """意图类型枚举"""
    CHITCHAT = "chitchat"
    KNOWLEDGE_QA = "knowledge_qa"
    ADMIN_OPERATION = "admin_operation"
    KNOWLEDGE_INSPECTION = "knowledge_inspection"
    IDENTITY_QUERY = "identity_query"
    UNKNOWN = "unknown"


@dataclass
class IntentResult:
    """意图识别结果"""
    intent: IntentType
    confidence: float
    reasoning: str
    requires_clarification: bool = False
    clarification_prompt: Optional[str] = None


class IntentClassifier:
    """统一意图分类器 - 优先使用LLM，fallback到关键词匹配"""

    def __init__(self):
        self._llm_service = None

        # 闲聊关键词库（fallback用）
        self.chitchat_keywords = [
            # 问候类
            "你好", "您好", "hello", "hi", "早上好", "下午好", "晚上好", "嗨", "嘿",
            # 告别类
            "谢谢", "感谢", "多谢", "再见", "拜拜", "拜拜咯", "下次见",
            # 身份询问类（针对系统）
            "你叫什么", "你是谁", "你是什么", "你的名字", "你从哪来",
            # 日常闲聊类
            "讲个笑话", "说个故事", "聊聊天", "有空吗", "最近怎么样", "最近如何",
            "在干嘛", "在做什么", "忙吗", "累不累",
            # 时间天气类（无法获取真实数据）
            "天气", "下雨", "晴天", "温度", "几点", "现在几点", "时间", "日期", "今天几号",
            # 娱乐类
            "笑话", "搞笑", "有趣", "好玩", "电影", "音乐", "歌曲",
            # 生活类
            "好吃", "美食", "餐厅", "好玩", "旅游", "景点",
            # 情感类
            "开心", "高兴", "难过", "伤心", "郁闷", "不爽",
            # 确认类
            "可以吗", "行吗", "好吗", "对不对", "是不是", "会不会",
        ]

        # 管理助手关键词库（fallback用）
        self.admin_keywords = [
            "管理", "后台", "运营", "统计", "报表", "仪表盘",
            "用户", "会员", "注册", "活跃", "用户分析",
            "未命中", "没有答案", "回答不了", "未知问题",
            "知识巡检", "文档检查", "质量检测", "过期",
            "热门问题", "问答统计", "使用率",
            # P4-3 Ops Agent新增
            "知识缺口", "缺口分析", "运营报告", "运营分析",
            "日志分析", "运营建议", "后台建议",
            "活跃用户", "活跃度", "用户活动",
        ]

        # 知识巡检关键词库（fallback用）
        self.inspection_keywords = [
            "巡检", "检查", "检测", "审查",
            "重复文档", "重复", "一样", "相同",
            "低质量", "质量差", "片段", "内容短", "内容长",
            "过期", "陈旧", "太久", "未更新",
            "无人访问", "没人看", "没人查", "访问量",
            "知识库健康", "知识库状态", "文档状态",
        ]

        # 专业问答关键词库（fallback用）
        self.knowledge_qa_keywords = [
            # 数据库相关（重要！）
            "mysql", "oracle", "postgresql", "mongodb", "redis", "elasticsearch",
            "sql", "nosql", "关系型", "非关系型", "数据表", "索引", "事务", "锁", "并发",
            "封锁协议", "一级封锁协议", "二级封锁协议", "三级封锁协议",
            "并发控制", "隔离级别", "可重复读", "脏读", "不可重复读", "丢失修改",
            # 疑问词
            "什么", "怎么", "如何", "为什么", "哪个", "哪里", "谁", "多少", "几", "何时", "怎样",
            # 技术类
            "编程", "代码", "算法", "数据结构", "数据库", "网络", "安全", "加密", "协议",
            "人工智能", "机器学习", "深度学习", "神经网络", "大模型", "LLM", "RAG", "AGI",
            "java", "python", "javascript", "js", "c++", "go", "rust", "typescript", "php", "ruby",
            "spring", "django", "flask", "react", "vue", "angular", "nodejs", "node.js",
            # 概念类
            "原理", "机制", "工作原理", "实现原理", "概念", "定义", "术语", "解释", "说明",
            "是什么", "什么意思", "指什么",
            # 方法类
            "步骤", "流程", "方法", "技巧", "策略", "方案", "思路",
            "怎么做", "如何实现", "如何处理", "如何解决",
            # 比较类
            "比较", "对比", "区别", "差异", "不同", "优势", "缺点", "优缺点",
            "哪个好", "有什么区别", "有什么不同",
            # 应用类
            "应用", "用途", "使用场景", "案例", "例子", "实例",
            "可以用在", "适用于", "用于",
            # 技术概念
            "架构", "设计模式", "微服务", "分布式", "集群", "容器", "docker", "k8s", "kubernetes",
            "缓存", "队列", "消息", "api", "rest", "rpc", "grpc", "websocket",
            "前端", "后端", "全栈", "运维", "DevOps", "CI/CD",
            # 其他技术领域
            "区块链", "物联网", "云计算", "边缘计算", "5G", "大数据", "数据分析",
            "机器视觉", "自然语言处理", "NLP", "CV", "语音识别",
        ]

        # 情感分析词汇（fallback用）
        self.emotion_keywords = [
            "哈哈", "呵呵", "嘿嘿", "开心", "高兴", "难过", "伤心", "郁闷", "烦",
            "累", "困", "饿", "渴", "舒服", "不爽", "真好", "太棒了", "不错",
        ]

    @property
    def llm_service(self):
        """延迟加载LLM服务"""
        if self._llm_service is None:
            try:
                from core.llm import llm_service
                self._llm_service = llm_service
            except Exception as e:
                logger.warning(f"Failed to load LLM service: {e}")
                self._llm_service = False  # 标记为不可用
        return self._llm_service

    def classify(self, input_text: str, is_admin: bool = False) -> IntentResult:
        """
        统一意图分类 - 优先使用LLM，fallback到关键词匹配

        Args:
            input_text: 用户输入文本
            is_admin: 是否为管理员

        Returns:
            IntentResult: 意图识别结果
        """
        # 优先使用LLM进行意图识别
        if self.llm_service and self.llm_service.llm:
            try:
                result = self._classify_with_llm(input_text, is_admin)
                if result:
                    return result
            except Exception as e:
                logger.warning(f"LLM classification failed, falling back to keywords: {e}")

        # Fallback到关键词匹配
        return self._classify_with_keywords(input_text, is_admin)

    def _classify_with_llm(self, input_text: str, is_admin: bool = False) -> Optional[IntentResult]:
        """使用LLM进行意图识别"""
        from langchain_core.prompts import PromptTemplate
        from langchain_core.output_parsers import StrOutputParser

        # 构建意图识别的prompt
        intent_prompt = PromptTemplate.from_template(
            """
你是一个意图识别系统。请分析用户输入，判断其意图类型。

用户输入：{input_text}
是否管理员：{is_admin}

可选的意图类型：
1. chitchat - 闲聊（问候、告别、日常聊天、情感表达、身份询问等）
2. knowledge_qa - 知识问答（技术问题、概念解释、方法步骤、比较分析等）
3. admin_operation - 管理操作（后台管理、数据统计、运营分析等，需要管理员权限）
4. knowledge_inspection - 知识巡检（检查文档质量、重复、过期等）
5. identity_query - 身份查询（询问系统身份、名称等）
6. unknown - 无法确定

请返回JSON格式：
{"intent": "意图类型", "confidence": 0.0-1.0, "reasoning": "判断理由"}

只返回JSON，不要其他内容。
"""
        )

        try:
            chain = intent_prompt | self.llm_service.llm | StrOutputParser()
            result_str = chain.invoke({
                "input_text": input_text,
                "is_admin": "是" if is_admin else "否"
            })

            # 解析JSON结果
            result_str = result_str.strip()
            # 提取JSON部分（处理可能的markdown代码块）
            json_match = re.search(r'\{[^}]+\}', result_str)
            if json_match:
                result_json = json.loads(json_match.group())
            else:
                result_json = json.loads(result_str)

            # 映射意图类型
            intent_map = {
                "chitchat": IntentType.CHITCHAT,
                "knowledge_qa": IntentType.KNOWLEDGE_QA,
                "admin_operation": IntentType.ADMIN_OPERATION,
                "knowledge_inspection": IntentType.KNOWLEDGE_INSPECTION,
                "identity_query": IntentType.IDENTITY_QUERY,
                "unknown": IntentType.UNKNOWN,
            }

            intent_str = result_json.get("intent", "unknown")
            intent = intent_map.get(intent_str, IntentType.UNKNOWN)
            confidence = float(result_json.get("confidence", 0.5))
            reasoning = result_json.get("reasoning", "LLM判断")

            return IntentResult(
                intent=intent,
                confidence=confidence,
                reasoning=f"LLM: {reasoning}"
            )

        except Exception as e:
            logger.error(f"LLM intent classification error: {e}")
            return None

    def _classify_with_keywords(self, input_text: str, is_admin: bool = False) -> IntentResult:
        """使用关键词匹配进行意图识别（fallback）"""
        lower_text = input_text.lower()

        # 身份查询优先检查
        if any(keyword in lower_text for keyword in ["我是谁", "我叫什么", "我的名字", "我的身份"]):
            return IntentResult(
                intent=IntentType.IDENTITY_QUERY,
                confidence=0.95,
                reasoning="用户询问身份相关问题"
            )

        # 去除中英文标点符号，避免 "你是谁？" 中 "？" 干扰分类
        clean_text = re.sub(r'[，。！？、；：""''【】《》（）\(\)\[\]\{\}<>\?\!\.\,\;\:\"\'\-\—\…\~\`]', '', lower_text)

        # 计算各类关键词命中数量（使用去除标点后的文本）
        chitchat_score = sum(1 for kw in self.chitchat_keywords if kw in clean_text)
        knowledge_score = sum(1 for kw in self.knowledge_qa_keywords if kw in clean_text)
        admin_score = sum(1 for kw in self.admin_keywords if kw in clean_text)
        inspection_score = sum(1 for kw in self.inspection_keywords if kw in clean_text)
        emotion_score = sum(1 for kw in self.emotion_keywords if kw in clean_text)

        logger.debug(f"[IntentClassifier] Scores - chitchat:{chitchat_score}, knowledge:{knowledge_score}, admin:{admin_score}, inspection:{inspection_score}")

        # 管理员模式：优先处理管理相关任务
        if is_admin:
            # 知识巡检优先级最高
            if inspection_score > 0:
                return IntentResult(
                    intent=IntentType.KNOWLEDGE_INSPECTION,
                    confidence=min(0.95, 0.6 + inspection_score * 0.1),
                    reasoning=f"管理员模式，命中{inspection_score}个巡检关键词"
                )
            # 管理助手
            if admin_score > 0:
                # 如果技术词汇更多，可能是知识问答
                if knowledge_score >= admin_score:
                    return IntentResult(
                        intent=IntentType.KNOWLEDGE_QA,
                        confidence=min(0.95, 0.5 + knowledge_score * 0.15),
                        reasoning=f"管理员模式，但技术词更多（{knowledge_score} vs {admin_score}）"
                    )
                return IntentResult(
                    intent=IntentType.ADMIN_OPERATION,
                    confidence=min(0.9, 0.5 + admin_score * 0.1),
                    reasoning=f"管理员模式，命中{admin_score}个管理关键词"
                )

        # 知识巡检（管理员和普通用户都可以触发）
        if inspection_score > 0 and inspection_score >= knowledge_score:
            return IntentResult(
                intent=IntentType.KNOWLEDGE_INSPECTION,
                confidence=min(0.9, 0.6 + inspection_score * 0.1),
                reasoning=f"命中{inspection_score}个巡检关键词"
            )

        # 技术问题优先判定为知识问答
        if knowledge_score >= 2:
            return IntentResult(
                intent=IntentType.KNOWLEDGE_QA,
                confidence=min(0.95, 0.5 + knowledge_score * 0.15),
                reasoning=f"命中{knowledge_score}个知识关键词"
            )

        # 闲聊判断（基于多个因素）
        if chitchat_score > 0 or emotion_score > 0:
            # 如果文本较短且包含闲聊词汇
            if len(input_text.strip()) < 15:
                # 包含技术词汇则优先知识问答（技术词汇权重更高）
                if knowledge_score > chitchat_score:
                    return IntentResult(
                        intent=IntentType.KNOWLEDGE_QA,
                        confidence=min(0.9, 0.5 + knowledge_score * 0.15),
                        reasoning=f"短文本但技术词更多（{knowledge_score} vs {chitchat_score}）"
                    )
                # 包含管理词汇则优先管理操作
                if admin_score > 0:
                    return IntentResult(
                        intent=IntentType.ADMIN_OPERATION,
                        confidence=min(0.9, 0.5 + admin_score * 0.1),
                        reasoning=f"短文本，命中{admin_score}个管理关键词"
                    )
                return IntentResult(
                    intent=IntentType.CHITCHAT,
                    confidence=min(0.9, 0.5 + chitchat_score * 0.1),
                    reasoning=f"短文本，命中{chitchat_score}个闲聊关键词"
                )
            # 长文本：如果闲聊词和技术词都多，取分值高的
            if knowledge_score > chitchat_score:
                return IntentResult(
                    intent=IntentType.KNOWLEDGE_QA,
                    confidence=min(0.9, 0.5 + knowledge_score * 0.15),
                    reasoning=f"长文本，技术词更多（{knowledge_score} vs {chitchat_score}）"
                )
            # 包含管理词汇则优先管理操作
            if admin_score > 0:
                return IntentResult(
                    intent=IntentType.ADMIN_OPERATION,
                    confidence=min(0.9, 0.5 + admin_score * 0.1),
                    reasoning=f"长文本，命中{admin_score}个管理关键词"
                )
            return IntentResult(
                intent=IntentType.CHITCHAT,
                confidence=min(0.85, 0.5 + chitchat_score * 0.1),
                reasoning=f"长文本，闲聊词更多（{chitchat_score} vs {knowledge_score}）"
            )

        # 短文本处理（没有任何关键词命中）
        if len(input_text.strip()) < 10:
            # 检查是否包含管理关键词
            if admin_score > 0:
                return IntentResult(
                    intent=IntentType.ADMIN_OPERATION,
                    confidence=min(0.85, 0.5 + admin_score * 0.1),
                    reasoning=f"短文本，命中{admin_score}个管理关键词"
                )
            # 检查是否包含疑问词
            question_words = ["什么", "怎么", "如何", "为什么", "哪个", "哪里", "谁", "多少", "?", "？"]
            if any(kw in clean_text for kw in question_words):
                return IntentResult(
                    intent=IntentType.KNOWLEDGE_QA,
                    confidence=0.6,
                    reasoning="短文本包含疑问词"
                )
            # 检查是否包含中文字符
            has_chinese = bool(re.search(r'[一-鿿]', input_text))
            if has_chinese:
                # 有中文但无关键词，视为闲聊
                return IntentResult(
                    intent=IntentType.CHITCHAT,
                    confidence=0.5,
                    reasoning="短文本默认为闲聊"
                )
            # 无中文且无关键词，返回UNKNOWN
            return IntentResult(
                intent=IntentType.UNKNOWN,
                confidence=0.5,
                reasoning="无法确定意图类型"
            )

        # 有一个技术关键词就判定为知识问答
        if knowledge_score >= 1:
            return IntentResult(
                intent=IntentType.KNOWLEDGE_QA,
                confidence=min(0.85, 0.5 + knowledge_score * 0.15),
                reasoning=f"命中{knowledge_score}个知识关键词"
            )

        # 默认是知识问答
        return IntentResult(
            intent=IntentType.KNOWLEDGE_QA,
            confidence=0.5,
            reasoning="默认为知识问答"
        )

    def get_keyword_stats(self) -> dict:
        """获取各类关键词数量统计（用于调试和分析）"""
        return {
            "chitchat_keywords": len(self.chitchat_keywords),
            "knowledge_qa_keywords": len(self.knowledge_qa_keywords),
            "admin_keywords": len(self.admin_keywords),
            "inspection_keywords": len(self.inspection_keywords),
            "emotion_keywords": len(self.emotion_keywords),
        }
