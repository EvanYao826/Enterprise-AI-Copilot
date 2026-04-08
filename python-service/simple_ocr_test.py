#!/usr/bin/env python3
"""
简单的OCR功能测试
"""

import os
import sys
import tempfile

# 添加当前目录到Python路径
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

try:
    import pytesseract
    from PIL import Image, ImageDraw, ImageFont
    print("✓ 导入依赖成功")
except ImportError as e:
    print(f"✗ 导入依赖失败: {e}")
    print("请安装依赖: pip install pillow pytesseract")
    sys.exit(1)

def setup_tesseract():
    """配置Tesseract"""
    possible_paths = [
        r'E:/Tesseract-OCR/tesseract.exe',
        r'C:/Program Files/Tesseract-OCR/tesseract.exe',
        r'C:/Program Files (x86)/Tesseract-OCR/tesseract.exe',
    ]

    for path in possible_paths:
        if os.path.exists(path):
            pytesseract.pytesseract.tesseract_cmd = path
            print(f"✓ Tesseract路径: {path}")

            # 检查语言包
            tessdata_dir = os.path.join(os.path.dirname(path), 'tessdata')
            if os.path.exists(tessdata_dir):
                print(f"✓ Tessdata目录: {tessdata_dir}")
                lang_files = [f for f in os.listdir(tessdata_dir) if f.endswith('.traineddata')]
                print(f"✓ 可用语言包: {', '.join(lang_files)}")
            return True

    print("✗ 未找到Tesseract，请先安装")
    return False

def create_test_image():
    """创建测试图片"""
    # 创建图片
    img = Image.new('RGB', (500, 300), color='white')
    draw = ImageDraw.Draw(img)

    # 尝试使用字体
    try:
        font = ImageFont.truetype('arial.ttf', 28)
    except:
        font = ImageFont.load_default()
        print("使用默认字体")

    # 添加中英文混合文字
    texts = [
        "OCR功能测试 - OCR Function Test",
        "简体中文: 你好世界",
        "English: Hello World",
        "Java面试题: Spring Boot原理",
        "Python服务: FastAPI + Tesseract"
    ]

    y = 30
    for text in texts:
        draw.text((30, y), text, fill='black', font=font)
        y += 50

    # 保存到临时文件
    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix='.png')
    img.save(temp_file.name)
    print(f"✓ 创建测试图片: {temp_file.name}")

    return temp_file.name

def test_ocr(image_path):
    """测试OCR功能"""
    if not os.path.exists(image_path):
        print(f"✗ 图片不存在: {image_path}")
        return

    print(f"\n=== 测试OCR功能 ===")

    try:
        # 打开并预处理图片
        image = Image.open(image_path)
        print(f"✓ 图片尺寸: {image.size}, 模式: {image.mode}")

        # 转换为灰度图
        if image.mode != 'L':
            image = image.convert('L')
            print("✓ 转换为灰度图")

        # 测试不同语言配置
        test_cases = [
            ('eng', '英文'),
            ('chi_sim', '简体中文'),
            ('chi_sim+eng', '简体中文+英文'),
            ('chi_tra+eng', '繁体中文+英文'),
        ]

        for lang, desc in test_cases:
            print(f"\n--- 测试: {desc} ({lang}) ---")
            try:
                text = pytesseract.image_to_string(
                    image,
                    lang=lang,
                    config='--psm 3 --oem 3'
                ).strip()

                if text:
                    print(f"✓ 识别成功，字符数: {len(text)}")
                    print(f"前100字符: {text[:100]}...")

                    # 保存结果
                    result_file = f"ocr_result_{lang}.txt"
                    with open(result_file, 'w', encoding='utf-8') as f:
                        f.write(text)
                    print(f"✓ 结果保存到: {result_file}")
                else:
                    print("✗ 未识别到文字")

            except Exception as e:
                print(f"✗ {desc} 识别失败: {e}")

    except Exception as e:
        print(f"✗ OCR测试失败: {e}")

def main():
    """主函数"""
    print("=== OCR功能测试开始 ===\n")

    # 1. 配置Tesseract
    if not setup_tesseract():
        return

    # 2. 创建测试图片
    test_image = create_test_image()

    # 3. 测试OCR
    test_ocr(test_image)

    # 4. 清理
    try:
        os.unlink(test_image)
        print(f"\n✓ 清理临时文件: {test_image}")
    except:
        pass

    print("\n=== 测试完成 ===")
    print("\n建议:")
    print("1. 如果中文识别不准确，可能需要更好的图片质量")
    print("2. 可以调整OCR参数 (--psm, --oem)")
    print("3. 对于复杂图片，可能需要图片预处理")

if __name__ == "__main__":
    main()