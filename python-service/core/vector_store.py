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

    def delete_collection(self):
        """
        删除整个向量库 (慎用)
        """
        if os.path.exists(self.persist_directory):
            shutil.rmtree(self.persist_directory)
        self.vector_store = None
