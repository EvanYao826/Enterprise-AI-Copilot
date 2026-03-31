from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel
from core.parser import DocumentParser
from core.vector_store import VectorStoreManager
from core.llm import LLMService
import os
import re
import logging
import json
import time

# 配置结构化日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

router = APIRouter()

# 初始化核心服务
try:
    logger.info("Initializing DocumentParser...")
    parser = DocumentParser()
    logger.info("Initializing VectorStoreManager...")
    vector_store = VectorStoreManager()
    logger.info("Initializing LLMService...")
    llm_service = LLMService()
    logger.info("Services initialized successfully.")
except Exception as e:
    logger.error(f"Error initializing services: {e}")
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

# 移除中间件，改用在每个路由中记录请求时间
# APIRouter 不支持 middleware 方法，中间件只能添加到 FastAPI 应用实例

@router.post("/parse")
async def parse_document(request: ParseRequest):
    """
    解析文档并存入向量库
    """
    start_time = time.time()
    try:
        # 判断 file_path 是 URL 还是本地路径
        is_url = re.match(r'^https?://', request.file_path) is not None or \
                 re.match(r'^[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+/', request.file_path) is not None
        
        if not is_url and not os.path.exists(request.file_path):
            logger.warning(f"File not found: {request.file_path}")
            raise HTTPException(status_code=404, detail="文件不存在")

        logger.info(f"Parsing document: {request.file_path}")
        # Parse the document
        chunks = parser.parse(request.file_path)
        
        # Add metadata
        for chunk in chunks:
            chunk.metadata["doc_id"] = request.doc_id
            chunk.metadata["source"] = request.file_path

        logger.info(f"Generated {len(chunks)} chunks. Adding to vector store...")
        vector_store.add_documents(chunks)
        
        process_time = time.time() - start_time
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/parse",
                "status_code": 200,
                "process_time": process_time
            })
        )
        return {"status": "success", "chunks_count": len(chunks)}
    except HTTPException as e:
        process_time = time.time() - start_time
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/parse",
                "status_code": e.status_code,
                "process_time": process_time
            })
        )
        raise
    except Exception as e:
        process_time = time.time() - start_time
        logger.error(f"Error parsing document: {str(e)}")
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/parse",
                "status_code": 500,
                "process_time": process_time
            })
        )
        # 不返回具体错误信息，避免泄露内部实现细节
        raise HTTPException(status_code=500, detail="文档解析失败")

@router.post("/ask")
async def ask_question(request: ChatRequest):
    """
    问答接口
    """
    start_time = time.time()
    try:
        logger.info(f"Received question: {request.question}")
        
        # 1. 向量搜索召回相关文档
        # Search for relevant documents
        docs = vector_store.search(request.question, k=3)
        logger.info(f"Found {len(docs)} relevant documents")
        
        # 2. 调用 LLM 生成回答
        # Generate answer using LLM
        answer = llm_service.get_answer(request.question, docs)
        logger.info(f"Generated answer successfully")
        
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
        
        response = {"answer": answer, "sources": sources}
        logger.info(f"Response generated successfully")
        
        process_time = time.time() - start_time
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/ask",
                "status_code": 200,
                "process_time": process_time
            })
        )
        return response
    except HTTPException as e:
        process_time = time.time() - start_time
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/ask",
                "status_code": e.status_code,
                "process_time": process_time
            })
        )
        raise
    except Exception as e:
        process_time = time.time() - start_time
        logger.error(f"Error processing question: {str(e)}")
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/ask",
                "status_code": 500,
                "process_time": process_time
            })
        )
        # 不返回具体错误信息，避免泄露内部实现细节
        raise HTTPException(status_code=500, detail="问答处理失败")

@router.post("/delete")
async def delete_document(request: ParseRequest):
    """
    删除文档的向量索引
    """
    start_time = time.time()
    try:
        if request.doc_id:
            logger.info(f"Deleting document with doc_id: {request.doc_id}")
            vector_store.delete_document(request.doc_id)
            
            process_time = time.time() - start_time
            logger.info(
                json.dumps({
                    "method": "POST",
                    "path": "/api/delete",
                    "status_code": 200,
                    "process_time": process_time
                })
            )
            return {"status": "success", "message": f"Document {request.doc_id} deleted"}
        else:
             logger.warning("doc_id is required")
             raise HTTPException(status_code=400, detail="doc_id is required")
    except HTTPException as e:
        process_time = time.time() - start_time
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/delete",
                "status_code": e.status_code,
                "process_time": process_time
            })
        )
        raise
    except Exception as e:
        process_time = time.time() - start_time
        logger.error(f"Error deleting document: {str(e)}")
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/delete",
                "status_code": 500,
                "process_time": process_time
            })
        )
        # 不返回具体错误信息，避免泄露内部实现细节
        raise HTTPException(status_code=500, detail="文档删除失败")

@router.post("/summary")
async def generate_summary(request: SummaryRequest):
    """
    生成会话标题
    """
    start_time = time.time()
    try:
        title = llm_service.generate_title(request.question)
        logger.info(f"Generated summary: {title}")
        
        process_time = time.time() - start_time
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/summary",
                "status_code": 200,
                "process_time": process_time
            })
        )
        return {"title": title}
    except HTTPException as e:
        process_time = time.time() - start_time
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/summary",
                "status_code": e.status_code,
                "process_time": process_time
            })
        )
        raise
    except Exception as e:
        process_time = time.time() - start_time
        logger.error(f"Error generating summary: {str(e)}")
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/summary",
                "status_code": 500,
                "process_time": process_time
            })
        )
        # 不返回具体错误信息，避免泄露内部实现细节
        raise HTTPException(status_code=500, detail="标题生成失败")
