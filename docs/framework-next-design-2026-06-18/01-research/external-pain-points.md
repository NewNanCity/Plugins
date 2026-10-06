# 外部约束与开发者痛点

本页把外部资料归纳成设计输入。官方资料用于平台约束，社区资料只作为痛点信号。

## 平台约束

### Tick 预算是真边界

Paper Scheduling 文档说明 Minecraft 以 tick 循环执行逻辑，正常是每秒 20 tick，也就是一个 tick 约 50ms；单 tick 超过预算，服务器就开始落后并表现为 lag。

设计映射：

- 框架必须把主线程耗时当作一等指标。
- 阻塞 I/O、大排序、大解析、网络请求不能默认在主线程运行。
- slow callback 应带模块 id、owner、任务名和耗时。

来源：https://docs.papermc.io/paper/dev/scheduler/

### 非 Folia 和 Folia 的 scheduler 模型不同

Paper Scheduling 文档明确非 Folia 服务器使用 BukkitScheduler；Paper Folia 支持文档列出 Global、Region、Async、Entity scheduler；Folia README 也说明 Bukkit Scheduler 天然依赖单主线程，而 Folia 通过 RegionScheduler/EntityScheduler 绑定位置或实体所有权。

设计映射：

- 新框架调度接口不能只叫 `sync/async`。
- 应引入 `ThreadAffinity.Main/AsyncIo/AsyncCpu/Region/Entity`。
- Paper 下 Region/Entity 可以映射回主线程，Folia 下映射到正确 scheduler。
- 是否真正支持 Folia 需要独立阶段验证，不能只在配置里写 supported。

来源：

- https://docs.papermc.io/paper/dev/scheduler/
- https://docs.papermc.io/paper/dev/folia-support/
- https://github.com/papermc/folia

### 异步任务不能随意访问 Bukkit API

BukkitScheduler API 对 async task 明确提醒要小心线程安全，异步任务不应访问 Bukkit API。Spigot 数据库异步指南也强调同步任务运行在 Minecraft tick 线程，数据库和重任务应离开主线程。

设计映射：

- 任务 API 必须带线程归属和 guard。
- 数据库、文件、网络默认走 `AsyncIo`。
- Bukkit 对象读取/写入必须通过 `Main/Region/Entity` 阶段。
- Kotlin coroutine bridge 应由框架提供，而不是每个插件自己处理。

来源：

- https://github.com/Bukkit/Bukkit/blob/master/src/main/java/org/bukkit/scheduler/BukkitScheduler.java
- https://www.spigotmc.org/wiki/asynchronously-working-with-a-database/

### Event listener 注册语义比传统示例更复杂

Paper event listener 文档展示的传统方式是写 Listener 类并在插件启动时注册；同时它强调 event priority、MONITOR 和 cancellation/ignoreCancelled。Spigot/Bukkit Javadocs 也把 `priority` 与 `ignoreCancelled` 作为 `EventHandler` 和 `registerEvent` 的核心参数。

设计映射：

- subscribe DSL 是正确方向，但必须完整表达 Bukkit/Paper event 语义。
- EventScope 要替代手动 listener lifecycle。
- `ignoreCancelled`、`MONITOR read-only`、priority order、cancellable event 都要进入 Interface。
- async event 与 sync event 必须接入 ThreadAffinity。

来源：

- https://docs.papermc.io/paper/dev/event-listeners/
- https://hub.spigotmc.org/javadocs/spigot/org/bukkit/event/EventHandler.html
- https://helpch.at/docs/1.13.1/org/bukkit/event/class-use/Listener.html

### 声明式命令是成熟痛点解法

Cloud v2 官方文档把 suggestions、execution coordinator、command descriptions、annotation-based command methods、parsers、suggestions 和 exception handlers 都作为核心能力。Aikar ACF 也明确把 DI、validation、tab completion、help documentation、syntax advice 和 stateful conditions 放到 annotations 后面。

设计映射：

- 我们不应该回到手写 `CommandExecutor` 和 yml 双维护。
- 当前 Cloud annotations 方向应成为 CommandRuntime 的主要 Adapter。
- Aikar ACF 是声明式命令体验的参照，不一定是依赖选择。
- command metadata 要能 build-time 生成静态 yml，运行时仍由 Cloud/adapter 注册完整命令树。

来源：

- https://cloud.incendo.org/core/
- https://cloud.incendo.org/annotations/
- https://github.com/aikar/commands

### 配置是插件行为接口

Paper plugin configuration 文档强调配置文件让用户改变插件行为，默认使用 YAML；也说明加载和保存文件会影响主线程，应异步处理。Paper plugin.yml 文档把插件名称、版本、主类、命令、权限、依赖、libraries 等都放进元数据。

设计映射：

- 配置系统要重视兼容、升级和报告，而不是只追求多格式。
- `plugin.yml`、命令、权限和依赖需要构建期生成与测试校验。
- 命令和权限最好从框架声明生成，避免文档、代码、plugin.yml 三处漂移。

来源：

- https://docs.papermc.io/paper/dev/plugin-configurations/
- https://docs.papermc.io/paper/dev/plugin-yml/

## 社区痛点

### 调度和取消任务容易写错

社区里常见问题是“如何取消某个玩家的重复任务”“logout 后怎么避免重复任务继续跑”“应该用 BukkitTask 还是 task id”。这说明开发者真正想要的不是更多 scheduler overload，而是生命周期绑定和 owner-based cancellation。

设计映射：

- `ctx.tasks.every(...).ownedBy(player/session/module)`。
- Player logout、GUI close、module close 自动取消相关任务。
- 诊断能列出某玩家或某模块的 active tasks。

相关资料：

- https://stackoverflow.com/questions/74631180/cancelling-a-scheduled-repeater-on-bukkit-java
- https://www.spigotmc.org/wiki/scheduler-programming/

### 开发者想要更顺手的 wrapper，但 wrapper 不能掩盖边界

Reddit 讨论里有开发者抱怨创建命令、延迟任务和线程处理不方便，希望有 wrapper。这个痛点成立，但新框架不能只提供更短语法，否则会把线程和生命周期问题藏得更深。

设计映射：

- 提供 DSL，但 DSL 必须表达 owner、affinity、timeout、failure。
- 命令 wrapper 必须同时处理 permission、i18n、error rendering 和 scheduler。
- 延迟任务 wrapper 必须返回可诊断、可取消的 handle。

来源：https://www.reddit.com/r/admincraft/comments/u90bbp/has_noone_written_an_api_wrapper_for_bukkit/

### Folia 对开发者心智提出新要求

PaperMC/Folia 的开发者指南 issue 里提到开发者不清楚命令、事件、Bukkit runnable 在线程和区域上如何决定，也不清楚如何在 Folia scheduler 和普通 Paper scheduler 之间 fallback。

设计映射：

- 框架文档必须教“上下文 -> affinity -> scheduler”的模型。
- API 不暴露“猜当前线程”的方法，业务必须通过 context 调度。
- Folia 支持应该是 adapter 和测试矩阵，不是几个 if 判断。

来源：https://github.com/PaperMC/Folia/issues/287

## 对新框架的具体要求

1. 用 owner tree 解决任务、事件、GUI、网络请求和缓存的关闭问题。
2. 用 `ThreadAffinity` 解决 Bukkit 主线程、异步 I/O、CPU 工作和 Folia 的区域/实体归属。
3. 用 EventRuntime 解决 listener scope、ignoreCancelled、priority、事件风暴和 event-task bridge。
4. 用结构化 report 解决 reload、配置升级、消息缺失、任务失败只能看日志的问题。
5. 用 command metadata 生成静态 plugin.yml，解决命令树、权限、usage、plugin.yml 漂移。
6. 用 batch writer 和 cache runtime 解决每个插件重复写 flush/cache 维护逻辑的问题。
7. 用 diagnostics 解决服务器管理员无法判断插件成本和泄漏的问题。

## 不应误读的点

- 社区希望 wrapper，不代表要做一个隐藏所有 Bukkit/Paper 细节的魔法层。
- Folia 是长期方向，不代表当前设计阶段就要承诺完整兼容。
- 多格式配置存在价值，但默认 YAML 和升级报告更贴近 Paper 插件真实运维。
- 性能优化不是到处加缓存，而是明确哪些数据可缓存、何时失效、谁负责关闭。
