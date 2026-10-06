# 从现有代码反推的隐含需求

本页只记录“代码背后已经存在的真实需求”。它不等于现有实现都是对的，但它说明下一代框架必须承接哪些业务压力。

## R1: 热重载是核心能力，不是附加命令

证据：

- 多数插件都有 `reloadPlugin()`。
- 常见流程是 `configManager.clearCache()`、`setupLanguageManager(...)`、`super.reloadPlugin()`。
- 多个 admin command 直接调用插件 reload。
- `touchWithMerge("config.yml", createBackup = true)` 和 `parse<T>()` 反复出现。

需求：

- 框架应提供一次性的 `runtime.reload()`，内部处理配置快照、语言目录、模块 reload 顺序、失败回滚和报告。
- 业务模块只实现 `reload(ctx)`，不手写全局清缓存和语言管理器重建。
- reload 结果必须能告诉管理员：哪些配置被 merge、哪些模块成功、哪些模块失败、是否仍在旧快照上运行。

设计含义：

热重载需要成为 lifecycle 的正式阶段。否则模块初始化顺序、缓存一致性和错误恢复都会继续散在业务侧。

## R2: 插件启动需要两阶段模型

证据：

- 插件入口常在 `onPluginEnable()` 里先调用 `reloadPlugin()`，然后再创建业务模块。
- `BasePlugin.reloadPlugin()` 又负责 reload 子模块。
- 这造成“首次 reload 时模块还没创建、后续 reload 又要 reload 模块”的语义分裂。

需求：

- 第一阶段声明配置、语言、命令、权限、依赖。
- 第二阶段安装模块。
- 第三阶段 start，框架把已加载快照传给模块。
- reload 必须只发生在 `Installed/Running` 状态之后。

设计含义：

不能再把“读取配置”和“启动模块”混成一个可随意调用的方法。

## R3: 主线程和异步桥接是业务高频路径

证据：

- 大量代码使用 `runAsync` 后再 `runSync`。
- 外部图书、经济、基金会、MCron、TPA、GUI 等模块都需要 I/O 与 Bukkit 操作交替。
- Paper/Bukkit 约束要求 Bukkit API 多数场景在 tick 线程运行，数据库等重任务应离开主线程。

需求：

- 框架提供任务链 API，明确每段在哪个 affinity 上运行。
- 支持依赖、取消、超时、失败传播、owner 自动关闭。
- 禁止用 `getNow(null)!!` 这类方式把失败和空值混在一起。

设计含义：

调度器是框架主接口，不是工具函数。它要比现在的 `runSync/runAsync` 多承载线程归属、生命周期和诊断。

## R4: GUI 是复杂应用框架，不是简单 Inventory DSL

证据：

- GUI 目录支持 session、page、component、paginated、scrollable、data provider、object protection、chat input 等能力。
- 插件页面普遍有分页、返回按钮、缓存清理、异步加载、点击后刷新。
- 事件路由和 session storage 已经承担全局状态。

需求：

- GUI runtime 需要稳定的 page/session owner tree。
- 事件必须按 inventory/page/component 路由，不能依赖全局 current manager。
- 数据 provider 必须支持同步、异步、分页、无限滚动、取消、旧请求丢弃。
- 渲染需要 diff 和 generation id，避免异步结果覆盖新页面。

设计含义：

GUI 模块应该被视为一个小型 UI runtime。它需要自己的资源所有权、渲染模型和测试 harness。

## R5: 配置升级和兼容比多格式更重要

证据：

- `touchWithMerge`、backup、template merge、version compatibility 文档和测试已经存在。
- 业务配置大量使用 Bukkit 类型、ItemStack、Location、Material、UUID、BigDecimal。
- Paper 官方配置文档也强调默认 YAML、多文件配置、复杂 Bukkit 类型序列化和异步 I/O。

需求：

- 配置系统要以 schema、template、config upgrade、merge report 为中心。
- 多格式支持可以保留为 plugin，但核心路径应优先保证 YAML 配置的可靠升级和兼容。
- save/load 要能标注是否阻塞主线程，以及是否更新快照。

设计含义：

`ConfigManager` 不应只是文件读写工具，而应生成可审计的配置快照。

## R6: 持久化与批量落盘是共性能力

证据：

- Foundation 有数据库/CSV repository、Hikari/Exposed、批量保存、定期 flush。
- DynamicEconomy 使用 `economy-cache.yml` 保存总财富、货币发行、商品缓存，部分路径会频繁保存。
- ExternalBook 同时支持数据库和 JSON/YAML 文件存储。
- RailArea、FeeFly、TPA 等也有缓存文件和周期性任务。

需求：

- 框架提供 batch writer、flush-on-close、backpressure、失败报告和诊断。
- 业务侧只定义“变更事件”和“如何写入”，不自己管理重复定时 flush。
- 高风险的每次交易立即保存应可配置为 sync、batch、write-through 或 write-behind。

设计含义：

持久化不必统一 ORM，但应统一运行时约束和成本模型。

## R7: 缓存是性能能力，也是正确性边界

证据：

- core 有 LRU/LFU cache，config/i18n/string formatter/scoreboard/GUI data 都使用缓存。
- GUI 分页组件和业务页面频繁 `clearCache()`。
- SmartCache 当前有独立清理线程，说明 TTL/淘汰是现实需求。
- 反射、格式插件 availability、ObjectMapper 等也被缓存。

需求：

- 缓存要声明 owner、scope、key、ttl、capacity、invalidation policy、thread policy。
- 禁止每个 cache 自己开线程，改用 runtime 共享维护任务。
- 配置 reload 时按 scope 批量失效，而不是业务侧猜要清哪些缓存。

设计含义：

缓存不能只是一个 Map wrapper。它必须和 lifecycle、reload、diagnostics 连接。

## R8: 命令和权限需要框架级元数据

证据：

- 多个插件使用 Cloud command coordinator。
- 几乎每个插件都有 `CommandRegistry`。
- admin reload 命令、user command、权限控制和 i18n 消息重复出现。
- Paper plugin.yml 官方要求 commands、permissions、depend/softdepend 等元数据清晰。

需求：

- 命令树声明、权限节点、描述、usage、aliases、i18n key 应从同一事实源生成。
- `plugin.yml` / `paper-plugin.yml` 应由构建期任务生成静态文件，开发者只维护代码声明。
- 执行器需要声明线程 affinity。
- 命令错误处理应统一，不让每个 command 手写权限、参数、异步失败消息。

设计含义：

命令模块不应只是封装 Cloud。它还要承担元数据完整性和运行时调度。

## R8b: 事件作用域是 lifecycle 的核心需求

证据：

- `subscribeEvent<T>` 在大量模块中出现，覆盖玩家、方块、实体、世界、Inventory、第三方插件事件。
- 当前 BaseModule 版本会把 EventSubscription 绑定到模块生命周期。
- Event DSL 已经有 filter、priority、expire、exception handler。
- GUI 内部也大量依赖 Inventory event、AsyncChatEvent、PlayerQuitEvent，并在 page/component 之间分发。
- RailArea 等模块使用自定义 EventEmitter 做 domain event，但它没有统一 owner 绑定。

需求：

- EventRuntime 必须是和 SchedulerFacade 同级的深 Module。
- 事件 subscription 必须支持 module/player/page/task 等 scope。
- 支持 `priority`、`ignoreCancelled`、`MONITOR read-only`、filter、once/until/timeout。
- 支持 event handler 派生 task，task 完成/取消也能关闭临时 event scope。
- 支持高频事件背压：throttle、debounce、coalesce、drop-if-busy。
- 支持 Bukkit Event、domain event、第三方 listener adapter。

设计含义：

事件不是注册工具，而是被动触发的 runtime。它和任务系统共享 owner、affinity、diagnostics，才能避免 listener 泄漏、异步线程误用和事件风暴。

## R8c: 声明式命令要成为框架事实源

证据：

- 当前多数插件使用 Incendo Cloud annotations：`@Command`、`@Argument`、`@Permission`、`@Suggestions`、`AnnotationParser`、`MinecraftHelp`。
- `modules/core` 仍有 CommandAPI 风格的 `BaseCommandRegistry`，说明命令模块存在实现漂移。
- 用户明确希望像 Aikar/Cloud 一样，用代码声明命令、参数、校验、补全、权限和帮助，框架负责资源和注册。

需求：

- 保留声明式命令模型，不回到 Bukkit `CommandExecutor`。
- CommandRuntime 以 Cloud annotations 为主要 Adapter，同时吸收 Aikar ACF 的声明式价值。
- build-time 从命令声明生成静态 `plugin.yml` / `paper-plugin.yml` root command 和 permissions。
- handler、parser、suggestion provider 都要声明 `ThreadAffinity`。
- command 中启动任务、等待事件、打开 GUI 都必须通过统一 owner model。

设计含义：

命令不只是 plugin metadata，也不只是运行时 parser。它是“代码声明 -> 静态 yml -> 运行时命令树 -> help/error/i18n/diagnostics”的完整链路。

## R9: 跨插件依赖和服务发现是常态

证据：

- 插件使用 Vault、WorldEdit、数据库、外部图书库等可选能力。
- plugin.yml 的 `depend`、`softdepend`、`provides` 决定加载顺序和功能可用性。
- 业务代码里存在插件服务获取、manager 暴露、兼容路径。

需求：

- 框架提供 typed service registry。
- soft dependency 缺失时模块可降级，报告应说明禁用了哪些能力。
- reload 不应重复解析一堆全局插件引用。

设计含义：

依赖不是 `Bukkit.getPluginManager().getPlugin()` 的散点调用，而是模块安装阶段的依赖声明。

## R10: 服务器环境要求强诊断

证据：

- Minecraft 插件错误通常只在运行服暴露。
- 当前项目已有 troubleshooting、review、performance test、security test 文档和测试。
- 业务日志里记录 batch flush、cache 更新、命令防火墙统计等信息。

需求：

- 框架提供 `/plugin diag` 或类似诊断输出：活跃任务、慢任务、缓存命中、打开 GUI、未关闭 owner、最后 reload 报告。
- 所有 report 可序列化，便于管理员贴日志。
- 对 tick 超时、异步 Bukkit API 风险、配置错误给出具体模块和资源路径。

设计含义：

诊断不是开发期附属工具，而是服务器运维体验的一部分。

## R11: 安全和输入验证应前移

证据：

- BetterCommandBlock 有 firewall、command trie、validation timeout、cache expiry、统计和安全测试。
- 外部书籍、命令、GUI 输入、聊天输入、配置文件都接受用户或管理员输入。

需求：

- 框架应提供统一的 input validation、permission check、rate limit、command audit hook。
- GUI/chat input 要有超时、取消、所有权检查和权限复验。
- 外部数据进入 Bukkit 对象前必须有边界校验。

设计含义：

安全能力不要复制到每个插件，而应作为 command/gui/config/network 的交叉能力。

## R12: Minecraft 特性要求“位置/实体/玩家”成为一等上下文

证据：

- TPA、RailArea、CreateArea、MCron、FeeFly、Guardian 等业务都围绕 Player、Location、World、Entity、Inventory。
- Folia 把线程所有权与 region/entity 绑定，进一步强化这个上下文。

需求：

- `PlayerContext`、`LocationContext`、`EntityContext` 应可派生 scheduler affinity、permission audience、locale、resource owner。
- 对离线玩家、实体消失、世界卸载、区块未加载等状态提供标准结果。

设计含义：

下一代框架的 Context 不能只是 `plugin` 的别名。它要编码 Minecraft 对象生命周期和线程所有权。

## 需求优先级

P0:

- 生命周期和资源 owner。
- 调度器 outcome/affinity。
- EventRuntime 与事件作用域。
- 配置快照和 reload report。
- GUI event routing 正确性。

P1:

- cache runtime。
- CommandRuntime、声明式命令和 plugin yml 生成。
- 持久化 batch writer。
- diagnostics。

P2:

- Folia adapter。
- per-player locale。
- network circuit breaker。
- 自动 plugin.yml 生成和产物校验。
