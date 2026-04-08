import os
import requests
import tempfile
import logging
from typing import List
from langchain_community.document_loaders import PyPDFLoader, Docx2txtLoader, TextLoader, UnstructuredMarkdownLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_core.documents import Document
from PIL import Image
import pytesseract

# 配置日志
logger = logging.getLogger(__name__)

# 动态配置Tesseract路径
def setup_tesseract():
    """动态配置Tesseract路径，支持多种安装位置"""
    possible_paths = [
        r'E:/Tesseract-OCR/tesseract.exe',  # 当前配置
        r'C:/Program Files/Tesseract-OCR/tesseract.exe',  # 默认安装路径
        r'C:/Program Files (x86)/Tesseract-OCR/tesseract.exe',  # 32位安装路径
        '/usr/bin/tesseract',  # Linux/Mac
        '/usr/local/bin/tesseract',  # Linux/Mac alternative
    ]

    for path in possible_paths:
        if os.path.exists(path):
            pytesseract.pytesseract.tesseract_cmd = path
            logger.info(f"Tesseract found at: {path}")

            # 检查语言包
            tessdata_dir = os.path.join(os.path.dirname(path), 'tessdata')
            if os.path.exists(tessdata_dir):
                logger.info(f"Tessdata directory: {tessdata_dir}")
                # 检查中文语言包
                chi_sim_path = os.path.join(tessdata_dir, 'chi_sim.traineddata')
                eng_path = os.path.join(tessdata_dir, 'eng.traineddata')

                if os.path.exists(chi_sim_path):
                    logger.info("Chinese language pack found: chi_sim.traineddata")
                else:
                    logger.warning("Chinese language pack (chi_sim.traineddata) not found!")

                if os.path.exists(eng_path):
                    logger.info("English language pack found: eng.traineddata")
                else:
                    logger.warning("English language pack (eng.traineddata) not found!")
            return

    logger.error("Tesseract not found in any known location!")
    # 如果找不到，尝试使用系统PATH
    try:
        pytesseract.get_tesseract_version()
        logger.info("Tesseract found in system PATH")
    except Exception as e:
        logger.error(f"Tesseract not found: {e}")
        raise RuntimeError("Tesseract OCR not installed. Please install Tesseract and language packs.")

# 初始化Tesseract
setup_tesseract()

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
                        elif 'image' in content_type:
                            # 图片类型
                            if 'png' in content_type:
                                ext = '.png'
                            elif 'jpeg' in content_type or 'jpg' in content_type:
                                ext = '.jpg'
                            elif 'gif' in content_type:
                                ext = '.gif'
                            elif 'bmp' in content_type:
                                ext = '.bmp'
                            else:
                                ext = '.png'  # 默认使用png
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
                documents = loader.load()
            elif ext == '.docx':
                loader = Docx2txtLoader(target_path)
                documents = loader.load()
            elif ext == '.txt':
                loader = TextLoader(target_path, encoding='utf-8')
                documents = loader.load()
            elif ext == '.md':
                loader = TextLoader(target_path, encoding='utf-8')
                documents = loader.load()
            elif ext in ['.png', '.jpg', '.jpeg', '.gif', '.bmp', '.tiff', '.tif']:
                # 处理图片文件，使用OCR
                try:
                    logger.info(f"Processing image file: {target_path}")
                    image = Image.open(target_path)

                    # 优化图片预处理
                    # 1. 转换为灰度图（提高OCR准确率）
                    if image.mode != 'L':
                        image = image.convert('L')

                    # 2. 调整图片大小（如果太大）
                    max_size = 2000
                    if max(image.size) > max_size:
                        ratio = max_size / max(image.size)
                        new_size = tuple(int(dim * ratio) for dim in image.size)
                        image = image.resize(new_size, Image.Resampling.LANCZOS)
                        logger.info(f"Resized image from {image.size} to {new_size}")

                    # 3. 尝试多种语言配置
                    ocr_text = ""
                    ocr_errors = []

                    # 尝试组合语言包
                    lang_configs = [
                        'chi_sim+eng',  # 简体中文+英文
                        'chi_sim',      # 仅简体中文
                        'eng',          # 仅英文
                        'chi_tra+eng',  # 繁体中文+英文
                    ]

                    for lang in lang_configs:
                        try:
                            logger.info(f"Trying OCR with language: {lang}")
                            text = pytesseract.image_to_string(
                                image,
                                lang=lang,
                                config='--psm 3 --oem 3'  # 自动页面分割，LSTM OCR引擎
                            )

                            if text and text.strip():
                                ocr_text = text.strip()
                                logger.info(f"OCR successful with language {lang}, text length: {len(ocr_text)}")
                                # 预览前100个字符
                                preview = ocr_text[:100].replace('\n', ' ')
                                logger.info(f"OCR preview: {preview}...")
                                break
                            else:
                                logger.warning(f"No text detected with language: {lang}")
                        except Exception as lang_error:
                            error_msg = f"Language {lang} failed: {str(lang_error)}"
                            ocr_errors.append(error_msg)
                            logger.warning(error_msg)

                    # 如果所有语言都失败，尝试默认语言
                    if not ocr_text:
                        try:
                            logger.info("Trying OCR with default settings")
                            ocr_text = pytesseract.image_to_string(image).strip()
                        except Exception as default_error:
                            logger.error(f"Default OCR failed: {default_error}")

                    # 最终检查
                    if not ocr_text or not ocr_text.strip():
                        ocr_text = "图片中未识别到文字"
                        logger.warning("No text detected in image")
                    else:
                        logger.info(f"OCR completed successfully. Text length: {len(ocr_text)}")

                    # 创建文档对象
                    documents = [Document(
                        page_content=ocr_text,
                        metadata={
                            "source": target_path,
                            "page": 1,
                            "file_type": "image",
                            "ocr_errors": ocr_errors if ocr_errors else None
                        }
                    )]
                except Exception as e:
                    logger.error(f"Failed to OCR image {target_path}: {e}")
                    # 返回一个包含错误信息的文档，而不是抛出异常
                    documents = [Document(
                        page_content=f"图片OCR处理失败: {str(e)}",
                        metadata={
                            "source": target_path,
                            "page": 1,
                            "file_type": "image",
                            "error": str(e)
                        }
                    )]
            else:
                raise ValueError(f"Unsupported file type: {ext}")
            chunks = self.text_splitter.split_documents(documents)
            return chunks

        finally:
            # 清理临时文件
            if temp_file and os.path.exists(temp_file.name):
                try:
                    os.unlink(temp_file.name)
                except Exception:
                    pass

