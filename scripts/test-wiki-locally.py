#!/usr/bin/env python3
"""
本地测试 Wiki 文档生成
用于在提交前验证 Wiki 文档的生成和链接
"""

import os
import sys
import subprocess
from pathlib import Path


def run_command(cmd, cwd=None):
    """运行命令并返回结果"""
    try:
        # 设置环境变量
        env = os.environ.copy()
        env["PYTHONIOENCODING"] = "utf-8"

        result = subprocess.run(
            cmd,
            shell=True,
            cwd=cwd,
            capture_output=True,
            text=True,
            check=True,
            env=env,
            encoding="utf-8",
            errors="ignore",
        )
        return result.stdout.strip() if result.stdout else ""
    except subprocess.CalledProcessError as e:
        print(f"❌ 命令执行失败: {cmd}")
        stderr_text = e.stderr if e.stderr else "未知错误"
        print(f"错误: {stderr_text}")
        return None
    except Exception as e:
        print(f"❌ 执行异常: {cmd}")
        print(f"异常: {str(e)}")
        return None


def test_wiki_generation():
    """测试 Wiki 文档生成"""

    print("🧪 开始本地测试 Wiki 文档生成...")

    # 检查必要文件
    required_files = ["scripts/prepare-wiki-multi.py", "docs/gui/README.md"]

    for file in required_files:
        if not Path(file).exists():
            print(f"❌ 缺少必要文件: {file}")
            return False

    # 运行 Wiki 准备脚本
    print("\n📝 运行 Wiki 准备脚本...")
    result = run_command("python scripts/prepare-wiki-multi.py")

    if result is None:
        print("❌ Wiki 准备脚本执行失败")
        return False

    # 检查生成的文件
    wiki_dir = Path("wiki")
    if not wiki_dir.exists():
        print("❌ Wiki 目录未生成")
        return False

    expected_files = [
        "Home.md",
        "_Sidebar.md",
        "GUI模块.md",
        "GUI模块-介绍.md",
        "GUI模块-快速开始.md",
        "GUI模块-API参考.md",
    ]

    print("\n📋 检查生成的文件...")
    missing_files = []
    for file in expected_files:
        file_path = wiki_dir / file
        if file_path.exists():
            print(f"✅ {file}")
        else:
            print(f"❌ {file}")
            missing_files.append(file)

    if missing_files:
        print(f"\n❌ 缺少 {len(missing_files)} 个文件")
        return False

    # 检查文件内容
    print("\n🔍 检查文件内容...")
    home_file = wiki_dir / "Home.md"
    with open(home_file, "r", encoding="utf-8") as f:
        content = f.read()

    # 检查是否包含预期的链接格式
    if "[GUI模块](GUI模块)" in content or "GUI模块-快速开始" in content:
        print("✅ Wiki 链接格式正确")
    else:
        print("❌ Wiki 链接格式不正确")
        return False

    # 统计生成的文件
    md_files = list(wiki_dir.glob("*.md"))
    print(f"\n📊 生成统计:")
    print(f"  - 总文件数: {len(md_files)}")
    print(f"  - 总大小: {sum(f.stat().st_size for f in md_files) / 1024:.1f} KB")

    print("\n✅ 本地测试通过!")
    return True


def preview_wiki():
    """预览 Wiki 文档"""

    wiki_dir = Path("wiki")
    if not wiki_dir.exists():
        print("❌ Wiki 目录不存在，请先运行测试")
        return

    print("📖 Wiki 文档预览:")
    print("=" * 50)

    # 显示 Home.md 的前几行
    home_file = wiki_dir / "Home.md"
    if home_file.exists():
        with open(home_file, "r", encoding="utf-8") as f:
            lines = f.readlines()

        print("🏠 Home.md 预览:")
        for i, line in enumerate(lines[:15]):
            print(f"  {i + 1:2d}: {line.rstrip()}")

        if len(lines) > 15:
            print(f"  ... (还有 {len(lines) - 15} 行)")

    print("\n📁 所有文件:")
    for file in sorted(wiki_dir.glob("*.md")):
        size = file.stat().st_size
        print(f"  - {file.name:<25} ({size:>6} bytes)")


def clean_wiki():
    """清理生成的 Wiki 文件"""

    wiki_dir = Path("wiki")
    if wiki_dir.exists():
        import shutil

        shutil.rmtree(wiki_dir)
        print("🧹 已清理 Wiki 目录")
    else:
        print("ℹ️  Wiki 目录不存在，无需清理")


def main():
    """主函数"""

    if len(sys.argv) > 1:
        command = sys.argv[1]

        if command == "test":
            success = test_wiki_generation()
            sys.exit(0 if success else 1)
        elif command == "preview":
            preview_wiki()
        elif command == "clean":
            clean_wiki()
        else:
            print(f"❌ 未知命令: {command}")
            print("可用命令: test, preview, clean")
            sys.exit(1)
    else:
        # 默认运行测试
        success = test_wiki_generation()
        if success:
            preview_wiki()
        sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
