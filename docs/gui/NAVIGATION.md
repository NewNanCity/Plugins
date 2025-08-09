# GUI 模块文档导航

本文档提供GUI模块完整文档的导航索引，帮助你快速找到所需的信息。

## 📚 文档结构概览

```
docs/gui/
├── README.md                    # 总览和快速导航
├── GETTING_STARTED.md          # 快速入门指南
├── CONCEPTS.md                  # 核心概念详解
├── NAVIGATION.md               # 文档导航索引（本文档）
├── CHANGELOG.md                # 版本更新记录
├── IMPROVEMENTS.md             # 改进建议和计划
├── tutorials/                  # 教程指南
│   ├── README.md              # 教程索引
│   ├── 01-first-gui.md        # 创建第一个GUI
│   ├── 02-components.md       # 组件使用详解
│   ├── 03-events.md           # 事件处理系统
│   ├── 04-sessions.md         # 会话管理
│   ├── 05-i18n-integration.md # i18n国际化集成
│   ├── 06-advanced-features.md # 高级功能
│   └── 07-infinite-scrolling.md # 无限滚动
├── api/                        # API参考文档
│   ├── README.md              # API总览
│   ├── pages.md               # 页面API
│   ├── components.md          # 组件API
│   ├── sessions.md            # 会话API
│   ├── events.md              # 事件API
│   ├── items.md               # 物品工具API
├── guides/                     # 开发指南
│   ├── best-practices.md      # 最佳实践
│   ├── performance.md         # 性能优化
│   ├── error-handling.md      # 错误处理
│   └── troubleshooting.md     # 故障排除
└── examples/                   # 示例代码
    ├── README.md              # 示例索引
    ├── basic/                 # 基础示例
    ├── advanced/              # 高级示例
    └── real-world/            # 实际项目示例
```

## 🚀 学习路径

### 新手入门路径
1. **[总览](README.md)** - 了解GUI模块的特性和能力
2. **[快速入门](GETTING_STARTED.md)** - 5分钟创建第一个GUI
3. **[核心概念](CONCEPTS.md)** - 理解设计理念和架构
4. **[教程1：创建第一个GUI](tutorials/01-first-gui.md)** - 详细的入门教程
5. **[教程2：组件使用详解](tutorials/02-components.md)** - 学习各种组件

### 进阶开发路径
1. **[教程3：事件处理系统](tutorials/03-events.md)** - 掌握事件处理
2. **[教程4：会话管理](tutorials/04-sessions.md)** - 学习页面导航
3. **[教程5：i18n国际化集成](tutorials/05-i18n-integration.md)** - 多语言支持
4. **[最佳实践](guides/best-practices.md)** - 编码规范和建议
5. **[性能优化](guides/performance.md)** - 提升GUI性能

### 高级开发路径
1. **[教程6：高级功能](tutorials/06-advanced-features.md)** - 调度器和异步操作
2. **[教程7：无限滚动](tutorials/07-infinite-scrolling.md)** - 大数据量处理
3. **[API文档](api/)** - 详细的API参考
4. **[高级示例](examples/advanced/)** - 复杂功能实现
5. **[实际项目](examples/real-world/)** - 真实插件案例

## 🔍 按需查找

### 按功能查找

#### 界面创建
- [创建基本GUI](tutorials/01-first-gui.md#创建第一个gui)
- [页面API](api/pages.md)
- [组件API](api/components.md)

#### 组件使用
- [单槽组件](tutorials/02-components.md#单槽组件)
- [边框组件](tutorials/02-components.md#边框组件)
- [分页组件](tutorials/02-components.md#分页组件)
- [边框组件示例](examples/basic/border-components.md)

#### 事件处理
- [事件处理教程](tutorials/03-events.md)
- [事件API](api/events.md)
- [事件处理示例](examples/advanced/event-handling-examples.md)

#### 会话管理
- [会话管理教程](tutorials/04-sessions.md)
- [会话API](api/sessions.md)

#### 国际化
- [i18n集成教程](tutorials/05-i18n-integration.md)
- [i18n最佳实践](guides/best-practices.md#i18n国际化最佳实践)

#### 数据处理
- [性能优化](guides/performance.md#数据处理优化)

#### 物品创建
- [物品工具API](api/items.md)
- [增强物品示例](examples/basic/enhanced-items-demo.md)
- [头颅物品示例](examples/basic/skull-items.md)

### 按问题类型查找

#### 开发问题
- [故障排除](guides/troubleshooting.md)
- [错误处理](guides/error-handling.md)
- [最佳实践](guides/best-practices.md)

#### 性能问题
- [性能优化指南](guides/performance.md)
- [渲染优化](guides/performance.md#渲染优化)
- [内存优化](guides/performance.md#内存优化)

#### 使用问题
- [常见问题](guides/troubleshooting.md#常见问题)
- [配置问题](guides/troubleshooting.md#配置问题)
- [兼容性问题](guides/troubleshooting.md#兼容性问题)

### 按示例类型查找

#### 基础示例
- [边框组件示例](examples/basic/border-components.md)
- [增强物品示例](examples/basic/enhanced-items-demo.md)
- [头颅物品示例](examples/basic/skull-items.md)

#### 高级示例
- [事件处理示例](examples/advanced/event-handling-examples.md)
- [组件特定事件](examples/advanced/component-specific-events.md)
- [功能基础事件](examples/advanced/feature-based-events.md)

#### 实际项目
- [TPA插件示例](examples/real-world/tpa-plugin-example.md)

## 📖 文档类型说明

### 📚 教程 (Tutorials)
- **目标**：系统性学习
- **特点**：循序渐进，包含完整示例
- **适用**：初学者和进阶开发者

### 📋 指南 (Guides)
- **目标**：解决特定问题
- **特点**：实用性强，针对性明确
- **适用**：有经验的开发者

### 📖 API参考 (API Reference)
- **目标**：查阅具体接口
- **特点**：详细完整，便于查找
- **适用**：开发过程中的参考

### 💡 示例 (Examples)
- **目标**：提供实际代码
- **特点**：可直接使用或修改
- **适用**：快速实现功能

## 🔗 外部链接

### 相关模块文档
- [Core模块文档](../core/) - 基础功能和架构
- [项目主页](../../README.md) - 项目总览

### 开发工具
- [Minecraft Wiki](https://minecraft.wiki/) - Minecraft相关信息
- [Paper API](https://papermc.io/javadocs/) - Paper API文档
- [Kotlin文档](https://kotlinlang.org/docs/) - Kotlin语言文档

## 💡 使用建议

### 首次使用
1. 阅读[总览](README.md)了解GUI模块
2. 完成[快速入门](GETTING_STARTED.md)创建第一个GUI
3. 学习[核心概念](CONCEPTS.md)理解设计理念
4. 按顺序完成[教程](tutorials/)

### 日常开发
1. 使用[API文档](api/)查阅具体接口
2. 参考[示例代码](examples/)快速实现功能
3. 遵循[最佳实践](guides/best-practices.md)编写高质量代码
4. 遇到问题查看[故障排除](guides/troubleshooting.md)

### 性能优化
1. 阅读[性能优化指南](guides/performance.md)
2. 使用性能监控工具
3. 参考高级示例的优化技巧

## 📝 文档贡献

如果你发现文档问题或有改进建议：

1. 检查是否已在[改进建议](IMPROVEMENTS.md)中记录
2. 提供具体的问题描述和建议
3. 如果可能，提供修改建议或示例代码

---

**希望这个导航能帮助你更好地使用GUI模块！** 🎉
