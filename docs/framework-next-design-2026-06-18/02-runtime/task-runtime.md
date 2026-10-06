# TaskRuntime

## 定位

TaskRuntime 是下一代框架最重要的深 Module。它把 Bukkit/Paper/Folia scheduler、Kotlin coroutine、executor、timeout、dependency、cancellation、backpressure 和 diagnostics 收到一个 Interface 后面。

它解决的问题不是“少写几行 runTask”，而是让业务代码明确表达：这段工作属于谁、在哪个 Minecraft 执行域运行、允许消耗多少预算、失败后如何传播、关闭时如何取消。

## Interface

调用者需要理解的核心概念：

- `Affinity`: 任务应该在哪个执行域运行。
- `TaskSpec`: 任务名称、owner、policy 和执行体。
- `TaskHandle<T>`: 可等待、可取消、可诊断的任务引用。
- `TaskOutcome<T>`: 成功、失败、取消、超时、拒绝等结构化结果。
- `TaskChain`: 多阶段跨 affinity 工作流。

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

Interface 不变量：

- 每个 task 必须有 owner。
- 每个 task 必须有名称，进入 diagnostics。
- 每个 task 必须有 affinity，不能隐式猜主线程。
- task failure 不允许被编码成 `null`。
- owner close 后，未完成 task 必须收到 cancellation reason。
- task handler 不能在错误 affinity 下触碰 Bukkit/Paper 高危对象。

## 基本用法

单阶段任务：

```kotlin
val handle = ctx.tasks.run("load-profile") {
    owner(ctx.owner)
    affinity(Affinity.AsyncIo)
    timeout(3.seconds)
    execute { profiles.load(playerId) }
}
```

跨 affinity 任务链：

```kotlin
ctx.tasks.chain("book-search", owner = ctx.owner) {
    io("query") { books.search(keyword) }
    cpu("sort") { result -> result.sortedByRelevance() }
    entity(player, "open-gui") { sorted -> ctx.gui.open(player, BookPage(sorted)) }
    onFailure { error -> ctx.text.send(player, Keys.Errors.BookSearchFailed, error) }
}
```

周期任务：

```kotlin
ctx.tasks.every("session-expiry") {
    owner(ctx.owner)
    affinity(Affinity.AsyncCpu)
    initialDelay(1.minutes)
    period(30.seconds)
    budget(TickBudget.none())
    execute { sessions.expireOldSessions() }
}
```

## Affinity 语义

`Affinity.Main`:

- Paper/Bukkit 主线程。
- 可触碰大多数 Bukkit API。
- 必须有 tick budget。

`Affinity.AsyncIo`:

- 文件、数据库、网络。
- 有界线程池。
- 不可直接触碰 Bukkit world/player/entity/inventory mutable state。

`Affinity.AsyncCpu`:

- 解析、排序、模板预编译。
- 受 CPU 核心数约束。
- 不适合阻塞 I/O。

`Affinity.Global`:

- Folia global scheduler 语义。
- Paper Adapter 可映射到主线程。

`Affinity.Region`:

- 位置或 chunk 所属区域。
- Folia Adapter 映射到 RegionScheduler。
- Paper Adapter 可映射到主线程，但 diagnostics 应保留 region 意图。

`Affinity.Entity`:

- 玩家或实体归属。
- Folia Adapter 映射到 EntityScheduler。
- 适合 GUI、玩家消息、实体状态操作。

## Policy

Task policy 应在任务入口声明：

```kotlin
data class TaskPolicy(
    val timeout: Duration?,
    val budget: TickBudget?,
    val queue: QueuePolicy,
    val retry: RetryPolicy?,
    val cancellation: CancellationPolicy,
    val failureMode: FailureMode
)
```

常见 policy：

- `timeout`: 防止任务悬挂。
- `tickBudget`: 主线程或 region/entity 任务的耗时约束。
- `maxQueueSize`: 防止无限排队。
- `dropIfBusy`: 高频事件派生任务常用。
- `coalesceByKey`: 同 key 合并，只处理最新状态。
- `retry`: 只用于幂等任务，且必须有 backoff。
- `failureMode`: fail-chain、continue-with-default、report-only。

## Implementation

TaskRuntime 内部隐藏：

- Bukkit/Paper scheduler 调用。
- Folia global/region/entity scheduler 映射。
- coroutine scope 和 Job 管理。
- executor pool 管理。
- task dependency graph。
- timeout timer。
- cancellation propagation。
- queue/backpressure。
- slow task measurement。
- diagnostics snapshot。

业务 Module 不直接保存 `BukkitTask`、`Job`、`Future` 或裸 executor。

## Seam 和 Adapter

外部 Seam：

- `TaskRuntime` Interface -> scheduler/executor/coroutine Implementation。
- `Affinity` -> Bukkit/Paper/Folia scheduler Adapter。
- `TaskHandle` -> coroutine Job / BukkitTask / internal scheduled task。

Adapter：

- `PaperTaskAdapter`: Main/AsyncIo/AsyncCpu 到 Bukkit/Paper scheduler + executor。
- `FoliaTaskAdapter`: Global/Region/Entity 到 Folia scheduler。
- `CoroutineTaskAdapter`: suspend 函数和 framework-owned CoroutineScope。
- `TestTaskAdapter`: deterministic fake scheduler，用于 harness。

Adapter 要求：

- Paper 下保留 `Region/Entity` 意图，不把它吞成普通 `Main`。
- Folia 下禁止把 region/entity 操作偷偷转到错误区域。
- Adapter failure 返回 `TaskOutcome.Failed` 或 `Rejected`，不抛到业务线程顶层。

## 与其他 Runtime 的协作

EventRuntime:

- event handler 通过 TaskRuntime 派生异步工作。
- TaskRuntime 可等待 event，并在 task cancel 时注销 listener。
- 高频事件使用 TaskRuntime 的 backpressure policy。

CommandRuntime:

- command handler 可以声明 affinity。
- command scope 下派生任务随命令 owner 关闭。
- 异步失败由 CommandRuntime 渲染给 sender。

GuiRuntime:

- GUI 数据加载用 AsyncIo/AsyncCpu。
- ItemStack materialize 用 Main/Entity。
- page close 取消 generation task。

ConfigRuntime/TextRuntime:

- reload 的文件 I/O 和解析 off-thread。
- snapshot apply 回到正确 affinity。

NetworkRuntime/PersistenceRuntime:

- 网络 request 和 batch flush 是可诊断 task。
- owner close 触发 cancellation 或 drain。

## Diagnostics

Task diagnostics 应包含：

- task name。
- owner path。
- affinity。
- state: queued/running/completed/cancelled/failed/timedOut/rejected。
- queued duration。
- execution duration。
- timeout。
- queue policy。
- cancellation reason。
- last error summary。
- dependency path。

慢任务报告示例：

```text
slow task module:gui/page:book-list/render-main
affinity=Main duration=7.8ms budget=2ms
owner=plugin:book/module:gui/player:uuid/page:book-list
suggestion=move data sorting to AsyncCpu or reduce ItemStack materialization
```

## Harness

`TaskHarness` 需要 deterministic，不依赖真实 server tick：

- 手动推进 tick。
- 手动完成 async job。
- 可注入 failure。
- 可检查 queue state。
- 可检查 owner close cancellation。

关键测试：

- dependency failure 不进入下一段。
- timeout 返回 `TaskOutcome.TimedOut`。
- owner close 取消未完成 task。
- `dropIfBusy` 对同 key 任务生效。
- `coalesceByKey` 只执行最新状态。
- Main task 超预算进入 diagnostics。
- Paper Adapter 下 Region/Entity 保留意图。
- fake Folia Adapter 下 Entity affinity 调用正确 scheduler。

## 设计禁区

- 不把 `null` 当失败或取消。
- 不让业务直接拿裸 `CoroutineScope` 长期保存。
- 不允许无限队列作为默认策略。
- 不允许异步任务直接修改 Bukkit world/player/entity/inventory。
- 不把 `runAsync` 当性能优化万能入口。
- 不在 task handler 里吞异常只打日志。

## 开放问题

1. `Affinity.Region` 的输入应使用 chunk、block location，还是 framework location snapshot。
2. 默认 executor pool 大小如何按 server 配置调整。
3. `TaskChain` 是否支持 typed DSL，还是先用普通 continuation。
4. 主线程 budget 默认阈值是多少。
5. coroutine 是否作为内部默认 Implementation，还是仅作为 Adapter。
