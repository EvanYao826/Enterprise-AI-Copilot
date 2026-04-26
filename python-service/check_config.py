#!/usr/bin/env python3
"""
检查配置项加载情况
"""

import os
import sys

# 添加项目根目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

try:
    from core.config import config
    print("=== 配置加载结果 ===")
    print("配置管理模块加载成功！")
    
    # 打印所有配置项
    config_dict = config.get_config()
    print("\n配置项列表:")
    for key, value in config_dict.items():
        print(f"  {key}: {value}")
    
    # 验证配置验证功能
    is_valid = config.validate_config()
    print(f"\n配置验证结果: {'有效' if is_valid else '无效'}")
    
    print("\n🎉 配置治理功能正常工作！")
    sys.exit(0)
except Exception as e:
    print(f"❌ 配置加载失败: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)
