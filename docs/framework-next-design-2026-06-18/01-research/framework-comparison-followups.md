# Framework Comparison Followups

## 定位

本页把 event、GUI、command 以及其他能力的调研结论压缩成设计取舍准则。调研原文保留在本目录其他文档中；本页服务于后续写作和开发评审。

## 共同结论

不同框架给出的启发高度一致：

- 好的业务 Interface 应该声明意图，不暴露底层样板。
- 生命周期、线程归属、关闭、错误和诊断必须由框架托管。
- 第三方库适合做 Adapter，不适合倒推主 Interface。
- Minecraft 特有的 tick、主线程、inventory event、安全交互、静态 plugin metadata 不能被普通应用框架抽象抹掉。

## Event 取舍

吸收：

- Bukkit/Paper 原生 priority、ignoreCancelled、cancellable 语义。
- scoped subscription、expire、filter、await one。
- 高频事件的 throttle、debounce、coalesce、dropIfBusy。
- event -> task 和 task -> await event 的桥。

避免：

- 完整暴露 reactive 流库。
- 让事件回调默默在不确定线程里触碰 Bukkit 对象。
- 用短 DSL 丢掉原生事件事实。

## GUI 取舍

吸收：

- session/page/component/pane/layout。
- per-slot/per-item action。
- paginated、scrolling、storage slot。
- render diff、data provider、loading/empty/error state。
- click/drag/shift/drop/creative/hopper 等安全保护内建。

避免：

- 每个页面手写 inventory event 路由。
- 高频 tick update 作为默认刷新方式。
- action 闭包长期捕获复杂状态却没有 owner 管理。

## Command 取舍

吸收：

- Cloud annotation/DSL 的声明式体验。
- ACF 风格的 condition、validation、completion、help。
- CommandAPI/Brigadier 的强类型参数和执行前错误。
- build-time descriptor 生成静态 yml。

避免：

- `plugin.yml`、command handler、permission、help 分散维护。
- 把 Cloud 或其他命令库类型变成业务主 Interface。
- 只依赖 async coordinator 解决线程问题。

## Scheduler/Lifecycle 取舍

吸收：

- owner-bound scope。
- structured cancellation。
- task outcome。
- fake scheduler harness。
- Folia region/entity/global 概念预留。

避免：

- 只提供 `runSync/runAsync` 二分法。
- 无限队列。
- 关闭时只 cancel task id 而不产生 report。

## Config/Text 取舍

吸收：

- typed schema。
- immutable snapshot。
- reload transaction。
- fallback and placeholder validation。
- message catalog report。

避免：

- 每个插件手写 reload 顺序。
- invalid config/catalog 替换旧 snapshot。
- 用户可见 raw string 散落。

## Network/Persistence/Cache 取舍

吸收：

- named client。
- timeout/retry/rate limit/bulkhead/circuit policy。
- redaction。
- transaction and batch writer。
- cache ttl/max size/version invalidation/single-flight。

避免：

- 裸 client、裸 map、裸 coroutine scope 到处传。
- retry 无 backoff。
- cache 无限增长。
- 关闭时补救写盘没有 drain report。

## Diagnostics/Integration 取舍

吸收：

- health dump。
- owner/resource graph。
- structured report。
- typed capability。
- provider health。
- required/optional explicit state。

避免：

- 只靠日志排查 runtime 状态。
- nullable service provider 到处传。
- provider 缺失后业务自行猜降级方式。

## 对外写作角度

未来技术博客可以按这些主题拆：

- 为什么 Minecraft 插件框架需要 owner tree，而不是只有工具函数。
- Event scope 如何降低 listener 泄漏和临时交互成本。
- 声明式 command 如何统一代码、help、permission 和静态 metadata。
- GUI 为什么需要 runtime，而不是 inventory helper。
- 配置、文本、缓存和诊断如何降低 reload 的心智负担。
- Network/Persistence policy 如何减少 tick 卡顿、请求风暴和关闭风险。
