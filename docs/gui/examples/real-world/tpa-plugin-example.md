# GUI1 实际项目示例

本文档展示了GUI1模块在实际项目中的使用方式，以TPA插件为例。

## 📋 目录

- [玩家选择GUI](#玩家选择gui)
- [分页组件的使用](#分页组件的使用)
- [边框组件的实际应用](#边框组件的实际应用)
- [聊天输入功能](#聊天输入功能)
- [导航和页面管理](#导航和页面管理)

## 🎯 玩家选择GUI

### 基本结构

TPA插件的玩家选择GUI展示了如何构建一个完整的、功能丰富的界面：

```kotlin
object TPAPlayerSelectGui {
    fun openPlayerSelectGui(plugin: TPAPlugin, player: Player, type: TPARequestType) {
        val config = plugin.tpaManager.config
        val guiConfig = config.gui

        plugin.openPage(
            InventoryType.CHEST,
            size = 54,
            player = player,
            title = plugin.messager.sprintf("<%gui.player_select.title%>", getTypeDisplayName(plugin, type))
        ) {
            // 1. 创建边框
            borderFillComponent(1, 1, 7, 4) {
                fill(Material.GRAY_STAINED_GLASS_PANE)
            }

            // 2. 获取并处理数据
            val onlinePlayers = Bukkit.getOnlinePlayers()
                .filter { it != player }
                .let { players ->
                    if (guiConfig.sortPlayersByName) {
                        players.sortedBy { it.name }
                    } else {
                        players.toList()
                    }
                }

            // 3. 处理空数据情况
            if (onlinePlayers.isEmpty()) {
                slotComponent(x = 4, y = 2) {
                    render {
                        ItemUtil.create(
                            Material.BARRIER,
                            name = plugin.messager.sprintf("<%gui.player_select.no_players%>"),
                            lore = listOf(plugin.messager.sprintf("<%gui.player_select.no_players_desc%>"))
                        )
                    }
                }
                return@openPage
            }

            // 4. 创建分页组件
            val userListComponent = createPlayerList(plugin, player, type, onlinePlayers)

            // 5. 添加导航按钮
            addNavigationButtons(plugin, userListComponent)

            // 6. 添加关闭按钮
            addCloseButton(plugin)
        }
    }
}
```

### 关键设计模式

1. **配置驱动**：使用配置文件控制GUI行为
2. **数据预处理**：在创建组件前处理和排序数据
3. **空状态处理**：优雅处理没有数据的情况
4. **模块化组件**：将复杂逻辑拆分为独立函数

## 🔄 分页组件的使用

### 实际应用模式

```kotlin
// 创建分页组件的实际模式
val userListComponent = paginatedComponent(
    startX = 1, startY = 1, 
    width = 7, height = 4, 
    data = onlinePlayers
) {
    render { cxt ->
        ItemUtil.skull(cxt.item!!) {
            name(plugin.messager.sprintf("<%gui.player_select.player_name%>", cxt.item!!.name))
            // 动态构建lore
            lore(buildPlayerLore(plugin, cxt.item!!, type, guiConfig))
        }
    }
    
    // 主要操作
    onLeftClick { cxt, index, targetPlayer ->
        val success = plugin.tpaManager.sendRequest(player, targetPlayer!!, type)
        if (success) {
            this@openPage.close()
        }
    }
    
    // 次要操作
    onShiftLeftClick { cxt, index, targetPlayer ->
        val success = plugin.tpaManager.blockManager.blockPlayer(player, targetPlayer!!)
        if (success) {
            plugin.messager.printf(player, "<%block.player_blocked%>", targetPlayer.name)
            // 重新打开GUI以刷新列表
            openPlayerSelectGui(plugin, player, type)
        }
    }
}

// 动态构建lore的辅助函数
private fun buildPlayerLore(
    plugin: TPAPlugin, 
    targetPlayer: Player, 
    type: TPARequestType, 
    guiConfig: TPAGuiConfig
): List<String> {
    return mutableListOf<String>().apply {
        add(plugin.messager.sprintf("<%gui.player_select.click_to_send%>", getTypeDisplayName(plugin, type)))
        add(plugin.messager.sprintf("<%gui.player_select.shift_click_to_block%>"))
        
        if (guiConfig.showWorldInfo) {
            add(plugin.messager.sprintf("<%gui.player_select.world%>", targetPlayer.world.name))
        }
        
        if (guiConfig.showDistanceInfo) {
            add(plugin.messager.sprintf("<%gui.player_select.distance%>", getDistance(plugin, player, targetPlayer)))
        }
    }
}
```

### 分页导航的实现

```kotlin
// 条件性显示翻页按钮
if (userListComponent.hasPreviousPage()) {
    slotComponent(0, 5) {
        render {
            ItemUtil.urlSkull(
                "37aee9a75bf0df7897183015cca0b2a7d755c63388ff01752d5f4419fc645",
                name = plugin.messager.sprintf("<%gui.common.previous_page%>")
            )
        }
        onLeftClick { _, _, _ ->
            userListComponent.previousPage()
        }
    }
}

if (userListComponent.hasNextPage()) {
    slotComponent(8, 5) {
        render {
            ItemUtil.urlSkull(
                "682ad1b9cb4dd21259c0d75aa315ff389c3cef752be3949338164bac84a96e",
                name = plugin.messager.sprintf("<%gui.common.next_page%>")
            )
        }
        onLeftClick { _, _, _ ->
            userListComponent.nextPage()
        }
    }
}
```

## 🖼️ 边框组件的实际应用

### 简单边框模式

TPA插件使用了最简单有效的边框模式：

```kotlin
// 实际项目中的边框使用 - 简单而有效
borderFillComponent(1, 1, 7, 4) {
    fill(Material.GRAY_STAINED_GLASS_PANE)
}
```

这种模式的优点：
- **简洁明了**：代码简单易懂
- **性能良好**：渲染开销小
- **视觉清晰**：为内容区域提供明确边界
- **易于维护**：不需要复杂的配置

### 与内容组件的配合

```kotlin
// 边框定义了内容区域
borderFillComponent(1, 1, 7, 4) {
    fill(Material.GRAY_STAINED_GLASS_PANE)
}

// 分页组件使用相同的区域参数
paginatedComponent(startX=1, startY=1, width=7, height=4, data=items) {
    // 内容渲染...
}
```

## 💬 聊天输入功能

### 实际使用场景

TPA插件在屏蔽列表管理中使用了聊天输入功能：

```kotlin
slotComponent(4, 5) {
    render {
        ItemUtil.create(
            Material.NAME_TAG,
            name = plugin.messager.sprintf("<%gui.blocklist.add_by_name%>"),
            lore = listOf(
                plugin.messager.sprintf("<%gui.blocklist.add_by_name_desc%>"),
                "",
                plugin.messager.sprintf("<%gui.blocklist.add_by_name_instruction%>")
            )
        )
    }
    
    onLeftClick { _, _, _ ->
        // 使用聊天输入添加黑名单
        this@openPage.chatInput(hide = true) { input ->
            when {
                input.equals("cancel", ignoreCase = true) -> {
                    plugin.messager.printf(player, "<%gui.blocklist.add_cancelled%>")
                    this@openPage.show()
                    true // 结束输入
                }
                input.trim().isEmpty() -> {
                    plugin.messager.printf(player, "<%error.player_name_empty%>")
                    false // 继续等待输入
                }
                input.trim().length > 16 -> {
                    plugin.messager.printf(player, "<%error.player_name_too_long%>")
                    false // 继续等待输入
                }
                else -> {
                    // 处理有效输入
                    handlePlayerNameInput(plugin, player, input.trim())
                    this@openPage.show()
                    true // 结束输入
                }
            }
        }.also { result ->
            if (result) {
                plugin.messager.printf(player, "<%gui.blocklist.enter_player_name%>")
            } else {
                plugin.messager.printf(player, "<%error.chat_input_busy%>")
            }
        }
    }
}
```

### 聊天输入的最佳实践

1. **输入验证**：始终验证用户输入
2. **取消机制**：提供取消输入的方式
3. **错误处理**：对无效输入给出明确提示
4. **状态管理**：正确处理输入开始和结束状态
5. **用户反馈**：及时告知用户当前状态

## 🧭 导航和页面管理

### 页面关闭模式

```kotlin
// 简单关闭 - 关闭当前页面
slotComponent(0, 8) {
    render {
        ItemUtil.create(Material.BARRIER, name = plugin.messager.sprintf("<%gui.common.close%>"))
    }
    onLeftClick { _, _, _ ->
        this@openPage.close()
    }
}
```

### 页面回退模式

```kotlin
// 智能回退 - 回到上一页或关闭
slotComponent(8, 8) {
    render {
        ItemUtil.create(Material.BARRIER, name = plugin.messager.sprintf("<%gui.common.close%>"))
    }
    onLeftClick { _, _, _ ->
        this@openPage.back() // 如果有上一页则回退，否则关闭
    }
}
```

### 页面刷新模式

```kotlin
// 刷新当前页面 - 重新打开相同的GUI
onShiftLeftClick { cxt, index, targetPlayer ->
    val success = plugin.tpaManager.blockManager.blockPlayer(player, targetPlayer!!)
    if (success) {
        plugin.messager.printf(player, "<%block.player_blocked%>", targetPlayer.name)
        // 重新打开GUI以刷新数据
        openPlayerSelectGui(plugin, player, type)
    }
}
```

## 🎯 总结

TPA插件的GUI实现展示了GUI1模块的实际应用模式：

1. **简洁有效**：使用最简单的方式实现功能
2. **配置驱动**：通过配置控制GUI行为
3. **用户友好**：提供清晰的反馈和操作指引
4. **错误处理**：优雅处理各种异常情况
5. **性能优化**：避免不必要的复杂性

这些模式可以作为其他项目的参考和模板。
