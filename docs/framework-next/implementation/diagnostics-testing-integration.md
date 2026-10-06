# Diagnostics, Testing, And Integration

## Diagnostics 是什么

DiagnosticsRuntime 是框架的操作面。它必须回答：

- 当前有哪些 owner。
- 哪些 task/listener/gui/request/cache/writer 还活着。
- 哪些回调超预算。
- 最近 reload 做了什么。
- 哪些 config/message key 有问题。
- 哪些 external capability 缺失或 degraded。
- 哪些队列、cache、client 正在拖慢插件。

## 开发者和管理员用法

```kotlin
diagnostics.debug {
    field("screen", screenName)
    field("player", player.name)
    field("slot", slot)
}
```

管理员命令：

```text
/market diag
/market diag export
/market reload
```

默认权限：

- diagnostics command 内建，但需要 admin permission。
- 可以在 config 中关闭。
- JSON export 默认到 `plugins/<PluginName>/diagnostics/`。

report ring buffer：

- 每 plugin 默认 1024 条。
- 每 source 默认 256 条软上限。
- 默认保留 30 分钟。

## Testing 是什么

新项目不能依赖真实服务器手动测试。测试分层：

| 层级 | 目的 |
| --- | --- |
| unit/contract | 不依赖 Bukkit，验证 runtime 语义 |
| adapter | MockBukkit/fake Paper，验证 glue |
| golden artifact | 静态 yml、descriptor、schema、catalog 稳定性 |
| sample | 最佳实践插件可复制、可构建 |
| smoke | 真实 Paper 1.20.1+ 仅验证 adapter/packaging 边界 |

MockBukkit 是刚需，但不是全部事实源。Testcontainers 只在真正引入 DB adapter 时使用。

## Integration 是什么

IntegrationRuntime 负责外部插件能力：

- Vault。
- LuckPerms。
- PlaceholderAPI。
- ProtocolLib。
- WorldEdit/FAWE。
- Dynmap、Essentials 等。

公共模型：

```kotlin
val economy = integrations.capability<EconomyCapability>("vault-economy") {
    softDepend("Vault")
    required(false)
}
```

状态：

- `Available(provider, version)`。
- `Unavailable(reason)`。
- `Degraded(reason, fallback)`。
- `DisabledByConfig`。

## 相关工作和取舍

### spark

spark 看 JVM/server 热点。框架 diagnostics 看业务资源归属和 runtime 状态。两者互补。

### Bukkit ServicesManager / Vault

ServicesManager 证明 Minecraft 插件生态已有能力发现模型，但它全局、弱诊断、生命周期变化需要调用方处理。框架应包装成 typed capability。

### ProtocolLib

packet access 高风险，必须作为 explicit capability。packet listener 也要有 owner、priority、scope、diagnostics。

### WorldEdit

WorldEdit core/bukkit 分层是很好的 adapter 边界案例。大规模区域操作必须进入 TaskRuntime budget/backpressure。

## 内部方案

Diagnostics data model：

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

Integration pipeline：

```text
plugin descriptor softDepend
-> adapter probes provider
-> capability snapshot
-> module dependency resolution
-> state change event
-> diagnostics report
```

required capability 缺失默认 fail module，不 fail whole plugin，除非 plugin 声明 startup-critical。

## 测试

- diagnostics JSON schema。
- bounded ring buffer。
- source location debug mode。
- required/optional capability。
- provider state change event。
- missing soft dependency report。
- MockBukkit plugin enable。
- golden sample artifact verification。
- release gate task。

