# 示例代码集合

本文档提供了 Core 模块的完整示例代码，展示了从简单到复杂的各种使用场景。

## 🚀 基础插件示例

### 最小插件实现

```kotlin
class SimplePlugin : BasePlugin() {

    override fun onPluginEnable() {
        logger.info("SimplePlugin enabling...")
        reloadPlugin()
        logger.info("<%plugin.enabled%>")
    }

    override fun reloadPlugin() {
        configManager.clearCache()

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

    fun getPluginConfig(): SimpleConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<SimpleConfig>("config.yml")
    }

    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SimpleConfig(
    @JsonProperty("debug")
    val debug: Boolean = false,

    @JsonProperty("message-prefix")
    val messagePrefix: String = "&7[&6Simple&7] &f"
) {
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        logging.logLevel = if (debug) LogLevel.DEBUG else LogLevel.INFO
        message.playerPrefix = messagePrefix
    }
}
```

## 📦 模块化插件示例

### 完整的模块化插件

```kotlin
class ModularPlugin : BasePlugin() {

    // 模块声明
    private lateinit var playerModule: PlayerModule
    private lateinit var economyModule: EconomyModule
    private lateinit var commandRegistry: CommandRegistry

    override fun onPluginEnable() {
        logger.info("ModularPlugin enabling...")

        // 初始化模块
        playerModule = PlayerModule("PlayerModule", this)
        economyModule = EconomyModule("EconomyModule", this)
        commandRegistry = CommandRegistry(this)

        reloadPlugin()
        logger.info("<%plugin.enabled%>")
    }

    override fun reloadPlugin() {
        try {
            configManager.clearCache()

            setupLanguageManager(
                languageFiles = mapOf(
                    Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                    Locale.US to "lang/en_US.yml"
                ),
                majorLanguage = Locale.SIMPLIFIED_CHINESE,
                defaultLanguage = Locale.US
            )

            super.reloadPlugin()
        } catch (e: Exception) {
            logger.error("配置重载失败", e)
            throw e
        }
    }

    // 模块访问方法
    fun getPlayerModule(): PlayerModule = playerModule
    fun getEconomyModule(): EconomyModule = economyModule

    // 配置方法
    fun getPluginConfig(): ModularConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<ModularConfig>("config.yml")
    }

    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()
}

// 玩家模块
class PlayerModule(moduleName: String, val plugin: ModularPlugin) : BaseModule(moduleName, plugin) {

    private val playerData = ConcurrentHashMap<UUID, PlayerData>()

    override fun onInit() {
        logger.info("PlayerModule 正在初始化...")

        // 玩家加入事件
        subscribeEvent<PlayerJoinEvent> { event ->
            handlePlayerJoin(event.player)
        }

        // 玩家退出事件
        subscribeEvent<PlayerQuitEvent> { event ->
            handlePlayerQuit(event.player)
        }

        // 定期保存数据
        runAsyncRepeating(0L, 20L * 300) { // 每5分钟
            saveAllPlayerData()
        }

        logger.info("PlayerModule 初始化完成")
    }

    override fun onReload() {
        logger.info("PlayerModule 正在重载...")
        // 重载玩家模块配置
        val config = plugin.getPluginConfig().playerModule
        applyPlayerConfig(config)
        logger.info("PlayerModule 重载完成")
    }

    private fun handlePlayerJoin(player: Player) {
        // 加载玩家数据
        val data = loadPlayerData(player.uniqueId)
        playerData[player.uniqueId] = data

        // 发送欢迎消息
        messager.printf(player, "<%welcome.message%>", player.name)

        // 通知经济模块
        plugin.getEconomyModule().setupPlayerAccount(player)
    }

    private fun handlePlayerQuit(player: Player) {
        // 保存玩家数据
        playerData[player.uniqueId]?.let { data ->
            savePlayerData(data)
        }
        playerData.remove(player.uniqueId)

        logger.info("<%player.quit%>", player.name)
    }

    fun getPlayerData(player: Player): PlayerData? {
        return playerData[player.uniqueId]
    }

    private fun loadPlayerData(playerId: UUID): PlayerData {
        // 从数据库或文件加载
        return PlayerData(playerId, 1, 0, System.currentTimeMillis())
    }

    private fun savePlayerData(data: PlayerData) {
        // 保存到数据库或文件
    }

    private fun saveAllPlayerData() {
        playerData.values.forEach { data ->
            savePlayerData(data)
        }
    }
}

// 经济模块
class EconomyModule(moduleName: String, val plugin: ModularPlugin) : BaseModule(moduleName, plugin) {

    private val balances = ConcurrentHashMap<UUID, Double>()

    override fun onInit() {
        logger.info("EconomyModule 正在初始化...")

        // 初始化经济系统
        initializeEconomy()

        logger.info("EconomyModule 初始化完成")
    }

    override fun onReload() {
        logger.info("EconomyModule 正在重载...")
        val config = plugin.getPluginConfig().economyModule
        applyEconomyConfig(config)
        logger.info("EconomyModule 重载完成")
    }

    fun setupPlayerAccount(player: Player) {
        if (!balances.containsKey(player.uniqueId)) {
            balances[player.uniqueId] = 1000.0 // 初始余额
            messager.printf(player, "<%economy.account_created%>", 1000.0)
        }
    }

    fun getBalance(player: Player): Double {
        return balances[player.uniqueId] ?: 0.0
    }

    fun transfer(from: Player, to: Player, amount: Double, reason: String): TransferResult {
        val fromBalance = getBalance(from)

        if (fromBalance < amount) {
            return TransferResult.failure("余额不足")
        }

        balances[from.uniqueId] = fromBalance - amount
        balances[to.uniqueId] = getBalance(to) + amount

        return TransferResult.success()
    }

    private fun initializeEconomy() {
        // 初始化经济系统
    }
}

// 命令注册器
class CommandRegistry(private val plugin: ModularPlugin) : BaseCommandRegistry("CommandRegistry", plugin) {

    override fun registerCommands() {
        val mainCommand = CommandAPICommand("modular")
            .withAliases("mod")
            .withSubcommands(
                createPlayerCommands(),
                createEconomyCommands(),
                createAdminCommands()
            )

        registerAndTrack(mainCommand, "modular")
    }

    private fun createPlayerCommands(): CommandAPICommand {
        return CommandAPICommand("player")
            .withSubcommands(
                CommandAPICommand("info")
                    .executesPlayer(PlayerCommandExecutor { player, _ ->
                        val data = plugin.getPlayerModule().getPlayerData(player)
                        if (data != null) {
                            plugin.messager.printf(player, "<%player.info%>", data.level, data.experience)
                        }
                    })
            )
    }

    private fun createEconomyCommands(): CommandAPICommand {
        return CommandAPICommand("economy")
            .withSubcommands(
                CommandAPICommand("balance")
                    .executesPlayer(PlayerCommandExecutor { player, _ ->
                        val balance = plugin.getEconomyModule().getBalance(player)
                        plugin.messager.printf(player, "<%economy.balance%>", balance)
                    }),

                CommandAPICommand("transfer")
                    .withArguments(
                        PlayerArgument("target"),
                        DoubleArgument("amount", 0.01)
                    )
                    .executesPlayer(PlayerCommandExecutor { player, args ->
                        val target = args["target"] as Player
                        val amount = args["amount"] as Double

                        val result = plugin.getEconomyModule().transfer(player, target, amount, "转账")
                        if (result.isSuccess) {
                            plugin.messager.printf(player, "<%economy.transfer.success%>", target.name, amount)
                        } else {
                            plugin.messager.printf(player, "<%economy.transfer.failed%>", result.errorMessage)
                        }
                    })
            )
    }

    private fun createAdminCommands(): CommandAPICommand {
        return CommandAPICommand("admin")
            .withPermission("modular.admin")
            .withSubcommands(
                CommandAPICommand("reload")
                    .executes(CommandExecutor { sender, _ ->
                        try {
                            plugin.reloadPlugin()
                            plugin.messager.printf(sender, "<%admin.reload.success%>")
                        } catch (e: Exception) {
                            plugin.messager.printf(sender, "<%admin.reload.failed%>", e.message)
                        }
                    })
            )
    }
}

// 数据类
data class PlayerData(
    val playerId: UUID,
    var level: Int,
    var experience: Long,
    var lastLogin: Long
)

data class TransferResult(
    val isSuccess: Boolean,
    val errorMessage: String?
) {
    companion object {
        fun success() = TransferResult(true, null)
        fun failure(message: String) = TransferResult(false, message)
    }
}

// 配置类
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ModularConfig(
    @JsonProperty("debug")
    val debug: Boolean = false,

    @JsonProperty("player-module")
    val playerModule: PlayerModuleConfig = PlayerModuleConfig(),

    @JsonProperty("economy-module")
    val economyModule: EconomyModuleConfig = EconomyModuleConfig()
) {
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        logging.logLevel = if (debug) LogLevel.DEBUG else LogLevel.INFO
        message.playerPrefix = "&7[&6Modular&7] &f"
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PlayerModuleConfig(
    @JsonProperty("auto-save-interval")
    val autoSaveInterval: Int = 300,

    @JsonProperty("welcome-message-enabled")
    val welcomeMessageEnabled: Boolean = true
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EconomyModuleConfig(
    @JsonProperty("starting-balance")
    val startingBalance: Double = 1000.0,

    @JsonProperty("max-transfer-amount")
    val maxTransferAmount: Double = 100000.0
)
```

## ⚡ 异步编程示例

### 任务调度示例

```kotlin
class AsyncExamplePlugin : BasePlugin() {

    override fun onPluginEnable() {
        // 基础任务调度
        demonstrateBasicTasks()

        // 链式任务调用
        demonstrateChainedTasks()

        // 依赖任务管理
        demonstrateDependentTasks()
    }

    private fun demonstrateBasicTasks() {
        // 同步任务
        runSync {
            logger.info("同步任务执行")
        }

        // 异步任务
        runAsync {
            val data = loadDataFromDatabase()
            logger.info("异步任务完成，数据大小: ${data.size}")
        }

        // 延迟任务
        runSyncLater(20L) { // 1秒后
            server.broadcastMessage("延迟消息")
        }

        // 重复任务
        runSyncRepeating(0L, 20L) { // 每秒
            updateServerStats()
        }
    }

    private fun demonstrateChainedTasks() {
        runAsync {
            loadConfigurationData()
        }.thenApply { config ->
            validateConfiguration(config)
        }.thenRunSync { validatedConfig ->
            applyConfiguration(validatedConfig)
        }.handle { result, exception ->
            if (exception != null) {
                logger.error("配置处理失败", exception)
                getDefaultConfiguration()
            } else {
                result
            }
        }
    }

    private fun demonstrateDependentTasks() {
        val configTask = runAsync { loadConfiguration() }
        val dbTask = runAsync { connectToDatabase() }

        val initTask = runAsync(dependencies = listOf(configTask, dbTask)) { handler ->
            val config = configTask.getNow(null)!!
            val database = dbTask.getNow(null)!!
            initializeSystem(config, database)
        }

        initTask.thenRunSync { result ->
            logger.info("系统初始化完成")
        }
    }
}
```

## 🎯 实用工具示例

### 自定义资源管理

```kotlin
class CustomResourceExample : BasePlugin() {

    override fun onPluginEnable() {
        // 创建自定义资源
        val databaseConnection = DatabaseConnection()
        val cacheManager = CacheManager()
        val networkClient = NetworkClient()

        // 绑定到插件生命周期
        bind(databaseConnection)
        bind(cacheManager)
        bind(networkClient)

        // 插件禁用时自动清理
    }
}

class DatabaseConnection : Terminable {
    private var connection: Connection? = null
    private var isShutdown = false

    init {
        connection = DriverManager.getConnection("jdbc:sqlite:data.db")
    }

    fun executeQuery(sql: String): ResultSet? {
        if (isShutdown) return null
        return connection?.createStatement()?.executeQuery(sql)
    }

    override fun close() {
        if (isShutdown) return
        isShutdown = true

        try {
            connection?.close()
            connection = null
        } catch (e: SQLException) {
            // 记录错误但不抛出异常
        }
    }
}

class CacheManager : Terminable {
    private val cache = ConcurrentHashMap<String, Any>()
    private var isShutdown = false

    fun put(key: String, value: Any) {
        if (!isShutdown) {
            cache[key] = value
        }
    }

    fun get(key: String): Any? {
        return if (isShutdown) null else cache[key]
    }

    override fun close() {
        isShutdown = true
        cache.clear()
    }
}
```

---

**相关文档：** [🚀 快速开始](quick-start.md) | [📦 BaseModule](base-module.md) | [💡 最佳实践](best-practices.md)
