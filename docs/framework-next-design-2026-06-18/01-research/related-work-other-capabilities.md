# 其他框架能力的相关工作

这份文档补充 event/gui/command 之外的能力比较。目标不是给下一代框架挑依赖，而是从相关项目中提炼设计压力：哪些复杂度应该被框架托管，哪些能力只应该作为 Adapter，哪些边界必须显式暴露给业务插件。

## 结论先行

新框架的深层 Interface 不应该按库名划分，而应该按 Minecraft 运行时事实划分：

- `LifecycleRuntime`: 托管 plugin/module/session/request 级 owner tree，所有 task、event、GUI、network、cache、resource 都必须挂到 owner。
- `TaskRuntime`: 显式表达 tick、wall-clock、sync、async、region、entity、global 等线程/区域归属。
- `ConfigRuntime`: 以 typed config snapshot、schema report、reload transaction 为核心，不把 YAML/JSON/TOML 解析器暴露成业务事实。
- `TextRuntime`: 以 message key、locale、audience、component rendering 为核心，MiniMessage 是表达格式，不是业务接口。
- `NetworkRuntime`: 以 named client、policy、quota、retry/circuit、metrics、shutdown 为核心，Ktor/OkHttp 是实现。
- `PersistenceRuntime`: 以 connection ownership、transaction boundary、batch write、cache invalidation、flush report 为核心，ORM/SQL DSL 是选择之一。
- `DiagnosticsRuntime`: 以 framework-owned probes、test harness、trace id、lifecycle dump 为核心，不依赖开发者手写散点日志。
- `IntegrationRuntime`: 以 typed capability、soft dependency state、provider health 为核心，Vault/ProtocolLib/WorldEdit 等第三方插件是外部能力源。

## Lifecycle 和 owner

### Paper Lifecycle API

Paper 的 Lifecycle API 单独服务于早期注册，尤其是 Brigadier command 和 registry 类能力。它明确区分 `Plugin` owner 和 `BootstrapContext` owner，并允许按 handler priority 或 monitor 配置处理顺序。这个 API 存在的理由很重要：普通 Bukkit Event 出现得太晚，且依赖 `Plugin` 实例，不适合承载早期初始化。

对我们的启发：

- 生命周期不是只有 `onEnable/onDisable`，还应有 bootstrap/configure/register/start/stop/dispose 等阶段。
- 不同阶段允许注册的能力不同，最好在类型或 builder 层约束，而不是运行时抛错。
- owner 是资源归属的事实源。没有 owner 的 task、event listener、http client、cache、GUI session 都是不完整的资源。
- 当前 `BaseModule` 已经有 Terminable 风格，但 public surface 仍把 scheduler、event、config 等能力散在多个 helper 中。新框架应把 owner tree 作为底层模型，而不是每个模块各自管理。

### lucko/helper

`lucko/helper` 把 Events、Scheduler、Promise、Metadata、Messenger、Commands、Scoreboard 等常见能力放进一个 Bukkit development toolkit。它的价值不是某个 API 细节，而是把“开发者重复写的基础设施”收拢成一组可组合工具。

优点：

- 降低样板代码，尤其是 functional event、scheduler、promise 这类高频能力。
- 给 Java/Bukkit 开发者一个一致入口。
- 证明 event/task/metadata/command 的组合是插件框架里真实存在的需求。

不足：

- 更像 toolkit，不像完整运行时。owner tree、诊断报告、跨 module 生命周期、Folia affinity 不是核心事实源。
- builder/tool helper 容易让资源注册看起来轻量，但如果没有统一 owner，仍可能留下泄漏和卸载顺序问题。

对我们的设计指导：

- 可以学习它的低样板体验，但不要止步于 helper。
- 深层设计应是 `Runtime + Scope + Adapter`，而不是一组全局 utility。
- Metadata 类能力可以转化为 `ScopedState` 或 `SessionState`，让过期、GC、玩家离线、module unload 都归 owner 管。

## Scheduler、task 和 coroutine

### Paper/Bukkit scheduler

Paper 文档把 tick 作为基本时间单位，并明确 20 TPS 下一个 tick 约 50ms。它同时强调：同步任务运行在主线程，会影响 server performance；异步任务不直接影响主线程，但大量 Bukkit API 不能在异步任务访问，尤其是读取或修改 world state。

这说明 `runSync/runAsync` 只是最低层能力，不能表达完整约束。真正的任务 API 至少需要：

- `delay` 的时间语义：tick delay 和 wall-clock delay 不能混在一起。
- `affinity`：main thread、async pool、Folia global、region、entity。
- `budget`：每 tick 最大执行时间、批处理切片大小、背压策略。
- `owner`：任务属于哪个 plugin/module/session/request，owner 结束时如何取消。
- `outcome`：success、failure、cancelled、timeout、skipped、dropped 都要可观察。

### Folia scheduler

Folia 把调度拆成 Global、Region、Async、Entity。Paper 文档强调，标记 `folia-supported: true` 远远不够，插件必须以支持 Folia 的意图开发。Region scheduler 绑定 location 所属 region，Entity scheduler 跟随 entity 跨 region 移动。

对我们的启发：

- `TaskRuntime` 的 Interface 不应叫 `runSync`，而应让调用方表达“我要操作哪个对象或区域”。
- Entity 和 Location 是调度输入，不只是 handler 参数。
- `ThreadAffinity` 应该是编译期/运行时都能检查的 policy，而不是注释。
- GUI、event、network 回调在回主线程时，也应该明确回到哪个 affinity，而不是默认 `Bukkit.getScheduler().runTask`。

建议的抽象方向：

```kotlin
module.tasks.schedule {
    owner(thisModule)
    affinity(EntityAffinity(player))
    every(Ticks(20))
    budget(TickBudget.millis(2))
    onCancel { reason -> ... }
    run { ctx -> refreshPlayerState(ctx.entity<Player>()) }
}
```

这段只是草案，关键点是调用方表达对象归属，框架选择底层 scheduler。

### MCCoroutine

MCCoroutine 把 Kotlin coroutine 扩展到 Minecraft server 环境，并覆盖 suspendable commands、events、schedules，还提供 plugin lifecycle scope。它支持 Bukkit/Paper/Spigot/Folia/Minestom 等多个平台。

优点：

- Kotlin 业务代码可以用 suspend 组织异步流程，减少 callback 嵌套。
- event、command、schedule 被放到同一异步模型里，和我们对 event/task/command 协同的判断一致。
- lifecycle scope 能防止插件卸载后 coroutine 继续运行。

风险：

- coroutine scope 如果暴露太自由，会产生新的 owner 泄漏。
- `Dispatchers.Default/IO` 不理解 Bukkit/Folia affinity，必须由框架封装切回游戏线程或区域线程。
- exception propagation、supervisor scope、cancellation reason 必须转成 framework diagnostics，否则调试体验会变差。

对我们的设计指导：

- 可以采用 coroutine 作为实现基座，但 public API 仍应表达 Minecraft affinity 和 owner。
- 每个 module/session/request 可以有 framework-owned `CoroutineScope`，禁止业务自行长期保存裸 scope。
- `EventRuntime` 和 `TaskRuntime` 应共享 cancellation、timeout、exception policy。

### FoliaLib / Universal scheduler 类库

FoliaLib 这类库的核心价值是把 Spigot/Paper/Folia 的 scheduler 差异封起来，提供 `runNextTick/runAsync/runAtLocation/runAtEntity/teleportAsync/cancelAllTasks` 之类统一入口。

优点：

- 快速覆盖多平台 scheduler 差异。
- 明确展示了 run at location/entity 是 Folia 场景的刚需。
- `cancelAllTasks` 说明 task owner 是基础需求。

不足：

- 它仍然是 scheduler adapter，不会替我们解决 module owner、event/task 协同、GUI session、网络请求和 cache 的统一治理。
- 如果框架直接把 FoliaLib 风格 API 暴露给业务，业务仍需理解底层平台差异。

对我们的设计指导：

- 这类库可作为 Adapter 候选或参考，不应成为核心 Interface。
- 核心 Interface 要表达 `affinity + owner + policy`；底层再映射到 Bukkit/Paper/Folia。

## Config 和 plugin spec

### Bukkit/Paper YAML 配置

传统 Bukkit 配置上手快，但问题也直接：schema 不强、默认值与实际文件容易漂移、注释保留/排序/格式控制有限、reload 经常不是 transaction。对业务插件来说，维护一堆 `plugin.yml/config.yml/messages.yml` 的心智负担明显。

我们已经倾向于“代码声明，生成静态 yml”。这个方向可以扩展为两类能力：

- `PluginSpec`: 用代码声明 plugin name/version/main/dependencies/permissions/commands，再生成 `plugin.yml` 或 `paper-plugin.yml`。
- `RuntimeConfig`: 用代码声明 typed config schema，再生成 default config、schema report、reload diff 和 validation error。

这两者不要混成一个系统。plugin spec 是构建期静态事实，runtime config 是运行期可读取/可重载事实。

### Configurate

Configurate 提供 node-based representation、支持多种配置格式，并带 transformation 能力。它适合复杂配置树和格式适配。

优点：

- 格式无关，YAML/JSON/HOCON/XML 等可用同一 node 模型处理。
- transformation 能帮助旧配置结构整理为新结构。
- 比 Bukkit `YamlConfiguration` 更适合作为配置基础设施。

不足：

- node API 本身仍偏底层，业务直接操作 node 会增加心智负担。
- transformation 很强，但如果滥用，会让配置演进逻辑散落在运行时。

### Okaeri Configs

Okaeri Configs 强调 Java class 作为 config adapter、强类型字段、durable comments、复杂类型序列化、小体积和多环境支持。

优点：

- 开发体验接近 typed POJO/Kotlin data class。
- durable comments 对 Minecraft 插件配置很有价值，服主常常直接编辑文件。
- 小体积和环境适配适合插件生态。

不足：

- 如果配置类直接暴露给业务，reload/snapshot/validation/report 容易和业务对象耦合。
- Java-style getter/setter 模式未必贴合 Kotlin data/value object。

对我们的设计指导：

- `ConfigRuntime` 应提供 typed snapshot，而不是让业务到处拿 mutable config。
- reload 应生成 `ConfigReloadReport`: loaded、changed、invalid、defaulted、deprecated、restartRequired。
- comments/format 是 Adapter 能力；schema、validation、snapshot、reload transaction 是核心能力。
- plugin spec 生成属于 build tool 或 annotation/KSP processor，不进入运行时热路径。

## Text、message 和 i18n

### Adventure / MiniMessage

Paper/Adventure 文档把 MiniMessage 定义为 chat components 的简单字符串表示，目标是让用户容易学习、让开发者容易扩展。这说明 MiniMessage 很适合作为可编辑文本格式，但不应该替代业务消息模型。

当前项目已有 `ComponentProcessor`、`MessageManager`、`ComponentParseMode.Auto`、MiniMessage/Legacy 自动检测等能力。这里的设计不足不是“不会渲染 Component”，而是 message key、locale、audience、placeholder、reload report 和 fallback 关系还不够统一。

建议：

- 业务只引用 `MessageKey`，不要散落 raw string key。
- `TextRuntime.render(key, audience, args)` 输出 Adventure Component。
- MiniMessage parser 由 TextRuntime 管，parser、tag resolver、placeholder resolver 可以按 owner/scope 注册。
- message catalog reload 要报告 missing key、unused key、invalid MiniMessage、fallback usage。
- GUI title、command feedback、event message、log message 可共享 key，但 audience/render policy 不同。

需要避免：

- `Auto` 模式过度猜测格式。自动检测可以作为导入/兼容工具，但 runtime 渲染最好由 schema 声明格式。
- 把 MiniMessage 字符串当成持久业务状态保存。持久状态应保存 key 和参数。

## Network 和外部 HTTP

### Ktor Client

Ktor Client 通过 plugin 机制组合 ContentNegotiation、Auth、Logging 等能力。官方文档显示 ContentNegotiation 负责 media type negotiation 和 serialization/deserialization，Logging 支持 logger、level、filter、sanitizeHeader，Auth 还带 token cache/clear 能力。

对我们的启发：

- 当前 `NetworkDSL.createHttpClient` 和 `HttpClientManager` 已经接近 Ktor 风格，但 ownership 和 policy 还可以更清晰。
- NetworkRuntime 不应只提供 `get/post`，应托管 named client 的 lifecycle、credentials、token cache、request policy 和 shutdown。
- header/token logging 必须默认脱敏，不能把 Ktor logging 配置直接交给业务随意开。

### OkHttp

OkHttp 的 `Call` 模型把 redirects、follow-ups、retries 统一为一次调用任务；它支持同步和异步执行，并强调 call 可以从任意线程 cancel。Dispatcher 对 async call 的总并发和单 host 并发有 policy。

对我们的启发：

- HTTP request 也应该是 TaskRuntime 可观察的一种 task，有 owner、timeout、cancel reason 和 diagnostics。
- 并发上限不能只靠业务自觉，应有 per-client/per-host/per-owner policy。
- 对 Minecraft 插件来说，网络请求的回调要明确 affinity：返回 async、回 owner scheduler、回 player entity affinity，不能默认回主线程。

### Resilience4j

Resilience4j 用 decorator 组合 Circuit Breaker、Rate Limiter、Retry、Bulkhead、TimeLimiter。它的启发是 policy 可组合，但不必强迫所有 client 都使用同一组 policy。

建议：

- `NetworkPolicy` 至少包含 timeout、retry、rate limit、bulkhead、circuit breaker、fallback、redaction。
- policy 应按 named client 声明，不在每个调用点重复。
- diagnostics 要能回答：请求失败是 timeout、rate limited、circuit open、remote error、deserialize error，还是 owner cancelled。

## Persistence、cache 和 state

### HikariCP

HikariCP 的定位是轻量、高性能、生产可用 JDBC connection pool。它适合作为 JDBC pool Adapter，但不应泄漏成业务层核心概念。

对我们的设计指导：

- `PersistenceRuntime` 应声明 data source owner、pool policy、transaction policy、shutdown flush。
- Hikari config 可以由 typed config 生成，但业务不直接构造 `HikariConfig`。
- pool metrics 应进入 diagnostics，至少包括 active/idle/pending/timeout/error。

### Exposed / Ktorm

Exposed 提供 Kotlin typesafe SQL DSL 和轻量 DAO，支持 JDBC/R2DBC。Ktorm 提供基于 JDBC 的强类型 SQL DSL、entity sequence API 和较少依赖。

两者都说明 Kotlin 插件开发者确实需要比手写 SQL 更好的表达，但框架不应强制一个 ORM 风格：

- Exposed/DAO 风格适合以数据库模型为中心的模块。
- Ktorm/SQL DSL 风格适合希望保留 SQL 控制权的模块。
- 关键不是选哪个库，而是明确 transaction、threading、batch write、cache invalidation 和 schema ownership。

建议：

- 核心只定义 `PersistenceRuntime` 和 `RepositoryScope`，具体 SQL DSL 作为 Adapter。
- 写入默认走 batch writer，允许声明 flush 时机和 backpressure。
- 读缓存必须绑定 owner 和 invalidation policy，不能散落 `ConcurrentHashMap`。

### Caffeine

Caffeine 是 Java 高性能 cache，支持 size-based eviction、time-based expiration、async loading/refresh、weak/soft references 等能力。它适合作为本地 cache Adapter。

对我们的设计指导：

- cache 不是 `Map`。必须声明 key/value 类型、maximum size/weight、expireAfter、refreshAfter、loader、invalidation。
- cache owner 结束时要关闭 loader/job，并清理指标。
- 大对象、ItemStack、Component、Player/Entity 引用要谨慎。优先保存 UUID、key、snapshot，避免缓存强引用拖住 Bukkit 对象。
- GUI 数据源、i18n catalog、permission lookup、network response 都可以共用 CacheRuntime，但 policy 不同。

## Diagnostics、test 和 profiling

### MockBukkit

MockBukkit 提供可由 unit test 控制的 Bukkit/CraftBukkit mock implementation，支持 mock plugin、mock players 等能力。它能提高反馈速度，但无法覆盖真实 server tick、Paper/Folia scheduler、packet、inventory click 的所有行为。

对我们的设计指导：

- 新框架应该有自己的 test harness，让 module 在没有完整 server 的情况下测试 lifecycle、event scope、task cancellation、config reload、message rendering。
- MockBukkit 适合验证 Bukkit API glue，不适合作为全部正确性的事实源。
- framework Interface 的测试应尽量不依赖具体 Bukkit 实现；Adapter 测试再接 MockBukkit/Paper test。

### spark

spark 是 Minecraft clients/servers/proxies 的 profiler。它说明性能诊断是 Minecraft 插件开发的常态需求，不应只靠开发者在日志里猜。

对我们的设计指导：

- `DiagnosticsRuntime` 应输出 framework-level health dump：active owners、tasks、listeners、GUI sessions、http clients、cache stats、persistence queues。
- 对 tick 热点要记录 task name、owner、average/max duration、drop/skip 次数。
- 对 event 高频路径要记录 listener count、filter drop count、handler duration、exception count。
- 与 spark 的关系是互补：spark 看 JVM/server 运行热点，框架 diagnostics 看业务资源归属和框架状态。

### 测试恢复问题

当前 root build 禁用了 Test task，但各 modules 自己有 `useJUnitPlatform()`。这暗示测试基础不是完全没有，而是聚合入口和模块 harness 不稳定。

建议拆成三类测试：

- Interface tests：不依赖 Bukkit，验证 owner tree、scope、task outcome、config snapshot、cache policy。
- Adapter tests：使用 MockBukkit 或轻量 fake，验证 Bukkit/Paper glue。
- Contract samples：用小插件样例验证 event/task/command/gui/config 的组合体验。

## Integration 和第三方插件能力

### Bukkit ServicesManager / Vault

Bukkit `ServicesManager` 以 service interface 和 provider implementation 管理服务，多个 provider 按 priority 取最高优先级。VaultAPI 的典型用法是从 Bukkit ServiceManager 取得 Economy/Permission/Chat service。

这说明 Minecraft 插件生态已经有“能力发现”模型，但它偏全局、弱类型诊断不足、生命周期变化需要调用方自己处理。

建议：

- `IntegrationRuntime.capability<T>()` 包装外部 provider，提供 `Available/Unavailable/Degraded` 状态。
- soft dependency 应有 startup report，而不是失败时 NPE。
- provider 变更、plugin disable、reload 类场景要触发 capability invalidation。
- Vault 这类接口可以作为 Adapter，不直接污染业务模块。

### ProtocolLib

ProtocolLib 的价值是把包监听/修改从 NMS/CraftBukkit 反射细节中抽出，提供 packet event 风格 API。它的维护重点是跨版本兼容。

对我们的启发：

- packet 能力是高风险 integration，应当作为 explicit capability，不应该让普通业务模块随便取全局 ProtocolManager。
- packet listener 也要有 owner、priority、scope、diagnostics，和 EventRuntime 共享一些模型。
- packet 修改必须显式标记风险级别和 platform/version support。

### WorldEdit / FAWE

WorldEdit 文档把 `worldedit-core` 和 `worldedit-bukkit` 分开：core 不依赖平台，bukkit implementation 负责 BukkitAdapter 类型转换。这是很好的边界案例。

对我们的启发：

- 我们自己的 framework core 也应尽量保持 domain Interface 不直接依赖 Bukkit 细节。
- Adapter 层负责 Bukkit/Paper/第三方插件类型转换。
- 大型操作如 schematic paste、区域编辑、批量方块修改必须进入 TaskRuntime budget/backpressure，而不是在 command/event handler 中直接执行。

## 对下一代框架的新增要求

从这些相关工作反推，下一代框架除了 event/gui/command，还应明确这些需求：

1. 统一 owner tree：所有资源都必须能回答“属于谁、何时释放、释放失败怎么报告”。
2. 明确 affinity：所有 touching Bukkit world/player/entity/inventory 的代码必须知道执行在哪个线程或区域。
3. policy 可声明：timeout、retry、rate limit、cache expiry、batch flush、tick budget 不应该散在调用点。
4. diagnostics 一等公民：框架要能 dump 当前资源状态和热点，不靠人工日志排查。
5. typed snapshot：配置、message catalog、external capability、cache entry 尽量用不可变 snapshot 暴露。
6. Adapter 可替换，但 Interface 稳定：Ktor/OkHttp/Hikari/Caffeine/Exposed/Configurate/Okaeri/MockBukkit 都只能影响 Adapter，不改变业务 API。
7. 高频路径低分配：event filter、GUI click、message render、cache lookup、task tick loop 都要避免 per-call builder 和临时集合。
8. 显式 backpressure：网络、数据库、批量 GUI 刷新、世界编辑、外部服务调用都要有排队上限和降级策略。

## 后续可展开的草案

- `../02-runtime/task-runtime.md`: `TaskRuntime`、`Affinity`、`TaskOutcome`、`TickBudget`、`CancellationReason`。
- `../02-runtime/config-runtime.md`: `PluginSpec`、`ConfigSchema`、`ConfigSnapshot`、`ConfigReloadReport`。
- `../02-runtime/text-runtime.md`: `MessageKey`、`AudiencePolicy`、`CatalogReport`、MiniMessage adapter。
- `../02-runtime/network-runtime.md`: `NamedHttpClient`、`NetworkPolicy`、`RequestDiagnostics`。
- `../02-runtime/persistence-cache-runtime.md`: `DataSourceScope`、`TransactionScope`、`BatchWriter`、`CacheRuntime`。
- `../02-runtime/diagnostics-runtime.md`: health dump、resource graph、test harness、profiling hooks。
- `../02-runtime/integration-runtime.md`: typed capability、soft dependency state、provider health。

## 资料来源

- Paper Scheduling: https://docs.papermc.io/paper/dev/scheduler/
- Paper Supporting Paper and Folia: https://docs.papermc.io/paper/dev/folia-support/
- Paper Lifecycle API: https://docs.papermc.io/paper/dev/lifecycle/
- MCCoroutine: https://shynixn.github.io/MCCoroutine/wiki/site/
- FoliaLib: https://github.com/TechnicallyCoded/FoliaLib
- lucko/helper: https://github.com/lucko/helper
- Configurate: https://github.com/SpongePowered/Configurate
- Okaeri Configs: https://github.com/OkaeriPoland/okaeri-configs
- Paper MiniMessage: https://docs.papermc.io/adventure/minimessage/
- Ktor Client plugins: https://ktor.io/docs/client-plugins.html
- Ktor Client serialization: https://ktor.io/docs/client-serialization.html
- Ktor Client auth: https://ktor.io/docs/client-auth.html
- Ktor Client logging: https://ktor.io/docs/client-logging.html
- OkHttp Calls: https://square.github.io/okhttp/features/calls/
- Resilience4j: https://resilience4j.readme.io/docs/getting-started
- HikariCP: https://github.com/brettwooldridge/HikariCP
- Exposed: https://github.com/jetbrains/exposed
- Ktorm: https://www.ktorm.org/
- Caffeine: https://github.com/ben-manes/caffeine
- MockBukkit: https://github.com/MockBukkit/MockBukkit
- spark: https://github.com/lucko/spark
- Bukkit ServicesManager: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/plugin/ServicesManager.html
- VaultAPI: https://github.com/MilkBowl/VaultAPI/
- ProtocolLib: https://github.com/dmulloy2/ProtocolLib/
- WorldEdit Developer API: https://worldedit.enginehub.org/en/latest/api/
