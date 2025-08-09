# CLAUDE.md

This file provides guidance to AI coding assistants when working with code in this repository. See README.md for the human-friendly overview and project-tree.md for a quick map.

## Build Commands

### Main Build Tasks
```bash
# Build all plugins (creates shadowJar files)
./gradlew buildAllPlugins

# Build all shadow JARs only
./gradlew shadowJarAll

# Build specific plugin
./gradlew :plugins:external-book:build

# Clean all projects
./gradlew cleanAll
```

### Development Tasks
```bash
# Run test server for specific plugin
./gradlew :plugins:external-book:runServer

# Build single plugin with shadowJar
./gradlew :plugins:external-book:shadowJar

# Build TPA plugin
./gradlew :plugins:tpa:build
./gradlew :plugins:tpa:shadowJar

# Build RailArea plugin
./gradlew :plugins:railarea:build
./gradlew :plugins:railarea:shadowJar

# Build BetterCommandBlock plugin
./gradlew :plugins:better-command-block:build
./gradlew :plugins:better-command-block:shadowJar

# Test RailArea octree
./gradlew :plugins:railarea:test --tests "city.newnan.railarea.spatial.OctreeTest"
```

### Architecture Notes
- **Tests are disabled** project-wide (see build.gradle.kts line 69-71)
- **Java 21** for development/compilation, **Java 17** bytecode for runtime compatibility
- **Kotlin 2.2.0** with JVM target 17
- **CommandRegistry** now uses BaseModule architecture for full lifecycle management

### ⚠️ Critical BaseModule Architecture Change (v2.0)

**Breaking Change**: BaseModule no longer automatically calls `onInit()` in constructor.

**Why this change was necessary**:
- **Problem**: BaseModule called `onInit()` immediately in constructor, but subclass properties weren't initialized yet
- **Result**: Accessing subclass properties in `onInit()` caused NullPointerException
- **Solution**: Subclasses must manually call `init()` in their init block

**Required Pattern**:
```kotlin
class MyModule(
    moduleName: String,
    val plugin: MyPlugin
) : BaseModule(moduleName, plugin) {

    // ✅ REQUIRED: Manual init() call
    init { init() }

    override fun onInit() {
        // Now safe to access all properties
        plugin.someMethod() // ✅ Works correctly
    }
}
```

**All affected modules have been updated**: CommandRegistry, all plugin modules, documentation examples.

## Project Architecture

### Multi-Module Structure
This is a **multi-project Gradle build** for Minecraft plugins with a sophisticated modular architecture:

```
NewNanPlugins/
├── modules/           # Reusable functionality modules
│   ├── core/         # BasePlugin foundation (required)
│   ├── config/       # Multi-format configuration management
│   ├── database/     # HikariCP connection pooling
│   ├── gui/         # Modern GUI framework with native i18n support
│   ├── i18n/         # Internationalization
│   └── network/      # HTTP client utilities
└── plugins/          # Individual plugin projects
    ├── external-book/
    ├── tpa/
    └── ... (8 plugins total)
```

### Core Module (`modules/core`)
**All plugins must extend `BasePlugin`** which provides:
- **Resource Management**: Terminable pattern for automatic cleanup
- **Enhanced Logging**: Multi-provider system (console, file, JSONL)
- **Performance Monitoring**: Built-in metrics and monitoring
- **Event System**: DSL-based event handling with auto-cleanup
- **Scheduler Integration**: Bukkit task scheduling with dependency management and DSL extensions
- **Message Management**: i18n support with template processing
- **Service Registration**: Bukkit service provider integration

### Module Dependencies
**Dependency Pattern**: `core` → optional modules → plugins
- **Required**: All plugins depend on `core` module
- **Optional**: Modules like `database`, `config`, `gui`, `i18n`, `network` are added as needed
- **No Circular Dependencies**: Clean dependency hierarchy

### Plugin Convention
All plugins use the **`newnancity-plugin` convention plugin** which:
- Applies Kotlin, Shadow, and RunPaper plugins
- Configures Java 21 toolchain with Java 17 bytecode target
- Sets up Paper repositories and core dependencies
- Provides `configurePluginMetadata()` function for plugin.yml generation
- Handles dependency relocation to avoid conflicts

## 🏗️ 核心架构原则

### 四层单向依赖结构
项目严格遵循四层单向依赖结构设计，确保代码模块化和可维护性：

```
基础层：配置信息 数据定义 工具类 通用算法 第三方API适配器
逻辑层：管理器 调度器
事务层：事件监听 指令 对外服务
主插件类
```

**重要规范**：
- 每一层的资源对象都需要实现`Terminable`接口并传递plugin实例
- 可以使用lazy初始化，但必须绑定到plugins或最近一级的TerminableConsumer
- 严格遵循单向依赖，上层可以调用下层，下层不可调用上层

### Manager懒加载和生命周期绑定
**必须规范**：所有Manager都必须使用懒加载模式并自动绑定生命周期：

```kotlin
class MyPlugin : BasePlugin() {
    // ✅ 正确的Manager懒加载模式
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
// ❌ 不要使用lateinit var
private lateinit var _dataManager: DataManager
val dataManager: DataManager get() = _dataManager
```

## Key Development Patterns

## 🔧 插件配置规范

### 必须实现的配置方法
**必须实现**：所有插件都必须实现以下配置方法：

```kotlin
class MyPlugin : BasePlugin() {
    // ✅ 标准实现：getPluginConfig方法（写法基本固定）
    fun getPluginConfig(): MyPluginConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<MyPluginConfig>("config.yml")
    }

    // ✅ 必须实现：getCoreConfig方法（根据配置继承方式决定实现）
    override fun getCoreConfig(): CorePluginConfig {
        return getPluginConfig().core // 组合方式示例
        // return getPluginConfig() // 继承方式
        // return createCustomCoreConfig() // 自定义方式
    }
}
```

### reloadPlugin()标准实现
**必须实现**：所有插件都必须重写reloadPlugin()方法：

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

        // 3. 重新初始化所有Manager
        dataManager.reload()
        cacheManager.reload()

        logger.info("<%plugin.config.reloaded%>")
    } catch (e: Exception) {
        logger.error("<%plugin.config.reload_failed%>", e)
        throw e
    }
}
```

## 🌐 国际化(i18n)最佳实践

### 重要生命周期规范
**关键要点**：
- **强制要求**：所有文本必须使用i18n而非硬编码
- **生命周期规范**：语言设置前必须使用英文日志
- **格式规范**：语言文件使用`{0} {1}`等StringFormat语法
- **设置方法**：使用`setupLanguageManager()`统一设置

```kotlin
override fun onPluginEnable() {
    // ✅ 语言设置前使用英文日志
    logger.info("MyPlugin enabling...")

    // 调用重载方法（会设置语言管理器）
    reloadPlugin()

    // ✅ 语言设置后可以使用i18n模板
    logger.info("<%myplugin.plugin.enabled%>")
}
```

### Plugin Structure
```kotlin
class YourPlugin : BasePlugin() {
    private lateinit var commandRegistry: CommandRegistry

    override fun onPluginEnable() {
        // Initialize modules (including CommandRegistry)
        commandRegistry = CommandRegistry(this)

        // Use reloadPlugin() for reloadable logic
        reloadPlugin()
    }

    override fun onPluginDisable() {
        // Cleanup handled automatically by BasePlugin
        // CommandRegistry auto-unregisters commands
    }

    override fun reloadPlugin() {
        // All reloadable initialization logic goes here
        // - Config reloading
        // - Language setup
        // - Manager reinitialization
    }
}
```

### Module Usage Patterns
```kotlin
// Config module usage
val config = configManager.parse<YourConfig>("config.yml")

// Database module usage
val db = mysql {
    host("localhost")
    database("plugin_db")
    credentials("user", "pass")
}

// Event handling with auto-cleanup
events<PlayerJoinEvent> {
    handler { event -> /* handle event */ }
}

// Task scheduling with DSL extensions
runSync { /* main thread task */ }
runAsync { /* background task */ }
runSyncLater(20) { /* delayed main thread task */ }
runAsyncRepeating(0, 20) { /* repeating background task */ }

// GUI module (modern framework with i18n integration)
openPage(InventoryType.CHEST, 54, player) {
    title("<%gui.main_menu.title%>")  // Direct i18n template usage

    slotComponent(0, 0) {
        render {
            item(Material.DIAMOND) {
                name("<%gui.button.confirm%>")  // Auto i18n processing
                lore("<%gui.button.hint%>")
            }
        }
    }
}
```

### Command Management (BaseCommandRegistry)

**⚠️ 重要架构变更（2.0版本）**：BaseModule不再自动调用onInit()，子类必须手动调用init()

```kotlin
// CommandRegistry as BaseModule
class CommandRegistry(
    val plugin: MyPlugin
) : BaseCommandRegistry("MyPluginCommandRegistry", plugin) {

    // ✅ 必须：手动调用init()来触发初始化
    init { init() }

    override fun registerCommands() {
        registerMainCommand()
        registerAliasCommands()
    }

    private fun registerMainCommand() {
        val mainCommand = CommandAPICommand("myplugin")
            .withPermission("myplugin.use")
            .withSubcommands(/* subcommands */)
            .executes(CommandExecutor { sender, _ ->
                // Command logic
            })

        // Auto-tracked registration
        registerAndTrack(mainCommand, "myplugin")
    }
}

// Plugin initialization
class MyPlugin : BasePlugin() {
    private lateinit var commandRegistry: CommandRegistry

    override fun onPluginEnable() {
        // Initialize command registry (auto-registers commands)
        commandRegistry = CommandRegistry(this)

        reloadPlugin()
    }
}
```

**Key Features:**
- **Automatic Lifecycle**: Commands registered in onInit(), unregistered in onClose()
- **Command Tracking**: Thread-safe tracking of all registered commands
- **Error Handling**: Comprehensive exception handling and logging
- **Flexible Registration**: Support for both CommandAPI and Bukkit commands

**Implementation Status:**
- ✅ **Cloud Framework**: Core implementation migrated from CommandAPI
- ✅ **DeathCost**: Successfully migrated to Cloud framework (2025-07-29)
- ✅ **ExternalBook**: Updated to use BaseCommandRegistry architecture
- ✅ **MCron**: Updated to use BaseCommandRegistry architecture
- ✅ **Foundation**: Already using BaseCommandRegistry architecture
- ✅ **Guardian**: Successfully migrated to Cloud framework (2025-07-30)
- ✅ **BetterCommandBlock**: Successfully migrated to Cloud framework (2025-08-04)
- ✅ **Documentation**: Updated best practices and command registry guide

**Best Practices:**
- Use `CommandRegistry` class name with `{PluginName}CommandRegistry` module name
- Implement `createAllCommands()` method for command instance management
- Separate main command and alias command registration logic
- Use `logger.debug()` for alias registration failures
- Initialize in `onPluginEnable()` with `lateinit var` declaration
```

### Resource Management
- **Automatic Cleanup**: BasePlugin handles all resource cleanup via Terminable pattern
- **Event Subscriptions**: Auto-registered and cleaned up
- **Scheduled Tasks**: Tracked and cancelled on disable
- **Database Connections**: Properly closed via connection pooling
- **Coroutine Scopes**: Cancelled automatically on plugin disable
- **Command Registration**: CommandRegistry auto-unregisters commands on disable

## Task Scheduling (BasePluginDSL)

### 完整的任务调度DSL扩展
BasePlugin提供了完整的任务调度DSL扩展函数，基于ITaskHandler设计：

#### 基础任务方法
```kotlin
// 同步任务（主线程）- 适用于Bukkit API操作
runSync { /* 任务代码 */ }
runSync(dependencies) { handler -> /* 带依赖的任务 */ }

// 异步任务（后台线程）- 适用于IO、网络、数据库操作
runAsync { /* 任务代码 */ }
runAsync(dependencies) { handler -> /* 带依赖的任务 */ }
```

#### 延迟任务方法
```kotlin
// tick单位延迟（1 tick = 50ms）
runSyncLater(20) { /* 1秒后执行 */ }
runAsyncLater(40) { /* 2秒后执行 */ }

// 时间单位延迟
runSyncLater(5, TimeUnit.SECONDS) { /* 5秒后执行 */ }
runAsyncLater(1, TimeUnit.MINUTES) { /* 1分钟后执行 */ }
```

#### 重复任务方法
```kotlin
// tick单位重复
runSyncRepeating(0, 20) { /* 每秒执行一次 */ }
runAsyncRepeating(20, 100) { /* 1秒后开始，每5秒执行一次 */ }

// 时间单位重复
runSyncRepeating(0, TimeUnit.SECONDS, 30, TimeUnit.SECONDS) { /* 每30秒执行 */ }
```

#### 别名函数（简洁调用）
```kotlin
sync { /* 同步任务 */ }
async { /* 异步任务 */ }
syncLater(20) { /* 延迟同步 */ }
asyncLater(20) { /* 延迟异步 */ }
syncRepeating(0, 20) { /* 重复同步 */ }
asyncRepeating(0, 20) { /* 重复异步 */ }
```

#### Java兼容函数
```kotlin
// 为Java代码提供Runnable接口支持
runSyncJava { /* Java Runnable */ }
runAsyncJava { /* Java Runnable */ }
runSyncLaterJava(20) { /* Java延迟任务 */ }
runSyncRepeatingJava(0, 20) { /* Java重复任务 */ }
```

#### 任务链式调用
```kotlin
val task = runAsync {
    loadDataFromDatabase()
}.thenRunSync { data ->
    updateGameState(data)
}.thenRunSyncLater(20) { result ->
    notifyPlayers(result)
}

// 任务状态查询
if (task.isCompleted()) {
    val result = task.getNow(null)
}

// 取消任务
task.cancel()
```

### 最佳实践
- **主线程任务**：使用`runSync`进行Bukkit API操作
- **后台任务**：使用`runAsync`进行IO、网络、数据库操作
- **依赖管理**：使用dependencies参数确保任务执行顺序
- **资源清理**：所有任务自动绑定到插件生命周期，无需手动清理
- **错误处理**：使用ITaskHandler的handle方法进行异常处理

## Configuration Management

### Multi-Format Support
The **config module** supports:
- **Core**: JSON, YAML (always available)
- **Optional**: TOML, XML, CSV, Properties, HOCON
- **Bukkit Serialization**: Location, ItemStack, etc.
- **Type Safety**: Generic parsing with `configManager.parse<T>()`

### Language Files
The **i18n module** provides:
- Template processing (`<%player%>`, `<%amount%>`)
- Multi-language fallback support
- Automatic config merging
- Performance caching

## GUI Framework

### GUI 模块 - 深度 i18n 集成
**重大更新** - 与 Core 的 Message 模块深度集成：
- **原生 i18n 支持**: 直接使用 `name("<%key%>")` 而不需要 `messager.sprintf()`
- **自动格式解析**: 支持 MiniMessage 和 Legacy 格式的自动识别
- **统一文本处理**: 通过 GuiManager 的 textPreprocessor 处理所有文本
- **三种使用方式**:
  - 简单模板: `name("<%gui.button.confirm%>")`
  - 单个参数: `guiManager.format("<%key%>", arg)`
  - 复杂参数: `messager.sprintf("<%key%>", args...)`

### 架构特性
- **Component-based**: Reusable UI components
- **Session Management**: Multi-GUI support per player
- **Optimization**: Async rendering, smart caching
- **Type Safety**: Sealed classes for page types
- **Event Handling**: DSL with event propagation

## Build System Details

### BuildSrc Structure
- **`Versions.kt`**: Centralized version management
- **`Dependencies.kt`**: Organized dependency definitions
- **`newnancity-plugin.gradle.kts`**: Plugin convention with common configuration

### Shadow JAR Configuration
- **Dependency Relocation**: Prevents conflicts between plugins
- **Service File Merging**: Preserves META-INF services
- **Minimal Packaging**: Only includes used dependencies

### Gradle Optimization
- **Parallel Execution**: Enabled for faster builds
- **Configuration Cache**: Improves build performance
- **Daemon Disabled**: For CI/CD compatibility

## ⚡ 命令系统最佳实践

### 🚨 重要架构变更：已迁移到 Cloud 框架

**项目已从 CommandAPI 迁移到 Cloud 命令框架**，使用注解驱动的方式编写命令系统。

### Cloud 框架架构
**新的架构分层**：
```
基础层：LanguageKeys（i18n常量）
逻辑层：业务服务（如TransferManager, PlayerService）
事务层：注解命令类（AdminCommands, UserCommands）
主插件层：CommandRegistry 统一注册
```

### 依赖配置
```kotlin
// Dependencies.kt
object Command {
    const val cloudPaper = "org.incendo:cloud-paper:2.0.0-beta.10"
    const val cloudMinecraftExtras = "org.incendo:cloud-minecraft-extras:2.0.0-beta.10"
    const val cloudAnnotations = "org.incendo:cloud-annotations:2.0.0-beta.10"
}
```

### CommandRegistry 模板（固定）
```kotlin
class CommandRegistry(val plugin: MyPlugin) {
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

    val commandAnnotationParser = AnnotationParser(commandManager, CommandSender::class.java).also {
        it.descriptionMapper { key -> RichDescription.of(plugin.messager.sprintf(key)) }
    }

    val help: MinecraftHelp<CommandSender>

    init {
        val commands = commandAnnotationParser.parse(this)
        commands.forEach { commandManager.command(it) }
        help = MinecraftHelp.createNative("/myplugin", commandManager)
    }
}
```

### 注解命令实现
```kotlin
class CommandRegistry(val plugin: MyPlugin) {
    // 基础命令
    @Command("myplugin reload")
    @CommandDescription(LanguageKeys.Commands.Reload.Description)
    @Permission("myplugin.admin.reload")
    fun reloadCommand(sender: CommandSender) {
        try {
            plugin.reloadPlugin()
            plugin.messager.success(sender, LanguageKeys.Commands.Reload.Success)
        } catch (e: Exception) {
            plugin.messager.error(sender, LanguageKeys.Commands.Reload.Failed, e.message)
        }
    }

    // 带参数命令
    @Command("myplugin give <player> <item> [amount]")
    @Permission("myplugin.admin.give")
    fun giveCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.Give.PlayerArg)
        target: Player,
        @Argument(value = "item", description = LanguageKeys.Commands.Give.ItemArg)
        material: Material,
        @Default("1") amount: Int
    ) {
        // 命令逻辑
    }
}
```

### 项目结构
```
commands/
├── CommandRegistry.kt      # 命令注册器
├── admin/                  # 管理员命令
│   ├── AdminCommands.kt
│   └── ReloadCommand.kt
└── user/                   # 用户命令
    ├── UserCommands.kt
    └── HelpCommand.kt
```

### 迁移说明
- ❌ **移除**：BaseCommand、CommandValidator、CommandPermissions、CommandMessages
- ❌ **移除**：CommandAPI 依赖和相关代码
- ✅ **新增**：Cloud 框架依赖
- ✅ **新增**：注解驱动的命令类
- ✅ **保留**：LanguageKeys 用于 i18n 集成

## 🛠️ 技术栈和工具链

### 关键技术迁移
- **Adventure Component API**：完全迁移，解决所有Bukkit API弃用警告
- **Jackson多格式配置**：支持JSON、YAML、TOML、HOCON、XML等
- **Kotlin代码风格**：官方风格，JDK21开发/JDK17运行
- **CommandAPI依赖声明**：必须在plugin.yml中声明depend: [CommandAPI]

### 构建配置模式
遵循 `Versions.kt → Dependencies.kt → build.gradle.kts` 模式：
- **Versions.kt**：集中管理所有版本号
- **Dependencies.kt**：组织化的依赖定义
- **build.gradle.kts**：引用版本和依赖，避免硬编码

## 🎯 插件特定最佳实践

### Foundation插件
- **XConomy经济插件集成**：监听PlayerAccountEvent事件检测玩家被动捐款
- **可选依赖处理**：动态检测并注册经济插件事件监听器

### FeeFly插件
- **扣费飞行服务**：为无飞行权限玩家提供定时扣费飞行
- **公开API设计**：创建Bukkit事件和API提高扩展性

### ExternalBook插件
- **命令拆分架构**：遵循一个指令一个文件的结构
- **GUI重写要求**：严格按照旧插件逻辑重写，保持细节一致
- **异步任务优化**：合理使用异步和同步任务减轻主线程压力

### Database模块选择
- **ORM vs 原生SQL**：推荐使用Exposed等ORM，适合常规CRUD操作
- **批量操作**：使用batchInsert或transaction进行批量处理
- **UPSERT操作**：支持插入或更新，保留原有字段值
- **Guardian插件**：已从Ktorm迁移到Exposed ORM，使用IntIdTable和Table定义

### Guardian插件架构改造
- **四层架构**：已完全重构为符合Core最佳实践的四层架构
- **Cloud框架**：从CommandAPI迁移到Cloud命令框架，使用注解驱动的现代化命令系统
- **i18n国际化**：实现完整的多语言支持，使用MiniMessage格式
- **权限系统**：严格按照老插件的权限节点定义，保持向后兼容
  - `guardian.bypass` - 略过检查权限
  - `guardian.lookup` - 查看玩家的信息
  - `guardian.town.read.other` - 查看其他玩家的城镇信息
  - `guardian.town.write.other` - 修改其他玩家的城镇信息
  - `guardian.judgemental.edit` - 增删风纪委员权限
  - `guardian.reload` - 重载插件配置
- **模块化设计**：清晰的职责分离，基础层、逻辑层、事务层、主插件层
- **命令结构**：按功能分类为admin/user目录，每个命令独立文件
- **异步处理**：数据库操作异步执行，Bukkit API操作同步执行

### BetterCommandBlock插件重写
- **完全重写**：从旧的violet框架迁移到现代化BasePlugin架构（2025-08-04）
- **Cloud框架**：使用Cloud命令框架替代ACF，支持原生选择器解析
- **双重防火墙架构**：
  - **新防火墙模块**：CommandBlockFirewallModule - 基于前缀树的高性能命令验证系统
  - **旧安全模块**：CommandBlockSecurityModule - 作为fallback的简单黑名单系统
- **查看模块**：CommandBlockViewModule支持右键查看命令方块内容（需权限）
- **扩展命令**：完整实现pick、scoreboard random、execute等扩展命令
- **权限系统**：
  - `better-command-block.admin` - 管理员权限（包含所有子权限）
  - `better-command-block.reload` - 重载插件配置
  - `better-command-block.execute` - 使用增强execute命令
  - `better-command-block.read` - 查看命令方块内容
  - `better-command-block.firewall.*` - 防火墙管理权限（status、stats、test、reload）
- **国际化支持**：完整的中英文双语支持，遵循五层架构i18n分类
- **配置管理**：支持被禁命令列表配置，自动销毁违规命令方块
- **日志记录**：详细的违规日志记录到文件，包含位置和时间信息
- **高级防火墙特性**（2025-08-05）：
  - **前缀树匹配**：O(m)时间复杂度的高效命令匹配，支持10万+命令规模
  - **多层验证器**：物品、坐标、选择器、Execute命令的专门验证器
  - **统计系统**：实时统计阻止率、验证时间、热门命令等指标
  - **安全测试**：防御Unicode同形字攻击、零宽字符绕过、命令注入等
  - **性能优化**：并发安全、内存优化、LRU缓存、批量处理
  - **管理命令**：`/cb firewall status|stats|test|reload` 完整的防火墙管理

## Development Guidelines

### Module Selection
Choose modules based on plugin needs:
- **Core**: Always required
- **Config**: For advanced configuration needs (~2.35MB)
- **Database**: For data persistence (~14MB)
- **GUI**: For modern user interfaces
- **I18N**: For multi-language support
- **Network**: For HTTP/REST integration

### Creating New Plugins
1. Add to `settings.gradle.kts`
2. Create `build.gradle.kts` with `newnancity-plugin` convention
3. Extend `BasePlugin`
4. Use `configurePluginMetadata()` for plugin.yml generation
5. Implement required abstract methods

### Performance Considerations
- **Resource Tracking**: BasePlugin monitors resource usage
- **Auto-Cleanup**: 30-second cleanup cycle for completed resources
- **Performance Monitoring**: Built-in timing and metrics
- **Caching**: LRU/LFU cache implementations available

## 📊 开发规范和质量保证

### 代码质量要求
- **异步任务处理**：事件和命令的异步/同步任务分配原则
- **资源管理**：30秒自动清理机制，防止内存泄漏
- **错误处理**：统一的异常处理模式和错误日志
- **性能监控**：资源使用监控和调试工具

### 检查清单

#### 开发阶段
- [ ] 所有资源都通过`bind()`绑定
- [ ] 实现reloadPlugin()方法
- [ ] 使用setupLanguageManager()设置语言管理器
- [ ] 所有文本使用国际化而非硬编码
- [ ] 命令遵循四层架构模式
- [ ] 使用CommandValidator进行参数验证

#### 测试阶段
- [ ] 插件启动和关闭正常
- [ ] 配置重载功能正常工作
- [ ] 没有资源泄漏警告
- [ ] 所有命令参数验证正确
- [ ] 权限检查功能正常
- [ ] 可选依赖集成正常

#### 生产部署
- [ ] 关闭调试模式
- [ ] 配置合理的缓存大小
- [ ] 启用必要的监控
- [ ] 文档和注释完整


## 🌐 国际化(i18n)语言键管理

### 语言分析脚本
项目提供了完整的语言键分析和管理工具：

```bash
# 分析所有插件的语言键使用情况
python .\scripts\language-analyzer.py

# 自动删除冗余的语言键
python .\scripts\language-analyzer.py --remove-redundant --confirm

# 检查i18n最佳实践合规性
python .\scripts\language-analyzer.py --check-best-practices
```

### 语言键补全最佳实践
**完成状态**：✅ 所有插件语言键已完全补全（2025-07-19）
- **总缺失键数**：0个（从233个缺失键补全到完美匹配）
- **总冗余键数**：0个（已自动清理）
- **合规性评分**：所有8个插件均为100分

**补全的关键语言键类别**：
- **Core系统键**：`core.success.operation_completed`
- **事件键**：`events.*.processed`, `events.*.received`, `events.*.started`
- **日志键**：`log.error.initialization_failed`, `log.info.plugin_loaded`
- **命令键**：`commands.*.failed`, `commands.stats.*`
- **GUI键**：完整的GUI界面文本支持

### 语言文件格式规范
**严格遵循**：
- 使用`{0} {1}`等StringFormat语法而非`$key$`格式
- 所有模板使用`<%key%>`格式便于替换
- 中英文语言文件必须保持键结构一致
- 遵循五层架构分类（Core、Commands、Gui、Business、Log）

## 📚 相关文档引用

**Core 模块文档**：
- 📖 [docs/core/README.md](docs/core/README.md) - Core 模块文档导航和快速开始
- 🎯 [docs/core/introduction.md](docs/core/introduction.md) - Core 模块介绍和技术对比
- 💡 [docs/core/concepts.md](docs/core/concepts.md) - 核心概念和设计理念
- 🔧 [docs/core/base-plugin.md](docs/core/base-plugin.md) - BasePlugin 插件基类详解
- 📦 [docs/core/base-module.md](docs/core/base-module.md) - BaseModule 模块化开发
- ♻️ [docs/core/terminable.md](docs/core/terminable.md) - Terminable 资源管理系统
- ⚡ [docs/core/events.md](docs/core/events.md) - 现代化事件处理系统
- 🚀 [docs/core/scheduler.md](docs/core/scheduler.md) - 任务调度系统详解
- 💬 [docs/core/messaging.md](docs/core/messaging.md) - 统一消息和国际化系统
- 🔄 [docs/core/lifecycle.md](docs/core/lifecycle.md) - 生命周期管理指南
- 💡 [docs/core/best-practices.md](docs/core/best-practices.md) - 核心最佳实践总结

**其他模块文档**：
- 🎮 [docs/core/commands.md](docs/core/commands.md) - Cloud 命令框架详解和最佳实践

**插件实现参考**：
- 🚀 [plugins/tpa/README.md](plugins/tpa/README.md) - TPA传送请求插件完整实现
- 📖 [plugins/external-book/README.md](plugins/external-book/README.md) - External-Book插件最佳实践示例

## 🔄 插件迁移记录

### 已完成迁移的插件（2025-08-05）

#### PowerTools插件迁移
- **状态**：✅ 完成迁移
- **功能**：实用工具插件，提供头颅获取功能
- **架构**：从violet框架迁移到BasePlugin + Cloud命令框架
- **特性**：
  - 支持通过URL和玩家名获取头颅
  - 完整的配置热重载功能
  - 国际化支持（中英文）
  - 权限系统集成
- **命令**：
  - `/powertools reload` - 重载配置
  - `/powertools skull url <url>` - 通过URL获取头颅
  - `/powertools skull player <player>` - 通过玩家名获取头颅

#### CreateArea插件迁移
- **状态**：✅ 完成迁移
- **功能**：创造区域管理插件
- **架构**：从violet框架迁移到BasePlugin + Cloud命令框架
- **特性**：
  - WorldEdit集成选择区域
  - Dynmap集成显示区域标记
  - Vault权限系统集成
  - 自动权限组管理
  - GUI界面管理（简化版）
  - 完整的国际化支持
- **命令**：
  - `/createarea reload` - 重载配置
  - `/ctp [player]` - 传送到创造区域
  - `/cset [player]` - 设置创造区域
  - `/cdel [player]` - 删除创造区域
  - `/createarea gui` - 打开管理界面
- **集成**：
  - **WorldEdit**：使用小木斧选择区域
  - **Dynmap**：自动同步区域标记到地图
  - **Vault**：自动管理玩家权限组（Visitor ↔ Builder）

#### NewNanMain插件迁移
- **状态**：✅ 完成迁移（包含GUI系统和功能补充）
- **功能**：牛腩小镇主插件，提供前缀管理和传送系统
- **架构**：从violet框架迁移到BasePlugin + Cloud命令框架 + GUI1模块
- **特性**：
  - 前缀管理系统（全局前缀配置、玩家前缀管理）
  - 传送点管理（权限控制、冷却时间）
  - Vault聊天系统集成
  - 完整的国际化支持
  - 自动前缀检查和应用
  - **现代化GUI界面**（主菜单、传送中心、前缀管理、管理员界面）
  - **内置传送点**（床、家、资源世界、资源下界）
  - **完整主菜单功能**（12个功能按钮，与旧版完全一致）
- **命令**：
  - `/newnan` - 打开主菜单GUI（玩家）/ 显示帮助（控制台）
  - `/newnan reload` - 重载配置
  - `/newnan gui` - 打开GUI界面
  - `/newnan prefix player set <player> <namespace> <key>` - 设置玩家前缀
  - `/newnan prefix player remove <player> <namespace>` - 移除玩家前缀
  - `/newnan prefix player activate <player> <namespace>` - 激活玩家前缀
- **GUI系统**：
  - **主菜单**：12个功能按钮（传送、称号、飞行、TPA、牛腩书局、创造区、慈善榜、成就、小镇、新人指南、熊服查询、管理）
  - **传送中心**：分页显示传送点，包含内置传送点（床、家、资源世界等）
  - **前缀管理**：查看和切换可用前缀，自动清理无效前缀
  - **管理员界面**：传送系统管理、称号系统管理、牛腩书局管理、创造区管理、铁路系统管理
- **集成**：
  - **Vault**：聊天前缀管理和权限检查
  - **GUI模块**：现代化界面系统，支持分页、导航、i18n
- **功能完整性**：✅ 与旧版功能完全一致，包含所有主菜单功能和内置传送点
- **高级管理功能**：✅ 全局前缀管理GUI、传送点管理GUI、聊天输入编辑系统已完整实现
- **聊天输入编辑系统**：
  - **YesInput**：确认/取消输入处理器（简化版，无帮助信息）
  - **TeleportInput**：传送点创建/编辑输入处理器，支持名称、位置、图标、权限设置
  - **PrefixInput**：前缀创建/编辑输入处理器，支持键名和内容设置
  - **NamespaceInput**：命名空间创建/编辑输入处理器，支持重复检查
  - **完整的命令解析**：支持help、cancel、ok等命令，提供详细的错误提示
  - **智能帮助系统**：在输入开始时和出现错误时自动显示帮助信息，参考RailArea实现模式
- **完整CRUD操作**：
  - **全局前缀**：创建、查看、编辑、删除命名空间和前缀
  - **传送点**：创建、查看、编辑、删除传送点，支持权限和图标设置
  - **数据持久化**：所有操作自动保存到配置文件

#### DynamicEconomy插件迁移
- **状态**：✅ 功能完整（架构重构完成，核心逻辑完全一致）
- **功能**：动态经济系统插件
- **架构**：从violet框架迁移到BasePlugin + Cloud命令框架
- **特性**：
  - 价值资源统计和管理
  - 动态商品价格系统
  - 货币发行和国库管理
  - 商品交易系统
  - 完整的国际化支持
- **命令**：
  - `/economy reload` - 重载配置
  - `/economy stats` - 查看经济统计
  - `/economy commodity list/info/buy/sell` - 商品交易系统
  - `/economy issue <amount>` - 发行货币
  - `/economy update-index` - 更新货币指数
  - `/economy reload-issuance` - 重新统计货币发行量
- **集成**：
  - **Vault**：经济系统集成

#### 功能一致性评审结果
- **核心算法完全一致**：✅
  - 时间衰减因子γ：`10.0 / (10.0 + log10((1 + curTime - lastTime)))`
  - 商品价格公式：`buyValue = value * ratio^0.8`，`sellValue = value * ratio^1.2`
  - 货币指数计算：`referenceCurrencyIndex = currencyIssuance / totalWealth`
  - 买卖指数：`buyCurrencyIndex = referenceCurrencyIndex^0.691`，`sellCurrencyIndex = referenceCurrencyIndex^1.309`
- **事件监听逻辑完全一致**：✅
  - BlockDropItemEvent、BlockPlaceEvent、ItemDespawnEvent、UserBalanceUpdateEvent
- **价值资源体系完全保持**：✅ 并扩展支持1.17+深层矿石和下界合金
- **架构显著提升**：✅ 从单体重构为WealthManager、EconomyManager、CommodityManager模块化架构
- **功能大幅扩展**：✅ 命令从3个扩展到9个，配置选项更丰富，增加定时任务
- **接口变化**：已与旧版保持一致（主命令 `dynamicaleconomy|de`，权限前缀 `dynamicaleconomy.*`）；同时保留 `economy|eco` 作为兼容别名
- **待完善**：已完成。详见 plugins/dynamiceconomy/docs/TODO.md

### 迁移技术要点
- **配置管理**：使用Jackson多格式配置，支持touchWithMerge自动补全
- **命令系统**：Cloud框架注解驱动，完全替代ACF
- **事件处理**：BaseModule的subscribeEvent DSL
- **资源管理**：自动绑定生命周期，无需手动清理
- **国际化**：五层架构语言键分类，MiniMessage格式支持
- **依赖管理**：软依赖处理（WorldEdit、Dynmap、Vault）

### 迁移模式总结
1. **主类**：继承BasePlugin，实现reloadPlugin()
2. **配置**：创建Config数据类，实现getCoreConfig()
3. **语言**：LanguageKeys常量类 + 中英文yml文件
4. **命令**：CommandRegistry + 注解命令类
5. **模块**：BaseModule子类，手动调用init()
6. **GUI系统**：使用GUI1模块的现代化界面
7. **构建**：使用newnancity-plugin约定，Shadow打包

### 🎨 GUI迁移最佳实践

通过NewNanMain的GUI迁移，建立了从旧GUI到新GUI1模块的标准迁移模式：

#### 架构迁移
- **旧系统**：violet.gui.PlayerGuiSession + triumph-gui
- **新系统**：GUI1模块 + openPage DSL + 组件化设计

#### 关键迁移步骤
1. **导入更新**：`import city.newnan.gui.dsl.*` + 组件特定导入
2. **页面创建**：`plugin.openPage(InventoryType, size, player, title)` 替代旧的session.open
3. **组件使用**：
   - `slotComponent(x, y)` 替代直接设置物品
   - `paginatedComponent()` 用于分页显示
   - `render {}` 块定义物品渲染逻辑
   - `onLeftClick {}` 处理点击事件
4. **物品创建**：
   - `item(Material) {}` 创建基础物品
   - `skull(player, name, lore)` 创建头颅
   - `urlSkull(hash)` 创建自定义材质头颅
   - `lore(listOf(...))` 设置物品描述
5. **导航管理**：
   - `back()` 返回上一页
   - `close()` 关闭当前页面
   - `show()` 刷新当前页面

#### 功能对应关系
- **防抖处理**：旧系统的debounce → 新系统内置防重复点击
- **分页显示**：旧系统的pageGui → 新系统的paginatedComponent
- **会话管理**：旧系统的PlayerGuiSession → 新系统的自动会话栈管理
- **物品渲染**：旧系统的ItemBuilder → 新系统的item {} DSL

This architecture provides enterprise-grade plugin development with modern Kotlin practices, comprehensive resource management, modular design, and modern GUI systems for maintainable, high-performance Minecraft plugins.