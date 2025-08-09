# GUI 模块文档

GUI 是新一代的 Minecraft 插件 GUI 开发框架，基于现代化的架构设计，提供强大而灵活的用户界面开发能力。**与 Core 模块深度集成，原生支持 i18n 国际化和 MiniMessage/Legacy 格式解析。**

## 🌟 核心特性

### 🌐 原生 i18n 支持
GUI 模块与 Core 的 Message 模块深度集成，支持：
- **直接使用 i18n 模板**：`name("<%gui.button.confirm%>")` 而不是 `name(plugin.messager.sprintf("<%gui.button.confirm%>"))`
- **自动格式解析**：支持 MiniMessage 和 Legacy 格式的自动识别和转换
- **统一的文本处理**：所有文本都通过 GuiManager 的 textPreprocessor 处理

### 🏗️ 现代化架构
- **Session系统**：类似浏览器的会话管理，支持页面栈和导航
- **组件化设计**：可复用的UI组件，支持复杂布局
- **事件冒泡**：从item到component到page的事件传播机制
- **生命周期管理**：自动资源管理，防止内存泄漏

### 🔧 丰富的组件
- **SingleSlotComponent**：单槽组件，最基础的UI元素
- **RectFillComponent**：矩形填充组件，填充矩形区域
- **BorderFillComponent**：边框填充组件，专门用于创建边框装饰
- **PatternFillComponent**：模式填充组件，基于字符模式的布局
- **PaginatedComponent**：统一分页组件，支持有限分页和无限滚动
- **ScrollableComponent**：滚动组件，支持垂直滚动
- **StorageComponent**：存储组件，允许物品操作

### 📊 强大的数据处理
- **DataProvider系统**：统一的数据提供器接口
- **自动模式识别**：根据数据特性自动选择有限/无限分页
- **多种缓存策略**：单页、多页、激进缓存
- **异步数据加载**：支持本地数据和远程API
- **加载状态管理**：友好的加载中和错误状态显示

## 📚 文档导航

### 🚀 快速开始
- [快速入门](GETTING_STARTED.md) - 5分钟上手GUI模块
- [核心概念](CONCEPTS.md) - 理解设计理念和架构

### 📖 教程指南
- [第一个GUI](tutorials/01-first-gui.md) - 创建你的第一个GUI
- [组件使用](tutorials/02-components.md) - 学习各种组件的使用
- [事件处理](tutorials/03-events.md) - 响应用户交互
- [会话管理](tutorials/04-sessions.md) - 深入理解Session和页面导航
- [i18n集成](tutorials/05-i18n-integration.md) - 掌握国际化功能
- [高级功能](tutorials/06-advanced-features.md) - 调度器、异步操作等

### 📚 API参考
- [API总览](api/README.md) - 完整的API文档
- [页面API](api/pages.md) - Page相关接口
- [组件API](api/components.md) - Component相关接口
- [会话API](api/sessions.md) - Session相关接口
- [事件API](api/events.md) - 事件处理接口
- [物品API](api/items.md) - ItemUtil工具类

### 📋 开发指南
- [最佳实践](guides/best-practices.md) - 开发规范和建议
- [性能优化](guides/performance.md) - 提升GUI性能的技巧
- [错误处理](guides/error-handling.md) - 调试和错误处理
- [故障排除](guides/troubleshooting.md) - 常见问题解决

### 🎯 示例代码
- [示例总览](examples/README.md) - 所有示例的索引
- [基础示例](examples/basic/) - 简单的GUI示例
- [高级示例](examples/advanced/) - 复杂功能的实现
- [实际项目](examples/real-world/) - 真实插件的GUI实现

### 📖 完整导航
- [文档导航](NAVIGATION.md) - 完整的文档索引和学习路径

## 🚀 快速预览

### 创建简单GUI（展示i18n集成）
```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 使用推荐的openPage方法创建GUI
        openPage(InventoryType.CHEST, 27, player) {
            // 直接使用i18n模板，支持MiniMessage和Legacy格式
            title("<%gui.example.title%>")

            // 添加按钮
            slotComponent(x = 4, y = 2) {
                render {
                    item(Material.EMERALD) {
                        // 直接使用i18n模板，无需手动sprintf
                        name("<%gui.button.confirm%>")
                        lore("<%gui.button.confirm_hint%>")
                    }
                }
                onLeftClick {
                    // 需要参数替换时仍使用messager
                    player.sendMessage(plugin.messager.sprintf("<%gui.message.confirmed%>", player.name))
                    this@openPage.close() // 关闭页面
                }
            }
        }
    }
}
```

### i18n语言文件示例
```yaml
# lang/zh_CN.yml
gui:
  example:
    title: "<green>示例GUI</green>"
  button:
    confirm: "<green>确认</green>"
    confirm_hint: "<gray>点击确认操作</gray>"
  message:
    confirmed: "<green>{0} 已确认操作！</green>"
```

## 📦 依赖要求

- **Minecraft**: 1.20.1+
- **Java**: 17+
- **Core模块**: 必需（提供基础功能）
- **Paper**: 推荐（更好的性能和API支持）

## 🚀 开始使用

1. **快速上手**：阅读 [快速入门](GETTING_STARTED.md) 在5分钟内创建第一个GUI
2. **理解概念**：学习 [核心概念](CONCEPTS.md) 掌握设计理念
3. **跟随教程**：按顺序完成 [教程指南](tutorials/) 中的内容
4. **查阅参考**：使用 [API文档](api/) 查找具体接口
5. **学习示例**：参考 [示例代码](examples/) 了解最佳实践

## 🔗 相关链接

- [Core模块文档](../core/) - 了解基础功能和架构
- [项目主页](../../README.md) - 项目总览和构建指南
- [更新日志](CHANGELOG.md) - 版本更新记录和变更说明

## 💡 获取帮助

- 查看 [故障排除](guides/troubleshooting.md) 解决常见问题
- 参考 [示例代码](examples/) 学习最佳实践
- 阅读 [API文档](api/) 了解详细用法
- 遵循 [最佳实践](guides/best-practices.md) 编写高质量代码

---

**开始你的GUI模块开发之旅吧！** 🎉
