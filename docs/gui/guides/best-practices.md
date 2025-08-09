# GUI 模块最佳实践

本文档总结了使用 GUI 模块开发高质量 GUI 界面的最佳实践和编码规范，**特别强调 i18n 国际化的正确使用方式**。

## 🌐 i18n 国际化最佳实践

### 1. 文本处理的三种方式

GUI 模块提供了三种文本处理方式，根据使用场景选择：

```kotlin
// ✅ 方式1：直接使用i18n模板（推荐用于简单文本）
slotComponent(0, 0) {
    render {
        item(Material.DIAMOND) {
            name("<%gui.button.confirm%>")      // 直接使用模板
            lore("<%gui.button.confirm_hint%>") // 自动处理格式和语言映射
        }
    }
}

// ✅ 方式2：使用GuiManager.format（推荐用于单个参数）
slotComponent(1, 0) {
    render {
        skull(player) {
            name(guiManager.format("<%gui.player.name%>", player.name))
            lore(guiManager.format("<%gui.player.level%>", player.level))
        }
    }
}

// ✅ 方式3：使用messager.sprintf（推荐用于复杂参数）
onLeftClick { context ->
    context.player.sendMessage(
        plugin.messager.sprintf("<%gui.message.operation_completed%>",
            context.player.name,
            System.currentTimeMillis(),
            operationType
        )
    )
}
```

### 2. 语言文件组织规范

```yaml
# lang/zh_CN.yml - 推荐的组织结构
gui:
  [plugin_name]:           # 插件名称空间
    [page_name]:           # 页面名称空间
      title: "页面标题"
      [component_name]:    # 组件名称空间
        name: "组件名称"
        hint: "提示信息"
        lore: |            # 多行文本使用 | 语法
          第一行描述
          第二行描述

          <green>操作提示</green>
    message:               # 消息名称空间
      operation_completed: "<green>{0} 完成了 {1} 操作！</green>"
```

## 🚀 推荐的开发方式

### 1. 使用推荐的页面创建方法

```kotlin
// ✅ 最佳实践：使用plugin.openPage函数（推荐）
class MyPlugin : BasePlugin() {
    fun showMainMenu(player: Player) {
        openPage(InventoryType.CHEST, 54, player) {
            // 直接使用i18n模板
            title("<%gui.main_menu.title%>")

            // 配置页面内容...
            slotComponent(x = 4, y = 2) {
                render {
                    item(Material.COMPASS) {
                        name("<%gui.button.navigation%>")
                        lore("<%gui.button.navigation_hint%>")
                        enchant(Enchantment.LURE, 1)
                        flag(ItemFlag.HIDE_ENCHANTS)
                    }
                }
                onLeftClick { showSubMenu(player) }
            }
        }
    }
}

// ✅ 实际项目示例：ExternalBook插件的实现方式
fun openPlayerBooksGui(plugin: ExternalBookPlugin, player: Player, target: OfflinePlayer?) {
    plugin.openPage(
        InventoryType.CHEST,
        size = 54,
        player = player,
        title = plugin.guiManager.format("<%gui.player_books.title%>", target?.name ?: player.name)
    ) {
        // 添加/修改按钮
        slotComponent(0, 5) {
            render {
                urlSkull("9a2d891c6ae9f6baa040d736ab84d48344bb6b70d7f1a280dd12cbac4d777") {
                    name("<%gui.player_books.add_modify%>")
                }
            }
        }
    }
}
```

### 2. i18n 使用场景指南

#### 何时使用不同的文本处理方式

```kotlin
// ✅ 场景1：静态文本 - 直接使用模板
slotComponent(0, 0) {
    render {
        item(Material.BARRIER) {
            name("<%gui.button.close%>")           // 静态按钮名称
            lore("<%gui.button.close_hint%>")      // 静态提示文本
        }
    }
}

// ✅ 场景2：单个参数 - 使用GuiManager.format
slotComponent(1, 0) {
    render {
        skull(player) {
            name(guiManager.format("<%gui.player.welcome%>", player.name))
            lore(guiManager.format("<%gui.player.online_time%>", getOnlineTime(player)))
        }
    }
}

// ✅ 场景3：多个参数或复杂逻辑 - 使用messager.sprintf
onLeftClick { context ->
    val result = performOperation()
    context.player.sendMessage(
        plugin.messager.sprintf("<%gui.message.operation_result%>",
            context.player.name,
            result.operationType,
            result.duration,
            if (result.success) "成功" else "失败"
        )
    )
}

// ✅ 场景4：多行文本 - 使用sprintfPlain + split
slotComponent(2, 0) {
    render {
        item(Material.BOOK) {
            name("<%gui.book.help%>")
            lore(mutableListOf<String>().apply {
                addAll(
                    plugin.messager.sprintfPlain(true, "<%gui.book.help_content%>",
                        plugin.name,
                        plugin.description.version
                    ).split("\n")
                )
            })
        }
    }
}
            fill(Material.GRAY_STAINED_GLASS_PANE)
        }

        // 使用paginatedComponent显示玩家列表
        val userListComponent = paginatedComponent(startX=1, startY=1, width=7, height=4, data=onlinePlayers) {
            render { cxt ->
                ItemUtil.skull(cxt.item!!) {
                    name(plugin.messager.sprintf("<%gui.player_select.player_name%>", cxt.item!!.name))
                    lore(buildPlayerLore(plugin, cxt.item!!, type))
                }
            }
            onLeftClick { cxt, index, targetPlayer ->
                // 处理点击事件
                handlePlayerSelection(plugin, player, targetPlayer!!, type)
            }
        }
    }
}

// ❌ 不推荐：手动创建和管理页面（仅限高级用法）
fun showMenuOldWay(player: Player) {
    val page = createPage(player, "菜单", InventoryType.CHEST, 54) {
        // 配置内容...
    }

    val session = getDefaultSession(player)
    session.push(page)  // 需要手动管理
    page.show()         // 需要手动显示
}
```

**为什么推荐使用openPage？**
- 自动处理Session管理
- 减少样板代码
- 降低出错概率
- 更清晰的代码结构
- 更好的可读性

### 2. 正确的导航方式

```kotlin
// ✅ 推荐：使用openPage进行导航
fun navigateToSubMenu(player: Player) {
    openPage(InventoryType.CHEST, 27, player) {
        title("&e子菜单")
        // 配置内容...
    }
}

// ✅ 推荐：使用page.close()关闭当前页面
slotComponent(x = 0, y = 8) {
    render {
        ItemUtil.create(Material.BARRIER, name = plugin.messager.sprintf("<%gui.common.close%>"))
    }
    onLeftClick { _, _, _ ->
        this@openPage.close() // 关闭当前页面，自动回退到上一页
    }
}

// ✅ 推荐：使用page.back()返回上一页（如果存在）
slotComponent(x = 8, y = 8) {
    render {
        ItemUtil.create(Material.ARROW, name = plugin.messager.sprintf("<%gui.common.back%>"))
    }
    onLeftClick { _, _, _ ->
        this@openPage.back() // 返回上一页，如果没有上一页则关闭
    }
}

// ✅ 推荐：使用Session操作进行高级导航
fun goBack(player: Player) {
    val session = getDefaultSession(player)
    session.pop() // 返回上一页
}

// ✅ 推荐：使用Session替换页面
fun replaceCurrentPage(player: Player) {
    val session = getDefaultSession(player)
    val newPage = createPage(player, "新页面", InventoryType.CHEST, 27) {
        // 配置内容...
    }
    session.replace(newPage)
}
```

## 🏗️ 架构设计原则

### 1. 单一职责原则

每个组件应该只负责一个特定的功能：

```kotlin
// ✅ 好的做法：专门的确认按钮组件
fun Page.addConfirmButton(x: Int, y: Int, onConfirm: () -> Unit) {
    slotComponent(x, y) {
        render {
            ItemUtil.confirmButton("确认操作") {
                enchant(Enchantment.LURE, 1)
                flag(ItemFlag.HIDE_ENCHANTS)
            }
        }
        onLeftClick { onConfirm() }
    }
}

// ❌ 避免：一个组件处理多种不同的功能
slotComponent(x = 4, y = 2) {
    render {
        // 根据不同条件渲染不同物品，逻辑复杂
        when (someState) {
            State.CONFIRM -> itemUtil.templates.confirmButton()
            State.CANCEL -> itemUtil.templates.cancelButton()
            State.INFO -> itemUtil.templates.infoButton()
            else -> itemUtil.createItemStack(Material.BARRIER)
        }
    }
    // 处理多种不同的点击逻辑...
}
```

### 2. 组合优于继承

使用组合构建复杂界面：

```kotlin
// ✅ 好的做法：使用组合构建复杂界面
fun Page.addPlayerInfoSection(player: Player) {
    // 玩家头像
    addPlayerAvatar(player, x = 4, y = 1)

    // 玩家统计
    addPlayerStats(player, x = 2, y = 2)

    // 操作按钮
    addPlayerActions(player, x = 6, y = 2)
}

// ❌ 避免：创建复杂的继承层次
abstract class BasePlayerPage : BasePage() {
    abstract class PlayerInfoPage : BasePlayerPage() {
        class DetailedPlayerInfoPage : PlayerInfoPage() {
            // 深层继承，难以维护
        }
    }
}
```

### 3. 声明式编程

使用DSL描述界面结构，而不是命令式编程：

```kotlin
// ✅ 好的做法：声明式描述界面
openPage(InventoryType.CHEST, 54, player) {
    title("玩家管理")

    createBorder()
    createPlayerInfo(targetPlayer)
    createActionButtons(targetPlayer)
    createNavigationButtons()
}

// ❌ 避免：命令式构建界面
val page = createPage(player, "玩家管理", InventoryType.CHEST, 54)
val borderComponent = RectFillComponent(...)
page.addComponent(borderComponent)
val playerComponent = SingleSlotComponent(...)
page.addComponent(playerComponent)
// ... 大量重复的添加代码
```

## 🧩 组件设计最佳实践

### 1. 避免槽位覆盖问题

GUI1模块已经修复了槽位覆盖问题，但仍建议遵循以下最佳实践：

```kotlin
// ✅ 好的做法：合理规划组件布局，避免不必要的覆盖
openPage(InventoryType.CHEST, 54, player) {
    title("布局规划示例")

    // 边框组件
    fullBorder(0, 0, 9, 6) {
        fillMaterial(Material.GRAY_STAINED_GLASS_PANE)
    }

    // 内容区域组件（不与边框重叠）
    rectFillComponent(1, 1, 7, 4) {
        render { context ->
            ItemUtil.create(Material.WHITE_WOOL, "内容区域")
        }
    }

    // 导航按钮（在边框内的特定位置）
    slotComponent(0, 5) {
        render { ItemUtil.backButton("返回") }
    }
}

// ❌ 避免：不必要的槽位覆盖（虽然系统会正确处理）
openPage(InventoryType.CHEST, 54, player) {
    // 大范围填充
    rectFillComponent(0, 0, 9, 6) {
        render { ItemUtil.create(Material.STONE, "背景") }
    }

    // 然后在同一位置添加其他组件（会覆盖上面的组件）
    slotComponent(0, 0) {
        render { ItemUtil.create(Material.DIAMOND, "特殊按钮") }
    }
    // 这样做虽然可以工作，但不够清晰
}
```

### 2. BorderFillComponent 最佳实践

BorderFillComponent是创建边框的专用组件，以下是推荐的使用方式：

```kotlin
// ✅ 推荐：使用borderFillComponent创建简单边框（TPA插件实际用法）
openPage(InventoryType.CHEST, 54, player) {
    title("边框示例")

    // 创建简单边框 - 实际项目中的常用方式
    borderFillComponent(1, 1, 7, 4) {
        fill(Material.GRAY_STAINED_GLASS_PANE)
    }

    // 内容区域在边框内部
    paginatedComponent(startX=1, startY=1, width=7, height=4, data=items) {
        // 分页内容...
    }
}

// ✅ 推荐：使用fullBorder创建完整边框（高级用法）
openPage(InventoryType.CHEST, 54, player) {
    title("完整边框示例")

    // 创建装饰性边框
    fullBorder(0, 0, 9, 6) {
        decorativeBorder(
            borderMaterial = Material.GRAY_STAINED_GLASS_PANE,
            cornerMaterial = Material.BLACK_STAINED_GLASS_PANE,
            borderName = "§7边框装饰",
            cornerName = "§8角落"
        )
    }

    // 内容区域
    // ... 其他组件
}

// ✅ 推荐：使用部分边框进行区域分隔
openPage(InventoryType.CHEST, 54, player) {
    title("区域分隔示例")

    // 顶部导航栏
    borderFillComponent(0, 0, 9, 1,
        fillTop = true, fillBottom = false,
        fillLeft = false, fillRight = false
    ) {
        fillMaterial(Material.BLUE_STAINED_GLASS_PANE, "§b导航栏")
        onClick { context ->
            // 导航功能
        }
    }

    // 底部操作栏
    borderFillComponent(0, 5, 9, 1,
        fillTop = false, fillBottom = true,
        fillLeft = false, fillRight = false
    ) {
        fillMaterial(Material.GREEN_STAINED_GLASS_PANE, "§a操作栏")
    }
}

// ✅ 推荐：基于边框类型的不同处理
borderFillComponent(0, 0, 9, 6) {
    borderMaterials(
        topMaterial = Material.RED_STAINED_GLASS_PANE,
        bottomMaterial = Material.BLUE_STAINED_GLASS_PANE,
        leftMaterial = Material.GREEN_STAINED_GLASS_PANE,
        rightMaterial = Material.YELLOW_STAINED_GLASS_PANE,
        cornerMaterial = Material.BLACK_STAINED_GLASS_PANE
    )

    onClickByBorderType { context, borderType, relativeX, relativeY ->
        when (borderType) {
            BorderType.TOP -> showTopMenu(context.player)
            BorderType.BOTTOM -> showBottomMenu(context.player)
            BorderType.LEFT -> showLeftMenu(context.player)
            BorderType.RIGHT -> showRightMenu(context.player)
            else -> showCornerMenu(context.player, borderType)
        }
    }
}

// ❌ 避免：用RectFillComponent创建边框（不够灵活）
rectFillComponent(0, 0, 9, 1) { /* 顶部边框 */ }
rectFillComponent(0, 5, 9, 1) { /* 底部边框 */ }
rectFillComponent(0, 1, 1, 4) { /* 左边框 */ }
rectFillComponent(8, 1, 1, 4) { /* 右边框 */ }
// 这样做代码冗长，且无法利用边框类型信息
```

### 3. 组件组合策略

```kotlin
// ✅ 推荐：合理组合不同类型的组件
fun Page.createPlayerManagementInterface(targetPlayer: Player) {
    // 1. 边框装饰
    fullBorder(0, 0, 9, 6) {
        decorativeBorder()
    }

    // 2. 玩家信息区域
    slotComponent(4, 1) {
        render {
            targetPlayer.getSkull(1) {
                name("§e${targetPlayer.name}")
                addLore("§7等级: ${targetPlayer.level}")
            }
        }
    }

    // 3. 操作按钮区域
    rectFillComponent(2, 3, 5, 1) {
        render { context ->
            val actions = listOf("踢出", "禁言", "传送", "查看", "奖励")
            val actionIndex = (context.slot - 11) % actions.size
            ItemUtil.button(Material.PAPER, "§a${actions[actionIndex]}")
        }
        onClick { context ->
            handlePlayerAction(targetPlayer, context.slot - 11)
        }
    }

    // 4. 导航按钮
    slotComponent(0, 5) {
        render { ItemUtil.backButton("返回") }
        onClick { goBack() }
    }
}
```

### 4. 性能优化建议

```kotlin
// ✅ 推荐：为大型边框启用智能缓存
borderFillComponent(0, 0, 9, 6) {
    enableSmartCaching = true  // 启用智能缓存

    renderByBorderType { borderType, x, y, relativeX, relativeY ->
        // 复杂的渲染逻辑
        createComplexBorderItem(borderType, x, y)
    }
}

// ✅ 推荐：缓存静态边框物品
object BorderCache {
    private val borderItems = mutableMapOf<BorderType, ItemStack>()

    fun getBorderItem(borderType: BorderType): ItemStack {
        return borderItems.getOrPut(borderType) {
            when (borderType) {
                BorderType.TOP, BorderType.BOTTOM ->
                    ItemStack(Material.IRON_BLOCK)
                BorderType.LEFT, BorderType.RIGHT ->
                    ItemStack(Material.GOLD_BLOCK)
                else -> ItemStack(Material.DIAMOND_BLOCK)
            }
        }
    }
}

borderFillComponent(0, 0, 9, 6) {
    renderByType { borderType ->
        BorderCache.getBorderItem(borderType)
    }
}
```

## 🔧 ItemBuilder 函数参数最佳实践

GUI1模块的所有物品创建方法都支持可选的`function: ItemBuilder.() -> Unit`参数，允许你在创建物品时进行额外的自定义配置。

### 1. 基本用法

```kotlin
// ✅ 使用function参数添加附魔和隐藏标志
slotComponent(x = 4, y = 2) {
    render {
        ItemUtil.button(Material.DIAMOND_SWORD, "强化武器") {
            enchant(Enchantment.DAMAGE_ALL, 5)
            enchant(Enchantment.FIRE_ASPECT, 2)
            flag(ItemFlag.HIDE_ENCHANTS)
            unbreakable(true)
        }
    }
}

// ✅ 创建发光的导航按钮
slotComponent(x = 0, y = 5) {
    render {
        ItemUtil.backButton("返回上级") {
            enchant(Enchantment.LURE, 1)
            flag(ItemFlag.HIDE_ENCHANTS)
        }
    }
}
```

### 2. 头颅物品的高级用法

```kotlin
// ✅ 创建玩家头颅并添加自定义属性
slotComponent(x = 4, y = 1) {
    render {
        ItemUtil.skull(player, "玩家信息") {
            addLore("等级: ${player.level}")
            addLore("血量: ${player.health}/${player.maxHealth}")
            addLore("点击查看详情")
        }
    }
}

// ✅ 创建URL头颅（自定义材质）
slotComponent(x = 2, y = 1) {
    render {
        ItemUtil.urlSkull("http://textures.minecraft.net/texture/abc123", "自定义头颅") {
            addLore("这是一个自定义材质的头颅")
            enchant(Enchantment.LURE, 1)
            flag(ItemFlag.HIDE_ENCHANTS)
        }
    }
}

// ✅ 创建Base64头颅
slotComponent(x = 6, y = 1) {
    render {
        ItemUtil.customSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWJjMTIzIn19fQ==", "特殊头颅") {
            addLore("Base64编码的头颅材质")
            amount(1)
        }
    }
}
```

### 3. 扩展函数的使用

```kotlin
// ✅ 使用扩展函数创建头颅
slotComponent(x = 4, y = 2) {
    render {
        player.getSkull(1) {
            name(Component.text("玩家: ${player.name}").color(NamedTextColor.GOLD))
            addLore("在线时间: ${getOnlineTime(player)}")
        }
    }
}

// ✅ 使用String扩展函数创建材质头颅
slotComponent(x = 5, y = 2) {
    render {
        "abc123def456".toSkull(1) {
            name(Component.text("材质头颅").color(NamedTextColor.AQUA))
            addLore("材质ID: abc123def456")
        }
    }
}
```

### 4. 组合使用模式

```kotlin
// ✅ 创建状态指示器并添加动画效果
fun createStatusIndicator(isOnline: Boolean): ItemStack {
    return ItemUtil.statusIndicator(isOnline, "在线", "离线") {
        if (isOnline) {
            enchant(Enchantment.LURE, 1)
            flag(ItemFlag.HIDE_ENCHANTS)
        }
        addLore("状态更新时间: ${System.currentTimeMillis()}")
    }
}

// ✅ 创建进度条物品
fun createProgressBar(progress: Int, total: Int): ItemStack {
    return ItemUtil.progressIndicator(progress, total, "任务进度") {
        val percentage = if (total > 0) (progress * 100) / total else 0
        amount(maxOf(1, percentage / 10)) // 根据进度设置数量

        if (percentage >= 100) {
            enchant(Enchantment.LURE, 1)
            flag(ItemFlag.HIDE_ENCHANTS)
        }
    }
}
```

### 5. 性能优化建议

```kotlin
// ✅ 缓存复杂的物品创建
class ItemCache {
    private val cachedItems = mutableMapOf<String, ItemStack>()

    fun getOrCreateButton(key: String, material: Material, name: String): ItemStack {
        return cachedItems.getOrPut(key) {
            ItemUtil.button(material, name) {
                enchant(Enchantment.LURE, 1)
                flag(ItemFlag.HIDE_ENCHANTS)
                addLore("缓存时间: ${System.currentTimeMillis()}")
            }
        }
    }
}

// ❌ 避免：每次都重新创建复杂物品
slotComponent(x = 4, y = 2) {
    render {
        // 每次渲染都会重新创建，性能较差
        ItemUtil.button(Material.DIAMOND, "复杂按钮") {
            repeat(10) { i ->
                addLore("描述行 $i: ${calculateExpensiveValue()}")
            }
        }
    }
}
```

## 🎨 界面设计规范

### 1. 一致的布局模式

建立统一的布局规范：

```kotlin
// 定义标准布局常量
object GUILayout {
    // 标准位置
    const val CENTER_X = 4
    const val CENTER_Y = 2

    // 导航按钮位置
    const val BACK_BUTTON_X = 0
    const val BACK_BUTTON_Y = 5
    const val CLOSE_BUTTON_X = 8
    const val CLOSE_BUTTON_Y = 5

    // 操作按钮行
    const val ACTION_ROW_Y = 3
}

// 使用统一的布局
fun Page.addStandardNavigation() {
    addBackButton(GUILayout.BACK_BUTTON_X, GUILayout.BACK_BUTTON_Y)
    addCloseButton(GUILayout.CLOSE_BUTTON_X, GUILayout.CLOSE_BUTTON_Y)
}
```

### 2. 统一的视觉风格

使用一致的颜色和物品：

```kotlin
object GUITheme {
    // 颜色方案
    const val PRIMARY_COLOR = "&6"      // 金色 - 主要元素
    const val SECONDARY_COLOR = "&e"    // 黄色 - 次要元素
    const val SUCCESS_COLOR = "&a"      // 绿色 - 成功/确认
    const val DANGER_COLOR = "&c"       // 红色 - 危险/删除
    const val INFO_COLOR = "&b"         // 青色 - 信息
    const val MUTED_COLOR = "&7"        // 灰色 - 次要文本

    // 标准物品
    val BORDER_ITEM = Material.GRAY_STAINED_GLASS_PANE
    val CONFIRM_ITEM = Material.GREEN_WOOL
    val CANCEL_ITEM = Material.RED_WOOL
    val INFO_ITEM = Material.BOOK
}
```

### 3. 响应式设计

适配不同的容器大小：

```kotlin
fun createResponsiveLayout(inventoryType: InventoryType): LayoutConfig {
    return when (inventoryType) {
        InventoryType.CHEST -> LayoutConfig(
            centerX = 4, centerY = 2,
            hasNavigation = true,
            hasBorder = true
        )
        InventoryType.HOPPER -> LayoutConfig(
            centerX = 2, centerY = 0,
            hasNavigation = false,
            hasBorder = false
        )
        InventoryType.DISPENSER -> LayoutConfig(
            centerX = 1, centerY = 1,
            hasNavigation = false,
            hasBorder = false
        )
        else -> throw UnsupportedOperationException("不支持的容器类型")
    }
}
```

## 🔧 性能优化

### 1. 懒加载和缓存

避免不必要的计算和对象创建：

```kotlin
// ✅ 好的做法：缓存昂贵的计算结果
class PlayerStatsComponent(private val player: Player) {
    private var cachedStats: PlayerStats? = null
    private var lastUpdate = 0L

    fun render(): ItemStack {
        val now = System.currentTimeMillis()
        if (cachedStats == null || now - lastUpdate > 5000) { // 5秒缓存
            cachedStats = calculatePlayerStats(player)
            lastUpdate = now
        }

        return ItemUtil.create(Material.PAPER, "玩家统计") {
            lore(cachedStats!!.toLore())
            enchant(Enchantment.LURE, 1)
            flag(ItemFlag.HIDE_ENCHANTS)
        }
    }
}

// ❌ 避免：每次渲染都重新计算
slotComponent(x = 4, y = 2) {
    render {
        val stats = calculateExpensivePlayerStats(player) // 每次都计算
        ItemUtil.create(Material.PAPER, "玩家统计") {
            lore(stats.toLore())
        }
    }
}
```

### 2. 批量更新

避免频繁的单个更新：

```kotlin
// ✅ 好的做法：批量更新
fun updatePlayerList(players: List<Player>) {
    // 收集所有更新，然后一次性应用
    val updates = players.mapIndexed { index, player ->
        index to createPlayerItem(player)
    }

    // 批量更新组件
    paginatedComponent.updateItems(updates)
}

// ❌ 避免：频繁的单个更新
fun updatePlayerList(players: List<Player>) {
    players.forEachIndexed { index, player ->
        paginatedComponent.updateItem(index, createPlayerItem(player))
        // 每次更新都会触发重新渲染
    }
}
```

### 3. 合理使用调度器

避免阻塞主线程：

```kotlin
// ✅ 好的做法：异步处理耗时操作
slotComponent(x = 4, y = 2) {
    onLeftClick {
        player.sendMessage("&e正在处理，请稍候...")

        // 异步执行耗时操作
        runAsync {
            val result = performExpensiveOperation()

            // 回到主线程更新UI
            sync {
                player.sendMessage("&a操作完成：$result")
                component.update()
            }
        }
    }
}

// ❌ 避免：在主线程执行耗时操作
slotComponent(x = 4, y = 2) {
    onLeftClick {
        val result = performExpensiveOperation() // 阻塞主线程
        player.sendMessage("&a操作完成：$result")
    }
}
```

## 🛡️ 安全性最佳实践

### 1. 权限验证

始终验证玩家权限：

```kotlin
// ✅ 好的做法：完整的权限检查
fun Page.addAdminButton(permission: String, action: () -> Unit) {
    slotComponent(x = 7, y = 3) {
        render {
            if (player.hasPermission(permission)) {
                ItemUtil.create(Material.REDSTONE, "管理功能", listOf("点击执行管理操作")) {
                    enchant(Enchantment.LURE, 1)
                    flag(ItemFlag.HIDE_ENCHANTS)
                }
            } else {
                ItemUtil.create(Material.GRAY_DYE, "管理功能", listOf("你没有权限使用此功能"))
            }
        }

        onLeftClick {
            if (player.hasPermission(permission)) {
                action()
            } else {
                player.sendMessage("&c权限不足！")
            }
        }
    }
}
```

### 2. 输入验证

验证所有用户输入：

```kotlin
// ✅ 好的做法：完整的输入验证
fun handleAmountInput(input: String): Boolean {
    return try {
        val amount = input.toInt()
        when {
            amount <= 0 -> {
                player.sendMessage("&c数量必须大于0！")
                false
            }
            amount > 64 -> {
                player.sendMessage("&c数量不能超过64！")
                false
            }
            else -> {
                processAmount(amount)
                true
            }
        }
    } catch (e: NumberFormatException) {
        player.sendMessage("&c请输入有效的数字！")
        false
    }
}
```

### 3. 防止重复操作

避免用户快速重复点击：

```kotlin
// ✅ 好的做法：防抖处理
class DebounceClickHandler(private val cooldownMs: Long = 1000) {
    private var lastClick = 0L

    fun handleClick(action: () -> Unit): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastClick < cooldownMs) {
            return false // 忽略重复点击
        }
        lastClick = now
        action()
        return true
    }
}

slotComponent(x = 4, y = 2) {
    val debouncer = DebounceClickHandler()

    onLeftClick {
        if (!debouncer.handleClick {
            performImportantAction()
        }) {
            player.sendMessage("&c操作太频繁，请稍候再试！")
        }
    }
}
```

## 📝 代码组织

### 1. 模块化设计

将相关功能组织到独立的类中：

```kotlin
// GUI组件工厂
object GUIComponents {
    fun createPlayerInfo(player: Player): ComponentBuilder {
        return ComponentBuilder { x, y ->
            slotComponent(x, y) {
                render { createPlayerInfoItem(player) }
                onLeftClick { showPlayerDetails(player) }
            }
        }
    }

    fun createConfirmDialog(message: String, onConfirm: () -> Unit): Page {
        // 创建确认对话框页面
    }
}

// GUI模板
object GUITemplates {
    fun createStandardMenu(title: String, builder: Page.() -> Unit): Page {
        return createPage(player, title, InventoryType.CHEST, 54) {
            createBorder()
            createStandardNavigation()
            builder()
        }
    }
}
```

### 2. 配置外部化

将可配置的内容提取到配置文件：

```kotlin
// 配置类
data class GUIConfig(
    val theme: ThemeConfig,
    val layout: LayoutConfig,
    val messages: MessageConfig
) {
    data class ThemeConfig(
        val primaryColor: String = "&6",
        val borderItem: Material = Material.GRAY_STAINED_GLASS_PANE
    )
}

// 使用配置
fun createThemedButton(config: GUIConfig.ThemeConfig): ItemStack {
    return ItemUtil.create(Material.DIAMOND, "${config.primaryColor}按钮") {
        enchant(Enchantment.LURE, 1)
        flag(ItemFlag.HIDE_ENCHANTS)
    }
}
```

### 3. 国际化支持

支持多语言：

```kotlin
// 使用消息管理器
slotComponent(x = 4, y = 2) {
    render {
        ItemUtil.create(Material.DIAMOND) {
            name(Component.text(messager.sprintf(player, "gui.button.confirm")))
            lore(listOf(messager.sprintf(player, "gui.button.confirm.lore")))
            enchant(Enchantment.LURE, 1)
            flag(ItemFlag.HIDE_ENCHANTS)
        }
    }

    onLeftClick {
        player.sendMessage(messager.sprintf(player, "gui.action.confirmed"))
    }
}
```

## 🧪 测试策略

### 1. 单元测试

测试关键的业务逻辑：

```kotlin
@Test
fun testPlayerStatsCalculation() {
    val mockPlayer = createMockPlayer()
    val stats = PlayerStatsCalculator.calculate(mockPlayer)

    assertEquals(100, stats.health)
    assertEquals(20, stats.level)
}
```

### 2. 集成测试

测试GUI的完整流程：

```kotlin
@Test
fun testPlayerMenuFlow() {
    val player = createTestPlayer()

    // 打开主菜单
    showPlayerMenu(player, targetPlayer)

    // 验证菜单已打开
    assertNotNull(getDefaultSession(player).current())

    // 模拟点击按钮
    simulateClick(player, 4, 2)

    // 验证结果
    assertTrue(player.hasReceivedMessage("操作完成"))
}
```

### 3. 性能测试

测试大量数据的处理能力：

```kotlin
@Test
fun testLargePlayerListPerformance() {
    val players = generateTestPlayers(1000)

    val startTime = System.currentTimeMillis()
    createPlayerListGUI(players)
    val endTime = System.currentTimeMillis()

    assertTrue("GUI创建时间应少于1秒", endTime - startTime < 1000)
}
```

## 📋 代码审查清单

在提交代码前，检查以下项目：

### 功能性
- [ ] 所有按钮都有正确的功能
- [ ] 错误情况得到适当处理
- [ ] 权限检查正确实现
- [ ] 输入验证完整

### 性能
- [ ] 没有不必要的重复计算
- [ ] 大量数据使用分页或虚拟化
- [ ] 异步操作不阻塞主线程
- [ ] 内存使用合理

### 安全性
- [ ] 所有用户输入都经过验证
- [ ] 权限检查在所有必要的地方
- [ ] 防止重复操作和滥用
- [ ] 敏感操作有确认机制

### 可维护性
- [ ] 代码结构清晰，职责分明
- [ ] 有适当的注释和文档
- [ ] 使用一致的命名规范
- [ ] 配置可外部化

### 用户体验
- [ ] 界面布局合理美观
- [ ] 操作流程直观
- [ ] 错误消息清晰有用
- [ ] 响应速度快

遵循这些最佳实践，你将能够创建高质量、可维护、性能优秀的GUI应用！
