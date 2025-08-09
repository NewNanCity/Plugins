# 事件处理系统

Core 模块提供了现代化的函数式事件处理系统，支持链式调用、过滤器、自动过期和异常处理，让事件处理更加简洁和安全。

## 🎯 核心特性

### 函数式 API
- **链式调用** - 流畅的 DSL 语法
- **类型安全** - 完整的泛型支持
- **自动资源管理** - 事件监听器自动绑定生命周期

### 高级功能
- **过滤器系统** - 丰富的预定义过滤器
- **自动过期** - 支持时间和次数限制
- **异常安全** - 完善的错误处理机制
- **优先级控制** - 灵活的事件优先级设置

## 🚀 基础用法

### 简单事件订阅

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 基础事件订阅
        subscribeEvent<PlayerJoinEvent> { event ->
            event.player.sendMessage("欢迎加入服务器！")
        }

        // 带优先级的事件订阅
        subscribeEvent<PlayerJoinEvent> {
            priority(EventPriority.HIGH)
            handler { event ->
                loadPlayerData(event.player)
            }
        }
    }
}
```

### 链式配置

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 完整的链式配置
        subscribeEvent<PlayerMoveEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            filter { event ->
                // 只处理跨区块的移动
                val from = event.from
                val to = event.to ?: return@filter false
                from.chunk != to.chunk
            }
            expireAfter(100) // 处理100次后自动注销
            handler { event ->
                handleChunkChange(event.player, event.to!!.chunk)
            }
            onException { event, e ->
                logger.error("处理区块变更失败", e)
            }
        }
    }
}
```

## 🔍 过滤器系统

### 内置过滤器

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 取消状态过滤
        subscribeEvent<PlayerMoveEvent> {
            filter { !it.isCancelled }
            handler { event -> /* 处理未取消的移动事件 */ }
        }

        // 玩家权限过滤
        subscribeEvent<PlayerCommandPreprocessEvent> {
            filter { it.player.isOp }
            handler { event -> /* 只处理管理员命令 */ }
        }

        // 自定义条件过滤
        subscribeEvent<PlayerInteractEvent> {
            filter { event ->
                event.action == Action.RIGHT_CLICK_BLOCK &&
                event.clickedBlock?.type == Material.CHEST
            }
            handler { event -> /* 处理右键点击箱子 */ }
        }

        // 多重过滤器
        subscribeEvent<PlayerChatEvent> {
            filter { !it.isCancelled }
            filter { it.player.hasPermission("chat.color") }
            filter { it.message.contains("&") }
            handler { event ->
                event.message = ChatColor.translateAlternateColorCodes('&', event.message)
            }
        }
    }
}
```

### 复杂过滤逻辑

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 复杂的过滤条件
        subscribeEvent<PlayerMoveEvent> {
            filter { event ->
                val player = event.player
                val from = event.from
                val to = event.to ?: return@filter false

                // 检查多个条件
                player.gameMode == GameMode.SURVIVAL &&
                !player.isFlying &&
                from.world == to.world &&
                from.distance(to) > 0.1
            }
            handler { event ->
                checkPlayerMovement(event.player, event.from, event.to!!)
            }
        }
    }
}
```

## ⏰ 自动过期机制

### 次数过期

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 处理指定次数后自动注销
        subscribeEvent<PlayerJoinEvent> {
            expireAfter(5) // 处理5次后自动注销
            handler { event ->
                event.player.sendMessage("这是前5个加入的玩家之一！")
            }
        }
    }
}
```

### 时间过期

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 指定时间后自动注销
        subscribeEvent<PlayerJoinEvent> {
            expireAfter(Duration.ofMinutes(10)) // 10分钟后自动注销
            handler { event ->
                event.player.sendMessage("活动期间加入的玩家！")
            }
        }
    }
}
```

### 条件过期

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        val eventEndTime = System.currentTimeMillis() + 3600000 // 1小时后

        // 条件满足时自动注销
        subscribeEvent<PlayerJoinEvent> {
            expireWhen { System.currentTimeMillis() > eventEndTime }
            handler { event ->
                event.player.sendMessage("活动还在进行中！")
            }
        }
    }
}
```

## 🛡️ 异常处理

### 统一异常处理

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 使用 onException 处理异常
        subscribeEvent<PlayerJoinEvent> {
            handler { event ->
                val player = event.player
                val playerData = loadPlayerData(player.uniqueId) // 可能抛出异常
                updatePlayerDisplay(player, playerData)
            }
            onException { event, e ->
                logger.error("玩家加入事件处理失败", e)
                handlePlayerJoinFallback(event.player)
            }
        }
    }

    private fun handlePlayerJoinFallback(player: Player) {
        // 提供基础的欢迎功能
        player.sendMessage("欢迎加入服务器！")
    }
}
```

### 内部异常处理

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 在 handler 内部处理异常
        subscribeEvent<PlayerQuitEvent> {
            handler { event ->
                try {
                    val player = event.player
                    savePlayerData(player.uniqueId)
                    cleanupPlayerResources(player)
                } catch (e: Exception) {
                    logger.error("保存玩家数据失败", e)
                    // 不影响其他玩家的退出处理
                }
            }
        }
    }
}
```

## 🔧 在 BaseModule 中使用

```kotlin
class PlayerModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    override fun onInit() {
        // 模块中的事件处理
        subscribeEvent<PlayerJoinEvent> { event ->
            handlePlayerJoin(event.player)
        }

        subscribeEvent<PlayerQuitEvent> { event ->
            handlePlayerQuit(event.player)
        }

        // 复杂的事件处理
        subscribeEvent<PlayerInteractEvent> {
            filter { it.action == Action.RIGHT_CLICK_BLOCK }
            filter { it.clickedBlock?.type == Material.SIGN }
            handler { event ->
                handleSignInteraction(event.player, event.clickedBlock!!)
            }
            onException { event, e ->
                logger.error("标牌交互处理失败", e)
                event.player.sendMessage("操作失败，请稍后重试")
            }
        }
    }

    private fun handlePlayerJoin(player: Player) {
        // 玩家加入处理逻辑
        plugin.getPlayerConfig().let { config ->
            if (config.welcomeMessage.enabled) {
                player.sendMessage(config.welcomeMessage.text)
            }
        }
    }

    private fun handlePlayerQuit(player: Player) {
        // 玩家退出处理逻辑
        plugin.cleanupPlayerData(player)
    }

    private fun handleSignInteraction(player: Player, block: Block) {
        // 标牌交互处理逻辑
    }
}
```

## ⚡ 性能优化

### 批量处理

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        val pendingUpdates = ConcurrentHashMap<Player, Location>()

        // 收集移动事件
        subscribeEvent<PlayerMoveEvent> {
            filter { it.to != null }
            filter { event ->
                val from = event.from
                val to = event.to!!
                from.distance(to) > 0.1 // 过滤微小移动
            }
            handler { event ->
                pendingUpdates[event.player] = event.to!!
            }
        }

        // 每秒批量处理一次
        runSyncRepeating(0L, 20L) {
            if (pendingUpdates.isNotEmpty()) {
                val updates = pendingUpdates.toMap()
                pendingUpdates.clear()

                processBatchLocationUpdates(updates)
            }
        }
    }

    private fun processBatchLocationUpdates(updates: Map<Player, Location>) {
        // 批量处理位置更新
        updates.forEach { (player, location) ->
            updatePlayerLocation(player, location)
        }
    }
}
```

### 智能过滤

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 使用多级过滤减少不必要的处理
        subscribeEvent<PlayerMoveEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled } // 第一级：快速过滤
            filter { event ->
                // 第二级：距离过滤
                val from = event.from
                val to = event.to ?: return@filter false
                from.distance(to) > 1.0
            }
            filter { event ->
                // 第三级：区块过滤
                val from = event.from
                val to = event.to!!
                from.chunk != to.chunk
            }
            handler { event ->
                handleSignificantMovement(event.player, event.from, event.to!!)
            }
        }
    }
}
```

## 🎯 使用场景示例

### 临时事件监听

```kotlin
class MyPlugin : BasePlugin() {

    fun startTemporaryEvent() {
        // 临时活动：前10个加入的玩家获得奖励
        subscribeEvent<PlayerJoinEvent> {
            expireAfter(10) // 处理10次后自动注销
            handler { event ->
                val player = event.player
                giveReward(player)
                player.sendMessage("恭喜你获得了活动奖励！")
            }
        }
    }
}
```

### 条件性事件处理

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 只在特定时间段处理事件
        subscribeEvent<PlayerChatEvent> {
            filter { isEventTime() }
            filter { !it.isCancelled }
            handler { event ->
                processEventChat(event.player, event.message)
            }
        }
    }

    private fun isEventTime(): Boolean {
        val hour = LocalTime.now().hour
        return hour in 20..22 // 晚上8点到10点
    }
}
```

### 复杂的交互处理

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 处理特定物品的右键点击
        subscribeEvent<PlayerInteractEvent> {
            filter { it.action == Action.RIGHT_CLICK_AIR || it.action == Action.RIGHT_CLICK_BLOCK }
            filter { event ->
                val item = event.item
                item != null && item.type == Material.COMPASS &&
                item.hasItemMeta() && item.itemMeta.hasDisplayName()
            }
            handler { event ->
                handleCompassUse(event.player, event.item!!)
            }
            onException { event, e ->
                logger.error("指南针使用处理失败", e)
                event.player.sendMessage("指南针使用失败")
            }
        }
    }

    private fun handleCompassUse(player: Player, compass: ItemStack) {
        // 处理指南针使用逻辑
    }
}
```

---

**相关文档：** [🚀 任务调度](scheduler.md) | [ 最佳实践](best-practices.md)
