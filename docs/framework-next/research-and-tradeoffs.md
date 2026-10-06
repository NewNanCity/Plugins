# Research And Trade-Offs

本页集中保留相关工作调研和我们吸收/拒绝的点。它可以作为后续技术博客、推广材料和设计说明的素材库。

## Platform

### Paper Scheduler

吸收：

- tick budget 是硬边界。
- 同步任务会影响 TPS。
- 异步任务不能随意访问 Bukkit API。

设计映射：

- `Affinity.Main`、`AsyncIo`、`AsyncCpu`。
- main-thread budget diagnostics。
- task owner cancellation。

### Folia

吸收：

- Region/Entity scheduler 是未来方向。
- 单主线程假设不应进入公共 API。

拒绝：

- 第一版本承诺 Folia support。

设计映射：

- 近期只设计 `LocationSnapshot`、`Affinity.Region`、`Affinity.Entity`。
- Paper adapter 可先映射回 main。

## Command

### Incendo Cloud

吸收：

- parser。
- suggestion。
- exception handling。
- annotation/declaration model。
- execution coordinator 思路。

拒绝：

- 直接把 Cloud 类型作为业务事实源。
- 让 Cloud 独自负责 plugin metadata generation。

### Aikar ACF

吸收：

- 声明式体验。
- validation、completion、help、conditions 与命令就地关联。

拒绝：

- 直接切换到 ACF 作为底层依赖目标。

### CommandAPI

吸收：

- 强类型参数。
- 执行前校验。
- Brigadier/vanilla UI 思路。

拒绝：

- 不生成 plugin yml metadata 的路径。

## GUI

### SmartInvs

吸收：

- inventory manager。
- page system。
- slot iterator。
- click action。

拒绝：

- 每页自由 tick update。
- 与 owner/task/text/diagnostics 脱节的 manager。

### Triumph GUI

吸收：

- per-item/per-slot action。
- paginated/scrolling/storage。
- 简洁业务接口。

拒绝：

- 简洁接口背后缺少 owner、generation、diagnostics。

### InventoryFramework

吸收：

- pane/layout 思想。

拒绝：

- XML 作为第一配置方式。

### devnatan inventory-framework

吸收：

- 平台已知问题由框架处理。
- secure/robust API 是 GUI 的核心价值。

拒绝：

- 为跨平台牺牲当前 Paper 1.20.1+ 优先目标。

## Config / Text

### Jackson

吸收：

- Kotlin data class mapping。
- JSON/typed conversion。
- 现有生态成熟。

拒绝：

- 把纯对象映射当成完整配置系统。
- 忽略注释、顺序、reload report。

### Configurate

吸收：

- node model。
- transformation。
- 多格式 adapter。

拒绝：

- 让业务直接操作 node。

### Okaeri Configs

吸收：

- typed config class。
- durable comments。
- Minecraft 插件配置体验。

拒绝：

- 没有 framework-level snapshot/report 的直接使用方式。

### Adventure / MiniMessage

吸收：

- Component 作为输出。
- MiniMessage 作为默认可编辑文本格式。

拒绝：

- 自动猜 legacy/plain/minimessage 作为默认运行时策略。
- 业务状态保存 MiniMessage 字符串。

## Task / Event

### Bukkit Listener

吸收：

- priority。
- ignoreCancelled。
- MONITOR。
- cancellable event 语义。

拒绝：

- 手动注册/注销作为主开发方式。

### Reactive streams

吸收：

- throttle。
- debounce。
- coalesce。
- drop-if-busy。

拒绝：

- 完整 reactive library 作为 public API。

### MCCoroutine

吸收：

- coroutine 适合作为内部实现。
- lifecycle scope 重要。

拒绝：

- 裸 CoroutineScope 暴露给业务长期持有。

## Optional Runtimes

### Caffeine

吸收：

- size/weight eviction。
- TTL/refresh。
- async loading。

拒绝：

- 让 cache API 变成 Caffeine API。

### Ktor / OkHttp

吸收：

- Ktor plugin model。
- OkHttp engine/call cancellation。
- per-host/per-client concurrency。

拒绝：

- 业务直接持有长期 HttpClient。
- 默认记录 request body。

### HikariCP / Exposed / Ktorm

吸收：

- pool/transaction/SQL DSL 是现实需求。

拒绝：

- 框架 core 绑定 ORM 或 driver。

## Diagnostics / Testing

### MockBukkit

吸收：

- adapter tests 和 plugin load tests。

拒绝：

- 把 MockBukkit 当成所有正确性的事实源。

### spark

吸收：

- 性能诊断是 Minecraft 运维常态。

拒绝：

- 用 profiler 替代框架 resource diagnostics。

