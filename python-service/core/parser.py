import os
import requests
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
                  ('clouddn.com' in file_path or 'aliyuncs.com' in file_path or '/' in file_path))
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
                    
                    response = requests.get(download_url, stream=True, timeout=30)
                    response.raise_for_status()
                    
                    # 推断扩展名，优先从 URL 获取，如果没有则尝试从 Content-Type 或 Content-Disposition 获取
                    # 简单起见，这里假设 URL 包含扩展名
                    ext = os.path.splitext(file_path)[1].lower()
                    if not ext:
                        # 尝试从 Content-Type 推断
                        content_type = response.headers.get('Content-Type', '').lower()
                        if 'pdf' in content_type:
                            ext = '.pdf'
                        elif 'word' in content_type:
                            ext = '.docx'
                        elif 'markdown' in content_type:
                            ext = '.md'
                        else:
                            ext = '.txt'

                    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=ext)
                    for chunk in response.iter_content(chunk_size=8192):
                        temp_file.write(chunk)
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
                try:
                    os.unlink(temp_file.name)
                except Exception:
                    pass

