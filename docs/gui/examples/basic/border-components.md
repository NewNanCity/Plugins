# BorderFillComponent 使用示例

BorderFillComponent是GUI1模块中专门用于创建边框的组件，本文档提供了详细的使用示例和最佳实践。

## 📋 目录

- [基础用法](#基础用法)
- [装饰性边框](#装饰性边框)
- [功能性边框](#功能性边框)
- [动态边框](#动态边框)
- [高级应用](#高级应用)
- [性能优化](#性能优化)

## 🚀 基础用法

### 1. 创建完整边框

```kotlin
fun showMainMenu(player: Player) {
    openPage(InventoryType.CHEST, 54, player) {
        title("§6主菜单")

        // 创建完整边框
        fullBorder(0, 0, 9, 6) {
            fillMaterial(Material.GRAY_STAINED_GLASS_PANE, "§7边框")

            onClick { context ->
                context.player.sendMessage("§e点击了边框")
            }
        }

        // 中心内容
        slotComponent(4, 3) {
            render {
                ItemUtil.create(Material.COMPASS, "§e导航") {
                    addLore("§7点击打开菜单")
                    enchant(Enchantment.LURE, 1)
                    flag(ItemFlag.HIDE_ENCHANTS)
                }
            }
            onClick { showSubMenu(player) }
        }
    }
}
```

### 2. 创建部分边框

```kotlin
fun showInventoryGUI(player: Player) {
    openPage(InventoryType.CHEST, 54, player) {
        title("§b物品管理")

        // 只创建顶部和底部边框
        borderFillComponent(0, 0, 9, 6,
            fillTop = true,
            fillBottom = true,
            fillLeft = false,
            fillRight = false
        ) {
            fillMaterial(Material.BLUE_STAINED_GLASS_PANE, "§b分隔线")
        }

        // 中间区域用于物品展示
        rectFillComponent(1, 1, 7, 4) {
            render { context ->
                // 物品展示逻辑
                null // 空槽位，允许放置物品
            }
        }
    }
}
```

## 🎨 装饰性边框

### 1. 彩色边框

```kotlin
fun showColorfulMenu(player: Player) {
    openPage(InventoryType.CHEST, 54, player) {
        title("§c彩色菜单")

        borderFillComponent(0, 0, 9, 6) {
            borderMaterials(
                topMaterial = Material.RED_STAINED_GLASS_PANE,
                bottomMaterial = Material.BLUE_STAINED_GLASS_PANE,
                leftMaterial = Material.GREEN_STAINED_GLASS_PANE,
                rightMaterial = Material.YELLOW_STAINED_GLASS_PANE,
                cornerMaterial = Material.PURPLE_STAINED_GLASS_PANE
            )

            onClickByBorderType { context, borderType, relativeX, relativeY ->
                val colorName = when (borderType) {
                    BorderType.TOP -> "§c红色"
                    BorderType.BOTTOM -> "§9蓝色"
                    BorderType.LEFT -> "§a绿色"
                    BorderType.RIGHT -> "§e黄色"
                    else -> "§5紫色"
                }
                context.player.sendMessage("§f你点击了${colorName}边框")
            }
        }
    }
}
```

### 2. 发光边框

```kotlin
fun showGlowingBorder(player: Player) {
    openPage(InventoryType.CHEST, 27, player) {
        title("§e✦ 发光边框 ✦")

        borderFillComponent(0, 0, 9, 3) {
            glowingBorder(Material.GLOWSTONE, "§e✦ 神秘边框 ✦")

            onClick { context ->
                // 播放音效
                context.player.playSound(
                    context.player.location,
                    Sound.BLOCK_NOTE_BLOCK_CHIME,
                    1.0f, 1.0f
                )
                context.player.sendMessage("§e✦ 边框发出了神秘的光芒 ✦")
            }
        }
    }
}
```

### 3. 主题边框

```kotlin
fun showThemedBorder(theme: GUITheme, player: Player) {
    openPage(InventoryType.CHEST, 54, player) {
        title("§f主题边框 - ${theme.name}")

        borderFillComponent(0, 0, 9, 6) {
            decorativeBorder(
                borderMaterial = theme.borderMaterial,
                cornerMaterial = theme.cornerMaterial,
                borderName = "§7${theme.name} 边框",
                cornerName = "§8${theme.name} 角落"
            )
        }
    }
}

enum class GUITheme(
    val displayName: String,
    val borderMaterial: Material,
    val cornerMaterial: Material
) {
    OCEAN("海洋", Material.CYAN_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE),
    FOREST("森林", Material.GREEN_STAINED_GLASS_PANE, Material.BROWN_STAINED_GLASS_PANE),
    DESERT("沙漠", Material.YELLOW_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE),
    NETHER("下界", Material.RED_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE)
}
```

## ⚙️ 功能性边框

### 1. 导航边框

```kotlin
fun showNavigationBorder(player: Player) {
    openPage(InventoryType.CHEST, 54, player) {
        title("§6导航界面")

        // 顶部导航栏
        borderFillComponent(0, 0, 9, 1,
            fillTop = true, fillBottom = false,
            fillLeft = false, fillRight = false
        ) {
            renderByBorderType { borderType, x, y, relativeX, relativeY ->
                when (relativeX) {
                    0 -> ItemUtil.create(Material.ARROW, "§a上一页") {
                        addLore("§7点击返回上一页")
                    }
                    4 -> ItemUtil.create(Material.COMPASS, "§e主菜单") {
                        addLore("§7点击返回主菜单")
                    }
                    8 -> ItemUtil.create(Material.BARRIER, "§c关闭") {
                        addLore("§7点击关闭界面")
                    }
                    else -> ItemUtil.create(Material.GRAY_STAINED_GLASS_PANE, " ")
                }
            }

            onClickByBorderType { context, borderType, relativeX, relativeY ->
                when (relativeX) {
                    0 -> goToPreviousPage(context.player)
                    4 -> showMainMenu(context.player)
                    8 -> context.player.closeInventory()
                }
            }
        }
    }
}
```

### 2. 状态指示边框

```kotlin
fun showStatusBorder(player: Player, status: ServerStatus) {
    openPage(InventoryType.CHEST, 27, player) {
        title("§f服务器状态")

        borderFillComponent(0, 0, 9, 3) {
            renderByType { borderType ->
                val material = when (status) {
                    ServerStatus.ONLINE -> Material.GREEN_STAINED_GLASS_PANE
                    ServerStatus.MAINTENANCE -> Material.YELLOW_STAINED_GLASS_PANE
                    ServerStatus.OFFLINE -> Material.RED_STAINED_GLASS_PANE
                }

                ItemStack(material).apply {
                    itemMeta = itemMeta?.apply {
                        setDisplayName("§f状态: ${status.displayName}")
                        lore = listOf(
                            "§7边框类型: ${borderType.name}",
                            "§7更新时间: ${System.currentTimeMillis()}"
                        )
                    }
                }
            }
        }
    }
}

enum class ServerStatus(val displayName: String) {
    ONLINE("§a在线"),
    MAINTENANCE("§e维护中"),
    OFFLINE("§c离线")
}
```

### 3. 交互式边框

```kotlin
fun showInteractiveBorder(player: Player) {
    openPage(InventoryType.CHEST, 54, player) {
        title("§d交互式边框")

        borderFillComponent(0, 0, 9, 6) {
            renderByBorderType { borderType, x, y, relativeX, relativeY ->
                when (borderType) {
                    BorderType.TOP -> ItemUtil.create(Material.REDSTONE_TORCH, "§c功能区") {
                        addLore("§7点击激活功能")
                    }
                    BorderType.BOTTOM -> ItemUtil.create(Material.LEVER, "§e控制区") {
                        addLore("§7点击切换设置")
                    }
                    BorderType.LEFT -> ItemUtil.create(Material.CHEST, "§b存储区") {
                        addLore("§7点击打开存储")
                    }
                    BorderType.RIGHT -> ItemUtil.create(Material.CRAFTING_TABLE, "§a工作区") {
                        addLore("§7点击打开工作台")
                    }
                    else -> ItemUtil.create(Material.DIAMOND, "§f特殊功能") {
                        addLore("§7角落的特殊功能")
                    }
                }
            }

            onClickByBorderType { context, borderType, relativeX, relativeY ->
                when (borderType) {
                    BorderType.TOP -> activateFunction(context.player)
                    BorderType.BOTTOM -> toggleSettings(context.player)
                    BorderType.LEFT -> openStorage(context.player)
                    BorderType.RIGHT -> openWorkbench(context.player)
                    else -> activateSpecialFunction(context.player, borderType)
                }
            }
        }
    }
}
```

## 🔄 动态边框

### 1. 动画边框

```kotlin
fun showAnimatedBorder(player: Player) {
    openPage(InventoryType.CHEST, 54, player) {
        title("§d动画边框")

        val borderComponent = borderFillComponent(0, 0, 9, 6) {
            var animationFrame = 0

            renderByBorderType { borderType, x, y, relativeX, relativeY ->
                val colors = listOf(
                    Material.RED_STAINED_GLASS_PANE,
                    Material.ORANGE_STAINED_GLASS_PANE,
                    Material.YELLOW_STAINED_GLASS_PANE,
                    Material.GREEN_STAINED_GLASS_PANE,
                    Material.BLUE_STAINED_GLASS_PANE,
                    Material.PURPLE_STAINED_GLASS_PANE
                )

                val colorIndex = (animationFrame + relativeX + relativeY) % colors.size
                ItemStack(colors[colorIndex])
            }

            // 每10tick更新一次动画
            setUpdateInterval(10L)
            onUpdate {
                animationFrame = (animationFrame + 1) % 6
            }
        }
    }
}
```

### 2. 响应式边框

```kotlin
fun showResponsiveBorder(player: Player) {
    openPage(InventoryType.CHEST, 54, player) {
        title("§f响应式边框")

        borderFillComponent(0, 0, 9, 6) {
            renderByBorderType { borderType, x, y, relativeX, relativeY ->
                // 根据玩家状态改变边框
                val material = when {
                    player.health < 10 -> Material.RED_STAINED_GLASS_PANE
                    player.foodLevel < 10 -> Material.ORANGE_STAINED_GLASS_PANE
                    player.level < 10 -> Material.YELLOW_STAINED_GLASS_PANE
                    else -> Material.GREEN_STAINED_GLASS_PANE
                }

                ItemStack(material).apply {
                    itemMeta = itemMeta?.apply {
                        setDisplayName("§f状态边框")
                        lore = listOf(
                            "§7血量: ${player.health}/${player.maxHealth}",
                            "§7饥饿值: ${player.foodLevel}/20",
                            "§7等级: ${player.level}"
                        )
                    }
                }
            }

            // 每秒更新一次
            setUpdateInterval(20L)
        }
    }
}
```

## 🚀 高级应用

### 1. 多层边框

```kotlin
fun showMultiLayerBorder(player: Player) {
    openPage(InventoryType.CHEST, 54, player) {
        title("§f多层边框")

        // 外层边框
        fullBorder(0, 0, 9, 6) {
            fillMaterial(Material.BLACK_STAINED_GLASS_PANE, "§8外层边框")
        }

        // 内层边框
        borderFillComponent(1, 1, 7, 4) {
            fillMaterial(Material.GRAY_STAINED_GLASS_PANE, "§7内层边框")
        }

        // 核心区域
        rectFillComponent(2, 2, 5, 2) {
            render { context ->
                ItemUtil.create(Material.DIAMOND, "§b核心内容")
            }
        }
    }
}
```

### 2. 自适应边框

```kotlin
fun showAdaptiveBorder(player: Player, contentSize: Int) {
    val rows = (contentSize + 8) / 9 + 2 // 计算需要的行数
    val inventorySize = rows * 9

    openPage(InventoryType.CHEST, inventorySize, player) {
        title("§f自适应边框")

        // 根据内容大小创建边框
        fullBorder(0, 0, 9, rows) {
            decorativeBorder()
        }

        // 内容区域
        rectFillComponent(1, 1, 7, rows - 2) {
            render { context ->
                val itemIndex = (context.y - 1) * 7 + (context.x - 1)
                if (itemIndex < contentSize) {
                    ItemUtil.create(Material.PAPER, "§f内容 #${itemIndex + 1}")
                } else {
                    null
                }
            }
        }
    }
}
```

## ⚡ 性能优化

### 1. 缓存边框物品

```kotlin
object BorderItemCache {
    private val cache = mutableMapOf<String, ItemStack>()

    fun getBorderItem(
        borderType: BorderType,
        material: Material,
        name: String
    ): ItemStack {
        val key = "${borderType.name}_${material.name}_$name"
        return cache.getOrPut(key) {
            ItemStack(material).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName(name)
                }
            }
        }
    }
}

fun showOptimizedBorder(player: Player) {
    openPage(InventoryType.CHEST, 54, player) {
        title("§f优化边框")

        borderFillComponent(0, 0, 9, 6) {
            enableSmartCaching = true

            renderByType { borderType ->
                BorderItemCache.getBorderItem(
                    borderType,
                    Material.GRAY_STAINED_GLASS_PANE,
                    "§7边框"
                )
            }
        }
    }
}
```

### 2. 延迟加载边框

```kotlin
fun showLazyBorder(player: Player) {
    openPage(InventoryType.CHEST, 54, player) {
        title("§f延迟加载边框")

        borderFillComponent(0, 0, 9, 6) {
            var isLoaded = false

            renderByBorderType { borderType, x, y, relativeX, relativeY ->
                if (!isLoaded) {
                    // 显示加载中
                    ItemUtil.create(Material.CLOCK, "§e加载中...")
                } else {
                    // 显示实际内容
                    createComplexBorderItem(borderType)
                }
            }

            // 异步加载复杂内容
            runAsync {
                Thread.sleep(1000) // 模拟加载时间
                runSync {
                    isLoaded = true
                    update() // 更新边框显示
                }
            }
        }
    }
}
```

这些示例展示了BorderFillComponent的各种用法，从基础的装饰性边框到复杂的交互式和动态边框。根据你的具体需求选择合适的实现方式。
