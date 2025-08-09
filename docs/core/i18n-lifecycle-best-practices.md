# I18n 生命周期最佳实践

## 概述

在插件开发中，正确处理国际化(i18n)模板的生命周期至关重要。本文档详细说明了在插件启用过程中如何正确使用i18n模板，避免在LanguageProvider设置之前使用模板导致的解析失败。

## 问题描述

### 常见错误模式

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // ❌ 错误：此时LanguageProvider还未设置
        logger.info("<%plugin.enabling%>")
        
        // 初始化代码...
        
        // 在最后调用reloadPlugin()，其中设置LanguageProvider
        reloadPlugin()
        
        // ❌ 错误：虽然LanguageProvider已设置，但前面的模板已经失败
        logger.info("<%plugin.enabled%>")
    }
    
    override fun reloadPlugin() {
        // 在这里设置LanguageProvider
        setupLanguageManager()
        // ...
    }
}
```

### 问题根因

1. `onPluginEnable()`方法在插件启用时立即调用
2. `setupLanguageManager()`和`setLanguageProvider()`在`reloadPlugin()`中调用
3. `reloadPlugin()`在`onPluginEnable()`的最后才调用
4. 在LanguageProvider设置之前使用i18n模板会导致模板无法解析

## 解决方案

### 方案1：延迟使用i18n模板（推荐）

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        instance = this
        
        // ✅ 正确：使用英文硬编码
        logger.info("MyPlugin enabling...")
        
        // 初始化不可重载的功能
        initializeManagers()
        registerCommands()
        registerListeners()
        
        // 调用重载方法（设置LanguageProvider）
        reloadPlugin()
        
        // ✅ 正确：此时LanguageProvider已设置
        logger.info("<%plugin.enabled%>")
    }
    
    override fun reloadPlugin() {
        try {
            logger.info("<%config.reloading%>")
            
            // 设置语言管理器
            setupLanguageManager()
            
            // 其他重载逻辑...
            
            logger.info("<%config.reloaded%>")
        } catch (e: Exception) {
            logger.error("<%config.reload_failed%>", e)
            throw e
        }
    }
    
    private fun setupLanguageManager() {
        val languageManager = LanguageManager(this, configManager)
            .register(Locale.SIMPLIFIED_CHINESE, "lang/zh_CN.yml")
            .register(Locale.US, "lang/en_US.yml")
            .setMajorLanguage(Locale.SIMPLIFIED_CHINESE)
            .setDefaultLanguage(Locale.US)
        
        setLanguageProvider(languageManager)
    }
}
```

### 方案2：提前设置LanguageProvider

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        instance = this
        
        // 立即设置语言管理器
        setupLanguageManager()
        
        // ✅ 现在可以使用i18n模板
        logger.info("<%plugin.enabling%>")
        
        // 其他初始化逻辑...
        
        // 调用重载方法处理其他可重载功能
        reloadPlugin()
        
        logger.info("<%plugin.enabled%>")
    }
}
```

## 最佳实践规则

### 1. 生命周期阶段划分

| 阶段 | 时机 | i18n模板使用 | 推荐日志方式 |
|------|------|-------------|-------------|
| **启用前期** | `onPluginEnable()`开始到`setupLanguageManager()`调用前 | ❌ 禁止 | 英文硬编码 |
| **启用后期** | `setupLanguageManager()`调用后 | ✅ 允许 | i18n模板 |
| **重载阶段** | `reloadPlugin()`方法内 | ✅ 允许 | i18n模板 |
| **运行阶段** | 插件正常运行时 | ✅ 允许 | i18n模板 |

### 2. 日志消息分类

#### 必须立即输出的日志（使用英文硬编码）
- 插件启用开始消息
- 依赖检查失败消息
- 致命错误消息
- 插件禁用消息（如果在LanguageProvider设置前）

#### 可以延迟的日志（使用i18n模板）
- 插件启用成功消息
- 配置重载消息
- 功能状态消息
- 调试信息

### 3. 错误处理

```kotlin
// ✅ 正确的错误处理
private fun setupDependencies(): Boolean {
    if (server.pluginManager.getPlugin("Vault") == null) {
        // 使用英文硬编码，因为LanguageProvider还未设置
        logger.error("Vault plugin not found! Plugin requires Vault for economy support.")
        return false
    }
    return true
}

// ❌ 错误的错误处理
private fun setupDependencies(): Boolean {
    if (server.pluginManager.getPlugin("Vault") == null) {
        // 此时LanguageProvider还未设置，模板无法解析
        logger.error("<%plugin.vault_not_found%>")
        return false
    }
    return true
}
```

## 检查清单

在编写或审查插件代码时，请检查以下项目：

- [ ] `onPluginEnable()`开始部分的日志使用英文硬编码
- [ ] 依赖检查失败的错误消息使用英文硬编码
- [ ] `setupLanguageManager()`在`reloadPlugin()`中调用
- [ ] 插件启用成功消息在`reloadPlugin()`调用之后
- [ ] 所有运行时日志正确使用i18n模板
- [ ] 错误处理消息根据时机选择合适的方式

## 常见问题

### Q: 为什么不在`onPluginEnable()`开始就设置LanguageProvider？

A: 虽然技术上可行，但不推荐，因为：
1. 违反了"可重载功能在`reloadPlugin()`中实现"的架构原则
2. 增加了代码重复（需要在两个地方设置LanguageProvider）
3. 降低了代码的可维护性

### Q: 如何判断某个日志是否可以延迟？

A: 考虑以下因素：
- 是否是错误或警告消息？（通常不能延迟）
- 是否影响插件的正常启用？（不能延迟）
- 是否是状态报告消息？（通常可以延迟）

### Q: 在事件处理器中使用i18n模板安全吗？

A: 是的，事件处理器在插件完全启用后才会被调用，此时LanguageProvider已经设置完成。

## 相关文档

- [Core模块文档](../core/README.md)
- [I18n模块文档](../i18n/README.md)
- [插件开发最佳实践](./best-practices.md)
