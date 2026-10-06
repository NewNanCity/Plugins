# 调研资产库

本页保留前面几轮调研材料，避免它们在整体设计重塑后丢失。它不是最终框架设计，也不是对外发布文案；它是后续写技术博客、设计说明、推广材料和对比文章时的素材库。

## 使用方式

这份材料有三种用途：

1. 作为设计证据：当统一设计里说“为什么要 owner tree、affinity、scope、policy、diagnostics”时，这里保存外部生态和现有代码压力。
2. 作为技术博客素材：可以拆成系列文章，解释 Minecraft 插件框架为什么不能只做 wrapper。
3. 作为框架推广素材：用清晰、克制的方式说明新框架如何吸收现有方案优点，并避免它们的常见失败模式。

对外使用时要注意：不要把第三方项目写成反面教材。正确姿态是“它们各自解决了局部问题，我们把这些经验整合成更适合 NewNanPlugins 的运行时设计”。

## 可复用核心叙事

### 叙事 A: 不是更短的 Bukkit wrapper，而是 Minecraft runtime

常见 wrapper 能减少样板代码，但往往没有解决真正难点：

- 资源属于谁。
- 什么时候释放。
- 哪些代码能在主线程、异步线程、区域线程或实体线程执行。
- 高频事件如何背压。
- 配置 reload 失败后如何保留旧快照。
- 服务器管理员如何看到插件成本和泄漏。

我们的新框架主张：业务插件不直接管理 Bukkit task、listener、Inventory、HttpClient、cache、database queue，而是通过 framework runtime 声明意图。Runtime 统一管理 owner、scope、affinity、policy、report 和 diagnostics。

### 叙事 B: 代码声明生成静态 yml，消除双维护

传统 Bukkit/Paper 插件需要同时维护：

- `plugin.yml` / `paper-plugin.yml`。
- command executor。
- tab completion。
- permissions。
- help/usage。
- i18n 描述。

Cloud、Aikar ACF、CommandAPI 都证明声明式命令是成熟方向。我们的补充是：运行时命令树和构建期静态 yml 应来自同一份代码声明。这样开发者不维护多份文件，框架可以校验产物是否和声明一致。

### 叙事 C: Event 和 Task 是一对，不是两个工具类

Minecraft 插件既有主动触发，也有被动触发：

- 主动触发：command、scheduled task、network callback、admin reload。
- 被动触发：player/event/inventory/chat/world/third-party events。

事件 handler 常常派生 task，task 也常常等待局部 event。新框架应把 EventRuntime 和 TaskRuntime 放到同一 owner tree 下，共享 cancellation、timeout、affinity、backpressure 和 diagnostics。

### 叙事 D: GUI 是 UI runtime，不是 Inventory helper

SmartInvs、Triumph GUI、IF、devnatan inventory-framework 都证明 GUI 封装是真需求。现有 NewNan GUI 也已经包含 session、page、component、paginated、scrollable、data provider、object protection、chat input。

下一代 GUI 不应只是 slot DSL，而应是完整 GuiRuntime：

- EventRouter: inventory event 到 page/component。
- RenderEngine: render model 到 ItemStack diff。
- DataRuntime: async provider、generation id、旧请求丢弃。
- SessionRegistry: player/session/page owner。
- Diagnostics: 打开的页面、慢渲染、未关闭 session。

### 叙事 E: Adapter 可替换，Interface 要稳定

相关项目各有价值：

- MCCoroutine 启发 coroutine lifecycle scope。
- FoliaLib 启发 location/entity scheduler adapter。
- Configurate/Okaeri 启发 typed config 和 durable comments。
- Ktor/OkHttp/Resilience4j 启发 HTTP client policy。
- HikariCP/Exposed/Ktorm/Caffeine 启发 persistence/cache adapter。
- MockBukkit/spark 启发测试和诊断。
- Vault/ProtocolLib/WorldEdit 启发 integration capability。

但业务 Interface 不应该被这些库倒推。核心 Interface 应围绕 Minecraft 运行时事实：owner、scope、affinity、policy、snapshot、report、diagnostics。

## 调研来源矩阵

| 主题 | 参考对象 | 可吸收能力 | 不应直接照搬 |
| --- | --- | --- | --- |
| Tick 和调度 | Paper Scheduling | 20 TPS、50ms tick、主线程预算、async guard | 只保留 `runSync/runAsync` 两档 |
| Folia | Paper Folia docs、FoliaLib | global/region/async/entity scheduler，location/entity affinity | 仅声明 supported 但不重塑线程模型 |
| Coroutine | MCCoroutine | suspendable commands/events/schedules，plugin lifecycle scope | 裸 `CoroutineScope` 暴露给业务长期保存 |
| Event | Bukkit/Paper Listener | priority、ignoreCancelled、MONITOR、cancellable semantics | 用 DSL 简化时丢失原生语义 |
| Command | Incendo Cloud、Aikar ACF、CommandAPI | annotations、parser、suggestions、help、exception handling、强参数 | 让业务强耦合某个第三方库 |
| GUI | SmartInvs、Triumph GUI、IF、devnatan inventory-framework | page、layout/pane、per-slot action、pagination、scrolling、platform issue handling | 只做 Inventory helper，不管理 owner 和 diagnostics |
| Config | Paper config、Configurate、Okaeri | schema、typed config、comments、format adapter、reload report | 让业务直接操作 mutable config node |
| Text/i18n | Adventure/MiniMessage | 可编辑 component text、tag resolver、audience rendering | 把 MiniMessage 字符串当业务状态 |
| Network | Ktor、OkHttp、Resilience4j | client plugins、auth/logging redaction、call cancellation、retry/circuit/bulkhead | 每个调用点重复写 timeout/retry |
| Persistence | HikariCP、Exposed、Ktorm | pool adapter、SQL DSL、transaction seam | 框架强制唯一 ORM |
| Cache | Caffeine | size/ttl/refresh/loading/invalidation policy | 把 cache 当 `Map`，无 owner 和失效策略 |
| Test | MockBukkit | Bukkit glue unit test、mock plugin/player | 以 mock 覆盖真实 tick/thread/platform 行为 |
| Profiling | spark | server performance profiler | 用 profiler 替代框架资源诊断 |
| Integration | ServicesManager、Vault、ProtocolLib、WorldEdit | capability discovery、provider health、platform-independent core + adapter | 全局散点拿 provider 或 packet manager |

## 可拆分技术博客题目

1. `为什么 Minecraft 插件框架不能只封装 BukkitScheduler`
   - 讲 tick、async、Folia、affinity、owner cancellation。

2. `从 Listener 类到 EventRuntime: Minecraft 插件事件系统的下一步`
   - 讲 priority、ignoreCancelled、scope、event-task bridge、backpressure。

3. `声明式命令不只是注解: command tree 如何生成 plugin.yml`
   - 讲 Cloud/Aikar/CommandAPI 的启发，以及 build-time static yml。

4. `Inventory GUI 应该是 Runtime，而不是一堆 Slot 回调`
   - 讲 page/session/render diff/data generation/object protection。

5. `配置 reload 为什么应该返回 Report，而不是只打印日志`
   - 讲 typed snapshot、validation、fallback、old snapshot retention。

6. `插件性能优化不是到处加缓存`
   - 讲 cache owner、invalidation、GC、ItemStack/Player 引用风险、Caffeine adapter。

7. `让服务器管理员看得懂插件在做什么`
   - 讲 DiagnosticsRuntime、owner tree、slow callbacks、active tasks、open GUI、network/persistence queues。

8. `Adapter 不是 Interface: 如何从生态库里提炼框架设计`
   - 讲 MCCoroutine、FoliaLib、Configurate、Ktor、Hikari、Caffeine、MockBukkit、Vault 等的取舍。

## 对外推广时可强调的框架卖点

- 用代码声明 plugin spec，生成静态 yml，减少文件双维护。
- 用 owner tree 自动管理 task、event、GUI、network、cache、persistence resource。
- 用 ThreadAffinity 让 Bukkit/Paper/Folia 线程归属显式化。
- 用 EventScope 表达临时事件监听，不再手写注册和注销。
- 用 TaskOutcome/Report 替代 `null`、日志和异常混合信号。
- 用 GuiRuntime 管 page/session/render/data，而不是每页重复处理 inventory event。
- 用 ConfigSnapshot 和 ReloadReport 让 reload 可审计。
- 用 DiagnosticsRuntime 让管理员能看到资源、成本和泄漏。
- 第三方库作为 Adapter，业务 Interface 保持稳定。

## 写作原则

- 先讲 Minecraft 插件开发者的真实痛点，再讲框架设计。
- 讲优劣，不拉踩。每个参考项目都有它解决得好的局部问题。
- 不宣称已经实现的能力，除非代码和测试已经证明。
- 对外文案区分“设计目标”“已实现能力”“验证中能力”。
- 保留来源链接，方便读者追溯。

## 与统一设计的关系

统一设计见 [../00-overview/unified-framework-design.md](../00-overview/unified-framework-design.md)。本页只保存调研资产和传播素材；统一设计才是当前框架方案的主入口。
