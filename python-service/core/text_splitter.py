import os
import re
from typing import List, Optional, Dict, Any
from langchain_core.documents import Document
from langchain.text_splitter import RecursiveCharacterTextSplitter, TextSplitter
from langchain_experimental.text_splitter import SemanticChunker
import logging

logger = logging.getLogger(__name__)

class SemanticChunkerSplitter(TextSplitter):
    """
    基于语义的分块器，结合多种策略进行文档切分

    策略：
    1. 首先尝试按段落切分（保留段落完整性）
    2. 如果段落太长，再按句子切分
    3. 保留文档结构信息（标题、列表等）
    4. 添加重叠以保持上下文连贯性
    """

    def __init__(
        self,
        chunk_size: int = 500,
        chunk_overlap: int = 50,
        min_chunk_size: int = 100,
        separator: str = "\n\n",
        sentence_separator: str = "\n",
        max_heading_length: int = 100
    ):
        super().__init__(chunk_size=chunk_size, chunk_overlap=chunk_overlap)
        self.min_chunk_size = min_chunk_size
        self.separator = separator
        self.sentence_separator = sentence_separator
        self.max_heading_length = max_heading_length

        # 备用递归切分器
        self.fallback_splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separators=["\n\n", "\n", "。", "！", "？", "；", "，", " ", ""]
        )

    def split_text(self, text: str) -> List[str]:
        """将文本分割成语义块"""
        if not text or not text.strip():
            return []

        # 清理文本
        text = self._clean_text(text)

        # 策略1：按双换行符切分（段落）
        paragraphs = self._split_by_paragraphs(text)

        chunks = []
        current_chunk = ""

        for paragraph in paragraphs:
            # 跳过空段落
            if not paragraph.strip():
                continue

            # 如果单个段落就超过chunk_size，需要进一步切分
            if len(paragraph) > self.chunk_size:
                # 先保存当前chunk
                if current_chunk.strip():
                    chunks.append(current_chunk.strip())
                    current_chunk = ""

                # 对长段落按句子切分
                sentence_chunks = self._split_long_paragraph(paragraph)
                chunks.extend(sentence_chunks)
            else:
                # 检查添加这个段落是否会超过chunk_size
                if len(current_chunk) + len(paragraph) + len(self.separator) > self.chunk_size:
                    # 保存当前chunk，开始新的
                    if current_chunk.strip():
                        chunks.append(current_chunk.strip())
                    current_chunk = paragraph
                else:
                    # 添加到当前chunk
                    if current_chunk:
                        current_chunk += self.separator + paragraph
                    else:
                        current_chunk = paragraph

        # 添加最后一个chunk
        if current_chunk.strip():
            chunks.append(current_chunk.strip())

        # 合并太小的chunks
        chunks = self._merge_small_chunks(chunks)

        logger.info(f"Semantic chunking resulted in {len(chunks)} chunks")
        return chunks

    def _clean_text(self, text: str) -> str:
        """清理文本"""
        # 移除多余的空白字符
        text = re.sub(r'\r\n', '\n', text)
        text = re.sub(r'[ \t]+', ' ', text)
        # 移除全角空格
        text = text.replace('\u3000', ' ')
        return text

    def _split_by_paragraphs(self, text: str) -> List[str]:
        """按段落切分"""
        # 按双换行符或单个换行符切分
        paragraphs = re.split(r'\n\s*\n|\n', text)
        return [p.strip() for p in paragraphs if p.strip()]

    def _split_long_paragraph(self, paragraph: str) -> List[str]:
        """对长段落按句子进一步切分"""
        # 句子结束符
        sentence_ends = r'(?<=[。！？；\n])|(?<=[.!?;]\s)'
        sentences = re.split(sentence_ends, paragraph)

        chunks = []
        current_chunk = ""

        for sentence in sentences:
            sentence = sentence.strip()
            if not sentence:
                continue

            # 如果单个句子就超过chunk_size，按字符切分
            if len(sentence) > self.chunk_size:
                if current_chunk.strip():
                    chunks.append(current_chunk.strip())
                    current_chunk = ""

                # 按字符切分，但尽量在标点处断开
                sub_chunks = self._split_by_punctuation(sentence)
                chunks.extend(sub_chunks)
            else:
                if len(current_chunk) + len(sentence) > self.chunk_size:
                    if current_chunk.strip():
                        chunks.append(current_chunk.strip())
                    current_chunk = sentence
                else:
                    if current_chunk:
                        current_chunk += sentence
                    else:
                        current_chunk = sentence

        if current_chunk.strip():
            chunks.append(current_chunk.strip())

        return chunks

    def _split_by_punctuation(self, text: str) -> List[str]:
        """按标点符号进一步切分长文本"""
        # 保留的分隔符
        delimiters = ['，', '。', '；', '、', ':', '：', '"', '"', "'", "'"]

        chunks = []
        current_pos = 0
        text_len = len(text)

        while current_pos < text_len:
            # 找到最近的分隔符
            next_delimiter_pos = -1
            delimiter = ''

            for d in delimiters:
                pos = text.find(d, current_pos)
                if pos != -1 and (next_delimiter_pos == -1 or pos < next_delimiter_pos):
                    next_delimiter_pos = pos
                    delimiter = d

            if next_delimiter_pos == -1 or next_delimiter_pos - current_pos > self.chunk_size:
                # 没有找到分隔符或下一个分隔符太远，直接取剩余字符
                chunk = text[current_pos:current_pos + self.chunk_size]
                if chunk.strip():
                    chunks.append(chunk.strip())
                current_pos += self.chunk_size
            else:
                # 在分隔符处断开
                chunk = text[current_pos:next_delimiter_pos + 1]
                if chunk.strip():
                    chunks.append(chunk.strip())
                current_pos = next_delimiter_pos + 1

        return chunks

    def _merge_small_chunks(self, chunks: List[str]) -> List[str]:
        """合并太小的chunks"""
        if not chunks:
            return []

        merged = []
        current = chunks[0]

        for i in range(1, len(chunks)):
            # 如果当前chunk太小，尝试与下一个合并
            if len(current) < self.min_chunk_size and i < len(chunks):
                current += self.separator + chunks[i]
            else:
                if current.strip():
                    merged.append(current.strip())
                current = chunks[i]

        # 添加最后一个
        if current.strip():
            merged.append(current.strip())

        return merged

    def split_documents(self, documents: List[Document]) -> List[Document]:
        """分割文档列表"""
        result = []
        for doc in documents:
            chunks = self.split_text(doc.page_content)
            for i, chunk in enumerate(chunks):
                result.append(Document(
                    page_content=chunk,
                    metadata={
                        **doc.metadata,
                        "chunk_index": i,
                        "total_chunks": len(chunks)
                    }
                ))
        return result


class AdaptiveChunker:
    """
    自适应分块器，根据文档类型和内容动态调整分块策略
    """

    def __init__(self, config: Optional[Dict[str, Any]] = None):
        self.config = config or {}

        # 从配置读取参数，使用默认值
        self.chunk_size = self.config.get('chunk_size', 500)
        self.chunk_overlap = self.config.get('chunk_overlap', 50)
        self.min_chunk_size = self.config.get('min_chunk_size', 100)

        # 根据文档类型选择策略
        self.strategy = self.config.get('strategy', 'semantic')

        if self.strategy == 'semantic':
            self.splitter = SemanticChunkerSplitter(
                chunk_size=self.chunk_size,
                chunk_overlap=self.chunk_overlap,
                min_chunk_size=self.min_chunk_size
            )
        else:
            # 默认使用RecursiveCharacterTextSplitter
            self.splitter = RecursiveCharacterTextSplitter(
                chunk_size=self.chunk_size,
                chunk_overlap=self.chunk_overlap,
                separators=["\n\n", "\n", "。", "！", "？", "；", "，", " ", ""]
            )

    def split_documents(self, documents: List[Document]) -> List[Document]:
        """分割文档"""
        logger.info(f"Splitting {len(documents)} documents using '{self.strategy}' strategy")

        # 检测文档类型
        for doc in documents:
            file_type = doc.metadata.get('file_type', '').lower()

            # 根据文件类型调整策略
            if file_type == 'pdf':
                # PDF文档可能有多列布局，使用更保守的策略
                self.splitter = RecursiveCharacterTextSplitter(
                    chunk_size=self.chunk_size,
                    chunk_overlap=self.chunk_overlap,
                    separators=["\n\n", "\n", " ", ""]
                )
            elif file_type == 'image':
                # 图片OCR结果通常不需要进一步切分
                return documents

        return self.splitter.split_documents(documents)

    def update_config(self, config: Dict[str, Any]):
        """更新配置并重新初始化切分器"""
        self.config.update(config)
        self.chunk_size = self.config.get('chunk_size', 500)
        self.chunk_overlap = self.config.get('chunk_overlap', 50)
        self.min_chunk_size = self.config.get('min_chunk_size', 100)
        self.strategy = self.config.get('strategy', 'semantic')

        if self.strategy == 'semantic':
            self.splitter = SemanticChunkerSplitter(
                chunk_size=self.chunk_size,
                chunk_overlap=self.chunk_overlap,
                min_chunk_size=self.min_chunk_size
            )
        else:
            self.splitter = RecursiveCharacterTextSplitter(
                chunk_size=self.chunk_size,
                chunk_overlap=self.chunk_overlap,
                separators=["\n\n", "\n", "。", "！", "？", "；", "，", " ", ""]
            )


def create_chunker(config: Optional[Dict[str, Any]] = None) -> AdaptiveChunker:
    """创建分块器的工厂函数"""
    return AdaptiveChunker(config)
