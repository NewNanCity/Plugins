# 性能、运行成本、缓存和 GC 设计

## 性能目标

Minecraft 服务器每秒 20 tick，单 tick 预算约 50ms。插件框架不应该把“业务逻辑还能跑”作为目标，而应该把“不会偷走主线程预算、能解释成本、能按 owner 限制资源”作为目标。

目标指标：

- 主线程回调默认应小于 1ms，超过阈值记录 slow callback。
- 单次 GUI refresh 只更新变化 slot，不整页重建。
- 配置 reload 不在主线程做阻塞文件 I/O。
- 每个插件默认不创建额外长期线程，除非明确声明 executor。
- 所有周期任务、缓存维护和 batch flush 都挂到 runtime 统一调度。
- 关闭插件后 active task、open GUI session、network job、cache maintainer 为 0。

## 主线程预算

主线程只做这些事：

- Bukkit/Paper API 要求的世界、实体、玩家、Inventory、ItemStack materialize 操作。
- 把异步阶段产出的轻量结果应用到游戏状态。
- 极短的状态切换和 owner 检查。

主线程不做：

- 文件 load/save。
- 数据库查询和事务。
- 网络请求。
- 大列表排序、分页、过滤。
- 大量 MiniMessage/Component 解析。
- 大量反射扫描。

框架要把每个任务段标为 `Main`、`AsyncIo`、`AsyncCpu`、`Region` 或 `Entity`。开发期可启用 guard，发现异步线程访问 Bukkit 高危 API 时直接 fail fast 或记录 fatal diagnostic。

## 异步并不等于无限线程

现有代码大量使用 `runAsync`，但下一代框架需要引入 executor 策略：

- `AsyncIo`: 小型有界线程池，适合 DB、文件、网络。
- `AsyncCpu`: 受 CPU 核数限制，适合解析、排序、模板预编译。
- `SchedulerManaged`: Bukkit/Paper scheduler 的桥接入口。
- `SharedMaintenance`: cache eviction、batch flush、metrics sweep 共用。

不建议：

- 每个 cache 自己创建清理线程。
- 每个模块自己创建 `Thread`。
- 用无限队列掩盖 I/O 堵塞。
- 用 `Thread.sleep` 模拟延迟。

需要：

- 队列长度 metrics。
- backpressure 策略：reject、drop-old、coalesce、block-with-timeout。
- 生命周期关闭时 drain/cancel 策略。

## 缓存分层

缓存分四类：

1. 静态缓存：反射 class、ObjectMapper、格式插件 availability。生命周期是 plugin runtime，reload 通常不清。
2. 配置快照缓存：配置 parse 结果。生命周期是 config version，reload 原子替换。
3. 业务热缓存：排行榜、玩家前缀、scoreboard、书籍索引。生命周期由模块声明。
4. GUI 渲染缓存：轻量 render model、分页数据、静态 item descriptor。生命周期是 page/session。

每个缓存必须声明：

```text
owner: module/gui/page/runtime
capacity: max entries or weight
ttl: optional
key stability: configVersion/player/page/query
value mutability: immutable/copy-on-read/main-thread-only
invalidation: reload/manual/event/generation
thread policy: single-thread/confined/concurrent
```

## 缓存禁区

不应长期缓存：

- 可变 `Player`、`Entity`、`Inventory` 的强引用。
- 可能跨线程误用的 Bukkit 对象。
- 大量完整 `ItemStack`，除非明确 clone 和主线程归属。
- 包含玩家动态变量的最终渲染消息。
- 未绑定 config version 的解析结果。

建议缓存：

- 编译后的消息模板。
- 解析后的配置不可变快照。
- GUI item descriptor，而不是总是缓存最终 `ItemStack`。
- 排序后的 id 列表和分页索引。
- 反射 lookup 结果。

## GUI 渲染成本

当前 GUI 业务经常在点击后 `clearCache()` 再 `show()`。新设计应改成：

```text
state change
-> invalidate affected data key
-> fetch or compute render model off-thread
-> switch to Main
-> materialize changed ItemStack
-> apply slot diff
```

优化点：

- slot layout 预编译，避免每次 refresh 重新计算所有坐标。
- component tree 有 dirty flag，只有 dirty component 参与 render。
- render model 使用小型不可变对象，减少闭包捕获大对象。
- page generation id 防止旧异步数据覆盖新页面。
- item builder 避免重复解析相同 lore/title 模板。
- 对分页数据采用 window cache，例如当前页、上一页、下一页。

GC 角度：

- 避免每 tick 为同一 GUI 创建大量 lambda、list、map。
- 大分页列表不要 `toList()` 多次复制，数据 provider 直接返回 window。
- 对高频更新 scoreboard/actionbar，使用预编译模板加少量参数替换。

## 配置 I/O 成本

配置 load/save 是文件 I/O，Paper 文档也提醒主线程执行 load/save 会拖慢服务器。下一代框架策略：

- reload 阶段先异步 read/merge/parse/validate。
- 生成 `ConfigSnapshot` 后在安全线程点原子替换。
- save 操作默认走 batch writer 或 explicit flush。
- 对管理员命令触发的保存，返回“已排队/已完成/失败”状态。
- config upgrade/merge 报告包含耗时和文件大小。

配置缓存策略：

- cache key 必须包含 canonical path、schema type、format、config version。
- reload 替换 version 后旧缓存自然失效。
- 单文件保存后按 path scope 失效，不扫全局字符串前缀。

## 持久化与批量落盘

业务高频写入要按语义分类：

- 必须立即持久化：付费、交易、不可丢安全状态。
- 可短暂延迟：排行榜、统计、缓存索引、UI 偏好。
- 可重建：派生索引、搜索缓存、分页缓存。

框架提供：

```text
WritePolicy.SyncCommit
WritePolicy.WriteThrough
WritePolicy.WriteBehind(maxDelay, maxBatchSize)
WritePolicy.DerivedCache
```

批量 writer 的默认规则：

- 按 key 合并同一对象的多次变更。
- 到达 max batch size 或 max delay flush。
- plugin close 时 flush，超时后报告未完成数量。
- flush 失败记录 structured failure，按配置重试或降级。
- 队列过长触发 backpressure，不无限吃内存。

## 网络成本

网络层的性能风险通常是：

- 没有 timeout 导致任务悬挂。
- 大响应一次性读入内存。
- reload 后旧 client 和旧请求仍在跑。
- retry 风暴放大外部服务故障。

策略：

- 每个 client 必须有 connect/read/write timeout。
- 每个请求必须有 owner 和 cancel token。
- 默认限制 body size，大文件必须 streaming。
- retry 使用指数退避和 jitter。
- circuit breaker 防止外部故障拖垮插件。
- metrics 记录 endpoint、status、latency、body size、failure kind。

## 日志成本

日志也是性能成本：

- 高频路径不做字符串拼接。
- debug 日志必须可关闭。
- 重复错误要 rate limit。
- report 用结构化字段保存，用户命令再格式化成人类文本。
- 慢任务只记录摘要，详细 trace 按采样或 debug 开关开启。

## 事件成本

高频事件是插件性能里最容易被低估的成本。`PlayerMoveEvent`、`VehicleMoveEvent`、Inventory event、chat event、chunk/world event 都可能在短时间内大量触发。

EventRuntime 需要提供：

- handler slow threshold。
- per-event call count、drop count、coalesce count。
- throttle/debounce/coalesce/drop-if-busy 策略。
- owner 关闭后拒绝新 handler 和派生任务。
- async event 的线程 guard。
- `MONITOR` handler 的只读 diagnostic。

事件 handler 默认不应该做重 I/O、大量分配或无限派生任务。需要异步工作时，应通过 EventTaskBridge 创建 owner-bound task。

## GC 和对象生命周期

优先策略：

- 使用不可变快照替代到处复制 mutable map。
- 让 owner tree 决定对象生命周期。
- 避免全局强引用 Player/Entity/Inventory。
- 对跨线程数据使用 DTO，不直接传 Bukkit mutable object。
- 对热路径先测量再引入对象池。

不建议默认对象池。JVM 对短命对象已经优化得很好，错误对象池反而会扩大生命周期、增加泄漏风险。只有在明确 profile 看到某类对象分配成为瓶颈时，才建立局部池，并绑定 owner。

## 开发者心智负担

性能设计的另一半是减少开发者需要记住的规则：

- 不要求业务开发者手动记“这段要 runSync 还是 runAsync”，而是通过 API 名称和上下文表达。
- 不要求业务开发者手动清哪些 cache，reload 自动按 scope 失效。
- 不要求每个插件复制 reload 三件套。
- 不要求每个 GUI 页面自己处理旧异步结果覆盖。
- 不要求每个命令自己格式化错误。
- 不要求每个持久化模块自己写 batch flush。

目标体验：

```kotlin
ctx.persistence.writeBehind("wealth-cache") {
    key(playerId)
    value(delta)
    merge(::mergeDelta)
    flush(::saveDelta)
}
```

开发者只声明写入语义，框架处理队列、flush、关闭、失败和诊断。

## 应优先测量的指标

- 每 tick 框架主线程耗时。
- active task 数和队列长度。
- GUI page refresh 耗时、更新 slot 数、ItemStack 创建数。
- config reload 耗时和文件 I/O 耗时。
- cache 命中率、淘汰数、当前容量。
- batch writer 队列长度、flush size、flush latency、失败数。
- network latency、timeout、retry、cancel。
- plugin disable 后未关闭资源数量。
