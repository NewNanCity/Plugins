#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
搜索modules目录中所有.kt文件中的模板模式
模板模式: <%([a-zA-Z0-9_.]+)%>
"""

import os
import re
from pathlib import Path

def search_templates_in_kt_files():
    """
    在modules目录中搜索所有.kt文件，查找符合模板模式的内容
    """
    # 定义模板模式
    template_pattern = re.compile(r"<%([a-zA-Z0-9_.]+)%>")

    # 获取当前脚本所在目录
    current_dir = Path(__file__).parent
    modules_dir = current_dir / "modules"

    # 检查modules目录是否存在
    if not modules_dir.exists():
        print(f"错误: modules目录不存在: {modules_dir}")
        return

    # 搜索所有.kt文件
    kt_files = []
    for root, dirs, files in os.walk(modules_dir):
        for file in files:
            if file.endswith('.kt'):
                kt_files.append(os.path.join(root, file))

    print(f"找到 {len(kt_files)} 个.kt文件")
    print("=" * 60)

    # 存储结果
    found_files = []
    total_templates = 0

    # 遍历每个.kt文件
    for kt_file in kt_files:
        try:
            with open(kt_file, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()

            # 查找所有匹配的模板
            matches = template_pattern.findall(content)

            if matches:
                # 计算相对路径（相对于modules目录）
                relative_path = os.path.relpath(kt_file, modules_dir)

                # 去除重复的key
                unique_keys = list(set(matches))
                unique_keys.sort()

                found_files.append({
                    'file': relative_path,
                    'full_path': kt_file,
                    'keys': unique_keys,
                    'total_count': len(matches)
                })

                total_templates += len(matches)

        except Exception as e:
            print(f"读取文件时出错 {kt_file}: {e}")

    # 打印结果
    if found_files:
        print(f"找到 {len(found_files)} 个包含模板的文件，共 {total_templates} 个模板实例:\n")

        for i, file_info in enumerate(found_files, 1):
            print(f"{i}. 文件: {file_info['file']}")
            print(f"   完整路径: {file_info['full_path']}")
            print(f"   模板数量: {file_info['total_count']}")
            print(f"   包含的Key:")
            for key in file_info['keys']:
                print(f"     - {key}")
            print()
    else:
        print("未找到任何包含模板模式的.kt文件")

    # 打印统计信息
    print("=" * 60)
    print("统计信息:")
    print(f"总共搜索的.kt文件数: {len(kt_files)}")
    print(f"包含模板的文件数: {len(found_files)}")
    print(f"模板实例总数: {total_templates}")

    if found_files:
        # 统计所有唯一的key
        all_keys = set()
        for file_info in found_files:
            all_keys.update(file_info['keys'])

        print(f"唯一模板Key总数: {len(all_keys)}")
        print("\n所有唯一的模板Key:")
        for key in sorted(all_keys):
            print(f"  - {key}")

def main():
    """主函数"""
    print("开始搜索modules目录中.kt文件的模板模式...")
    print("模板模式: <%([a-zA-Z0-9_.]+)%>")
    print()

    search_templates_in_kt_files()

if __name__ == "__main__":
    main()
