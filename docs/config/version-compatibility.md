# 配置文件版本兼容性

本文档介绍如何使用配置文件和语言文件的版本兼容性功能，自动补全缺失的配置项。

## 概述

随着插件版本的迭代，配置文件和语言文件的内容会发生变更。新版本可能会添加新的配置项或翻译键，而用户的旧配置文件中可能缺少这些新项。

版本兼容性功能可以：
- 检测现有配置文件中缺失的配置项
- 自动补全缺失的配置项，使用默认值
- 保持现有配置值不变
- 支持深度嵌套的配置结构
- 可选择性创建备份文件

## 配置文件补全

### 基本用法

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 使用 touchWithMerge 替代 touch
        // 如果配置文件存在，会自动补全缺失的配置项
        touchConfigWithMerge("config.yml", createBackup = true)
        
        // 读取配置
        val config = configManager.parse<MyConfig>("config.yml")
    }
}
```

### 使用默认数据对象

```kotlin
data class ServerConfig(
    val name: String = "Default Server",
    val maxPlayers: Int = 20,
    val features: List<String> = listOf("pvp", "economy"),
    val database: DatabaseConfig = DatabaseConfig()
)

class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 使用默认数据对象进行补全
        touchConfigWithMerge("server.yml", { ServerConfig() }, createBackup = true)
        
        val serverConfig = configManager.parse<ServerConfig>("server.yml")
    }
}
```

### 直接使用 ConfigManager

```kotlin
// 基本模板文件补全
configManager.touchWithMerge("config.yml", "config.yml", createBackup = true)

// 使用默认数据补全
configManager.touchWithMerge("settings.yml", { MySettings() }, createBackup = true)
```

## 语言文件补全

### 基本用法

```kotlin
class MyPlugin : BasePlugin() {
    private fun setupLanguageManager() {
        // 使用扩展方法，自动补全缺失的翻译键
        setupDefaultLanguages(
            mergeWithTemplate = true,
            createBackup = true
        )
    }
}
```

### 自定义语言文件

```kotlin
class MyPlugin : BasePlugin() {
    private fun setupLanguageManager() {
        setupLanguageManager(
            languageFiles = mapOf(
                Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                Locale.US to "lang/en_US.yml",
                Locale.JAPAN to "lang/ja_JP.yml"
            ),
            mergeWithTemplate = true,
            createBackup = true,
            majorLanguage = Locale.SIMPLIFIED_CHINESE,
            defaultLanguage = Locale.US
        )
    }
}
```

### 手动注册语言文件

```kotlin
val languageManager = LanguageManager(this, configManager)
    .register(Locale.SIMPLIFIED_CHINESE, "lang/zh_CN.yml", mergeWithTemplate = true, createBackup = true)
    .register(Locale.US, "lang/en_US.yml", mergeWithTemplate = true, createBackup = true)
    .setMajorLanguage(Locale.SIMPLIFIED_CHINESE)
    .setDefaultLanguage(Locale.US)

setLanguageProvider(languageManager)
```

## 重新加载时的补全

### 配置文件重新加载

```kotlin
override fun reloadPlugin() {
    // 重新加载前先补全配置
    touchConfigWithMerge("config.yml", createBackup = true)
    
    // 然后正常加载配置
    val config = configManager.parse<MyConfig>("config.yml")
}
```

### 语言文件重新加载

```kotlin
override fun reloadPlugin() {
    // 重新加载语言文件并补全缺失的翻译键
    reloadLanguagesWithMerge(
        languageManager,
        mergeWithTemplate = true,
        createBackup = true
    )
}
```

## 工作原理

### 配置文件合并

1. **深度合并**：使用递归算法合并嵌套的配置结构
2. **保留现有值**：只添加缺失的键，不修改现有配置
3. **类型安全**：支持对象、数组、基本类型的合并

### 语言文件合并

1. **键值补全**：检测缺失的翻译键并从模板文件补全
2. **格式保持**：保持原有的YAML格式和注释
3. **多语言支持**：每个语言文件独立补全

## 备份机制

当启用 `createBackup = true` 时：

```
config.yml -> config.backup.1640995200000.yml
lang/zh_CN.yml -> lang/zh_CN.backup.1640995200000.yml
```

备份文件名包含时间戳，确保不会覆盖之前的备份。

## 最佳实践

1. **生产环境备份**：在生产环境中始终启用备份
2. **开发环境测试**：在开发环境中禁用备份以提高性能
3. **版本升级**：在插件版本升级时自动运行补全
4. **配置验证**：补全后验证配置的完整性

## 示例：完整的插件实现

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // 补全主配置文件
        touchConfigWithMerge("config.yml", createBackup = true)
        
        // 设置语言管理器并补全语言文件
        setupDefaultLanguages(
            mergeWithTemplate = true,
            createBackup = true
        )
        
        // 加载配置
        val config = configManager.parse<MyConfig>("config.yml")
        
        logger.info("Plugin enabled with configuration version compatibility")
    }
    
    override fun reloadPlugin() {
        // 重新补全配置
        touchConfigWithMerge("config.yml", createBackup = false)
        
        // 重新补全语言文件
        reloadLanguagesWithMerge(
            languageManager,
            mergeWithTemplate = true,
            createBackup = false
        )
        
        logger.info("Plugin reloaded with updated configurations")
    }
}
```

这样，您的插件就能够自动处理配置文件的版本兼容性问题，为用户提供更好的升级体验。
