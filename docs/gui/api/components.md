# 组件API参考

组件是GUI1中构建用户界面的基本单元，本文档详细介绍所有组件相关的API。

## 📋 目录

- [组件基础](#组件基础)
- [基础组件](#基础组件)
- [填充组件](#填充组件)
- [高级组件](#高级组件)
- [组件生命周期](#组件生命周期)
- [渲染系统](#渲染系统)

## 🧩 组件基础

### Component接口

```kotlin
interface Component {
    val page: Page                    // 所属页面
    val slots: Set<Int>              // 占用的槽位
    val isVisible: Boolean           // 是否可见
    val isClosed: Boolean            // 是否已关闭

    fun render()                     // 渲染组件
    fun update()                     // 更新组件
    fun hide()                       // 隐藏组件
    fun show()                       // 显示组件
    fun close()                      // 关闭组件
}
```

### 渲染上下文

```kotlin
data class RenderContext(
    val component: Component,        // 当前组件
    val slot: Int,                  // 当前槽位
    val x: Int,                     // X坐标
    val y: Int,                     // Y坐标
    val oldItem: ItemStack?         // 上次渲染的物品
)
```

### 事件上下文

```kotlin
data class ClickContext(
    val player: Player,             // 点击的玩家
    val component: Component,       // 被点击的组件
    val slot: Int,                 // 点击的槽位
    val x: Int,                    // X坐标
    val y: Int,                    // Y坐标
    val clickType: ClickType,      // 点击类型
    val item: ItemStack?,          // 点击的物品
    val event: InventoryClickEvent // 原始事件
) {
    fun stopPropagation()          // 阻止事件传播
}
```

## 🔧 基础组件

### SingleSlotComponent

**单槽组件 - 最基础的组件类型**

```kotlin
fun Page.slotComponent(
    x: Int,
    y: Int,
    builder: SingleSlotComponent.() -> Unit
): SingleSlotComponent
```

**参数**：
- `x` (required) - X坐标 (0-8)
- `y` (required) - Y坐标 (0-5)
- `builder` (required) - 组件配置DSL

**示例**：
```kotlin
slotComponent(x = 4, y = 2) {
    render { context ->
        ItemUtil.create(Material.DIAMOND) {
            name("&b钻石")
            lore("&7点击获取钻石")
        }
    }

    onLeftClick { context ->
        context.player.sendMessage("&a你获得了钻石！")
        context.player.inventory.addItem(ItemStack(Material.DIAMOND))
    }

    onRightClick { context ->
        context.player.sendMessage("&e右键点击钻石")
    }
}
```

### 组件配置方法

```kotlin
interface SingleSlotComponent {
    // 渲染配置
    fun render(renderer: (RenderContext) -> ItemStack)

    // 事件处理
    fun onLeftClick(handler: (ClickContext) -> Unit)
    fun onRightClick(handler: (ClickContext) -> Unit)
    fun onShiftClick(handler: (ClickContext) -> Unit)
    fun onMiddleClick(handler: (ClickContext) -> Unit)
    fun onDoubleClick(handler: (ClickContext) -> Unit)
    fun onDrag(handler: (ClickContext) -> Unit)

    // 生命周期
    fun onRender(handler: (RenderContext) -> Unit)
    fun onUpdate(handler: () -> Unit)
    fun onShow(handler: () -> Unit)
    fun onHide(handler: () -> Unit)

    // 条件渲染
    fun renderIf(condition: () -> Boolean, renderer: (RenderContext) -> ItemStack)
    fun showIf(condition: () -> Boolean)

    // 更新控制
    fun setUpdateInterval(ticks: Long)  // 设置自动更新间隔
    fun enableAutoUpdate(enabled: Boolean) // 启用/禁用自动更新
}
```

## 🎨 填充组件

### RectFillComponent

**矩形填充组件**

```kotlin
fun Page.rectFillComponent(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    builder: RectFillComponent.() -> Unit
): RectFillComponent
```

**示例**：
```kotlin
// 创建边框
rectFillComponent(x = 0, y = 0, width = 9, height = 1) {
    render { context ->
        ItemUtil.create(Material.GRAY_STAINED_GLASS_PANE) {
            name(" ") // 空名称
        }
    }
}

// 创建按钮区域
rectFillComponent(x = 2, y = 2, width = 5, height = 2) {
    render { context ->
        when (context.slot % 2) {
            0 -> ItemUtil.create(Material.GREEN_WOOL) { name("&a选项A") }
            else -> ItemUtil.create(Material.RED_WOOL) { name("&c选项B") }
        }
    }

    onLeftClick { context ->
        val option = if (context.slot % 2 == 0) "A" else "B"
        context.player.sendMessage("&e你选择了选项 $option")
    }
}
```

### LineFillComponent

**线性填充组件**

```kotlin
fun Page.lineFillComponent(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    builder: LineFillComponent.() -> Unit
): LineFillComponent
```

**示例**：
```kotlin
// 水平分隔线
lineFillComponent(x = 0, y = 2, width = 9, height = 1) {
    render { context ->
        ItemUtil.create(Material.BLACK_STAINED_GLASS_PANE) {
            name(" ")
        }
    }
}

// 垂直分隔线
lineFillComponent(x = 4, y = 0, width = 1, height = 6) {
    render { context ->
        ItemUtil.create(Material.IRON_BARS) {
            name("&7分隔线")
        }
    }
}
```

### PatternFillComponent

**模式填充组件**

```kotlin
fun Page.patternFillComponent(
    pattern: List<String>,
    builder: PatternFillComponent.() -> Unit
): PatternFillComponent
```

**示例**：
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
    setItem('a') { context ->
        ItemUtil.create(Material.GRAY_STAINED_GLASS_PANE) {
            name(" ")
        }
    }

    setItem('b') { context ->
        ItemUtil.create(Material.DIAMOND) {
            name("&b中心按钮")
            lore("&7点击执行操作")
        }
    }

    setItem(' ') { context ->
        null // 空槽位
    }

    onItemClick('b') { context ->
        context.player.sendMessage("&a中心按钮被点击！")
    }
}
```

### BorderFillComponent

**边框填充组件 - 在矩形区域的边框填充物品**

```kotlin
fun Page.borderFillComponent(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    fillTop: Boolean = true,
    fillBottom: Boolean = true,
    fillLeft: Boolean = true,
    fillRight: Boolean = true,
    builder: BorderFillComponent.() -> Unit
): BorderFillComponent

// 便利方法：创建完整边框
fun Page.fullBorder(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    builder: BorderFillComponent.() -> Unit
): BorderFillComponent
```

**参数**：
- `startX` (required) - 起始X坐标
- `startY` (required) - 起始Y坐标
- `width` (required) - 宽度
- `height` (required) - 高度
- `fillTop` (optional) - 是否填充上边框，默认true
- `fillBottom` (optional) - 是否填充下边框，默认true
- `fillLeft` (optional) - 是否填充左边框，默认true
- `fillRight` (optional) - 是否填充右边框，默认true

**边框类型枚举**：
```kotlin
enum class BorderType {
    TOP,           // 上边
    BOTTOM,        // 下边
    LEFT,          // 左边
    RIGHT,         // 右边
    TOP_LEFT,      // 左上角
    TOP_RIGHT,     // 右上角
    BOTTOM_LEFT,   // 左下角
    BOTTOM_RIGHT,  // 右下角
    UNKNOWN        // 未知（不应该出现）
}
```

**渲染上下文**：
```kotlin
data class BorderFillRenderContext(
    override val x: Int,           // 绝对X坐标
    override val y: Int,           // 绝对Y坐标
    override val slot: Int,        // 槽位索引
    override val oldItem: ItemStack?, // 上次渲染的物品
    val relativeX: Int,            // 相对X坐标（相对于边框起始位置）
    val relativeY: Int,            // 相对Y坐标（相对于边框起始位置）
    val borderType: BorderType     // 边框类型
) : RenderContext()
```

**基本示例**：
```kotlin
// 创建完整边框
page.fullBorder(0, 0, 9, 6) {
    fillMaterial(Material.GRAY_STAINED_GLASS_PANE, "§7边框")

    onClick { context ->
        context.player.sendMessage("§e点击了边框")
    }
}

// 创建部分边框（只有上下边）
page.borderFillComponent(1, 1, 7, 4,
    fillTop = true,
    fillBottom = true,
    fillLeft = false,
    fillRight = false
) {
    fillMaterial(Material.BLUE_STAINED_GLASS_PANE, "§b水平边框")
}
```

**高级示例**：
```kotlin
// 不同类型的边框材料
page.borderFillComponent(0, 0, 9, 6) {
    borderMaterials(
        topMaterial = Material.RED_STAINED_GLASS_PANE,
        bottomMaterial = Material.BLUE_STAINED_GLASS_PANE,
        leftMaterial = Material.GREEN_STAINED_GLASS_PANE,
        rightMaterial = Material.YELLOW_STAINED_GLASS_PANE,
        cornerMaterial = Material.BLACK_STAINED_GLASS_PANE
    )

    // 基于边框类型的点击处理
    onClickByBorderType { context, borderType, relativeX, relativeY ->
        when (borderType) {
            BorderType.TOP -> context.player.sendMessage("§c点击了上边框")
            BorderType.BOTTOM -> context.player.sendMessage("§9点击了下边框")
            BorderType.LEFT -> context.player.sendMessage("§a点击了左边框")
            BorderType.RIGHT -> context.player.sendMessage("§e点击了右边框")
            BorderType.TOP_LEFT -> context.player.sendMessage("§8点击了左上角")
            BorderType.TOP_RIGHT -> context.player.sendMessage("§8点击了右上角")
            BorderType.BOTTOM_LEFT -> context.player.sendMessage("§8点击了左下角")
            BorderType.BOTTOM_RIGHT -> context.player.sendMessage("§8点击了右下角")
            else -> context.player.sendMessage("§7点击了边框")
        }
    }
}

// 装饰性边框
page.borderFillComponent(1, 1, 7, 4) {
    decorativeBorder(
        borderMaterial = Material.GRAY_STAINED_GLASS_PANE,
        cornerMaterial = Material.BLACK_STAINED_GLASS_PANE,
        borderName = "§7装饰边框",
        cornerName = "§8角落装饰"
    )
}

// 发光边框
page.borderFillComponent(2, 2, 5, 2) {
    glowingBorder(Material.GLOWSTONE, "§e✦ 发光边框 ✦")
}

// 动态边框（基于位置的自定义渲染）
page.borderFillComponent(0, 0, 9, 6) {
    renderByBorderType { borderType, x, y, relativeX, relativeY ->
        val material = when (borderType) {
            BorderType.TOP, BorderType.BOTTOM -> Material.IRON_BLOCK
            BorderType.LEFT, BorderType.RIGHT -> Material.GOLD_BLOCK
            else -> Material.DIAMOND_BLOCK // 角落
        }

        ItemStack(material).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName("§f位置: ($relativeX, $relativeY)")
                lore = listOf(
                    "§7边框类型: ${borderType.name}",
                    "§7绝对坐标: ($x, $y)",
                    "§7相对坐标: ($relativeX, $relativeY)"
                )
            }
        }
    }
}
```

**DSL方法**：
```kotlin
interface BorderFillComponent {
    // 基础渲染方法
    fun render(function: RenderFunction<BorderFillRenderContext>)
    fun setItem(item: ItemStack?)
    fun setMaterial(material: Material)

    // 高级渲染方法
    fun renderByBorderType(function: (borderType: BorderType, x: Int, y: Int, relativeX: Int, relativeY: Int) -> ItemStack?)
    fun renderByType(function: (borderType: BorderType) -> ItemStack?)

    // DSL便利方法
    fun fillMaterial(material: Material, name: String? = null, lore: List<String>? = null)
    fun borderMaterials(
        topMaterial: Material = Material.GRAY_STAINED_GLASS_PANE,
        bottomMaterial: Material = Material.GRAY_STAINED_GLASS_PANE,
        leftMaterial: Material = Material.GRAY_STAINED_GLASS_PANE,
        rightMaterial: Material = Material.GRAY_STAINED_GLASS_PANE,
        cornerMaterial: Material = Material.BLACK_STAINED_GLASS_PANE
    )
    fun decorativeBorder(
        borderMaterial: Material = Material.GRAY_STAINED_GLASS_PANE,
        cornerMaterial: Material = Material.BLACK_STAINED_GLASS_PANE,
        borderName: String? = null,
        cornerName: String? = null
    )
    fun glowingBorder(material: Material = Material.GLOWSTONE, name: String = "§e✦ 边框 ✦")

    // 事件处理
    fun onClick(handler: ClickHandler)
    fun onLeftClick(handler: LeftClickHandler)
    fun onRightClick(handler: RightClickHandler)
    fun onShiftClick(handler: ShiftClickHandler)
    fun onClickByBorderType(handler: (context: ClickEventContext, borderType: BorderType, relativeX: Int, relativeY: Int) -> Unit)
}
```

**使用场景**：
- **装饰性边框**: 为GUI添加美观的边框装饰
- **功能性边框**: 创建可点击的导航边框
- **区域分隔**: 使用边框分隔不同的功能区域
- **状态指示**: 通过边框颜色表示不同状态
- **导航控制**: 边框作为导航按钮使用

## 🚀 高级组件

### PaginatedComponent

**分页组件 - 支持有限分页和无限滚动**

`PaginatedComponent` 是一个强大的分页组件，可以处理大量数据的展示。它支持两种模式：
- **有限分页模式**：适用于已知数据总量的场景
- **无限滚动模式**：适用于数据量未知或需要懒加载的场景

```kotlin
fun <T> Page.paginatedComponent(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    builder: PaginatedComponent<T>.() -> Unit
): PaginatedComponent<T>
```

#### DataProvider 数据提供器

数据提供器是分页组件的核心，它定义了如何获取和管理数据：

```kotlin
interface DataProvider<T> {
    // 获取数据总数：>= 0 表示有限分页，< 0 表示无限分页
    fun getSize(callback: DataProviderCallback<Int>)

    // 获取指定范围的数据
    fun fetchItems(offset: Int, limit: Int, callback: DataProviderCallback<List<T>>)

    // 检查是否还有更多数据
    fun canFetchMore(offset: Int, callback: DataProviderCallback<Boolean>)
}
```

#### 无缓存设计

组件采用无缓存设计，所有缓存逻辑由DataProvider负责：

```kotlin
class CachedDataProvider<T>(
    private val delegate: IDataProvider<T>
) : IDataProvider<T> {
    private val cache = mutableMapOf<String, List<T>>()

    override fun fetchItems(offset: Int, limit: Int, callback: DataProviderCallback<List<T>>) {
        val key = "${offset}_${limit}"
        val cached = cache[key]
        if (cached != null) {
            callback(Result.success(cached))
        } else {
            delegate.fetchItems(offset, limit) { result ->
                result.onSuccess { data -> cache[key] = data }
                callback(result)
            }
        }
    }
    // ... 其他方法
}
```

#### 创建 DataProvider

**1. 从 List 创建（有限分页）**：
```kotlin
val players = server.onlinePlayers.toList()
val dataProvider = DataProviders.fromList(players)
```

**2. 从函数创建（有限分页）**：
```kotlin
val dataProvider = DataProviders.fromFunction {
    // 每次调用时重新获取数据
    server.onlinePlayers.toList()
}
```

**3. 创建无限分页数据提供器**：
```kotlin
val infiniteProvider = DataProviders.infinite(
    itemProvider = { offset, limit ->
        // 模拟API调用
        generateSequence(offset) { it + 1 }
            .take(limit)
            .map { "Item #$it" }
            .toList()
    },
    hasMoreProvider = { offset -> offset < 1000 }
)
```

**4. API 风格的无限分页**：
```kotlin
val apiProvider = DataProviders.infiniteApi { page, pageSize ->
    // 调用外部API
    externalApi.getUsers(page, pageSize)
}
```

**5. 使用 DSL 构建器**：
```kotlin
val customProvider = dataProvider<String> {
    size { 100 }  // 总数据量
    items { offset, limit ->
        // 获取数据逻辑
        (offset until offset + limit).map { "Item $it" }
    }
    hasMore { offset -> offset < 100 }
}
```

#### 基本使用示例

**有限分页示例**：
```kotlin
val players = server.onlinePlayers.toList()

paginatedComponent<Player>(x = 0, y = 1, width = 9, height = 4) {
    // 使用 List 数据源（自动创建有限分页）
    setData(players)

    // 或直接设置 DataProvider
    setDataProvider(DataProviders.fromList(players))

    render { context ->
        ItemUtil.skull(context.item!!) {
            name("&e${context.item!!.name}")
            lore(
                "&7等级: ${context.item!!.level}",
                "&7生命值: ${context.item!!.health.toInt()}",
                "&7页面: ${context.pageIndex + 1}",
                "&7索引: ${context.index}",
                "&7",
                "&a点击查看详情"
            )
        }
    }

    // 空槽位渲染
    renderEmptySlot { context ->
        ItemUtil.create(Material.GRAY_STAINED_GLASS_PANE) {
            name(" ")
        }
    }

    onLeftClick { context, index, clickedPlayer ->
        if (clickedPlayer != null) {
            showPlayerDetails(context.player, clickedPlayer)
        }
    }
}
```

**无限滚动示例**：
```kotlin
paginatedComponent<TransferRankEntry>(x = 0, y = 1, width = 9, height = 4) {
    // 设置无限分页数据提供器
    setDataProvider(DataProviders.infinite(
        itemProvider = { offset, limit ->
            // 异步加载排行榜数据
            rankApi.getTopPlayers(offset / limit + 1, limit)
        },
        hasMoreProvider = { offset ->
            // 检查是否还有更多数据
            rankApi.hasMorePlayers(offset)
        }
    ))

    render { context ->
        val entry = context.item ?: return@render null
        val rank = context.globalIndex + 1

        ItemUtil.skull(entry.player) {
            name("&6#$rank ${entry.playerName}")
            lore(
                "&7积分: ${entry.score}",
                "&7加载状态: ${if (context.isLoading) "&e加载中..." else "&a已加载"}",
                "&7分页模式: ${if (context.isInfiniteMode) "&b无限滚动" else "&e有限分页"}",
                "&7",
                "&a点击查看详情"
            )
        }
    }

    // 加载中状态渲染
    renderLoadingSlot { context ->
        ItemUtil.create(Material.CLOCK) {
            name("&e加载中...")
            lore("&7正在获取数据，请稍候")
        }
    }

    // 空槽位渲染
    renderEmptySlot { context ->
        ItemUtil.create(Material.BARRIER) {
            name("&c暂无数据")
        }
    }

    onLeftClick { context, index, entry ->
        if (entry != null) {
            showPlayerRankDetails(context.player, entry)
        }
    }
}
```

#### 分页控制

**导航按钮**：
```kotlin
paginatedComponent<Item>(x = 1, y = 1, width = 7, height = 4) {
    setData(items)

    render { context ->
        // 渲染物品
    }
}

// 添加分页控制按钮
singleSlotComponent(x = 0, y = 5) {
    render { context ->
        ItemUtil.create(Material.ARROW) {
            name("&7« 上一页")
            lore("&7点击返回上一页")
        }
    }

    onLeftClick { context ->
        paginatedComponent.previousPage()
    }
}

singleSlotComponent(x = 8, y = 5) {
    render { context ->
        ItemUtil.create(Material.ARROW) {
            name("&7下一页 »")
            lore("&7点击前往下一页")
        }
    }

    onLeftClick { context ->
        paginatedComponent.nextPage()
    }
}

// 页面信息显示
singleSlotComponent(x = 4, y = 5) {
    render { context ->
        val currentPage = paginatedComponent.getCurrentPage() + 1
        val totalPages = paginatedComponent.totalPages

        ItemUtil.create(Material.PAPER) {
            name("&e页面信息")
            lore(
                if (paginatedComponent.isInfiniteMode) {
                    "&7当前页: $currentPage"
                    "&7模式: &b无限滚动"
                } else {
                    "&7第 $currentPage 页，共 $totalPages 页"
                    "&7模式: &e有限分页"
                }
            )
        }
    }
}
```

#### 页面变更监听

```kotlin
paginatedComponent<Player>(x = 0, y = 1, width = 9, height = 4) {
    setData(players)

    // 监听页面变更事件
    pageChangeHandler = { context ->
        val currentPage = context.currentPage + 1

        if (context.totalPages == -1) {
            // 无限分页模式
            player.sendMessage("&a已切换到第 $currentPage 页（无限滚动）")
        } else {
            // 有限分页模式
            val totalPages = context.totalPages
            player.sendMessage("&a已切换到第 $currentPage/$totalPages 页")
        }

        // 预加载相邻页面（仅在无限模式下）
        if (context.totalPages == -1) {
            preloadPage(context.currentPage + 1)
        }
    }
}
```

#### 高级功能

**缓存管理**：
```kotlin
paginatedComponent<Item>(x = 0, y = 1, width = 9, height = 4) {
    setDataProvider(customProvider)

    // 检查缓存状态
    if (isPageCached(2)) {
        println("第3页已缓存")
    }

    // 预加载页面
    preloadPage(5)

    // 清空缓存
    clearCache()

    // 刷新当前页
    refresh()
}
```

**动态数据更新**：
```kotlin
// 添加数据项（仅适用于 ListDataProvider）
paginatedComponent.addItem(newPlayer)

// 移除数据项
paginatedComponent.removeItem(player)

// 清空数据
paginatedComponent.clearData()

// 更新数据提供器
paginatedComponent.setDataProvider(newDataProvider)
```

#### 渲染上下文

```kotlin
data class PaginatedBaseRenderContext<T>(
    val x: Int,                    // 绝对X坐标
    val y: Int,                    // 绝对Y坐标
    val slot: Int,                 // 槽位索引
    val oldItem: ItemStack?,       // 上次渲染的物品
    val relativeX: Int,            // 相对X坐标
    val relativeY: Int,            // 相对Y坐标
    val index: Int,                // 页面内索引
    val pageIndex: Int,            // 当前页码
    val item: T?,                  // 数据项
    val globalIndex: Int,          // 全局索引
    val isLoading: Boolean,        // 是否正在加载
    val isPageCached: Boolean,     // 页面是否已缓存
    val isInfiniteMode: Boolean    // 是否为无限分页模式
)
```

#### 实际应用场景

**1. 玩家列表**：
```kotlin
// 有限分页：在线玩家列表
paginatedComponent<Player>(x = 0, y = 1, width = 9, height = 4) {
    setDataProvider(DataProviders.fromFunction {
        server.onlinePlayers.toList()
    })

    render { context ->
        ItemUtil.skull(context.item!!) {
            name("&e${context.item!!.name}")
        }
    }
}
```

**2. 排行榜**：
```kotlin
// 无限滚动：服务器排行榜
paginatedComponent<RankEntry>(x = 0, y = 1, width = 9, height = 4) {
    setDataProvider(DataProviders.infiniteApi { page, pageSize ->
        rankingApi.getTopPlayers(page, pageSize)
    })

    renderLoadingSlot { context ->
        ItemUtil.create(Material.CLOCK) {
            name("&e加载排行榜...")
        }
    }
}
```

**3. 商店物品**：
```kotlin
// 有限分页：商店物品展示
paginatedComponent<ShopItem>(x = 0, y = 1, width = 9, height = 4) {
    setData(shopItems)

    render { context ->
        val item = context.item ?: return@render null
        ItemUtil.create(item.material) {
            name("&a${item.name}")
            lore(
                "&7价格: &6${item.price} 金币",
                "&7库存: &e${item.stock}",
                "&7",
                "&a左键购买 &7| &c右键预览"
            )
        }
    }
}
```

#### 性能优化建议

1. **选择合适的缓存策略**：
   - 小数据集：使用 `CURRENT_PAGE_ONLY`
   - 大数据集或API数据：使用 `MULTI_PAGE`

2. **无限分页的懒加载**：
   - 实现 `canFetchMore()` 避免不必要的请求
   - 使用异步加载，不阻塞主线程

3. **预加载策略**：
   - 在页面变更时预加载相邻页面
   - 避免过度预加载消耗内存

4. **错误处理**：
   - 处理网络请求失败
   - 提供重试机制
   - 显示友好的错误信息
```

### ScrollableComponent

**滚动组件**

```kotlin
fun Page.scrollableComponent(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    builder: ScrollableComponent.() -> Unit
): ScrollableComponent
```

**示例**：
```kotlin
scrollableComponent(x = 1, y = 1, width = 7, height = 4) {
    totalItems(100) // 总共100个物品

    render { index ->
        itemUtil.createItemStack(Material.PAPER) {
            name("&e物品 #${index + 1}")
            lore("&7这是第 ${index + 1} 个物品")
        }
    }

    onItemClick { index ->
        player.sendMessage("&a你点击了物品 #${index + 1}")
    }

    // 滚动控制
    scrollUpButton(x = 8, y = 1) {
        itemUtil.createItemStack(Material.ARROW) {
            name("&7向上滚动")
        }
    }

    scrollDownButton(x = 8, y = 4) {
        itemUtil.createItemStack(Material.ARROW) {
            name("&7向下滚动")
        }
    }

    scrollBar(x = 8, y = 2, height = 2) {
        itemUtil.createItemStack(Material.STICK) {
            name("&7滚动条")
        }
    }
}
```

### StorageComponent

**存储组件 - 允许物品操作的特殊组件**

```kotlin
fun Page.storageComponent(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    builder: StorageComponent.() -> Unit
): StorageComponent
```

**示例**：
```kotlin
storageComponent(x = 1, y = 1, width = 7, height = 4) {
    // 允许的操作
    allowPickup(true)     // 允许拿取物品
    allowPlace(true)      // 允许放置物品
    allowDrop(false)      // 禁止丢弃物品
    allowShiftClick(true) // 允许Shift点击

    // 物品过滤
    itemFilter { item ->
        // 只允许特定类型的物品
        item.type in listOf(Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT)
    }

    // 事件监听
    onItemAdd { item, slot ->
        player.sendMessage("&a物品已添加到槽位 $slot")
    }

    onItemRemove { item, slot ->
        player.sendMessage("&c物品已从槽位 $slot 移除")
    }

    onItemMove { fromSlot, toSlot ->
        player.sendMessage("&e物品从槽位 $fromSlot 移动到 $toSlot")
    }

    // 初始物品
    setInitialItems(mapOf(
        0 to ItemStack(Material.DIAMOND, 5),
        1 to ItemStack(Material.EMERALD, 3)
    ))
}
```

## 🔄 组件生命周期

### 生命周期状态

```kotlin
enum class ComponentState {
    CREATED,    // 已创建
    RENDERED,   // 已渲染
    SHOWN,      // 已显示
    HIDDEN,     // 已隐藏
    CLOSED      // 已关闭
}
```

### 生命周期事件

```kotlin
slotComponent(x = 4, y = 2) {
    onRender { context ->
        logger.debug("组件开始渲染: slot=${context.slot}")
    }

    onShow {
        logger.debug("组件已显示")
    }

    onHide {
        logger.debug("组件已隐藏")
    }

    onUpdate {
        logger.debug("组件已更新")
    }

    render { context ->
        itemUtil.createItemStack(Material.CLOCK) {
            name("&e当前时间")
            lore("&7${System.currentTimeMillis()}")
        }
    }

    // 每秒更新一次
    setUpdateInterval(20L)
}
```

## 🎯 渲染系统

### 渲染优化

```kotlin
slotComponent(x = 4, y = 2) {
    // 缓存渲染结果
    var cachedItem: ItemStack? = null
    var lastUpdate = 0L

    render { context ->
        val now = System.currentTimeMillis()

        // 5秒内使用缓存
        if (cachedItem != null && now - lastUpdate < 5000) {
            return@render cachedItem!!
        }

        // 重新渲染
        cachedItem = itemUtil.createItemStack(Material.DIAMOND) {
            name("&b缓存的钻石")
            lore("&7更新时间: ${now}")
        }
        lastUpdate = now

        cachedItem!!
    }
}
```

### 条件渲染

```kotlin
slotComponent(x = 4, y = 2) {
    renderIf({ player.hasPermission("admin") }) { context ->
        itemUtil.createItemStack(Material.REDSTONE) {
            name("&c管理功能")
            lore("&7仅管理员可见")
        }
    }

    renderIf({ !player.hasPermission("admin") }) { context ->
        itemUtil.createItemStack(Material.GRAY_DYE) {
            name("&7权限不足")
            lore("&c你没有权限查看此功能")
        }
    }
}
```

### 异步渲染

```kotlin
slotComponent(x = 4, y = 2) {
    render { context ->
        // 显示加载中的物品
        val loadingItem = itemUtil.createItemStack(Material.CLOCK) {
            name("&e加载中...")
            lore("&7正在获取数据")
        }

        // 异步加载数据
        runAsync {
            val data = loadExpensiveData(player)

            // 回到主线程更新
            runSync {
                // 更新为实际数据
                context.component.update()
            }
        }

        loadingItem
    }
}
```

## 🔗 相关API

- [页面API](pages.md) - 页面管理
- [事件API](events.md) - 事件处理
- [物品工具API](items.md) - 物品创建
- [调度器API](scheduler.md) - 异步操作
