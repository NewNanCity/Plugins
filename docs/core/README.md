# Core 模块文档

欢迎使用现代化的 Minecraft 插件核心框架！Core 模块提供了完整的插件开发基础设施，包括自动资源管理、现代化事件处理和统一的消息系统。

## 🚀 快速开始

- [📖 模块介绍](introduction.md) - 了解 Core 模块的核心价值
- [⚡ 快速开始](quick-start.md) - 5分钟创建第一个插件
- [🎯 核心概念](concepts.md) - 理解基础架构和设计理念

## 📚 核心功能

### 基础架构
- [🔧 BasePlugin](base-plugin.md) - 增强的插件基类
- [📦 BaseModule](base-module.md) - 模块化开发架构
- [♻️ 资源管理](terminable.md) - Terminable 自动资源管理体系
- [🔄 生命周期](lifecycle.md) - 插件和模块生命周期管理

### 异步编程
- [⏰ 任务调度](scheduler.md) - 现代化任务调度系统
- [⚡ 事件处理](events.md) - 函数式事件处理系统

### 通信与配置
- [💬 消息系统](messaging.md) - 统一的消息和国际化管理
- [⚙️ 配置管理](configuration.md) - 多格式配置支持
- [🌐 国际化](i18n.md) - 多语言支持和生命周期管理

## 🎯 高级主题

- [🏗️ 架构设计](architecture.md) - 四层架构和设计模式
- [🎮 命令系统](commands.md) - CommandAPI 集成和最佳实践
- [⚡ 性能优化](performance.md) - 性能调优和监控 *(待创建)*
- [🛡️ 错误处理](error-handling.md) - 异常处理和容错机制 *(待创建)*

## 📖 参考资料

- [📋 API 参考](api-reference.md) - 完整的 API 文档 *(待创建)*
- [💡 最佳实践](best-practices.md) - 开发规范和建议
- [📝 示例代码](examples.md) - 实用代码示例集合
- [🔧 故障排除](troubleshooting.md) - 常见问题和解决方案
- [🔄 迁移指南](migration.md) - 从其他框架迁移 *(待创建)*

## 🎯 快速导航

### 我想要...
- **创建第一个插件** → [快速开始](quick-start.md)
- **理解核心概念** → [核心概念](concepts.md)
- **管理资源生命周期** → [资源管理](terminable.md)
- **处理事件** → [事件处理](events.md)
- **调度任务** → [任务调度](scheduler.md)
- **使用任务调度** → [任务调度系统](scheduler.md)
- **发送消息** → [消息系统](messaging.md)
- **配置插件** → [配置管理](configuration.md)
- **设计架构** → [架构设计](architecture.md)
- **优化性能** → [性能优化](performance.md)
- **解决问题** → [故障排除](troubleshooting.md)

## 🆕 核心特性

### 🔥 自动资源管理
基于 Terminable 模式的完整资源生命周期管理，插件禁用时自动清理所有资源，防止内存泄漏。

### ⚡ 现代化任务调度
ITaskHandler 接口提供类似 CompletableFuture 的 API，支持链式调用、依赖管理和组合任务。

### 🔄 任务调度支持
基于任务调度器的异步编程，支持并行任务、自动生命周期管理和非阻塞 IO 操作。

### 🎯 函数式事件处理
链式调用的事件处理 API，支持过滤器、自动过期和异常处理。

### 📦 模块化架构
BaseModule 提供完整的模块开发支持，自动资源绑定和生命周期管理。

### 💬 统一消息系统
支持 Legacy、MiniMessage 和 Plain 格式，自动格式检测和多语言支持。

## 🏆 设计理念

Core 模块基于以下设计理念构建：

- **自动化优于手动** - 自动资源管理，减少样板代码
- **类型安全** - 完整的 Kotlin 类型支持和编译时检查
- **现代化 API** - 函数式编程和任务调度支持
- **模块化设计** - 高内聚低耦合的架构
- **性能优先** - 优化的资源使用和异步处理

---

**开始您的 Core 开发之旅** → [📖 模块介绍](introduction.md)
