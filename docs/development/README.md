# 开发指南

本目录包含面向插件开发者和 AI 编码助手的开发规范与参考文档。

## 📚 文档索引

| 文档                                           | 说明                                                         |
| ---------------------------------------------- | ------------------------------------------------------------ |
| [plugin-conventions.md](plugin-conventions.md) | 插件开发规范：配置、重载、生命周期、Manager 模式             |
| [build-system.md](build-system.md)             | 构建系统详解：BuildSrc、Shadow JAR、Gradle 优化              |
| [migration-guide.md](migration-guide.md)       | 迁移指南：从 violet 框架迁移到 BasePlugin + Cloud 的标准模式 |
| [migration-records.md](migration-records.md)   | 迁移记录：各插件迁移状态与功能对照                           |
| [quality-checklist.md](quality-checklist.md)   | 质量检查清单：开发、测试、部署阶段                           |
| [i18n-management.md](i18n-management.md)       | 国际化管理：语言键分析工具和规范                             |

## 🔗 相关文档

- [Core 模块文档](../core/README.md) — BasePlugin、事件、调度器、消息系统
- [命令系统（Cloud）](../core/commands.md) — 注解驱动命令框架
- [最佳实践](../core/best-practices.md) — 四层架构、BaseModule 规范
- [GUI 模块文档](../gui/README.md) — 现代化 GUI 系统
- [配置模块文档](../config/README.md) — Jackson 多格式配置
- [国际化模块文档](../i18n/README.md) — 多语言模板系统
