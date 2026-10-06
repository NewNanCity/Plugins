# 插件开发规范

本文档定义了所有插件必须遵循的开发规范，包括配置管理、重载机制、Manager 生命周期等。

## 四层单向依赖结构

项目严格遵循四层单向依赖结构设计：

```
基础层：配置信息 数据定义 工具类 通用算法 第三方API适配器
逻辑层：管理器 调度器
事务层：事件监听 指令 对外服务
主插件类
```

**重要规范**：
- 每一层的资源对象都需要实现 `Terminable` 接口并传递 plugin 实例
- 可以使用 lazy 初始化，但必须绑定到 plugin 或最近一级的 TerminableConsumer
- 严格遵循单向依赖，上层可以调用下层，下层不可调用上层

> 📖 详细架构设计见 [docs/core/architecture.md](../core/architecture.md)

## Manager 懒加载和生命周期绑定

**必须规范**：所有 Manager 都必须使用懒加载模式并自动绑定生命周期：

```kotlin
class MyPlugin : BasePlugin() {
    // ✅ 正确的 Manager 懒加载模式
    val dataManager: DataManager by lazy {
        DataManager(plugin = this).also { bind(it) }
    }

    val configManager: ConfigManager by lazy {
        ConfigManager(plugin = this).also { bind(it) }
    }
}
```

**错误示例**：
```kotlin
// ❌ 不要使用 lateinit var
private lateinit var _dataManager: DataManager
val dataManager: DataManager get() = _dataManager
```

## 插件配置规范

### 必须实现的配置方法

所有插件都必须实现 `getPluginConfig()` 和 `getCoreConfig()`：

```kotlin
class MyPlugin : BasePlugin() {
    // ✅ 标准实现：getPluginConfig 方法（写法基本固定）
    fun getPluginConfig(): MyPluginConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<MyPluginConfig>("config.yml")
    }

    // ✅ 必须实现：getCoreConfig 方法
    override fun getCoreConfig(): CorePluginConfig {
        return getPluginConfig().core // 组合方式
        // return getPluginConfig() // 继承方式
        // return createCustomCoreConfig() // 自定义方式
    }
}
```

### reloadPlugin() 标准实现

所有插件都必须重写 `reloadPlugin()` 方法：

```kotlin
override fun reloadPlugin() {
    try {
        logger.info("<%plugin.config.reloading%>")

        // 1. 清理配置缓存（必需，否则无法从磁盘加载最新文件）
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

        // 3. 重新初始化所有 Manager
        dataManager.reload()
        cacheManager.reload()

        logger.info("<%plugin.config.reloaded%>")
    } catch (e: Exception) {
        logger.error("<%plugin.config.reload_failed%>", e)
        throw e
    }
}
```

## 国际化(i18n)生命周期规范

**关键要点**：
- **强制要求**：所有文本必须使用 i18n 而非硬编码
- **生命周期规范**：语言设置前必须使用英文日志
- **格式规范**：语言文件使用 `{0} {1}` 等 StringFormat 语法
- **设置方法**：使用 `setupLanguageManager()` 统一设置

```kotlin
override fun onPluginEnable() {
    // ✅ 语言设置前使用英文日志
    logger.info("MyPlugin enabling...")

    // 调用重载方法（会设置语言管理器）
    reloadPlugin()

    // ✅ 语言设置后可以使用 i18n 模板
    logger.info("<%myplugin.plugin.enabled%>")
}
```

> 📖 更多 i18n 细节见 [docs/core/messaging.md](../core/messaging.md) 和 [docs/i18n/README.md](../i18n/README.md)

## 插件结构模板

```kotlin
class YourPlugin : BasePlugin() {
    private lateinit var commandRegistry: CommandRegistry

    override fun onPluginEnable() {
        // 初始化模块（包括 CommandRegistry）
        commandRegistry = CommandRegistry(this)

        // 使用 reloadPlugin() 执行可重载逻辑
        reloadPlugin()
    }

    override fun onPluginDisable() {
        // 清理由 BasePlugin 自动处理
    }

    override fun reloadPlugin() {
        // 所有可重载的初始化逻辑放这里
        // - 配置重载
        // - 语言设置
        // - Manager 重新初始化
    }
}
```

## 模块使用模式

```kotlin
// Config 模块
val config = configManager.parse<YourConfig>("config.yml")

// Database 模块
val db = mysql {
    host("localhost")
    database("plugin_db")
    credentials("user", "pass")
}

// 事件处理（自动清理）
events<PlayerJoinEvent> {
    handler { event -> /* handle event */ }
}

// 任务调度（DSL 扩展）
runSync { /* 主线程任务 */ }
runAsync { /* 后台任务 */ }
runSyncLater(20) { /* 延迟主线程任务 */ }
runAsyncRepeating(0, 20) { /* 重复后台任务 */ }

// GUI 模块（原生 i18n 集成）
openPage(InventoryType.CHEST, 54, player) {
    title("<%gui.main_menu.title%>")
    slotComponent(0, 0) {
        render {
            item(Material.DIAMOND) {
                name("<%gui.button.confirm%>")
                lore("<%gui.button.hint%>")
            }
        }
    }
}
```

## 资源管理

- **自动清理**：BasePlugin 通过 Terminable 模式处理所有资源清理
- **事件订阅**：自动注册和清理
- **调度任务**：跟踪并在 disable 时取消
- **数据库连接**：通过连接池正确关闭
- **协程作用域**：插件 disable 时自动取消
- **命令注册**：CommandRegistry 自动在 disable 时注销

## 相关文档

- [BasePlugin 详解](../core/base-plugin.md)
- [BaseModule 模块化开发](../core/base-module.md)
- [Terminable 资源管理](../core/terminable.md)
- [任务调度系统](../core/scheduler.md)
- [Cloud 命令框架](../core/commands.md)
- [最佳实践总结](../core/best-practices.md)
