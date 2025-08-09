# GUI 模块 i18n 集成指南

GUI 模块与 Core 的 Message 模块深度集成，提供了原生的国际化支持。本指南将详细介绍如何在 GUI 中使用 i18n 功能。

## 🌟 核心特性

### 自动文本处理
GUI 模块通过 `GuiManager` 的 `textPreprocessor` 自动处理所有文本：
- **语言映射**：自动将 `<%key%>` 模板转换为对应语言的文本
- **格式解析**：支持 MiniMessage 和 Legacy 格式的自动识别和转换
- **参数替换**：支持 StringFormat 的参数替换功能

### 统一的API设计
- **直接使用模板**：在 `name()`、`lore()` 等方法中直接使用 `<%key%>` 模板
- **无需手动调用**：不再需要手动调用 `plugin.messager.sprintf()`
- **向后兼容**：仍支持传统的 messager 调用方式

## 🚀 快速开始

### 基础用法

```kotlin
// 旧方式（仍然支持）
slotComponent(0, 0) {
    render {
        item(Material.DIAMOND) {
            name(plugin.messager.sprintf("<%gui.button.confirm%>"))
            lore(plugin.messager.sprintf("<%gui.button.confirm_hint%>"))
        }
    }
}

// 新方式（推荐）
slotComponent(0, 0) {
    render {
        item(Material.DIAMOND) {
            // 直接使用i18n模板
            name("<%gui.button.confirm%>")
            lore("<%gui.button.confirm_hint%>")
        }
    }
}
```

### 语言文件配置

```yaml
# lang/zh_CN.yml
gui:
  button:
    confirm: "<green>确认</green>"
    confirm_hint: "<gray>点击确认操作</gray>"
    cancel: "<red>取消</red>"
  message:
    operation_confirmed: "<green>{0} 已确认操作！</green>"
  title:
    main_menu: "<gold>主菜单</gold>"
```

## 🔧 详细用法

### 在不同组件中使用

#### 页面标题
```kotlin
openPage(InventoryType.CHEST, 54, player) {
    // 直接使用i18n模板
    title("<%gui.title.main_menu%>")

    // 或者使用format方法进行参数替换
    title(guiManager.format("<%gui.title.player_menu%>", player.name))
}
```

#### 物品名称和描述
```kotlin
slotComponent(0, 0) {
    render {
        item(Material.EMERALD) {
            // 简单模板
            name("<%gui.button.confirm%>")

            // 多行lore
            lore("<%gui.button.confirm_hint%>")

            // 或者使用列表形式
            lore(listOf(
                "<%gui.lore.line1%>",
                "<%gui.lore.line2%>"
            ))
        }
    }
}
```

#### 头颅物品
```kotlin
slotComponent(1, 1) {
    render {
        skull(player) {
            // 直接使用i18n模板
            name("<%gui.player.info%>")
            lore("<%gui.player.click_hint%>")
        }
    }
}
```

### 参数替换

当需要参数替换时，有两种方式：

#### 方式1：使用 GuiManager.format()
```kotlin
slotComponent(0, 0) {
    render {
        item(Material.PLAYER_HEAD) {
            // 使用guiManager.format进行参数替换
            name(guiManager.format("<%gui.player.name%>", player.name))
            lore(guiManager.format("<%gui.player.level%>", player.level))
        }
    }
}
```

#### 方式2：使用传统的 messager（推荐用于复杂参数）
```kotlin
onLeftClick { context ->
    // 复杂的参数替换仍使用messager
    context.player.sendMessage(
        plugin.messager.sprintf("<%gui.message.operation_confirmed%>",
            context.player.name,
            System.currentTimeMillis()
        )
    )
}
```

### 多行文本处理

```kotlin
slotComponent(0, 0) {
    render {
        item(Material.BOOK) {
            name("<%gui.book.title%>")

            // 处理多行lore
            lore(mutableListOf<String>().apply {
                addAll(
                    plugin.messager.sprintfPlain(true, "<%gui.book.description%>",
                        player.name,
                        book.pageCount
                    ).split("\n")
                )
            })
        }
    }
}
```

## 🎯 实际示例

### 来自 ExternalBook 插件的实际用法

```kotlin
// 页面标题
plugin.openPage(
    InventoryType.CHEST,
    size = 54,
    player = player,
    title = plugin.guiManager.format("<%gui.player_books.title%>", playerName)
) {
    // 按钮
    slotComponent(0, 5) {
        render {
            urlSkull("9a2d891c6ae9f6baa040d736ab84d48344bb6b70d7f1a280dd12cbac4d777") {
                name("<%gui.player_books.add_modify%>")
            }
        }
    }

    // 分页组件
    paginatedComponent(
        startX = 0, startY = 0,
        width = 9, height = 5,
        dataProvider = PlayerBookDataProvider(plugin, playerId)
    ) {
        render { context ->
            val book = context.item ?: return@render null
            ItemStack(Material.WRITTEN_BOOK).apply {
                itemMeta = (itemMeta as BookMeta?)?.also { meta ->
                    meta.title(book.title.toComponent())
                    meta.lore(listOf(
                        plugin.messager.sprintf("<%gui.player_books.book_display.created_time%>",
                            dateFormatter.format(book.created)),
                        plugin.messager.sprintf("<%gui.player_books.book_display.left_click%>"),
                        plugin.messager.sprintf("<%gui.player_books.book_display.right_click%>")
                    ))
                }
            }
        }
    }
}
```

## 🔍 技术原理

### GuiManager 的 textPreprocessor

GUI 模块通过 `GuiManager` 构造器中的 `textPreprocessor` 实现 i18n 集成：

```kotlin
// BasePlugin 集成
constructor(plugin: BasePlugin) : this(
    plugin,
    textPreprocessor = object : IGuiTextPreprocessor {
        override fun processLegacy(text: String, parseMode: ComponentParseMode, vararg args: Any?): String =
            plugin.stringFormatter.sprintfLegacy(true, parseMode, text, *args)

        override fun processComponent(text: String, parseMode: ComponentParseMode, vararg args: Any?): Component =
            plugin.stringFormatter.sprintf(true, parseMode, text, *args)
    }
)
```

### ItemBuilder 的 format 方法

```kotlin
class ItemBuilder(private var itemStack: ItemStack, private val guiManager: GuiManager) {
    fun format(text: String, vararg args: Any, parseMode: ComponentParseMode = ComponentParseMode.Auto): Component =
        guiManager.format(text, args, parseMode = parseMode)
}
```

## 📝 最佳实践

### 1. 模板命名规范
```yaml
gui:
  [plugin_name]:
    [page_name]:
      title: "页面标题"
      [component_name]:
        name: "组件名称"
        hint: "提示信息"
```

### 2. 何时使用不同的方式
- **简单模板**：直接使用 `name("<%key%>")`
- **单个参数**：使用 `guiManager.format("<%key%>", arg)`
- **复杂参数**：使用 `plugin.messager.sprintf("<%key%>", args...)`
- **多行文本**：使用 `messager.sprintfPlain().split("\n")`

### 3. 性能考虑
- GUI 模块内置了 LRU 缓存，频繁使用的模板会被缓存
- 简单模板的处理性能优于复杂参数替换
- 推荐在组件渲染中使用简单模板，在事件处理中使用复杂替换

## 🔗 相关文档

- [Core 模块 i18n 文档](../core/i18n.md)
- [Message 模块文档](../core/message.md)
- [GUI API 参考](api/items.md)
- [实际项目示例](examples/real-world-examples.md)
