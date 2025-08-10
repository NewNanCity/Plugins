# Config 模块最佳实践

> 📋 **状态**: 文档规划中，内容正在完善

## 配置设计最佳实践

### 1. 使用数据类和默认值

```kotlin
data class ServerConfig(
    val name: String = "Minecraft Server",
    val port: Int = 25565,
    val maxPlayers: Int = 20,
    val motd: String = "Welcome to our server!"
) : BasePluginConfig()
```

**优点**：
- 类型安全
- 自动生成 equals/hashCode/toString
- 提供合理的默认值

### 2. 模块化配置结构

```kotlin
data class PluginConfig(
    val server: ServerConfig = ServerConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val features: FeatureConfig = FeatureConfig()
) : BasePluginConfig()
```

**优点**：
- 清晰的配置层次
- 便于维护和扩展
- 支持部分配置重载

### 3. 使用枚举类型

```kotlin
enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LoggingConfig(
    val level: LogLevel = LogLevel.INFO,
    val enableFileLogging: Boolean = true
)
```

**优点**：
- 限制有效值范围
- 提供类型安全
- 便于 IDE 自动补全

### 4. 配置验证

```kotlin
data class DatabaseConfig(
    val host: String = "localhost",
    val port: Int = 3306,
    val username: String = "root"
) : BasePluginConfig() {
    
    override fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (port !in 1..65535) {
            errors.add("端口号必须在 1-65535 范围内")
        }
        
        if (username.isBlank()) {
            errors.add("用户名不能为空")
        }
        
        return errors
    }
}
```

### 5. 配置迁移策略

```kotlin
data class MyConfig(
    val newProperty: String = "default",
    override val version: String = "2.0.0"
) : BasePluginConfig() {
    
    override fun migrate(oldVersion: String): BasePluginConfig {
        return when (oldVersion) {
            "1.0.0" -> migrateFrom1_0_0()
            else -> this
        }
    }
    
    private fun migrateFrom1_0_0(): MyConfig {
        // 迁移逻辑
        return this.copy(newProperty = "migrated_value")
    }
}
```

## 性能优化

### 1. 延迟加载配置

```kotlin
class MyPlugin : BasePlugin() {
    // 使用 lazy 延迟加载
    private val config by lazy { configManager.getPluginConfig<MyConfig>() }
    
    // 避免在构造函数中加载配置
    override fun onPluginEnable() {
        // 配置在首次访问时才加载
        logger.info("配置加载完成: ${config.serverName}")
    }
}
```

### 2. 缓存配置值

```kotlin
class FeatureManager(private val plugin: BasePlugin) : BaseModule(plugin) {
    // 缓存经常访问的配置值
    private var enabledFeatures: Set<String> = emptySet()
    
    override fun onReload() {
        val config = plugin.configManager.getPluginConfig<MyConfig>()
        enabledFeatures = config.features.enabled.toSet()
    }
    
    fun isFeatureEnabled(feature: String): Boolean {
        return feature in enabledFeatures
    }
}
```

### 3. 批量配置更新

```kotlin
// 避免频繁的单个配置更新
fun updateMultipleSettings(updates: Map<String, Any>) {
    val config = configManager.getPluginConfig<MyConfig>()
    val updatedConfig = config.copy(
        serverName = updates["serverName"] as? String ?: config.serverName,
        maxPlayers = updates["maxPlayers"] as? Int ?: config.maxPlayers
    )
    configManager.save(updatedConfig)
}
```

## 安全考虑

### 1. 敏感信息处理

```kotlin
data class DatabaseConfig(
    val host: String = "localhost",
    val port: Int = 3306,
    val username: String = "root",
    @JsonIgnore // 不序列化到配置文件
    val password: String = ""
) {
    // 从环境变量或安全存储获取密码
    fun getPassword(): String {
        return System.getenv("DB_PASSWORD") ?: password
    }
}
```

### 2. 配置文件权限

```kotlin
// 确保配置文件有适当的权限
fun saveSecureConfig(config: Any, path: String) {
    configManager.save(config, path)
    
    // 设置文件权限（仅所有者可读写）
    val file = File(path)
    file.setReadable(false, false)
    file.setReadable(true, true)
    file.setWritable(false, false)
    file.setWritable(true, true)
}
```

## 错误处理

### 1. 优雅的配置加载失败处理

```kotlin
override fun onPluginEnable() {
    try {
        config = configManager.getPluginConfig()
        logger.info("配置加载成功")
    } catch (e: ConfigurationException) {
        logger.error("配置加载失败，使用默认配置", e)
        config = MyConfig() // 使用默认配置
    }
}
```

### 2. 配置验证错误处理

```kotlin
fun validateAndLoad(): MyConfig {
    val config = configManager.getPluginConfig<MyConfig>()
    val errors = config.validate()
    
    if (errors.isNotEmpty()) {
        logger.warn("配置验证发现问题:")
        errors.forEach { logger.warn("- $it") }
        logger.warn("将使用默认值继续运行")
    }
    
    return config
}
```

## 相关文档

- [🚀 快速开始](quick-start.md) - 基本使用方法
- [💡 基础概念](concepts.md) - 配置系统概念
- [📋 API 参考](api-reference.md) - 完整 API 文档

---

**📝 注意**: 此文档正在完善中，更多最佳实践请参考 [示例代码](examples.md)。
