# 配置管理

Core 模块提供了强大的配置管理系统，支持多种格式、自动合并、类型安全解析和配置热重载。

## 🎯 核心特性

### 多格式支持
- **JSON** - 现代化的数据交换格式
- **YAML** - 人类友好的配置格式
- **TOML** - 简洁明了的配置语言
- **HOCON** - 人类优化的配置对象表示法
- **XML** - 结构化标记语言
- **Properties** - 传统的键值对格式
- **CSV** - 表格数据格式

### 高级功能
- **自动合并** - 检测并补全缺失的配置项
- **类型安全** - 基于 Jackson 的强类型解析
- **配置缓存** - 提升性能的智能缓存机制
- **热重载** - 运行时配置更新支持

## 🚀 基础用法

### 配置类设计

```kotlin
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MyPluginConfig(
    @JsonProperty("debug")
    val debug: Boolean = false,
    
    @JsonProperty("database")
    val database: DatabaseConfig = DatabaseConfig(),
    
    @JsonProperty("features")
    val features: FeatureConfig = FeatureConfig(),
    
    @JsonProperty("message-settings")
    val messageSettings: MessageSettings = MessageSettings()
) {
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        logging.logLevel = if (debug) LogLevel.DEBUG else LogLevel.INFO
        logging.fileLoggingEnabled = true
        logging.logFilePrefix = "MyPlugin_"
        
        message.playerPrefix = messageSettings.playerPrefix
        message.consolePrefix = messageSettings.consolePrefix
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DatabaseConfig(
    @JsonProperty("enabled")
    val enabled: Boolean = false,
    
    @JsonProperty("host")
    val host: String = "localhost",
    
    @JsonProperty("port")
    val port: Int = 3306,
    
    @JsonProperty("database")
    val database: String = "minecraft",
    
    @JsonProperty("username")
    val username: String = "root",
    
    @JsonProperty("password")
    val password: String = ""
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FeatureConfig(
    @JsonProperty("auto-save")
    val autoSave: Boolean = true,
    
    @JsonProperty("save-interval")
    val saveInterval: Int = 300, // 秒
    
    @JsonProperty("max-players")
    val maxPlayers: Int = 100
)
```

### 插件配置方法

```kotlin
class MyPlugin : BasePlugin() {
    
    fun getPluginConfig(): MyPluginConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<MyPluginConfig>("config.yml")
    }
    
    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()
    
    override fun reloadPlugin() {
        try {
            // 1. 清理配置缓存（必需）
            configManager.clearCache()
            
            // 2. 重新设置语言管理器（必需）
            setupLanguageManager(
                languageFiles = mapOf(
                    Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                    Locale.US to "lang/en_US.yml"
                ),
                majorLanguage = Locale.SIMPLIFIED_CHINESE,
                defaultLanguage = Locale.US
            )
            
            // 3. 应用新配置
            val config = getPluginConfig()
            applyConfiguration(config)
            
            // 4. 重载所有子模块
            super.reloadPlugin()
            
        } catch (e: Exception) {
            logger.error("配置重载失败", e)
            throw e
        }
    }
    
    private fun applyConfiguration(config: MyPluginConfig) {
        // 应用配置到插件
        if (config.features.autoSave) {
            setupAutoSave(config.features.saveInterval)
        }
        
        if (config.database.enabled) {
            setupDatabase(config.database)
        }
    }
}
```

## 📁 多格式配置示例

### YAML 格式 (config.yml)
```yaml
debug: false

database:
  enabled: true
  host: "localhost"
  port: 3306
  database: "minecraft"
  username: "root"
  password: "password123"

features:
  auto-save: true
  save-interval: 300
  max-players: 100

message-settings:
  player-prefix: "&7[&6MyPlugin&7] &f"
  console-prefix: "[MyPlugin] "
```

### JSON 格式 (config.json)
```json
{
  "debug": false,
  "database": {
    "enabled": true,
    "host": "localhost",
    "port": 3306,
    "database": "minecraft",
    "username": "root",
    "password": "password123"
  },
  "features": {
    "auto-save": true,
    "save-interval": 300,
    "max-players": 100
  },
  "message-settings": {
    "player-prefix": "&7[&6MyPlugin&7] &f",
    "console-prefix": "[MyPlugin] "
  }
}
```

### TOML 格式 (config.toml)
```toml
debug = false

[database]
enabled = true
host = "localhost"
port = 3306
database = "minecraft"
username = "root"
password = "password123"

[features]
auto-save = true
save-interval = 300
max-players = 100

[message-settings]
player-prefix = "&7[&6MyPlugin&7] &f"
console-prefix = "[MyPlugin] "
```

## 🔧 高级配置功能

### 自动配置合并

```kotlin
class MyPlugin : BasePlugin() {
    
    fun getPluginConfig(): MyPluginConfig {
        // touchWithMerge 会检测并补全缺失的配置项
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<MyPluginConfig>("config.yml")
    }
}
```

**工作原理：**
1. 检查现有配置文件
2. 与默认配置对比
3. 补全缺失的配置项
4. 保留现有配置值
5. 可选择创建备份文件

### 配置缓存管理

```kotlin
class MyPlugin : BasePlugin() {
    
    override fun reloadPlugin() {
        // 清理缓存以强制重新加载
        configManager.clearCache()
        
        // 重新加载配置
        val config = getPluginConfig()
        
        // 配置会被自动缓存
    }
    
    private fun getConfigWithoutCache(): MyPluginConfig {
        // 绕过缓存直接加载
        configManager.clearCache()
        return configManager.parse<MyPluginConfig>("config.yml")
    }
}
```

### 条件配置加载

```kotlin
class MyPlugin : BasePlugin() {
    
    private fun loadEnvironmentConfig() {
        val environment = System.getProperty("environment", "production")
        
        val configFile = when (environment) {
            "development" -> "config-dev.yml"
            "testing" -> "config-test.yml"
            else -> "config.yml"
        }
        
        configManager.touchWithMerge(configFile, createBackup = true)
        val config = configManager.parse<MyPluginConfig>(configFile)
        
        logger.info("已加载 $environment 环境配置")
    }
}
```

## 🔄 在 BaseModule 中使用配置

### 模块级配置

```kotlin
class PlayerModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {
    
    private var moduleConfig: PlayerModuleConfig? = null
    
    override fun onInit() {
        loadModuleConfig()
        setupWithConfig()
    }
    
    override fun onReload() {
        logger.info("正在重载玩家模块配置...")
        loadModuleConfig()
        setupWithConfig()
    }
    
    private fun loadModuleConfig() {
        // 加载模块特定配置
        configManager.touchWithMerge("modules/player.yml", createBackup = true)
        moduleConfig = configManager.parse<PlayerModuleConfig>("modules/player.yml")
    }
    
    private fun setupWithConfig() {
        moduleConfig?.let { config ->
            if (config.welcomeMessage.enabled) {
                setupWelcomeMessage(config.welcomeMessage)
            }
            
            if (config.playerTracking.enabled) {
                setupPlayerTracking(config.playerTracking)
            }
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PlayerModuleConfig(
    @JsonProperty("welcome-message")
    val welcomeMessage: WelcomeMessageConfig = WelcomeMessageConfig(),
    
    @JsonProperty("player-tracking")
    val playerTracking: PlayerTrackingConfig = PlayerTrackingConfig()
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class WelcomeMessageConfig(
    @JsonProperty("enabled")
    val enabled: Boolean = true,
    
    @JsonProperty("message")
    val message: String = "<%welcome.message%>",
    
    @JsonProperty("delay")
    val delay: Int = 20 // ticks
)
```

### 访问主插件配置

```kotlin
class EconomyModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {
    
    override fun onInit() {
        // 访问主插件配置
        val pluginConfig = plugin.getPluginConfig()
        
        if (pluginConfig.features.autoSave) {
            setupAutoSave(pluginConfig.features.saveInterval)
        }
        
        // 访问数据库配置
        if (pluginConfig.database.enabled) {
            setupDatabaseConnection(pluginConfig.database)
        }
    }
    
    override fun onReload() {
        // 重载时重新读取配置
        val pluginConfig = plugin.getPluginConfig()
        applyEconomyConfig(pluginConfig)
    }
}
```

## 🛡️ 配置验证和错误处理

### 配置验证

```kotlin
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ValidatedConfig(
    @JsonProperty("port")
    val port: Int = 8080,
    
    @JsonProperty("timeout")
    val timeout: Int = 30,
    
    @JsonProperty("max-connections")
    val maxConnections: Int = 100
) {
    init {
        // 配置验证
        require(port in 1..65535) { "端口必须在 1-65535 范围内" }
        require(timeout > 0) { "超时时间必须大于 0" }
        require(maxConnections > 0) { "最大连接数必须大于 0" }
    }
    
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (port !in 1..65535) {
            errors.add("端口 $port 不在有效范围内 (1-65535)")
        }
        
        if (timeout <= 0) {
            errors.add("超时时间 $timeout 必须大于 0")
        }
        
        if (maxConnections <= 0) {
            errors.add("最大连接数 $maxConnections 必须大于 0")
        }
        
        return errors
    }
}
```

### 错误处理

```kotlin
class MyPlugin : BasePlugin() {
    
    fun getPluginConfig(): MyPluginConfig {
        return try {
            configManager.touchWithMerge("config.yml", createBackup = true)
            val config = configManager.parse<MyPluginConfig>("config.yml")
            
            // 验证配置
            validateConfiguration(config)
            
            config
        } catch (e: Exception) {
            logger.error("配置加载失败，使用默认配置", e)
            createDefaultConfig()
        }
    }
    
    private fun validateConfiguration(config: MyPluginConfig) {
        // 验证数据库配置
        if (config.database.enabled) {
            require(config.database.host.isNotBlank()) { "数据库主机不能为空" }
            require(config.database.port in 1..65535) { "数据库端口无效" }
        }
        
        // 验证功能配置
        require(config.features.saveInterval > 0) { "保存间隔必须大于 0" }
        require(config.features.maxPlayers > 0) { "最大玩家数必须大于 0" }
    }
    
    private fun createDefaultConfig(): MyPluginConfig {
        logger.warning("使用默认配置")
        return MyPluginConfig()
    }
}
```

## 🎯 最佳实践

### 1. 配置类设计

```kotlin
// ✅ 推荐：使用数据类和默认值
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MyConfig(
    @JsonProperty("enabled")
    val enabled: Boolean = true,
    
    @JsonProperty("settings")
    val settings: SettingsConfig = SettingsConfig()
)

// ✅ 推荐：嵌套配置结构
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SettingsConfig(
    @JsonProperty("auto-save")
    val autoSave: Boolean = true,
    
    @JsonProperty("interval")
    val interval: Int = 300
)
```

### 2. 配置方法实现

```kotlin
// ✅ 标准配置方法模式
fun getPluginConfig(): MyPluginConfig {
    configManager.touchWithMerge("config.yml", createBackup = true)
    return configManager.parse<MyPluginConfig>("config.yml")
}

override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()
```

### 3. 重载机制

```kotlin
override fun reloadPlugin() {
    try {
        // 1. 清理缓存
        configManager.clearCache()
        
        // 2. 设置语言管理器
        setupLanguageManager()
        
        // 3. 应用新配置
        applyConfiguration(getPluginConfig())
        
        // 4. 重载子模块
        super.reloadPlugin()
        
    } catch (e: Exception) {
        logger.error("配置重载失败", e)
        throw e
    }
}
```

### 4. 配置缓存

```kotlin
// ✅ 在模块中缓存配置
class MyModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {
    
    private var cachedConfig: ModuleConfig? = null
    
    override fun onReload() {
        // 重载时清理缓存
        cachedConfig = null
        loadConfig()
    }
    
    private fun getConfig(): ModuleConfig {
        return cachedConfig ?: loadConfig().also { cachedConfig = it }
    }
    
    private fun loadConfig(): ModuleConfig {
        return configManager.parse<ModuleConfig>("module.yml")
    }
}
```

---

**相关文档：** [💬 消息系统](messaging.md) | [🌐 国际化](i18n.md) | [💡 最佳实践](best-practices.md)
