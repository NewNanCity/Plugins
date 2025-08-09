# GUI 模块性能优化指南

本文档提供GUI模块的性能优化技巧和最佳实践，帮助你构建高性能的GUI界面。

## 🎯 性能优化目标

- **减少内存使用**：避免内存泄漏和不必要的对象创建
- **提升渲染速度**：优化物品创建和更新过程
- **降低服务器负载**：减少主线程阻塞和CPU使用
- **改善用户体验**：提供流畅的交互响应

## 🚀 渲染优化

### 1. 懒加载渲染

只在需要时创建物品，避免不必要的计算：

```kotlin
render { context ->
    // 检查是否需要重新渲染
    if (context.oldItem != null && !needsUpdate()) {
        return@render context.oldItem
    }

    // 只在必要时创建新物品
    item(Material.DIAMOND) {
        name("&b钻石")
        lore("&7珍贵的宝石")
    }
}
```

### 2. 物品缓存

缓存常用的物品，避免重复创建：

```kotlin
class ItemCache {
    private val cache = mutableMapOf<String, ItemStack>()

    fun getOrCreate(key: String, creator: () -> ItemStack): ItemStack {
        return cache.getOrPut(key, creator)
    }

    fun clear() {
        cache.clear()
    }
}

// 使用示例
private val itemCache = ItemCache()

render {
    itemCache.getOrCreate("diamond_button") {
        item(Material.DIAMOND) {
            name("&b钻石按钮")
            lore("&7缓存的物品")
        }
    }
}
```

### 3. 批量更新

使用批量更新减少渲染次数：

```kotlin
// 避免多次单独更新
component1.update()
component2.update()
component3.update()

// 使用批量更新
page.batchUpdate {
    component1.update()
    component2.update()
    component3.update()
}
```

## 💾 内存优化

### 1. 及时清理资源

确保页面关闭时清理所有资源：

```kotlin
page.onClose { context ->
    // 清理缓存
    itemCache.clear()

    // 取消定时任务
    scheduledTasks.forEach { it.cancel() }

    // 清理事件监听器
    eventHandlers.clear()
}
```

### 2. 使用弱引用

对于可能长期持有的对象使用弱引用：

```kotlin
import java.lang.ref.WeakReference

class GuiManager {
    private val playerSessions = mutableMapOf<UUID, WeakReference<Session>>()

    fun getSession(player: Player): Session? {
        val ref = playerSessions[player.uniqueId]
        val session = ref?.get()

        if (session == null) {
            // 清理失效的引用
            playerSessions.remove(player.uniqueId)
        }

        return session
    }
}
```

### 3. 避免内存泄漏

注意以下常见的内存泄漏场景：

```kotlin
// ❌ 错误：持有Player引用可能导致内存泄漏
class BadComponent(private val player: Player) {
    // ...
}

// ✅ 正确：使用UUID或在适当时机清理引用
class GoodComponent(private val playerUUID: UUID) {
    private fun getPlayer(): Player? {
        return Bukkit.getPlayer(playerUUID)
    }
}
```

## 🔄 数据处理优化

### 1. 异步数据加载

将耗时的数据加载操作移到异步线程：

```kotlin
paginatedComponent<PlayerData>(x = 0, y = 1, width = 9, height = 4) {
    setDataProvider(DataProviders.asyncPaged { offset, limit, callback ->
        // 在异步线程中加载数据
        runAsync {
            try {
                val players = database.getPlayers(offset, limit)
                callback(Result.success(players))
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    })

    render { context ->
        val playerData = context.item ?: return@render null
        skull(playerData.uuid) {
            name("&e${playerData.name}")
            lore("&7等级: ${playerData.level}")
        }
    }

    // 显示加载状态
    renderLoadingSlot { context ->
        item(Material.CLOCK) {
            name("&e加载中...")
            lore("&7正在获取数据...")
        }
    }
}
```

### 2. 数据分页

对大量数据使用分页，避免一次性加载：

```kotlin
// ❌ 错误：一次性加载所有数据
val allPlayers = database.getAllPlayers() // 可能有数千个玩家

// ✅ 正确：使用分页加载
paginatedComponent(data = emptyList()) {
    setDataProvider(DataProviders.paged { page, pageSize ->
        database.getPlayers(page, pageSize)
    })
}
```

### 3. 智能缓存策略

根据数据特性选择合适的缓存策略：

```kotlin
// 静态数据：长期缓存
setDataProvider(DataProviders.cached(
    provider = { page, pageSize -> getStaticData(page, pageSize) },
    cacheTime = Duration.ofHours(1)
))

// 动态数据：短期缓存
setDataProvider(DataProviders.cached(
    provider = { page, pageSize -> getDynamicData(page, pageSize) },
    cacheTime = Duration.ofSeconds(30)
))

// 实时数据：不缓存
setDataProvider(DataProviders.realtime { page, pageSize ->
    getRealTimeData(page, pageSize)
})
```

## ⚡ 事件处理优化

### 1. 事件去重

避免重复处理相同的事件：

```kotlin
private var lastClickTime = 0L
private val clickCooldown = 500L // 500ms冷却

onLeftClick { context ->
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastClickTime < clickCooldown) {
        return@onLeftClick // 忽略重复点击
    }
    lastClickTime = currentTime

    // 处理点击事件
    handleClick(context)
}
```

### 2. 异步事件处理

将耗时的事件处理移到异步线程：

```kotlin
onLeftClick { context ->
    // 立即给用户反馈
    context.player.sendMessage("&7处理中...")

    // 异步处理耗时操作
    plugin.runAsync {
        try {
            val result = performExpensiveOperation()

            // 回到主线程更新UI
            plugin.runSync {
                context.player.sendMessage("&a操作完成: $result")
                updateGui(result)
            }
        } catch (e: Exception) {
            plugin.sync {
                context.player.sendMessage("&c操作失败: ${e.message}")
            }
        }
    }
}
```

### 3. 事件优先级

合理设置事件优先级，避免不必要的处理：

```kotlin
// 高优先级：重要的系统事件
onLeftClick(priority = EventPriority.HIGH) { context ->
    if (isSystemCritical(context)) {
        handleSystemEvent(context)
        context.stopPropagation() // 阻止其他处理器
    }
}

// 低优先级：日志记录等
onLeftClick(priority = EventPriority.LOW) { context ->
    logClickEvent(context)
}
```

## 🔧 组件优化

### 1. 组件复用

复用相似的组件，避免重复创建：

```kotlin
// 创建可复用的组件工厂
object ComponentFactory {
    fun createNavigationButton(
        x: Int, y: Int,
        material: Material,
        name: String,
        onClick: (ClickContext) -> Unit
    ): ComponentBuilder {
        return slotComponent(x, y) {
            render {
                item(material) {
                    name(name)
                    lore("&7点击执行操作")
                }
            }
            onLeftClick(onClick)
        }
    }
}

// 使用工厂创建组件
ComponentFactory.createNavigationButton(0, 5, Material.ARROW, "&7返回") {
    page.session.pop()
}
```

### 2. 条件渲染

只渲染必要的组件：

```kotlin
// 根据条件决定是否渲染组件
if (player.hasPermission("admin.gui")) {
    slotComponent(8, 0) {
        render {
            item(Material.COMMAND_BLOCK) {
                name("&c管理员工具")
            }
        }
        onLeftClick {
            openAdminPanel(player)
        }
    }
}
```

### 3. 组件状态管理

合理管理组件状态，避免不必要的更新：

```kotlin
class StatefulComponent {
    private var lastState: ComponentState? = null

    fun render(currentState: ComponentState): ItemStack? {
        // 状态未变化时不重新渲染
        if (currentState == lastState) {
            return null // 返回null表示不更新
        }

        lastState = currentState
        return createItemForState(currentState)
    }
}
```

## 📊 性能监控

### 1. 渲染时间监控

监控渲染性能，识别瓶颈：

```kotlin
class PerformanceMonitor {
    fun measureRenderTime(componentName: String, renderAction: () -> ItemStack): ItemStack {
        val startTime = System.nanoTime()
        val result = renderAction()
        val endTime = System.nanoTime()

        val duration = (endTime - startTime) / 1_000_000.0 // 转换为毫秒
        if (duration > 10.0) { // 超过10ms记录警告
            logger.warn("组件 $componentName 渲染耗时 ${duration}ms")
        }

        return result
    }
}
```

### 2. 内存使用监控

定期检查内存使用情况：

```kotlin
class MemoryMonitor {
    fun checkMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory

        val usagePercent = (usedMemory * 100.0) / totalMemory

        if (usagePercent > 80.0) {
            logger.warn("内存使用率过高: ${usagePercent}%")
            // 触发清理操作
            triggerCleanup()
        }
    }

    private fun triggerCleanup() {
        // 清理缓存
        ItemCache.clearAll()
        // 强制垃圾回收
        System.gc()
    }
}
```

## 💡 性能优化检查清单

### 渲染优化
- [ ] 使用懒加载渲染
- [ ] 实现物品缓存
- [ ] 使用批量更新
- [ ] 避免不必要的重新渲染

### 内存优化
- [ ] 及时清理资源
- [ ] 使用弱引用
- [ ] 避免内存泄漏
- [ ] 合理管理对象生命周期

### 数据优化
- [ ] 异步加载数据
- [ ] 使用数据分页
- [ ] 实现智能缓存
- [ ] 优化数据库查询

### 事件优化
- [ ] 实现事件去重
- [ ] 异步处理耗时操作
- [ ] 合理设置事件优先级
- [ ] 避免事件处理阻塞

### 组件优化
- [ ] 复用相似组件
- [ ] 实现条件渲染
- [ ] 管理组件状态
- [ ] 优化组件结构

## 🔗 相关链接

- [最佳实践](best-practices.md) - 编码规范和建议
- [故障排除](troubleshooting.md) - 性能问题诊断
- [API文档](../api/) - 详细的API参考
- [高级示例](../examples/advanced/) - 性能优化示例
