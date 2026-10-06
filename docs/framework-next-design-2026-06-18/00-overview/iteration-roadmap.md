# 迭代路线

## 设计节奏

当前阶段不进入代码实现，也不讨论插件切换路径。正确路线是先把事实源、API 草案和测试 harness 设计清楚，再决定是否进入实现。

## V0: 设计固化和证据补齐

目标：

- 保持现有代码不动，完善本文档包。
- 以 `00-overview/unified-framework-design.md` 作为主设计入口。
- 以 `01-research/research-assets.md` 保留调研、博客和推广素材。
- 为每个现有模块列出 public surface、内部实现、风险和设计候选。
- 确认哪些插件必须保持二进制/源码兼容。

验收：

- 有模块 API 清单。
- 有插件使用矩阵。
- 有公共接口风险列表。
- 统一设计能覆盖 lifecycle/task/event/gui/command/config/text/network/persistence/cache/diagnostics/integration。

## V1: 测试和诊断先行

目标：

- 重新启用或补充 core/modules 的关键测试路径。
- 增加 scheduler、config、event scope、GUI routing、network policy 的 harness。
- 先测出当前行为，再谈替换。

验收：

- `BaseModule` lifecycle、`ITaskHandler` outcome、config merge/cache、GUI event routing 有锁定测试。
- root build 禁用 Test 的决策被记录和重新评估。
- 文档区分 source-verified 和 runtime-verified 结论。

## V2: Resource Owner Tree

目标：

- 引入 owner tree 作为兼容能力，不立即替换所有模块。
- 任务、事件、GUI session、network call、cache 都能挂 owner。

验收：

- plugin disable 后 diagnostics 显示无未关闭 owner。
- 现有 `bind`/`Terminable` 能映射到新 owner。
- 不破坏现有插件启动。

## V3: Scheduler Outcome 和 Affinity

目标：

- 用 `TaskOutcome<T>` 替换 `null` 信号。
- 调度 API 增加 affinity、timeout、cancel reason、dependency failure。
- 提供 Paper adapter，Folia adapter 只留接口和测试 fake。

验收：

- 任务链失败不再进入 `getNow(null)!!`。
- async -> main -> async 的常见路径有简洁 API。
- 能按 owner 列出 active tasks。

## V3b: EventRuntime 与 EventTaskBridge

目标：

- 把事件作为和任务同级的 runtime 能力写清楚。
- 设计 event scope、ignoreCancelled、priority、once/until/timeout、backpressure。
- 设计 event 中启动 task、task 中 await event 的协作模型。

验收：

- API 草案能覆盖 module scope、player scope、page scope、task scope。
- 高频事件有 throttle/debounce/coalesce/drop-if-busy 语义。
- `EventDSLBuilder.expireAfter` 这类语义问题在新设计中有明确替代。
- domain event 和第三方 listener adapter 有 owner 模型。

## V4: Config Snapshot 和 Reload Report

目标：

- 把 `clearCache + touchWithMerge + parse + setupLanguageManager + super.reloadPlugin` 收束为 runtime reload。
- 配置和消息都生成 report。

验收：

- reload 流程 API 草案能解释至少两个现有插件的启动和重载需求。
- reload 失败时保留旧快照。
- 管理员命令能输出 reload 摘要。

## V5: GUI Runtime 重构

目标：

- 修复 event routing owner。
- 分离 page scheduler、component scheduler 和 close 责任。
- 引入 render model、diff render、data generation cancellation。

验收：

- 多 session、多页面、异步分页、旧请求丢弃都有测试。
- 不再依赖 global current manager。
- 组件 close 不会关闭页面 scheduler。

## V6: Cache Runtime 和 Batch Writer

目标：

- 统一缓存声明、scope invalidation、shared maintenance。
- 提供 batch writer 给 Foundation/DynamicEconomy/ExternalBook 这类持久化路径。
- 把 Caffeine 这类本地缓存能力作为 Adapter，而不是把 `Map` 或具体 cache 库泄漏给业务。

验收：

- SmartCache 类能力不再需要每实例线程。
- reload 按 config version/scope 清缓存。
- batch queue、flush latency、failure 可诊断。

## V7: Command Metadata 和 plugin.yml 生成

目标：

- 命令、参数、权限、usage、aliases、i18n key、suggestions、help 和 plugin yml 构建产物建立生成关系。
- 明确 Cloud annotations 是主要 Adapter，Aikar ACF 是声明式体验参照。
- 执行器声明线程 affinity。

验收：

- plugin spec 草案能生成静态 `plugin.yml` / `paper-plugin.yml`。
- 构建校验能证明 yml 产物与代码声明一致。
- handler/parser/suggestion provider 的 `ThreadAffinity` 规则明确。
- command 中启动 task、等待 event、打开 GUI 的 owner 关系明确。
- 权限不足、参数错误、异步失败统一渲染。
- reload command 使用 runtime reload report。

## V8: Folia-ready Adapter

目标：

- 在不承诺所有业务逻辑已 Folia-safe 的前提下，把 scheduler adapter 补齐。
- 让 `Region`、`Entity` affinity 在 Paper 和 Folia 上有明确行为。

验收：

- Paper 下 Region/Entity 退化到主线程。
- Folia fake/集成环境下可验证 region/entity scheduling。
- 文档明确哪些 API 不可在 Folia 下使用。

## V9: Diagnostics 和 Integration Runtime

目标：

- 给 owner tree、tasks、listeners、GUI sessions、http clients、cache、persistence queue 提供 health dump。
- 把 Vault、ProtocolLib、WorldEdit 等第三方插件能力建模为 typed capability。
- soft dependency、provider health、plugin disable、capability invalidation 都可诊断。

验收：

- 管理员命令或测试 harness 能列出当前 framework resources。
- 外部 provider 不可用时能返回明确状态和建议，而不是散落 NPE。
- packet/world-edit 类高风险能力必须显式声明 owner、scope 和风险级别。

## 决策记录

### D1: 当前只迭代设计，不进入代码实现

原因：框架边界还需要继续打磨。当前最有价值的是把 `PluginRuntime`、owner tree、调度、配置、GUI、持久化和 plugin spec 的设计讲清楚。

### D2: 调度接口优先于模块抽象

原因：Minecraft 插件最容易出错的是主线程/异步边界。模块抽象再漂亮，如果调度仍然弱，框架仍会把复杂度推给业务。

### D3: 配置默认 YAML，其他格式插件化

原因：Paper 生态默认 YAML，现有代码和文档也大量围绕 YAML/template/merge。多格式是能力，但不是核心事实源。

### D4: GUI 作为 runtime，而不是 DSL 工具包

原因：现有 GUI 已经包含 session、page stack、component tree、data provider、event bubbling 和 protection。继续当工具包会放大所有权和关闭顺序问题。

### D5: Folia 先设计接口，再承诺兼容

原因：Folia 改变线程模型。只加 `folia-supported: true` 没有意义。需要 adapter、测试和业务 API 审计。

### D6: 第三方库只能启发 Adapter，不能倒推核心 Interface

原因：MCCoroutine、FoliaLib、Configurate、Okaeri、Ktor、OkHttp、HikariCP、Caffeine、MockBukkit、Vault、ProtocolLib、WorldEdit 等项目都解决了局部问题，但真正稳定的框架事实仍然是 owner、scope、affinity、policy、snapshot、diagnostics。核心 Interface 应围绕这些事实设计，具体库只影响 Adapter 和实现成本。

### D7: 统一设计收束分散发现，调研材料保留为资产

原因：前几轮文档已经积累了平台约束、社区痛点、现有代码隐含需求、event/gui/command 比较和其他能力相关工作。如果这些内容只停留在独立文档里，会变成发现清单，而不是框架设计。因此当前以 `00-overview/unified-framework-design.md` 收束成一套整体方案，同时用 `01-research/research-assets.md` 保存未来博客、推广和外部对比素材。

## 拍板状态

这些问题已经集中记录在 [decision-recommendations.md](decision-recommendations.md)：

1. Folia 是中期目标。
2. build-time descriptor 采用 Gradle Kotlin DSL + KSP + 生成任务校验。
3. root build 禁用 Test 先按历史遗留处理，新框架必须建设 MockBukkit/Testcontainers harness。
4. DB PersistenceRuntime 作为 optional runtime module，不进入 framework core。
5. plugin yml 生成任务按目标平台生成，未知或多平台时同时生成并校验。

## 下一轮建议

优先做一个 read-only API surface map：

- 列出 core/config/i18n/gui/network 的 public classes/functions。
- 标出哪些是业务真正高频使用，哪些是内部泄漏。
- 标出哪些能力应该进入下一轮 API 草案。

然后按深 Module 补草案：Lifecycle/OwnerRuntime、TaskRuntime、EventRuntime、CommandRuntime、GuiRuntime、plugin spec 生成静态 yml、ConfigRuntime、TextRuntime、NetworkRuntime、Persistence/CacheRuntime、DiagnosticsRuntime、IntegrationRuntime。每份草案都要说明 Interface 是什么、Implementation 藏了什么、Seam 在哪里、Adapter 怎么接入、测试 harness 如何验证。
