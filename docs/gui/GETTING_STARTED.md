# GUI 模块快速入门

欢迎使用 GUI 模块！本指南将在5分钟内带你创建第一个支持 i18n 的GUI界面。

## 📋 前置条件

确保你的项目已经：
1. 继承了 `BasePlugin` 类
2. 添加了 `gui` 模块依赖
3. 运行在 Minecraft 1.20.1+ 环境
4. 配置了 i18n 语言文件（可选，但推荐）

## 🌐 准备语言文件

创建语言文件 `lang/zh_CN.yml`：
```yaml
gui:
  main_menu:
    title: "<gold>主菜单</gold>"
  button:
    navigation: "<yellow>导航</yellow>"
    navigation_hint: "<gray>点击打开导航菜单</gray>"
    close: "<red>关闭</red>"
    close_hint: "<gray>点击关闭菜单</gray>"
  message:
    navigation_clicked: "<green>{0} 点击了导航按钮！</green>"
```

## 🚀 创建第一个GUI

### 1. 基础设置

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // GUI模块会自动初始化，无需额外设置
        // 自动集成 i18n 和 message 模块
        logger.info("插件启用，GUI模块已就绪！")
    }
}
```

### 2. 创建支持 i18n 的GUI

```kotlin
// 在命令或事件中创建GUI
class MyPlugin : BasePlugin() {
    fun showMainMenu(player: Player) {
        openPage(InventoryType.CHEST, 27, player) {
            // 直接使用i18n模板，支持MiniMessage格式
            title("<%gui.main_menu.title%>")

            // 添加一个按钮
            slotComponent(x = 4, y = 1) { // 中间位置
                render {
                    item(Material.COMPASS) {
                        // 直接使用i18n模板，无需手动调用messager
                        name("<%gui.button.navigation%>")
                        lore("<%gui.button.navigation_hint%>")
                    }
                }

                onLeftClick {
                    // 需要参数替换时使用messager
                    player.sendMessage(
                        messager.sprintf("<%gui.message.navigation_clicked%>", player.name)
                    )
                }
            }

            // 添加关闭按钮
            slotComponent(x = 8, y = 2) {
                render {
                    item(Material.BARRIER) {
                        name("<%gui.button.close%>")
                        lore("<%gui.button.close_hint%>")
                    }
                }
                onLeftClick {
                    // 关闭当前页面
                    this@openPage.close()
                }
            }
        }
    }
}
```

## 🎯 核心概念速览

### 坐标系统
```kotlin
// x: 横坐标 (0-8)，y: 纵坐标 (0-5)
slotComponent(x = 4, y = 2) { /* 中间位置 */ }
slotComponent(x = 0, y = 0) { /* 左上角 */ }
slotComponent(x = 8, y = 5) { /* 右下角 */ }
```

### 物品渲染（i18n 支持）
```kotlin
render {
    // 使用item方法创建物品，支持i18n
    item(Material.DIAMOND) {
        // 直接使用i18n模板
        name("<%gui.item.diamond%>")
        lore("<%gui.item.diamond_description%>")

        // 或使用format进行参数替换
        addLore(format("<%gui.item.durability%>", 100))

        enchant(Enchantment.DURABILITY, 1)
        hideEnchants()
    }
}
```

### 事件处理
```kotlin
onLeftClick { context ->
    val player = context.player
    val clickType = context.clickType
    player.sendMessage("左键点击！")
}

onRightClick { context ->
    player.sendMessage("右键点击！")
}

onShiftClick { context ->
    player.sendMessage("Shift+点击！")
}
```

## 🔧 常用模式

### 确认对话框
```kotlin
fun showConfirmDialog(player: Player, message: String, onConfirm: () -> Unit) {
    openPage(InventoryType.HOPPER, player = player) {
        title("&c确认操作")

        // 确认按钮
        slotComponent(x = 1, y = 0) {
            render {
                item(Material.GREEN_WOOL) {
                    name("&a确认")
                    lore("&7$message")
                }
            }
            onLeftClick {
                onConfirm()
                this@openPage.close()
            }
        }

        // 取消按钮
        slotComponent(x = 3, y = 0) {
            render {
                item(Material.RED_WOOL) {
                    name("&c取消")
                }
            }
            onLeftClick {
                this@openPage.close()
                player.sendMessage("&7操作已取消")
            }
        }
    }
}
```

### 返回按钮
```kotlin
// 在任何GUI中添加返回按钮
slotComponent(x = 0, y = 5) {
    render {
        item(Material.ARROW) {
            name("&7返回")
        }
    }
    onLeftClick {
        // 返回上一页
        page.session.pop()
    }
}
```

## 📱 会话管理

### 页面导航
```kotlin
// 打开新页面（推荐方法）
fun openSubMenu(player: Player) {
    // 使用推荐的openPage方法，自动压入栈并显示
    openPage(InventoryType.CHEST, 27, player) {
        title("&e子菜单")
        // 配置子菜单...

        slotComponent(x = 4, y = 2) {
            render {
                item(Material.BOOK) {
                    name("&b返回主菜单")
                }
            }
            onLeftClick {
                // 返回主菜单
                this@openPage.close()
            }
        }
    }
}
```

## ⚡ 下一步

现在你已经掌握了基础用法！继续学习：

1. [核心概念](CONCEPTS.md) - 深入理解GUI模块的设计理念
2. [第一个GUI](tutorials/01-first-gui.md) - 更详细的入门教程
3. [组件使用](tutorials/02-components.md) - 学习更多强大的组件
4. [事件处理](tutorials/03-events.md) - 掌握复杂的交互逻辑
5. [最佳实践](guides/best-practices.md) - 编写高质量的GUI代码

## 🆘 遇到问题？

- 查看 [故障排除](guides/troubleshooting.md)
- 参考 [示例代码](examples/)
- 检查控制台日志中的错误信息

祝你使用愉快！🎉
