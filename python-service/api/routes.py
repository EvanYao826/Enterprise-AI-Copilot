from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from core.parser import DocumentParser
from core.vector_store import VectorStoreManager
from core.llm import LLMService
import os
import re

router = APIRouter()

# 初始化核心服务
try:
    print("Initializing DocumentParser...")
    parser = DocumentParser()
    print("Initializing VectorStoreManager...")
    vector_store = VectorStoreManager()
    print("Initializing LLMService...")
    llm_service = LLMService()
    print("Services initialized successfully.")
except Exception as e:
    print(f"Error initializing services: {e}")
    # Consider whether to exit or just log, depending on whether the app can run partially.
    # For now, we'll let it run, but requests might fail.

class ParseRequest(BaseModel):
    file_path: str
    doc_id: int

class ChatRequest(BaseModel):
    question: str
    # context: str = "" # Optional, if context is passed directly (not used here)

class SummaryRequest(BaseModel):
    question: str

@router.post("/parse")
async def parse_document(request: ParseRequest):
    """
    解析文档并存入向量库
    """
    try:
        # 判断 file_path 是 URL 还是本地路径
        is_url = re.match(r'^https?://', request.file_path) is not None or \
                 re.match(r'^[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+/', request.file_path) is not None
        
        if not is_url and not os.path.exists(request.file_path):
            raise HTTPException(status_code=404, detail=f"File not found: {request.file_path}")

        print(f"Parsing document: {request.file_path}")
        # Parse the document
        chunks = parser.parse(request.file_path)
        
        # Add metadata
        for chunk in chunks:
            chunk.metadata["doc_id"] = request.doc_id
            chunk.metadata["source"] = request.file_path

        print(f"Generated {len(chunks)} chunks. Adding to vector store...")
        vector_store.add_documents(chunks)
        
        return {"status": "success", "chunks_count": len(chunks)}
    except HTTPException:
        raise
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/ask")
async def ask_question(request: ChatRequest):
    """
    问答接口
    """
    try:
        print(f"Received question: {request.question}")
        
        # 1. 向量搜索召回相关文档
        # Search for relevant documents
        docs = vector_store.search(request.question, k=3)
        print(f"Found {len(docs)} relevant documents")
        
        # 2. 调用 LLM 生成回答
        # Generate answer using LLM
        answer = llm_service.get_answer(request.question, docs)
        
        # Extract sources for the response
        sources = []
        seen_docs = set()
        
        for doc in docs:
            source = doc.metadata.get("source")
            doc_id = doc.metadata.get("doc_id")
            
            # 使用 source 或 doc_id 进行去重
            unique_key = str(doc_id) if doc_id else source
            if unique_key in seen_docs:
                continue
            seen_docs.add(unique_key)
            
            source_info = {
                "source": source,
                "doc_id": doc_id,
                "page": doc.metadata.get("page")
            }
            # 尝试从 source 中提取文件名
            if source_info["source"]:
                source_info["doc_name"] = os.path.basename(source_info["source"])
                # 如果是 URL，提取 URL 的文件名部分
                if source_info["source"].startswith(('http://', 'https://')):
                    source_info["doc_name"] = source_info["source"].split('/')[-1]
                    # 移除 URL 参数（如果有）
                    if '?' in source_info["doc_name"]:
                         source_info["doc_name"] = source_info["doc_name"].split('?')[0]
            
            sources.append(source_info)
        
        return {"answer": answer, "sources": sources}
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/summary")
async def generate_summary(request: SummaryRequest):
    """
    生成会话标题
    """
    try:
        title = llm_service.generate_title(request.question)
        return {"title": title}
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))
