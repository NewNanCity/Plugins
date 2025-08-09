# 页面API参考

页面是GUI1中的核心概念，本文档详细介绍所有页面相关的API。

## 📋 目录

- [页面创建](#页面创建)
- [Page接口](#page接口)
- [页面实现类](#页面实现类)
- [页面生命周期](#页面生命周期)
- [页面配置](#页面配置)

## 🚀 页面创建

### openPage

**推荐的页面创建方法**

```kotlin
fun openPage(
    inventoryType: InventoryType,
    size: Int = -1,
    player: Player,
    builder: Page.() -> Unit
): Page
```

**参数**：
- `inventoryType` (required) - 容器类型
- `size` (optional) - 容器大小，仅对CHEST类型有效
- `player` (required) - 目标玩家
- `builder` (required) - 页面配置DSL

**返回值**：创建并显示的页面实例

**示例**：
```kotlin
// 创建箱子页面
val page = openPage(InventoryType.CHEST, 54, player) {
    title("&6主菜单")

    slotComponent(x = 4, y = 2) {
        render { itemUtil.createItemStack(Material.DIAMOND) }
        onLeftClick { player.sendMessage("点击了钻石！") }
    }
}

// 创建漏斗页面
openPage(InventoryType.HOPPER, player = player) {
    title("&e快速选择")
    // 配置内容...
}
```

### createPage

**高级页面创建方法**

```kotlin
fun createPage(
    player: Player,
    title: String,
    inventoryType: InventoryType,
    size: Int = -1,
    builder: Page.() -> Unit = {}
): Page
```

**参数**：
- `player` (required) - 目标玩家
- `title` (required) - 页面标题
- `inventoryType` (required) - 容器类型
- `size` (optional) - 容器大小
- `builder` (optional) - 页面配置DSL

**返回值**：创建的页面实例（需要手动管理）

**示例**：
```kotlin
val page = createPage(player, "&c确认操作", InventoryType.HOPPER) {
    slotComponent(x = 1, y = 0) {
        render { itemUtil.createItemStack(Material.GREEN_WOOL) { name("&a确认") } }
        onLeftClick { /* 确认逻辑 */ }
    }
}

// 需要手动管理
val session = getDefaultSession(player)
session.push(page)
page.show()
```

### createBookPage

**创建书本页面**

```kotlin
fun createBookPage(
    player: Player,
    title: String,
    author: String,
    vararg pages: String
): BookPage
```

**参数**：
- `player` (required) - 目标玩家
- `title` (required) - 书本标题
- `author` (required) - 书本作者
- `pages` (vararg) - 书本页面内容

**返回值**：书本页面实例

**示例**：
```kotlin
val bookPage = createBookPage(
    player = player,
    title = "服务器指南",
    author = "管理员",
    "欢迎来到服务器！",
    "这里是第二页内容...",
    "更多信息请查看官网"
)

getDefaultSession(player).push(bookPage)
bookPage.show()
```

## 📄 Page接口

### 基本属性

```kotlin
interface Page {
    val player: Player              // 页面所属玩家
    val title: String              // 页面标题
    val inventoryType: InventoryType // 容器类型
    val size: Int                  // 容器大小
    val inventory: Inventory       // Bukkit容器实例
    val session: Session           // 所属Session
    val components: List<Component> // 页面组件列表
    val isVisible: Boolean         // 是否可见
    val isClosed: Boolean          // 是否已关闭
}
```

### 生命周期方法

```kotlin
interface Page {
    fun show()                     // 显示页面
    fun hide()                     // 隐藏页面
    fun close()                    // 关闭页面（从Session中移除）
    fun update()                   // 更新页面（重新渲染所有组件）
    fun destroyInternal()          // 内部销毁方法
}
```

### 组件管理

```kotlin
interface Page {
    fun addComponent(component: Component)           // 添加组件
    fun removeComponent(component: Component)        // 移除组件
    fun getComponent(slot: Int): Component?          // 获取指定槽位的组件
    fun getComponents(x: Int, y: Int): List<Component> // 获取指定坐标的组件
    fun clearComponents()                            // 清空所有组件
}
```

### 事件处理

```kotlin
interface Page {
    fun onOpen(handler: (PageOpenContext) -> Unit)     // 页面打开事件
    fun onClose(handler: (PageCloseContext) -> Unit)   // 页面关闭事件
    fun onLeftClick(handler: (ClickContext) -> Unit)   // 左键点击事件
    fun onRightClick(handler: (ClickContext) -> Unit)  // 右键点击事件
    fun onShiftClick(handler: (ClickContext) -> Unit)  // Shift点击事件
}
```

### 聊天输入

```kotlin
interface Page {
    fun chatInput(
        hide: Boolean = true,
        handler: (input: String) -> Boolean
    ): Boolean
}
```

**参数**：
- `hide` (optional) - 是否隐藏当前GUI，默认true
- `handler` (required) - 输入处理器，返回true结束输入

**返回值**：是否成功开始输入（false表示已有其他输入在进行）

### 工具方法

```kotlin
interface Page {
    fun getSlot(x: Int, y: Int): Int               // 坐标转槽位
    fun getCoordinates(slot: Int): Pair<Int, Int>  // 槽位转坐标
    fun isValidSlot(slot: Int): Boolean            // 检查槽位是否有效
    fun isValidCoordinates(x: Int, y: Int): Boolean // 检查坐标是否有效
    fun getPositionInSession(): Int                // 获取在Session中的位置
}
```

## 🏗️ 页面实现类

### BasePage

**基础页面实现**

```kotlin
abstract class BasePage(
    override val player: Player,
    override val title: String,
    override val inventoryType: InventoryType,
    override val size: Int
) : Page
```

**特性**：
- 自动Session管理
- 组件生命周期绑定
- 容器关闭事件监听
- 线程安全的组件管理

### ChestPage

**箱子页面实现**

```kotlin
class ChestPage(
    player: Player,
    title: String,
    size: Int = 27
) : BasePage(player, title, InventoryType.CHEST, size)
```

**支持的大小**：9, 18, 27, 36, 45, 54

### BookPage

**书本页面实现**

```kotlin
class BookPage(
    player: Player,
    title: String,
    author: String,
    pages: List<String>
) : BasePage(player, title, InventoryType.CHEST, 0)
```

**特殊方法**：
```kotlin
fun addPage(content: String)           // 添加页面
fun removePage(index: Int)             // 移除页面
fun updatePage(index: Int, content: String) // 更新页面内容
```

## 🔄 页面生命周期

### 生命周期状态

```kotlin
enum class PageState {
    CREATED,    // 已创建
    SHOWN,      // 已显示
    HIDDEN,     // 已隐藏
    CLOSED      // 已关闭
}
```

### 状态转换

```
CREATED → show() → SHOWN
SHOWN → hide() → HIDDEN
HIDDEN → show() → SHOWN
SHOWN/HIDDEN → close() → CLOSED
```

### 生命周期事件

```kotlin
// 页面打开时触发
page.onOpen { context ->
    logger.info("页面已打开: ${context.page.title}")
}

// 页面关闭时触发
page.onClose { context ->
    logger.info("页面已关闭: ${context.page.title}")
    // 清理资源...
}
```

## ⚙️ 页面配置

### 标题配置

```kotlin
// 在DSL中设置标题
openPage(InventoryType.CHEST, 27, player) {
    title("&6主菜单")  // 支持颜色代码
}

// 动态更新标题（需要重新打开页面）
page.updateTitle("&c新标题")
```

### 大小配置

```kotlin
// 箱子页面支持自定义大小
openPage(InventoryType.CHEST, 54, player) {
    // 6行9列的大箱子
}

// 其他容器类型使用固定大小
openPage(InventoryType.HOPPER, player = player) {
    // 1行5列的漏斗
}
```

### 权限检查

```kotlin
openPage(InventoryType.CHEST, 27, player) {
    // 检查权限
    if (!player.hasPermission("myplugin.admin")) {
        player.sendMessage("&c权限不足！")
        page.close()
        return@openPage
    }

    title("&c管理面板")
    // 配置管理功能...
}
```

### 异常处理

```kotlin
openPage(InventoryType.CHEST, 27, player) {
    title("&6安全页面")

    try {
        // 可能出错的操作
        loadPlayerData(player)

        slotComponent(x = 4, y = 2) {
            render { createPlayerInfoItem(player) }
        }
    } catch (e: Exception) {
        guiLogger.logPageLifecycleError(this, "LOAD_DATA", e)

        // 显示错误信息
        slotComponent(x = 4, y = 2) {
            render {
                itemUtil.createItemStack(Material.BARRIER) {
                    name("&c加载失败")
                    lore("&7请稍后重试")
                }
            }
        }
    }
}
```

## 🔗 相关API

- [会话API](sessions.md) - Session管理
- [组件API](components.md) - 页面组件
- [事件API](events.md) - 事件处理
- [DSL API](dsl.md) - DSL语法
