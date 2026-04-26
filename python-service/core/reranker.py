import os
import logging
from typing import List, Optional, Tuple, Dict, Any
from langchain_core.documents import Document
from langchain_core.embeddings import Embeddings
import numpy as np

logger = logging.getLogger(__name__)


class RerankerResult:
    """Rerank结果封装"""

    def __init__(self, document: Document, score: float, original_index: int):
        self.document = document
        self.score = score
        self.original_index = original_index

    def __repr__(self):
        return f"RerankerResult(score={self.score:.4f}, index={self.original_index})"


class BaseReranker:
    """Reranker基类"""

    def rerank(
        self,
        query: str,
        documents: List[Document],
        top_k: int = 3
    ) -> List[RerankerResult]:
        """
        对文档进行重排序

        Args:
            query: 查询文本
            documents: 文档列表
            top_k: 返回前k个结果

        Returns:
            重排序后的文档列表
        """
        raise NotImplementedError


class BGEReranker(BaseReranker):
    """
    使用BGE模型进行重排序

    BGE-Reranker是一种中文语义重排序模型，可以显著提升检索质量
    """

    def __init__(
        self,
        model_name: str = "BAAI/bge-reranker-base",
        use_fp16: bool = True,
        device: Optional[str] = None
    ):
        """
        初始化BGE Reranker

        Args:
            model_name: 模型名称
            use_fp16: 是否使用FP16加速
            device: 设备类型，'cpu', 'cuda', 'mps'
        """
        self.model_name = model_name
        self.use_fp16 = use_fp16
        self.device = device or self._get_default_device()
        self.model = None
        self._load_model()

    def _get_default_device(self) -> str:
        """获取默认设备"""
        try:
            import torch
            if torch.cuda.is_available():
                return "cuda"
            elif hasattr(torch.backends, 'mps') and torch.backends.mps.is_available():
                return "mps"
        except ImportError:
            pass
        return "cpu"

    def _load_model(self):
        """加载模型"""
        try:
            from sentence_transformers import CrossEncoder
            logger.info(f"Loading BGE Reranker model: {self.model_name}")

            self.model = CrossEncoder(
                self.model_name,
                max_length=512,
                device=self.device
            )
            logger.info(f"BGE Reranker loaded successfully on {self.device}")
        except ImportError as e:
            logger.warning(f"sentence-transformers not installed: {e}")
            logger.warning("Falling back to simple reranking")
            self.model = None
        except Exception as e:
            logger.error(f"Failed to load BGE Reranker: {e}")
            logger.warning("Falling back to simple reranking")
            self.model = None

    def rerank(
        self,
        query: str,
        documents: List[Document],
        top_k: int = 3
    ) -> List[RerankerResult]:
        """
        对文档进行重排序

        Args:
            query: 查询文本
            documents: 文档列表
            top_k: 返回前k个结果

        Returns:
            重排序后的文档列表
        """
        if not documents:
            return []

        if self.model is None:
            logger.warning("Reranker model not loaded, returning original order")
            return [
                RerankerResult(doc, 1.0, i)
                for i, doc in enumerate(documents[:top_k])
            ]

        try:
            # 准备输入对
            pairs = [[query, doc.page_content] for doc in documents]

            # 计算相关性分数
            scores = self.model.predict(pairs)

            # 如果返回的是单个值而不是数组
            if not isinstance(scores, np.ndarray):
                scores = np.array([scores])

            # 创建结果列表
            results = [
                RerankerResult(doc, float(score), i)
                for i, (doc, score) in enumerate(zip(documents, scores))
            ]

            # 按分数降序排序
            results.sort(key=lambda x: x.score, reverse=True)

            # 返回top_k
            logger.info(f"Reranked {len(documents)} documents, top {top_k} scores: {[r.score for r in results[:top_k]]}")
            return results[:top_k]

        except Exception as e:
            logger.error(f"Reranking failed: {e}")
            # 失败时返回原始顺序
            return [
                RerankerResult(doc, 1.0, i)
                for i, doc in enumerate(documents[:top_k])
            ]


class SimpleReranker(BaseReranker):
    """
    简单的重排序器，使用关键词匹配和BM25

    作为BGE Reranker的备选方案
    """

    def __init__(self, alpha: float = 0.5):
        """
        初始化简单重排序器

        Args:
            alpha: 语义分数权重 (1-alpha为关键词匹配权重)
        """
        self.alpha = alpha

    def _calculate_keyword_score(self, query: str, document: Document) -> float:
        """计算关键词匹配分数"""
        # 简单的字符级别匹配（适用于中文）
        query_chars = set([c for c in query.lower() if c.isalnum()])
        doc_chars = set([c for c in document.page_content.lower() if c.isalnum()])

        if not query_chars:
            return 0.0

        # 计算Jaccard相似度
        intersection = query_chars & doc_chars
        union = query_chars | doc_chars

        jaccard = len(intersection) / len(union) if union else 0

        # 计算覆盖率
        coverage = len(intersection) / len(query_chars) if query_chars else 0

        return (jaccard + coverage) / 2

    def rerank(
        self,
        query: str,
        documents: List[Document],
        top_k: int = 3
    ) -> List[RerankerResult]:
        """对文档进行简单重排序"""
        if not documents:
            return []

        results = []

        for i, doc in enumerate(documents):
            keyword_score = self._calculate_keyword_score(query, doc)
            # 简单重排序，保留原始分数
            combined_score = keyword_score
            results.append(RerankerResult(doc, combined_score, i))

        # 按分数降序排序
        results.sort(key=lambda x: x.score, reverse=True)

        logger.info(f"Simple reranked {len(documents)} documents")
        return results[:top_k]


class CohereReranker(BaseReranker):
    """
    使用Cohere API进行重排序

    需要COHERE_API_KEY环境变量
    """

    def __init__(self, api_key: Optional[str] = None, model: str = "rerank-multilingual-v2.0"):
        """
        初始化Cohere Reranker

        Args:
            api_key: Cohere API密钥（默认从环境变量读取）
            model: 使用的模型
        """
        self.api_key = api_key or os.getenv("COHERE_API_KEY")
        self.model = model
        self.client = None
        self._load_client()

    def _load_client(self):
        """加载Cohere客户端"""
        if not self.api_key:
            logger.warning("COHERE_API_KEY not found, Cohere Reranker disabled")
            return

        try:
            from cohere import Client
            self.client = Client(self.api_key)
            logger.info("Cohere Reranker initialized successfully")
        except ImportError:
            logger.warning("cohere not installed")
        except Exception as e:
            logger.error(f"Failed to initialize Cohere client: {e}")

    def rerank(
        self,
        query: str,
        documents: List[Document],
        top_k: int = 3
    ) -> List[RerankerResult]:
        """使用Cohere API进行重排序"""
        if not documents:
            return []

        if self.client is None:
            logger.warning("Cohere client not initialized, returning original order")
            return [
                RerankerResult(doc, 1.0, i)
                for i, doc in enumerate(documents[:top_k])
            ]

        try:
            response = self.client.rerank(
                query=query,
                documents=[doc.page_content for doc in documents],
                model=self.model,
                top_n=top_k
            )

            results = []
            for idx, result in enumerate(response.results):
                doc_index = result.index
                results.append(RerankerResult(
                    documents[doc_index],
                    result.relevance_score,
                    doc_index
                ))

            logger.info(f"Cohere reranked {len(documents)} documents")
            return results

        except Exception as e:
            logger.error(f"Cohere reranking failed: {e}")
            return [
                RerankerResult(doc, 1.0, i)
                for i, doc in enumerate(documents[:top_k])
            ]


def create_reranker(
    reranker_type: str = "bge",
    config: Optional[Dict[str, Any]] = None
) -> BaseReranker:
    """
    创建Reranker实例的工厂函数

    Args:
        reranker_type: Reranker类型，可选 'bge', 'cohere', 'simple'
        config: 配置字典

    Returns:
        Reranker实例
    """
    config = config or {}

    if reranker_type == "bge":
        return BGEReranker(
            model_name=config.get("model_name", "BAAI/bge-reranker-base"),
            use_fp16=config.get("use_fp16", True),
            device=config.get("device", None)
        )
    elif reranker_type == "cohere":
        return CohereReranker(
            api_key=config.get("api_key"),
            model=config.get("model", "rerank-multilingual-v2.0")
        )
    elif reranker_type == "simple":
        return SimpleReranker(
            alpha=config.get("alpha", 0.5)
        )
    else:
        logger.warning(f"Unknown reranker type '{reranker_type}', using simple reranker")
        return SimpleReranker()


class HybridReranker(BaseReranker):
    """
    混合重排序器，结合多种Reranker的结果

    使用加权投票的方式融合多个Reranker的结果
    """

    def __init__(self, rerankers: List[Tuple[BaseReranker, float]]):
        """
        初始化混合重排序器

        Args:
            rerankers: (Reranker, 权重) 元组列表
        """
        self.rerankers = rerankers

    def rerank(
        self,
        query: str,
        documents: List[Document],
        top_k: int = 3
    ) -> List[RerankerResult]:
        """使用混合策略对文档进行重排序"""
        if not documents:
            return []

        if len(self.rerankers) == 1:
            return self.rerankers[0][0].rerank(query, documents, top_k)

        # 收集所有Reranker的结果
        all_scores = []
        for reranker, weight in self.rerankers:
            results = reranker.rerank(query, documents, top_k=len(documents))
            # 归一化分数
            max_score = max(r.score for r in results) if results else 1.0
            min_score = min(r.score for r in results) if results else 0.0
            score_range = max_score - min_score if max_score != min_score else 1.0

            scores = {}
            for r in results:
                # 归一化到[0,1]
                normalized = (r.score - min_score) / score_range if score_range != 0 else 0.5
                scores[r.original_index] = normalized * weight

            all_scores.append(scores)

        # 合并分数
        combined_scores = {}
        for i, doc in enumerate(documents):
            combined_scores[i] = sum(scores.get(i, 0) for scores in all_scores)

        # 创建结果
        results = [
            RerankerResult(doc, combined_scores[i], i)
            for i, doc in enumerate(documents)
        ]

        # 按分数降序排序
        results.sort(key=lambda x: x.score, reverse=True)

        logger.info(f"Hybrid reranked {len(documents)} documents using {len(self.rerankers)} rerankers")
        return results[:top_k]
