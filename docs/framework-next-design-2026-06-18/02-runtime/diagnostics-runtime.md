# DiagnosticsRuntime

## 定位

DiagnosticsRuntime 管 owner graph、resource graph、runtime report、health dump、structured log bridge、admin diagnostics command、test snapshot 和 profiling hooks。

它解决的是框架可运维性问题：当事件监听没有注销、任务卡住、GUI session 泄漏、缓存过大、网络重试风暴或配置 reload 失败时，开发者不应该靠翻散乱日志猜状态，而应该能从统一诊断入口看到事实。

## Interface

核心概念：

- `DiagnosticSource`: 每个 Runtime 暴露诊断数据的入口。
- `DiagnosticSnapshot`: 某一时刻的结构化状态。
- `DiagnosticReport`: 面向开发者和管理员的报告。
- `HealthCheck`: 可执行的健康检查。
- `ResourceGraph`: owner、resource、task、listener、session、client、cache 的归属图。
- `DiagnosticSink`: console、file、command、test harness 输出目标。

```kotlin
interface DiagnosticSource {
    val name: String
    fun snapshot(request: DiagnosticRequest): DiagnosticSnapshot
}

ctx.diagnostics.registerSource(ctx.owner, "task-runtime", taskRuntime)
ctx.diagnostics.report("slow-task") {
    owner(task.owner)
    severity(DiagnosticSeverity.Warning)
    field("affinity", task.affinity)
    field("durationMs", task.duration.inWholeMilliseconds)
}
```

Interface 不变量：

- 每条 report 必须有 source、owner、severity、timestamp。
- report 中的 secret 必须先 redaction。
- diagnostics 采样不能显著增加主线程负担。
- snapshot 必须能在测试中稳定断言。
- health check 不能产生破坏性副作用。
- diagnostics 失败不能让业务流程二次失败。

## Snapshot Model

统一 snapshot 建议包含：

```kotlin
data class DiagnosticSnapshot(
    val generatedAt: Instant,
    val plugin: PluginIdentity,
    val owners: OwnerGraphSnapshot,
    val sources: Map<String, SourceSnapshot>,
    val reports: List<DiagnosticReport>,
    val health: List<HealthCheckResult>
)
```

`SourceSnapshot` 由各 Runtime 自己定义 detail，但外层保持统一字段：

- name。
- owner path。
- status。
- counters。
- durations。
- recent failures。
- suggestions。

## Report Types

基础 report 类型：

- `startup`: 插件启动摘要。
- `shutdown`: 关闭顺序和未释放资源。
- `reload`: config/text/plugin descriptor reload report。
- `slow-task`: 任务超预算。
- `event-backpressure`: 高频事件降载。
- `gui-session`: session/page/component 状态。
- `network`: request/client/service health。
- `persistence`: datasource、transaction、writer。
- `cache`: size、hit rate、eviction。
- `integration`: capability provider 状态。
- `test`: harness 断言失败上下文。

Report 应同时支持机器可读和人类可读：

```text
diagnostics owner=plugin:tpa/module:session severity=warning
type=event-backpressure event=PlayerMoveEvent dropped=182 coalesced=41
suggestion=raise filter specificity or move work behind coalesced task
```

## Health Dump

Admin 命令和测试都应能生成 health dump：

```text
/newnan diag
/newnan diag owner plugin:tpa/module:gui
/newnan diag source task-runtime
/newnan diag export json
```

输出层级：

1. plugin summary。
2. owner tree。
3. critical/warning reports。
4. runtime source summary。
5. slowest tasks/queries/renders。
6. open resources。
7. recent reload/request/flush reports。

## Implementation

Implementation 隐藏：

- source registry。
- report ring buffer。
- redaction pipeline。
- owner graph traversal。
- resource graph traversal。
- health check scheduler。
- structured log bridge。
- command renderer。
- JSON export。
- test snapshot serializer。

业务 Module 不直接拼接诊断字符串，只注册 source 或写结构化 report。

## Seam 和 Adapter

外部 Seam：

- `DiagnosticSource` -> Runtime-specific snapshot provider。
- `DiagnosticSink` -> console/file/command/test output。
- `HealthCheck` -> Runtime or integration check。
- `ResourceGraph` -> OwnerRuntime。

Adapter：

- `LoggerDiagnosticSink`: 写入框架 logger。
- `CommandDiagnosticSink`: 渲染给 command sender。
- `JsonDiagnosticSink`: 输出 JSON 文件。
- `TestDiagnosticSink`: 供 harness 断言。
- `ProfilerAdapter`: 对接外部 profiling 工具或 timings。

Adapter 要求：

- command 输出必须短，默认只展示异常和摘要。
- JSON 输出必须包含结构化字段，不能只放 message。
- secret redaction 在 sink 前完成。
- sink failure 只能进入 diagnostics internal report，不能影响业务。

## 协作关系

- Lifecycle/OwnerRuntime: 提供 owner/resource graph 和关闭报告。
- TaskRuntime: slow task、queue、affinity、cancellation。
- EventRuntime: listener、scope、backpressure。
- CommandRuntime: diagnostics command。
- GuiRuntime: session/page/component/render。
- ConfigRuntime/TextRuntime: reload report。
- NetworkRuntime/PersistenceRuntime/CacheRuntime: 外部 I/O 和状态统计。
- IntegrationRuntime: capability/provider health。

## Performance Budget

DiagnosticsRuntime 自身要有成本约束：

- 常规 report 写入 O(1)。
- snapshot 默认按需生成，不在每 tick 全量扫描。
- 高频 counter 使用轻量聚合。
- command health dump 可以分页或截断。
- 详细 export 运行在 AsyncCpu/AsyncIo，避免阻塞 main affinity。

## Harness

`DiagnosticsHarness` 覆盖：

- 注册多个 source 后生成 snapshot。
- report ring buffer 按容量淘汰。
- secret redaction 生效。
- owner graph 可定位未释放 resource。
- health check failure 不影响业务。
- command sink 输出关键摘要。
- JSON sink 可稳定序列化。
- slow task report 带 owner、affinity、duration。
- test sink 可断言 report type/count/severity。

## 开放问题

1. diagnostics command 是否作为 core 内建命令。
2. JSON export 默认保存到哪个目录。
3. report ring buffer 默认容量和保留周期。
4. 是否提供 Prometheus/JMX 之类外部 metrics Adapter。
