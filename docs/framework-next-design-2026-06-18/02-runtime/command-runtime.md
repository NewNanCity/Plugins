# CommandRuntime

## 定位

CommandRuntime 是声明式命令、权限、参数、补全、帮助、错误渲染、执行 affinity 和静态插件元数据的深 Module。

它不应回到传统 Bukkit 模式：`plugin.yml` 写 root command，另写 `CommandExecutor`，另写 tab completer，另写 permission/help/i18n。命令是一条完整链路：

```text
代码声明
-> command descriptor
-> build-time plugin.yml / paper-plugin.yml
-> runtime command tree
-> parser / suggestion / permission / help
-> TaskRuntime / EventRuntime / GuiRuntime
-> diagnostics
```

当前仓库实际主要使用 Incendo Cloud annotations；Aikar ACF 和 CommandAPI 是体验与能力参照，不是业务 Interface 的事实源。

## 当前压力

现有代码已经体现：

- `LegacyPaperCommandManager`
- `AnnotationParser`
- `@Command`
- `@CommandDescription`
- `@Permission`
- `@Argument`
- `@Default`
- `@Suggestions`
- `MinecraftHelp`
- `ExecutionCoordinator.asyncCoordinator()`
- `RichDescription` 与 i18n key 映射

当前问题：

- `modules/core` 还有 CommandAPI 风格 registry，而多数插件使用 Cloud annotations，存在实现漂移。
- runtime command annotation 无法自动保证静态 yml 元数据一致。
- async coordinator 不能表达 handler 内 Bukkit API 的正确 affinity。
- permission、usage、help、i18n 描述缺少同一事实源。
- parser failure、permission denied、async failure 等错误渲染分散。

## Interface

调用者需要理解的核心概念：

- `CommandDescriptor`: 命令树和静态元数据的事实源。
- `CommandHandler`: 声明式 handler。
- `ArgumentParser<T>`: 参数解析和错误模型。
- `SuggestionProvider<T>`: 补全和可见性过滤。
- `CommandExecutionContext`: sender、owner、affinity、locale、diagnostic tags。
- `CommandResult`: 成功、拒绝、失败、异步挂起等结构化结果。

框架注解形态示例：

```kotlin
@PluginCommand("area create <name>")
@CommandDescriptionKey(Keys.Command.AreaCreate.Description)
@PermissionNode("newnan.area.create")
@CommandAffinity(Affinity.Main)
fun createArea(ctx: CommandExecutionContext, name: AreaName) {
    ctx.events.scope("area-create", owner = ctx.owner) {
        awaitCorners(name)
    }
}
```

Cloud Adapter 形态示例：

```kotlin
@Command("area create <name>")
@CommandDescription("commands.area.create.description")
@Permission("newnan.area.create")
@CommandAffinity(Affinity.Main)
fun createArea(sender: Player, @Argument("name") name: AreaName) {
    // framework adapter wraps this in CommandExecutionContext
}
```

Interface 不变量：

- 每个 root command 必须有 descriptor。
- 每个 permission node 必须有默认值和描述 key。
- handler、parser、suggestion provider 必须声明 affinity 或由框架推导并校验。
- command execution 必须有 owner。
- parser failure、permission denied、player-only、timeout、internal failure 都必须可渲染、可诊断。
- 静态 yml 是 build-time 产物，不在运行时生成。

## Command Descriptor

Descriptor 是 build-time 和 runtime 的共享事实源：

```kotlin
data class CommandDescriptor(
    val root: String,
    val aliases: List<String>,
    val descriptionKey: MessageKey,
    val permission: PermissionNode?,
    val children: List<CommandNodeDescriptor>,
    val source: SourceRef
)

data class PermissionDescriptor(
    val node: PermissionNode,
    val default: PermissionDefault,
    val descriptionKey: MessageKey
)
```

Build-time 生成：

```text
Kotlin source
-> KSP / annotation processor / Gradle descriptor collector
-> command descriptor json
-> plugin spec merge
-> paper-plugin.yml / plugin.yml
-> verification task
```

静态 yml 只需要 root command 和 permission 元数据；完整 subcommand tree 由 runtime command adapter 注册和管理。

## Argument Parser

参数不是字符串。CommandRuntime 应提供 parser catalog：

- `Player`
- `OfflinePlayer`
- `World`
- `Location`
- `Material`
- `Entity`
- `UUID`
- `Duration`
- `Money`
- domain id: station、rail area、book id、commodity name 等。

```kotlin
ctx.commands.parser<StationId>("station") {
    affinity(Affinity.AsyncCpu)
    cache(ttl = 1.seconds, key = "station-parse")
    parse { input, ctx -> stationStorage.findByNameOrId(input, visibleTo = ctx.sender) }
    error { input -> Keys.Command.Errors.UnknownStation with "input" to input }
}
```

Parser Interface 包含：

- input syntax。
- validation error。
- suggestion relation。
- permission-aware filtering。
- affinity。
- cache policy。
- failure report。

## Suggestions

SuggestionProvider 可能比 handler 更容易成为性能问题，因为它在用户输入过程中高频触发。

要求：

- suggestion provider 必须声明 affinity。
- 默认有 timeout 和 result limit。
- 可按 sender、permission、locale、input prefix 做过滤。
- 高频 provider 应支持 short ttl cache。
- failure 不应刷屏，统一进入 diagnostics。

```kotlin
ctx.commands.suggestions("online-players") {
    affinity(Affinity.Main)
    limit(20)
    provide { ctx, input ->
        server.onlinePlayers
            .asSequence()
            .filter { it.name.startsWith(input, ignoreCase = true) }
            .filter { ctx.sender.canSee(it) }
            .map { it.name }
            .toList()
    }
}
```

## Execution Bridge

命令执行是主动入口，应接入 TaskRuntime：

```kotlin
@Command("book search <keyword>")
@CommandAffinity(Affinity.AsyncIo)
fun search(ctx: CommandExecutionContext, keyword: String) {
    ctx.tasks.chain("book-search", owner = ctx.owner) {
        io("query") { books.search(keyword) }
        entity(ctx.player(), "open-gui") { result -> ctx.gui.open(ctx.player(), BookPage(result)) }
        onFailure { error -> ctx.replyError(error) }
    }
}
```

关键规则：

- 快速权限/参数校验可同步。
- I/O、网络、数据库进入 AsyncIo。
- Bukkit player/world/inventory 操作进入 Main/Entity/Region。
- command owner close 取消派生任务和临时 event scope。
- 异步失败通过 TextRuntime 渲染。

## Event 和 GUI 协作

命令经常开启局部 EventScope：

- `/area create` 等待点击两个角点。
- `/book import` 等待聊天输入。
- `/rail select` 等待玩家点击或乘车。
- `/gui open` 后监听 close。

命令也经常打开 GUI：

```kotlin
@Command("shop")
@CommandAffinity(Affinity.Entity)
fun openShop(ctx: CommandExecutionContext) {
    ctx.gui.open(ctx.player(), ShopHomePage())
}
```

CommandRuntime 不自己实现 event/gui 行为，只负责创建 command owner，并把上下文交给 EventRuntime 和 GuiRuntime。

## Error Renderer

CommandRuntime 统一处理这些错误：

- permission denied。
- unknown command/subcommand。
- parser failure。
- suggestion failure。
- player-only / console-only / command-block-only。
- business validation failure。
- task timeout。
- owner closed。
- internal exception。

错误渲染规则：

- 每个错误都有 MessageKey。
- 每个错误都有 structured diagnostic。
- 对用户隐藏内部 stack。
- 对管理员 diagnostics 保留 cause summary 和 source。
- 高频错误做 rate limit。

```kotlin
sealed interface CommandFailure {
    data class PermissionDenied(val node: PermissionNode) : CommandFailure
    data class ParseFailed(val argument: String, val reason: MessageKey) : CommandFailure
    data class HandlerFailed(val error: Throwable) : CommandFailure
    data class TimedOut(val timeout: Duration) : CommandFailure
}
```

## Implementation

CommandRuntime 内部隐藏：

- Cloud command manager 初始化。
- Cloud annotations parsing。
- CommandAPI/Brigadier Adapter 差异。
- descriptor 收集与运行时校验。
- permission catalog。
- parser/suggestion registry。
- help renderer。
- exception handler。
- execution coordinator。
- command owner 创建和关闭。
- diagnostics report。

业务 Module 不直接依赖 Cloud manager 生命周期，也不自己比较 plugin.yml。

## Seam 和 Adapter

外部 Seam：

- `CommandRuntime` Interface -> Cloud/CommandAPI/Bukkit command registration。
- `CommandDescriptor` -> build-time yml generation。
- `ArgumentParser` -> Cloud parser / CommandAPI argument。
- `SuggestionProvider` -> Cloud suggestion。

Adapter：

- `CloudCommandAdapter`: 当前主要 Adapter。
- `CommandApiAdapter`: 只在需要保留旧 BaseCommandRegistry 能力时接入。
- `BukkitCommandAdapter`: 最低层 fallback，不作为业务 Interface。
- `DescriptorGenerationAdapter`: KSP/annotation processor/Gradle source collector。
- `TestCommandAdapter`: fake command dispatcher。

## Diagnostics

Command diagnostics 应包含：

- registered root commands。
- descriptors 和 packaged yml 一致性。
- permission nodes。
- parser registry。
- suggestion provider latency。
- command execution count。
- failures by kind。
- active command scopes。
- last handler exception。

示例：

```text
command /book search
owner=module:external-book/commands
executions=842 failures=3 p95=31ms
slow suggestion provider=book-title max=82ms cacheHit=74%
```

## Harness

`CommandHarness` 需要覆盖：

- descriptor 生成包含 root command、aliases、permission、description key。
- packaged yml 与 descriptor 一致。
- permission denied 渲染正确 MessageKey。
- parser failure 不进入 handler。
- suggestion timeout 进入 diagnostics。
- handler affinity 正确桥接 TaskRuntime。
- command owner close 取消派生 task/event scope。
- player-only command 拒绝 console。
- async handler failure 渲染给 sender 并记录 diagnostic。

## 开放问题

1. 保留 Cloud 原生注解作为主 Interface，还是定义框架注解再适配 Cloud。
2. descriptor 生成采用 KSP、annotation processor，还是 Gradle source collector。
3. `plugin.yml` 和 `paper-plugin.yml` 是否都生成，或只生成目标平台文件。
4. permission description key 是否必须强制存在。
5. suggestion provider 默认 timeout 和 result limit 取值。
