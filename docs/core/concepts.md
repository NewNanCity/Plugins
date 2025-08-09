# Core 模块核心概念

本文档介绍 Core 模块的核心概念和设计理念，帮助您深入理解框架的工作原理。

## 🎯 设计理念

### 自动化优于手动
Core 模块的核心理念是通过自动化减少开发者的心智负担：
- **自动资源管理** - 无需手动清理资源
- **自动生命周期绑定** - 资源自动跟随插件生命周期
- **自动异常处理** - 统一的错误处理机制

### 类型安全优先
利用 Kotlin 的类型系统提供编译时安全保障：
- **泛型支持** - 完整的泛型类型推导
- **空安全** - 编译时空指针检查
- **DSL 设计** - 类型安全的领域特定语言

### 现代化异步编程
支持多种异步编程模式：
- **任务调度** - 异步任务和非阻塞 IO
- **任务调度器** - 类似 CompletableFuture 的 API
- **事件驱动** - 响应式编程模式

## 🔄 Terminable 资源管理体系

### 核心接口

```kotlin
// 可终止资源接口
interface Terminable : AutoCloseable {
    override fun close()
}

// 资源消费者接口
interface TerminableConsumer {
    fun <T : AutoCloseable> bind(terminable: T): T
}
```

### 工作原理

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 1. 创建资源
        val database = DatabaseConnection()
        val cache = CacheManager()

        // 2. 绑定到插件生命周期
        bind(database)  // 插件禁用时自动调用 database.close()
        bind(cache)     // 插件禁用时自动调用 cache.close()

        // 3. 事件和任务也会自动绑定
        subscribeEvent<PlayerJoinEvent> { /* 处理逻辑 */ }
        runSyncRepeating(0L, 20L) { /* 定时任务 */ }
    }

    // 插件禁用时，所有绑定的资源自动清理
    // 无需手动实现 onPluginDisable()
}
```

### 资源清理顺序
资源按照 LIFO（后进先出）顺序清理，确保依赖关系正确：

```kotlin
bind(database)    // 第一个绑定
bind(cache)       // 第二个绑定
bind(service)     // 第三个绑定

// 清理顺序：service -> cache -> database
```

## 📦 模块化架构

### BaseModule 设计

```kotlin
abstract class BaseModule(
    val moduleName: String,
    val bukkitPlugin: Plugin
) : Terminable, TerminableConsumer {

    // 生命周期方法
    abstract fun onInit()           // 模块初始化
    open fun onReload() {}          // 模块重载
    open fun onClose() {}           // 模块关闭

    // 自动资源管理
    fun <T : AutoCloseable> bind(terminable: T): T

    // 事件处理
    fun <T : Event> subscribeEvent(eventClass: Class<T>): EventSubscription<T>

    // 任务调度
    fun runSync(task: () -> Unit): ITaskHandler<Unit>
    fun runAsync(task: () -> Unit): ITaskHandler<Unit>

    // 任务调度支持
    fun launchSync(block: suspend CoroutineScope.() -> Unit): Job
}
```

### 模块层次结构

```kotlin
class MyPlugin : BasePlugin() {
    private lateinit var playerModule: PlayerModule
    private lateinit var economyModule: EconomyModule

    override fun onPluginEnable() {
        // 初始化模块（自动注册到 childModules）
        playerModule = PlayerModule("PlayerModule", this)
        economyModule = EconomyModule("EconomyModule", this)
    }

    override fun reloadPlugin() {
        // 重载所有子模块
        super.reloadPlugin()
    }
}

class PlayerModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {
    override fun onInit() {
        // 模块初始化逻辑
    }

    override fun onReload() {
        // 模块重载逻辑
    }
}
```

## ⚡ 事件处理系统

### 函数式 API

```kotlin
// 基础事件订阅
subscribeEvent<PlayerJoinEvent> { event ->
    event.player.sendMessage("欢迎！")
}

// 链式调用配置
subscribeEvent<PlayerMoveEvent> {
    priority(EventPriority.MONITOR)
    filter { !it.isCancelled }
    filter { event ->
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
```

### 自动过期机制

```kotlin
// 时间过期
subscribeEvent<PlayerJoinEvent> {
    expireAfter(Duration.ofMinutes(10))
    handler { /* 10分钟后自动注销 */ }
}

// 次数过期
subscribeEvent<PlayerJoinEvent> {
    expireAfter(5) // 处理5次后自动注销
    handler { /* 处理逻辑 */ }
}

// 条件过期
subscribeEvent<PlayerJoinEvent> {
    expireWhen { System.currentTimeMillis() > deadline }
    handler { /* 条件满足时自动注销 */ }
}
```

## 🚀 任务调度系统

### ITaskHandler API

```kotlin
// 基础任务调度
val task: ITaskHandler<String> = runAsync {
    loadDataFromDatabase()
}

// 链式调用
task.thenApply { data ->
    processData(data)
}.thenRunSync { processedData ->
    updateGameState(processedData)
}.handle { result, exception ->
    if (exception != null) {
        logger.error("任务执行失败", exception)
        getDefaultResult()
    } else {
        result
    }
}
```

### 任务依赖管理

```kotlin
val configTask = runAsync { loadConfiguration() }
val dbTask = runAsync { connectToDatabase() }

// 等待依赖任务完成
val initTask = runAsync(dependencies = listOf(configTask, dbTask)) { handler ->
    val config = configTask.getNow(null)!!
    val database = dbTask.getNow(null)!!
    initializeSystem(config, database)
}

// 组合任务
val allTask = combinedTaskHandlers(CombindMode.ALL, configTask, dbTask)
allTask.thenRunSync { _ ->
    logger.info("所有任务完成")
}
```

## 🔄 任务调度系统

### 并行任务执行

```kotlin
runAsync {
    // 并行执行多个任务
    val task1 = runAsync { performTask1() }
    val task2 = runAsync { performTask2() }
    val task3 = runAsync { performTask3() }

    // 等待所有任务完成
    val result1 = task1.get()
    val result2 = task2.get()
    val result3 = task3.get()

    // 处理结果
    processResults(result1, result2, result3)
}
```

### 并发协调

```kotlin
launchSync {
    // 并发执行多个 IO 操作
    val data1 = async { loadFromAPI1() }
    val data2 = async { loadFromAPI2() }
    val data3 = async { loadFromFile() }

    // 等待所有操作完成
    val results = listOf(
        data1.await(),
        data2.await(),
        data3.await()
    )

    // 处理合并结果
    val combined = combineResults(results)

    // 在主线程中更新游戏状态
    withSync {
        applyToGame(combined)
    }
}
```

## 💬 消息系统

### 统一消息 API

```kotlin
// 自动格式检测
messager.printf(player, "&a成功！")                    // Legacy 格式
messager.printf(player, "<green>成功！</green>")        // MiniMessage 格式

// 多语言支持
messager.printf(player, "<%welcome.message%>", player.name)

// 控制台日志
logger.info("<%plugin.enabled%>")
logger.error("<%plugin.error%>", exception)
```

### 格式支持

| 格式            | 示例                                 | 特性                       |
| --------------- | ------------------------------------ | -------------------------- |
| **Legacy**      | `&a&l成功！`                         | 传统颜色代码               |
| **MiniMessage** | `<green><bold>成功！</bold></green>` | 现代化标签，支持点击、悬停 |
| **Plain**       | `成功！`                             | 纯文本，无格式             |

## 🔧 配置管理

### 多格式支持

```kotlin
// 支持多种配置格式
configManager.parse<MyConfig>("config.yml")      // YAML
configManager.parse<MyConfig>("config.json")     // JSON
configManager.parse<MyConfig>("config.toml")     // TOML
configManager.parse<MyConfig>("config.conf")     // HOCON
```

### 配置合并

```kotlin
// 自动检测并补全缺失配置
configManager.touchWithMerge("config.yml", createBackup = true)

// 解析配置
val config = configManager.parse<MyPluginConfig>("config.yml")
```

---

**下一步：** [🚀 快速开始](quick-start.md) | [🔧 BasePlugin 详解](base-plugin.md)
