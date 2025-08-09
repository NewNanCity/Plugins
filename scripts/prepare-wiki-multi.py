#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
GitHub Wiki 文档准备脚本 (多模块版本)
将 docs/ 目录下的多模块文档转换为 GitHub Wiki 格式
动态检测所有模块并自动生成Wiki文档
"""

import os
import re
import shutil
import sys
from pathlib import Path
from datetime import datetime

# 设置输出编码
if sys.platform.startswith("win"):
    os.environ["PYTHONIOENCODING"] = "utf-8"

# 模块图标映射
MODULE_ICONS = {
    "gui": "🖱️",
    "core": "⚙️",
    "config": "⚙️",
    "database": "🗄️",
    "i18n": "🌐",
    "network": "🌐",
    "plugins": "🔌",
    "troubleshooting": "🔧",
}

# 文件名映射 (英文/大写 -> 中文描述)
FILE_NAME_MAPPING = {
    # 通用
    "README.md": "",  # 将作为模块主页
    "intro.md": "介绍",
    "INTRO.md": "介绍",
    "quick-start.md": "快速开始",
    "GETTING_STARTED.md": "快速开始",
    "concepts.md": "基础概念",
    "CONCEPTS.md": "基础概念",
    "NAVIGATION.md": "文档导航",
    "CHANGELOG.md": "更新日志",
    "IMPROVEMENTS.md": "改进",
    "REORGANIZATION_SUMMARY.md": "重组总结",
    # GUI 专用历史映射
    "basic-gui.md": "基础GUI",
    "paginated-gui.md": "分页GUI",
    "scrolling-gui.md": "滚动GUI",
    "storage-gui.md": "存储GUI",
    "session-management.md": "会话管理",
    "task-system.md": "任务系统",
    "layout-schemes.md": "布局方案",
    "event-handling.md": "事件处理",
    "chat-input.md": "聊天输入",
    # 高级/参考
    "lifecycle.md": "生命周期管理",
    "architecture.md": "架构设计",
    "configuration.md": "配置和扩展",
    "api-reference.md": "API参考",
    "best-practices.md": "最佳实践",
    "troubleshooting.md": "故障排除",
    "examples.md": "示例代码",
    "version-compatibility.md": "版本兼容性",
    "i18n-lifecycle-best-practices.md": "国际化生命周期最佳实践",
    "scheduler-lifecycle-best-practices.md": "调度器生命周期最佳实践",
}

# 子目录文件名映射（按目录分类）
SUBDIR_FILE_NAME_MAPPING = {
    "api": {
        "README.md": "API总览",
        "pages.md": "页面API",
        "components.md": "组件API",
        "sessions.md": "会话API",
        "events.md": "事件API",
        "items.md": "物品API",
    },
    "guides": {
        "README.md": "开发指南",
        "best-practices.md": "最佳实践",
        "performance.md": "性能优化",
        "error-handling.md": "错误处理",
        "troubleshooting.md": "故障排除",
    },
    "tutorials": {
        "README.md": "教程索引",
        "01-first-gui.md": "教程-第一个GUI",
        "02-components.md": "教程-组件使用",
        "03-events.md": "教程-事件处理",
        "04-sessions.md": "教程-会话管理",
        "05-i18n-integration.md": "教程-国际化集成",
        "06-advanced-features.md": "教程-高级功能",
        "07-infinite-scrolling.md": "教程-无限滚动",
    },
    "examples": {
        "README.md": "示例索引",
        "basic/enhanced-items-demo.md": "示例-基础-增强物品展示",
        "basic/border-components.md": "示例-基础-边框组件",
        "basic/skull-items.md": "示例-基础-头颅物品",
        "advanced/event-handling-examples.md": "示例-高级-事件处理示例",
        "advanced/feature-based-events.md": "示例-高级-特性化事件",
        "advanced/component-specific-events.md": "示例-高级-组件特定事件",
        "real-world/tpa-plugin-example.md": "示例-实战-TPA插件",
    },
}


def discover_modules():
    """动态发现docs目录下的所有模块"""
    docs_dir = Path("docs")
    if not docs_dir.exists():
        print(f"❌ docs 目录不存在: {docs_dir.absolute()}")
        return {}

    modules = {}

    # 遍历docs目录下的所有子目录
    for module_dir in docs_dir.iterdir():
        if not module_dir.is_dir():
            continue

        module_id = module_dir.name

        # 跳过隐藏目录和特殊目录
        if module_id.startswith(".") or module_id in ["__pycache__"]:
            continue

        print(f"🔍 发现模块: {module_id}")

        # 获取模块中的所有Markdown文件
        md_files = list(module_dir.glob("*.md"))
        if not md_files:
            print(f"  ⚠️ 模块 {module_id} 中没有找到Markdown文件，跳过")
            continue

        # 生成模块配置
        module_config = generate_module_config(module_id, md_files)
        modules[module_id] = module_config

        print(f"  ✅ 发现 {len(md_files)} 个文档文件")

    return modules


def generate_module_config(module_id, md_files):
    """为模块生成配置（包含已知子目录）"""
    # 模块名称
    module_name = f"{module_id.upper()}模块"

    # 模块图标
    icon = MODULE_ICONS.get(module_id, "📄")

    # 模块描述 (尝试从README.md中提取)
    description = get_module_description(module_id, md_files)

    # 生成文件映射
    files: dict[str, str] = {}
    links: dict[str, str] = {}

    # 顶层 .md 文件
    for md_file in md_files:
        file_name = md_file.name

        # 生成Wiki页面名称
        if file_name == "README.md":
            wiki_name = f"{module_name}.md"
            link_name = module_name
        else:
            # 获取中文描述
            chinese_name = FILE_NAME_MAPPING.get(
                file_name, file_name.replace(".md", "").replace("-", " ").title()
            )
            wiki_name = f"{module_name}-{chinese_name}.md"
            link_name = f"{module_name}-{chinese_name}"

        files[file_name] = wiki_name
        links[file_name] = link_name

    # 已知子目录（如 api）
    module_dir = Path("docs") / module_id
    for subdir, sub_mapping in SUBDIR_FILE_NAME_MAPPING.items():
        sd = module_dir / subdir
        if not sd.exists() or not sd.is_dir():
            continue
        for md_path in sd.glob("*.md"):
            rel_key = f"{subdir}/{md_path.name}"  # 用于链接替换
            title_key = md_path.name
            chinese_name = sub_mapping.get(
                title_key, title_key.replace(".md", "").replace("-", " ").title()
            )
            wiki_name = f"{module_name}-{chinese_name}.md"
            link_name = wiki_name.replace(".md", "")

            files[rel_key] = wiki_name
            links[rel_key] = link_name

    return {
        "name": module_name,
        "description": description,
        "icon": icon,
        "files": files,
        "links": links,
    }


def get_module_description(module_id, md_files):
    """尝试从README.md中提取模块描述"""
    readme_file = None
    for md_file in md_files:
        if md_file.name == "README.md":
            readme_file = md_file
            break

    if readme_file:
        try:
            with open(readme_file, "r", encoding="utf-8") as f:
                content = f.read()
                # 尝试提取第一行非标题的内容作为描述
                lines = content.split("\n")
                for line in lines:
                    line = line.strip()
                    if line and not line.startswith("#") and not line.startswith("---"):
                        return line[:50] + "..." if len(line) > 50 else line
        except Exception as e:
            print(f"  ⚠️ 读取 {readme_file} 失败: {e}")

    # 默认描述
    return f"{module_id.title()} 模块文档"


def update_links_in_content(content, module_config):
    """更新文档内容中的链接"""
    link_mapping = module_config.get("links", {})

    # 更新 Markdown 链接 [text](link)
    def replace_link(match):
        text = match.group(1)
        link = match.group(2)

        # 跳过外部链接
        if link.startswith("http"):
            return match.group(0)

        # 如果是相对链接且在映射表中，则替换
        if link in link_mapping:
            return f"[{text}]({link_mapping[link]})"

        return match.group(0)

    # 替换链接
    content = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", replace_link, content)

    # 移除 "下一步" 链接中的箭头和格式
    content = re.sub(
        r"---\n\n\*\*下一步\*\* → \[([^\]]+)\]\(([^)]+)\)",
        lambda m: f"---\n\n**下一步** → [{m.group(1)}]({link_mapping.get(m.group(2), m.group(2))})",
        content,
    )

    # 移除 "返回目录" 链接
    content = re.sub(r"---\n\n\*\*返回目录\*\* → \[📚 README\]\([^)]+\)", "", content)

    return content


def create_home_page(modules):
    """创建主页"""
    """创建主页"""

    content = """# 📚 项目文档

欢迎使用项目文档！本文档包含了所有模块的完整使用指南。

## 🎯 模块导航

"""

    for module_id, module_config in modules.items():
        icon = module_config.get("icon", "📦")
        name = module_config.get("name", module_id)
        description = module_config.get("description", "")

        # 获取模块主页链接
        files = module_config.get("files", {})
        main_page = files.get("README.md", f"{name}.md")
        main_page_name = main_page.replace(".md", "")

        content += f"### {icon} [{name}]({main_page_name})\n\n"
        content += f"{description}\n\n"

        # 添加快速链接
        if "quick-start.md" in files:
            quick_start_name = files["quick-start.md"].replace(".md", "")
            content += f"- [🚀 快速开始]({quick_start_name})\n"

        if "api-reference.md" in files:
            api_ref_name = files["api-reference.md"].replace(".md", "")
            content += f"- [📋 API参考]({api_ref_name})\n"

        if "examples.md" in files:
            examples_name = files["examples.md"].replace(".md", "")
            content += f"- [📝 示例代码]({examples_name})\n"

        content += "\n"

    content += (
        """## 📖 如何使用

1. **选择模块** - 点击上方的模块链接进入对应文档
2. **快速开始** - 每个模块都有快速开始指南
3. **深入学习** - 查看详细的功能指南和API参考
4. **解决问题** - 参考故障排除和最佳实践

## 🔗 相关链接

- [项目源码](https://github.com/YOUR_REPO)
- [问题反馈](https://github.com/YOUR_REPO/issues)
- [贡献指南](https://github.com/YOUR_REPO/blob/main/CONTRIBUTING.md)

---

📝 **文档更新**: 此文档由 GitHub Actions 自动同步
🔄 **最后更新**: """
        + datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        + """
"""
    )

    return content


def create_sidebar(modules):
    """创建侧边栏"""

    content = """## 📚 项目文档

### 🏠 主页
- [📖 文档首页](Home)

"""

    for module_id, module_config in modules.items():
        icon = module_config.get("icon", "📦")
        name = module_config.get("name", module_id)
        files = module_config.get("files", {})

        content += f"### {icon} {name}\n"

        # 主要页面
        if "README.md" in files:
            main_page = files["README.md"].replace(".md", "")
            content += f"- [📖 模块首页]({main_page})\n"

        if "quick-start.md" in files:
            quick_start = files["quick-start.md"].replace(".md", "")
            content += f"- [🚀 快速开始]({quick_start})\n"

        # 功能指南 - 动态检测文件
        guide_patterns = [
            ("basic-", "基础"),
            ("paginated-", "分页"),
            ("storage-", "存储"),
            ("task-", "任务"),
            ("event-", "事件"),
            ("architecture", "架构"),
            ("configuration", "配置"),
            ("api-reference", "API参考"),
            ("best-practices", "最佳实践"),
            ("troubleshooting", "故障排除"),
            ("examples", "示例代码"),
        ]

        for pattern, display_prefix in guide_patterns:
            matching_files = [
                f for f in files.keys() if pattern in f and f != "README.md"
            ]
            for file_name in matching_files:
                page_name = files[file_name].replace(".md", "")
                # 提取更好的显示名称
                display_name = FILE_NAME_MAPPING.get(
                    file_name, file_name.replace(".md", "").replace("-", " ").title()
                )
                content += f"- [🛠️ {display_name}]({page_name})\n"

        # 参考资料
        if "api-reference.md" in files:
            api_ref = files["api-reference.md"].replace(".md", "")
            content += f"- [📋 API参考]({api_ref})\n"

        if "examples.md" in files:
            examples = files["examples.md"].replace(".md", "")
            content += f"- [📝 示例代码]({examples})\n"

        content += "\n"

    return content


def prepare_wiki_docs():
    """准备 Wiki 文档"""

    target_dir = Path("wiki")

    # 创建目标目录
    if target_dir.exists():
        shutil.rmtree(target_dir)
    target_dir.mkdir(parents=True)

    print("开始准备多模块 GitHub Wiki 文档...")

    # 动态发现模块
    print("🔍 扫描模块...")
    modules = discover_modules()

    if not modules:
        print("❌ 未发现任何模块，请检查docs目录结构")
        return

    print(f"✅ 发现 {len(modules)} 个模块: {', '.join(modules.keys())}")

    # 创建主页
    print("创建主页...")
    home_content = create_home_page(modules)
    with open(target_dir / "Home.md", "w", encoding="utf-8") as f:
        f.write(home_content)

    # 创建侧边栏
    print("创建侧边栏...")
    sidebar_content = create_sidebar(modules)
    with open(target_dir / "_Sidebar.md", "w", encoding="utf-8") as f:
        f.write(sidebar_content)

    # 处理每个模块
    for module_id, module_config in modules.items():
        print(f"\n处理模块: {module_config['name']}")

        source_dir = Path(f"docs/{module_id}")
        if not source_dir.exists():
            print(f"警告: 模块目录不存在: {source_dir}")
            continue

        file_mapping = module_config.get("files", {})

        # 处理模块的每个文件
        for source_file, target_file in file_mapping.items():
            source_path = source_dir / source_file
            target_path = target_dir / target_file

            if not source_path.exists():
                print(f"警告: 源文件不存在: {source_path}")
                continue

            print(f"处理文件: {module_id}/{source_file} -> {target_file}")

            # 读取源文件内容
            with open(source_path, "r", encoding="utf-8") as f:
                content = f.read()

            # 更新链接
            content = update_links_in_content(content, module_config)

            # 写入目标文件
            with open(target_path, "w", encoding="utf-8") as f:
                f.write(content)

    print("\nWiki 文档准备完成!")
    print(f"输出目录: {target_dir.absolute()}")

    # 显示文件列表
    print("\n生成的文件:")
    for file in sorted(target_dir.glob("*.md")):
        print(f"  - {file.name}")


def validate_links():
    """验证链接的有效性"""

    wiki_dir = Path("wiki")
    if not wiki_dir.exists():
        print("错误: Wiki 目录不存在，请先运行 prepare_wiki_docs()")
        return

    print("\n验证链接...")

    # 获取所有 Wiki 页面名称
    wiki_pages = set()
    for file in wiki_dir.glob("*.md"):
        page_name = file.stem
        wiki_pages.add(page_name)

    # 检查每个文件中的链接
    broken_links = []

    for file in wiki_dir.glob("*.md"):
        with open(file, "r", encoding="utf-8") as f:
            content = f.read()

        # 查找所有链接
        links = re.findall(r"\[([^\]]+)\]\(([^)]+)\)", content)

        for text, link in links:
            # 跳过外部链接
            if link.startswith("http"):
                continue

            # 检查内部链接
            if link not in wiki_pages:
                broken_links.append((file.name, text, link))

    if broken_links:
        print("发现无效链接:")
        for file, text, link in broken_links:
            print(f"  {file}: [{text}]({link})")
    else:
        print("所有链接都有效!")


if __name__ == "__main__":
    prepare_wiki_docs()
    validate_links()
