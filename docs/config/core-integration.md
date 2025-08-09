# CorePluginConfig集成指南

本文档详细介绍如何在插件中集成CorePluginConfig，推荐使用CorePluginConfig.build DSL方式。

## 📋 概述

CorePluginConfig是所有插件必须实现的核心配置接口，提供日志和消息管理的标准化配置。推荐使用CorePluginConfig.build DSL构建核心配置，这是最灵活和清晰的方式。

## 🎯 推荐方式：CorePluginConfig.build DSL

**最佳实践**：配置类提供getCoreConfig()方法，使用CorePluginConfig.build DSL构建核心配置。

### 完整示例

```kotlin
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MyPluginConfig(
    /**
     * 调试模式
     */
    @JsonProperty("debug")
    val debug: Boolean = false,

    /**
     * 是否记录到文件
     */
    @JsonProperty("log-to-file")
    val logToFile: Boolean = false,

    /**
     * 玩家消息前缀
     */
    @JsonProperty("player-prefix")
    val playerPrefix: String = "&7[&6MyPlugin&7] &f",

    /**
     * 控制台消息前缀
     */
    @JsonProperty("console-prefix")
    val consolePrefix: String = "[MyPlugin]",

    /**
     * 插件特定设置
     */
    @JsonProperty("plugin-settings")
    val pluginSettings: PluginSettings = PluginSettings()
) {
    /**
     * 构建CorePluginConfig
     */
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        // 日志配置
        logging.logLevel = if (debug) LogLevel.DEBUG else LogLevel.INFO
        logging.fileLoggingEnabled = logToFile
        logging.logFilePrefix = "MyPlugin_"

        // 消息配置
        message.playerPrefix = playerPrefix
        message.consolePrefix = consolePrefix
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PluginSettings(
    @JsonProperty("max-connections")
    val maxConnections: Int = 10,

    @JsonProperty("timeout")
    val timeout: Long = 5000
)

class MyPlugin : BasePlugin() {
    fun getPluginConfig(): MyPluginConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<MyPluginConfig>("config.yml")
    }

    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()
}
```

### 配置文件示例

```yaml
# 基础配置
debug: false
log-to-file: true
player-prefix: "&7[&6MyPlugin&7] &f"
console-prefix: "[MyPlugin]"

# 插件特定配置
plugin-settings:
  max-connections: 10
  timeout: 5000
```

### 优势

1. **灵活性**：可以根据插件配置动态构建核心配置
2. **清晰性**：配置映射逻辑集中在getCoreConfig()方法中
3. **可维护性**：不需要继承复杂的配置类
4. **类型安全**：使用DSL构建器确保类型安全
5. **自定义性**：完全控制配置结构和命名

## 🔧 其他配置方式

如果需要使用config模块的JacksonCorePluginConfig，也可以选择以下方式：

### 1. 继承方式（适用于简单插件）

```kotlin
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SimplePluginConfig(
    @JsonProperty("my-setting")
    val mySetting: String = "default"
) : JacksonCorePluginConfig()

class SimplePlugin : BasePlugin() {
    override fun getCoreConfig(): CorePluginConfig = getPluginConfig()
}
```

### 2. 组合方式（适用于复杂插件）

```kotlin
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ComplexPluginConfig(
    @JsonProperty("core")
    val core: JacksonCorePluginConfig = JacksonCorePluginConfig(),

    @JsonProperty("database")
    val database: DatabaseConfig = DatabaseConfig()
)

class ComplexPlugin : BasePlugin() {
    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().core
}
```

## 📊 方式对比

| 方式                           | 适用场景         | 优点                           | 缺点             |
| ------------------------------ | ---------------- | ------------------------------ | ---------------- |
| **CorePluginConfig.build DSL** | 所有插件（推荐） | 最大灵活性，清晰映射，类型安全 | 需要编写映射逻辑 |
| **继承方式**                   | 简单插件         | 代码简洁，性能最好             | 可能产生属性冲突 |
| **组合方式**                   | 复杂插件         | 结构清晰，避免冲突             | 配置文件嵌套较深 |

## 🎯 选择建议

### 推荐：CorePluginConfig.build DSL
- **所有插件都推荐使用**
- 提供最大的灵活性和控制力
- 配置映射逻辑清晰
- 支持动态配置构建

### 其他方式
- **继承方式**：仅适用于非常简单的插件
- **组合方式**：适用于需要标准化配置结构的复杂插件

## 🔧 配置方法标准模式

所有插件都必须遵循以下标准模式：

```kotlin
class MyPlugin : BasePlugin() {
    // ✅ 标准实现：getPluginConfig方法（写法基本固定）
    fun getPluginConfig(): MyPluginConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<MyPluginConfig>("config.yml")
    }

    // ✅ 必须实现：getCoreConfig方法（推荐使用配置类的getCoreConfig方法）
    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()
}
```



## 🔄 迁移指南

详细的迁移步骤和注意事项，请参考：[迁移指南](../modules/config/MIGRATION_GUIDE.md)

## 💡 最佳实践

1. **一致性**：在同一个插件中只使用一种配置方式
2. **命名规范**：使用kebab-case命名配置字段
3. **默认值**：为所有配置项提供合理的默认值
4. **文档**：为复杂配置提供注释说明
5. **验证**：在配置加载后进行必要的验证
6. **备份**：使用`touchWithMerge`时启用备份功能

## 🔧 故障排除

### 常见问题

1. **配置文件无法读取**
   - 检查Jackson注解是否正确
   - 确保字段名与@JsonProperty匹配

2. **配置重载失败**
   - 确保调用了`configManager.clearCache()`
   - 检查配置文件语法是否正确

3. **默认值不生效**
   - 检查配置类的默认值设置
   - 确保使用了正确的构造函数

更多故障排除信息，请参考：[故障排除](troubleshooting.md)
