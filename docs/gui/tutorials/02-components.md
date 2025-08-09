# 教程2：组件使用详解

本教程将详细介绍GUI模块中各种组件的使用方法，帮助你构建复杂而美观的界面。

## 📋 学习目标

完成本教程后，你将学会：
- 使用各种类型的组件
- 创建复杂的布局
- 实现边框和装饰效果
- 使用分页和滚动组件
- 优化组件性能

## 🔧 前置条件

- 已完成 [教程1：创建第一个GUI](01-first-gui.md)
- 理解基本的坐标系统和渲染概念

## 🧩 组件类型概览

GUI模块提供以下组件类型：

### 基础组件
- **SingleSlotComponent** - 单槽组件
- **RectFillComponent** - 矩形填充组件
- **LineFillComponent** - 线性填充组件

### 布局组件
- **BorderFillComponent** - 边框组件
- **PatternFillComponent** - 模式填充组件

### 高级组件
- **PaginatedComponent** - 分页组件
- **ScrollableComponent** - 滚动组件
- **StorageComponent** - 存储组件

## 📝 基础组件详解

### 1. 单槽组件 (SingleSlotComponent)

最基础的组件，用于在单个槽位放置物品：

```kotlin
slotComponent(x = 4, y = 2) {
    render {
        item(Material.DIAMOND) {
            name("&b钻石")
            lore("&7珍贵的宝石")
            amount(5)
        }
    }

    onLeftClick {
        player.sendMessage("&a点击了钻石！")
    }
}
```

### 2. 矩形填充组件 (RectFillComponent)

用于填充矩形区域：

```kotlin
rectFillComponent(x = 1, y = 1, width = 7, height = 3) {
    render {
        item(Material.GRAY_STAINED_GLASS_PANE) {
            name(" ") // 空名称，作为装饰
        }
    }

    onClick { context ->
        context.player.sendMessage("&7点击了背景区域")
    }
}
```

### 3. 线性填充组件 (LineFillComponent)

用于创建水平或垂直线条：

```kotlin
// 水平分隔线
lineFillComponent(x = 0, y = 2, width = 9, height = 1) {
    render {
        item(Material.BLACK_STAINED_GLASS_PANE) {
            name(" ")
        }
    }
}

// 垂直分隔线
lineFillComponent(x = 4, y = 0, width = 1, height = 6) {
    render {
        item(Material.BLACK_STAINED_GLASS_PANE) {
            name(" ")
        }
    }
}
```

## 🎨 布局组件详解

### 1. 边框组件 (BorderFillComponent)

专门用于创建边框效果：

```kotlin
// 创建完整边框
borderFillComponent(x = 0, y = 0, width = 9, height = 6) {
    // 统一边框材料
    fillMaterial(Material.GRAY_STAINED_GLASS_PANE, "&7边框")

    onClick { context ->
        context.player.sendMessage("&e点击了边框")
    }
}

// 创建彩色边框
borderFillComponent(x = 1, y = 1, width = 7, height = 4) {
    borderMaterials(
        topMaterial = Material.RED_STAINED_GLASS_PANE,
        bottomMaterial = Material.BLUE_STAINED_GLASS_PANE,
        leftMaterial = Material.GREEN_STAINED_GLASS_PANE,
        rightMaterial = Material.YELLOW_STAINED_GLASS_PANE,
        cornerMaterial = Material.BLACK_STAINED_GLASS_PANE
    )
}

// 创建部分边框（只有上下边）
borderFillComponent(x = 0, y = 0, width = 9, height = 6,
    fillTop = true,
    fillBottom = true,
    fillLeft = false,
    fillRight = false
) {
    fillMaterial(Material.BLUE_STAINED_GLASS_PANE, "&b水平分隔线")
}
```

### 2. 模式填充组件 (PatternFillComponent)

基于字符模式创建复杂布局：

```kotlin
patternFillComponent(
    pattern = listOf(
        "aaaaaaaaa",
        "a       a",
        "a   b   a",
        "a       a",
        "aaaaaaaaa"
    )
) {
    // 设置边框
    setItem('a') {
        item(Material.GRAY_STAINED_GLASS_PANE) {
            name(" ")
        }
    }

    // 设置中心按钮
    setItem('b') {
        item(Material.EMERALD) {
            name("&a中心按钮")
            lore("&7点击执行操作")
        }
    }

    // 为特定字符设置事件
    setClickHandler('b') { context ->
        context.player.sendMessage("&a点击了中心按钮！")
    }
}
```

## 🚀 高级组件详解

### 1. 分页组件 (PaginatedComponent)

用于显示大量数据，支持分页：

```kotlin
// 有限分页示例
val playerList = server.onlinePlayers.toList()
paginatedComponent(x = 0, y = 1, width = 9, height = 4, data = playerList) {
    render { context ->
        val player = context.item ?: return@render null
        skull(player) {
            name("&e${player.name}")
            lore(
                "&7等级: ${player.level}",
                "&7生命值: ${player.health.toInt()}/${player.maxHealth.toInt()}",
                "",
                "&a点击查看详情"
            )
        }
    }

    onLeftClick { context, index, clickedPlayer ->
        context.player.sendMessage("&a点击了玩家: ${clickedPlayer?.name}")
        // 打开玩家详情页面
    }

    // 添加分页控制按钮
    addPaginationControls()
}

// 无限分页示例（适用于大数据量）
paginatedComponent<RankEntry>(x = 0, y = 1, width = 9, height = 4) {
    setDataProvider(DataProviders.infiniteApi { page, pageSize ->
        // 调用API获取排行榜数据
        rankingApi.getTopPlayers(page, pageSize)
    })

    render { context ->
        val entry = context.item ?: return@render null
        val rank = context.globalIndex + 1

        skull(entry.playerUUID) {
            name("&6#$rank ${entry.playerName}")
            lore(
                "&7积分: ${entry.score}",
                "&7排名: #$rank"
            )
        }
    }

    // 加载中状态
    renderLoadingSlot { context ->
        item(Material.CLOCK) {
            name("&e加载中...")
            lore("&7正在获取数据...")
        }
    }
}
```

### 2. 滚动组件 (ScrollableComponent)

用于垂直滚动显示内容：

```kotlin
scrollableComponent(x = 2, y = 1, width = 5, height = 4) {
    totalItems(50) // 总共50个项目

    render { index ->
        item(Material.PAPER) {
            name("&e项目 #${index + 1}")
            lore(
                "&7这是第 ${index + 1} 个项目",
                "&7点击查看详情"
            )
        }
    }

    onItemClick { context, index ->
        context.player.sendMessage("&a点击了项目 #${index + 1}")
    }

    // 添加滚动控制
    addScrollControls()
}
```

### 3. 存储组件 (StorageComponent)

允许玩家操作物品的组件：

```kotlin
storageComponent(x = 1, y = 1, width = 7, height = 4) {
    // 设置初始物品
    setInitialItems(playerInventoryItems)

    // 允许的操作
    allowPickup(true)
    allowPlace(true)
    allowDrag(true)

    // 物品过滤器
    itemFilter { item ->
        // 只允许特定类型的物品
        item.type in listOf(Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT)
    }

    onItemChanged { context, oldItem, newItem ->
        context.player.sendMessage("&7物品已更改: ${oldItem?.type} -> ${newItem?.type}")
    }
}
```

## 🎯 组件组合示例

### 创建复杂的商店界面

```kotlin
fun createShopGui(player: Player) {
    openPage(InventoryType.CHEST, 54, player) {
        title("&6商店")

        // 顶部装饰边框
        borderFillComponent(0, 0, 9, 1) {
            fillMaterial(Material.YELLOW_STAINED_GLASS_PANE, "&6商店")
        }

        // 左侧分类菜单
        rectFillComponent(0, 1, 2, 4) {
            render {
                item(Material.CHEST) {
                    name("&e分类")
                    lore("&7选择商品分类")
                }
            }
        }

        // 中间商品展示区域
        paginatedComponent(x = 2, y = 1, width = 5, height = 4, data = shopItems) {
            render { context ->
                val item = context.item ?: return@render null
                item(item.material) {
                    name("&e${item.name}")
                    lore(
                        "&7价格: &6${item.price} 金币",
                        "&7库存: &a${item.stock}",
                        "",
                        "&a左键购买 &7| &c右键预览"
                    )
                }
            }

            onLeftClick { context, index, shopItem ->
                // 购买逻辑
                purchaseItem(context.player, shopItem!!)
            }

            onRightClick { context, index, shopItem ->
                // 预览逻辑
                previewItem(context.player, shopItem!!)
            }
        }

        // 右侧玩家信息
        slotComponent(7, 1) {
            render {
                skull(player) {
                    name("&e${player.name}")
                    lore(
                        "&7金币: &6${getPlayerMoney(player)}",
                        "&7背包空间: &a${getEmptySlots(player)}"
                    )
                }
            }
        }

        // 底部控制按钮
        slotComponent(0, 5) {
            render {
                item(Material.ARROW) {
                    name("&7返回")
                }
            }
            onLeftClick {
                this@openPage.close()
            }
        }

        slotComponent(8, 5) {
            render {
                item(Material.BARRIER) {
                    name("&c关闭")
                }
            }
            onLeftClick {
                this@openPage.close()
            }
        }
    }
}
```

## 💡 性能优化技巧

### 1. 懒加载
```kotlin
// 只在需要时渲染
render { context ->
    if (context.oldItem == null) {
        // 首次渲染
        createExpensiveItem()
    } else {
        // 更新现有物品
        context.oldItem
    }
}
```

### 2. 缓存重用
```kotlin
// 缓存常用物品
private val cachedItems = mutableMapOf<String, ItemStack>()

render {
    cachedItems.getOrPut("diamond_button") {
        item(Material.DIAMOND) {
            name("&b钻石按钮")
            lore("&7缓存的物品")
        }
    }
}
```

### 3. 批量更新
```kotlin
// 批量更新多个组件
page.batchUpdate {
    component1.update()
    component2.update()
    component3.update()
}
```

## 📚 知识点总结

1. **组件选择**：根据需求选择合适的组件类型
2. **布局设计**：合理使用边框和填充组件创建美观界面
3. **数据处理**：使用分页和滚动组件处理大量数据
4. **性能优化**：通过懒加载和缓存提升性能
5. **用户体验**：提供清晰的导航和反馈

## ⚡ 下一步

- [教程3：事件处理](03-events.md) - 深入了解事件系统
- [教程4：会话管理](04-sessions.md) - 学习页面导航
- [高级示例](../examples/advanced/) - 查看复杂组件的实际应用

## 🔗 相关链接

- [组件API](../api/components.md) - 详细的组件API文档
- [性能优化](../guides/performance.md) - 性能优化指南
- [最佳实践](../guides/best-practices.md) - 组件使用最佳实践
