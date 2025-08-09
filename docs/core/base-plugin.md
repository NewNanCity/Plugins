# BasePlugin 插件基类

BasePlugin 是 Core 模块提供的增强插件基类，继承自 JavaPlugin 并提供了完整的现代化插件开发基础设施。

## 🎯 核心特性

### 🔄 自动资源管理
- 实现 TerminableConsumer 接口，自动管理所有绑定资源
- 插件禁用时自动清理事件监听器、任务调度等
- 防止内存泄漏，无需手动清理代码

### ⚡ 现代化异步编程
- ITaskHandler 任务调度系统，类似 CompletableFuture API
- 自动生命周期绑定，任务自动跟随插件生命周期

### 💬 统一消息系统
- 集成 MessageManager，支持多格式消息（Legacy、MiniMessage、Plain）
- 自动国际化支持，类型安全的消息模板
- 统一的日志记录系统

### 📦 模块化支持
- BaseModule 子模块自动管理
- 模块重载和生命周期同步
- 层次化模块结构支持

## 🚀 基本用法

### 创建插件主类

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginLoad() {
        // 插件加载阶段（可选）
        logger.info("插件正在加载...")
    }

    override fun onPluginEnable() {
        // 插件启用阶段
        logger.info("插件正在启用...")

        // 注册不可重载的功能
        registerCommands()
        registerEventListeners()

        // 调用重载方法处理可重载功能
        reloadPlugin()

        logger.info("插件启用完成")
    }

    override fun onPluginDisable() {
        // 插件禁用阶段（可选）
        logger.info("插件正在禁用...")
        // 所有资源自动清理，通常无需手动处理
    }

    override fun reloadPlugin() {
        try {
            logger.info("正在重载配置...")

            // 1. 清理配置缓存
            configManager.clearCache()

            // 2. 重新设置语言管理器
            setupLanguageManager()

            // 3. 其他可重载的逻辑
            setupFeatures()

            logger.info("配置重载完成")
        } catch (e: Exception) {
            logger.error("配置重载失败", e)
            throw e
        }
    }

    private fun setupLanguageManager() {
        setupLanguageManager(
            languageFiles = mapOf(
                Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                Locale.US to "lang/en_US.yml"
            ),
            majorLanguage = Locale.SIMPLIFIED_CHINESE,
            defaultLanguage = Locale.US
        )
    }
}
```

## 🔧 生命周期方法

### 三阶段生命周期

```kotlin
class MyPlugin : BasePlugin() {

    // 1. 加载阶段 - 插件类加载时调用
    override fun onPluginLoad() {
        // 基础初始化，不依赖其他插件
        initializeBasicComponents()
    }

    // 2. 启用阶段 - 插件启用时调用
    override fun onPluginEnable() {
        // 主要初始化逻辑
        setupDependencies()
        registerComponents()
        reloadPlugin()
    }

    // 3. 禁用阶段 - 插件禁用时调用
    override fun onPluginDisable() {
        // 可选的清理逻辑
        // 大部分资源会自动清理
        saveImportantData()
    }

    // 重载方法 - 配置重载时调用
    override fun reloadPlugin() {
        // 可重载的功能配置
        reloadConfiguration()
        reloadModules()
    }
}
```

### 生命周期最佳实践

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // ✅ 在启用阶段进行的操作

        // 1. 检查依赖（不可重载）
        if (!checkDependencies()) {
            logger.error("依赖检查失败")
            server.pluginManager.disablePlugin(this)
            return
        }

        // 2. 注册命令（不可重载）
        registerCommands()

        // 3. 注册事件监听器（不可重载）
        registerEventListeners()

        // 4. 调用重载方法（可重载）
        reloadPlugin()
    }

    override fun reloadPlugin() {
        // ✅ 在重载方法中进行的操作

        // 1. 清理配置缓存
        configManager.clearCache()

        // 2. 重新设置语言管理器
        setupLanguageManager()

        // 3. 重载模块配置
        super.reloadPlugin() // 重载所有子模块

        // 4. 重新初始化可变功能
        setupDynamicFeatures()
    }
}
```

## 📦 模块化开发

### 使用 BaseModule

```kotlin
class MyPlugin : BasePlugin() {
    // 使用 lateinit 声明模块
    private lateinit var playerModule: PlayerModule
    private lateinit var economyModule: EconomyModule
    private lateinit var commandRegistry: CommandRegistry

    override fun onPluginEnable() {
        // 在 onPluginEnable 中初始化模块
        playerModule = PlayerModule("PlayerModule", this)
        economyModule = EconomyModule("EconomyModule", this)
        commandRegistry = CommandRegistry(this)

        // 调用模块方法
        playerModule.setupPlayerTracking()
        economyModule.setupEconomyIntegration()

        reloadPlugin()
    }

    override fun reloadPlugin() {
        // 重载所有子模块
        super.reloadPlugin()
    }

    // 提供模块访问方法
    fun getPlayerModule(): PlayerModule = playerModule
    fun getEconomyModule(): EconomyModule = economyModule
}
```

### 模块间通信

```kotlin
class PlayerModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    override fun onInit() {
        subscribeEvent<PlayerJoinEvent> { event ->
            // 通过插件访问其他模块
            plugin.getEconomyModule().setupPlayerAccount(event.player)
        }
    }

    fun notifyPlayerAction(player: Player, action: String) {
        // 模块间数据传递
        plugin.getEconomyModule().recordTransaction(player, action)
    }
}
```

## ⚡ 异步编程支持

### 任务调度

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 同步任务
        runSync {
            logger.info("同步任务执行")
        }

        // 异步任务
        runAsync {
            val data = loadDataFromDatabase()
            processData(data)
        }.thenRunSync { result ->
            // 回到主线程更新游戏状态
            updateGameState(result)
        }

        // 延迟任务
        runSyncLater(20L) { // 1秒后
            server.broadcastMessage("延迟消息")
        }

        // 重复任务
        runSyncRepeating(0L, 20L) { // 每秒
            updatePlayerDisplays()
        }

        // 异步重复任务
        runAsyncRepeating(0L, 20L * 60) { // 每分钟
            performMaintenanceTasks()
        }
    }
}
```

## 💬 消息和国际化

### 消息系统

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 设置消息前缀
        messager.setPlayerPrefix("&7[&6MyPlugin&7] &f")
        messager.setConsolePrefix("[MyPlugin] ")

        reloadPlugin()
    }

    override fun reloadPlugin() {
        // 设置语言管理器
        setupLanguageManager(
            languageFiles = mapOf(
                Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                Locale.US to "lang/en_US.yml"
            ),
            majorLanguage = Locale.SIMPLIFIED_CHINESE,
            defaultLanguage = Locale.US
        )

        super.reloadPlugin()
    }

    private fun sendMessages(player: Player) {
        // 发送消息给玩家
        messager.printf(player, "<%welcome.message%>", player.name)

        // 发送格式化消息
        messager.printf(
            sendTo = player,
            prefix = true,
            formatText = "<%player.level_info%>",
            player.name, player.level
        )

        // 控制台日志
        logger.info("<%plugin.player_joined%>", player.name)
    }
}
```

### 语言文件示例

```yaml
# lang/zh_CN.yml
welcome:
  message: "欢迎 {0} 加入服务器！"

player:
  level_info: "玩家 {0} 当前等级：{1}"

plugin:
  enabled: "插件已启用"
  disabled: "插件已禁用"
  player_joined: "玩家 {0} 加入了服务器"
```

## 🔧 配置管理

### 配置类设计

```kotlin
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MyPluginConfig(
    @JsonProperty("debug")
    val debug: Boolean = false,

    @JsonProperty("database")
    val database: DatabaseConfig = DatabaseConfig(),

    @JsonProperty("features")
    val features: FeatureConfig = FeatureConfig()
) {
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        logging.logLevel = if (debug) LogLevel.DEBUG else LogLevel.INFO
        logging.fileLoggingEnabled = true
        logging.logFilePrefix = "MyPlugin_"

        message.playerPrefix = "&7[&6MyPlugin&7] &f"
        message.consolePrefix = "[MyPlugin] "
    }
}
```

### 配置方法实现

```kotlin
class MyPlugin : BasePlugin() {

    fun getPluginConfig(): MyPluginConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<MyPluginConfig>("config.yml")
    }

    override fun getCoreConfig(): CorePluginConfig {
        return getPluginConfig().getCoreConfig()
    }

    override fun reloadPlugin() {
        try {
            // 清理配置缓存
            configManager.clearCache()

            // 重新设置语言管理器
            setupLanguageManager()

            // 应用新配置
            val config = getPluginConfig()
            applyConfiguration(config)

            super.reloadPlugin()
        } catch (e: Exception) {
            logger.error("配置重载失败", e)
            throw e
        }
    }
}
```

## 🛡️ 错误处理

### 异常安全的初始化

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        try {
            // 检查依赖
            if (!checkDependencies()) {
                throw IllegalStateException("依赖检查失败")
            }

            // 初始化组件
            initializeComponents()

            // 注册功能
            registerFeatures()

            // 重载配置
            reloadPlugin()

            logger.info("插件启用成功")

        } catch (e: Exception) {
            logger.error("插件启用失败", e)

            // 禁用插件
            server.pluginManager.disablePlugin(this)
            throw e
        }
    }

    override fun reloadPlugin() {
        try {
            logger.info("正在重载配置...")

            // 重载逻辑
            performReload()

            logger.info("配置重载成功")

        } catch (e: Exception) {
            logger.error("配置重载失败", e)

            // 可以选择回滚到默认配置
            loadDefaultConfiguration()
            throw e
        }
    }
}
```

## 📊 性能监控

### 内置监控功能

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 启用调试模式时的性能监控
        if (getPluginConfig().debug) {
            enablePerformanceMonitoring()
        }

        reloadPlugin()
    }

    private fun enablePerformanceMonitoring() {
        // 监控资源使用
        runAsyncRepeating(0L, 20L * 60) { // 每分钟
            val stats = getResourceStats()
            logger.debug("""
                性能监控报告:
                - 绑定资源数: ${stats.totalBound}
                - 活跃任务数: ${taskManager.getActiveTaskCount()}
                - 内存使用: ${stats.memoryUsage}MB
                - 活跃任务数: ${scheduler.getActiveTaskCount()}
            """.trimIndent())
        }

        // 监控事件处理性能
        runAsyncRepeating(0L, 20L * 300) { // 每5分钟
            val eventStats = getEventProcessingStats()
            if (eventStats.averageProcessingTime > 50) {
                logger.warning("事件处理性能警告: 平均处理时间 ${eventStats.averageProcessingTime}ms")
            }
        }
    }
}
```

---

**相关文档：** [📦 BaseModule](base-module.md) | [♻️ 资源管理](terminable.md) | [🔄 生命周期管理](lifecycle.md)
