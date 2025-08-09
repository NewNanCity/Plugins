# 插件框架文档中心

欢迎来到现代化 Minecraft 插件框架的文档中心！这里包含了所有模块的完整文档和使用指南。

## 📚 模块文档

### 🔧 Core 模块（核心）
现代化的插件核心框架，提供资源管理、事件处理、任务调度等基础功能。

**[📖 查看 Core 模块文档](core/README.md)**

- **特色功能**: Terminable体系、BasePlugin基类、异步调度支持、自动清理
- **适用场景**: 所有插件的必需基础，资源生命周期管理
- **学习时间**: 30分钟快速上手，2小时深入掌握

### ⚙️ Config 模块（配置）
基于 Jackson 的多格式配置管理器，支持类型安全的配置操作。

**[📖 查看 Config 模块文档](config/README.md)**

- **特色功能**: 多格式支持、类型安全、Bukkit集成、缓存机制
- **适用场景**: 复杂配置管理、多格式配置文件、类型安全需求
- **学习时间**: 20分钟快速上手，1小时深入掌握

### 🗄️ Database 模块（数据库）
基于 HikariCP 的高性能数据库管理器，支持多种数据库和事务管理。

**[📖 查看 Database 模块文档](database/README.md)**

- **特色功能**: HikariCP连接池、DSL配置、事务管理、批量操作
- **适用场景**: 数据持久化、高并发数据访问、事务处理
- **学习时间**: 25分钟快速上手，1.5小时深入掌握

### 🌍 I18n 模块（国际化）
完整的多语言支持系统，提供模板替换和智能回退机制。

**[📖 查看 I18n 模块文档](i18n/README.md)**

- **特色功能**: 多格式语言文件、模板变量、三级回退、用户语言
- **适用场景**: 多语言插件、国际化服务器、个性化语言设置
- **学习时间**: 15分钟快速上手，45分钟深入掌握

### 🌐 Network 模块（网络）
基于 Ktor Client 的现代化 HTTP 客户端，支持异步调度和类型安全。

**[📖 查看 Network 模块文档](network/README.md)**

- **特色功能**: 异步支持、类型安全、可取消请求、多种认证
- **适用场景**: API集成、文件下载、网络监控、外部服务调用
- **学习时间**: 20分钟快速上手，1小时深入掌握

### 🖱️ GUI 模块（图形界面）
现代化的 Minecraft GUI 框架，支持会话管理和动态界面。

**[📖 查看 GUI 模块文档](gui/README.md)**

- **特色功能**: 会话管理、分页GUI、任务系统、布局方案
- **适用场景**: 复杂用户界面、多页面导航、动态内容展示
- **学习时间**: 30分钟快速上手，2小时深入掌握

## 🚀 快速开始

### 1. 选择您需要的模块

根据您的需求选择合适的模块：

- **基础插件开发** → 从 [Core 模块](core/README.md) 开始
- **配置管理** → 添加 [Config 模块](config/README.md)
- **数据存储** → 添加 [Database 模块](database/README.md)
- **多语言支持** → 添加 [I18n 模块](i18n/README.md)
- **网络请求** → 添加 [Network 模块](network/README.md)
- **用户界面** → 添加 [GUI 模块](gui/README.md)

### 2. 查看快速开始指南

每个模块都提供了详细的快速开始指南：

- [Core 快速开始](core/quick-start.md)
- [Config 快速开始](config/quick-start.md)
- [Database 快速开始](database/quick-start.md)
- [I18n 快速开始](i18n/quick-start.md)
- [Network 快速开始](network/quick-start.md)
- [GUI 快速开始](gui/quick-start.md)

### 3. 参考最佳实践

学习如何正确使用各个模块：

- [Core 最佳实践](core/best-practices.md)
- [Config 最佳实践](config/best-practices.md)
- [Database 最佳实践](database/best-practices.md)
- [I18n 最佳实践](i18n/best-practices.md)
- [Network 最佳实践](network/best-practices.md)
- [GUI 最佳实践](gui/best-practices.md)

## 🔧 故障排除

遇到问题？查看对应模块的故障排除指南：

- [Core 故障排除](core/troubleshooting.md)
- [Config 故障排除](config/troubleshooting.md)
- [Database 故障排除](database/troubleshooting.md)
- [I18n 故障排除](i18n/troubleshooting.md)
- [Network 故障排除](network/troubleshooting.md)
- [GUI 故障排除](gui/troubleshooting.md)

## 📖 文档特色

### 🎯 结构化组织
每个模块文档都按照统一的结构组织：
- **介绍** - 5分钟快速了解模块
- **快速开始** - 第一个示例程序
- **功能指南** - 详细功能说明
- **高级主题** - 深入技术细节
- **参考资料** - API文档和最佳实践

### 📝 实用导向
- **完整示例** - 每个功能都有可运行的代码示例
- **最佳实践** - 基于实际项目经验的开发建议
- **故障排除** - 常见问题的解决方案
- **性能优化** - 提升插件性能的技巧

### 🔄 持续更新
- **版本同步** - 文档与代码版本保持同步
- **社区反馈** - 根据用户反馈持续改进
- **示例更新** - 定期更新示例代码

## 🤝 贡献指南

### 文档改进
如果您发现文档中的错误或有改进建议：

1. **提交 Issue** - 描述问题或建议
2. **提交 PR** - 直接修改文档
3. **参与讨论** - 在社区中分享经验

### 示例代码
欢迎贡献更多的示例代码：

1. **实际案例** - 分享您的插件使用案例
2. **最佳实践** - 分享开发经验和技巧
3. **性能优化** - 分享性能优化方案

## 📞 获取帮助

### 社区支持
- **GitHub Issues** - 报告问题和功能请求
- **GitHub Discussions** - 技术讨论和经验分享
- **Wiki** - 查看更多文档和教程

### 技术支持
- **API 文档** - 查看详细的 API 参考
- **示例项目** - 参考完整的示例项目
- **最佳实践** - 学习推荐的开发模式

---

**开始您的插件开发之旅** → 选择一个模块开始学习！
