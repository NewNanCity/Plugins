# 架构设计

Core 模块基于现代软件架构原则设计，提供了清晰的分层结构、模块化组织和可扩展的插件开发框架。

## 🏗️ 四层架构模式

### 架构概览

```
┌─────────────────────────────────────┐
│           主插件类                   │  ← 协调层：生命周期管理、模块协调
├─────────────────────────────────────┤
│        事务层：Commands/Events       │  ← 对外接口：用户交互、外部集成
├─────────────────────────────────────┤
│       逻辑层：Modules/Services       │  ← 业务逻辑：核心功能实现
├─────────────────────────────────────┤
│    基础层：Utils/Config/Data        │  ← 基础设施：工具类、配置、数据
└─────────────────────────────────────┘
```

### 层次职责

| 层次 | 职责 | 组件示例 | 依赖方向 |
|------|------|----------|----------|
| **主插件类** | 生命周期管理、模块协调 | MyPlugin, BasePlugin | ↓ |
| **事务层** | 用户交互、外部接口 | Commands, Events, API | ↓ |
| **逻辑层** | 业务逻辑、核心功能 | Modules, Services, Managers | ↓ |
| **基础层** | 基础设施、工具支持 | Utils, Config, Data, Cache | - |

### 依赖原则

**核心原则：单向依赖**
- ✅ 上层可以依赖下层
- ❌ 下层不能依赖上层
- ✅ 同层之间可以协作
- ✅ 通过接口和事件解耦

## 📦 模块化设计

### BaseModule 架构

```kotlin
// 主插件类 - 协调层
class MyPlugin : BasePlugin() {
    // 模块声明
    private lateinit var playerModule: PlayerModule
    private lateinit var economyModule: EconomyModule
    private lateinit var commandRegistry: CommandRegistry
    
    override fun onPluginEnable() {
        // 初始化逻辑层模块
        playerModule = PlayerModule("PlayerModule", this)
        economyModule = EconomyModule("EconomyModule", this)
        
        // 初始化事务层
        commandRegistry = CommandRegistry(this)
        
        reloadPlugin()
    }
}

// 逻辑层 - 业务模块
class PlayerModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {
    
    override fun onInit() {
        // 使用基础层工具
        val playerCache = PlayerCache()
        val playerConfig = PlayerConfig()
        bind(playerCache)
        
        // 注册事务层事件
        subscribeEvent<PlayerJoinEvent> { event ->
            handlePlayerJoin(event.player)
        }
    }
    
    private fun handlePlayerJoin(player: Player) {
        // 业务逻辑实现
        val playerData = loadPlayerData(player)
        updatePlayerStatus(player, playerData)
        
        // 通知其他模块（通过插件协调）
        plugin.getEconomyModule().setupPlayerAccount(player)
    }
}

// 事务层 - 命令处理
class CommandRegistry(private val plugin: MyPlugin) : BaseCommandRegistry("CommandRegistry", plugin) {
    
    override fun registerCommands() {
        // 注册用户交互命令
        registerPlayerCommands()
        registerAdminCommands()
    }
    
    private fun registerPlayerCommands() {
        val playerCommand = CommandAPICommand("player")
            .withSubcommands(
                createInfoCommand(),
                createStatsCommand()
            )
        
        registerAndTrack(playerCommand, "player")
    }
    
    private fun createInfoCommand(): CommandAPICommand {
        return CommandAPICommand("info")
            .executesPlayer(PlayerCommandExecutor { player, _ ->
                // 调用逻辑层获取数据
                val playerData = plugin.getPlayerModule().getPlayerData(player)
                
                // 展示给用户
                displayPlayerInfo(player, playerData)
            })
    }
}
```

### 模块间通信

```kotlin
// 方式1：通过主插件协调
class PlayerModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {
    
    fun notifyPlayerAction(player: Player, action: String) {
        // 通过插件访问其他模块
        plugin.getEconomyModule().recordTransaction(player, action)
        plugin.getStatisticsModule().updatePlayerStats(player, action)
    }
}

// 方式2：通过事件解耦
class PlayerModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {
    
    fun notifyPlayerAction(player: Player, action: String) {
        // 发布自定义事件
        val event = PlayerActionEvent(player, action)
        server.pluginManager.callEvent(event)
    }
}

class EconomyModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {
    
    override fun onInit() {
        // 监听其他模块的事件
        subscribeEvent<PlayerActionEvent> { event ->
            recordTransaction(event.player, event.action)
        }
    }
}
```

## 🔧 设计模式应用

### 1. 工厂模式

```kotlin
// 基础层 - 工厂类
object ManagerFactory {
    
    fun createDataManager(plugin: BasePlugin): DataManager {
        val config = plugin.getPluginConfig()
        
        return when (config.database.type) {
            "mysql" -> MySQLDataManager(plugin, config.database)
            "sqlite" -> SQLiteDataManager(plugin, config.database)
            "file" -> FileDataManager(plugin, config.database)
            else -> throw IllegalArgumentException("不支持的数据库类型: ${config.database.type}")
        }
    }
    
    fun createCacheManager(plugin: BasePlugin): CacheManager {
        val config = plugin.getPluginConfig()
        
        return when (config.cache.type) {
            "lru" -> LRUCacheManager(config.cache.size)
            "lfu" -> LFUCacheManager(config.cache.size)
            "unlimited" -> UnlimitedCacheManager()
            else -> LRUCacheManager(1000) // 默认
        }
    }
}

// 逻辑层 - 使用工厂
class DataModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {
    
    override fun onInit() {
        // 使用工厂创建管理器
        val dataManager = ManagerFactory.createDataManager(plugin)
        val cacheManager = ManagerFactory.createCacheManager(plugin)
        
        bind(dataManager)
        bind(cacheManager)
    }
}
```

### 2. 策略模式

```kotlin
// 基础层 - 策略接口
interface ValidationStrategy {
    fun validate(input: String): ValidationResult
}

class EmailValidationStrategy : ValidationStrategy {
    override fun validate(input: String): ValidationResult {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        return if (input.matches(emailRegex.toRegex())) {
            ValidationResult.success()
        } else {
            ValidationResult.error("无效的邮箱格式")
        }
    }
}

class UsernameValidationStrategy : ValidationStrategy {
    override fun validate(input: String): ValidationResult {
        return when {
            input.length < 3 -> ValidationResult.error("用户名至少3个字符")
            input.length > 16 -> ValidationResult.error("用户名最多16个字符")
            !input.matches("^[a-zA-Z0-9_]+$".toRegex()) -> ValidationResult.error("用户名只能包含字母、数字和下划线")
            else -> ValidationResult.success()
        }
    }
}

// 逻辑层 - 使用策略
class ValidationModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {
    
    private val strategies = mapOf(
        "email" to EmailValidationStrategy(),
        "username" to UsernameValidationStrategy()
    )
    
    fun validate(type: String, input: String): ValidationResult {
        val strategy = strategies[type] ?: throw IllegalArgumentException("未知的验证类型: $type")
        return strategy.validate(input)
    }
}
```

### 3. 观察者模式

```kotlin
// 基础层 - 观察者接口
interface PlayerEventObserver {
    fun onPlayerJoin(player: Player)
    fun onPlayerQuit(player: Player)
}

// 逻辑层 - 被观察者
class PlayerEventManager(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {
    
    private val observers = mutableListOf<PlayerEventObserver>()
    
    override fun onInit() {
        subscribeEvent<PlayerJoinEvent> { event ->
            notifyPlayerJoin(event.player)
        }
        
        subscribeEvent<PlayerQuitEvent> { event ->
            notifyPlayerQuit(event.player)
        }
    }
    
    fun addObserver(observer: PlayerEventObserver) {
        observers.add(observer)
    }
    
    fun removeObserver(observer: PlayerEventObserver) {
        observers.remove(observer)
    }
    
    private fun notifyPlayerJoin(player: Player) {
        observers.forEach { it.onPlayerJoin(player) }
    }
    
    private fun notifyPlayerQuit(player: Player) {
        observers.forEach { it.onPlayerQuit(player) }
    }
}

// 逻辑层 - 观察者实现
class StatisticsModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin), PlayerEventObserver {
    
    override fun onInit() {
        // 注册为观察者
        plugin.getPlayerEventManager().addObserver(this)
    }
    
    override fun onPlayerJoin(player: Player) {
        updateJoinStatistics(player)
    }
    
    override fun onPlayerQuit(player: Player) {
        updateQuitStatistics(player)
    }
}
```

## 🔄 依赖注入

### 构造函数注入

```kotlin
// 基础层 - 服务接口
interface DatabaseService {
    fun save(data: Any)
    fun load(id: String): Any?
}

interface CacheService {
    fun put(key: String, value: Any)
    fun get(key: String): Any?
}

// 基础层 - 服务实现
class MySQLDatabaseService(private val config: DatabaseConfig) : DatabaseService {
    override fun save(data: Any) { /* 实现 */ }
    override fun load(id: String): Any? { /* 实现 */ }
}

class LRUCacheService(private val maxSize: Int) : CacheService {
    override fun put(key: String, value: Any) { /* 实现 */ }
    override fun get(key: String): Any? { /* 实现 */ }
}

// 逻辑层 - 依赖注入
class PlayerDataModule(
    moduleName: String,
    plugin: MyPlugin,
    private val databaseService: DatabaseService,
    private val cacheService: CacheService
) : BaseModule(moduleName, plugin) {
    
    fun savePlayerData(player: Player, data: PlayerData) {
        // 先保存到缓存
        cacheService.put("player:${player.uniqueId}", data)
        
        // 异步保存到数据库
        runAsync {
            databaseService.save(data)
        }
    }
    
    fun loadPlayerData(player: Player): PlayerData? {
        val cacheKey = "player:${player.uniqueId}"
        
        // 先从缓存获取
        return cacheService.get(cacheKey) as? PlayerData
            ?: databaseService.load(cacheKey) as? PlayerData
    }
}

// 主插件类 - 依赖组装
class MyPlugin : BasePlugin() {
    
    override fun onPluginEnable() {
        val config = getPluginConfig()
        
        // 创建服务
        val databaseService = MySQLDatabaseService(config.database)
        val cacheService = LRUCacheService(config.cache.maxSize)
        
        // 注入依赖
        val playerDataModule = PlayerDataModule(
            "PlayerDataModule",
            this,
            databaseService,
            cacheService
        )
        
        reloadPlugin()
    }
}
```

## 🎯 架构最佳实践

### 1. 单一职责原则

```kotlin
// ✅ 好的设计：每个模块职责单一
class PlayerModule : BaseModule {
    // 只负责玩家相关功能
}

class EconomyModule : BaseModule {
    // 只负责经济相关功能
}

class DatabaseModule : BaseModule {
    // 只负责数据库操作
}

// ❌ 不好的设计：职责混乱
class MegaModule : BaseModule {
    // 包含玩家、经济、数据库等多种功能
}
```

### 2. 开闭原则

```kotlin
// 基础层 - 可扩展的接口
interface NotificationService {
    fun sendNotification(player: Player, message: String)
}

// 基础层 - 具体实现
class ChatNotificationService : NotificationService {
    override fun sendNotification(player: Player, message: String) {
        player.sendMessage(message)
    }
}

class TitleNotificationService : NotificationService {
    override fun sendNotification(player: Player, message: String) {
        player.sendTitle(message, "", 10, 70, 20)
    }
}

// 逻辑层 - 使用接口
class NotificationModule(
    moduleName: String,
    plugin: MyPlugin,
    private val notificationService: NotificationService
) : BaseModule(moduleName, plugin) {
    
    fun notifyPlayer(player: Player, message: String) {
        notificationService.sendNotification(player, message)
    }
}

// 扩展新功能时无需修改现有代码
class ActionBarNotificationService : NotificationService {
    override fun sendNotification(player: Player, message: String) {
        player.sendActionBar(message)
    }
}
```

### 3. 依赖倒置原则

```kotlin
// ✅ 好的设计：依赖抽象
class PlayerService(
    private val dataRepository: PlayerDataRepository, // 接口
    private val cacheService: CacheService            // 接口
) {
    fun savePlayer(player: PlayerData) {
        cacheService.put(player.id, player)
        dataRepository.save(player)
    }
}

// ❌ 不好的设计：依赖具体实现
class PlayerService(
    private val mysqlRepository: MySQLPlayerRepository, // 具体类
    private val lruCache: LRUCache                      // 具体类
) {
    // 难以测试和扩展
}
```

### 4. 接口隔离原则

```kotlin
// ✅ 好的设计：细粒度接口
interface PlayerReader {
    fun getPlayer(id: UUID): Player?
}

interface PlayerWriter {
    fun savePlayer(player: Player)
}

interface PlayerDeleter {
    fun deletePlayer(id: UUID)
}

// 客户端只依赖需要的接口
class PlayerQueryService(private val reader: PlayerReader) {
    // 只需要读取功能
}

class PlayerSaveService(private val writer: PlayerWriter) {
    // 只需要写入功能
}

// ❌ 不好的设计：臃肿接口
interface PlayerRepository {
    fun getPlayer(id: UUID): Player?
    fun savePlayer(player: Player)
    fun deletePlayer(id: UUID)
    fun getAllPlayers(): List<Player>
    fun searchPlayers(query: String): List<Player>
    fun updatePlayerStats(id: UUID, stats: Stats)
    // ... 更多方法
}
```

---

**相关文档：** [📦 BaseModule](base-module.md) | [🔧 BasePlugin](base-plugin.md) | [💡 最佳实践](best-practices.md)
