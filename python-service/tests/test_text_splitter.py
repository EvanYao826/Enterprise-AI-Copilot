"""测试 text_splitter 文本分块器"""

import pytest
from langchain_core.documents import Document
from core.text_splitter import SemanticChunkerSplitter, AdaptiveChunker, create_chunker


@pytest.fixture
def splitter():
    return SemanticChunkerSplitter(chunk_size=200, chunk_overlap=20, min_chunk_size=50)


@pytest.fixture
def adaptive_chunker():
    return AdaptiveChunker({"chunk_size": 200, "chunk_overlap": 20, "min_chunk_size": 50})


class TestSemanticChunkerSplitter:
    """测试语义分块器"""

    def test_split_empty_text(self, splitter):
        """测试空文本"""
        assert splitter.split_text("") == []
        assert splitter.split_text("   ") == []
        assert splitter.split_text(None) == []

    def test_split_short_text(self, splitter):
        """测试短文本（不需要切分）"""
        text = "这是一段短文本。"
        chunks = splitter.split_text(text)
        assert len(chunks) == 1
        assert chunks[0] == text

    def test_split_by_paragraph(self, splitter):
        """测试按段落切分"""
        text = "段落一的内容。\n\n段落二的内容。\n\n段落三的内容。"
        chunks = splitter.split_text(text)
        assert len(chunks) >= 1

    def test_split_long_paragraph(self, splitter):
        """测试长段落按句子切分"""
        # 构造超过 chunk_size 的段落（每句约8字符，需要足够多句）
        sentences = ["这是编号" + str(i) + "的一个测试句子。" for i in range(50)]
        text = "".join(sentences)
        chunks = splitter.split_text(text)
        assert len(chunks) > 1

    def test_merge_small_chunks(self, splitter):
        """测试合并小块"""
        # 构造多个小块
        small_chunks = ["短", "文本", "块"]
        merged = splitter._merge_small_chunks(small_chunks)
        # 应该被合并
        assert len(merged) < len(small_chunks)

    def test_clean_text(self, splitter):
        """测试文本清理"""
        text = "有\r\n多余  的\t空格"
        cleaned = splitter._clean_text(text)
        assert "\r\n" not in cleaned
        assert "\t" not in cleaned

    def test_split_documents(self, splitter):
        """测试分割文档列表"""
        docs = [
            Document(page_content="文档一的内容，比较短。", metadata={"source": "test1"}),
            Document(page_content="文档二的内容，也比较短。", metadata={"source": "test2"}),
        ]
        result = splitter.split_documents(docs)
        assert len(result) >= 2
        # 检查 metadata 被保留
        for doc in result:
            assert "source" in doc.metadata
            assert "chunk_index" in doc.metadata
            assert "total_chunks" in doc.metadata

    def test_split_by_punctuation(self, splitter):
        """测试按标点切分"""
        text = "这是一段很长的文本，包含很多逗号。还有句号！还有感叹号？"
        chunks = splitter._split_by_punctuation(text)
        assert len(chunks) >= 1


class TestAdaptiveChunker:
    """测试自适应分块器"""

    def test_default_config(self):
        """测试默认配置"""
        chunker = AdaptiveChunker()
        assert chunker.chunk_size == 500
        assert chunker.chunk_overlap == 50
        assert chunker.min_chunk_size == 100
        assert chunker.strategy == "semantic"

    def test_custom_config(self):
        """测试自定义配置"""
        config = {"chunk_size": 300, "chunk_overlap": 30, "strategy": "recursive"}
        chunker = AdaptiveChunker(config)
        assert chunker.chunk_size == 300
        assert chunker.strategy == "recursive"

    def test_split_documents(self, adaptive_chunker):
        """测试分割文档"""
        docs = [Document(page_content="测试内容。" * 20, metadata={"file_type": "txt"})]
        result = adaptive_chunker.split_documents(docs)
        assert len(result) >= 1

    def test_image_docs_returned_as_is(self, adaptive_chunker):
        """测试图片文档不切分"""
        docs = [Document(page_content="OCR识别结果", metadata={"file_type": "image"})]
        result = adaptive_chunker.split_documents(docs)
        assert len(result) == 1
        assert result[0].page_content == "OCR识别结果"

    def test_update_config(self, adaptive_chunker):
        """测试更新配置"""
        adaptive_chunker.update_config({"chunk_size": 1000})
        assert adaptive_chunker.chunk_size == 1000


class TestCreateChunker:
    """测试工厂函数"""

    def test_create_default(self):
        """测试默认创建"""
        chunker = create_chunker()
        assert isinstance(chunker, AdaptiveChunker)

    def test_create_with_config(self):
        """测试带配置创建"""
        config = {"chunk_size": 100}
        chunker = create_chunker(config)
        assert chunker.chunk_size == 100
