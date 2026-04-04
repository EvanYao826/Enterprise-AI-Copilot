from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import StreamingResponse
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
    context: str = "" # Optional, if context is passed directly (not used here)
    username: str = None # Optional, if username is provided

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
        is_url = request.file_path.startswith('http://') or request.file_path.startswith('https://')
        
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
        logger.info(f"Received question: {request.question}, username: {request.username}")
        
        # 处理身份相关问题
        lower_question = request.question.lower()
        identity_keywords = ["我是谁", "我叫什么", "我的名字", "我的身份"]
        if any(keyword in lower_question for keyword in identity_keywords) and request.username:
            logger.info(f"Answering identity question for user: {request.username}")
            answer = f"你是 {request.username}，是本系统的注册用户。"
            response = {"answer": answer, "sources": []}
        else:
            # 1. 向量搜索召回相关文档
            # Search for relevant documents
            docs = vector_store.search(request.question, k=3)
            logger.info(f"Found {len(docs)} relevant documents")
            
            # 2. 调用 LLM 生成回答（传入对话上下文）
            # Generate answer using LLM (pass conversation context)
            answer = llm_service.get_answer(request.question, docs, request.context)
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

@router.post("/ask/stream")
async def ask_question_stream(request: ChatRequest):
    """
    流式问答接口 (Server-Sent Events)
    """
    start_time = time.time()

    async def event_generator():
        try:
            logger.info(f"Streaming question: {request.question}, username: {request.username}")

            # 处理身份相关问题
            lower_question = request.question.lower()
            identity_keywords = ["我是谁", "我叫什么", "我的名字", "我的身份"]
            if any(keyword in lower_question for keyword in identity_keywords) and request.username:
                logger.info(f"Streaming identity answer for user: {request.username}")
                answer = f"你是 {request.username}，是本系统的注册用户。"
                # 流式返回身份回答
                for char in answer:
                    yield f"data: {json.dumps({'type': 'token', 'content': char})}\n\n"
                yield f"data: {json.dumps({'type': 'end', 'content': answer})}\n\n"
                return

            # 1. 向量搜索召回相关文档
            docs = vector_store.search(request.question, k=3)
            logger.info(f"Found {len(docs)} relevant documents for streaming")

            # 2. 提取来源信息
            sources = []
            seen_docs = set()

            for doc in docs:
                source = doc.metadata.get("source")
                doc_id = doc.metadata.get("doc_id")

                unique_key = str(doc_id) if doc_id else source
                if unique_key in seen_docs:
                    continue
                seen_docs.add(unique_key)

                source_info = {
                    "source": source,
                    "doc_id": doc_id,
                    "page": doc.metadata.get("page")
                }
                if source_info["source"]:
                    source_info["doc_name"] = os.path.basename(source_info["source"])
                    if source_info["source"].startswith(('http://', 'https://')):
                        source_info["doc_name"] = source_info["source"].split('/')[-1]
                        if '?' in source_info["doc_name"]:
                            source_info["doc_name"] = source_info["doc_name"].split('?')[0]

                sources.append(source_info)

            # 发送来源信息
            yield f"data: {json.dumps({'type': 'sources', 'content': sources})}\n\n"

            # 3. 调用流式 LLM 生成回答
            for chunk in llm_service.get_answer_stream(request.question, docs, request.context):
                yield f"data: {chunk}\n\n"

            process_time = time.time() - start_time
            logger.info(
                json.dumps({
                    "method": "POST",
                    "path": "/api/ask/stream",
                    "status_code": 200,
                    "process_time": process_time
                })
            )

        except Exception as e:
            process_time = time.time() - start_time
            logger.error(f"Error in streaming question: {str(e)}")
            logger.info(
                json.dumps({
                    "method": "POST",
                    "path": "/api/ask/stream",
                    "status_code": 500,
                    "process_time": process_time
                })
            )
            yield f"data: {json.dumps({'type': 'error', 'content': '流式问答处理失败'})}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no"  # 禁用Nginx缓冲
        }
    )

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

@router.get("/vector-store/stats")
async def get_vector_store_stats():
    """
    获取向量库统计信息
    """
    start_time = time.time()
    try:
        stats = vector_store.get_stats()

        process_time = time.time() - start_time
        logger.info(
            json.dumps({
                "method": "GET",
                "path": "/api/vector-store/stats",
                "status_code": 200,
                "process_time": process_time
            })
        )
        return stats
    except Exception as e:
        process_time = time.time() - start_time
        logger.error(f"Error getting vector store stats: {str(e)}")
        logger.info(
            json.dumps({
                "method": "GET",
                "path": "/api/vector-store/stats",
                "status_code": 500,
                "process_time": process_time
            })
        )
        raise HTTPException(status_code=500, detail="获取向量库统计信息失败")

@router.post("/vector-store/migrate")
async def migrate_to_milvus():
    """
    将FAISS数据迁移到Milvus
    """
    start_time = time.time()
    try:
        if not vector_store.use_milvus:
            raise HTTPException(status_code=400, detail="当前未使用Milvus，无法迁移")

        success = vector_store.migrate_faiss_to_milvus()

        process_time = time.time() - start_time
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/vector-store/migrate",
                "status_code": 200,
                "process_time": process_time
            })
        )

        if success:
            return {"status": "success", "message": "数据迁移成功"}
        else:
            return {"status": "partial", "message": "迁移完成但可能有部分数据未迁移"}

    except HTTPException as e:
        process_time = time.time() - start_time
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/vector-store/migrate",
                "status_code": e.status_code,
                "process_time": process_time
            })
        )
        raise
    except Exception as e:
        process_time = time.time() - start_time
        logger.error(f"Error migrating to Milvus: {str(e)}")
        logger.info(
            json.dumps({
                "method": "POST",
                "path": "/api/vector-store/migrate",
                "status_code": 500,
                "process_time": process_time
            })
        )
        raise HTTPException(status_code=500, detail="数据迁移失败")

@router.delete("/vector-store/collection")
async def delete_vector_collection():
    """
    删除整个向量库（慎用）
    """
    start_time = time.time()
    try:
        # 添加确认机制（实际生产环境需要更严格的权限控制）
        vector_store.delete_collection()

        process_time = time.time() - start_time
        logger.info(
            json.dumps({
                "method": "DELETE",
                "path": "/api/vector-store/collection",
                "status_code": 200,
                "process_time": process_time
            })
        )
        return {"status": "success", "message": "向量库已删除"}
    except Exception as e:
        process_time = time.time() - start_time
        logger.error(f"Error deleting vector collection: {str(e)}")
        logger.info(
            json.dumps({
                "method": "DELETE",
                "path": "/api/vector-store/collection",
                "status_code": 500,
                "process_time": process_time
            })
        )
        raise HTTPException(status_code=500, detail="删除向量库失败")
