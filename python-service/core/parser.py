import os
import httpx
import re
import tempfile
from typing import List
from langchain_community.document_loaders import PyPDFLoader, Docx2txtLoader, TextLoader, UnstructuredMarkdownLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_core.documents import Document

class DocumentParser:
    def __init__(self):
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=500,
            chunk_overlap=50
        )

    def parse(self, file_path: str) -> List[Document]:
        """
        根据文件扩展名选择合适的加载器解析文档，并切分文本。
        支持本地路径和 HTTP/HTTPS URL。
        """
        # 检测是否为 URL(支持带或不带协议头)
        is_url = file_path.startswith(('http://', 'https://')) or \
                 (not os.path.exists(file_path) and 
                  re.match(r'^[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+/', file_path))
        temp_file = None

        try:
            target_path = file_path
            
            # 如果是 URL，先下载到临时文件
            if is_url:
                try:
                    # 如果没有协议头，添加 https://
                    download_url = file_path
                    if not download_url.startswith(('http://', 'https://')):
                        download_url = 'https://' + download_url
                    
                    with httpx.Client() as client:
                        response = client.get(download_url)
                        response.raise_for_status()
                        
                        # 推断扩展名
                        ext = os.path.splitext(file_path)[1].lower()
                        if not ext:
                            ext = '.txt' 

                        temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=ext)
                        temp_file.write(response.content)
                        temp_file.close()
                        target_path = temp_file.name
                except Exception as e:
                    raise Exception(f"Failed to download file from URL: {e}")
            else:
                if not os.path.exists(file_path):
                    raise FileNotFoundError(f"File not found: {file_path}")

            ext = os.path.splitext(target_path)[1].lower()
            
            if ext == '.pdf':
                loader = PyPDFLoader(target_path)
            elif ext == '.docx':
                loader = Docx2txtLoader(target_path)
            elif ext == '.txt':
                loader = TextLoader(target_path, encoding='utf-8')
            elif ext == '.md':
                loader = TextLoader(target_path, encoding='utf-8')
            else:
                raise ValueError(f"Unsupported file type: {ext}")

            documents = loader.load()
            chunks = self.text_splitter.split_documents(documents)
            return chunks

        finally:
            # 清理临时文件
            if temp_file and os.path.exists(temp_file.name):
                os.unlink(temp_file.name)

