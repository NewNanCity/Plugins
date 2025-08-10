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


def update_links_in_content(
    content,
    module_config,
    module_id: str,
    current_source_rel: str,
    global_links: dict[str, str],
):
    """更新文档内容中的链接（支持跨模块/子目录，保留锚点）

    Parameters
    ----------
    content : str
        源 Markdown 文本
    module_config : dict
        当前模块配置，包含本模块 files/links
    module_id : str
        当前模块 id（如 gui/core）
    current_source_rel : str
        当前处理文件相对模块目录的路径（如 'README.md' 或 'api/pages.md'）
    global_links : dict[str, str]
        全局链接映射：'module_id/relative/path.md' → 'Wiki页面名(无.md)'
    """
    link_mapping = module_config.get("links", {})

    def normalize_rel_path(rel: str) -> str:
        # 目录链接转 README.md
        if rel.endswith("/"):
            rel = rel + "README.md"
        # 去掉当前目录前缀
        if rel.startswith("./"):
            rel = rel[2:]
        return rel

    # 更新 Markdown 链接 [text](link)
    def replace_link(match):
        text = match.group(1)
        link = match.group(2).strip()

        # 外部链接或锚点链接
        if link.startswith("http") or "://" in link:
            return match.group(0)

        # 分离锚点
        anchor = None
        if "#" in link:
            base, anchor = link.split("#", 1)
        else:
            base = link

        base = normalize_rel_path(base)

        # 如果源是空，保持原样；若为非.md，尝试回退到对应模块的README
        if not base:
            return match.group(0)
        if not base.endswith(".md"):
            from pathlib import PurePosixPath
            import posixpath as _pp

            base_dir = PurePosixPath(current_source_rel).parent
            abs_rel = _pp.normpath(str(PurePosixPath(base_dir).joinpath(base)))

            target = None
            # 跨模块：回退到目标模块README
            if abs_rel.startswith("../"):
                parts = abs_rel.split("/")
                if len(parts) >= 2 and parts[0] == "..":
                    target_mod = parts[1]
                    readme_key = f"{target_mod}/README.md"
                    for k, v in global_links.items():
                        if k == readme_key:
                            target = v
                            break
            # 同模块：回退到本模块README
            if not target:
                readme_key = f"{module_id}/README.md"
                target = global_links.get(readme_key)

            if not target:
                return match.group(0)
            return f"[{text}]({target}{('#' + anchor) if anchor else ''})"

        # 基于当前文件目录解析相对路径到 docs 下绝对模块路径（规范化 .. 和重复分隔符）
        from pathlib import PurePosixPath
        import posixpath as _pp

        base_dir = PurePosixPath(current_source_rel).parent
        abs_rel = _pp.normpath(str(PurePosixPath(base_dir).joinpath(base)))

        # 组装全局查找 key
        global_key = f"{module_id}/{abs_rel}"

        target = None
        if global_key in global_links:
            target = global_links[global_key]
        elif abs_rel in link_mapping:
            # 同模块的直接映射（不带模块前缀）
            target = link_mapping[abs_rel]
        elif base in link_mapping:
            # 退回原始相对键
            target = link_mapping[base]
        else:
            # 跨模块相对路径，如 ../core/README.md
            norm = _pp.normpath(abs_rel)
            cross_key = f"{module_id}/{norm}"
            target = global_links.get(cross_key)

            # 额外别名修正：core/schedule.md → core/scheduler.md
            if not target and (
                norm.endswith("core/schedule.md") or norm == "core/schedule.md"
            ):
                alias_norm = norm.replace("core/schedule.md", "core/scheduler.md")
                alias_key = f"{module_id}/{alias_norm}"
                target = global_links.get(alias_key)

            # 退化：若仍找不到且是跨模块引用，回退到目标模块README
            if not target and norm.startswith("../"):
                # 提取 '../<mod>/' 的模块名
                parts = norm.split("/")
                if len(parts) >= 2 and parts[0] == "..":
                    target_mod = parts[1]
                    readme_key = f"{target_mod}/README.md"
                    # 在全局映射中查找该模块的主页
                    for k, v in global_links.items():
                        if k == readme_key:
                            target = v
                            break

            # 退化：同模块内未知页面回退到本模块README
            if not target:
                readme_key = f"{module_id}/README.md"
                if readme_key in global_links:
                    target = global_links[readme_key]

        if not target:
            return match.group(0)

        if anchor:
            return f"[{text}]({target}#{anchor})"
        return f"[{text}]({target})"

    # 替换链接
    content = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", replace_link, content)

    # 标准化 “下一步” 链接格式
    def repl_next(m):
        raw_link = m.group(2)
        # 复用上面的替换器逻辑（构造一个假的匹配对象不方便，这里尝试直接走全局映射）
        link_only = raw_link.split("#", 1)[0]
        link_only = normalize_rel_path(link_only)
        from pathlib import PurePosixPath

        abs_rel = str(
            PurePosixPath(PurePosixPath(current_source_rel).parent)
            .joinpath(link_only)
            .as_posix()
        )
        global_key = f"{module_id}/{abs_rel}"
        target = global_links.get(global_key, link_mapping.get(link_only, raw_link))
        return f"---\n\n**下一步** → [{m.group(1)}]({target})"

    content = re.sub(
        r"---\n\n\*\*下一步\*\* → \[([^\]]+)\]\(([^)]+)\)", repl_next, content
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

        # 获取模块主页链接（仅当 README.md 存在时，否则链接到第一个页面或不链接）
        files: dict[str, str] = module_config.get("files", {})
        main_link_label = name
        main_link_target = None
        if "README.md" in files:
            main_link_target = files["README.md"].replace(".md", "")
        elif files:
            any_page = next(iter(files.values()))
            main_link_target = any_page.replace(".md", "")

        if main_link_target:
            content += f"### {icon} [{main_link_label}]({main_link_target})\n\n"
        else:
            content += f"### {icon} {main_link_label}\n\n"

        if description:
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
    """创建侧边栏（按 docs 目录语义分组）"""

    content = """## 📚 项目文档

### 🏠 主页
- [📖 文档首页](Home)

"""

    known_groups = ["tutorials", "guides", "api", "examples"]

    for module_id, module_config in modules.items():
        icon = module_config.get("icon", "📦")
        name = module_config.get("name", module_id)
        files: dict[str, str] = module_config.get("files", {})

        content += f"### {icon} {name}\n"

        # 主要页面
        if "README.md" in files:
            main_page = files["README.md"].replace(".md", "")
            content += f"- [📖 模块首页]({main_page})\n"

        if "quick-start.md" in files:
            quick_start = files["quick-start.md"].replace(".md", "")
            content += f"- [🚀 快速开始]({quick_start})\n"

        # 收集已分组的键，避免重复
        grouped_keys: set[str] = set()

        # 目录分组：tutorials/guides/api/examples
        for group in known_groups:
            present = [k for k in files.keys() if k.startswith(f"{group}/")]
            if not present:
                continue

            # 组标题与索引（如果存在 README）
            group_title = {
                "tutorials": "📖 教程",
                "guides": "🛠️ 指南",
                "api": "📚 API",
                "examples": "📝 示例",
            }.get(group, group)
            index_key = f"{group}/README.md"
            if index_key in files:
                index_page = files[index_key].replace(".md", "")
                content += f"- [{group_title}]({index_page})\n"
            else:
                content += f"- {group_title}\n"

            # 分组排序规则
            def sort_key(x: str) -> tuple:
                import re as _re

                # tutorials: 编号优先
                if group == "tutorials":
                    m = _re.match(r"^tutorials/(\d+)-", x)
                    if m:
                        return (0, int(m.group(1)), x)
                    return (1, x)
                # api: 固定顺序
                if group == "api":
                    order = [
                        "api/README.md",
                        "api/pages.md",
                        "api/components.md",
                        "api/sessions.md",
                        "api/events.md",
                        "api/items.md",
                    ]
                    idx = order.index(x) if x in order else 999
                    return (idx, x)
                # guides: 固定顺序
                if group == "guides":
                    order = [
                        "guides/README.md",
                        "guides/best-practices.md",
                        "guides/performance.md",
                        "guides/error-handling.md",
                        "guides/troubleshooting.md",
                    ]
                    idx = order.index(x) if x in order else 999
                    return (idx, x)
                # examples: README → basic → advanced → real-world
                if group == "examples":
                    if x == "examples/README.md":
                        return (0, x)
                    if x.startswith("examples/basic/"):
                        return (1, x)
                    if x.startswith("examples/advanced/"):
                        return (2, x)
                    if x.startswith("examples/real-world/"):
                        return (3, x)
                    return (9, x)
                # 其他默认字典序
                return (5, x)

            for k in sorted(set(present), key=sort_key):
                grouped_keys.add(k)
                page = files[k].replace(".md", "")
                # 友好显示名：去掉“模块名前缀-”；教程再去掉“教程-”前缀
                display = files[k].replace(".md", "").replace(f"{name}-", "")
                if group == "tutorials":
                    display = display.replace("教程-", "")
                # 子目录 README 作为分组索引项
                if k.endswith("/README.md"):
                    content += f"  - [索引]({page})\n"
                else:
                    content += f"  - [{display}]({page})\n"

        # 其他未分组文件（排除 README 和 quick-start）
        others = [
            k
            for k in files.keys()
            if k not in grouped_keys and k not in ("README.md", "quick-start.md")
        ]
        if others:
            content += f"- 其他\n"
            for k in sorted(others):
                page = files[k].replace(".md", "")
                display = files[k].replace(".md", "").replace(f"{name}-", "")
                content += f"  - [{display}]({page})\n"

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

    # 构建全局链接映射：module_id/relative/path.md → Wiki页面名(无.md)
    global_links: dict[str, str] = {}
    for mid, mconf in modules.items():
        for src_rel, tgt in mconf.get("files", {}).items():
            key = f"{mid}/{src_rel}"
            global_links[key] = tgt.replace(".md", "")

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

            # 更新链接（带全局映射与当前上下文）
            content = update_links_in_content(
                content,
                module_config,
                module_id=module_id,
                current_source_rel=source_file,
                global_links=global_links,
            )

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
            if link.startswith("http") or "://" in link:
                continue

            # 忽略锚点，取页面名部分
            link_base = link.split("#", 1)[0]

            # 忽略明显的非Wiki页面资源链接（含扩展名，如 .yml/.png/.kt 等）
            if "." in link_base and link_base not in wiki_pages:
                continue

            # 检查内部Wiki页面链接
            if link_base not in wiki_pages:
                broken_links.append((file.name, text, link))

    if broken_links:
        print("发现无效链接:")
        for file, text, link in broken_links:
            print(f"  {file}: [{text}]({link})")
    else:
        print("所有链接都有效!")


if __name__ == "__main__":
    try:
        prepare_wiki_docs()
        validate_links()
    except Exception as e:
        print(f"❌ 脚本执行失败: {e}")
        import traceback

        traceback.print_exc()
