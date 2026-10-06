# Pain Points And Key Technical Analysis

## 1. 生命周期不是 `onEnable/onDisable`

Minecraft 插件的真实生命周期至少包含：

```text
described -> loaded config/text -> resolved integrations -> installed modules
-> started -> reloaded -> closing -> closed
```

旧写法常把配置读取、语言初始化、命令注册、模块创建和 reload 混在 `onEnable()` 或 `reloadPlugin()` 里。结果是首次启动和后续 reload 语义不同，模块失败也很难知道保留了什么状态。

新框架必须把 lifecycle 变成显式状态机，并让每个长期资源挂到 owner tree。

## 2. `sync/async` 不够表达 Minecraft 线程边界

Paper/Bukkit 主线程、异步 I/O、CPU 计算、Folia region/entity scheduler 不是同一个维度。只暴露 `runSync` / `runAsync` 会导致：

- 异步代码误碰 Bukkit API。
- 主线程执行文件、数据库、网络或大排序。
- Folia 未来无法自然接入。
- 任务取消、超时和错误传播全靠业务手写。

新框架需要 `Affinity`：

```kotlin
sealed interface Affinity {
    data object Main : Affinity
    data object AsyncIo : Affinity
    data object AsyncCpu : Affinity
    data class Region(val location: LocationSnapshot) : Affinity
    data class Entity(val entityId: UUID) : Affinity
}
```

Paper 1.20.1 下 `Region/Entity` 可以先映射回主线程；Folia adapter 以后再实现。

## 3. 事件是被动 runtime，不是 listener helper

事件是插件里最重要的被动触发模型。传统 Listener 类的问题不是语法长，而是生命周期和作用域弱：

- 临时监听器难管理。
- 玩家退出、GUI 关闭、模块卸载后容易泄漏。
- 高频事件容易造成事件风暴。
- 异步事件线程边界不清晰。
- 事件启动任务时 owner 归属不清晰。

新 EventRuntime 必须支持：

- module/player/page/task/command scope。
- priority、ignoreCancelled、MONITOR read-only。
- ordered filters、permission helpers。
- once-by-handled。
- throttle、debounce、snapshot、coalesce、drop-if-busy。
- event -> task bridge。
- domain event 和第三方 listener adapter。

## 4. 命令是插件元数据、运行时执行和帮助系统的交汇点

原生 `CommandExecutor + plugin.yml` 会让命令名、权限、usage、描述、补全、错误处理、handler 分散。Cloud、ACF、CommandAPI 都证明声明式命令是成熟方向，但每个框架只解决了一部分问题。

新 CommandRuntime 要把这些事实收拢：

- 玩家看到的命令形态。
- sender 类型。
- 参数类型、联合类型、枚举、自定义转换。
- 校验、建议、补全、help。
- 权限和默认权限描述。
- 构建期静态 yml。
- 运行时错误渲染和 diagnostics。
- handler 线程归属和 owner。

命令字符串应尽量所见即所得：

```kotlin
command("/market give <target> <item> [amount=1] [--silent]") { ... }
```

类型和约束放在就地 refinements 中，而不是把 `<target:player>` 写成冗长类型语法。

## 5. GUI 是 UI runtime，不是 Inventory DSL

复杂 GUI 不是几个 button 的集合。真实需求包括：

- session、named session、TTL、page stack。
- list、pagination、scrolling、input slot、output slot、storage slot。
- click/drag/close/open/quit 事件路由。
- shift-click、hotbar swap、creative、cursor、drop 等安全边界。
- 异步数据加载和 stale generation 丢弃。
- ItemStack materialize 主线程归属。
- title 更新平台差异。

GUI 必须有自己的 runtime：session registry、page registry、event router、render diff、slot policy、data runtime、object protection、diagnostics。

## 6. 配置和文本是管理员/开发者共同维护的接口

配置文件不是普通 JSON 对象，服主会手动编辑，注释和顺序有价值。语言文件不是散落的 string key，而是命令、GUI、事件反馈、错误提示和 diagnostics 的统一文本接口。

新框架应优先提供：

- typed config data class。
- comment-preserving YAML adapter。
- unknown key warning，strict mode error。
- restart required field report。
- typed text catalog interface。
- MiniMessage default，Legacy 显式 opt-in。
- missing/unused/invalid catalog report。
- owner-scoped placeholder resolver。

底层 `ConfigRuntime`、`MessageKey`、`MessageTemplate` 是内部事实模型，不是第一屏 API。

## 7. 性能不是到处加缓存

Minecraft 的主线程预算大约 50ms/tick。框架必须默认避免偷主线程预算：

- 文件、数据库、网络不在主线程。
- GUI refresh 只更新 dirty slot。
- 高频事件有背压。
- 配置 reload 异步读写，原子替换 snapshot。
- 缓存有 owner、TTL、容量、失效策略。
- 关闭时所有 task、listener、client、writer、cache maintainer 清零。

缓存也可能制造错误。长期强引用 Player、Entity、Inventory、可变 ItemStack 或跨线程 Bukkit 对象都应避免。

## 8. 测试不能只靠启动真实服务器

旧项目里测试成本高，很多问题只能通过 runServer 暴露。新项目必须从第一天建立测试分层：

- pure contract tests: 不依赖 Bukkit，验证 owner/task/event/config/text/gui 中立模型。
- adapter tests: MockBukkit 或 fake Paper glue。
- golden sample tests: 生成 `paper-plugin.yml`、命令 descriptor、config schema、message catalog 的稳定契约。
- approved smoke: 必要时用真实 Paper 1.20.1 server 验证 adapter 边界。

## 9. 诊断是产品能力

服务器管理员需要知道插件为什么卡、为什么 reload 失败、为什么某功能 disabled。开发者需要知道哪个 owner 还没关、哪个 task 慢、哪个事件 handler 超预算。

DiagnosticsRuntime 必须能输出：

- owner tree。
- active tasks。
- event subscriptions。
- command registry。
- open GUI sessions。
- config/text snapshots。
- network clients and requests。
- cache stats。
- batch writer queues。
- integration capabilities。
- recent reports。

