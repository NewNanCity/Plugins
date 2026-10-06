# Lifecycle/OwnerRuntime

## 定位

Lifecycle/OwnerRuntime 是下一代框架的根 Module。它把插件、Module、临时交互、GUI session、task、event subscription、network request、cache、persistence writer 等长期资源组织成一棵 owner tree，并负责状态转换、关闭顺序、失败报告和资源诊断。

没有这个 Module，其他 Runtime 会各自实现一套生命周期：task 保存自己的 handle，event 保存自己的 listener，GUI 保存自己的 session，network 保存自己的 job，cache 保存自己的清理线程。这样 Interface 会变浅，复杂度会重新扩散到业务插件。

## Interface

调用者需要理解的核心概念只有四个：

- `PluginRuntime`: 插件运行期根 owner。
- `PluginModule`: 业务 Module 的生命周期 Interface。
- `ResourceOwner`: 所有长期资源的父子关系和关闭入口。
- `LifecycleReport`: start/reload/close 的结构化结果。

```kotlin
interface PluginModule {
    val id: ModuleId
    fun start(ctx: ModuleContext): StartReport = StartReport.ok()
    fun reload(ctx: ReloadContext): ReloadReport = ReloadReport.noop()
    fun close(ctx: CloseContext): CloseReport = CloseReport.ok()
}

interface ResourceOwner {
    val id: OwnerId
    val parent: ResourceOwner?
    val tags: OwnerTags
    val state: OwnerState

    fun child(id: OwnerId, tags: OwnerTags = OwnerTags.empty()): ResourceOwner
    fun close(reason: CloseReason): CloseReport
    fun diagnostics(): OwnerDiagnostics
}
```

`ResourceOwner` 的 Interface 不只是方法签名，还包含这些不变量：

- child owner 不能比 parent 活得更久。
- parent close 必须反向关闭 child。
- close 必须幂等。
- close failure 必须进入 report，不能只打日志。
- owner id 在同一 parent 下必须稳定且可诊断。
- owner 只能由框架创建或经框架授权创建，业务不能伪造长期 owner。

## Module 状态机

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

失败状态不是一个简单 boolean，而要记录失败阶段：

```text
Starting -> Failed(start)
Reloading -> Running with old snapshot + Failed(reload)
Closing -> Closed with close failures
```

设计约束：

- 构造函数只保存依赖，不启动任务、不注册事件、不打开网络 client。
- `Installed` 阶段允许声明子 Module 和依赖，但不触碰 Bukkit 世界状态。
- `Starting` 阶段拿到 ConfigRuntime、TextRuntime、IntegrationRuntime 初始 snapshot。
- `Reloading` 阶段先准备新 snapshot，再调用 Module reload。
- `Closing` 阶段按 owner tree 反向关闭资源。

## Owner Tree

典型结构：

```text
plugin:tpa
├── module:sessions
│   ├── task:session-expiry
│   ├── event:PlayerQuitEvent/session-cleanup
│   └── cache:session-index
├── module:commands
│   └── command-scope:/tpa accept
└── module:gui
    └── player:uuid
        └── gui-session:default
            └── page:tpa-request-list
                ├── task:load-page-data
                └── event:InventoryClickEvent/page-router
```

owner tags 至少包含：

- `kind`: module/task/event/gui/network/cache/persistence/integration。
- `origin`: 创建它的 Module 或 Runtime。
- `purpose`: 人类可读用途。
- `affinity`: 如果资源会执行代码，则记录默认 affinity。
- `createdAt`: 创建时间。

这些 tag 是 diagnostics 的事实源，不是日志字符串。

## Close Order

关闭顺序必须由框架统一执行：

1. 标记 owner 为 `Closing`，拒绝新 child。
2. 通知 child 进入关闭流程。
3. 对可取消资源发送 cancellation reason。
4. 等待可 drain 资源完成，受 timeout 限制。
5. 强制关闭剩余 child。
6. 调用 owner 自己的 close hook。
7. 生成 `CloseReport`。

示例：

```text
module close
-> stop accepting new commands/events/tasks
-> cancel command scopes
-> cancel GUI page tasks
-> unregister event subscriptions
-> flush persistence writers
-> close network requests
-> invalidate owner-scoped caches
-> close module resources
-> emit CloseReport
```

不同资源的 close policy 可不同：

- task: cancel immediately 或 drain until timeout。
- batch writer: flush until timeout，失败进入 report。
- network request: cancel 或 drain 当前请求。
- GUI page: close inventory 或只释放后台资源。
- event subscription: unregister immediately。

## Implementation

Lifecycle/OwnerRuntime 内部隐藏这些复杂性：

- owner id 分配和重复检测。
- child owner 并发创建/关闭的锁策略。
- close 顺序、timeout 和 failure aggregation。
- Module dependency order。
- start/reload/close report 聚合。
- diagnostics snapshot。
- shutdown 时的 best-effort cleanup。
- 测试用 fake owner tree。

业务 Module 不应知道这些细节。它只在 `ModuleContext` 中拿到自己的 owner：

```kotlin
class TpaSessionModule(private val ctx: ModuleContext) : PluginModule {
    override val id = ModuleId("sessions")

    override fun start(ctx: ModuleContext): StartReport {
        ctx.events.on<PlayerQuitEvent>("cleanup") {
            owner(ctx.owner)
            handle { event -> sessions.remove(event.player.uniqueId) }
        }
        return StartReport.ok()
    }
}
```

这里 event subscription 的真实注册、注销、异常聚合都由 EventRuntime 和 OwnerRuntime 协作完成。

## Seam 和 Adapter

外部 Seam：

- Bukkit/Paper `JavaPlugin` lifecycle -> `PluginRuntime`。
- Paper Lifecycle API -> build/early registration Adapter。
- Module implementation -> `PluginModule` Interface。
- Runtime resource -> `ResourceOwner` Interface。

Adapter：

- `BukkitPluginLifecycleAdapter`: 接 `onLoad/onEnable/onDisable`。
- `PaperLifecycleAdapter`: 接早期 command/registry 类生命周期。
- `TerminableAdapter`: 让旧 Terminable 风格资源接入 owner tree。
- `CoroutineScopeOwnerAdapter`: 让 framework-owned scope 随 owner close。

Adapter 的要求：

- Adapter 可以知道 Bukkit/Paper 细节。
- 业务 Module 不直接依赖 Adapter。
- Adapter failure 必须进入 LifecycleReport。

## 与其他 Runtime 的协作

- TaskRuntime: task owner 结束时取消 task；task failure 能关闭 task scope。
- EventRuntime: subscription 是 owner child；scope 关闭时注销 listener。
- GuiRuntime: player/session/page/component 都是 owner。
- CommandRuntime: 每次命令执行可创建 command scope。
- ConfigRuntime/TextRuntime: reload report 挂到 runtime owner。
- NetworkRuntime: request owner 控制 cancellation。
- PersistenceRuntime: writer owner 控制 flush-on-close。
- CacheRuntime: owner close 触发 cache invalidation。
- IntegrationRuntime: capability 状态挂到 module/runtime owner。
- DiagnosticsRuntime: 读取 owner tree 生成 health dump。

## Diagnostics

`OwnerDiagnostics` 应回答：

- 当前 owner path。
- owner 状态。
- children 数量和类型。
- 创建来源。
- 默认 affinity。
- 已运行时长。
- close reason。
- 最后错误。
- 未关闭 child 列表。

示例输出形态：

```text
owner module:tpa.gui Running
children:
  gui-session:player/uuid/default Running age=12m
  task:page-data-load Running age=320ms affinity=AsyncIo
warnings:
  slow close: page:tpa-request-list close took 84ms
```

DiagnosticsRuntime 可以把它渲染成管理员命令输出，但原始数据应是结构化对象。

## Harness

`OwnerHarness` 需要覆盖：

- child close 顺序为后创建先关闭。
- parent close 后拒绝新 child。
- child close failure 被聚合到 parent report。
- close 幂等。
- task/event/cache/network fake resource 能挂 owner。
- module start failure 不会留下未关闭 child。
- reload failure 保留旧 snapshot 且 report 可诊断。

Interface test 示例：

```kotlin
test("module close cancels all child resources") {
    val runtime = OwnerHarness.plugin("test")
    val module = runtime.module("sessions")
    val task = module.fakeTask("expiry")
    val event = module.fakeEvent("quit")

    module.close(CloseReason.PluginDisable)

    assertThat(task.cancelledReason).isEqualTo(CloseReason.PluginDisable)
    assertThat(event.unregistered).isTrue()
    assertThat(module.diagnostics().children).isEmpty()
}
```

## 开放问题

1. owner id 是否要求全局唯一，还是 parent 下唯一即可。
2. close timeout 默认值如何分层：plugin/module/page/request。
3. Module dependency graph 是否允许 optional dependency。
4. 旧 `BaseModule.bind(Terminable)` 风格是否作为 Adapter 暂时保留。
5. diagnostics 中是否记录 source location，如何避免过高开销。
