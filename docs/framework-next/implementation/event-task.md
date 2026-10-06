# Event And Task

## 是什么

EventRuntime 管被动触发。TaskRuntime 管主动执行。两者必须共用 owner、scope、affinity、policy、diagnostics。

事件 handler 经常启动任务；任务里也经常等待局部事件。它们不能分开设计。

## 相关工作和取舍

### Bukkit/Paper Listener

原生 Listener 提供完整平台语义：priority、ignoreCancelled、MONITOR、cancellable。但 lifecycle、临时作用域和 diagnostics 弱。

### subscribe DSL

本地化、scope 化是正确方向，但必须补齐 ignoreCancelled、async affinity、backpressure、event-task bridge。

### Reactive stream

throttle/debounce/coalesce 很适合高频事件，但完整 reactive 库会让公共接口过宽。结论：只吸收少量高价值操作符。

### MCCoroutine / FoliaLib

Coroutine 适合作为内部实现。FoliaLib 证明 location/entity scheduler 是真实需求。但公共 API 应表达 owner + affinity + policy，而不是直接暴露库 API。

## 开发者用法

事件：

```kotlin
on<PlayerMoveEvent>("market-region-hint") {
    priority(EventPriority.LOW)
    filter("block-changed") { from.block != to.block }
    requirePermission("market.use")
    filter("inside-market") { shops.isMarketRegion(to.snapshot()) }
    throttle(250.milliseconds, key = { player.uniqueId })

    run {
        shops.updateNearbyHint(player, to.snapshot())
    }
}
```

debounce snapshot：

```kotlin
on<PlayerMoveEvent>("save-last-market-position") {
    filter("block-changed") { from.block != to.block }

    debounce(1.seconds, key = { player.uniqueId }) {
        snapshot {
            LastPositionSnapshot(player.uniqueId, to.snapshot())
        }
    }

    run { snapshot: LastPositionSnapshot ->
        shops.saveLastPosition(snapshot.playerId, snapshot.location)
    }
}
```

任务：

```kotlin
val profile = tasks.io("load-profile") {
    profiles.load(player.uniqueId)
}

tasks.entity(player, "apply-profile") {
    player.sendMessage(text.welcome(profile.displayName))
}
```

周期任务：

```kotlin
every(5.minutes) {
    shops.flushDirtySnapshots()
}
```

临时事件 scope：

```kotlin
eventScope("capture-area", owner = ctx.sender, ttl = 2.minutes) {
    val first = await<PlayerInteractEvent>("first-corner") {
        filter { player == ctx.sender && action.isLeftClick }
    }
    val second = await<PlayerInteractEvent>("second-corner") {
        filter { player == ctx.sender && action.isRightClick }
    }

    tasks.io("save-capture") {
        shops.saveCapture(ctx.sender.uniqueId, first.clickedBlock, second.clickedBlock)
    }
}
```

race：

```kotlin
when (val result = events.race("rename-item", owner = ctx.sender, timeout = 30.seconds) {
    branch("chat") {
        await<AsyncChatEvent> { filter { player == ctx.sender } }
    }
    branch("close") {
        await<InventoryCloseEvent> { filter { player == ctx.sender } }
    }
}) {
    is RaceResult.Winner<AsyncChatEvent> -> rename(result.event)
    is RaceResult.Winner<InventoryCloseEvent> -> cancel()
    is RaceResult.Timeout -> cancel()
}
```

## 内部方案

Event pipeline：

```text
platform/domain event
-> adapter normalizes event facts
-> EventRuntime finds subscriptions
-> priority and cancellation policy
-> ordered filters
-> backpressure policy
-> snapshot if delayed
-> handler scope
-> task bridge if needed
-> diagnostics counters
```

Task pipeline：

```text
task declaration
-> owner child
-> affinity resolver
-> executor/scheduler selection
-> timeout/budget/backpressure
-> structured outcome
-> close resource
-> diagnostics report
```

Event expiry：

- public 初版只暴露 handled count。
- `seen/matched` 可作为 diagnostics 内部指标，不作为第一屏 API。

MONITOR：

- 开发期 warning。
- strict/test mode fail fast。

async event：

- 已知 Bukkit async event 默认不允许直接触碰 mutable Bukkit state。
- 需要 Bukkit 操作时必须显式切到 `tasks.main/entity/region`。

## 测试

- filter order。
- once-by-handled。
- throttle/debounce。
- snapshot prevents mutable event retention。
- owner close unregisters listener。
- task timeout/cancel/failure。
- join/race loser cleanup。
- async event guard。
- Paper adapter priority/ignoreCancelled。

