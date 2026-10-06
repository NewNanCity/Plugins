# EventRuntime

## 定位

EventRuntime 是被动触发的运行时 Module。它统一管理 Bukkit/Paper event、框架 domain event、第三方 listener，以及任务中等待事件的临时作用域。

Minecraft 插件有两类入口：

- 主动入口：command、scheduled task、network callback、admin reload。
- 被动入口：player、entity、block、world、inventory、chat、third-party events。

EventRuntime 的目标不是让 listener 少写几行，而是让事件拥有 owner、scope、affinity、priority、cancellation、backpressure、diagnostics。事件 handler 不能再是“随手注册的回调”，它必须是 owner tree 中可关闭、可诊断、可测试的资源。

## 当前压力

现有代码中的 `subscribeEvent<T>` 已经证明这些需求存在：

- module scope：`BaseModule.subscribeEvent` 注册后绑定模块生命周期。
- filter、priority、expire、exception handler。
- GUI 依赖大量 Inventory event、AsyncChatEvent、PlayerQuitEvent。
- 第三方插件 event 和自定义 EventEmitter。
- handler 内经常派生 task，task 中也经常等待 event。

当前设计暴露的问题也很明确：

- 缺少 `ignoreCancelled`。
- `expireAfter` 语义不一致，且 filter 与 expiry 顺序不清。
- async event 没有显式 affinity。
- GUI routing 受 global current manager 影响。
- domain event 和 external listener 没有统一 owner。

这些问题说明 EventRuntime 应是深 Module，而不是 Event DSL helper。

## Interface

调用者需要理解的核心概念：

- `EventSubscription`: 一个 owner-bound listener。
- `EventScope`: 一组会一起关闭的 subscription。
- `EventContext<T>`: 带 owner、source、affinity、cancellable view 的安全事件上下文。
- `EventAwaitResult<T>`: task/command 等待事件的结构化结果。
- `EventBackpressurePolicy`: 高频事件到 task 的保护策略。

```kotlin
ctx.events.on<PlayerInteractEvent>("select-first-corner") {
    owner(selectionScope)
    priority(EventPriority.HIGH)
    ignoreCancelled(true)
    affinity(Affinity.Main)
    filter { it.player.uniqueId == playerId && it.action.isLeftClick }
    once()
    handle { ctx ->
        ctx.cancelIfNeeded("area-selection")
        selection.setFirst(ctx.event.clickedBlock?.location)
    }
}
```

Interface 不变量：

- 每个 subscription 必须有 owner。
- subscription close 必须注销 listener。
- `priority`、`ignoreCancelled`、`MONITOR read-only` 是 Interface 的一部分。
- async event 必须携带或推导 affinity。
- filter、expiry、once/until/timeout 的执行顺序必须可解释。
- handler exception 必须进入 structured report。
- handler 派生 task 时继承 event scope，除非显式指定 owner。

## EventScope

EventScope 用来表达局部生命周期。

常见 scope：

- plugin scope。
- module scope。
- player scope。
- command scope。
- task scope。
- GUI session/page scope。
- once scope。
- until scope。
- timeout scope。

示例：

```kotlin
ctx.events.scope("area-selection", owner = ctx.playerScope(player)) {
    val first = await<PlayerInteractEvent>("first-corner") {
        filter { it.player == player && it.action.isLeftClick }
        timeout(30.seconds)
    }

    val second = await<PlayerInteractEvent>("second-corner") {
        filter { it.player == player && it.action.isRightClick }
        timeout(30.seconds)
    }

    on<PlayerQuitEvent>("cancel") {
        filter { it.player == player }
        handle { close(CloseReason.PlayerQuit) }
    }
}
```

这比传统 Listener 类更深：调用者不需要自己保存 listener、task id、timeout 和注销逻辑。

## EventContext

`EventContext<T>` 把 Bukkit event 包成更安全的框架上下文：

```kotlin
data class EventContext<T : Event>(
    val event: T,
    val owner: ResourceOwner,
    val source: EventSource,
    val affinity: Affinity,
    val cancellable: CancellableView?,
    val firedAt: Instant,
    val sequence: Long,
    val diagnosticTags: EventTags
)
```

能力：

- `cancel(reason)`：只有 cancellable event 可用。
- `requireAffinity(...)`：开发期检测错误线程。
- `shiftTo(...)`：派生正确 affinity 的 task。
- `spawnTask(...)`：派生 task 默认挂到 event owner。
- `readOnlyMonitor()`：MONITOR handler 默认只读。

## Expiry 语义

EventRuntime 必须拆清楚三类计数：

- `seen(n)`: 看见 N 次原始 event 后过期。
- `matched(n)`: filter 命中 N 次后过期。
- `handled(n)`: handler 成功执行 N 次后过期。

默认推荐：

```kotlin
once() == handled(1)
until(predicate) == predicate 在 filter 之后、handler 之前判断
timeout(duration) == wall-clock timeout，随 owner close 取消
```

这样可以避免 `expireAfter(1)` 在第一次 handler 前关闭这种歧义。

## Event 与 Task 协作

事件 handler 应短小。遇到 DB、文件、网络、排序、GUI 数据准备时，只做采样和派发任务：

```kotlin
ctx.events.on<PlayerJoinEvent>("load-profile") {
    handle { event ->
        ctx.tasks.chain("profile-load", owner = event.owner) {
            io("load") { repository.load(event.event.player.uniqueId) }
            entity(event.event.player, "apply") { profile ->
                profile.applyTo(event.event.player)
            }
        }
    }
}
```

注意：

- 需要立即 cancel 的 event，不能等异步任务返回再决定。
- 如果异步判断动作是否合法，应使用预热缓存或保守拒绝策略。
- event 派生 task 必须有 backpressure policy，避免高频事件无限创建 task。

Task 等待 event：

```kotlin
val click = ctx.events.await<PlayerInteractEvent>(
    owner = taskOwner,
    timeout = 10.seconds,
    affinity = Affinity.Main
) { it.player.uniqueId == playerId }

when (click) {
    is EventAwaitResult.Hit -> handleClick(click.event)
    is EventAwaitResult.Timeout -> ctx.text.send(player, Keys.Timeout)
    is EventAwaitResult.Cancelled -> Unit
}
```

`await` 必须在命中、timeout、task cancel、owner close 时自动注销 listener。

## Backpressure

高频 event 不允许无界派生 task。EventRuntime 内建这些 policy：

- `throttle(duration)`：固定窗口最多处理一次。
- `debounce(duration)`：稳定后处理最后一次。
- `coalesceBy(key)`：按 player/entity/chunk 合并状态。
- `dropIfBusy(key)`：同 key 任务未完成时丢弃新 event。
- `queueLatest(key)`：运行中时记录最后状态，完成后补跑一次。

适用高频事件：

- `PlayerMoveEvent`
- `VehicleMoveEvent`
- `InventoryClickEvent`
- `AsyncChatEvent`
- chunk/world events

## Implementation

EventRuntime 内部隐藏：

- Bukkit/Paper `PluginManager.registerEvent` 和 HandlerList 注销。
- priority、ignoreCancelled、MONITOR read-only enforcement。
- domain event bus。
- external listener adapter。
- subscription registry。
- scope close propagation。
- await-event continuation。
- filter/expiry ordering。
- event-task backpressure。
- handler timing and failure report。

业务 Module 不直接实现 Listener class，也不直接调用 HandlerList unregister。

## Seam 和 Adapter

外部 Seam：

- `EventRuntime` Interface -> Bukkit/Paper event system。
- `DomainEventBus` Interface -> in-memory event dispatch。
- `ExternalListenerAdapter` -> Dynmap/Essentials/XConomy/ProtocolLib 等非 Bukkit HandlerList。

Adapter：

- `BukkitEventAdapter`: Bukkit/Paper event 注册和注销。
- `DomainEventAdapter`: framework/domain event 分发。
- `ExternalListenerAdapter`: 第三方 listener register/unregister。
- `PacketEventAdapter`: ProtocolLib packet listener，可复用 EventRuntime scope。
- `TestEventAdapter`: deterministic fake event bus。

Adapter 只满足 EventRuntime 的 Seam，不进入业务 Interface。

## 协作关系

- Lifecycle/OwnerRuntime: subscription 和 scope 都是 owner child。
- TaskRuntime: event handler 派生 task，task 可 await event。
- CommandRuntime: command 可开启交互式 EventScope。
- GuiRuntime: inventory/chat/player quit 事件先进入 EventRuntime，再由 GuiRuntime 做 page/component routing。
- TextRuntime: timeout、取消、错误消息走 message key。
- DiagnosticsRuntime: 汇总 event count、drop count、handler duration、last error。

## Diagnostics

Event diagnostics 应包含：

- event type。
- owner path。
- priority。
- ignoreCancelled。
- affinity。
- filter hit count。
- handled count。
- dropped/coalesced count。
- average/max handler duration。
- last exception。
- active scopes。

示例：

```text
event PlayerMoveEvent rail.detector
owner=module:rail-area/detector
policy=coalesceBy(player) handled=134 dropped=812 max=0.7ms
```

## Harness

`EventHarness` 需要覆盖：

- `ignoreCancelled` 行为。
- priority 顺序。
- MONITOR read-only diagnostic。
- seen/matched/handled expiry 区别。
- once/until/timeout 自动关闭。
- owner close 注销 subscription。
- async event affinity guard。
- await event 命中、timeout、cancel。
- dropIfBusy/coalesceBy/throttle/debounce。
- domain event owner binding。
- external listener adapter close。

## 开放问题

1. 默认 expiry 语义是否只保留 `handled(n)`，把 `seen/matched` 做高级选项。
2. MONITOR read-only 是开发期 warning，还是测试/strict mode fail fast。
3. async event 默认 affinity 如何推导，哪些 event 必须显式声明。
4. DomainEventBus 是否支持异步 dispatch，还是统一通过 TaskRuntime。
5. ProtocolLib packet listener 是否放 EventRuntime，还是 IntegrationRuntime 提供独立 Adapter 后转接。
