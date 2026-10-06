# GUI1 故障排除

本文档帮助你快速诊断和解决GUI1使用过程中遇到的常见问题。

## 🚨 常见问题快速诊断

### 问题分类

根据症状快速定位问题类型：

| 症状         | 可能原因     | 快速检查                   |
| ------------ | ------------ | -------------------------- |
| GUI无法打开  | 初始化问题   | [检查初始化](#gui无法打开) |
| 按钮无响应   | 事件处理问题 | [检查事件](#按钮无响应)    |
| 物品显示错误 | 渲染问题     | [检查渲染](#物品显示错误)  |
| 内存泄漏     | 资源管理问题 | [检查资源](#内存泄漏)      |
| 性能问题     | 优化问题     | [检查性能](#性能问题)      |

## 🔧 具体问题解决

### GUI无法打开

**症状**：调用`openPage`后没有界面显示

**诊断步骤**：

1. **检查插件继承**
```kotlin
// ✅ 正确：继承BasePlugin
class MyPlugin : BasePlugin() {
    // ...
}

// ❌ 错误：没有继承BasePlugin
class MyPlugin : JavaPlugin() {
    // GUI1需要BasePlugin的支持
}
```

2. **检查模块依赖**
```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":modules:gui"))
    implementation(project(":modules:core")) // 必需的依赖
}
```

3. **检查玩家状态**
```kotlin
fun showGUI(player: Player) {
    // 检查玩家是否在线
    if (!player.isOnline) {
        logger.warn("尝试为离线玩家显示GUI: ${player.name}")
        return
    }

    // 检查玩家是否在正确的世界
    if (player.world.name == "some_restricted_world") {
        player.sendMessage("&c在此世界无法使用GUI")
        return
    }

    openPage(InventoryType.CHEST, 27, player) {
        title("测试GUI")
    }
}
```

4. **检查错误日志**
```bash
# 查看插件日志
tail -f plugins/YourPlugin/logs/plugin-$(date +%Y-%m-%d).log | grep -i error
```

### 按钮无响应

**症状**：点击按钮没有任何反应

**诊断步骤**：

1. **检查事件注册**
```kotlin
slotComponent(x = 4, y = 2) {
    render {
        itemUtil.createItemStack(Material.DIAMOND) {
            name("&e测试按钮")
        }
    }

    // 确保注册了事件处理器
    onLeftClick { context ->
        // 添加调试日志
        logger.info("按钮被点击: ${context.player.name}")
        context.player.sendMessage("&a按钮工作正常！")
    }
}
```

2. **检查权限**
```kotlin
onLeftClick { context ->
    // 检查权限
    if (!context.player.hasPermission("myplugin.use")) {
        context.player.sendMessage("&c权限不足！")
        return@onLeftClick
    }

    // 执行操作
    performAction()
}
```

3. **检查异常**
```kotlin
onLeftClick { context ->
    try {
        performAction()
    } catch (e: Exception) {
        logger.error("按钮点击处理失败", e)
        context.player.sendMessage("&c操作失败，请查看日志")
    }
}
```

4. **检查事件冲突**
```kotlin
// 确保没有其他插件拦截事件
onLeftClick { context ->
    logger.info("事件详情: ${context.event}")
    logger.info("事件是否被取消: ${context.event.isCancelled}")
}
```

### 物品显示错误

**症状**：GUI中显示空白、错误物品或崩溃的物品

**诊断步骤**：

1. **检查render函数**
```kotlin
slotComponent(x = 4, y = 2) {
    render { context ->
        try {
            // 确保总是返回有效物品
            return@render itemUtil.createItemStack(Material.DIAMOND) {
                name("&e正常物品")
            }
        } catch (e: Exception) {
            logger.error("物品渲染失败", e)
            // 返回备用物品
            return@render itemUtil.createItemStack(Material.BARRIER) {
                name("&c渲染错误")
                lore("&7请检查日志")
            }
        }
    }
}
```

2. **检查材质有效性**
```kotlin
fun createSafeItem(material: Material?): ItemStack {
    val safeMaterial = when {
        material == null -> Material.BARRIER
        material == Material.AIR -> Material.BARRIER
        !material.isItem -> Material.BARRIER
        else -> material
    }

    return itemUtil.createItemStack(safeMaterial) {
        name("&e安全物品")
    }
}
```

3. **检查NBT数据**
```kotlin
// 避免使用可能损坏的NBT数据
render {
    // ❌ 避免：直接复制可能损坏的物品
    val copiedItem = someExistingItem.clone()

    // ✅ 推荐：重新创建物品
    itemUtil.createItemStack(someExistingItem.type) {
        name(someExistingItem.displayName)
        amount(someExistingItem.amount)
    }
}
```

### 内存泄漏

**症状**：服务器内存持续增长，最终导致OutOfMemoryError

**诊断步骤**：

1. **检查Session清理**
```kotlin
// 确保玩家离线时清理Session
// GUI1会自动处理，但可以手动检查
fun checkSessionCleanup() {
    val onlinePlayers = server.onlinePlayers.map { it.uniqueId }.toSet()
    val sessionPlayers = SessionStorage.getAllPlayerSessions().keys

    val offlineSessionPlayers = sessionPlayers - onlinePlayers
    if (offlineSessionPlayers.isNotEmpty()) {
        logger.warn("发现离线玩家的Session未清理: $offlineSessionPlayers")
    }
}
```

2. **检查组件绑定**
```kotlin
// 确保组件正确绑定到页面生命周期
class MyComponent : BaseComponent() {
    private val scheduler = Schedulers.async(plugin)

    init {
        // ✅ 正确：绑定到组件生命周期
        bind(scheduler)

        // ❌ 错误：没有绑定，会导致泄漏
        // scheduler.runTaskTimer(...)
    }
}
```

3. **检查事件监听器**
```kotlin
// 确保事件监听器正确注销
class MyPlugin : BasePlugin() {
    override fun onPluginDisable() {
        // BasePlugin会自动清理绑定的资源
        // 但要确保没有手动创建未绑定的监听器
        logger.info("插件关闭，检查资源清理...")
    }
}
```

### 性能问题

**症状**：GUI响应缓慢，服务器TPS下降

**诊断步骤**：

1. **检查渲染频率**
```kotlin
// ❌ 避免：频繁更新
repeat(delay = 1L, interval = 1L) { // 每tick更新
    component.update()
}

// ✅ 推荐：合理的更新频率
repeat(delay = 20L, interval = 20L) { // 每秒更新
    component.update()
}
```

2. **检查数据量**
```kotlin
// ❌ 避免：一次性加载大量数据
paginatedComponent(x = 0, y = 1, width = 9, height = 4) {
    items(getAllPlayersEverJoined()) // 可能有数万个玩家
}

// ✅ 推荐：分页加载
paginatedComponent(x = 0, y = 1, width = 9, height = 4) {
    totalItems(getTotalPlayerCount())
    pageSize(36) // 每页36个

    render { index ->
        val player = getPlayerByIndex(index) // 按需加载
        createPlayerItem(player)
    }
}
```

3. **检查异步操作**
```kotlin
// ❌ 避免：在主线程执行耗时操作
onLeftClick {
    val data = loadDataFromDatabase() // 阻塞主线程
    updateGUI(data)
}

// ✅ 推荐：异步执行
onLeftClick {
    player.sendMessage("&e正在加载数据...")

    runAsync {
        val data = loadDataFromDatabase()

        sync {
            updateGUI(data)
            player.sendMessage("&a数据加载完成！")
        }
    }
}
```

## 🔍 调试工具

### 1. 启用详细日志

```kotlin
override fun getCoreConfig(): CorePluginConfig {
    return CorePluginConfig.builder()
        .loggingConfig(
            CorePluginConfig.LoggingConfig.builder()
                .debugEnabled(true)
                .fileLoggingEnabled(true)
                .build()
        )
        .build()
}
```

### 2. GUI状态检查命令

```kotlin
// 添加调试命令
getCommand("guidebug")?.setExecutor { sender, _, _, args ->
    if (sender is Player && sender.hasPermission("myplugin.debug")) {
        when (args.getOrNull(0)) {
            "session" -> {
                val session = SessionStorage.getSession(sender)
                sender.sendMessage("Session信息: ${session?.toString() ?: "无"}")
            }
            "stats" -> {
                val report = guiLogger.generateErrorReport()
                sender.sendMessage(report)
            }
            "memory" -> {
                val runtime = Runtime.getRuntime()
                val used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                val total = runtime.totalMemory() / 1024 / 1024
                sender.sendMessage("内存使用: ${used}MB / ${total}MB")
            }
        }
    }
    true
}
```

### 3. 性能监控

```kotlin
// 监控GUI操作性能
fun monitorGUIPerformance() {
    performanceMonitor.monitor("GUI渲染") {
        page.update()
    }

    performanceMonitor.monitor("事件处理") {
        handleClickEvent()
    }
}
```

## 📋 问题报告模板

当遇到无法解决的问题时，请提供以下信息：

```
### 环境信息
- Minecraft版本: 1.20.1
- 服务器类型: Paper/Spigot/Bukkit
- 插件版本: x.x.x
- Java版本: 17

### 问题描述
[详细描述问题现象]

### 复现步骤
1. 执行命令 /xxx
2. 点击按钮 xxx
3. 观察到问题 xxx

### 期望行为
[描述期望的正确行为]

### 错误日志
```
[粘贴相关的错误日志]
```

### 相关代码
```kotlin
[粘贴相关的代码片段]
```
```

## 🆘 获取帮助

### 1. 检查文档
- [快速入门](../quick-start.md)
- [核心概念](../CONCEPTS.md)
- [最佳实践](best-practices.md)

### 2. 查看示例
- [基础示例](../examples/basic/)
- [高级示例](../examples/advanced/)

### 3. 检查日志
```bash
# 查看最新日志
tail -f plugins/YourPlugin/logs/plugin-$(date +%Y-%m-%d).log

# 搜索错误
grep -i "error\|exception" plugins/YourPlugin/logs/plugin-$(date +%Y-%m-%d).log

# 搜索GUI相关日志
grep -i "gui" plugins/YourPlugin/logs/plugin-$(date +%Y-%m-%d).log
```

### 4. 社区支持
- 查看项目文档
- 提交Issue（使用上面的模板）
- 参考其他插件的实现

记住：大多数问题都有简单的解决方案，仔细检查代码和日志通常能找到答案！
