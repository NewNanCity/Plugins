# 教程1：创建第一个GUI

本教程将详细指导你创建第一个GUI界面，包括基础设置、组件添加和事件处理。

## 📋 学习目标

完成本教程后，你将学会：
- 创建基本的GUI页面
- 添加按钮组件
- 处理点击事件
- 使用i18n国际化
- 管理页面生命周期

## 🔧 前置条件

- 已完成 [快速入门](../GETTING_STARTED.md)
- 理解 [核心概念](../CONCEPTS.md) 中的基本概念
- 项目已正确配置GUI模块依赖

## 🎯 项目结构

我们将创建一个简单的主菜单GUI，包含以下功能：
- 玩家信息显示
- 设置按钮
- 帮助按钮
- 关闭按钮

## 📝 步骤1：准备语言文件

首先创建语言文件 `lang/zh_CN.yml`：

```yaml
gui:
  main_menu:
    title: "<gold>主菜单</gold>"
  button:
    player_info: "<yellow>玩家信息</yellow>"
    player_info_hint: "<gray>查看你的详细信息</gray>"
    settings: "<blue>设置</blue>"
    settings_hint: "<gray>打开设置菜单</gray>"
    help: "<green>帮助</green>"
    help_hint: "<gray>查看帮助信息</gray>"
    close: "<red>关闭</red>"
    close_hint: "<gray>关闭菜单</gray>"
  message:
    player_info_clicked: "<green>显示玩家信息...</green>"
    settings_clicked: "<blue>打开设置菜单...</blue>"
    help_clicked: "<green>显示帮助信息...</green>"
    menu_closed: "<gray>菜单已关闭</gray>"
  player:
    name: "<yellow>{0}</yellow>"
    level: "<gray>等级: {0}</gray>"
    health: "<red>生命值: {0}/{1}</red>"
    location: "<gray>位置: {0}, {1}, {2}</gray>"
```

## 📝 步骤2：创建主菜单类

```kotlin
class MainMenuGui(private val plugin: MyPlugin) {
    
    fun openMainMenu(player: Player) {
        plugin.openPage(InventoryType.CHEST, 27, player) {
            // 设置标题，使用i18n
            title("<%gui.main_menu.title%>")
            
            // 添加玩家信息按钮
            addPlayerInfoButton(player)
            
            // 添加功能按钮
            addSettingsButton(player)
            addHelpButton(player)
            addCloseButton(player)
            
            // 添加装饰边框
            addDecorationBorder()
        }
    }
    
    private fun PageBuilder.addPlayerInfoButton(player: Player) {
        slotComponent(x = 2, y = 1) {
            render {
                skull(player) {
                    name("<%gui.button.player_info%>")
                    lore(
                        "<%gui.button.player_info_hint%>",
                        "",
                        plugin.messager.sprintf("<%gui.player.name%>", player.name),
                        plugin.messager.sprintf("<%gui.player.level%>", player.level),
                        plugin.messager.sprintf("<%gui.player.health%>", 
                            player.health.toInt(), 
                            player.maxHealth.toInt()
                        )
                    )
                }
            }
            
            onLeftClick {
                player.sendMessage(plugin.messager.sprintf("<%gui.message.player_info_clicked%>"))
                // 这里可以打开详细的玩家信息页面
                openPlayerDetailPage(player)
            }
        }
    }
    
    private fun PageBuilder.addSettingsButton(player: Player) {
        slotComponent(x = 4, y = 1) {
            render {
                item(Material.REDSTONE) {
                    name("<%gui.button.settings%>")
                    lore("<%gui.button.settings_hint%>")
                }
            }
            
            onLeftClick {
                player.sendMessage(plugin.messager.sprintf("<%gui.message.settings_clicked%>"))
                // 这里可以打开设置页面
                openSettingsPage(player)
            }
        }
    }
    
    private fun PageBuilder.addHelpButton(player: Player) {
        slotComponent(x = 6, y = 1) {
            render {
                item(Material.BOOK) {
                    name("<%gui.button.help%>")
                    lore("<%gui.button.help_hint%>")
                }
            }
            
            onLeftClick {
                player.sendMessage(plugin.messager.sprintf("<%gui.message.help_clicked%>"))
                // 这里可以打开帮助页面
                openHelpPage(player)
            }
        }
    }
    
    private fun PageBuilder.addCloseButton(player: Player) {
        slotComponent(x = 8, y = 2) {
            render {
                item(Material.BARRIER) {
                    name("<%gui.button.close%>")
                    lore("<%gui.button.close_hint%>")
                }
            }
            
            onLeftClick {
                player.sendMessage(plugin.messager.sprintf("<%gui.message.menu_closed%>"))
                this@openPage.close()
            }
        }
    }
    
    private fun PageBuilder.addDecorationBorder() {
        // 添加顶部边框
        lineFillComponent(x = 0, y = 0, width = 9, height = 1) {
            render {
                item(Material.GRAY_STAINED_GLASS_PANE) {
                    name(" ") // 空名称
                }
            }
        }
        
        // 添加底部边框
        lineFillComponent(x = 0, y = 2, width = 9, height = 1) {
            render {
                item(Material.GRAY_STAINED_GLASS_PANE) {
                    name(" ")
                }
            }
        }
    }
    
    // 占位方法，实际项目中需要实现
    private fun openPlayerDetailPage(player: Player) {
        player.sendMessage("&7玩家详情页面开发中...")
    }
    
    private fun openSettingsPage(player: Player) {
        player.sendMessage("&7设置页面开发中...")
    }
    
    private fun openHelpPage(player: Player) {
        player.sendMessage("&7帮助页面开发中...")
    }
}
```

## 📝 步骤3：在插件中使用

```kotlin
class MyPlugin : BasePlugin() {
    
    private lateinit var mainMenuGui: MainMenuGui
    
    override fun onPluginEnable() {
        // 初始化GUI
        mainMenuGui = MainMenuGui(this)
        
        // 注册命令
        registerCommand()
    }
    
    private fun registerCommand() {
        // 使用CommandAPI注册命令
        CommandAPICommand("menu")
            .withShortDescription("打开主菜单")
            .executesPlayer { player, _ ->
                mainMenuGui.openMainMenu(player)
            }
            .register()
    }
}
```

## 🎯 运行测试

1. 启动服务器
2. 在游戏中执行 `/menu` 命令
3. 观察GUI是否正确显示
4. 测试各个按钮的点击功能
5. 检查i18n文本是否正确显示

## 📚 知识点总结

### 1. GUI创建模式
- 使用 `openPage()` 方法创建页面
- 通过DSL语法配置组件
- 自动管理页面生命周期

### 2. 组件使用
- `slotComponent()` 创建单槽组件
- `lineFillComponent()` 创建线性填充
- 每个组件都有独立的渲染和事件处理

### 3. i18n集成
- 直接在 `name()` 和 `lore()` 中使用i18n模板
- 需要参数替换时使用 `messager.sprintf()`
- 支持MiniMessage和Legacy格式

### 4. 事件处理
- 使用 `onLeftClick` 等方法处理事件
- 事件上下文提供详细信息
- 支持事件冒泡机制

## ⚡ 下一步

- [教程2：组件使用](02-components.md) - 学习更多组件类型
- [教程3：事件处理](03-events.md) - 深入了解事件系统
- [最佳实践](../guides/best-practices.md) - 编写高质量代码

## 🔗 相关链接

- [组件API](../api/components.md) - 详细的组件API文档
- [事件API](../api/events.md) - 事件处理API参考
- [基础示例](../examples/basic/) - 更多基础示例代码
