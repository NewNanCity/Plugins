# 消息系统

Core 模块提供了统一的消息系统，支持多种格式（Legacy、MiniMessage、Plain），自动格式检测，完整的国际化支持，以及类型安全的消息模板。

## 🎯 核心特性

### 多格式支持
- **Legacy 格式** - 传统的 `&a&l` 颜色代码
- **MiniMessage 格式** - 现代化的 `<green><bold>` 标签
- **Plain 格式** - 纯文本，无格式
- **自动检测** - 系统自动识别消息格式

### 统一 API
- **MessageManager** - 用户交互消息，支持所有格式
- **Logger** - 日志记录，专用于 Legacy 格式
- **自动国际化** - 支持多语言模板和参数替换

## 🚀 基础用法

### MessageManager 使用

```kotlin
class MyPlugin : BasePlugin() {
    
    override fun onPluginEnable() {
        // 设置消息前缀
        messager.setPlayerPrefix("&7[&6MyPlugin&7] &f")
        messager.setConsolePrefix("[MyPlugin] ")
        
        reloadPlugin()
    }
    
    private fun sendMessages(player: Player) {
        // 自动格式检测
        messager.printf(player, "&a操作成功!")                    // Legacy 格式
        messager.printf(player, "<green>操作成功!</green>")        // MiniMessage 格式
        messager.printf(player, "操作成功!")                      // Plain 格式
        
        // 高级 MiniMessage 功能
        messager.printf(player, "<click:run_command:/help>点击查看帮助</click>")
        messager.printf(player, "<gradient:green:blue>渐变色文本</gradient>")
        messager.printf(player, "<hover:show_text:'<red>提示文本'>悬停查看</hover>")
        
        // 多语言支持
        messager.printf(player, "<%welcome.message%>", player.name)
        
        // 带前缀的消息
        messager.printf(
            sendTo = player,
            prefix = true,
            formatText = "<%player.level_info%>",
            player.name, player.level
        )
    }
}
```

### Logger 使用

```kotlin
class MyPlugin : BasePlugin() {
    
    override fun onPluginEnable() {
        // Logger 专注于日志记录，仅支持 Legacy 格式
        logger.info("&a插件启用中...")      // 在控制台显示为绿色
        logger.warn("&6警告信息")           // 在控制台显示为黄色
        logger.error("&c严重错误")          // 在控制台显示为红色
        
        // 不同的日志级别
        logger.trace("&7详细跟踪信息")      // LogLevel.TRACE
        logger.debug("&e调试信息")          // LogLevel.DEBUG
        logger.info("&a一般信息")           // LogLevel.INFO
        logger.warn("&6警告信息")           // LogLevel.WARN
        logger.error("&c错误信息")          // LogLevel.ERROR
    }
    
    override fun reloadPlugin() {
        // 语言设置前使用英文日志
        logger.info("Plugin reloading...")
        
        setupLanguageManager()
        
        // 语言设置后可以使用 i18n 模板
        logger.info("<%plugin.config.reloaded%>")
    }
}
```

## 🌐 国际化支持

### 设置语言管理器

```kotlin
class MyPlugin : BasePlugin() {
    
    override fun reloadPlugin() {
        try {
            // 清理配置缓存
            configManager.clearCache()
            
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
        } catch (e: Exception) {
            logger.error("配置重载失败", e)
            throw e
        }
    }
}
```

### 语言文件示例

```yaml
# lang/zh_CN.yml
welcome:
  message: "欢迎 {0} 加入服务器！"
  first_time: "这是你第一次加入服务器"

player:
  level_info: "玩家 {0} 当前等级：{1}"
  not_found: "找不到玩家 {0}"

plugin:
  enabled: "插件已启用"
  disabled: "插件已禁用"
  config:
    reloaded: "配置重载成功"
    reload_failed: "配置重载失败"

error:
  no_permission: "你没有权限执行此操作"
  invalid_argument: "无效的参数：{0}"
```

```yaml
# lang/en_US.yml
welcome:
  message: "Welcome {0} to the server!"
  first_time: "This is your first time joining the server"

player:
  level_info: "Player {0} current level: {1}"
  not_found: "Player {0} not found"

plugin:
  enabled: "Plugin enabled"
  disabled: "Plugin disabled"
  config:
    reloaded: "Configuration reloaded successfully"
    reload_failed: "Configuration reload failed"

error:
  no_permission: "You don't have permission to perform this action"
  invalid_argument: "Invalid argument: {0}"
```

### 使用国际化消息

```kotlin
class MyPlugin : BasePlugin() {
    
    private fun sendInternationalizedMessages(player: Player) {
        // 基础国际化消息
        messager.printf(player, "<%welcome.message%>", player.name)
        
        // 带参数的国际化消息
        messager.printf(player, "<%player.level_info%>", player.name, getPlayerLevel(player))
        
        // 错误消息
        messager.printf(player, "<%error.no_permission%>")
        
        // 控制台日志
        logger.info("<%plugin.enabled%>")
        logger.error("<%plugin.config.reload_failed%>")
    }
}
```

## 🎨 格式选择指南

### Legacy 格式
```kotlin
// 传统颜色代码
messager.printf(player, "&a&l成功！&r 操作已完成")
messager.printf(player, "&c错误：&4操作失败")
```

### MiniMessage 格式
```kotlin
// 现代化标签，功能更丰富
messager.printf(player, "<green><bold>成功！</bold></green> 操作已完成")
messager.printf(player, "<red>错误：<dark_red>操作失败</dark_red></red>")

// 高级功能
messager.printf(player, "<click:run_command:/help>点击查看帮助</click>")
messager.printf(player, "<hover:show_text:'详细信息'>悬停查看</hover>")
messager.printf(player, "<gradient:green:blue>渐变色文本</gradient>")
```

### Plain 格式
```kotlin
// 纯文本，无格式
messager.printf(player, "成功！操作已完成")
messager.printf(player, "错误：操作失败")
```

### 格式选择建议

| 场景 | 推荐格式 | 原因 |
|------|----------|------|
| **新项目** | MiniMessage | 功能最丰富，现代化 |
| **兼容性** | Auto 模式 | 自动检测，兼容性最好 |
| **性能敏感** | 指定格式 | 避免检测开销 |
| **日志记录** | Legacy | Logger 专用格式 |

## 🔧 在 BaseModule 中使用

```kotlin
class PlayerModule(moduleName: String, val plugin: MyPlugin) : BaseModule(moduleName, plugin) {
    
    override fun onInit() {
        // 模块初始化消息
        logger.info("&a模块 {0} 已初始化", moduleName)
        
        // 事件中发送消息
        subscribeEvent<PlayerJoinEvent> { event ->
            val player = event.player
            
            // 发送欢迎消息
            messager.printf(
                player,
                "<green>欢迎 <yellow>{0}</yellow> 加入服务器!</green>",
                player.name
            )
            
            // 检查首次加入
            if (isFirstTimePlayer(player)) {
                messager.printf(player, "<%welcome.first_time%>")
            }
        }
    }
    
    private fun sendErrorToPlayer(player: Player, error: String) {
        messager.printf(
            sendTo = player,
            prefix = true,
            formatText = "&c错误: &4{0}",
            error
        )
    }
    
    private fun sendSuccessToPlayer(player: Player, message: String) {
        messager.printf(
            sendTo = player,
            prefix = true,
            formatText = "<green>{0}</green>",
            message
        )
    }
}
```

## ⚙️ 配置消息系统

### 在配置类中设置

```kotlin
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MyPluginConfig(
    @JsonProperty("debug")
    val debug: Boolean = false,
    
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
data class MessageSettings(
    @JsonProperty("player-prefix")
    val playerPrefix: String = "&7[&6MyPlugin&7] &f",
    
    @JsonProperty("console-prefix")
    val consolePrefix: String = "[MyPlugin] ",
    
    @JsonProperty("enable-colors")
    val enableColors: Boolean = true
)
```

## 🛡️ 最佳实践

### 1. 消息类型区分

```kotlin
class MyPlugin : BasePlugin() {
    
    private fun demonstrateMessageTypes() {
        val player = server.onlinePlayers.first()
        
        // ✅ 用户交互 - 使用 MessageManager
        messager.printf(player, "<green>操作成功</green>")
        messager.printf(player, "<%welcome.message%>", player.name)
        
        // ✅ 日志记录 - 使用 Logger
        logger.info("&a玩家 {0} 执行了操作", player.name)
        logger.error("&c操作执行失败")
        
        // ❌ 不要混用
        // logger.info("<green>这不会正确显示</green>") // Logger 不支持 MiniMessage
    }
}
```

### 2. 国际化最佳实践

```kotlin
class MyPlugin : BasePlugin() {
    
    override fun onPluginEnable() {
        // ✅ 语言设置前使用英文日志
        logger.info("MyPlugin enabling...")
        
        // 注册不可重载功能
        registerCommands()
        registerEventListeners()
        
        // 调用重载方法（会设置语言管理器）
        reloadPlugin()
        
        // ✅ 语言设置后可以使用 i18n 模板
        logger.info("<%plugin.enabled%>")
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
}
```

### 3. 错误消息处理

```kotlin
class MyPlugin : BasePlugin() {
    
    private fun handleErrors(player: Player) {
        try {
            performRiskyOperation()
        } catch (e: Exception) {
            // 用户友好的错误消息
            messager.printf(player, "<%error.operation_failed%>")
            
            // 详细的日志记录
            logger.error("&c操作失败", e)
        }
    }
}
```

### 4. 性能考虑

```kotlin
class MyPlugin : BasePlugin() {
    
    private fun optimizeMessages() {
        // ✅ 对于高频消息，预先指定格式避免检测开销
        messager.printf(
            player,
            formatText = "&a成功",
            format = MessageFormat.LEGACY
        )
        
        // ✅ 批量发送消息
        val players = server.onlinePlayers
        players.forEach { player ->
            messager.printf(player, "<%server.announcement%>")
        }
    }
}
```

---

**相关文档：** [⚙️ 配置管理](configuration.md) | [🌐 国际化](i18n.md) | [💡 最佳实践](best-practices.md)
