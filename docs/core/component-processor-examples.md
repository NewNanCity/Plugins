# ComponentProcessor 使用示例

本文档展示了修复后的 `ComponentProcessor` 的正确用法，现在它返回真正的 `Component` 对象而不是字符串。

## 📋 目录

- [基本用法](#基本用法)
- [扩展函数](#扩展函数)
- [发送消息](#发送消息)
- [与其他API集成](#与其他api集成)

## 🎯 基本用法

### 从Legacy文本创建Component

```kotlin
import city.newnan.core.utils.text.ComponentProcessor
import net.kyori.adventure.text.Component

class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 基本用法 - 返回真正的Component对象
        val component: Component = ComponentProcessor.fromLegacy("§aHello World!§r")

        // 使用自定义颜色代码字符
        val component2: Component = ComponentProcessor.fromLegacy("&aHello World!&r", '&')

        // 验证返回的是Component对象
        logger.info("Component class: ${component::class.simpleName}")
        logger.info("Component content: ${component}")
    }
}
```

### 将Component转换为Legacy文本

```kotlin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 创建一个Component
        val component = Component.text("Hello World!")
            .color(NamedTextColor.GREEN)
            .append(Component.text(" - Welcome!").color(NamedTextColor.YELLOW))

        // 转换为Legacy文本
        val legacyText: String = ComponentProcessor.toLegacy(component)
        logger.info("Legacy text: $legacyText")

        // 使用自定义颜色代码字符
        val customLegacy: String = ComponentProcessor.toLegacy(component, '&')
        logger.info("Custom legacy text: $customLegacy")
    }
}
```

## 🔧 扩展函数

### 字符串扩展

```kotlin
import city.newnan.core.utils.text.toComponent
import city.newnan.core.utils.text.toLegacy

class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 字符串转Component（使用扩展函数）
        val component1: Component = "§aHello World!".toComponent()
        val component2: Component = "&bHello World!".toComponent('&')

        // Component转Legacy文本（使用扩展函数）
        val legacy1: String = component1.toLegacy()
        val legacy2: String = component1.toLegacy('&')

        logger.info("Original: §aHello World!")
        logger.info("Component: $component1")
        logger.info("Back to legacy: $legacy1")
    }
}
```

### CommandSender扩展

```kotlin
import city.newnan.core.utils.text.sendComponent
import city.newnan.core.utils.text.sendLegacy
import city.newnan.core.utils.text.toComponent

class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 注册命令示例
        commands<PlayerCommandSender> {
            command("test") {
                handler { context ->
                    val player = context.sender.player

                    // 发送Component消息
                    val component = "§aWelcome to the server!".toComponent()
                    player.sendComponent(component)

                    // 发送Legacy消息（便利方法）
                    player.sendLegacy("§bThis is a legacy message!")

                    // 发送给多个玩家
                    val players = listOf(player, /* 其他玩家 */)
                    players.sendComponent("§cBroadcast message!".toComponent())
                }
            }
        }
    }
}
```

## 📨 发送消息

### 单个接收者

```kotlin
class MyPlugin : BasePlugin() {
    fun sendWelcomeMessage(player: Player) {
        // 方法1：直接使用ComponentProcessor
        val welcomeComponent = ComponentProcessor.fromLegacy("§a欢迎来到服务器！")
        ComponentProcessor.sendComponentMessage(player, welcomeComponent)

        // 方法2：使用扩展函数（推荐）
        player.sendComponent("§a欢迎来到服务器！".toComponent())

        // 方法3：使用便利方法
        player.sendLegacy("§a欢迎来到服务器！")
    }
}
```

### 多个接收者

```kotlin
class MyPlugin : BasePlugin() {
    fun broadcastMessage(message: String) {
        val onlinePlayers = server.onlinePlayers

        // 方法1：直接使用ComponentProcessor
        val component = ComponentProcessor.fromLegacy(message)
        ComponentProcessor.sendComponentMessage(onlinePlayers, component)

        // 方法2：使用扩展函数（推荐）
        onlinePlayers.sendComponent(message.toComponent())

        // 方法3：使用便利方法
        onlinePlayers.sendLegacy(message)
    }
}
```

## 🔗 与其他API集成

### 与Adventure API集成

```kotlin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 创建复杂的Component
        val complexComponent = Component.text()
            .append(Component.text("服务器").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
            .append(Component.text(" - ").color(NamedTextColor.GRAY))
            .append(Component.text("欢迎您！").color(NamedTextColor.GREEN))
            .build()

        // 转换为Legacy格式用于配置文件或数据库存储
        val legacyForStorage = ComponentProcessor.toLegacy(complexComponent)
        logger.info("Storing in config: $legacyForStorage")

        // 从配置文件读取并转换回Component
        val restoredComponent = ComponentProcessor.fromLegacy(legacyForStorage)

        // 发送给玩家
        server.onlinePlayers.forEach { player ->
            player.sendComponent(restoredComponent)
        }
    }
}
```

### 与GUI模块集成

```kotlin
import city.newnan.gui.extensions.openPage
import city.newnan.core.utils.text.toComponent

class MyPlugin : BasePlugin() {
    fun createColorfulGUI(player: Player) {
        player.openPage(InventoryType.CHEST, 27, "§6彩色GUI示例".toComponent()) {
            slotComponent(x = 4, y = 1) {
                render { context ->
                    itemUtil.createItemStack(Material.DIAMOND) {
                        // 使用Component设置物品名称
                        name("§b钻石".toComponent())
                        lore(
                            "§7这是一个".toComponent(),
                            "§a彩色的物品".toComponent(),
                            "§c点击测试".toComponent()
                        )
                    }
                }

                onClick { context ->
                    // 发送彩色消息
                    context.player.sendComponent("§a你点击了钻石！".toComponent())
                }
            }
        }
    }
}
```

### 与消息系统集成

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 使用MessageManager和ComponentProcessor
        messageManager.printf(
            player,
            "welcome.message", // 配置中的键
            "§a欢迎 §b%s §a来到服务器！".toComponent(), // 使用Component作为格式
            player.name
        )
    }
}
```

## 🎨 高级用法

### 自定义颜色代码

```kotlin
class MyPlugin : BasePlugin() {
    fun handleCustomColorCodes() {
        // 支持不同的颜色代码字符
        val ampersandText = "&a&lHello &b&oWorld!"
        val component = ComponentProcessor.fromLegacy(ampersandText, '&')

        // 转换为标准的§格式
        val standardLegacy = ComponentProcessor.toLegacy(component, '§')
        logger.info("Standard format: $standardLegacy")

        // 转换为自定义格式
        val customLegacy = ComponentProcessor.toLegacy(component, '#')
        logger.info("Custom format: $customLegacy")
    }
}
```

### 错误处理

```kotlin
class MyPlugin : BasePlugin() {
    fun safeComponentProcessing() {
        try {
            // ComponentProcessor内部已经有错误处理
            val component = ComponentProcessor.fromLegacy("§invalid§text")
            player.sendComponent(component)
        } catch (e: Exception) {
            // 一般不会到达这里，因为ComponentProcessor有内部错误处理
            logger.error("Component processing failed", e)
            player.sendMessage("消息发送失败")
        }
    }
}
```

## 📝 最佳实践

1. **优先使用扩展函数**：`"text".toComponent()` 比 `ComponentProcessor.fromLegacy("text")` 更简洁
2. **类型安全**：现在返回真正的 `Component` 对象，享受类型安全的好处
3. **性能考虑**：Component对象可以缓存和重用
4. **向后兼容**：提供了Legacy便利方法，方便迁移现有代码
5. **错误处理**：ComponentProcessor内部已处理错误，无需额外try-catch

这个修复确保了 `ComponentProcessor` 返回真正的 `Component` 对象，提供了类型安全和现代化的API体验。
