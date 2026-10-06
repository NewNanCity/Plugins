# Development Slices

## 定位

本页把设计拆成可以直接进入开发排期的切片。每个切片都包含输入文档、交付范围、验收标准、测试入口和暂停条件。

原则：先建立深 Module 的骨架和可验证语义，再接平台 Adapter；先证明 owner、task、report 和 harness，后接 GUI、网络、持久化等高成本能力。

## Definition Of Ready

一个切片开始前必须具备：

- 对应 Runtime spec 已存在。
- Interface 不变量清楚。
- 至少一个 fake harness 场景清楚。
- 不涉及未拍板的公共 Interface 大改。
- 不需要线上副作用。

## Definition Of Done

一个切片完成时必须具备：

- Interface 或 descriptor 有最小实现。
- contract tests 覆盖成功、失败、关闭/取消路径。
- DiagnosticsRuntime 能看到关键状态或 report。
- README 或 sample 展示业务侧用法。
- 发现的开放问题写回对应 Runtime 文档。

## Slice 1: OwnerRuntime Foundation

输入：

- [lifecycle-owner-runtime.md](lifecycle-owner-runtime.md)
- [../00-overview/api-surface-map.md](../00-overview/api-surface-map.md)

交付：

- owner tree。
- owner state。
- resource binding。
- LIFO close。
- close report。
- fake owner harness。

验收：

- parent close 会关闭 child。
- resource close 失败被聚合到 report。
- owner close 后不能再绑定新 resource。
- diagnostics 能看到 owner/resource graph。

暂停条件：

- `Owner` 与现有 BasePlugin/BaseModule 的职责边界无法清晰切开。

## Slice 2: Diagnostics Skeleton

输入：

- [diagnostics-runtime.md](diagnostics-runtime.md)

交付：

- `DiagnosticSource` registry。
- `DiagnosticReport`。
- redaction pipeline。
- test sink。
- minimal health dump。

验收：

- Runtime 能注册 source。
- report 可按 severity/source/owner 查询。
- secret redaction 生效。
- JSON snapshot 可稳定断言。

暂停条件：

- report 模型需要暴露第三方库类型。

## Slice 3: TaskRuntime Contract

输入：

- [task-runtime.md](task-runtime.md)
- [test-strategy.md](test-strategy.md)

交付：

- affinity model。
- task spec/handle/outcome。
- fake scheduler。
- timeout/cancellation/backpressure。
- diagnostics reports。

验收：

- owner close 取消未完成 task。
- timeout 返回结构化 outcome。
- dependency failure 不进入下一段。
- fake tick 可 deterministic 推进。

暂停条件：

- Folia/Paper affinity 语义迫使公共 Interface 改动。

## Slice 4: EventRuntime Contract

输入：

- [event-runtime.md](event-runtime.md)

交付：

- scoped subscription。
- priority、ignoreCancelled、filter、expiry。
- event-task bridge。
- backpressure policy。
- fake event bus。

验收：

- owner close 注销 listener。
- expire after 语义有测试。
- high-frequency event coalesce/drop 可诊断。
- handler 失败进入 report。

暂停条件：

- 平台事件语义被 DSL 简化到无法表达原生行为。

## Slice 5: Plugin Spec And Command Contract

输入：

- [plugin-spec-generation.md](plugin-spec-generation.md)
- [command-runtime.md](command-runtime.md)

交付：

- plugin descriptor DSL。
- command descriptor。
- permission descriptor。
- static yml generation。
- command fake sender harness。

验收：

- duplicate command fail。
- command permission 未声明 fail。
- generated yml snapshot 稳定。
- command permission denied 走 TextRuntime key。

暂停条件：

- descriptor 无法覆盖 Paper/Bukkit loader 需要的静态字段。

## Slice 6: Config And Text Snapshots

输入：

- [config-runtime.md](config-runtime.md)
- [text-runtime.md](text-runtime.md)

交付：

- config descriptor/schema/snapshot。
- reload report。
- message key/catalog/snapshot。
- fallback and placeholder validation。
- cache invalidation event。

验收：

- validation failure 保留旧 config snapshot。
- invalid message catalog 保留旧 catalog。
- reload report 可被 diagnostics 查询。
- missing placeholder 返回 render failure。

暂停条件：

- 为保留文件注释导致 schema/snapshot 语义不清。

## Slice 7: GuiRuntime Core

输入：

- [gui-runtime.md](gui-runtime.md)

交付：

- session/page/component owner。
- render model。
- render diff。
- event routing。
- fake inventory harness。

验收：

- player close/page close 清理 owner。
- stale async data 不覆盖新页面。
- click/drag/shift/drop 保护路径有测试。
- render failure 进入 diagnostics。

暂停条件：

- GUI component 设计开始变成任意 UI 框架，而不是 inventory runtime。

## Slice 8: NetworkRuntime Contract

输入：

- [network-runtime.md](network-runtime.md)

交付：

- named client。
- request spec/outcome。
- timeout/retry/rate limit/bulkhead。
- redaction。
- fake network adapter。

验收：

- timeout、retry、rate limit、circuit 都可 deterministic 测。
- secret 不进入 logs/report。
- owner close 取消 request。
- streaming download respects max size。

暂停条件：

- 业务 Interface 需要直接暴露 Ktor/OkHttp client。

## Slice 9: Persistence And Cache Contract

输入：

- [persistence-cache-runtime.md](persistence-cache-runtime.md)

交付：

- repository executor。
- transaction scope。
- batch writer。
- cache spec/handle。
- ttl/version invalidation/single-flight。

验收：

- transaction failure rollback。
- batch writer close drain 有 report。
- cache max size/ttl/negative cache 生效。
- config version changed invalidates cache。

暂停条件：

- PersistenceRuntime 是否进入 core 的产品边界未拍板。

## Slice 10: IntegrationRuntime Contract

输入：

- [integration-runtime.md](integration-runtime.md)

交付：

- typed capability。
- provider registry。
- required/optional state。
- provider health。
- fake provider harness。

验收：

- required missing 按声明失败。
- optional missing 关闭功能但不失败。
- provider state change 通知 consumer。
- diagnostics 显示 capability health。

暂停条件：

- capability key 管理模式未拍板且影响多个插件。

## Slice 11: Cross-Runtime Sample

输入：

- 全部 Runtime specs。

交付：

- 一个最小 sample plugin descriptor。
- command -> task -> config/text -> event/gui 的组合样例。
- network/cache 或 persistence/cache 的一个组合样例。
- health dump golden sample。

验收：

- sample 不需要手写静态 yml。
- command help、permission、message key 来自 descriptor/catalog。
- owner close 后 task/listener/gui/client/cache 无泄漏。
- diagnostics health dump 展示完整路径。

暂停条件：

- sample 为了跑通而绕过 Runtime Interface。

## Slice 12: Adapter Pass

输入：

- 已通过 contract tests 的 Runtime。

交付：

- Paper/Bukkit task/event/command/gui Adapter。
- Ktor network Adapter。
- YAML/Text Adapter。
- cache/persistence Adapter 的最小实现。
- integration provider Adapter 的首批样例。

验收：

- Adapter 不改变 contract tests 的语义。
- Adapter-specific tests 覆盖平台绑定。
- diagnostics 能区分 Interface failure 和 Adapter failure。

暂停条件：

- Adapter 需要把第三方类型扩散到业务 Interface。

## Execution Matrix

| Slice | 依赖 | 主要风险 | 最小验收证据 |
| --- | --- | --- | --- |
| 1 Owner | 无 | 生命周期边界过大 | owner close report |
| 2 Diagnostics | 1 | report 成本过高 | redacted snapshot |
| 3 Task | 1,2 | affinity 语义不清 | fake scheduler tests |
| 4 Event | 1,2,3 | 高频事件背压 | scoped listener tests |
| 5 Plugin/Command | 1,2 | descriptor 不完整 | yml golden sample |
| 6 Config/Text | 1,2,3 | reload 原子性 | snapshot tests |
| 7 GUI | 1,2,3,4,6 | session 泄漏 | fake inventory tests |
| 8 Network | 1,2,3 | secret 泄漏和重试风暴 | redaction + timeout tests |
| 9 Persistence/Cache | 1,2,3,6 | close drain 和缓存一致性 | flush + invalidation tests |
| 10 Integration | 1,2,4 | provider 状态漂移 | capability state tests |
| 11 Sample | 1-10 | 样例绕过框架 | health dump + descriptor |
| 12 Adapter | 1-11 | 第三方类型泄漏 | adapter contract tests |
