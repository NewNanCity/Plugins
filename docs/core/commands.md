# 🎮 Cloud 命令系统最佳实践

## CommandExample.kt 权威模板

以下是完整的 CommandExample.kt 内容，这是所有命令系统实现的权威标准：

```kotlin
import city.newnan.myplugin.MyPlugin
import city.newnan.myplugin.i18n.LanguageKeys
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotation.specifier.Greedy
import org.incendo.cloud.annotation.specifier.Quoted
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Default
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.bukkit.CloudBukkitCapabilities
import org.incendo.cloud.bukkit.annotation.specifier.AllowEmptySelection
import org.incendo.cloud.bukkit.annotation.specifier.DefaultNamespace
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.description.Description
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.minecraft.extras.MinecraftHelp
import org.incendo.cloud.minecraft.extras.RichDescription
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.type.Either
import java.util.concurrent.CompletableFuture

// 需要：
// implementation(Dependencies.Optional.Command.cloudPaper)
// implementation(Dependencies.Optional.Command.cloudMinecraftExtras)
// implementation(Dependencies.Optional.Command.cloudAnnotations)
// 形成结构：
// commands  每个命令要单独写一个文件，其中有一个处理方法，该方法可能有很多Command别名，以及其他配套服务于这个处理方法的方法
// ├── CommandRegistry.kt 本文件
// ├── admin
// │   ├── AdminCommand1.kt // 替换为对应的名字
// │   └── ...
// ├── user
// │   ├── UserCommand1.kt // 替换为对应的名字
// │   └── ...
// └── ...
// 之前的BaseCommand、CommandMessage、CommandPermissions、CommandValidator文件都不要，因为功能冗余
// 在 MyPlugin 主类的 onPluginEnable 或类似方法中创建该 CommandRegistry 即可注册所有指令
// 指令和参数描述必须来自于LanguageKey

// ====== 从这里开始是固定模板 ======
class CommandRegistry(val plugin: MyPlugin) {
    // 创建命令管理器
    val commandManager = LegacyPaperCommandManager.createNative(
        plugin,
        ExecutionCoordinator.asyncCoordinator()
    ).also {
        if (it.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            it.registerBrigadier()
        } else if (it.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
          it.registerAsynchronousCompletions();
        }
    }

    // 创建注解解析器
    val commandAnnotationParser = AnnotationParser(commandManager, CommandSender::class.java).also {
        // i18n 映射，并支持 Adventure 组件
        // 相关键：
        // LanguageKeys
        // ├── Commands
        // │   ├── Common 一些通用的信息
        // │   │   ├── NoPermission 无权限执行
        // │   │   └── UUIDInvalid UUID无效
        // │   ├── Help
        // │   │   ├── Description 指令的描述
        // │   │   └── Query 查询参数的描述
        // │   └── Foo
        // │       ├── Description 指令的描述
        // │       ├── String1 参数的描述
        // │       └── String2 参数的描述
        // └── ... 其他模块的语言键
        it.descriptionMapper { it -> RichDescription.of(plugin.messager.sprintf(it)) }
    }

    val help: MinecraftHelp<CommandSender>

    init {
        // 解析注解
        // 最佳实践应当是将指令方法按功能划分到不同的类/单例中
        // 除了帮助指令之外+主指令，其他指令都应当在各自的类中实现
        // 这样可以避免一个类过于庞大，难以维护
        listOf(
            // 帮助命令+主指令
            this,
            // 用户命令
            HomepageComman(plugin),
            QueryCommand(plugin),
            ...
            // 管理员命令
            ReloadCommand(plugin),
            ManageCommand(plugin),
            ...
            // 其他类型的命令
            ...
        ).forEach { commandAnnotationParser.parse(it) }

        // 生成帮助指令 /myplugin 是插件的指令前缀
        help = MinecraftHelp.createNative("/myplugin help", commandManager)
    }

    // 为help的查询提供补全
    @Suggestions("help-query")
    fun helpQuerySuggestions(ctx: CommandContext<CommandSender>, input: String) = CompletableFuture.supplyAsync {
        commandManager.createHelpHandler()
            .queryRootIndex(ctx.sender())
            .entries()
            .map { Suggestion.suggestion(it.syntax()) }
            .toList()
    }

    // 帮助指令
    @Command("myplugin|pluginalias help [help-query]")
    @CommandDescription(LanguageKeys.Commands.Help.DESCRIPTION)
    fun helpCommand(sender: CommandSender, @Greedy @Default("1") @Argument(value = "help-query", description = LanguageKeys.Commands.Help.Query) query: String) {
        help.queryCommands(query, sender) // 所有插件的帮助命令都这样写
    }

    // 主命令
    @Command("myplugin|pluginalias")
    @CommandDescription(LanguageKeys.Commands.Main.DESCRIPTION)
    fun mainCommand(sender: CommandSender) {
        helpCommand(sender, "") // 不同插件的主命令行为可能不同，比如展示帮助、打印信息或者打开某个gui都有可能
    }
}

// 假设在某个具体的指令脚本中，比如 admin/ManagerCommand.kt
class ManagerCommand(val plugin: MyPlugin) {
    // 包含一个指令，以及其他配套服务于这个指令的方法
    @Command("myplugin|pluginalas manage <target>")
    @CommandDescription(LanguageKeys.Commands.Manage.DESCRIPTION)
    fun manageCommand(sender: CommandSender, @Argument(value = "target", description = LanguageKeys.Commands.Manage.Target) target: String) {
        // 指令逻辑
    }
    // 以及其他配套服务于这个指令的方法
    // 比如 managePlayer、dateFormat 等
    ...
}
```

## Cheat Sheet

这里浓缩出所有常用的知识，包括注解的用法、支持的类型和写法等，更全面的文档请参考 context7的/incendo/cloud-docs 相关文档或浏览 https://github.com/incendo/cloud-docs 或 https://cloud.incendo.org 。

```kotlin
// 一个常规的例子，@Command定义了一个指令的语法，会自动解析、补全、校验并提供帮助提示
// <xxx> 是指必填参数
// [xxx] 是可选参数
// 没有<>[]的是字面量，字面量可以使用`|`来定义别名
@Command("myplugin test|test-alias1|test-alias2 <number> [string1]")
@Command("test <number> [string1]") // 一个方法可以定义多个不同的指令
@CommandDescription(LanguageKeys.Commands.Test.Description) // 指令的描述，会显示在 /help 中
@Permission("myplugin.test") // 执行该指令需要的权限
fun deathcost(
    // 方法的参数顺序不重要，会自动映射指令参数到方法对应名称的参数，如果方法参数名称和指令参数名不一致，就使用@Argument来指定
    // 指令参数的类型由方法参数的类型决定，下面列出支持的类型
    // CommandSender CommandContext 是特殊参数，会自动注入，和指令的定义无关
    sender: CommandSender,
    number: Int, // 因为number就是参数名，且不需要额外的信息，所以@Argument是可选的 —— 但是不建议这样，至少要添加description和suggestions来提升用户体验
    // 对于可选参数，@Default设置默认值，如果没有默认值，那么其类型应当为可空类型如String?
    @Argument(value = "string1", description = LanguageKeys.Commands.Test.String1 /* 描述(可选) */, suggestions = "自定义补全提示器(可选)", parserName = "自定义解析器名称(可选)") @Default(value = "默认值") string: String
    // 默认值的另一种用法是指定 name ，引用另一个参数的值比如 @Default(name = "string2")
) {
    sender.sendMessage("DeathCost")
}

// 警告：@Default不能够设置空串，否则会出现无限递归问题

// 支持的参数类型举例
@Command("...")
fun typesCommand(
    str1: String,
    @Greedy /* 匹配后面所有的输入为一个字符串参数 */ str2: String,
    @Quoted /* 匹配一个对单/双引号括起来的字符串 */ str3: String,
    strs: Array<String>,
    ch: Char,
    byte: Byte,
    short: Short,
    int: Int,
    long: Long,
    float: Float,
    double: Double,
    bool: Boolean, // 接收 true false yes no on off
    enum: org.bukkit.GameMode, // Enum，按照枚举名，不区分大小写
    uuid: java.util.UUID, // 带短横线的格式
    @DefaultNamespace("minecraft"/*在用户没有指定命名空间时的默认命名空间*/) namespacedKey: org.bukkit.NamespacedKey, // 比如 minecraft:diamond
    enchantment: org.bukkit.enchantments.Enchantment, // 附魔
    itemStack: org.incendo.cloud.bukkit.data.ProtoItemStack, // 物品
    itemStackPredicate: org.incendo.cloud.bukkit.data.ItemStackPredicate, // 物品过滤器
    blockPredicate: org.incendo.cloud.bukkit.data.BlockPredicate, // 方块过滤器
    @AllowEmptySelection /* 允许命令发送者使用选择器执行一个选择零实体的命令 */ singleEntitySelector: org.incendo.cloud.bukkit.data.SingleEntitySelector, // 单个实体选择器
    singlePlayerSelector: org.incendo.cloud.bukkit.data.SinglePlayerSelector, // 单个玩家选择器
    multipleEntitySelector: org.incendo.cloud.bukkit.data.MultipleEntitySelector, // 多个实体选择器
    multiplePlayerSelector: org.incendo.cloud.bukkit.data.MultiplePlayerSelector, // 多个玩家选择器
    location: org.bukkit.Location, // 位置
    location2d: org.incendo.cloud.bukkit.parser.location.Location2D, // 2D 位置
    material: org.bukkit.Material, // 材料
    offlinePlayer: org.bukkit.OfflinePlayer, // 离线玩家
    player: org.bukkit.entity.Player, // 在线玩家
    world: org.bukkit.World, // 世界
    ) {}

// 命令方法可以返回 CompletableFuture<T> ，在这种情况下，执行协调器将等待返回的未来对象完成
@Command("myplugin async")
fun asyncCommand(sender: CommandSender): CompletableFuture<Int> {
    return CompletableFuture.supplyAsync { 1 }
}

// 可以使用Either来表示一个参数可能有多种类型
@Command("myplugin kill <player>")
fun eitherCommand(player: Either<Player, SinglePlayerSelector>) {
    val player = player.mapEither(
        { it },
        { it.single() }
    )
    // ...
}

// 还可以为指令添加像命令行那样的Flag
@Command("myplugin foo") // Flag不要出现在Command中
fun flagCommand(
    sender: CommandSender,
    @Flag(value = "enable-xxx", aliases = ["e"]) flag: Boolean, // --enable-xxx 或 -e，对于布尔flag，-a -b -c 等价于 -abc
    @Flag("name", aliases = ["n"]) name: String?, // --name <name> 或 -n <name>
    @Flag("set", repeatable = true) set: List<String>?, // --set <value> 可以重复出现
) {}

// 高级指令检查
@Command("myplugin bar")
@Permission(value = ["myplugin.bar1", "myplugin.bar2"], mode = Permission.Mode.ALL_OF) // 或 ANY_OF
fun advancedPermission() {}
```

还有更多高级功能，请参考文档，这里不做展开，比如：

- Default 默认值提供方法
- Parsers   解析器
- Suggestion Providers   建议提供者
- Exception Handlers   异常处理器
- Injections   注入
- Customization   自定义
- Builder Decorators   构建器装饰器
- Builder Modifiers   构建器修饰符
- Annotation Mappers   注释映射器
- Pre-processor mappers   预处理器映射器
- Annotation Processing   注解处理
- Command Containers   命令容器

## 核心原则

### 1. 固定模板不可修改

CommandRegistry 中的以下部分是**固定模板**，不允许修改：
- 命令管理器创建和配置
- 注解解析器创建和 i18n 映射
- help 查询补全方法
- 帮助指令的实现（只调用 `help.queryCommands(query, sender)`）

### 2. 目录结构规范

```
commands/
├── CommandRegistry.kt          # 主注册器（只包含help指令）
├── admin/                      # 管理员命令
│   ├── AdminCommand.kt
│   ├── GiveCommand.kt
│   └── ReloadCommand.kt
└── user/                       # 用户命令
    ├── ImportCommand.kt
    ├── ExportCommand.kt
    └── GuiCommand.kt
```

### 3. LanguageKeys 规范

**重要：所有Command的LanguageKey都必须使用大写+下划线格式！**

#### 层次结构设计

LanguageKeys应该按照功能层次进行组织：

```kotlin
object LanguageKeys {
    // ==================== 核心系统层 (Core System Layer) ====================
    object Core {
        object Error {
            const val PLAYER_ONLY = "<%core.error.player_only%>"
            const val INVALID_UUID = "<%core.error.invalid_uuid%>"
            const val OPERATION_FAILED = "<%core.error.operation_failed%>"
        }

        object Common {
            const val INVALID_BOOK_ID = "<%core.common.invalid_book_id%>"
        }
    }

    // ==================== 命令系统层 (Command System Layer) ====================
    object Commands {
        object Common {
            const val LOG_LIBRARIAN_NOT_AVAILABLE = "<%commands.common.log_librarian_not_available%>"
            const val LIBRARIAN_NOT_AVAILABLE = "<%commands.common.librarian_not_available%>"
        }

        object Help {
            const val DESCRIPTION = "<%commands.help.description%>"
            const val QUERY = "<%commands.help.query%>"
        }

        object Give {
            const val DESCRIPTION = "<%commands.give.description%>"
            const val PLAYER = "<%commands.give.player%>"
            const val BOOK_ID = "<%commands.give.book_id%>"
            const val SUCCESS = "<%commands.give.success%>"
            const val FAILED = "<%commands.give.failed%>"
            const val LOG_SUCCESS = "<%commands.give.log_success%>"
            const val LOG_FAILED = "<%commands.give.log_failed%>"
        }
    }
}
```

#### 使用规范

- **Core.Error**: 通用错误信息，如PLAYER_ONLY、INVALID_UUID等
- **Commands.Common**: 命令系统通用信息，如资源不可用等
- **Commands.具体命令**: 特定命令的所有相关文本

## 最佳实践示例

### 1. 简单命令示例 - AdminCommand

适用于只需要打开GUI或执行简单操作的命令：

```kotlin
class AdminCommand(private val plugin: ExternalBookPlugin) {

    @Command("externalbook|book admin")
    @CommandDescription(LanguageKeys.Commands.Admin.DESCRIPTION)
    @Permission("externalbook.admin")
    fun adminCommand(sender: CommandSender) {
        // 1. 玩家检查
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        // 2. 业务逻辑执行
        try {
            plugin.messager.printf(sender, LanguageKeys.Commands.Admin.GUI_OPENING)
            openAuthorListGui(plugin, sender) { target ->
                openPlayerBooksGui(plugin, sender, target)
            }
        } catch (e: Exception) {
            plugin.logger.error(LanguageKeys.Commands.Admin.LOG_FAILED, e, sender.name)
            plugin.messager.printf(sender, LanguageKeys.Commands.Admin.GUI_FAILED)
        }
    }
}
```

### 2. 复杂异步命令示例 - OpenCommand

适用于需要数据库查询或其他异步操作的命令：

```kotlin
class OpenCommand(private val plugin: ExternalBookPlugin) {

    @Command("externalbook|book open <player> <ulid>")
    @CommandDescription(LanguageKeys.Commands.Open.DESCRIPTION)
    @Permission("externalbook.open")
    fun openCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.Open.PLAYER) target: Player,
        @Argument(value = "ulid", description = LanguageKeys.Commands.Open.ULID) ulidString: String
    ) {
        // 1. 基础验证
        val librarian = plugin.librarian ?: run {
            plugin.logger.error(LanguageKeys.Commands.Common.LOG_LIBRARIAN_NOT_AVAILABLE)
            plugin.messager.printf(sender, LanguageKeys.Commands.Common.LIBRARIAN_NOT_AVAILABLE)
            return@openCommand
        }

        // 2. 参数验证
        val bookId = try {
            Ulid.from(ulidString)
        } catch (e: IllegalArgumentException) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.INVALID_UUID)
            return@openCommand
        }

        // 3. 异步处理（数据库操作）
        plugin.runAsync<Unit> {
            try {
                val book = librarian[bookId] ?: run {
                    plugin.runSync<Unit> {
                        plugin.messager.printf(sender, LanguageKeys.Commands.Open.BOOK_NOT_FOUND)
                    }
                    return@runAsync
                }

                // 4. 回到主线程执行Bukkit API操作
                plugin.runSync {
                    book.readBook(target)
                    plugin.messager.printf(sender, LanguageKeys.Commands.Open.SUCCESS, target.name, book.title)
                    plugin.logger.info(LanguageKeys.Commands.Open.LOG_SUCCESS, sender.name, target.name, book.title)
                }

            } catch (e: Exception) {
                plugin.logger.error(LanguageKeys.Commands.Open.LOG_FAILED, e, sender.name, target.name)
                plugin.runSync<Unit> {
                    plugin.messager.printf(sender, LanguageKeys.Commands.Open.FAILED)
                }
            }
        }
    }
}
```

## 命令实现模式

### 1. 玩家检查模式

所有需要玩家执行的命令都应该使用统一的检查模式：

```kotlin
if (sender !is Player) {
    plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
    return
}
```

### 2. 资源检查模式

对于依赖外部资源（如数据库连接）的命令：

```kotlin
val librarian = plugin.librarian ?: run {
    plugin.logger.error(LanguageKeys.Commands.Common.LOG_LIBRARIAN_NOT_AVAILABLE)
    plugin.messager.printf(sender, LanguageKeys.Commands.Common.LIBRARIAN_NOT_AVAILABLE)
    return@commandFunction
}
```

### 3. 异步处理模式

**重要：数据库操作必须异步，Bukkit API操作必须同步**

```kotlin
// 异步执行数据库查询
plugin.runAsync<Unit> {
    try {
        // 数据库操作
        val result = database.query(...)

        // 回到主线程执行Bukkit API
        plugin.runSync {
            // Bukkit API操作
            player.sendMessage(...)
            player.inventory.addItem(...)
        }
    } catch (e: Exception) {
        // 异步异常处理
        plugin.logger.error(LanguageKeys.Commands.XXX.LOG_FAILED, e, ...)
        plugin.runSync<Unit> {
            plugin.messager.printf(sender, LanguageKeys.Commands.XXX.FAILED)
        }
    }
}
```

### 4. 错误处理模式

每个命令都应该有完整的错误处理：

```kotlin
try {
    // 主要逻辑
} catch (e: IllegalArgumentException) {
    // 参数错误
    plugin.messager.printf(sender, LanguageKeys.Core.Error.INVALID_ARGUMENT)
} catch (e: Exception) {
    // 通用错误
    plugin.logger.error(LanguageKeys.Commands.XXX.LOG_FAILED, e, ...)
    plugin.messager.printf(sender, LanguageKeys.Commands.XXX.FAILED)
}
```

### 5. 命令别名模式

支持多个别名的命令应该使用管道符分隔：

```kotlin
@Command("externalbook|book export|origin|edit")  // 主命令|别名 子命令|别名1|别名2
@Command("externalbook|book import|register")     // 主命令|别名 子命令|别名
```

## BaseModule集成模式

CommandRegistry应该作为BaseModule的一部分进行生命周期管理：

```kotlin
class MyPlugin : BasePlugin() {
    private lateinit var commandRegistry: CommandRegistry

    override fun onPluginEnable() {
        // 初始化命令注册器（不可重载）
        commandRegistry = CommandRegistry(this)

        // 调用重载方法
        reloadPlugin()
    }
}
```

## 迁移检查清单

### 基础结构
- [ ] 删除旧的 BaseCommand、CommandMessage、CommandPermissions、CommandValidator 文件
- [ ] 创建新的 CommandRegistry.kt（严格按照模板）
- [ ] 每个命令创建独立文件，按admin/user分类
- [ ] CommandRegistry在onPluginEnable中初始化（不可重载）

### LanguageKeys规范
- [ ] 所有Command的LanguageKey使用大写+下划线格式（DESCRIPTION, LOG_SUCCESS等）
- [ ] 所有文本使用 LanguageKeys，包括参数描述
- [ ] printf 和 log 都使用 LanguageKey
- [ ] 使用Core.Error.PLAYER_ONLY而不是Commands.Common.PLAYER_ONLY

### 命令实现模式
- [ ] 玩家检查使用统一模式：`if (sender !is Player)`
- [ ] 资源检查使用统一模式：`plugin.librarian ?: run { ... }`
- [ ] 数据库操作必须异步：`plugin.runAsync`
- [ ] Bukkit API操作必须同步：`plugin.runSync`
- [ ] 完整的错误处理：try-catch + 日志记录
- [ ] 命令别名使用管道符：`command|alias subcommand|alias1|alias2`

### 代码质量
- [ ] 保留原有逻辑和注释
- [ ] 每个命令都有完整的KDoc文档
- [ ] 异常处理包含具体的错误信息
- [ ] 日志记录包含足够的上下文信息
