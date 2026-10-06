# 统一框架设计

这份文档把前面几轮架构审查、隐含需求、性能设计、事件/命令/GUI 讨论和外部相关工作重新揉成一个整体方案。它是当前设计包的主入口：其他文档保留证据、对比和展开细节，本页描述一体化框架应长什么样。

## 设计定位

下一代 NewNanPlugins 框架不是 Bukkit/Paper 的薄 wrapper，也不是把一批第三方库拼在一起。它是一个面向 Minecraft 插件的 runtime framework。

它的核心任务是把插件开发中最容易出错、最重复、最难诊断的部分收进深 Module：

- 生命周期和资源归属。
- 任务调度和线程/区域归属。
- 事件作用域和事件风暴背压。
- GUI session/page/render/data。
- 声明式命令和静态插件元数据。
- 配置快照、消息目录和 reload report。
- 网络、持久化、缓存的运行时 policy。
- 第三方插件能力发现和降级。
- 诊断、测试 harness 和性能证据。

业务插件只表达业务意图：我要注册哪些模块、声明哪些命令、监听哪些事件、打开什么 GUI、读写哪些数据。框架负责把这些意图映射到 Bukkit/Paper/Folia 的安全执行模型。

## 核心原则

### 1. Owner 是所有长期资源的事实源

任何长期存在或可取消的东西都必须属于一个 owner：

- module。
- task。
- event subscription。
- GUI session/page/component。
- network request。
- http client。
- cache。
- persistence writer。
- external capability。

没有 owner 的资源不允许进入 runtime。owner 结束时，框架按资源树反向关闭，并生成 close report。

### 2. Affinity 比 sync/async 更真实

Minecraft 的执行域不只是同步和异步。框架 Interface 要表达：

- `Main`: 传统 Paper/Bukkit 主线程。
- `AsyncIo`: 文件、数据库、网络。
- `AsyncCpu`: 解析、排序、模板预编译。
- `Global`: Folia global scheduler 语义。
- `Region`: 位置或区块归属。
- `Entity`: 玩家或实体归属。

Paper 下 `Region/Entity` 可以由 Adapter 映射回主线程；Folia 下映射到对应 scheduler。业务 Interface 不应该依赖“唯一主线程”这个假设。

### 3. Policy 声明在入口，不散落在调用点

timeout、retry、rate limit、tick budget、cache ttl、batch flush、event backpressure 都是 Interface 的一部分。业务代码不应在每个调用点重复写这些策略，也不应靠默认无限队列或裸异常处理。

### 4. Snapshot 和 Report 替代可变全局状态与日志猜测

配置、消息目录、外部 capability、cache entry、reload result、task outcome 都应有结构化对象：

- snapshot 表示当前生效事实。
- report 表示一次操作做了什么、失败在哪里、是否保留旧状态。
- diagnostics 把 report 串成管理员和开发者能理解的证据。

### 5. Adapter 可替换，Interface 不跟库漂移

Cloud、Ktor、OkHttp、HikariCP、Exposed、Ktorm、Caffeine、Configurate、Okaeri、MockBukkit、Vault、ProtocolLib、WorldEdit 都可以成为 Adapter 或参考，但不能倒推业务 Interface。

核心 Interface 围绕 Minecraft runtime facts：owner、scope、affinity、policy、snapshot、report、diagnostics。

## 总体结构

```text
Build time
└── FrameworkSpecCompiler
    ├── PluginSpec descriptors
    ├── Command descriptors
    ├── Permission descriptors
    ├── Config schema descriptors
    └── plugin.yml / paper-plugin.yml

Runtime
└── PluginRuntime
    ├── LifecycleRuntime
    ├── ModuleRuntime
    ├── TaskRuntime
    ├── EventRuntime
    ├── CommandRuntime
    ├── GuiRuntime
    ├── ConfigRuntime
    ├── TextRuntime
    ├── NetworkRuntime
    ├── PersistenceRuntime
    ├── CacheRuntime
    ├── IntegrationRuntime
    └── DiagnosticsRuntime
```

这里有两个关键 Seam：

- build-time seam：从代码声明生成静态元数据和校验报告。
- runtime seam：从业务模块意图映射到 Bukkit/Paper/Folia Adapter。

这两个 Seam 共享同一组 descriptor，但职责不同。构建期生成静态文件，运行期托管资源和行为。

## 插件声明模型

插件入口分成声明和安装两部分：

```kotlin
abstract class NewNanPlugin : JavaPlugin() {
    protected abstract fun describe(spec: PluginSpecBuilder.() -> Unit)
    protected abstract fun install(runtime: PluginRuntime)
}
```

`describe` 是构建期和启动期都能读取的事实源：

```kotlin
pluginSpec {
    id("tpa")
    name("TPA")
    mainClass("city.newnan.tpa.TPAPlugin")
    apiVersion("1.21")

    config<TpaConfig>("config.yml") {
        template("config.yml")
        format(ConfigFormat.Yaml)
        reloadable(true)
    }

    messages {
        defaultLocale("zh_CN")
        catalog("lang/{locale}.yml")
    }

    command("tpa") {
        permission("newnan.tpa.use")
        descriptionKey(Keys.Command.TPA.DESCRIPTION)
    }

    softDepend("Vault")
}
```

`install` 只安装 Module，不启动重任务：

```kotlin
override fun install(runtime: PluginRuntime) {
    runtime.modules.install("sessions") { TpaSessionModule(it) }
    runtime.modules.install("commands") { TpaCommandModule(it) }
    runtime.modules.install("gui") { TpaGuiModule(it) }
}
```

设计要求：

- `plugin.yml` / `paper-plugin.yml` 由 build-time compiler 生成。
- 运行期只校验 jar 内元数据和 descriptor 是否一致。
- 命令、权限、依赖、config schema、message catalog 都来自声明。
- 声明是 Interface；Gradle/KSP/annotation processor 是 Implementation 选择。

## LifecycleRuntime

LifecycleRuntime 负责插件和 Module 的状态机：

```text
Discovered
-> Described
-> Installed
-> Starting
-> Running
-> Reloading
-> Running
-> Closing
-> Closed
```

Module Interface：

```kotlin
interface PluginModule {
    val id: ModuleId
    fun start(ctx: ModuleContext): StartReport = StartReport.ok()
    fun reload(ctx: ReloadContext): ReloadReport = ReloadReport.noop()
    fun close(ctx: CloseContext): CloseReport = CloseReport.ok()
}
```

重要规则：

- 构造函数只保存依赖，不启动 task，不注册 listener，不打开 client。
- `start` 只在 ConfigRuntime、TextRuntime、IntegrationRuntime 的初始 snapshot 可用后执行。
- `reload` 接收新 snapshot，失败时保留旧 snapshot。
- `close` 幂等，且由 owner tree 统一处理子资源。
- Module 失败要进入 diagnostics，不能只打日志。

这会替代“构造时自动 init”和“首次 reload 顺便启动模块”的隐式模型。

## ResourceOwner Tree

所有 runtime resource 都挂到 owner tree：

```text
plugin:tpa
├── module:sessions
│   ├── event:PlayerQuitEvent/session-cleanup
│   ├── task:session-expiry
│   └── cache:tpa-session-index
├── module:commands
│   └── command-scope:/tpa
└── module:gui
    └── player:{uuid}
        └── gui-session:default
            └── page:tpa-request-list
                ├── task:load-page-data
                └── event:InventoryClickEvent/page-router
```

ResourceOwner Interface：

```kotlin
interface ResourceOwner : AutoCloseable {
    val id: OwnerId
    val parent: ResourceOwner?
    val tags: OwnerTags
    fun child(id: OwnerId, tags: OwnerTags = OwnerTags.empty()): ResourceOwner
    fun close(reason: CloseReason): CloseReport
    fun diagnostics(): OwnerDiagnostics
}
```

Leverage：

- player quit 能关闭 player scope 下的 GUI、等待事件、任务和网络请求。
- module close 能关闭所有 listener、cache、batch writer 和 client。
- diagnostics 能列出未关闭资源和创建来源。

Locality：

- 资源释放问题集中在 LifecycleRuntime 和 owner implementation。
- 业务 Module 不再到处保存 BukkitTask、Listener、Job、HttpClient 引用。

## TaskRuntime

TaskRuntime 是框架最核心的深 Module。它隐藏 Bukkit/Paper/Folia scheduler、coroutine、executor、timeout、dependency、cancellation、diagnostics。

Interface 示例：

```kotlin
ctx.tasks.chain("book-search", owner = ctx.owner) {
    io("query") { books.search(keyword) }
    main("open-gui") { result -> ctx.gui.open(player, BookPage(result)) }
    onFailure { error -> ctx.text.send(player, Keys.Errors.BookSearchFailed, error) }
}
```

基础类型：

```kotlin
sealed interface Affinity {
    data object Main : Affinity
    data object AsyncIo : Affinity
    data object AsyncCpu : Affinity
    data object Global : Affinity
    data class Region(val world: String, val chunkX: Int, val chunkZ: Int) : Affinity
    data class Entity(val uuid: UUID) : Affinity
}

sealed interface TaskOutcome<out T> {
    data class Success<T>(val value: T) : TaskOutcome<T>
    data class Failed(val error: Throwable) : TaskOutcome<Nothing>
    data class Cancelled(val reason: CancellationReason) : TaskOutcome<Nothing>
    data class TimedOut(val timeout: Duration) : TaskOutcome<Nothing>
    data class Rejected(val reason: BackpressureReason) : TaskOutcome<Nothing>
}
```

Policy：

- timeout。
- tick budget。
- max queue size。
- retry/backoff。
- cancellation reason。
- dependency failure behavior。

Diagnostics：

- active tasks。
- queued tasks。
- average/max duration。
- slow callbacks。
- cancelled/timed out/rejected count。
- owner path。

## EventRuntime

EventRuntime 负责 Bukkit/Paper event、domain event 和第三方 listener adapter。它不是 `registerEvents` 的语法糖，而是被动触发的 runtime。

Interface 示例：

```kotlin
ctx.events.on<PlayerInteractEvent>("select-first-corner") {
    owner(selectionScope)
    priority(EventPriority.HIGH)
    ignoreCancelled(true)
    affinity(Affinity.Main)
    filter { it.player.uniqueId == playerId && it.action.isLeftClick }
    once()
    handle { event -> selection.setFirst(event.clickedBlock?.location) }
}
```

EventScope 类型：

- plugin scope。
- module scope。
- player scope。
- GUI page scope。
- command scope。
- task scope。
- once/until/timeout scope。

Event 与 Task 协作：

```kotlin
ctx.events.on<PlayerJoinEvent>("load-profile") {
    handle { event ->
        ctx.tasks.chain("profile-load", owner = event.owner) {
            io("load") { profiles.load(event.player.uniqueId) }
            entity(event.player, "apply") { profile -> profile.applyTo(event.player) }
        }
    }
}
```

背压能力：

- `throttle`。
- `debounce`。
- `coalesceBy`。
- `dropIfBusy`。
- `queueLatest`。

这用于 `PlayerMoveEvent`、Inventory event、chat event、chunk/world event 等高频路径。

## CommandRuntime

CommandRuntime 把声明式命令、运行时命令树、权限、帮助、错误渲染和静态 yml metadata 串起来。

Interface 可以是框架注解，也可以是 Cloud annotation Adapter：

```kotlin
@Command("area create <name>")
@CommandDescriptionKey(Keys.Command.AreaCreate.Description)
@Permission("newnan.area.create")
@CommandAffinity(Affinity.Main)
fun createArea(sender: Player, name: AreaName) {
    ctx.commands.startInteraction(sender, "area-create") {
        awaitCorners(name)
    }
}
```

CommandRuntime 内部 Module：

- `CommandSpecRegistry`。
- `CloudCommandAdapter`。
- `CommandMetadataGenerator`。
- `PermissionCatalog`。
- `ArgumentParserCatalog`。
- `SuggestionRuntime`。
- `CommandExecutionBridge`。
- `CommandDiagnostics`。

要求：

- root command 和 permission descriptor 进入 build-time yml 生成。
- parser/suggestion/help/error renderer 都有 i18n key。
- handler、parser、suggestion provider 都声明 affinity。
- command 中启动 task、等待 event、打开 GUI 都继承 command owner。
- permission denied、parser failure、player-only、async timeout、internal error 都统一渲染和记录。

## GuiRuntime

GuiRuntime 是 UI runtime，不是 Inventory helper。

结构：

```text
GuiRuntime
├── SessionRegistry
├── PageRegistry
├── EventRouter
├── ComponentTree
├── RenderEngine
├── DataRuntime
├── ObjectProtection
└── GuiDiagnostics
```

业务 Interface 示例：

```kotlin
ctx.gui.open(player, chestGui(Keys.Gui.BookList, rows = 6)) {
    paginated("books") {
        slots(rect(x = 1, y = 1, width = 7, height = 4))
        data { page, size -> books.findPage(page, size) }
        item { book -> BookItemDescriptor(book) }
        onClick { book -> ctx.commands.execute(player, "book open ${book.id}") }
    }
}
```

渲染流程：

```text
state change
-> mark dirty component
-> fetch/compute render model off-thread
-> switch by affinity
-> materialize ItemStack
-> apply slot diff
-> record render report
```

要求：

- event routing 按 inventory/page/component owner，不依赖 global current manager。
- page stack、chat input、object protection、storage slot、pagination、scrollable 是一等能力。
- async data provider 必须有 generation id 和 cancellation token。
- close page 不应误关整个 scheduler。
- GUI title、button、lore 走 TextRuntime。
- render model 尽量轻量，不缓存长期可变 Bukkit 对象。

## ConfigRuntime

ConfigRuntime 以 typed snapshot 和 reload report 为核心。

```kotlin
data class ConfigSnapshot<T : Any>(
    val value: T,
    val version: ConfigVersion,
    val loadedAt: Instant,
    val report: ConfigLoadReport
)
```

职责：

- 从 PluginSpec 读取 config schema。
- 生成/校验默认配置文件。
- load/merge/validate/upgrade。
- 生成 `ConfigReloadReport`。
- 原子替换 snapshot。
- 按 config version 触发 cache invalidation。

Adapter：

- Bukkit/Paper YAML。
- Jackson。
- Configurate。
- Okaeri。

Interface 不暴露具体 parser。业务拿到的是不可变 snapshot 和 typed value。

## TextRuntime

TextRuntime 管 message key、catalog、locale、placeholder、MiniMessage/Legacy/plain renderer、audience adapter。

Interface 示例：

```kotlin
ctx.text.send(player, Keys.Tpa.RequestSent) {
    arg("target", target.name)
    localeFrom(player)
}
```

职责：

- 加载 message catalog snapshot。
- 校验 missing key、unused key、invalid MiniMessage。
- fallback chain。
- per-player locale 作为可选能力。
- parser/tag resolver/placeholder resolver 按 owner 注册。
- command feedback、GUI text、event message、admin diagnostics 共享 catalog。

避免：

- 业务散落 raw string key。
- runtime 到处猜 `Auto` 格式。
- 把 MiniMessage 字符串当持久业务状态。

## NetworkRuntime

NetworkRuntime 托管 named client 和 request policy。

Interface 示例：

```kotlin
val booksApi = ctx.network.client("books-api") {
    baseUrl(config.books.baseUrl)
    timeout(config.books.timeout)
    retry(config.books.retry)
    rateLimit(config.books.rateLimit)
    auth(ApiKey(config.books.apiKey))
    redactHeaders("Authorization", "X-API-Key")
}
```

职责：

- client lifecycle。
- token/credential cache。
- timeout、retry、rate limit、bulkhead、circuit breaker。
- per-client/per-host/per-owner concurrency。
- request owner 和 cancellation。
- streaming 大响应。
- request diagnostics。

Ktor/OkHttp/Resilience4j 是 Adapter/Implementation 候选。业务 Interface 只表达 HTTP 意图和 policy。

## PersistenceRuntime 和 CacheRuntime

PersistenceRuntime 不是 ORM。它负责 I/O 运行时约束：

- data source owner。
- transaction scope。
- repository executor。
- batch writer。
- flush-on-close。
- backpressure。
- failure report。
- metrics。

Write policy：

```kotlin
sealed interface WritePolicy {
    data object SyncCommit : WritePolicy
    data object WriteThrough : WritePolicy
    data class WriteBehind(val maxDelay: Duration, val maxBatchSize: Int) : WritePolicy
    data object DerivedCache : WritePolicy
}
```

CacheRuntime 不是 `Map` wrapper。每个 cache 声明：

- owner。
- key/value type。
- maximum size/weight。
- ttl/refresh。
- invalidation policy。
- loader affinity。
- value mutability。
- diagnostics。

HikariCP、Exposed、Ktorm、Caffeine 都只影响 Adapter 和 Implementation。框架 Interface 保持稳定。

## IntegrationRuntime

IntegrationRuntime 管外部插件能力：

```kotlin
val economy = ctx.integrations.capability<EconomyCapability>("vault-economy") {
    softDepend("Vault")
    required(false)
}
```

Capability 状态：

- `Available(provider, version)`。
- `Unavailable(reason)`。
- `Degraded(reason, fallback)`。
- `DisabledByConfig`。

适用对象：

- Vault economy/permission/chat。
- ProtocolLib packet access。
- WorldEdit/FAWE region operation。
- Dynmap、Essentials、XConomy 等第三方事件或能力。

要求：

- soft dependency 缺失时生成 startup report。
- provider 变化触发 capability invalidation。
- 高风险 capability 必须显式声明 owner、scope、risk level。
- packet listener 进入 EventRuntime owner model。
- WorldEdit 这类大型操作进入 TaskRuntime budget/backpressure。

## DiagnosticsRuntime

DiagnosticsRuntime 是框架的操作面。它要能回答：

- 当前有哪些 owner。
- 哪些 task/listener/gui/network/cache/writer 还活着。
- 哪些回调超过 tick budget。
- 最近一次 reload 做了什么。
- 哪些 config/message key 有问题。
- 哪些 external capability 不可用。
- 哪些 queue、cache、client 正在拖慢插件。

输出对象：

```text
RuntimeDiagnostics
├── owner tree
├── active tasks
├── event subscriptions
├── command registry
├── open GUI sessions
├── config/message snapshots
├── network clients and requests
├── persistence queues
├── cache stats
├── integration capabilities
└── recent reports
```

面向两类读者：

- 开发者：测试、定位 bug、评估 Interface 是否深。
- 服务器管理员：复制诊断摘要、判断插件资源成本。

## 测试 Harness

测试围绕 Interface，而不是 Implementation 细节：

- `LifecycleHarness`: start/reload/close/state failure。
- `OwnerHarness`: child resource close、leak detection、close order。
- `TaskHarness`: affinity、timeout、dependency failure、cancellation。
- `EventHarness`: scope、ignoreCancelled、priority、await、backpressure。
- `CommandHarness`: parser、suggestion、permission、error rendering、descriptor generation。
- `GuiHarness`: event routing、page stack、render diff、generation id。
- `ConfigHarness`: snapshot、merge、validation、old snapshot retention。
- `TextHarness`: key validation、fallback、MiniMessage error。
- `NetworkHarness`: policy、timeout、redaction、circuit state。
- `PersistenceHarness`: batch writer、flush report、backpressure。
- `IntegrationHarness`: capability available/unavailable/degraded。

MockBukkit 可用于 Adapter tests；纯 Interface tests 不应依赖完整 server。

## 典型流程

### 启动

```text
Bukkit loads plugin yml
-> NewNanPlugin boot
-> validate packaged descriptors
-> create PluginRuntime owner
-> load ConfigSnapshot and MessageCatalog
-> resolve Integration capabilities
-> install modules
-> start modules in dependency order
-> register commands/events/gui adapters
-> publish StartReport
```

### Reload

```text
admin command
-> CommandRuntime creates command owner
-> TaskRuntime runs config/text load off-thread
-> ConfigRuntime validates new snapshots
-> modules reload in order
-> cache invalidation by scope/version
-> old snapshots retained on failure
-> ReloadReport rendered through TextRuntime
-> DiagnosticsRuntime stores report
```

### Command 打开 GUI

```text
command handler
-> permission/parser/suggestion already validated
-> command owner creates task chain
-> AsyncIo loads page data
-> Entity/Main affinity opens GUI
-> GuiRuntime creates player/session/page owner
-> EventRuntime routes inventory events to page
-> DataRuntime cancels stale generation
```

### 局部事件交互

```text
/area create
-> CommandRuntime creates interaction scope
-> EventRuntime awaits first click
-> EventRuntime awaits second click
-> TaskRuntime validates region off-thread
-> Region/Main affinity applies result
-> scope closes all listeners/tasks
```

## Interface 与 Adapter 映射

| 深 Module | 稳定 Interface | Adapter/参考 |
| --- | --- | --- |
| LifecycleRuntime | owner tree、module state、reports | Paper Lifecycle API |
| TaskRuntime | affinity、task chain、outcome、budget | BukkitScheduler、Paper scheduler、Folia scheduler、MCCoroutine、FoliaLib |
| EventRuntime | event scope、subscription、await、backpressure | Bukkit/Paper events、domain bus、external listener |
| CommandRuntime | command descriptor、parser、suggestion、error renderer | Incendo Cloud、Aikar ACF、CommandAPI |
| GuiRuntime | session/page/component/render/data | SmartInvs、Triumph GUI、IF、devnatan inventory-framework |
| ConfigRuntime | schema、snapshot、reload report | YAML、Jackson、Configurate、Okaeri |
| TextRuntime | message key、catalog、audience rendering | Adventure、MiniMessage |
| NetworkRuntime | named client、policy、request diagnostics | Ktor、OkHttp、Resilience4j |
| PersistenceRuntime | data source scope、transaction、batch writer | HikariCP、Exposed、Ktorm |
| CacheRuntime | cache policy、invalidation、stats | Caffeine |
| DiagnosticsRuntime | health dump、resource graph、harness | MockBukkit、spark |
| IntegrationRuntime | typed capability、provider health | ServicesManager、Vault、ProtocolLib、WorldEdit |

## 设计中刻意避免的东西

- 不把第三方库类型直接变成业务 Interface。
- 不让业务长期保存裸 `CoroutineScope`、`BukkitTask`、`Listener`、`HttpClient`。
- 不用 `null` 同时表示失败、取消、无结果。
- 不让 GUI 依赖 global current manager。
- 不让 cache 各自创建维护线程。
- 不让 plugin yml、命令声明、权限说明三处漂移。
- 不把 Folia 简化为一个配置字段。
- 不把 diagnostics 留给“开发者自己打日志”。
- 不用宽松 wrapper 掩盖 schema、线程、生命周期问题。

## 当前开放问题

这些问题不阻塞统一设计，但需要在具体 API 草案阶段拍板：

1. build-time descriptor 使用 KSP、annotation processor，还是 Gradle source scanning。
2. Module Interface 是否保留与旧 BaseModule 源码相似的入口形态。
3. `Affinity.Region` 的 public 参数应使用 chunk、block location，还是 framework location snapshot。
4. ConfigRuntime 默认 Adapter 选 Jackson/YAML 还是 Configurate/Okaeri 风格。
5. TextRuntime 的 typed key 是代码生成、手写 object，还是 KSP 校验。
6. CommandRuntime 是保留 Cloud annotation 原样，还是定义框架注解再适配 Cloud。
7. Diagnostics 管理命令的输出格式和权限节点。

## 下一步文档拆分

统一设计之后，后续文档应按深 Module 写 Interface 草案：

- `../02-runtime/lifecycle-owner-runtime.md`
- `../02-runtime/task-runtime.md`
- `../02-runtime/event-runtime.md`
- `../02-runtime/command-runtime.md`
- `../02-runtime/gui-runtime.md`
- `../02-runtime/plugin-spec-generation.md`
- `../02-runtime/config-runtime.md`
- `../02-runtime/text-runtime.md`
- `../02-runtime/network-runtime.md`
- `../02-runtime/persistence-cache-runtime.md`
- `../02-runtime/diagnostics-runtime.md`
- `../02-runtime/integration-runtime.md`

每份草案都要固定回答五件事：Interface 是什么、Implementation 藏了什么、Seam 在哪里、Adapter 怎么接入、测试 harness 如何验证。
