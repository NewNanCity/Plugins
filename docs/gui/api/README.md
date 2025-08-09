# GUI 模块API参考文档

本目录包含GUI模块的完整API参考文档，按功能模块组织。

## 📚 文档结构

### 核心API
- [页面API](pages.md) - Page接口和实现类
- [会话API](sessions.md) - Session接口和管理
- [组件API](components.md) - 所有组件类型的API
- [事件API](events.md) - 事件处理和监听

### 工具API
- [物品工具API](items.md) - ItemUtil和物品创建

## 🔍 快速查找

### 按功能查找
- **创建页面**: [openPage](pages.md#openpage), [createPage](pages.md#createpage)
- **组件渲染**: [render](components.md#render), [update](components.md#update)
- **边框组件**: [borderFillComponent](components.md#borderfillcomponent), [fullBorder](components.md#fullborder)
- **事件处理**: [onLeftClick](events.md#onleftclick), [onRightClick](events.md#onrightclick)
- **Session管理**: [push](sessions.md#push), [pop](sessions.md#pop), [close](sessions.md#close)
- **物品创建**: [item](items.md#item), [skull](items.md#skull), [customSkull](items.md#customskull)

### 按类型查找
- **接口**: Page, Session, Component, EventHandler, DataProvider
- **实现类**: BasePage, ChestPage, BookPage, SingleSlotComponent, BorderFillComponent
- **工具类**: ItemUtil, DataProviders
- **扩展函数**: openPage, getDefaultSession, guiManager
- **枚举类**: BorderType, ComponentState, ClickType, CacheStrategy

## 📖 使用指南

### 新手入门
1. 从[页面API](pages.md)开始，了解基本的页面创建
2. 学习[组件API](components.md)，掌握UI组件的使用
3. 阅读[事件API](events.md)，理解交互处理
4. 参考[DSL API](dsl.md)，使用声明式语法

### 进阶开发
1. 深入[会话API](sessions.md)，掌握复杂导航
2. 掌握[事件API](events.md)，实现复杂交互逻辑
3. 优化性能，参考[性能优化指南](../guides/performance.md)

## 🔗 相关链接

- [快速入门](../GETTING_STARTED.md) - 5分钟上手GUI模块
- [核心概念](../CONCEPTS.md) - 理解设计理念
- [教程指南](../tutorials/) - 系统学习GUI开发
- [最佳实践](../guides/best-practices.md) - 编码规范和建议
- [示例代码](../examples/) - 实用的代码示例

## 📝 API版本

当前文档对应GUI模块版本：**1.0.0**

### 版本兼容性
- **1.0.x**: 完全兼容
- **0.9.x**: 部分兼容，建议升级
- **0.8.x及以下**: 不兼容，需要迁移

## 💡 使用提示

### 代码示例约定
- 所有示例使用Kotlin语言
- 假设已正确导入相关包
- 使用`player`表示当前玩家
- 使用`plugin`表示插件实例

### 参数说明约定
- `required` - 必需参数
- `optional` - 可选参数，有默认值
- `nullable` - 可为null的参数
- `vararg` - 可变参数

### 返回值说明
- 明确标注返回类型
- 说明可能的异常情况
- 提供使用示例

---

**开始探索GUI模块的强大API吧！** 🚀
