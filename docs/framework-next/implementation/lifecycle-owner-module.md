# Lifecycle, Owner, And Module

## 是什么

Lifecycle/Owner/Module 是框架的地基。

Lifecycle 管插件和模块状态机。Owner 管所有长期资源归属。Module 是业务能力的组织单元。

任何会跨过当前函数调用继续存在的东西都必须挂到 owner：

- event subscription。
- scheduled task。
- coroutine branch。
- GUI session/page。
- network request/client。
- cache。
- batch writer。
- integration capability handle。

## 相关工作和取舍

### Bukkit/Paper 原生生命周期

`onEnable/onDisable` 太粗。Paper Lifecycle API 提醒我们早期注册、bootstrap owner、runtime owner 应分阶段处理。

### lucko/helper

helper 展示了 events、scheduler、commands、metadata 组合工具的价值，但它更像 toolkit，不是完整 runtime。我们吸收低样板体验，但 owner tree 和 reports 必须成为核心。

### 旧 BaseModule/Terminable

旧模型证明“自动清理”是真需求，但不兼容新入口。新框架不保留旧 `BaseModule.bind(Terminable)`，而是用统一 OwnerRuntime。

## 开发者用法

```kotlin
class MarketModule(
    private val config: MarketConfig,
    private val text: MarketText,
    private val shops: ShopService
) : Module {
    override fun build() = module("market") {
        on<PlayerQuitEvent>("close-viewer") {
            run { shops.closeViewer(player.uniqueId) }
        }

        every(5.minutes) {
            shops.flushDirtySnapshots()
        }
    }
}
```

开发者不手动传 `owner`。模块 DSL 自动创建 module owner，并把 command/event/task/gui 绑定到它。

## 内部方案

状态机：

```text
Discovered
-> Described
-> Configured
-> Installed
-> Starting
-> Running
-> Reloading
-> Running
-> Closing
-> Closed
```

核心类型：

```kotlin
interface Owner : AutoCloseable {
    val id: OwnerId
    val path: OwnerPath
    val parent: Owner?
    fun child(name: String, tags: OwnerTags = OwnerTags.empty()): Owner
    fun bind(resource: RuntimeResource): BoundResource
    fun close(reason: CloseReason): CloseReport
}

interface Module {
    fun build(): ModuleDeclaration
}
```

Module dependency graph：

- required dependency 缺失: fail module。
- optional dependency 缺失: module degraded，并进入 diagnostics。
- module start/reload/close 按拓扑顺序执行。
- close 反向执行，幂等。

Close timeout 默认：

- plugin: 10s。
- module: 5s。
- GUI session/page: 2s。
- request/writer: 3s。
- critical writer 可显式提高到 10s。

## 测试

- owner child close order。
- owner close 幂等。
- resource leak detection。
- module dependency graph required/optional。
- start failure 后 close 已启动模块。
- reload failure 保留旧 snapshot。
- plugin disable 后 active resource 为 0。

