# GitHub Wiki 自动同步设置指南

本指南将帮助您设置 GitHub Wiki 的自动同步功能，将 `docs/` 目录下的模块文档自动同步到 GitHub Wiki。

## 🎯 功能概述

- ✅ **自动同步** - 每次提交到主分支时自动更新 Wiki
- ✅ **格式转换** - 自动将 Markdown 链接转换为 Wiki 格式
- ✅ **文件重命名** - 使用中文名称作为 Wiki 页面标题
- ✅ **侧边栏生成** - 自动生成结构化的侧边栏导航
- ✅ **链接验证** - 检查内部链接的有效性
- ✅ **本地测试** - 提供本地测试工具
- ✅ **多模块支持** - 支持 Core、Config、Database、I18n、Network、GUI 等模块

## 📁 文件结构

```
├── .github/workflows/
│   └── sync-wiki.yml          # GitHub Action 工作流
├── scripts/
│   ├── prepare-wiki.py        # Wiki 文档准备脚本
│   └── test-wiki-locally.py   # 本地测试脚本
├── docs/
│   ├── core/                  # Core 模块文档
│   ├── config/                # Config 模块文档
│   ├── database/              # Database 模块文档
│   ├── i18n/                  # I18n 模块文档
│   ├── network/               # Network 模块文档
│   ├── gui/                   # GUI 模块文档
│   └── WIKI-SETUP.md         # 本文档
├── wiki-config.yml           # Wiki 配置文件
└── wiki/                     # 生成的 Wiki 文档 (临时)
```

## 🚀 快速开始

### 1. 启用 GitHub Wiki

1. 进入您的 GitHub 仓库
2. 点击 **Settings** 标签
3. 滚动到 **Features** 部分
4. 勾选 **Wikis** 复选框

### 2. 配置权限

确保 GitHub Actions 有权限推送到 wiki 分支：

1. 进入 **Settings** → **Actions** → **General**
2. 在 **Workflow permissions** 部分选择 **Read and write permissions**
3. 勾选 **Allow GitHub Actions to create and approve pull requests**

### 3. 触发首次同步

有三种方式触发同步：

#### 方式 1: 修改文档并提交
```bash
# 修改 docs/ 下任何模块的文件
git add docs/
git commit -m "更新模块文档"
git push origin main
```

#### 方式 2: 手动触发
1. 进入 **Actions** 标签
2. 选择 **同步文档到 Wiki** 工作流
3. 点击 **Run workflow**

#### 方式 3: 本地测试后推送
```bash
# 本地测试
python scripts/test-wiki-locally.py

# 如果测试通过，推送更改
git push origin main
```

## 🛠️ 本地开发和测试

### 安装依赖

```bash
# 确保安装了 Python 3.7+
python --version

# 脚本使用标准库，无需额外依赖
```

### 本地测试命令

```bash
# 完整测试（生成 + 验证 + 预览）
python scripts/test-wiki-locally.py

# 仅生成文档
python scripts/test-wiki-locally.py test

# 预览生成的文档
python scripts/test-wiki-locally.py preview

# 清理生成的文件
python scripts/test-wiki-locally.py clean
```

### 手动生成 Wiki 文档

```bash
# 运行准备脚本
python scripts/prepare-wiki.py

# 查看生成的文件
ls -la wiki/
```

## 📝 文档编写规范

### 文件命名

源文件使用英文名称，会自动转换为中文 Wiki 页面名：

```
README.md          → Home.md
intro.md           → 模块介绍.md
quick-start.md     → 快速开始.md
troubleshooting.md → 故障排除.md
best-practices.md  → 最佳实践.md
```

### 链接格式

在源文档中使用相对链接：

```markdown
<!-- 源文档中 -->
[快速开始](quick-start.md)
[基础概念](concepts.md)

<!-- 自动转换为 Wiki 链接 -->
[快速开始](快速开始)
[基础概念](基础概念)
```

### 导航链接

避免使用 "返回目录" 链接，Wiki 有自动导航：

```markdown
<!-- ❌ 不推荐 -->
**返回目录** → [📚 README](README.md)

<!-- ✅ 推荐 -->
**下一步** → [基础GUI](basic-gui.md)
```

## ⚙️ 自定义配置

### 修改文件映射

编辑 `scripts/prepare-wiki.py` 中的 `FILE_MAPPING` 字典：

```python
FILE_MAPPING = {
    'your-file.md': '您的文件.md',
    # 添加新的映射
}
```

### 修改侧边栏

编辑 `.github/workflows/sync-wiki.yml` 中的 `_Sidebar.md` 部分：

```yaml
cat > _Sidebar.md << 'EOF'
## 📚 您的文档标题

### 您的分类
- [📖 您的页面](您的页面)
EOF
```

### 自定义同步触发条件

修改 `.github/workflows/sync-wiki.yml` 中的触发条件：

```yaml
on:
  push:
    branches: [ main, develop ]  # 添加更多分支
    paths:
      - 'docs/**'               # 监控更多路径
  schedule:
    - cron: '0 2 * * *'         # 每日定时同步
```

## 🔍 故障排除

### 常见问题

#### 1. Action 执行失败

**问题**: GitHub Action 显示权限错误
**解决**: 检查仓库的 Actions 权限设置

#### 2. Wiki 页面显示异常

**问题**: 链接无法正常工作
**解决**: 运行本地测试检查链接格式

```bash
python scripts/test-wiki-locally.py
```

#### 3. 文档未同步

**问题**: 修改文档后 Wiki 没有更新
**解决**: 检查文件路径是否在 `docs/` 下的模块目录中

#### 4. 中文文件名问题

**问题**: 中文文件名在某些系统上显示异常
**解决**: 确保系统支持 UTF-8 编码

### 调试方法

#### 查看 Action 日志

1. 进入 **Actions** 标签
2. 点击最近的工作流运行
3. 查看详细日志输出

#### 本地调试

```bash
# 启用详细输出
python scripts/prepare-wiki.py --verbose

# 检查生成的文件
cat wiki/Home.md | head -20
```

#### 验证 Wiki 分支

```bash
# 检查 wiki 分支
git fetch origin
git checkout wiki
ls -la *.md
```

## 📊 监控和维护

### 定期检查

- **每周**: 检查 Wiki 链接是否正常
- **每月**: 验证文档结构和导航
- **版本发布前**: 完整测试所有文档

### 性能优化

- 大文件拆分为多个小文件
- 使用图片压缩减少仓库大小
- 定期清理不需要的历史版本

### 备份策略

Wiki 内容会自动备份到 `wiki` 分支，建议：

- 定期导出 Wiki 内容
- 保持源文档的版本控制
- 使用 Git 标签标记重要版本

## 🎉 完成设置

设置完成后，您将拥有：

- ✅ 自动同步的 GitHub Wiki
- ✅ 结构化的文档导航
- ✅ 本地测试和预览工具
- ✅ 完整的错误处理和日志

现在您可以专注于编写文档内容，同步过程将完全自动化！

---

📝 **需要帮助？** 查看 [GitHub Actions 文档](https://docs.github.com/en/actions) 或提交 Issue
