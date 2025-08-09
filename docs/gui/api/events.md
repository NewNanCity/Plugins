# 事件API参考

本文档详细介绍GUI模块的事件处理API，包括事件类型、处理方法和最佳实践。

## 📋 事件类型概览

GUI模块支持以下事件类型：

### 点击事件
- `onLeftClick` - 左键点击
- `onRightClick` - 右键点击
- `onMiddleClick` - 中键点击
- `onShiftClick` - Shift+点击
- `onDoubleClick` - 双击

### 拖拽事件
- `onDrag` - 拖拽操作
- `onDragSingle` - 单个物品拖拽
- `onDragEven` - 平均分配拖拽

### 生命周期事件
- `onOpen` - 页面打开
- `onClose` - 页面关闭
- `onShow` - 页面显示
- `onHide` - 页面隐藏

## 🎯 点击事件API

### onLeftClick

处理左键点击事件：

```kotlin
onLeftClick { context ->
    val player = context.player
    val slot = context.slot
    val item = context.item

    player.sendMessage("左键点击了槽位 $slot")
}

// 带优先级的事件处理
onLeftClick(priority = EventPriority.HIGH) { context ->
    if (isImportantAction(context)) {
        handleImportantAction(context)
        context.stopPropagation() // 阻止其他处理器
    }
}

// 带条件的事件处理
onLeftClick(condition = { context -> context.player.hasPermission("admin") }) { context ->
    handleAdminAction(context)
}
```

### onRightClick

处理右键点击事件：

```kotlin
onRightClick { context ->
    val player = context.player
    val clickType = context.clickType

    when (clickType) {
        ClickType.RIGHT -> player.sendMessage("普通右键")
        ClickType.SHIFT_RIGHT -> player.sendMessage("Shift+右键")
        else -> player.sendMessage("其他右键操作")
    }
}
```

### onShiftClick

处理Shift+点击事件：

```kotlin
onShiftClick { context ->
    val player = context.player
    val item = context.item

    if (item != null) {
        player.sendMessage("Shift+点击了 ${item.type}")
        // 快速操作逻辑
        performQuickAction(player, item)
    }
}
```

### onDoubleClick

处理双击事件：

```kotlin
onDoubleClick(
    maxInterval = 500L // 双击间隔时间（毫秒）
) { context ->
    val player = context.player
    player.sendMessage("双击检测到！")

    // 执行双击特定操作
    performDoubleClickAction(context)
}
```

## 🔄 拖拽事件API

### onDrag

处理拖拽操作：

```kotlin
onDrag { context ->
    val player = context.player
    val dragType = context.dragType
    val slots = context.affectedSlots

    player.sendMessage("拖拽操作影响了 ${slots.size} 个槽位")

    // 根据拖拽类型处理
    when (dragType) {
        DragType.SINGLE -> handleSingleDrag(context)
        DragType.EVEN -> handleEvenDrag(context)
    }
}
```

### onDragSingle

处理单个物品拖拽：

```kotlin
onDragSingle { context ->
    val player = context.player
    val sourceSlot = context.sourceSlot
    val targetSlots = context.targetSlots

    // 验证拖拽操作
    if (isValidDrag(sourceSlot, targetSlots)) {
        performDragOperation(context)
    } else {
        context.cancel() // 取消拖拽
        player.sendMessage("&c无效的拖拽操作")
    }
}
```

## 🔄 生命周期事件API

### onOpen

页面打开时触发：

```kotlin
onOpen { context ->
    val player = context.player
    val page = context.page

    logger.info("玩家 ${player.name} 打开了页面 ${page.title}")

    // 初始化页面数据
    initializePageData(player, page)

    // 记录访问日志
    logPageAccess(player, page)
}
```

### onClose

页面关闭时触发：

```kotlin
onClose { context ->
    val player = context.player
    val page = context.page

    logger.info("玩家 ${player.name} 关闭了页面 ${page.title}")

    // 清理页面资源
    cleanupPageResources(page)

    // 保存用户状态
    savePlayerState(player)
}
```

## 📊 事件上下文API

### ClickContext

点击事件的上下文信息：

```kotlin
interface ClickContext {
    val player: Player              // 触发事件的玩家
    val page: Page                  // 当前页面
    val component: Component?       // 触发事件的组件
    val slot: Int                   // 点击的槽位
    val item: ItemStack?           // 点击的物品
    val clickType: ClickType       // 点击类型
    val event: InventoryClickEvent // 原始Bukkit事件

    fun stopPropagation()          // 阻止事件传播
    fun cancel()                   // 取消事件
    fun isShiftClick(): Boolean    // 是否为Shift点击
    fun isRightClick(): Boolean    // 是否为右键点击
}
```

### DragContext

拖拽事件的上下文信息：

```kotlin
interface DragContext {
    val player: Player                    // 触发事件的玩家
    val page: Page                        // 当前页面
    val dragType: DragType               // 拖拽类型
    val sourceSlot: Int                  // 源槽位
    val targetSlots: Set<Int>            // 目标槽位集合
    val affectedSlots: Set<Int>          // 受影响的槽位
    val event: InventoryDragEvent        // 原始Bukkit事件

    fun stopPropagation()                // 阻止事件传播
    fun cancel()                         // 取消事件
}
```

### LifecycleContext

生命周期事件的上下文信息：

```kotlin
interface LifecycleContext {
    val player: Player              // 相关玩家
    val page: Page                  // 相关页面
    val session: Session           // 相关会话
    val timestamp: Long            // 事件时间戳

    fun getData(key: String): Any? // 获取附加数据
    fun setData(key: String, value: Any) // 设置附加数据
}
```

## 🔄 事件冒泡机制

### 事件传播顺序

事件按以下顺序传播：
1. **Item级别** - 物品特定的事件处理
2. **Component级别** - 组件的事件处理
3. **Page级别** - 页面的事件处理
4. **Session级别** - 会话的事件处理

### 阻止事件传播

```kotlin
// 在组件级别阻止传播
slotComponent(x = 4, y = 2) {
    onLeftClick { context ->
        logger.info("组件处理点击")

        if (shouldStopPropagation(context)) {
            context.stopPropagation() // 阻止传播到页面级别
        }
    }
}

// 页面级别的处理（如果没有被阻止）
page.onLeftClick { context ->
    logger.info("页面处理点击")
    // 处理所有未被组件处理的点击事件
}
```

## ⚡ 异步事件处理

### 异步处理模式

```kotlin
onLeftClick { context ->
    val player = context.player

    // 立即给用户反馈
    player.sendMessage("&7处理中...")

    // 异步执行耗时操作
    plugin.runAsync {
        try {
            val result = performExpensiveOperation()

            // 回到主线程更新UI
            plugin.sync {
                player.sendMessage("&a操作完成: $result")
                updateGuiWithResult(result)
            }
        } catch (e: Exception) {
            plugin.sync {
                player.sendMessage("&c操作失败: ${e.message}")
                logger.error("异步操作失败", e)
            }
        }
    }
}
```

### 任务调度支持

```kotlin
onLeftClick { context ->
    val player = context.player

    // 使用任务调度处理
    plugin.runAsync {
        try {
            player.sendMessage("&7开始处理...")

            // 在异步线程中执行
            val data = fetchDataFromDatabase()

            // 回到主线程更新UI
            plugin.runSync {
                updateGui(data)
                player.sendMessage("&a处理完成")
            }
        } catch (e: Exception) {
            player.sendMessage("&c处理失败: ${e.message}")
        }
    }
}
```

## 🛡️ 事件安全性

### 权限检查

```kotlin
onLeftClick(
    permission = "myplugin.admin",
    permissionMessage = "&c你没有权限执行此操作"
) { context ->
    // 只有有权限的玩家才能触发
    handleAdminAction(context)
}

// 自定义权限检查
onLeftClick(
    condition = { context ->
        context.player.hasPermission("myplugin.use") &&
        isPlayerAllowed(context.player)
    }
) { context ->
    handleAction(context)
}
```

### 冷却时间

```kotlin
private val clickCooldowns = mutableMapOf<UUID, Long>()

onLeftClick { context ->
    val player = context.player
    val now = System.currentTimeMillis()
    val lastClick = clickCooldowns[player.uniqueId] ?: 0

    if (now - lastClick < 1000) { // 1秒冷却
        player.sendMessage("&c操作太频繁，请稍后再试")
        return@onLeftClick
    }

    clickCooldowns[player.uniqueId] = now
    handleAction(context)
}
```

## 📚 事件处理最佳实践

### 1. 事件优先级

```kotlin
// 高优先级：系统关键事件
onLeftClick(priority = EventPriority.HIGHEST) { context ->
    if (isSystemCritical(context)) {
        handleSystemEvent(context)
        context.stopPropagation()
    }
}

// 普通优先级：业务逻辑
onLeftClick(priority = EventPriority.NORMAL) { context ->
    handleBusinessLogic(context)
}

// 低优先级：日志记录
onLeftClick(priority = EventPriority.LOWEST) { context ->
    logEvent(context)
}
```

### 2. 错误处理

```kotlin
onLeftClick { context ->
    try {
        handleAction(context)
    } catch (e: Exception) {
        logger.error("事件处理失败", e)
        context.player.sendMessage("&c操作失败，请稍后重试")

        // 可选：回滚操作
        rollbackAction(context)
    }
}
```

### 3. 性能优化

```kotlin
// 避免重复计算
private val cachedResults = mutableMapOf<String, Any>()

onLeftClick { context ->
    val cacheKey = generateCacheKey(context)
    val result = cachedResults.getOrPut(cacheKey) {
        expensiveCalculation(context)
    }

    handleResult(context, result)
}
```

## 🔗 相关链接

- [组件API](components.md) - 组件相关接口
- [页面API](pages.md) - 页面相关接口
- [事件处理教程](../tutorials/03-events.md) - 事件处理详细教程
- [事件处理示例](../examples/advanced/event-handling-examples.md) - 实际使用示例
