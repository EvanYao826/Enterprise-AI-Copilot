import os
import shutil
from typing import List
from langchain_community.vectorstores import FAISS, Milvus
from langchain_community.embeddings import DashScopeEmbeddings, HuggingFaceEmbeddings
from langchain_core.documents import Document

class VectorStoreManager:
    def __init__(self, persist_directory="./faiss_index", use_milvus=False):
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
        
        # 直接使用 FAISS 向量存储，不使用 Milvus
        print("Using FAISS vector store")
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
            # 直接使用 FAISS
            self.vector_store = FAISS.from_documents(documents, self.embeddings)
            self.vector_store.save_local(self.persist_directory)
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
        """
        if self.vector_store is None:
            return

        # 只使用 FAISS 删除逻辑
        print(f"Deleting document with doc_id: {doc_id} from FAISS")
        
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
        # 只使用 FAISS 删除逻辑
        if os.path.exists(self.persist_directory):
            shutil.rmtree(self.persist_directory)
        self.vector_store = None
        print("Successfully deleted FAISS collection")
