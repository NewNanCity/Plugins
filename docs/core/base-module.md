# BaseModule 模块化架构指南

## 📋 概述

BaseModule 是 Core 模块提供的现代化模块开发基类，旨在简化模块开发、提供自动资源管理和完整的生命周期支持。

## 🎯 核心特性

### 1. 自动资源管理
- 同时实现 `Terminable` 和 `TerminableConsumer` 接口
- 自动绑定到父级（插件或父模块）
- 模块销毁时自动清理所有绑定的资源

### 2. 模块级上下文
- 事件订阅绑定到模块而不是插件
- 调度任务绑定到模块而不是插件
- 任务调度器绑定到模块
- 模块销毁时相关资源自动清理

### 3. 层次化模块管理
- 支持子模块嵌套
- 父模块销毁时自动销毁所有子模块
- 重载时自动重载所有子模块

### 4. 完整的生命周期
- `onInit()`: 模块初始化
- `onReload()`: 模块重载
- `onClose()`: 模块关闭

## 🚀 快速开始

### 基本用法

```kotlin
// 1. 定义模块（推荐模式）
class PlayerManager(
    moduleName: String,
    val plugin: MyPlugin  // ✅ 声明为具体Plugin类型的属性
) : BaseModule(moduleName, plugin) {

    // ✅ 重要：手动调用init()来触发初始化
    init { init() }

    override fun onInit() {
        logger.info("PlayerManager initializing...")

        // 事件绑定到模块
        subscribeEvent<PlayerJoinEvent> { event ->
            handlePlayerJoin(event.player)
        }

        // 调度任务绑定到模块
        runAsyncRepeating(0L, 20L * 60) {
            cleanupPlayerData()
        }
    }

    override fun onReload() {
        logger.info("PlayerManager reloading...")
        // ✅ 直接访问插件特定功能，无需类型转换
        plugin.getPlayerConfig().let { config ->
            // 重载玩家配置
        }
    }

    private fun handlePlayerJoin(player: Player) {
        // ✅ 直接使用具体类型的plugin
        plugin.notifyPlayerJoin(player)
    }

    private fun cleanupPlayerData() {
        // ✅ 访问插件特有的方法
        plugin.getPlayerDataManager().cleanup()
    }
}

// 2. 在插件中使用
class MyPlugin : BasePlugin() {
    private lateinit var playerManager: PlayerManager

    override fun onPluginEnable() {
        // 初始化模块
        playerManager = PlayerManager("PlayerManager", this)

        // 调用模块方法
        playerManager.setupPlayerTracking()

        reloadPlugin()
    }

    override fun reloadPlugin() {
        // 重载所有子模块
        super.reloadPlugin()
    }
}
```

## 🏗️ 构造器选项

BaseModule 提供多种构造器以适应不同场景：

### 1. 基础构造器
```kotlin
class MyModule(
    moduleName: String,
    plugin: Plugin,
    logger: Ilogger,
    messager: IMessager
) : BaseModule(moduleName, plugin, logger, messager)
```

### 2. 插件子模块构造器（推荐）
```kotlin
class MyModule(
    moduleName: String,
    val plugin: MyPlugin  // ✅ 声明为具体Plugin类型的属性
) : BaseModule(moduleName, plugin)
// 自动使用插件的 logger 和 messager，并自动绑定到插件
```

**重要提示**：推荐在构造函数中声明`val plugin: MyPlugin`属性，这样可以：
- 直接访问插件特定的方法和属性
- 避免需要类型转换 `(bukkitPlugin as MyPlugin)`
- 提供更好的类型安全和IDE支持

### 3. 嵌套子模块构造器
```kotlin
class ChildModule(moduleName: String, parentModule: BaseModule) : BaseModule(moduleName, parentModule)
// 自动使用父模块的 logger 和 messager，并自动绑定到父模块
```

## 📚 详细功能

### 事件处理

```kotlin
class EventModule(moduleName: String, plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    override fun onInit() {
        // 简单事件订阅
        subscribeEvent<PlayerJoinEvent> { event ->
            event.player.sendMessage("欢迎！")
        }

        // 复杂事件订阅
        subscribeEvent<PlayerMoveEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            filter { event ->
                val from = event.from
                val to = event.to ?: return@filter false
                from.chunk != to.chunk
            }
            handler { event ->
                handleChunkChange(event.player, event.to!!.chunk)
            }
            onException { event, e ->
                logger.error("处理区块变更事件失败", e)
            }
        }
    }
}
```

### 调度任务

```kotlin
class SchedulerModule(moduleName: String, plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    override fun onInit() {
        // 同步任务
        runSync {
            logger.info("同步任务执行")
        }

        // 延迟任务
        runSyncLater(20L) {
            logger.info("1秒后执行")
        }

        // 重复任务
        runAsyncRepeating(0L, 20L * 60) {
            performPeriodicCleanup()
        }

        // 使用时间单位
        runAsyncLater(5L, TimeUnit.SECONDS) {
            logger.info("5秒后执行")
        }
    }
}
```

### 异步任务支持

```kotlin
class AsyncModule(moduleName: String, plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    override fun onInit() {
        // 启动异步任务
        runAsync {
            performAsyncOperation()
        }

        // 同步任务
        runSync {
            performSyncOperation()
        }

        // 重复任务
        runSyncRepeating(0L, 20L) { task ->
            updatePlayerStats()
        }
    }

    private fun performAsyncOperation() {
        // 异步操作
    }
}
```

### 子模块管理

```kotlin
class ParentModule(moduleName: String, plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    private lateinit var childModule1: ChildModule
    private lateinit var childModule2: ChildModule

    override fun onInit() {
        // 初始化子模块
        childModule1 = ChildModule("Child1", this)
        childModule2 = ChildModule("Child2", this)
    }

    // 获取子模块
    fun getChildModules(): List<ChildModule> {
        return getChildren(ChildModule::class.java)
    }

    fun getFirstChild(): ChildModule? {
        return getFirstChildOrNull(ChildModule::class.java)
    }
}

class ChildModule(moduleName: String, parentModule: BaseModule) : BaseModule(moduleName, parentModule) {

    override fun onInit() {
        logger.info("子模块 $moduleName 初始化")
        setupFeature1()
    }

    fun setupFeature1() {
        // 功能设置
    }
}
```

## 🔄 生命周期管理

### 初始化流程
1. 构造器调用
2. **手动调用 `init()` 方法**（重要！）
3. `onInit()` 方法调用
4. 标记为已初始化

**⚠️ 重要变更**：从 2.0 版本开始，BaseModule 不再在构造函数中自动调用 `onInit()`。子类必须在 `init` 块中手动调用 `init()` 方法来触发初始化。这确保了子类的属性在 `onInit()` 执行时已经正确初始化。

### 重载流程
1. 调用 `onReload()` 方法
2. 递归重载所有子模块
3. 异常处理和日志记录

### 关闭流程
1. 调用 `onClose()` 方法
2. 关闭所有绑定的资源（包括子模块）
3. 清理内部状态

## 🎯 最佳实践

### 1. 具体Plugin类型声明

**强烈推荐**：在模块构造函数中声明具体的Plugin类型属性：

```kotlin
// ✅ 推荐：声明具体Plugin类型的属性
class DataModule(
    moduleName: String,
    val plugin: MyPlugin  // 声明为具体类型的属性
) : BaseModule(moduleName, plugin) {

    // ✅ 重要：手动调用init()来触发初始化
    init { init() }

    override fun onInit() {
        // ✅ 直接访问插件特定功能
        plugin.getDataConfig().let { config ->
            setupDatabase(config.databaseUrl)
        }

        // ✅ 使用插件特有的管理器
        plugin.getCustomManager().registerModule(this)
    }

    override fun onReload() {
        // ✅ 重载时直接访问插件功能
        plugin.reloadDataConfig()
        plugin.getCustomManager().refreshModule(this)
    }

    fun performDataOperation() {
        // ✅ 在任何方法中都可以直接使用
        plugin.getDataSource().execute { /* 数据操作 */ }
    }
}

// ❌ 不推荐：需要类型转换
class BadDataModule(moduleName: String, plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    override fun onInit() {
        // ❌ 需要类型转换，不优雅且容易出错
        (bukkitPlugin as MyPlugin).getDataConfig()

        // ❌ 每次都需要转换
        val myPlugin = bukkitPlugin as MyPlugin
        myPlugin.getCustomManager().registerModule(this)
    }
}
```

**优势**：
- **类型安全**：编译时检查，避免ClassCastException
- **代码简洁**：无需重复的类型转换
- **IDE友好**：完整的代码补全和重构支持
- **可维护性**：代码意图明确，易于理解和维护

## ⚠️ 重要注意事项

### 0. 手动初始化要求（重要！）

**从 2.0 版本开始的重要变更**：BaseModule 不再在构造函数中自动调用 `onInit()`。这是为了解决子类属性未初始化就被访问的问题。

**必须遵循的规范**：
```kotlin
class MyModule(
    moduleName: String,
    val plugin: MyPlugin
) : BaseModule(moduleName, plugin) {

    // ✅ 必须：在init块中手动调用init()
    init { init() }

    override fun onInit() {
        // 现在可以安全地访问子类的所有属性
        plugin.someMethod() // ✅ plugin已经正确初始化
    }
}
```

**为什么需要这个变更**：
- **问题**：之前BaseModule在构造函数中立即调用onInit()，但此时子类的属性还未初始化
- **后果**：在onInit()中访问子类属性会导致NullPointerException
- **解决方案**：子类必须在init块中手动调用init()，确保属性初始化完成后再执行onInit()

### 1. 模块初始化规范
**重要**：BaseModule必须使用lateinit并在onPluginEnable中初始化，确保正确注册到childModules。

```kotlin
class MyPlugin : BasePlugin() {
    // ✅ 正确：使用lateinit声明模块
    private lateinit var playerModule: PlayerModule
    private lateinit var economyModule: EconomyModule

    override fun onPluginEnable() {
        // ✅ 正确：在onPluginEnable中初始化模块
        // 注意：模块构造时会自动调用init()进行初始化
        playerModule = PlayerModule("PlayerModule", this)
        economyModule = EconomyModule("EconomyModule", this)

        // 模块会自动注册到childModules，重载时会被正确重载
        reloadPlugin()
    }

    override fun reloadPlugin() {
        // 所有已初始化的模块都会被重载
        super.reloadPlugin()
    }
}

// 模块定义示例
class PlayerModule(
    moduleName: String,
    val plugin: MyPlugin
) : BaseModule(moduleName, plugin) {

    // ✅ 重要：手动调用init()来触发初始化
    init { init() }

    override fun onInit() {
        // 初始化逻辑
    }
}
```

**❌ 不要使用lazy委托**：
```kotlin
class MyPlugin : BasePlugin() {
    // ❌ 错误：不要使用lazy委托
    private val playerModule: PlayerModule by lazy {
        PlayerModule("PlayerModule", this)
    }

    // lazy模块需要主动触发初始化，容易遗漏，不推荐使用
}
```

### 2. 重载机制
```kotlin
class MyPlugin : BasePlugin() {
    override fun reloadPlugin() {
        try {
            // 插件特定的重载逻辑
            configManager.clearCache()
            setupLanguageManager(...)

            // 必须调用父类方法以重载所有子模块
            super.reloadPlugin()

        } catch (e: Exception) {
            logger.error("重载失败", e)
            throw e
        }
    }
}
```

### 2. 异常处理
- 模块初始化异常会抛出 `ModuleInitializationException`
- 重载和关闭异常会被捕获并记录日志
- 不会影响其他模块的正常运行

### 3. 资源绑定
```kotlin
override fun onInit() {
    // ✅ 正确：资源自动绑定到模块
    val customResource = MyCustomResource()
    bind(customResource)

    // ✅ 正确：事件自动绑定到模块
    subscribeEvent<PlayerJoinEvent> { ... }

    // ✅ 正确：任务自动绑定到模块
    runAsyncRepeating(0L, 20L) { ... }
}
```

## 📊 最佳实践

### 1. 模块命名
- 使用描述性的模块名称
- 遵循驼峰命名规范
- 避免重复的模块名称

### 2. 模块职责
- 单一职责原则
- 高内聚低耦合
- 清晰的模块边界

### 3. 错误处理
- 在 `onInit()` 中进行必要的验证
- 使用适当的异常处理
- 提供有意义的错误信息

### 4. 性能考虑
- 避免在 `onInit()` 中执行耗时操作
- 使用异步任务处理重量级操作
- 合理使用缓存机制

## 🔗 相关文档

- [Core 模块最佳实践](best-practices.md)
- [事件系统教程](event-system-tutorial.md)
- [调度器教程](scheduler-tutorial.md)
- [任务调度系统文档](scheduler.md)
