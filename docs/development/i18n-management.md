# 国际化(i18n)管理

## 语言分析工具

项目提供了完整的语言键分析和管理工具：

```bash
# 分析所有插件的语言键使用情况
python scripts/language-analyzer.py

# 自动删除冗余的语言键
python scripts/language-analyzer.py --remove-redundant --confirm

# 检查 i18n 最佳实践合规性
python scripts/language-analyzer.py --check-best-practices
```

## 语言文件格式规范

- 使用 `{0} {1}` 等 StringFormat 语法而非 `$key$` 格式
- 所有模板使用 `<%key%>` 格式便于替换
- 中英文语言文件必须保持键结构一致
- 遵循五层架构分类：Core、Commands、Gui、Business、Log

## 语言键分类（五层架构）

```yaml
# Core 系统键
core:
  success:
    operation_completed: "..."

# 命令键
commands:
  reload:
    description: "..."
    success: "..."

# GUI 键
gui:
  main_menu:
    title: "..."

# 业务键
business:
  player:
    welcome: "..."

# 日志键
log:
  info:
    plugin_loaded: "..."
  error:
    initialization_failed: "..."
```

## 相关文档

- [i18n 模块文档](../i18n/README.md)
- [i18n 命名规范](../i18n/naming-conventions.md)
- [消息系统文档](../core/messaging.md)
- [i18n 生命周期最佳实践](../core/i18n-lifecycle-best-practices.md)
