# Config 模块 API 参考

> 📋 **状态**: 文档规划中，内容正在完善

## 核心 API

### ConfigManager

配置管理器是 Config 模块的核心 API：

```kotlin
interface ConfigManager {
    // 加载配置
    fun <T> load(path: String, type: Class<T>): T
    fun <T> getPluginConfig(): T
    
    // 保存配置
    fun <T> save(config: T, path: String)
    
    // 配置验证
    fun <T> validate(config: T): ValidationResult
    
    // 热重载
    fun reload()
    fun touchWithMerge(path: String)
}
```

### BasePluginConfig

所有插件配置的基类：

```kotlin
abstract class BasePluginConfig {
    // 配置版本
    open val version: String = "1.0.0"
    
    // 配置验证
    open fun validate(): List<String> = emptyList()
    
    // 配置迁移
    open fun migrate(oldVersion: String): BasePluginConfig = this
}
```

### 配置注解

用于配置类的注解：

```kotlin
@ConfigProperty("custom-name")
val customProperty: String = "default"

@ConfigDescription("配置项描述")
val describedProperty: Int = 100

@ConfigValidation(min = 1, max = 100)
val validatedProperty: Int = 50
```

## 工具类 API

### ConfigValidator

配置验证工具：

```kotlin
object ConfigValidator {
    fun validate(config: Any): ValidationResult
    fun validateProperty(value: Any, constraints: List<Constraint>): Boolean
}
```

### ConfigMigrator

配置迁移工具：

```kotlin
object ConfigMigrator {
    fun migrate(oldConfig: Map<String, Any>, targetVersion: String): Map<String, Any>
    fun detectVersion(config: Map<String, Any>): String
}
```

### ConfigFormatter

配置格式化工具：

```kotlin
object ConfigFormatter {
    fun format(config: Any, format: ConfigFormat): String
    fun parse(content: String, format: ConfigFormat): Map<String, Any>
}
```

## 扩展 API

### 自定义序列化器

```kotlin
class CustomSerializer : JsonSerializer<CustomType>() {
    override fun serialize(value: CustomType, gen: JsonGenerator, serializers: SerializerProvider) {
        // 自定义序列化逻辑
    }
}
```

### 配置监听器

```kotlin
interface ConfigChangeListener {
    fun onConfigChanged(path: String, oldValue: Any?, newValue: Any?)
    fun onConfigReloaded(config: Any)
}
```

## 使用示例

### 基本使用

```kotlin
// 定义配置类
data class MyPluginConfig(
    val serverName: String = "默认服务器",
    val features: FeatureConfig = FeatureConfig()
) : BasePluginConfig()

// 在插件中使用
class MyPlugin : BasePlugin() {
    private val config by lazy { configManager.getPluginConfig<MyPluginConfig>() }
    
    override fun onPluginEnable() {
        logger.info("服务器名称: ${config.serverName}")
    }
}
```

### 高级用法

```kotlin
// 自定义配置路径
val customConfig = configManager.load("custom/config.yml", CustomConfig::class.java)

// 配置验证
val result = configManager.validate(config)
if (!result.isValid) {
    logger.warn("配置验证失败: ${result.errors}")
}

// 热重载
configManager.reload()
```

## 相关文档

- [🚀 快速开始](quick-start.md) - 基本使用指南
- [💡 基础概念](concepts.md) - 核心概念说明
- [📄 支持格式](formats.md) - 配置格式详解

---

**📝 注意**: 此文档正在完善中，完整 API 说明请参考源码注释和 [示例代码](examples.md)。
