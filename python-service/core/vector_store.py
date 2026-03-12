import os
import shutil
from typing import List
from langchain_community.vectorstores import FAISS
from langchain_community.embeddings import DashScopeEmbeddings
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_core.documents import Document

class VectorStoreManager:
    def __init__(self, persist_directory="./faiss_index"):
        self.persist_directory = persist_directory
        
        # 优先使用阿里云 DashScope Embeddings (text-embedding-v1)
        api_key = os.getenv("DASHSCOPE_API_KEY")
        if api_key:
            print("Using DashScope Embeddings (text-embedding-v1)")
            self.embeddings = DashScopeEmbeddings(
                model="text-embedding-v1",
                dashscope_api_key=api_key
            )
        else:
            print("Warning: DASHSCOPE_API_KEY not found. Falling back to local HuggingFace Embeddings.")
            # Fallback to local model
            self.embeddings = HuggingFaceEmbeddings(model_name="sentence-transformers/all-MiniLM-L6-v2")
        
        if os.path.exists(persist_directory):
            try:
                self.vector_store = FAISS.load_local(persist_directory, self.embeddings, allow_dangerous_deserialization=True)
            except Exception as e:
                print(f"Error loading existing index (likely due to embedding model mismatch): {e}")
                print("Re-initializing empty vector store...")
                # Backup old index just in case
                if os.path.exists(persist_directory + "_backup"):
                    shutil.rmtree(persist_directory + "_backup")
                shutil.move(persist_directory, persist_directory + "_backup")
                self.vector_store = None
        else:
            self.vector_store = None

    def add_documents(self, documents: List[Document]):
        """
        添加文档到向量数据库
        """
        if not documents:
            return

        if self.vector_store is None:
            self.vector_store = FAISS.from_documents(documents, self.embeddings)
        else:
            self.vector_store.add_documents(documents)
        
        self.vector_store.save_local(self.persist_directory)

    def search(self, query: str, k: int = 3) -> List[Document]:
        """
        相似度搜索
        """
        if self.vector_store is None:
            return []
        
        return self.vector_store.similarity_search(query, k=k)

    def delete_document(self, doc_id: int):
        """
        根据 doc_id 删除文档向量
        注意：FAISS 不支持直接删除，通常需要重建索引。
        这里使用一种变通方法：加载所有文档，过滤掉要删除的，然后重新构建索引。
        这在数据量大时效率较低，但对于演示项目足够。
        """
        if self.vector_store is None:
            return

        # 这是一个简化的实现，实际上 FAISS 删除比较麻烦
        # 如果使用的是内存中的 FAISS，可以通过重建来实现
        # 1. 获取所有文档 (FAISS 不直接支持遍历所有文档，这是一个限制)
        # 更好的做法是切换到 Chroma 或 Milvus 等支持删除的向量库
        
        # 针对 FAISS 的一种折中方案：
        # 我们假设无法直接删除，只能在搜索时过滤 (filter)，或者清空重建。
        # 为了真正删除，我们需要维护一个 doc_store 或者使用支持删除的 VectorStore
        
        # 这里为了演示，我们尝试重建索引（仅当 doc_store 可用时，但 FAISS 默认不持久化 doc_store）
        # 由于 FAISS 的局限性，我们暂时打印日志，提示需要手动重建或切换向量库
        print(f"Warning: FAISS implementation does not support efficient deletion by doc_id: {doc_id}")
        print("To fully support deletion, consider using Chroma, Pinecone, or Milvus.")
        
        # 尝试通过 index_to_docstore_id 删除（如果可能）
        try:
            # 找到所有 metadata['doc_id'] == doc_id 的 ID
            ids_to_delete = []
            for doc_uuid, doc in self.vector_store.docstore._dict.items():
                if doc.metadata.get('doc_id') == doc_id:
                    ids_to_delete.append(doc_uuid)
            
            if ids_to_delete:
                self.vector_store.delete(ids_to_delete)
                self.vector_store.save_local(self.persist_directory)
                print(f"Deleted {len(ids_to_delete)} chunks for doc_id {doc_id}")
            else:
                print(f"No chunks found for doc_id {doc_id}")
                
        except Exception as e:
            print(f"Failed to delete document from FAISS: {e}")

    def delete_collection(self):
        """
        删除整个向量库 (慎用)
        """
        if os.path.exists(self.persist_directory):
            shutil.rmtree(self.persist_directory)
        self.vector_store = None
