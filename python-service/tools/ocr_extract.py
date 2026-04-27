from typing import Dict, Any
from tools.base import Tool, ToolSchema, SchemaProperty, ToolMetadata
from core.llm import LLMService
from core.config import config


class OCRExtractTool(Tool):
    """OCR文字提取工具"""
    
    def __init__(self):
        input_schema = ToolSchema(
            properties={
                "image_url": SchemaProperty(
                    type="string",
                    description="图片URL",
                    required=True
                )
            },
            type="object"
        )
        
        output_schema = ToolSchema(
            properties={
                "extracted_text": SchemaProperty(
                    type="string",
                    description="提取的文字",
                    required=True
                ),
                "image_url": SchemaProperty(
                    type="string",
                    description="图片URL",
                    required=True
                )
            },
            type="object"
        )
        
        metadata = ToolMetadata(
            timeout_ms=30000,
            max_retries=2,
            permission="user",
            description="从图片中提取文字"
        )
        
        super().__init__(
            name="ocr_extract",
            description="从图片中提取文字",
            input_schema=input_schema,
            output_schema=output_schema,
            metadata=metadata
        )
        
        # 初始化 LLM 服务（用于 OCR 功能）
        self.llm_service = LLMService()
    
    def execute(self, parameters: Dict[str, Any]) -> Dict[str, Any]:
        """执行OCR文字提取"""
        image_url = parameters.get("image_url")
        
        config.logger.info(f"Extracting text from image: {image_url}")
        
        try:
            # 使用 LLM 服务的 OCR 功能
            extracted_text = self.llm_service.extract_text_from_image(image_url)
            
            config.logger.info(f"OCR extraction completed, extracted {len(extracted_text)} characters")
            
            return {
                "extracted_text": extracted_text,
                "image_url": image_url
            }
            
        except Exception as e:
            config.logger.error(f"OCR extraction failed: {e}")
            return {
                "extracted_text": f"无法从图片中提取文字: {str(e)}",
                "image_url": image_url
            }
