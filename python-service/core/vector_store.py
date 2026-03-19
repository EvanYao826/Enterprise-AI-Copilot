import os
import shutil
from typing import List
from langchain_community.vectorstores import FAISS, Milvus
from langchain_community.embeddings import DashScopeEmbeddings
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_core.documents import Document

class VectorStoreManager:
    def __init__(self, persist_directory="./faiss_index", use_milvus=True):
        self.persist_directory = persist_directory
        self.use_milvus = use_milvus
        
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
        
        if self.use_milvus:
            print("Using Milvus vector store")
            # 初始化 Milvus 向量存储
            # 默认连接到本地 Milvus 实例 (localhost:19530)
            # 生产环境建议配置 Milvus 服务器地址
            try:
                self.vector_store = Milvus(
                    embedding_function=self.embeddings,
                    connection_args={"host": "localhost", "port": "19530"},
                    collection_name="ai_knowledge_system",
                    drop_old=True  # 开发环境可以设置为 True，生产环境建议设置为 False
                )
            except Exception as e:
                print(f"Error initializing Milvus: {e}")
                print("Falling back to FAISS vector store")
                self.use_milvus = False
                self._init_faiss()
        else:
            self._init_faiss()

    def _init_faiss(self):
        """
        初始化 FAISS 向量存储（作为 Milvus 的 fallback）
        """
        print("Using FAISS vector store")
        if os.path.exists(self.persist_directory):
            try:
                self.vector_store = FAISS.load_local(self.persist_directory, self.embeddings, allow_dangerous_deserialization=True)
            except Exception as e:
                print(f"Error loading existing index (likely due to embedding model mismatch): {e}")
                print("Re-initializing empty vector store...")
                # Backup old index just in case
                if os.path.exists(self.persist_directory + "_backup"):
                    shutil.rmtree(persist_directory + "_backup")
                shutil.move(self.persist_directory, self.persist_directory + "_backup")
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
            if self.use_milvus:
                self.vector_store = Milvus(
                    embedding_function=self.embeddings,
                    connection_args={"host": "localhost", "port": "19530"},
                    collection_name="ai_knowledge_system",
                    drop_old=True
                )
                self.vector_store.add_documents(documents)
            else:
                self.vector_store = FAISS.from_documents(documents, self.embeddings)
                self.vector_store.save_local(self.persist_directory)
        else:
            self.vector_store.add_documents(documents)
            if not self.use_milvus:
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
        Milvus 支持按元数据过滤删除，效率更高
        """
        if self.vector_store is None:
            return

        if self.use_milvus:
            try:
                # Milvus 支持按元数据过滤删除
                # 构建过滤条件
                filter_expr = f"doc_id == {doc_id}"
                # 执行删除
                self.vector_store.delete(filter=filter_expr)
                print(f"Successfully deleted document with doc_id: {doc_id} from Milvus")
            except Exception as e:
                print(f"Failed to delete document from Milvus: {e}")
        else:
            # FAISS 删除逻辑（保持原有实现）
            print(f"Warning: FAISS implementation does not support efficient deletion by doc_id: {doc_id}")
            print("To fully support deletion, consider using Milvus.")
            
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
        if self.use_milvus:
            try:
                # Milvus 删除集合
                from pymilvus import connections, utility
                connections.connect("default", host="localhost", port="19530")
                utility.drop_collection("ai_knowledge_system")
                print("Successfully deleted Milvus collection")
            except Exception as e:
                print(f"Failed to delete Milvus collection: {e}")
            self.vector_store = None
        else:
            if os.path.exists(self.persist_directory):
                shutil.rmtree(self.persist_directory)
            self.vector_store = None
