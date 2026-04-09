#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
快速测试修复效果
"""

import http.client
import json
import time

def test_single_question(question):
    """测试单个问题"""
    print(f"\n{'='*60}")
    print(f"测试问题: {question}")
    print(f"{'='*60}")

    conn = http.client.HTTPConnection("localhost", 8000)

    payload = json.dumps({
        "question": question
    })

    headers = {
        'Content-Type': 'application/json'
    }

    try:
        conn.request("POST", "/api/ask", payload, headers)
        res = conn.getresponse()
        data = res.read()

        if res.status == 200:
            response_json = json.loads(data.decode('utf-8'))
            print(f"状态: {res.status}")
            print(f"回答: {response_json.get('answer', '')[:100]}...")
            print(f"返回的文档引用数量: {len(response_json.get('sources', []))}")

            if response_json.get('sources'):
                print("文档引用:")
                for i, source in enumerate(response_json.get('sources', [])):
                    print(f"  {i+1}. {source.get('doc_name', '未知')}")
            else:
                print("✅ 正确：没有返回文档引用")
        else:
            print(f"错误状态: {res.status}")
            print(f"响应: {data.decode('utf-8')}")

    except Exception as e:
        print(f"请求失败: {e}")
    finally:
        conn.close()

    time.sleep(1)  # 避免请求过快

def main():
    """主测试函数"""
    print("测试RAG系统修复效果")
    print("="*60)

    # 测试生活类问题（不应该返回文档引用）
    test_questions_life = [
        "你知道山东吗",
        "今天天气怎么样",
        "有什么好吃的？",
        "讲个笑话吧",
        "我帅吗？",
        "最近有什么好看的电影？",
        "你好，最近怎么样？",
        "谢谢你的帮助！",
    ]

    print("\n测试生活类问题（不应该返回文档引用）:")
    for question in test_questions_life:
        test_single_question(question)

    # 测试技术类问题（应该返回文档引用）
    test_questions_tech = [
        "什么是一级封锁协议？",
        "什么是二级封锁协议",
        "数据库是什么？",
        "什么是人工智能？",
        "如何学习Python编程？",
        "帮我写一段代码",
        "数据库索引的原理是什么？",
        "Spring Boot框架有什么特点？",
        "什么是RESTful API？",
    ]

    print("\n\n测试技术类问题（应该返回文档引用）:")
    for question in test_questions_tech:
        test_single_question(question)

if __name__ == "__main__":
    main()