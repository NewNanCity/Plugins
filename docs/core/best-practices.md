# Core 模块最佳实践

本文档总结了使用 Core 模块的核心最佳实践，帮助您构建高质量、高性能的 Minecraft 插件。

## 🏗️ 架构设计原则

### 四层架构模式

```
┌─────────────────────────────────────┐
│           主插件类                   │  ← 协调层：生命周期管理
├─────────────────────────────────────┤
│        事务层：Commands/Events       │  ← 对外接口：用户交互
├─────────────────────────────────────┤
│       逻辑层：Modules/Services       │  ← 业务逻辑：核心功能
├─────────────────────────────────────┤
│    基础层：Utils/Config/Data        │  ← 基础设施：工具和数据
└─────────────────────────────────────┘
```

**核心原则：**
- **单向依赖** - 上层可以依赖下层，下层不能依赖上层
- **高内聚低耦合** - 每层职责明确，层间接口清晰
- **自动资源管理** - 所有资源实现 Terminable 并绑定生命周期

## 📦 模块化开发规范

### ✅ 推荐：使用 BaseModule

```kotlin
class MyPlugin : BasePlugin() {
    // ✅ 使用 lateinit 声明模块
    private lateinit var playerModule: PlayerModule
    private lateinit var economyModule: EconomyModule

    override fun onPluginEnable() {
        // ✅ 在 onPluginEnable 中初始化模块
        playerModule = PlayerModule("PlayerModule", this)
        economyModule = EconomyModule("EconomyModule", this)

        reloadPlugin()
    }

    override fun reloadPlugin() {
        // ✅ 重载所有子模块
        super.reloadPlugin()
    }
}

// ✅ 模块实现最佳实践
class PlayerModule(
    moduleName: String,
    val plugin: MyPlugin  // ✅ 声明为具体类型
) : BaseModule(moduleName, plugin) {

    override fun onInit() {
        // 模块初始化逻辑
        subscribeEvent<PlayerJoinEvent> { event ->
            handlePlayerJoin(event.player)
        }

        runAsyncRepeating(0L, 20L * 60) {
            cleanupPlayerData()
        }
    }

    override fun onReload() {
        // 模块重载逻辑
        reloadPlayerConfig()
    }

    private fun handlePlayerJoin(player: Player) {
        // ✅ 直接访问插件特定功能
        plugin.getPlayerConfig().let { config ->
            // 使用插件特定的配置和方法
        }
    }
}
```

### ❌ 避免：传统手动管理

```kotlin
// ❌ 不推荐：手动资源管理（容易出错）
class LegacyManager(private val plugin: BasePlugin) : Terminable {
    override fun close() {
        // 手动清理逻辑，容易遗漏
    }
}

// ❌ 不推荐：lazy 委托模式
val legacyManager: LegacyManager by lazy {
    LegacyManager(plugin = this).also { bind(it) }
}
```

## ⚙️ 配置管理最佳实践

### 标准配置模式

```kotlin
// ✅ 配置类：使用 getCoreConfig() 方法
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MyPluginConfig(
    @JsonProperty("debug")
    val debug: Boolean = false,

    @JsonProperty("database")
    val database: DatabaseConfig = DatabaseConfig(),

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

// ✅ 插件主类：标准配置方法
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

            // 3. 重载所有子模块（必需）
            super.reloadPlugin()

        } catch (e: Exception) {
            logger.error("配置重载失败", e)
            throw e
        }
    }
}
```

## 💬 消息系统最佳实践

### 消息格式选择

```kotlin
class MyPlugin : BasePlugin() {

    private fun sendMessages(player: Player) {
        // ✅ 推荐：使用 Auto 模式，系统自动检测格式
        messager.printf(player, "&a操作成功!")                    // Legacy 格式
        messager.printf(player, "<green>操作成功!</green>")        // MiniMessage 格式

        // ✅ MiniMessage高级功能：悬浮提示
        messager.printf(player, "<yellow><hover:show_text:'<gray>点击查看详细帮助信息'>帮助</hover></yellow>")

        // ✅ MiniMessage高级功能：点击行为
        messager.printf(player, "<click:run_command:/help><green>[点击查看帮助]</green></click>")
        messager.printf(player, "<click:open_url:'https://docs.example.com'><blue>[查看文档]</blue></click>")

        // ✅ MiniMessage高级功能：渐变色彩
        messager.printf(player, "<gradient:green:blue>欢迎来到服务器！</gradient>")

        // ✅ MiniMessage高级功能：复杂交互组合
        messager.printf(player, """
            <click:run_command:/shop>
                <hover:show_text:'<gray>点击打开商店<br><green>购买各种物品'>
                    <gradient:gold:yellow>[商店]</gradient>
                </hover>
            </click>
        """.trimIndent())

        // ✅ 多语言支持
        messager.printf(player, "<%welcome.message%>", player.name)
    }
}
```

### 日志记录规范

**重要区别**：Logger 与 MessageManager 的使用场景不同：

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // ✅ Logger：专注于日志记录，仅支持 Legacy 格式
        logger.info("&a插件启用中...")  // 在控制台显示为绿色
        logger.error("&c严重错误")      // 在控制台显示为红色

        // ✅ MessageManager：专注于用户交互，支持所有格式
        messager.printf(null, "<green>插件启用成功</green>")
        messager.printf(player, "&a欢迎加入服务器!")
    }

    override fun reloadPlugin() {
        // ✅ 语言设置前使用英文日志
        logger.info("Plugin reloading...")

        setupLanguageManager()

        // ✅ 语言设置后可以使用 i18n 模板
        logger.info("<%plugin.config.reloaded%>")
    }
}
```

### 🌐 i18n 国际化最佳实践

#### 1. 统一的i18n Key命名规范

**遵循五层架构的i18n Key分类**：

```yaml
# 1. 核心系统层 (Core System Layer)
core:
  plugin:     # 插件生命周期
  config:     # 配置管理
  error:      # 系统级错误
  success:    # 系统级成功消息
  validation: # 数据验证

# 2. 命令系统层 (Command System Layer)
commands:
  help:             # 帮助系统
  validation:       # 命令参数验证
  [command_name]:   # 具体命令
    success:        # 命令成功
    error:          # 命令错误
    info:           # 命令信息

# 3. 图形界面层 (GUI Layer)
gui:
  common:           # 通用GUI元素
  [gui_name]:       # 具体GUI
    title:          # 标题
    button:         # 按钮文本
    hint:           # 提示信息

# 4. 业务领域层 (Business Domain Layer)
[business_domain]:  # 具体业务（如 death, fly, tpa）
  [action]:         # 具体动作
    success:        # 成功
    error:          # 错误
    info:           # 信息
    broadcast:      # 广播消息

# 5. 日志系统层 (Logging Layer)
log:
  info:             # 信息日志
  warn:             # 警告日志
  error:            # 错误日志
```

#### 2. 语言文件模板和格式

**✅ 推荐：使用MiniMessage格式**：

```yaml
# lang/zh_CN.yml
core:
  plugin:
    enabled: "<green>插件已启用</green>"
    disabled: "<red>插件已禁用</red>"
    reload_completed: "<green>插件重载完成</green>"

  error:
    no_permission: "<red>您没有权限执行此操作</red>"
    player_not_found: "<red>找不到玩家: {0}</red>"

commands:
  help:
    header: "<gold>===== {0} 帮助 =====</gold>"
    usage: "<yellow>用法: {0}</yellow>"

  reload:
    success: "<green>重载成功</green>"
    error: "<red>重载失败: {0}</red>"

gui:
  common:
    back: "<white>返回</white>"
    close: "<red>关闭</red>"
    confirm: "<green>确认</green>"

# 业务领域示例
death:
  cost_deducted: "<red>死亡扣除 {0} 金币</red>"
  insufficient_balance: "<red>余额不足</red>"

# 日志消息不使用颜色
log:
  info:
    plugin_loaded: "插件已加载"
    config_loaded: "配置已加载"
  error:
    service_error: "服务错误: {0}"
```

#### 3. LanguageKeys常量类规范

**✅ 推荐：统一的常量管理**：

基于external-book插件的最佳实践，创建统一的LanguageKeys常量类：

```kotlin
/**
 * 语言键常量类
 *
 * 遵循五层架构的 i18n Key 分类体系，使用 <%key%> 格式便于模板替换。
 *
 * 五层架构分类：
 * 1. 核心系统层 (Core System Layer) - 插件生命周期、配置管理、系统级错误
 * 2. 命令系统层 (Command System Layer) - 命令处理、参数验证、帮助系统
 * 3. 图形界面层 (GUI Layer) - GUI界面、按钮、提示信息
 * 4. 业务领域层 (Business Domain Layer) - 具体业务逻辑、事件处理
 * 5. 日志系统层 (Logging Layer) - 日志消息
 *
 * @author YourName
 * @since 1.0.0
 */
object LanguageKeys {

    // ==================== 核心系统层 (Core System Layer) ====================
    object Core {
        object Plugin {
            const val ENABLED = "<%core.plugin.enabled%>"
            const val DISABLED = "<%core.plugin.disabled%>"
            const val RELOADING = "<%core.plugin.reloading%>"
            const val RELOADED = "<%core.plugin.reloaded%>"
            const val RELOAD_FAILED = "<%core.plugin.reload_failed%>"
        }

        object Error {
            const val NO_PERMISSION = "<%core.error.no_permission%>"
            const val PLAYER_ONLY = "<%core.error.player_only%>"
            const val OPERATION_FAILED = "<%core.error.operation_failed%>"
            const val INVALID_UUID = "<%core.error.invalid_uuid%>"
            const val INVALID_ARGS = "<%core.error.invalid_args%>"
            const val PLAYER_NOT_FOUND = "<%core.error.player_not_found%>"
        }

        object Success {
            const val OPERATION_COMPLETED = "<%core.success.operation_completed%>"
        }

        object Info {
            const val PROCESSING = "<%core.info.processing%>"
        }
    }

    // ==================== 命令系统层 (Command System Layer) ====================
    object Commands {
        object Help {
            const val HEADER = "<%commands.help.header%>"
            const val FOOTER = "<%commands.help.footer%>"
            const val USAGE = "<%commands.help.usage%>"
            const val BASIC_COMMANDS = "<%commands.help.basic_commands%>"
            const val ADMIN_COMMANDS = "<%commands.help.admin_commands%>"
        }

        object Reload {
            const val SUCCESS = "<%commands.reload.success%>"
            const val FAILED = "<%commands.reload.failed%>"
        }

        // 为每个具体命令创建子对象
        object Import {
            const val SUCCESS_NEW = "<%commands.import.success_new%>"
            const val SUCCESS_UPDATE = "<%commands.import.success_update%>"
            const val INVALID_ITEM = "<%commands.import.invalid_item%>"
            const val FAILED = "<%commands.import.failed%>"
        }

        object Export {
            const val SUCCESS = "<%commands.export.success%>"
            const val NOT_FOUND = "<%commands.export.not_found%>"
            const val FAILED = "<%commands.export.failed%>"
        }
    }

    // ==================== 图形界面层 (GUI Layer) ====================
    object Gui {
        object Common {
            const val BACK = "<%gui.common.back%>"
            const val CLOSE = "<%gui.common.close%>"
            const val CONFIRM = "<%gui.common.confirm%>"
            const val PREVIOUS_PAGE = "<%gui.common.previous_page%>"
            const val NEXT_PAGE = "<%gui.common.next_page%>"
            const val UNKNOWN_PLAYER = "<%gui.common.unknown_player%>"
        }

        // 为每个具体GUI创建子对象
        object PlayerList {
            const val TITLE = "<%gui.player_list.title%>"
            const val PLAYER_ENTRY = "<%gui.player_list.player_entry%>"
        }

        // GUI 通用操作和错误
        const val PLAYER_NOT_FOUND = "<%gui.player_not_found%>"
        const val OPERATION_FAILED = "<%gui.operation_failed%>"
        const val INVENTORY_FULL = "<%gui.inventory_full%>"
    }

    // ==================== 业务领域层 (Business Domain Layer) ====================
    // 根据具体业务创建对象，如Death、Economy、TPA等
    object Death {
        const val COST_DEDUCTED = "<%death.cost_deducted%>"
        const val INSUFFICIENT_BALANCE = "<%death.insufficient_balance%>"
    }

    object Economy {
        const val BALANCE_UPDATED = "<%economy.balance_updated%>"
        const val TRANSACTION_FAILED = "<%economy.transaction_failed%>"
    }

    // 事件处理相关
    object Events {
        const val PROCESSING_FAILED = "<%events.processing_failed%>"
        const val DATA_NOT_FOUND = "<%events.data_not_found%>"
    }

    // ==================== 日志系统层 (Logging Layer) ====================
    object Log {
        object Info {
            const val PLUGIN_LOADED = "<%log.info.plugin_loaded%>"
            const val CONFIG_LOADED = "<%log.info.config_loaded%>"
            const val MODULE_INITIALIZED = "<%log.info.module_initialized%>"
        }

        object Error {
            const val INITIALIZATION_FAILED = "<%log.error.initialization_failed%>"
            const val SERVICE_ERROR = "<%log.error.service_error%>"
            const val DATABASE_ERROR = "<%log.error.database_error%>"
        }

        object Warning {
            const val DEPRECATED_CONFIG = "<%log.warning.deprecated_config%>"
            const val PERFORMANCE_WARNING = "<%log.warning.performance_warning%>"
        }
    }
}
```

**🎯 LanguageKeys最佳实践要点**：

1. **五层架构分类**：严格按照Core、Commands、Gui、Business、Log五层分类
2. **嵌套结构**：使用object嵌套提供清晰的层次结构
3. **<%key%>格式**：统一使用<%key%>格式便于模板替换
4. **详细文档**：在类顶部提供完整的分类说明和使用指南
5. **具体化命名**：为每个具体的命令、GUI、业务创建专门的子对象
6. **统一管理**：所有i18n键都在这里定义，其他地方引用这里的常量

#### 4. 实际使用最佳实践

**✅ 推荐：在插件中使用常量**：

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // ✅ 语言设置前使用英文日志
        logger.info("Plugin enabling...")

        // ✅ 设置语言管理器
        setupLanguageManager(
            languageFiles = mapOf(
                Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                Locale.US to "lang/en_US.yml"
            ),
            majorLanguage = Locale.SIMPLIFIED_CHINESE,
            defaultLanguage = Locale.US
        )

        // ✅ 语言设置后使用i18n模板
        logger.info(LanguageKeys.Log.Info.PLUGIN_LOADED)

        reloadPlugin()
    }

    override fun reloadPlugin() {
        // ✅ 使用常量而非硬编码字符串
        logger.info(LanguageKeys.Core.Plugin.RELOADING)

        // 清理配置缓存
        configManager.clearCache()

        // 重新设置语言管理器
        setupLanguageManager(
            languageFiles = mapOf(
                Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                Locale.US to "lang/en_US.yml"
            ),
            majorLanguage = Locale.SIMPLIFIED_CHINESE,
            defaultLanguage = Locale.US
        )

        // ✅ 重新初始化所有Manager
        super.reloadPlugin()

        logger.info(LanguageKeys.Core.Plugin.RELOADED)
    }

    private fun handlePlayerJoin(player: Player) {
        // ✅ 带参数的多语言消息
        messager.printf(player, LanguageKeys.Core.Plugin.ENABLED, player.name)

        // ✅ 错误处理时使用常量
        if (!player.hasPermission("myplugin.use")) {
            messager.error(player, LanguageKeys.Core.Error.NO_PERMISSION)
            return
        }

        // ✅ 成功消息
        messager.success(player, LanguageKeys.Core.Success.OPERATION_COMPLETED)
    }
}
```

**✅ 推荐：在命令中使用常量**：

```kotlin
class ReloadCommand(val plugin: MyPlugin) : BaseCommand {

    override fun execute(sender: CommandSender, args: Array<String>) {
        // ✅ 权限检查使用常量
        if (!sender.hasPermission("myplugin.reload")) {
            messager.error(sender, LanguageKeys.Core.Error.NO_PERMISSION)
            return
        }

        try {
            // ✅ 执行重载
            plugin.reloadPlugin()

            // ✅ 成功消息使用常量
            messager.success(sender, LanguageKeys.Commands.Reload.SUCCESS)
        } catch (e: Exception) {
            // ✅ 错误消息使用常量，带参数
            messager.error(sender, LanguageKeys.Commands.Reload.FAILED, e.message)
            logger.error(LanguageKeys.Log.Error.SERVICE_ERROR, e)
        }
    }
}
```

**✅ 推荐：在GUI中使用常量**：

```kotlin
class PlayerListGui(val plugin: MyPlugin) : BaseGui {

    override fun createGui(): Inventory {
        val gui = Bukkit.createInventory(null, 54,
            messager.sprintf(LanguageKeys.Gui.PlayerList.TITLE))

        // ✅ 返回按钮使用常量
        gui.setItem(49, createButton(
            Material.BARRIER,
            messager.sprintf(LanguageKeys.Gui.Common.BACK)
        ))

        return gui
    }

    private fun handlePlayerClick(player: Player, target: Player) {
        try {
            // 执行传送逻辑
            player.teleport(target.location)

            // ✅ 成功消息使用常量
            messager.success(player, LanguageKeys.Core.Success.OPERATION_COMPLETED)
        } catch (e: Exception) {
            // ✅ 错误消息使用常量
            messager.error(player, LanguageKeys.Gui.OPERATION_FAILED)
            logger.error(LanguageKeys.Log.Error.SERVICE_ERROR, e)
        }
    }
}
```

#### 5. 项目结构中的i18n组织

**✅ 推荐：标准的i18n项目结构**：

```
src/main/kotlin/com/example/myplugin/
├── MyPlugin.kt                     # 主插件类
├── i18n/                          # i18n相关
│   └── LanguageKeys.kt            # 语言键常量类（必需）
├── commands/                      # 命令处理
│   ├── CommandRegistry.kt         # 命令注册器
│   └── impl/                      # 具体命令实现
├── gui/                           # GUI相关
│   └── impl/                      # 具体GUI实现
└── modules/                       # 业务模块
    └── impl/                      # 具体模块实现

src/main/resources/
├── lang/                          # 语言文件目录
│   ├── zh_CN.yml                  # 中文语言文件
│   └── en_US.yml                  # 英文语言文件
├── config.yml                     # 插件配置
└── plugin.yml                     # 插件描述
```

**✅ 推荐：语言文件对应结构**：

```yaml
# lang/zh_CN.yml - 严格对应LanguageKeys的结构
core:
  plugin:
    enabled: "<green>插件已启用</green>"
    disabled: "<red>插件已禁用</red>"
    reloading: "<yellow>插件重载中...</yellow>"
    reloaded: "<green>插件重载完成</green>"
    reload_failed: "<red>插件重载失败: {0}</red>"

  error:
    no_permission: "<red>您没有权限执行此操作</red>"
    player_only: "<red>此命令只能由玩家执行</red>"
    operation_failed: "<red>操作失败: {0}</red>"
    invalid_uuid: "<red>无效的UUID: {0}</red>"
    invalid_args: "<red>参数错误: {0}</red>"
    player_not_found: "<red>找不到玩家: {0}</red>"

commands:
  help:
    header: "<gold>===== {0} 帮助 =====</gold>"
    footer: "<gray>使用 /{0} help <命令> 查看详细帮助</gray>"
    usage: "<yellow>用法: {0}</yellow>"
    basic_commands: "<green>基础命令:</green>"
    admin_commands: "<red>管理员命令:</red>"

  reload:
    success: "<green>配置重载成功</green>"
    failed: "<red>配置重载失败: {0}</red>"

gui:
  common:
    back: "<white>返回</white>"
    close: "<red>关闭</red>"
    confirm: "<green>确认</green>"
    previous_page: "<yellow>上一页</yellow>"
    next_page: "<yellow>下一页</yellow>"

# 业务领域按具体插件功能组织
death:
  cost_deducted: "<red>死亡扣除 {0} 金币</red>"
  insufficient_balance: "<red>余额不足，无法扣除死亡费用</red>"

# 日志消息不使用颜色格式
log:
  info:
    plugin_loaded: "插件已加载"
    config_loaded: "配置已加载"
    module_initialized: "模块 {0} 已初始化"

  error:
    initialization_failed: "初始化失败: {0}"
    service_error: "服务错误: {0}"
    database_error: "数据库错误: {0}"
```

#### 6. MiniMessage高级功能在实际开发中的应用

**✅ 推荐：在GUI和用户交互中使用MiniMessage高级功能**：

```kotlin
class MyPlugin : BasePlugin() {

    private fun sendInteractiveWelcome(player: Player) {
        // ✅ 使用悬浮提示显示详细信息
        messager.printf(player, """
            <gradient:gold:yellow>欢迎来到服务器！</gradient>
            <hover:show_text:'<gray>服务器信息:<br><yellow>在线玩家: {0}<br><green>今日新人: {1}'>
                <blue>[服务器状态]</blue>
            </hover>
        """.trimIndent(), server.onlinePlayers.size, todayNewPlayers)

        // ✅ 可点击的帮助菜单
        messager.printf(player, """
            <click:run_command:/help>
                <hover:show_text:'<gray>点击查看所有可用命令'>
                    <green>[📚 帮助]</green>
                </hover>
            </click>
            <click:run_command:/shop>
                <hover:show_text:'<gray>点击打开商店<br><gold>限时8折优惠！'>
                    <yellow>[🛒 商店]</yellow>
                </hover>
            </click>
        """.trimIndent())
    }

    private fun sendEconomyNotification(player: Player, amount: Double) {
        // ✅ 复杂交互：余额变化通知
        val languageKey = if (amount > 0) "<%economy.money_received%>" else "<%economy.money_lost%>"

        messager.printf(player, """
            $languageKey
            <click:run_command:/balance>
                <hover:show_text:'<gray>当前余额: <yellow>{1}</yellow><br><blue>点击查看详细'>
                    <gold>[💰 查看余额]</gold>
                </hover>
            </click>
        """.trimIndent(), abs(amount), getPlayerBalance(player))
    }

    private fun sendDeathMessage(player: Player, cost: Double) {
        // ✅ 死亡扣费的交互式通知
        messager.printf(player, """
            <red>💀 死亡扣费: {0} 金币</red>
            <click:run_command:/balance>
                <hover:show_text:'<gray>查看当前余额<br><yellow>剩余: {1} 金币'>
                    <yellow>[查看余额]</yellow>
                </hover>
            </click>
            <click:run_command:/respawn>
                <hover:show_text:'<gray>快速复活<br><green>费用: {2} 金币'>
                    <green>[快速复活]</green>
                </hover>
            </click>
        """.trimIndent(), cost, getPlayerBalance(player), respawnCost)
    }
}
```

**🎯 MiniMessage在语言文件中的高级应用**：

```yaml
# 语言文件中的高级交互模板
economy:
  interactive_balance: |
    <gradient:gold:yellow>💰 当前余额: {0}</gradient>
    <click:run_command:/shop>
      <hover:show_text:'<gray>点击打开商店<br><green>购买各种物品'>
        <green>[前往商店]</green>
      </hover>
    </click>
    <click:run_command:/bank>
      <hover:show_text:'<gray>点击打开银行<br><blue>存取款管理'>
        <blue>[银行系统]</blue>
      </hover>
    </click>

gui:
  player_list:
    player_entry: |
      <click:run_command:/tp {0}>
        <hover:show_text:'<gray>玩家信息:<br><yellow>名称: {0}<br><green>在线时间: {1}<br><blue>点击传送到该玩家'>
          <gradient:green:yellow>{0}</gradient>
        </hover>
      </click>

help:
  interactive_menu: |
    <gold>===== 插件帮助 =====</gold>
    <click:run_command:'/myplugin reload'>
      <hover:show_text:'<gray>重新加载插件配置'>
        <green>• 重载配置</green>
      </hover>
    </click>
    <click:run_command:'/myplugin info'>
      <hover:show_text:'<gray>查看插件信息和状态'>
        <blue>• 插件信息</blue>
      </hover>
    </click>
```

**⚠️ 重要提醒**：
- **性能考虑**：复杂的MiniMessage格式会增加解析开销，适合在非高频消息中使用
- **用户体验**：不要过度使用装饰，保持消息的可读性
- **兼容性**：确保客户端支持MiniMessage功能

#### 6. i18n开发检查清单

**开发阶段**：
- [ ] 遵循五层架构i18n Key分类（Core、Commands、Gui、Business、Log）
- [ ] 创建LanguageKeys常量类，使用<%key%>格式
- [ ] 使用object嵌套结构提供清晰层次
- [ ] 在类顶部提供详细的分类说明文档
- [ ] 使用MiniMessage格式编写语言文件
- [ ] 日志消息不使用颜色格式
- [ ] 提供中英文两套完整翻译

**代码规范**：
- [ ] 使用LanguageKeys常量而非硬编码字符串
- [ ] 使用位置参数{0}, {1}传递动态值
- [ ] 在reloadPlugin()中重新设置语言管理器
- [ ] 语言设置前使用英文日志
- [ ] 为每个具体命令、GUI、业务创建专门的子对象

**项目结构**：
- [ ] 创建i18n/LanguageKeys.kt文件
- [ ] 创建lang/目录存放语言文件
- [ ] 语言文件结构严格对应LanguageKeys结构
- [ ] 所有模块引用LanguageKeys中的常量

**测试验证**：
- [ ] 测试所有语言键值是否正确显示
- [ ] 测试语言切换功能
- [ ] 测试参数替换功能{0}, {1}等
- [ ] 测试回退机制（缺失键时的处理）
- [ ] 测试MiniMessage格式解析
- [ ] 验证日志消息不包含颜色代码

**External-Book插件示例**：
参考`plugins/external-book/src/main/kotlin/city/newnan/externalbook/i18n/LanguageKeys.kt`作为标准实现模板。

**相关资源**：
- [External-Book LanguageKeys示例](../../plugins/external-book/src/main/kotlin/city/newnan/externalbook/i18n/LanguageKeys.kt)
- [i18n命名规范](../i18n/naming-conventions.md) - 完整的命名规范文档
- [通用模板文件](../i18n/template.yml) - 中文模板文件
- [英文模板文件](../i18n/template_en.yml) - 英文模板文件

## ⚡ 异步编程最佳实践

### 任务调度最佳实践

| 场景                    | 推荐方案                        | 原因                               |
| ----------------------- | ------------------------------- | ---------------------------------- |
| **计算密集型任务**      | `runAsync {}`                   | 线程级并行，充分利用多核CPU        |
| **简单的后台计算**      | `runAsync {}`                   | 更直接，性能开销小                 |
| **需要返回值的计算**    | `runAsync {}.thenApply {}`      | ITaskHandler提供类型安全的结果处理 |
| **简单的定时/延迟任务** | `runSyncLater/runSyncRepeating` | 专为这种场景设计                   |

### 任务调度实战指南

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // ✅ 计算密集型 - 使用调度器
        runAsync {
            val result = performComplexCalculation(largeDataSet)
            cacheCalculationResult(result)
        }

        // ✅ 链式任务处理
        runAsync {
            loadDataFromDatabase()
        }.thenApply { data ->
            processData(data)
        }.thenRunSync { processedData ->
            updateGameState(processedData)
        }.handle { result, exception ->
            if (exception != null) {
                logger.error("任务执行失败", exception)
                getDefaultResult()
            } else {
                result
            }
        }

        // ✅ 简单重复任务
        runSyncRepeating(0L, 20L) { // 每秒
            updatePlayerScoreboards()
        }
    }
}
```

## 🛡️ 错误处理最佳实践

### 异常安全的事件处理

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // ✅ 使用 events DSL 的异常处理机制
        subscribeEvent<PlayerJoinEvent> {
            handler { event ->
                val player = event.player
                val playerData = loadPlayerData(player.uniqueId)
                updatePlayerDisplay(player, playerData)
            }
            onException { event, e ->
                logger.error("玩家加入事件处理失败", e)
                handlePlayerJoinFallback(event.player)
            }
        }

    }
}
```

### 资源管理最佳实践

```kotlin
class MyPlugin : BasePlugin() {

    override fun onPluginEnable() {
        // ✅ 定期检查资源状态
        runSyncRepeating(0L, 20L * 60) { // 每分钟
            val stats = getResourceStats()

            if (stats.inactiveCount > 50) {
                logger.warning("检测到可能的资源泄漏: ${stats.inactiveCount} 个非活跃资源")
                cleanupInactiveResources()
            }

            if (getPluginConfig().debug) {
                logger.debug("资源统计: $stats")
            }
        }
    }
}
```

## 📊 项目结构组织

### 推荐的项目结构
```
src/main/kotlin/com/example/myplugin/
├── PluginMain.kt               # 主插件类
├── config/                     # 配置相关
│   ├── PluginConfig.kt
│   └── MessageConfig.kt
├── i18n/                       # 国际化相关
│   └── LanguageKeys.kt         # 语言键常量类
├── modules/                    # BaseModule模块（推荐）
│   ├── DataModule.kt           # 数据管理模块
│   ├── CacheModule.kt          # 缓存管理模块
│   ├── PlayerModule.kt         # 玩家管理模块
│   └── EconomyModule.kt        # 经济管理模块
├── commands/                   # 命令处理（事务层）
│   ├── CommandRegistry.kt      # 命令注册器（Cloud框架）
│   ├── admin/                  # 管理员命令
│   │   ├── AdminCommand.kt
│   │   └── ReloadCommand.kt
│   └── user/                   # 用户命令
│       └── UserCommand.kt
├── services/                   # 业务服务（可选）
│   ├── PlayerService.kt
│   └── EconomyService.kt
└── utils/                      # 工具类
    ├── Extensions.kt
    └── Constants.kt
```

## 🎯 核心原则总结

### ✅ 推荐做法

1. **使用 BaseModule** - 享受自动资源管理和生命周期绑定
2. **声明具体 Plugin 类型** - 避免类型转换，提供更好的 IDE 支持
3. **遵循四层架构** - 基础层、逻辑层、事务层、主插件层
4. **使用 CorePluginConfig.build DSL** - 灵活配置核心功能
5. **区分 Logger 和 MessageManager** - 日志记录 vs 用户交互
6. **选择合适的异步方案** - 根据任务特性选择合适的任务调度方式
7. **实现 reloadPlugin 方法** - 支持配置热重载

### ❌ 避免做法

1. **手动实现 TerminableConsumer** - 容易出现资源泄漏
2. **使用 lazy 委托初始化模块** - 可能导致初始化时机问题
3. **避免在任务中使用阻塞调用** - 会影响性能
4. **忘记调用 super.reloadPlugin()** - 子模块不会被重载
5. **在语言设置前使用 i18n 模板** - 会导致模板解析失败

## 🎮 命令系统最佳实践

### Cloud 命令框架

项目已从 CommandAPI 迁移到 **Cloud 命令框架**，使用注解驱动的方式编写命令系统。

#### ✅ 推荐：使用 Cloud 框架

```kotlin
// ✅ 依赖配置（在 Dependencies.kt 中）
object Command {
    const val cloudPaper = "org.incendo:cloud-paper:2.0.0-beta.10"
    const val cloudMinecraftExtras = "org.incendo:cloud-minecraft-extras:2.0.0-beta.10"
    const val cloudAnnotations = "org.incendo:cloud-annotations:2.0.0-beta.10"
}

// ✅ 命令注册器（固定模板）
class CommandRegistry(val plugin: MyPlugin) {
    // 创建命令管理器
    val commandManager = LegacyPaperCommandManager.createNative(
        plugin,
        ExecutionCoordinator.asyncCoordinator()
    ).also {
        if (it.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            it.registerBrigadier()
        } else if (it.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            it.registerAsynchronousCompletions()
        }
    }

    // 创建注解解析器
    val commandAnnotationParser = AnnotationParser(commandManager, CommandSender::class.java).also {
        // i18n 映射，支持 Adventure 组件
        it.descriptionMapper { key -> RichDescription.of(plugin.messager.sprintf(key)) }
    }

    val help: MinecraftHelp<CommandSender>

    init {
        // 解析注解并注册命令
        val commands = commandAnnotationParser.parse(this)
        commands.forEach { commandManager.command(it) }

        // 生成帮助指令
        help = MinecraftHelp.createNative("/myplugin", commandManager)
    }
}
```

#### 命令实现最佳实践

```kotlin
class CommandRegistry(val plugin: MyPlugin) {
    // ... 初始化代码 ...

    // ✅ 基础命令示例
    @Command("myplugin reload")
    @CommandDescription(LanguageKeys.Commands.Reload.Description)
    @Permission("myplugin.admin.reload")
    fun reloadCommand(sender: CommandSender) {
        try {
            plugin.reloadPlugin()
            plugin.messager.success(sender, LanguageKeys.Commands.Reload.Success)
        } catch (e: Exception) {
            plugin.messager.error(sender, LanguageKeys.Commands.Reload.Failed, e.message)
            plugin.logger.error("重载失败", e)
        }
    }

    // ✅ 带参数的命令
    @Command("myplugin give <player> <item> [amount]")
    @CommandDescription(LanguageKeys.Commands.Give.Description)
    @Permission("myplugin.admin.give")
    fun giveCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.Give.PlayerArg)
        target: Player,
        @Argument(value = "item", description = LanguageKeys.Commands.Give.ItemArg)
        material: Material,
        @Argument(value = "amount", description = LanguageKeys.Commands.Give.AmountArg)
        @Default("1") amount: Int
    ) {
        val itemStack = ItemStack(material, amount)
        target.inventory.addItem(itemStack)

        plugin.messager.success(sender, LanguageKeys.Commands.Give.Success,
            target.name, amount, material.name)
    }

    // ✅ 帮助指令（必需）
    @Command("myplugin help [query]")
    @CommandDescription(LanguageKeys.Commands.Help.Description)
    fun helpCommand(
        sender: CommandSender,
        @Greedy @Default("") @Argument(value = "query", description = LanguageKeys.Commands.Help.Query)
        query: String
    ) {
        help.queryCommands(query, sender)
    }

    // ✅ 自动补全提供器
    @Suggestions("help-query")
    fun helpQuerySuggestions(ctx: CommandContext<CommandSender>, input: String) =
        CompletableFuture.supplyAsync {
            commandManager.createHelpHandler()
                .queryRootIndex(ctx.sender())
                .entries()
                .map { Suggestion.suggestion(it.syntax()) }
                .toList()
        }
}
```

#### 项目结构最佳实践

```kotlin
// ✅ 推荐：按功能分类命令
class CommandRegistry(val plugin: MyPlugin) {
    init {
        // 注册所有命令类
        val commandClasses = listOf(
            AdminCommands(plugin),
            UserCommands(plugin),
            EconomyCommands(plugin)
        )

        commandClasses.forEach { commandAnnotationParser.parse(it) }
    }
}

// ✅ 管理员命令类
class AdminCommands(val plugin: MyPlugin) {
    @Command("myplugin admin reload")
    @Permission("myplugin.admin.reload")
    fun reloadCommand(sender: CommandSender) { /* ... */ }

    @Command("myplugin admin debug <toggle>")
    @Permission("myplugin.admin.debug")
    fun debugCommand(sender: CommandSender, toggle: Boolean) { /* ... */ }
}

// ✅ 用户命令类
class UserCommands(val plugin: MyPlugin) {
    @Command("myplugin info")
    @Permission("myplugin.user.info")
    fun infoCommand(sender: CommandSender) { /* ... */ }

    @Command("myplugin status")
    @Permission("myplugin.user.status")
    fun statusCommand(player: Player) { /* ... */ }
}
```

### ❌ 避免：CommandAPI 旧方式

```kotlin
// ❌ 不再使用：CommandAPI 方式（已废弃）
// class BaseCommand { ... }
// class CommandValidator { ... }
// class CommandPermissions { ... }
// class CommandMessages { ... }
```

**迁移说明**：
- 移除所有 CommandAPI 相关依赖和代码
- 使用 Cloud 框架的注解方式重写命令
- 删除 `base/` 目录下的命令基础类
- 更新 `plugin.yml` 依赖配置

## 🔗 相关文档

- [📦 BaseModule 详解](base-module.md)
- [♻️ 资源管理系统](terminable.md)
- [⚡ 事件处理](events.md)
- [🚀 任务调度](scheduler.md)
- [💬 消息系统](messaging.md)
- [🎮 命令系统详解](commands.md)

---

**遵循这些最佳实践，构建高质量、高性能的 Minecraft 插件！**
```kotlin
class MyPlugin : BasePlugin() {
    // ✅ 使用lateinit声明BaseModule
    private lateinit var playerModule: PlayerModule
    private lateinit var economyModule: EconomyModule
    private lateinit var commandRegistry: CommandRegistry

    override fun onPluginLoad() {
        // ✅ 在 load 阶段进行基础初始化
        logger.info("插件加载中...")
    }

    override fun onPluginEnable() {
        // ✅ 按依赖顺序初始化组件
        // 初始化模块
        playerModule = PlayerModule("PlayerModule", this)
        economyModule = EconomyModule("EconomyModule", this)
        commandRegistry = CommandRegistry(this)

        // 调用模块方法
        playerModule.setupPlayerTracking()
        economyModule.setupEconomyIntegration()

        // 调用重载方法
        reloadPlugin()

        logger.info("插件启用完成")
    }

    override fun reloadPlugin() {
        // 插件特定重载逻辑
        super.reloadPlugin()  // 重载所有子模块
    }
}

// ✅ BaseModule实现示例（推荐模式）
class PlayerModule(
    moduleName: String,
    val plugin: MyPlugin  // ✅ 声明为具体Plugin类型的属性
) : BaseModule(moduleName, plugin) {

    // ✅ 重要：手动调用init()来触发初始化
    init { init() }

    override fun onInit() {
        logger.info("PlayerModule initializing...")

        // 事件绑定到模块
        subscribeEvent<PlayerJoinEvent> { event ->
            handlePlayerJoin(event.player)
        }

        subscribeEvent<PlayerQuitEvent> { event ->
            handlePlayerQuit(event.player)
        }

        // 调度任务绑定到模块
        runAsyncRepeating(0L, 20L * 60) {
            cleanupOfflinePlayerData()
        }
    }

    override fun onReload() {
        logger.info("PlayerModule reloading...")
        // 重新加载玩家配置
    }

    fun setupPlayerTracking() {
        // ✅ 可以直接使用具体类型的plugin，无需类型转换
        plugin.getPlayerConfig().forEach { config ->
            // 访问插件特定的方法和属性
        }
    }

    private fun handlePlayerJoin(player: Player) {
        // ✅ 直接访问插件特定功能，无需 (bukkitPlugin as MyPlugin)
        plugin.notifyPlayerJoin(player)
    }

    private fun handlePlayerQuit(player: Player) {
        // 玩家退出处理逻辑
        plugin.cleanupPlayerData(player)
    }
}
```

## ⚡ 性能优化

### 1. 事件处理优化

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // ✅ 使用events DSL和过滤器减少不必要的处理，会自动绑定资源自动释放
        events<PlayerMoveEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            filter { event ->
                // 只处理跨区块的移动
                val from = event.from
                val to = event.to ?: return@filter false
                from.chunk != to.chunk
            }
            handler { event ->
                handleChunkChange(event.player, event.to!!.chunk)
            }
            onException { event, e ->
                logger.error("处理区块变更事件失败", e)
            }
        }

        // ✅ 使用自动过期的事件监听器
        events<VehicleExitEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            filter { it.vehicle is Minecart }
            filter { it.vehicle.isEmpty }
            expireWhen { System.currentTimeMillis() > someTimestamp }
            expireAfter(3) // 处理3次后自动注销
            handler { event ->
                (event.vehicle as Minecart).maxSpeed = DEFAULT_SPEED
            }
            onException { event, e ->
                logger.error("处理载具退出事件失败", e)
            }
        }

        // ✅ 批量处理频繁事件
        val pendingUpdates = ConcurrentHashMap<Player, Location>()

        events<PlayerMoveEvent> {
            filter { it.to != null }
            handler { event ->
                pendingUpdates[event.player] = event.to!!
            }
        }

        // 每秒批量处理一次
        runSyncRepeating(0L, 20L) {
            if (pendingUpdates.isNotEmpty()) {
                val updates = pendingUpdates.toMap()
                pendingUpdates.clear()

                processBatchLocationUpdates(updates)
            }
        }
    }
}
```

### 2. 任务调度优化

#### 使用场景选择指南

**任务调度器**：适用于各种异步操作
- 文件读写操作
- 数据库查询和更新
- 网络请求和API调用
- 磁盘缓存操作
- 复杂的数据处理和计算
- CPU密集型算法
- 大量数据的处理和转换
- 简单的异步任务

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {

        // ✅ 计算密集型 - 使用Scheduler异步
        runAsync {
            // CPU密集型计算
            val result = performComplexCalculation(largeDataSet)
            // 回到主线程应用结果
            runSync {
                applyCalculationResult(result)
            }
        }

    }
}
```

#### 基础任务调度（推荐）
使用新的ITaskHandler API进行类型安全的任务调度：

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // ✅ 基础任务调度 - 使用新的ITaskHandler API

        // 轻量级同步任务
        val displayTask: ITaskHandler<Unit> = runSync {
            updatePlayerDisplays()
        }

        // 轻量级异步任务
        val dataTask: ITaskHandler<String> = runAsync {
            loadDataFromFile()
        }

        // 重量级任务使用异步，避免阻塞主线程
        val heavyTask: ITaskHandler<ProcessedData> = runAsync {
            performHeavyCalculation()
        }

        // ✅ 任务链式调用 - 类型安全
        heavyTask
            .thenRunAsync { data ->
                // 异步处理结果
                processHeavyData(data)
            }
            .thenRunSync { processedData ->
                // 回到主线程更新游戏状态
                updateGameState(processedData)
            }

        // ✅ 延迟任务
        runSyncLater(20L) { // 1秒后执行
            broadcastMessage("延迟消息")
        }

        // ✅ 重复任务
        runSyncRepeating(0L, 20L) { // 每秒执行
            updatePlayerDisplays()
        }

        runAsyncRepeating(0L, 20L * 60) { // 每分钟执行
            performPeriodicMaintenance()
        }
    }
}
```

#### 依赖管理和组合任务

**依赖任务最佳实践**：
```kotlin
class MyPlugin : BasePlugin() {
    private fun setupDependentTasks() {
        // ✅ 基础依赖 - 顺序执行
        val configTask = runAsync { loadConfiguration() }
        val dbTask = runAsync(dependencies = listOf(configTask)) { handler ->
            val config = configTask.getNow(null)!!
            connectToDatabase(config.dbUrl)
        }

        // ✅ 多重依赖 - 等待多个任务完成
        val userTask = runAsync { loadUserData() }
        val permTask = runAsync { loadPermissions() }
        val initTask = runAsync(dependencies = listOf(userTask, permTask, dbTask)) { handler ->
            val users = userTask.getNow(null)!!
            val permissions = permTask.getNow(null)!!
            val database = dbTask.getNow(null)!!
            initializeSystem(users, permissions, database)
        }

        // ✅ 组合任务 - ALL模式
        val allDataTask = combinedTaskHandlers(CombindMode.ALL, userTask, permTask)
        allDataTask.thenRunSync { _ ->
            logger.info("所有基础数据加载完成")
        }

        // ✅ 组合任务 - ANY模式（容错处理）
        val backupTask1 = runAsync { loadFromPrimarySource() }
        val backupTask2 = runAsync { loadFromBackupSource() }
        val anyDataTask = combinedTaskHandlers(CombindMode.ANY, backupTask1, backupTask2)
        anyDataTask.thenRunSync { _ ->
            logger.info("至少一个数据源可用")
        }
    }
}
```

#### 高级任务调度模式

**错误处理和异常安全**：
```kotlin
class MyPlugin : BasePlugin() {
    private fun setupAdvancedTasks() {
        // ✅ 错误处理和重试机制
        val robustTask: ITaskHandler<String> = runAsync {
            try {
                loadCriticalData()
            } catch (e: Exception) {
                logger.error("Critical data loading failed", e)
                "default_fallback_data"
            }
        }

        // ✅ 复杂任务链
        val complexChain = runAsync {
            loadUserData()
        }.thenRunAsync { userData, handler ->
            // 依据用户数据加载权限信息
            loadPermissionsFor(userData)
        }.thenRunAsync { permissions, handler ->
            // 生成用户会话
            createUserSession(permissions)
        }.thenRunSync { session, handler ->
            // 最终回到主线程通知完成
            notifySessionReady(session)
        }

        // ✅ 并行任务处理 - 使用依赖管理
        val configTask = runAsync { loadConfig() }
        val languageTask = runAsync { loadLanguageFiles() }
        val permissionsTask = runAsync { loadPermissions() }

        // 等待所有任务完成后继续
        val initTask = runAsync(dependencies = listOf(configTask, languageTask, permissionsTask)) { handler ->
            val config = configTask.getNow(null)!!
            val language = languageTask.getNow(null)
            val permissions = permissionsTask.getNow(null)

            if (language != null && permissions != null) {
                initializePlugin(config, language, permissions)
            } else {
                // 降级处理
                initializePluginWithDefaults(config)
            }
        }

        // ✅ 或者使用组合任务
        val allDataTask = combinedTaskHandlers(CombindMode.ALL, configTask, languageTask, permissionsTask)
        allDataTask.thenRunSync { _ ->
            val config = configTask.getNow(null)!!
            val language = languageTask.getNow(null)!!
            val permissions = permissionsTask.getNow(null)!!
            initializePlugin(config, language, permissions)
        }
    }
}
```

**任务生命周期管理**：
```kotlin
class MyPlugin : BasePlugin() {
    private val longRunningTasks = mutableListOf<ITaskHandler<*>>()

    private fun setupLifecycleManagedTasks() {
        // ✅ 跟踪长期运行的任务
        val monitoringTask = runAsyncRepeating(0L, 20L * 30) { // 每30秒
            performSystemMonitoring()
        }
        longRunningTasks.add(monitoringTask)

        val cleanupTask = runAsyncRepeating(0L, 20L * 300) { // 每5分钟
            performCleanupTasks()
        }
        longRunningTasks.add(cleanupTask)

        // ✅ 任务状态监控
        runSyncRepeating(0L, 20L * 60) { // 每分钟检查
            longRunningTasks.removeAll { task ->
                if (task.isCompleted()) {
                    logger.info("长期任务已完成: ${task}")
                    true
                } else if (task.isCancelled()) {
                    logger.info("长期任务已取消: ${task}")
                    true
                } else {
                    false
                }
            }
        }
    }

    override fun close() {
        // ✅ 清理时取消所有长期任务
        longRunningTasks.forEach { task ->
            if (!task.isCompleted()) {
                task.cancel(true)
            }
        }
        longRunningTasks.clear()
        super.close()
    }
}
```

**避免阻塞的最佳实践**：
```kotlin
class MyPlugin : BasePlugin() {
    private fun setupNonBlockingTasks() {
        // ⚠️ 重要警告：避免在任务中使用阻塞调用

        // ❌ 错误方式：使用get()会阻塞线程
        // val badTask = runAsync {
        //     val result = someTask.get() // 这会阻塞当前线程！
        //     processResult(result)
        // }

        // ❌ 错误方式：在异步任务中阻塞等待
        // val anotherBadTask = runAsync {
        //     val future = loadDataAsync()
        //     val result = future.get(5, TimeUnit.SECONDS) // 阻塞等待！
        //     processResult(result)
        // }

        // ✅ 正确方式：使用getNow()进行非阻塞检查
        val goodTask = runAsync {
            loadDataAsync()
        }.thenApplyAsync { data ->
            processData(data)
        }.handle { result, exception ->
            if (exception != null) {
                logger.error("任务执行失败", exception)
                null
            } else {
                result
            }
        }

        // ✅ 正确方式：使用依赖任务避免阻塞
        val task1 = runAsync { loadUserData() }
        val task2 = runAsync { loadConfigData() }

        // 使用依赖管理等待两个任务都完成
        val combinedTask = runAsync(dependencies = listOf(task1, task2)) { handler ->
            val userData = task1.getNow(null)!!
            val configData = task2.getNow(null)!!
            combineData(userData, configData)
        }

        // ✅ 或者使用组合任务
        val allTask = combinedTaskHandlers(CombindMode.ALL, task1, task2)
        allTask.thenRunSync { _ ->
            val userData = task1.getNow(null)!!
            val configData = task2.getNow(null)!!
            val combined = combineData(userData, configData)
            applyData(combined)
        }

        // ✅ 任务调度方式：结构化并发
        runAsync {
            try {
                // 传统异步任务处理
                val dataTask = runAsync { loadDataFromDatabase() }
                val configTask = runAsync { loadConfigFromFile() }

                val data = dataTask.get()
                val config = configTask.get()

                // 回到主线程处理结果
                runSync {
                    processLoadedData(data, config)
                }
            } catch (e: Exception) {
                logger.error("异步任务执行失败", e)
            }
        }
    }


    private fun demonstrateTaskHandlerWarnings() {
        val task = runAsync { "some result" }

        // ⚠️ 警告：这些方法会阻塞线程，仅在必要时使用
        // val result = task.get() // 阻塞直到任务完成
        // val result2 = task.get(5, TimeUnit.SECONDS) // 阻塞最多5秒

        // ✅ 推荐：使用非阻塞方式
        val result = task.getNow(null) // 立即返回，如果未完成则返回null

        if (result != null) {
            // 任务已完成，处理结果
            processResult(result)
        } else {
            // 任务未完成，设置回调
            task.thenRunSync { actualResult ->
                processResult(actualResult)
            }
        }
    }
}
```

**新调度器API最佳实践总结**：

```kotlin
class SchedulerBestPractices(plugin: MyPlugin) : BaseModule("SchedulerBestPractices", plugin) {
    override fun onInit() {

        // ✅ 推荐：使用getNow()进行非阻塞检查
        val task = runAsync { loadData() }
        val result = task.getNow(null)
        if (result != null) {
            processResult(result)
        } else {
            task.thenRunSync { data -> processResult(data) }
        }

        // ✅ 推荐：使用依赖管理而不是手动等待
        val task1 = runAsync { loadUserData() }
        val task2 = runAsync { loadConfigData() }
        val combinedTask = runAsync(dependencies = listOf(task1, task2)) { handler ->
            val userData = task1.getNow(null)!!
            val configData = task2.getNow(null)!!
            combineData(userData, configData)
        }

        // ✅ 推荐：使用组合任务处理多个并行任务
        val allTask = combinedTaskHandlers(CombindMode.ALL, task1, task2)
        val anyTask = combinedTaskHandlers(CombindMode.ANY, task1, task2)

        // ✅ 推荐：在任务内部处理异常而不是使用已废弃的handle方法
        val safeTask = runAsync {
            try {
                riskyOperation()
            } catch (e: Exception) {
                logger.error("操作失败", e)
                getDefaultValue()
            }
        }

        // ✅ 推荐：使用链式调用进行任务流水线
        runAsync { loadRawData() }
            .thenRunAsync { data -> processData(data) }
            .thenRunSync { processedData -> applyToGame(processedData) }
    }
}
```

### 3. 缓存策略

```kotlin
class PlayerDataService : Terminable {
    // ✅ 使用合适的缓存类型
    private val playerCache = LRUCache<UUID, PlayerData>(100)
    private val configCache = InfiniteCache<String, ConfigData>() // 警告：无限缓存可能导致内存泄漏！

    suspend fun getPlayerData(uuid: UUID): PlayerData {
        // ✅ 缓存优先策略
        return playerCache.getOrPut(uuid) {
            // 缓存未命中时从数据库加载
            loadPlayerDataFromDatabase(uuid)
        }
    }

    fun updatePlayerData(uuid: UUID, data: PlayerData) {
        // ✅ 更新缓存和持久化
        playerCache.put(uuid, data)

        // 异步保存到数据库
        runAsync {
            savePlayerDataToDatabase(uuid, data)
        }
    }

    override fun close() {
        // ✅ 清理缓存
        playerCache.clear()
        configCache.clear()
    }
}
```

## 🛡️ 错误处理

### 1. 异常安全的事件处理

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // ✅ 使用events DSL的异常处理机制（推荐）
        events<PlayerJoinEvent> {
            priority(EventPriority.NORMAL)
            filter { !it.isCancelled }
            handler { event ->
                val player = event.player

                // 可能出错的操作
                val playerData = loadPlayerData(player.uniqueId)
                updatePlayerDisplay(player, playerData)
            }
            onException { event, e ->
                // ✅ 统一的异常处理
                logger.error("<%event.player_join_failed%>", e, event.player.name)

                // ✅ 提供降级处理
                handlePlayerJoinFallback(event.player)
            }
        }

        // ✅ 或者在handler内部处理异常
        events<PlayerQuitEvent> {
            handler { event ->
                try {
                    val player = event.player
                    savePlayerData(player.uniqueId)
                } catch (e: Exception) {
                    logger.error("保存玩家数据失败", e)
                    // 不影响其他玩家的退出处理
                }
            }
        }
    }

    private fun handlePlayerJoinFallback(player: Player) {
        // 提供基础的欢迎功能
        messager.printf(player, "<%welcome.basic%>")
    }
}
```

}
```

## 🔧 资源管理

### 1. BaseModule vs 传统资源管理

**强烈推荐**：使用BaseModule进行资源管理，避免手动实现TerminableConsumer或Terminable：

```kotlin
// ✅ 推荐：使用BaseModule（自动资源管理）
class DataModule(
    moduleName: String,
    val plugin: MyPlugin  // ✅ 声明为具体Plugin类型的属性
) : BaseModule(moduleName, plugin) {

    // ✅ 重要：手动调用init()来触发初始化
    init { init() }

    override fun onInit() {
        // 所有资源自动绑定到模块，无需手动管理
        subscribeEvent<PlayerJoinEvent> { event ->
            handlePlayerJoin(event.player)
        }

        runAsyncRepeating(0L, 20L * 60) {
            performMaintenance()
        }
    }

    private fun performMaintenance() {
        // ✅ 直接访问插件特定功能，无需类型转换
        plugin.getDataConfig().let { config ->
            // 使用插件特定的配置和方法
        }
    }

    // 无需手动实现close()，BaseModule自动处理
}

// ❌ 不推荐：手动实现资源管理（高级功能，需要小心资源管理问题）
class LegacyDataManager(private val plugin: MyPlugin) : TerminableConsumer, Terminable {
    private val terminableRegistry = CompositeTerminable.create()

    // 需要手动绑定和清理所有资源，容易出现资源泄漏
    override fun <T : AutoCloseable> bind(terminable: T): T {
        return terminableRegistry.bind(terminable)
    }

    override fun close() {
        terminableRegistry.close()
    }
}
```

### 2. 自动资源回收策略

**优先使用自动回收**：相比定期遍历检查，自动回收机制更高效：

```kotlin
class SmartCacheModule(moduleName: String, plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    override fun onInit() {
        // ✅ 推荐：基于事件的自动清理
        subscribeEvent<PlayerQuitEvent> { event ->
            // 玩家离开时自动清理相关缓存
            playerCache.remove(event.player.uniqueId)
            sessionManager.invalidateSession(event.player.uniqueId)
        }

        // ✅ 推荐：基于弱引用的自动回收
        val weakReferenceCache = WeakHashMap<UUID, PlayerData>()

        // ✅ 推荐：合批清理过期资源
        runAsyncRepeating(0L, 20L * 300) { // 每5分钟
            val expiredKeys = cacheManager.getExpiredKeys()
            if (expiredKeys.isNotEmpty()) {
                cacheManager.removeAll(expiredKeys) // 批量清理
                logger.debug("Cleaned up ${expiredKeys.size} expired cache entries")
            }
        }

        // ❌ 避免：频繁的全量遍历检查
        // runAsyncRepeating(0L, 20L * 30) { // 每30秒全量检查
        //     cacheManager.entries.forEach { (key, value) ->
        //         if (value.isExpired()) {
        //             cacheManager.remove(key) // 逐个检查和清理，效率低
        //         }
        //     }
        // }
    }
}
```

### 3. 正确的资源绑定和懒加载

**注意**：对于新项目，推荐使用BaseModule而不是传统的lazy + bind模式：

```kotlin
class MyPlugin : BasePlugin() {
    // ✅ 推荐：使用lateinit声明BaseModule
    private lateinit var dataModule: DataModule
    private lateinit var cacheModule: CacheModule
    private lateinit var economyModule: EconomyModule

    // ✅ 兼容性：传统Manager仍然支持（但不推荐新项目使用）
    private val legacyDatabaseManager: DatabaseManager by lazy {
        DatabaseManager(this).also { bind(it) }
    }

    private val legacyCacheManager: CacheManager by lazy {
        CacheManager().also { bind(it) }
    }

    override fun onPluginEnable() {
        // ✅ 在onPluginEnable中初始化模块
        dataModule = DataModule("DataModule", this)
        cacheModule = CacheModule("CacheModule", this)
        economyModule = EconomyModule("EconomyModule", this)

        // 调用模块方法
        economyModule.setup()
    }

    override fun reloadPlugin() {
        try {
            logger.info("<%plugin.config.reloading%>")

            // 1. 清理配置缓存
            configManager.clearCache()

            // 2. 重新设置语言管理器
            setupLanguageManager(
                languageFiles = mapOf(
                    Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                    Locale.US to "lang/en_US.yml"
                ),
                majorLanguage = Locale.SIMPLIFIED_CHINESE,
                defaultLanguage = Locale.US
            )

            // 3. 重载所有BaseModule子模块（必需）
            super.reloadPlugin()

            logger.info("<%plugin.config.reloaded%>")
        } catch (e: Exception) {
            logger.error("<%plugin.config.reload_failed%>", e)
            throw e
        }
    }

    // ✅ close() 方法已由 BasePlugin 自动处理资源清理
    // 不需要手动实现，所有通过 bind() 绑定的资源会自动释放
}

// ✅ 自定义资源实现 Terminable
class CustomService : Terminable {
    private val executorService = Executors.newFixedThreadPool(4)
    private var isShutdown = false

    fun doSomething() {
        if (!isShutdown) {
            executorService.submit {
                // 异步任务
            }
        }
    }

    override fun close() {
        // ✅ 防止重复关闭
        if (isShutdown) return
        isShutdown = true

        // ✅ 正确关闭资源
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warning("线程池未能正常关闭")
                }
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}

// ✅ 管理器示例
class DatabaseManager(private val plugin: MyPlugin) : Terminable {
    private var connection: Connection? = null
    val isConnected: Boolean get() = connection?.isClosed == false

    init {
        connect()
    }

    private fun connect() {
        // 数据库连接逻辑
    }

    fun reload() {
        close()
        connect()
    }

    override fun close() {
        connection?.close()
        connection = null
    }
}

class CacheManager : Terminable {
    private val cache = ConcurrentHashMap<String, Any>()
    val status: String get() = "缓存条目: ${cache.size}"

    fun reload() {
        cache.clear()
        // 重新加载缓存逻辑
    }

    override fun close() {
        cache.clear()
    }
}
```

### 2. 内存泄漏预防

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        // ✅ 定期检查资源状态
        runSyncRepeating(0L, 20L * 60) { // 每分钟
            val stats = getResourceStats()

            // 检查是否有资源泄漏
            if (stats.inactiveCount > 50) {
                logger.warning("检测到可能的资源泄漏: ${stats.inactiveCount} 个非活跃资源")

                // 手动清理非活跃资源
                cleanupInactiveResources()
            }

            // 记录资源使用情况
            if (getPluginConfig().debug.resources) {
                logger.info("资源统计: $stats")
            }
        }
    }

    private fun cleanupInactiveResources() {
        // Core 框架会自动清理绑定的资源
        // 这里可以添加额外的清理逻辑
        System.gc() // 建议垃圾回收（可选）
    }
}
```

### 3. 资源监控和调试

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        if (getPluginConfig().debug.enabled) {
            // ✅ 资源使用监控
            runAsyncRepeating(0L, 20L * 30) { // 每30秒
                val resourceStats = getResourceStats()
                val memoryUsage = Runtime.getRuntime().let {
                    (it.totalMemory() - it.freeMemory()) / 1024 / 1024
                }

                logger.info("""
                    资源监控:
                    - 绑定资源数: ${resourceStats.totalBound}
                    - 活跃资源数: ${resourceStats.activeCount}
                    - 内存使用: ${memoryUsage}MB
                    - 线程数: ${Thread.activeCount()}
                """.trimIndent())

                // 检查内存使用过高
                if (memoryUsage > 500) {
                    logger.warning("内存使用过高: ${memoryUsage}MB")
                }
            }
        }
    }
}
```

## 🎯 命令系统最佳实践

### 1. 四层架构在命令系统中的应用

**架构分层**：
```
基础层：CommandValidator, CommandPermissions, CommandMessages
逻辑层：业务服务（如TransferManager, PlayerService）
事务层：具体命令实现（DonateCommand, AllocateCommand）
主插件层：CommandRegistry 作为BaseModule提供生命周期管理
```

#### CommandMessages 消息键格式规范

**重要**：所有 CommandMessages 常量必须使用 `<%xxx%>` 格式，便于脚本检测所有message key：

```kotlin
// ✅ 正确格式
object CommandMessages {
    const val ERROR_NO_PERMISSION = "<%error.no_permission%>"
    const val ERROR_EXECUTION_FAILED = "<%commands.error.execution_failed%>"
    const val SUCCESS_OPERATION = "<%success.operation%>"
    const val RELOAD_SUCCESS = "<%commands.reload.success%>"
    const val RELOAD_FAILED = "<%commands.reload.failed%>"
    const val INFO_STATUS = "<%info.status%>"
}

// ✅ 正确使用方式
plugin.messager.printf(sender, CommandMessages.ERROR_NO_PERMISSION)

// ❌ 错误方式：不要手动添加 <%...%>
plugin.messager.printf(sender, "<%${CommandMessages.ERROR_NO_PERMISSION}%>")
```

#### ✅ 统一的命令处理模式
```kotlin
// 事务层：具体命令实现
class DonateCommand(plugin: MyPlugin) : BaseCommand(plugin) {

    override fun createSubcommand(): CommandAPICommand {
        return CommandAPICommand("donate")
            .withPermission(CommandPermissions.DONATE)
            .withHelp("Donate to foundation", "Transfer money to the foundation")
            .withArguments(DoubleArgument("amount", 0.01))
            .executesPlayer(PlayerCommandExecutor { player, args ->
                // 第一步：参数解析和类型转换
                val amount = args["amount"] as Double

                // 第二步：参数验证
                if (!validateAndSendError(player) { CommandValidator.validateAmount(amount) }) {
                    return@PlayerCommandExecutor
                }

                // 第三步：权限检查（已在withPermission中处理）

                // 第四步：调用业务逻辑层
                handleDonate(player, amount)
            })
    }

    private fun handleDonate(player: Player, amount: Double) {
        executeWithErrorHandling(player, "donate") {
            // 调用逻辑层服务
            val result = plugin.transferManager.activeTransfer(player, amount)

            if (result.success) {
                sendSuccessMessage(player, result.messageKey, *result.messageArgs)
            } else {
                sendErrorMessage(player, result.messageKey, *result.messageArgs)
            }
        }
    }
}
```

### 2. 基础层工具类设计

#### 参数验证器（基础层）
```kotlin
// base/CommandValidator.kt
object CommandValidator {

    fun validateAmount(amount: Double): String? {
        return when {
            !amount.isFinite() -> "commands.validation.amount_not_finite"
            amount <= 0.0 -> "commands.validation.amount_not_positive"
            amount > 1000000.0 -> "commands.validation.amount_too_large"
            else -> null
        }
    }

    fun validateAll(vararg validations: () -> String?): String? {
        for (validation in validations) {
            val error = validation()
            if (error != null) return error
        }
        return null
    }
}
```

#### 权限管理器（基础层）
```kotlin
// base/CommandPermissions.kt
object CommandPermissions {
    const val DONATE = "myplugin.donate"
    const val ADMIN = "myplugin.admin"
    const val OTHER = "myplugin.other"

    fun hasAdminPermission(sender: CommandSender): Boolean {
        return sender.hasPermission(ADMIN) || sender.isOp
    }

    fun hasPermission(sender: CommandSender, permission: String): Boolean {
        return sender.hasPermission(permission) || hasAdminPermission(sender)
    }
}
```

### 3. 命令注册器（BaseModule架构）

**强烈推荐**：使用BaseCommandRegistry进行命令管理，提供完整的生命周期管理：

```kotlin
/**
 * MyPlugin命令注册器
 *
 * 基于BaseModule架构的命令管理系统，提供完整的命令生命周期管理：
 * - onInit时自动注册所有命令
 * - onClose时自动注销所有命令
 * - 命令跟踪和错误处理
 *
 * 负责注册和管理所有插件命令，包括：
 * - 主命令注册
 * - 子命令组织
 * - 别名命令注册
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class CommandRegistry(
    val plugin: MyPlugin
) : BaseCommandRegistry("MyPluginCommandRegistry", plugin) {

    // ✅ 重要：手动调用init()来触发初始化
    init { init() }

    override fun registerCommands() {
        registerMainCommand()
        registerAliasCommands()
    }

    /**
     * 注册主命令
     */
    private fun registerMainCommand() {
        // 创建所有子命令
        val commands = createAllCommands()

        // 注册主命令树
        val mainCommand = CommandAPICommand("myplugin")
            .withAliases("mp")
            .withPermission(CommandPermissions.USE)
            .withSubcommands(*commands.mapNotNull { it.createSubcommand() }.toTypedArray())
            .executes(CommandExecutor { sender, _ ->
                // 主命令默认显示帮助信息
                HelpCommand(plugin).handleMainCommand(sender)
            })

        // 使用BaseCommandRegistry的跟踪注册方法
        registerAndTrack(mainCommand, "myplugin")
    }

    /**
     * 注册别名命令
     */
    private fun registerAliasCommands() {
        // 创建所有命令实例
        val commands = createAllCommands()

        // 注册每个命令的别名（如果有）
        commands.forEach { command ->
            try {
                command.registerAlias()
            } catch (e: Exception) {
                logger.debug("Command ${command::class.simpleName} has no alias to register")
            }
        }
    }

    /**
     * 创建所有命令实例
     */
    private fun createAllCommands(): List<BaseCommand> = listOf(
        // 用户命令
        HelpCommand(plugin),
        DonateCommand(plugin),
        StatusCommand(plugin),

        // 管理员命令
        ReloadCommand(plugin),
        AdminCommand(plugin)
    )
}

// Plugin.kt
class MyPlugin : BasePlugin() {
    private lateinit var commandRegistry: CommandRegistry

    override fun onPluginEnable() {
        // 其他初始化...

        // 初始化命令注册器（BaseModule架构，自动注册命令）
        commandRegistry = CommandRegistry(this)

        // 调用重载方法
        reloadPlugin()
    }

    // 命令注册器会在插件关闭时自动注销所有命令
}
```

**核心特性**：
- **自动生命周期管理**：onInit时注册命令，onClose时注销命令
- **命令跟踪**：线程安全的命令跟踪和管理
- **错误处理**：完善的异常处理和日志记录
- **灵活注册**：支持CommandAPI和Bukkit命令的不同注销方式

**最佳实践要点**：

1. **命名规范**：
   - 类名使用 `CommandRegistry`
   - 模块名使用 `{PluginName}CommandRegistry` 格式
   - 方法名遵循 `registerXxxCommand()` 模式

2. **代码组织**：
   - 使用 `createAllCommands()` 方法统一创建命令实例
   - 分离主命令注册和别名命令注册逻辑
   - 添加详细的文档注释说明功能

3. **错误处理**：
   - 别名注册失败时使用 `logger.debug()` 记录
   - 不要因为别名注册失败而中断整个注册过程
   - 使用 try-catch 包装可能失败的操作

4. **插件集成**：
   - 在 `onPluginEnable()` 中初始化 CommandRegistry
   - 使用 `lateinit var` 声明 CommandRegistry 属性
   - 无需手动管理命令生命周期，BaseModule 自动处理
```

### 4. 错误处理和消息管理

#### 统一错误处理模式
```kotlin
// base/BaseCommand.kt
abstract class BaseCommand(protected val plugin: MyPlugin) {

    protected fun validateAndSendError(sender: CommandSender, validation: () -> String?): Boolean {
        val error = validation()
        if (error != null) {
            sendErrorMessage(sender, error)
            return false
        }
        return true
    }

    protected fun executeWithErrorHandling(
        sender: CommandSender,
        operation: String,
        action: () -> Unit
    ) {
        try {
            action()
        } catch (e: Exception) {
            plugin.messager.printf(sender, CommandMessages.ERROR_EXECUTION_FAILED, operation, e.message)
            plugin.logger.error("Command operation '$operation' failed", e)
        }
    }
}
```

### 5. 可选依赖处理

#### 经济插件集成最佳实践

**重要**：推荐使用BaseModule而不是手动实现TerminableConsumer，享受自动资源管理：

```kotlin
// ✅ 推荐：使用BaseModule进行可选依赖处理
class TransferModule(moduleName: String, plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    override fun onInit() {
        // 检查并注册EssentialsX事件监听器
        if (plugin.server.pluginManager.getPlugin("Essentials") != null) {
            try {
                registerEssentialsXEventListener()
                logger.info("EssentialsX integration enabled")
            } catch (e: Exception) {
                logger.warn("Failed to register EssentialsX event listener", e)
            }
        }

        // 检查并注册XConomy事件监听器
        if (plugin.server.pluginManager.getPlugin("XConomy") != null) {
            try {
                registerXConomyEventListener()
                logger.info("XConomy integration enabled")
            } catch (e: Exception) {
                logger.warn("Failed to register XConomy event listener", e)
            }
        }
    }

    private fun registerEssentialsXEventListener() {
        // 事件自动绑定到模块，模块销毁时自动清理
        subscribeEvent<UserBalanceUpdateEvent> {
            priority(EventPriority.MONITOR)
            filter { enableTransferDetection }
            handler { event -> handleEssentialsXEvent(event) }
        }
    }

    private fun registerXConomyEventListener() {
        // 事件自动绑定到模块，模块销毁时自动清理
        subscribeEvent<PlayerAccountEvent> {
            priority(EventPriority.MONITOR)
            filter { enableTransferDetection }
            handler { event -> handleXConomyEvent(event) }
        }
    }

    private fun handleEssentialsXEvent(event: UserBalanceUpdateEvent) {
        // 处理EssentialsX事件
    }

    private fun handleXConomyEvent(event: PlayerAccountEvent) {
        // 处理XConomy事件
    }
}

// ❌ 不推荐：手动实现TerminableConsumer（仅用于高级场景）
class LegacyTransferManager(private val plugin: MyPlugin) : TerminableConsumer {
    // 需要手动管理资源，容易出现资源泄漏
    // 仅在需要精细控制资源生命周期时使用
}
```

## 🏗️ BaseModule 模块化开发

### 1. BaseModule vs 传统Manager

**BaseModule优势**：
- 自动资源管理，无需手动实现TerminableConsumer
- 模块级事件和任务绑定，避免资源泄漏
- 完整的生命周期管理（初始化、重载、关闭）
- 支持子模块嵌套和层次化管理
- 统一的日志和消息接口

### 🎯 BaseModule最佳实践：具体Plugin类型

**强烈推荐**：在模块构造函数中声明具体的Plugin类型属性，避免类型转换：

```kotlin
// ✅ 推荐：声明具体Plugin类型的属性
class MyModule(
    moduleName: String,
    val plugin: MyPlugin  // 声明为具体类型的属性
) : BaseModule(moduleName, plugin) {

    override fun onInit() {
        // ✅ 直接访问插件特定功能，无需类型转换
        plugin.getSpecificConfig().let { config ->
            // 使用插件特有的方法和属性
        }

        plugin.registerCustomListener(this)
        plugin.getCustomManager().initialize()
    }

    fun someModuleMethod() {
        // ✅ 在任何地方都可以直接使用具体类型的plugin
        plugin.performSpecificAction()
    }
}

// ❌ 不推荐：需要类型转换
class BadModule(moduleName: String, plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    override fun onInit() {
        // ❌ 需要类型转换，容易出错且不优雅
        (bukkitPlugin as MyPlugin).getSpecificConfig()

        // ❌ 每次都需要转换
        val myPlugin = bukkitPlugin as MyPlugin
        myPlugin.performSpecificAction()
    }
}
```

**优势**：
- **类型安全**：编译时检查，避免运行时ClassCastException
- **代码简洁**：无需重复的类型转换代码
- **IDE支持**：完整的代码补全和重构支持
- **可读性强**：意图明确，代码更易理解

```kotlin
// ✅ BaseModule方式（推荐）
class PlayerManager(
    moduleName: String,
    val plugin: MyPlugin  // ✅ 声明为具体Plugin类型的属性
) : BaseModule(moduleName, plugin) {

    // ✅ 重要：手动调用init()来触发初始化
    init { init() }

    override fun onInit() {
        // 事件绑定到模块，模块销毁时自动清理
        subscribeEvent<PlayerJoinEvent> { event ->
            handlePlayerJoin(event.player)
        }

        // 任务绑定到模块，模块销毁时自动清理
        runAsyncRepeating(0L, 20L * 60) {
            cleanupPlayerData()
        }
    }

    override fun onReload() {
        // 重载逻辑
        reloadPlayerConfig()
    }

    private fun cleanupPlayerData() {
        // ✅ 直接访问插件特定功能
        plugin.getPlayerDataManager().cleanup()
    }

    // 无需手动实现close()，由BaseModule自动处理
}

// ❌ 传统Manager方式（仍然支持，但不推荐新项目使用）
class LegacyPlayerManager(private val plugin: MyPlugin) : TerminableConsumer, Terminable {
    private val terminableRegistry = CompositeTerminable.create()

    init {
        // 事件绑定到插件，可能导致资源泄漏
        plugin.subscribeEvent<PlayerJoinEvent> { event ->
            handlePlayerJoin(event.player)
        }
    }

    override fun <T : AutoCloseable> bind(terminable: T): T {
        return terminableRegistry.bind(terminable)
    }

    override fun close() {
        terminableRegistry.close()
    }

    fun reload() {
        // 手动重载逻辑
    }
}
```

### 2. 模块层次化设计

```kotlin
// 父模块
class EconomyModule(
    moduleName: String,
    val plugin: MyPlugin  // ✅ 声明为具体Plugin类型的属性
) : BaseModule(moduleName, plugin) {

    // 子模块使用lateinit声明
    private lateinit var bankModule: BankModule
    private lateinit var shopModule: ShopModule

    override fun onInit() {
        logger.info("EconomyModule initializing...")

        // 初始化子模块
        bankModule = BankModule("BankModule", this)
        shopModule = ShopModule("ShopModule", this)

        // 调用子模块方法
        bankModule.setupBankSystem()
        shopModule.setupShopSystem()
    }

    override fun onReload() {
        // 父模块重载时，子模块会自动重载
        logger.info("EconomyModule reloading...")
        // ✅ 可以直接访问插件特定功能
        plugin.getEconomyConfig().let { config ->
            // 重载经济配置
        }
    }
}

// 子模块
class BankModule(moduleName: String, parentModule: BaseModule) : BaseModule(moduleName, parentModule) {

    override fun onInit() {
        logger.info("BankModule initializing...")

        // 子模块的事件和任务也绑定到自己
        subscribeEvent<PlayerInteractEvent> { event ->
            handleBankInteraction(event)
        }
    }

    fun setupBankSystem() {
        // 银行系统设置
    }
}
```

### 3. 模块间通信

```kotlin
class PlayerModule(moduleName: String, plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    // 获取其他模块的引用
    private val economyModule: EconomyModule? by lazy {
        plugin.getFirstChildOrNull(EconomyModule::class.java)
    }

    override fun onInit() {
        subscribeEvent<PlayerJoinEvent> { event ->
            val player = event.player

            // 与其他模块交互
            economyModule?.let { economy ->
                economy.setupPlayerAccount(player)
            }
        }
    }
}
```

### 4. 模块配置管理

```kotlin
class ConfigurableModule(moduleName: String, plugin: MyPlugin) : BaseModule(moduleName, plugin) {

    private lateinit var moduleConfig: ModuleConfig

    override fun onInit() {
        loadConfig()
        setupWithConfig()
    }

    override fun onReload() {
        // 重载时重新加载配置
        loadConfig()
        setupWithConfig()
    }

    private fun loadConfig() {
        // 假设插件有configManager
        if (plugin is MyPlugin) {
            moduleConfig = plugin.configManager.parse<ModuleConfig>("modules/${moduleName.lowercase()}.yml")
        }
    }

    private fun setupWithConfig() {
        if (moduleConfig.enabled) {
            // 根据配置设置模块
        }
    }
}
```

## 📝 代码质量

### 1. 类型安全

```kotlin
// ✅ 使用密封类表示状态
sealed class LoadResult<out T> {
    data class Success<T>(val data: T) : LoadResult<T>()
    data class Error(val exception: Exception) : LoadResult<Nothing>()
    object Loading : LoadResult<Nothing>()
}

class DataService {
    suspend fun loadData(): LoadResult<PlayerData> {
        return try {
            val data = fetchDataFromDatabase()
            LoadResult.Success(data)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}

// 使用时的类型安全处理
when (val result = dataService.loadData()) {
    is LoadResult.Success -> {
        processData(result.data)
    }
    is LoadResult.Error -> {
        logger.error("数据加载失败", result.exception)
    }
    is LoadResult.Loading -> {
        showLoadingIndicator()
    }
}
```

### 2. 扩展函数

```kotlin
// ✅ 创建有用的扩展函数
fun Player.sendColoredMessage(message: String) {
    this.sendMessage(ChatColor.translateAlternateColorCodes('&', message))
}

fun Player.hasPermissionOrOp(permission: String): Boolean {
    return this.isOp || this.hasPermission(permission)
}

fun Location.isSafeForTeleport(): Boolean {
    val block = this.block
    val above = this.clone().add(0.0, 1.0, 0.0).block

    return block.type.isSolid &&
           above.type == Material.AIR &&
           this.clone().add(0.0, 2.0, 0.0).block.type == Material.AIR
}

// 使用扩展函数
player.sendColoredMessage("&a欢迎来到服务器！")
if (player.hasPermissionOrOp("myplugin.admin")) {
    // 管理员操作
}
```

## 📊 监控和调试

### 1. 性能监控

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        if (config.getBoolean("monitoring.enabled", false)) {
            // ✅ 启用性能监控
            runSyncRepeating(0L, 20L * 30) { // 每30秒
                val stats = getResourceStats()
                val memoryUsage = Runtime.getRuntime().let {
                    (it.totalMemory() - it.freeMemory()) / 1024 / 1024
                }

                logger.info("""
                    性能统计:
                    - 活跃事件: ${stats.activeEvents}
                    - 活跃任务: ${stats.activeTasks}
                    - 内存使用: ${memoryUsage}MB
                    - TPS: ${server.tps[0]}
                """.trimIndent())
            }
        }
    }
}
```

### 2. 调试工具

```kotlin
class MyPlugin : BasePlugin() {
    override fun onPluginEnable() {
        if (config.getBoolean("debug.enabled", false)) {
            // ✅ 调试模式下的额外功能

            // 事件执行时间监控
            subscribeEvent<Event>()
                .monitor { event, duration ->
                    if (duration > 50) { // 超过50ms
                        logger.warning("事件处理耗时: ${event.javaClass.simpleName} - ${duration}ms")
                    }
                }

            // 命令执行统计
            registerCommand("debug-stats") { sender, _ ->
                val stats = getResourceStats()
                sender.sendMessage("资源统计: $stats")
                true
            }
        }
    }
}
```

## 📋 检查清单

## 🔄 配置重载最佳实践

### 1. reloadPlugin 方法规范

**必须实现**：所有插件都必须重写 `reloadPlugin()` 抽象方法：

```kotlin
/**
 * 重载插件配置 - 标准实现
 */
override fun reloadPlugin() {
    try {
        logger.info("<%plugin.config.reloading%>")

        // 1. 清理配置缓存（必需，否则无法从磁盘加载最新的文件）
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

        // 3. 重新加载配置文件
        // 具体实现根据插件需求而定

        // 4. 重新初始化管理器
        // 例如：manager.reload()

        // 5. 清理缓存和重置状态
        // 例如：cache.clear()

        logger.info("<%plugin.config.reloaded%>")
    } catch (e: Exception) {
        logger.error("<%plugin.config.reload_failed%>", e)
        throw e
    }
}
```

**必须规范**：使用 `setLanguageProvider()` 统一设置：

```kotlin
setupLanguageManager(
    languageFiles = mapOf(
        Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
        Locale.US to "lang/en_US.yml"
    ),
    majorLanguage = Locale.SIMPLIFIED_CHINESE,
    defaultLanguage = Locale.US
)
```

**重要**：
- 使用 `setLanguageProvider()` 而不是分别设置 Logger 和 MessageManager
- 这会自动应用到 StringFormatter、Logger 和 MessageManager
- 确保在插件启用早期和重载时都调用此方法

### 2. onPluginEnable 架构模式

**推荐模式**：在 `onPluginEnable()` 中调用 `reloadPlugin()` 避免代码重复：

```kotlin
override fun onPluginEnable() {
    logger.info("正在启用插件...")

    // 1. 不可重载的功能（依赖检查、命令注册、事件监听器等）
    if (!setupDependencies()) {
        logger.error("依赖检查失败！插件将被禁用。")
        server.pluginManager.disablePlugin(this)
        return
    }

    // 注册命令（不可重载）
    registerCommands()

    // 注册事件监听器（不可重载）
    registerEventListeners()

    // 2. 调用重载方法处理可重载的功能
    reloadPlugin()

    logger.info("插件已成功启用！")
}
```

**架构原则**：
- **不可重载功能**：依赖检查、命令注册、事件监听器注册、管理器初始化
- **可重载功能**：配置加载、语言设置、缓存清理、状态重置
- **避免重复**：可重载的逻辑只在 `reloadPlugin()` 中实现一次

### 3. 命令中的重载调用

```kotlin
private fun handleReloadCommand(plugin: MyPlugin, sender: CommandSender) {
    try {
        plugin.reloadPlugin()
        plugin.messager.printf(sender, CommandMessages.RELOAD_SUCCESS)
    } catch (e: Exception) {
        plugin.messager.printf(sender, CommandMessages.RELOAD_FAILED, e.message)
        plugin.logger.error("<%commands.reload.error_log%>", e)
    }
}
```

### 开发阶段
- [ ] **优先使用BaseModule进行模块化开发**
- [ ] **模块事件和任务绑定到模块而非插件**
- [ ] 所有资源都通过 `bind()` 绑定
- [ ] 事件处理器有异常处理
- [ ] 重量级操作使用异步执行
- [ ] 使用合适的缓存策略
- [ ] 代码有适当的类型注解
- [ ] **实现 reloadPlugin() 方法并调用 super.reloadPlugin()**
- [ ] **使用 setLanguageProvider() 设置语言管理器**
- [ ] **所有文本使用国际化**
- [ ] **命令遵循四层架构模式**
- [ ] **使用CommandValidator进行参数验证**
- [ ] **使用CommandPermissions管理权限**
- [ ] **在executes中完成参数解析和验证**
- [ ] **BaseModule实现onInit、onReload生命周期方法**
- [ ] **使用新的ITaskHandler任务调度系统**
- [ ] **避免在任务中使用get()阻塞方法**
- [ ] **优先使用getNow()进行非阻塞检查**
- [ ] **任务链使用thenApply/thenCompose等方法**
- [ ] **长期任务使用生命周期管理**
- [ ] **异步任务使用适当的场景（复杂异步流程、并发协调）**
- [ ] **避免在异步任务中使用阻塞操作**
- [ ] **异步任务正确处理取消状态**
- [ ] **异步任务异常处理完善**
- [ ] **使用结构化的任务管理**

### 测试阶段
- [ ] 插件启动和关闭正常
- [ ] **BaseModule初始化和关闭正常**
- [ ] **模块重载功能正常工作**
- [ ] **配置重载功能正常工作**
- [ ] 没有资源泄漏警告
- [ ] **模块销毁时相关事件和任务自动清理**
- [ ] 性能表现符合预期
- [ ] 异常情况处理正确
- [ ] 内存使用稳定
- [ ] **所有命令参数验证正确**
- [ ] **权限检查功能正常**
- [ ] **错误消息清晰准确**
- [ ] **可选依赖集成正常**
- [ ] **子模块层次化管理正常**
- [ ] **任务调度无阻塞问题**
- [ ] **任务异常处理正确**
- [ ] **任务生命周期管理正常**
- [ ] **长期运行任务能正确取消**
- [ ] **异步任务正确启动和停止**
- [ ] **异步任务异常不影响其他任务**
- [ ] **异步任务取消机制工作正常**
- [ ] **异步任务内存使用稳定**
- [ ] **异步任务性能符合预期**

### 生产部署
- [ ] 关闭调试模式
- [ ] 配置合理的缓存大小
- [ ] 启用必要的监控
- [ ] 准备错误处理预案
- [ ] 文档和注释完整

## 🗄️ Database 模块最佳实践

### 1. DatabaseManager 懒加载和生命周期绑定

**必须规范**：DatabaseManager 必须使用懒加载模式并自动绑定生命周期：

```kotlin
class MyPlugin : BasePlugin() {
    // ✅ 正确的 DatabaseManager 懒加载模式
    private var _databaseManager: DatabaseManager? = null
    val databaseManager: DatabaseManager? get() = _databaseManager

    // ✅ 根据配置动态创建数据库管理器
    private fun createDatabaseManager(): DatabaseManager? {
        val config = getPluginConfig()

        return when (config.storage.mode.lowercase()) {
            "mysql" -> {
                val mysqlConfig = config.storage.mysqlStorage
                mysql {
                    host(mysqlConfig.host)
                    port(mysqlConfig.port)
                    database(mysqlConfig.database)
                    username(mysqlConfig.username)
                    password(mysqlConfig.password)
                    maxPoolSize(mysqlConfig.poolSettings.maxPoolSize)
                    minIdle(mysqlConfig.poolSettings.minIdle)
                }.also { _databaseManager = it }
            }
            "sqlite" -> {
                sqlite {
                    file("${dataFolder}/database.db")
                    maxPoolSize(10)
                }.also { _databaseManager = it }
            }
            else -> {
                logger.info("Database storage disabled")
                null
            }
        }
    }
}
```

### 2. 数据库配置最佳实践

**推荐配置结构**：
```kotlin
data class StorageConfig(
    val mode: String = "json", // "json", "mysql", "sqlite", "disabled"
    val mysqlStorage: MySQLStorageConfig = MySQLStorageConfig(),
    val sqliteStorage: SQLiteStorageConfig = SQLiteStorageConfig()
)

data class MySQLStorageConfig(
    val host: String = "localhost",
    val port: Int = 3306,
    val database: String = "minecraft",
    val username: String = "root",
    val password: String = "",
    val poolSettings: PoolSettings = PoolSettings()
)

data class PoolSettings(
    val maxPoolSize: Int = 10,
    val minIdle: Int = 2,
    val connectionTimeoutMs: Long = 30000,
    val idleTimeoutMs: Long = 600000,
    val maxLifetimeMs: Long = 1800000
)
```

### 3. 数据库操作最佳实践

Database 模块支持两种操作方式，建议优先使用 ORM：

#### 方式一：ORM 操作（推荐）
使用 Ktorm 等现代 ORM，代码更简洁、类型安全：

```kotlin
// 定义表结构
object Books : Table<Nothing>("books") {
    val id = varchar("id").primaryKey()
    val title = varchar("title")
    val creator = varchar("creator")
    val created = timestamp("created")
    val modified = timestamp("modified")
    val pages = text("pages")
}

class BookManager(private val plugin: MyPlugin) : Terminable {
    private val database: Database by lazy {
        Database.connect(
            dataSource = plugin.databaseManager!!.hikariDataSource!!,
            dialect = MySqlDialect()
        )
    }

    fun saveBook(book: Book) {
        plugin.tasks {
            async {
                // 使用 Ktorm ORM 操作
                database.insert(Books) {
                    set(it.id, book.id.toString())
                    set(it.title, book.title)
                    set(it.creator, book.creator.toString())
                    set(it.created, Timestamp(book.created.time))
                    set(it.modified, Timestamp(book.modified.time))
                    set(it.pages, objectMapper.writeValueAsString(book.pages))
                }
            }
        }
    }

    fun getBook(bookId: String): Book? {
        return database.from(Books)
            .select()
            .where { Books.id eq bookId }
            .map { row ->
                Book(
                    id = row[Books.id]!!,
                    title = row[Books.title]!!,
                    creator = UUID.fromString(row[Books.creator]!!),
                    created = row[Books.created]!!,
                    modified = row[Books.modified]!!,
                    pages = objectMapper.readValue(row[Books.pages]!!, List::class.java) as List<String>
                )
            }
            .firstOrNull()
    }
}
```

#### 方式二：原生 SQL 操作
适用于复杂查询或性能敏感场景：

```kotlin
class BookManager(private val plugin: MyPlugin) : Terminable {

    fun saveBook(book: Book) {
        plugin.databaseManager?.let { db ->
            plugin.tasks {
                async {
                    db.useTransaction { connection ->
                        // 插入书籍基本信息
                        val insertBook = connection.prepareStatement(
                            "INSERT INTO books (id, title, creator, created, modified, pages) VALUES (?, ?, ?, ?, ?, ?)"
                        )
                        insertBook.setString(1, book.id.toString())
                        insertBook.setString(2, book.title)
                        insertBook.setString(3, book.creator.toString())
                        insertBook.setTimestamp(4, Timestamp(book.created.time))
                        insertBook.setTimestamp(5, Timestamp(book.modified.time))
                        insertBook.setString(6, objectMapper.writeValueAsString(book.pages))
                        insertBook.executeUpdate()

                        // 如果任何操作失败，事务会自动回滚
                    }
                }
            }
        }
    }
}
```

#### 选择建议

- **推荐使用 ORM（Ktorm）**：适合大多数常规 CRUD 操作，代码更简洁、类型安全
- **使用原生 SQL**：适合复杂查询、性能敏感场景、需要特定 SQL 功能的情况

**批量操作**：

ORM 批量操作（推荐）：
```kotlin
fun saveBooksInBatch(books: List<Book>) {
    plugin.tasks {
        async {
            // Ktorm 批量插入
            database.batchInsert(Books) {
                books.forEach { book ->
                    item {
                        set(it.id, book.id.toString())
                        set(it.title, book.title)
                        set(it.creator, book.creator.toString())
                        set(it.created, Timestamp(book.created.time))
                        set(it.modified, Timestamp(book.modified.time))
                        set(it.pages, objectMapper.writeValueAsString(book.pages))
                    }
                }
            }
            plugin.logger.info("批量保存了 ${books.size} 本书籍")
        }
    }
}
```

原生 SQL 批量操作：
```kotlin
fun saveBooksInBatch(books: List<Book>) {
    plugin.databaseManager?.let { db ->
        plugin.tasks {
            async {
                db.useBatch { batch ->
                    val sql = "INSERT INTO books (id, title, creator, pages, created, modified) VALUES (?, ?, ?, ?, ?, ?)"

                    books.forEach { book ->
                        batch.addBatch(sql) { statement ->
                            statement.setString(1, book.id.toString())
                            statement.setString(2, book.title)
                            statement.setString(3, book.creator.toString())
                            statement.setString(4, objectMapper.writeValueAsString(book.pages))
                            statement.setTimestamp(5, Timestamp(book.created.time))
                            statement.setTimestamp(6, Timestamp(book.modified.time))
                        }
                    }

                    val results = batch.executeBatch()
                    plugin.logger.info("批量保存了 ${results.sum()} 本书籍")
                }
            }
        }
    }
}
```

### 4. 错误处理和降级策略

**数据库连接失败处理**：
```kotlin
override fun reloadPlugin() {
    try {
        // ... 其他重载逻辑

        // 重新初始化数据库（如果启用）
        val config = getPluginConfig()
        if (config.storage.mode != "json") {
            try {
                _databaseManager?.close() // 关闭旧连接
                _databaseManager = createDatabaseManager()
                logger.info("Database manager reloaded successfully")
            } catch (e: Exception) {
                logger.error("Failed to reload database manager, falling back to JSON storage", e)
                // 降级到 JSON 存储
                _databaseManager = null
            }
        }

    } catch (e: Exception) {
        logger.error("<%plugin.config.reload_failed%>", e)
        throw e
    }
}
```

### 5. 性能监控

**连接池监控**：
```kotlin
override fun onPluginEnable() {
    // ... 其他初始化逻辑

    // 启用数据库监控（如果配置启用）
    if (getPluginConfig().debug.databaseMonitoring) {
        runSyncRepeating(0L, 20L * 60) { // 每分钟检查一次
            databaseManager?.let { db ->
                val stats = db.getPoolStats()
                logger.info("""
                    数据库连接池状态:
                    - 活跃连接: ${stats.activeConnections}
                    - 空闲连接: ${stats.idleConnections}
                    - 总连接数: ${stats.totalConnections}
                    - 等待连接数: ${stats.threadsAwaitingConnection}
                """.trimIndent())

                // 健康检查
                if (!db.isHealthy()) {
                    logger.warning("数据库连接异常！")
                }
            }
        }
    }
}
```

---

**相关文档**：
- 📖 [CommandAPI 最佳使用规范](commandapi-best-practices.md) - 命令系统的详细实现指南
- 🎮 [事件系统教程](events-tutorial.md) - 事件处理最佳实践
- ⏰ [调度器教程](scheduler-tutorial.md) - 任务调度最佳实践
- 🔄 [任务调度系统](scheduler.md) - 现代化异步编程完整教程
- 🗄️ [Database 模块教程](../database/README.md) - 数据库模块完整教程

**返回文档首页** → [📚 Core模块文档](README.md)
