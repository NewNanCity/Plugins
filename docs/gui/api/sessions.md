# 会话API参考

会话(Session)管理GUI的页面栈和导航逻辑，本文档详细介绍所有会话相关的API。

## 📋 目录

- [会话获取](#会话获取)
- [Session接口](#session接口)
- [页面栈操作](#页面栈操作)
- [会话状态](#会话状态)
- [会话管理](#会话管理)

## 🔍 会话获取

### getDefaultSession

**获取玩家的默认会话**

```kotlin
fun getDefaultSession(player: Player): Session
```

**参数**：
- `player` (required) - 目标玩家

**返回值**：玩家的默认Session实例

**示例**：
```kotlin
val session = getDefaultSession(player)
session.push(page)
```

### getSession

**获取或创建命名会话**

```kotlin
fun getSession(player: Player, name: String): Session
```

**参数**：
- `player` (required) - 目标玩家
- `name` (required) - 会话名称

**返回值**：指定名称的Session实例

**示例**：
```kotlin
// 为不同功能使用不同的Session
val adminSession = getSession(player, "admin")
val shopSession = getSession(player, "shop")

adminSession.openPage(InventoryType.CHEST, 54) {
    title("&c管理面板")
    // 配置管理功能...
}
```

### getAllSessions

**获取玩家的所有会话**

```kotlin
fun getAllSessions(player: Player): Map<String, Session>
```

**返回值**：会话名称到Session实例的映射

## 📱 Session接口

### 基本属性

```kotlin
interface Session {
    val player: Player              // 会话所属玩家
    val name: String               // 会话名称
    val isVisible: Boolean         // 是否可见
    val isClosed: Boolean          // 是否已关闭
}
```

### 页面栈操作

```kotlin
interface Session {
    fun push(page: Page)           // 压入页面到栈顶
    fun pop(): Page?               // 弹出并销毁栈顶页面
    fun replace(page: Page)        // 替换栈顶页面
    fun goto(index: Int)           // 跳转到指定页面
    fun current(): Page?           // 获取栈顶页面
    fun size(): Int                // 获取栈大小
    fun isEmpty(): Boolean         // 检查栈是否为空
    fun clear()                    // 清空所有页面
}
```

### 会话控制

```kotlin
interface Session {
    fun show()                     // 显示会话
    fun hide()                     // 隐藏会话
    fun close()                    // 关闭会话
}
```

### 页面创建

```kotlin
interface Session {
    fun openPage(
        inventoryType: InventoryType,
        size: Int = -1,
        builder: Page.() -> Unit
    ): Page
}
```

## 📚 页面栈操作

### push - 压入页面

```kotlin
fun push(page: Page)
```

**功能**：将页面压入栈顶并自动显示

**示例**：
```kotlin
val session = getDefaultSession(player)
val page = createPage(player, "新页面", InventoryType.CHEST, 27)

session.push(page) // 页面被压入栈顶并显示
```

### pop - 弹出页面

```kotlin
fun pop(): Page?
```

**功能**：弹出并销毁栈顶页面，显示新的栈顶页面

**返回值**：被弹出的页面，如果栈为空则返回null

**示例**：
```kotlin
val session = getDefaultSession(player)
val poppedPage = session.pop()

if (poppedPage != null) {
    player.sendMessage("&7已返回上一页")
} else {
    player.sendMessage("&7已经是第一页了")
}
```

### replace - 替换页面

```kotlin
fun replace(page: Page)
```

**功能**：替换栈顶页面，销毁旧页面

**示例**：
```kotlin
val session = getDefaultSession(player)
val newPage = createPage(player, "替换页面", InventoryType.CHEST, 27)

session.replace(newPage) // 替换当前页面
```

### goto - 跳转页面

```kotlin
fun goto(index: Int)
```

**功能**：跳转到指定位置的页面，销毁其后的所有页面

**参数**：
- `index` (required) - 目标页面索引，0为栈底，-1为栈顶

**示例**：
```kotlin
val session = getDefaultSession(player)

// 跳转到第一页
session.goto(0)

// 跳转到倒数第二页
session.goto(-2)
```

### current - 获取当前页面

```kotlin
fun current(): Page?
```

**返回值**：栈顶页面，如果栈为空则返回null

**示例**：
```kotlin
val session = getDefaultSession(player)
val currentPage = session.current()

if (currentPage != null) {
    logger.info("当前页面: ${currentPage.title}")
} else {
    logger.info("没有活动页面")
}
```

### 栈状态查询

```kotlin
// 获取栈大小
val size = session.size()

// 检查是否为空
val isEmpty = session.isEmpty()

// 获取所有页面
val allPages = session.getAllPages()
```

## 🎛️ 会话状态

### 显示控制

```kotlin
// 显示会话（显示栈顶页面）
session.show()

// 隐藏会话（隐藏栈顶页面）
session.hide()

// 检查可见状态
if (session.isVisible) {
    logger.info("会话当前可见")
}
```

### 生命周期管理

```kotlin
// 清空所有页面但保持会话开启
session.clear()

// 关闭会话（销毁所有页面并关闭会话）
session.close()

// 检查关闭状态
if (session.isClosed) {
    logger.info("会话已关闭")
}
```

## 🔧 会话管理

### 多会话管理

```kotlin
class MyPlugin : BasePlugin() {
    fun openAdminPanel(player: Player) {
        val adminSession = getSession(player, "admin")
        adminSession.openPage(InventoryType.CHEST, 54) {
            title("&c管理面板")
            
            // 关闭按钮
            slotComponent(x = 8, y = 5) {
                render { itemUtil.createItemStack(Material.BARRIER) { name("&c关闭") } }
                onLeftClick {
                    adminSession.close() // 关闭管理会话
                }
            }
        }
    }
    
    fun openShop(player: Player) {
        val shopSession = getSession(player, "shop")
        shopSession.openPage(InventoryType.CHEST, 54) {
            title("&e商店")
            // 配置商店内容...
        }
    }
}
```

### 会话切换

```kotlin
fun switchToAdminPanel(player: Player) {
    // 隐藏当前会话
    val defaultSession = getDefaultSession(player)
    defaultSession.hide()
    
    // 显示管理会话
    val adminSession = getSession(player, "admin")
    adminSession.show()
}

fun returnToMainMenu(player: Player) {
    // 关闭管理会话
    val adminSession = getSession(player, "admin")
    adminSession.close()
    
    // 显示默认会话
    val defaultSession = getDefaultSession(player)
    defaultSession.show()
}
```

### 会话状态监控

```kotlin
fun checkSessionState(player: Player) {
    val allSessions = getAllSessions(player)
    
    allSessions.forEach { (name, session) ->
        logger.info("会话 '$name':")
        logger.info("  - 大小: ${session.size()}")
        logger.info("  - 可见: ${session.isVisible}")
        logger.info("  - 关闭: ${session.isClosed}")
        
        session.current()?.let { page ->
            logger.info("  - 当前页面: ${page.title}")
        }
    }
}
```

### 会话清理

```kotlin
fun cleanupPlayerSessions(player: Player) {
    val allSessions = getAllSessions(player)
    
    allSessions.values.forEach { session ->
        if (!session.isClosed) {
            session.close()
        }
    }
    
    logger.info("已清理玩家 ${player.name} 的所有会话")
}
```

## 🚀 高级用法

### 会话间通信

```kotlin
fun transferToAdminPanel(player: Player, data: Map<String, Any>) {
    val defaultSession = getDefaultSession(player)
    val adminSession = getSession(player, "admin")
    
    // 保存当前状态
    val currentPage = defaultSession.current()
    if (currentPage != null) {
        // 保存页面状态到数据中
        data["previousPage"] = currentPage.title
    }
    
    // 切换到管理面板
    defaultSession.hide()
    adminSession.openPage(InventoryType.CHEST, 54) {
        title("&c管理面板")
        
        // 使用传递的数据
        val previousPage = data["previousPage"] as? String
        if (previousPage != null) {
            slotComponent(x = 0, y = 5) {
                render {
                    itemUtil.createItemStack(Material.ARROW) {
                        name("&7返回到 $previousPage")
                    }
                }
                onLeftClick {
                    adminSession.hide()
                    defaultSession.show()
                }
            }
        }
    }
}
```

### 会话持久化

```kotlin
fun saveSessionState(player: Player) {
    val allSessions = getAllSessions(player)
    val sessionData = mutableMapOf<String, Any>()
    
    allSessions.forEach { (name, session) ->
        sessionData[name] = mapOf(
            "size" to session.size(),
            "visible" to session.isVisible,
            "currentPageTitle" to (session.current()?.title ?: "")
        )
    }
    
    // 保存到配置文件或数据库
    savePlayerData(player, "sessions", sessionData)
}

fun restoreSessionState(player: Player) {
    val sessionData = loadPlayerData(player, "sessions") as? Map<String, Any>
    
    sessionData?.forEach { (name, data) ->
        val session = getSession(player, name)
        val sessionInfo = data as Map<String, Any>
        
        // 根据保存的状态恢复会话
        if (sessionInfo["visible"] as Boolean) {
            session.show()
        }
    }
}
```

## 🔗 相关API

- [页面API](pages.md) - 页面管理
- [组件API](components.md) - 页面组件
- [事件API](events.md) - 事件处理
- [DSL API](dsl.md) - DSL语法
