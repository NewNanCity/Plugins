# 详细发现

术语沿用架构审查词汇：Module、Interface、Implementation、Depth、Seam、Adapter、Leverage、Locality。

## F1. BaseModule 生命周期 Interface 太浅

证据：

- `BaseModule` 构造器只把子模块加入父级：`modules/core/src/main/kotlin/city/newnan/core/base/BaseModule.kt:34`、`BaseModule.kt:115`。
- 初始化需要子类手动调用：`BaseModule.kt:57`。
- 文档要求 `init { init() }`：`docs/core/base-module.md:41`、`docs/core/base-module.md:267`。
- 同一批文档又冲突：`CLAUDE.md:39` 要 Manager `by lazy` 且禁止 `lateinit`，`docs/core/base-module.md:366` 要 `lateinit` 且 `docs/core/base-module.md:405` 禁止 lazy，`docs/development/plugin-conventions.md:23` 又要求 Manager 懒加载。

问题：

这个 Module 的 Interface 没有隐藏生命周期复杂度。调用方必须知道构造器不会初始化、必须在子类 `init` 块手动调用、父级只 bind 不 init、reload 只会触达 initialized 子模块。删除测试看，删除 `BaseModule` 后复杂度会重新散落到各插件；说明 Module 值得存在，但当前 Depth 不够。

影响：

- 漏掉 `init()` 时模块静默不可用。
- 在构造期间调用 overridable `onReload()`/`onInit()` 仍是脆弱协议。
- 文档冲突会让新插件随机选择 lazy 或 lateinit，导致生命周期行为不可预测。

更好的方向：

- 把 `BaseModule` 外部 Interface 收敛为一个稳定创建 seam：例如 `plugin.installModule { PlayerManager(...) }` 或 `BaseModule.create(...)`，由框架完成 addChild、bind、init。
- 保留旧构造器作为兼容入口，但标记为 legacy，并用运行时检查在未初始化模块被 reload/close 前报明确错误。
- 先统一文档事实源：BaseModule 管理的模块用显式 install；普通 Manager 若不继承 BaseModule 才允许 lazy + bind。

## F2. Scheduler 任务链混淆失败、取消和可空结果

证据：

- `ITaskHandler.thenRun*` 多处使用 `function(getNow(null)!!, handler)`：`modules/core/src/main/kotlin/city/newnan/core/scheduler/ITaskHandler.kt:104`、`ITaskHandler.kt:106`、`ITaskHandler.kt:114`、`ITaskHandler.kt:122`、`ITaskHandler.kt:130`、`ITaskHandler.kt:138`、`ITaskHandler.kt:146`、`ITaskHandler.kt:154`、`ITaskHandler.kt:162`、`ITaskHandler.kt:170`、`ITaskHandler.kt:178`。
- `CombinedTaskHandler` 对外声明 `ITaskHandler<Unit>`，但 `get()`/`getNow()` 永远返回 null：`CombinedTaskHandler.kt:49`。
- `CombindMode` 拼写进入公共 Interface：`CombinedTaskHandler.kt:5`。
- `start()` 先通知 scheduler 再设置 `_internalFinished`：`CombinedTaskHandler.kt:66`。

问题：

任务链 Interface 看起来像 `CompletableFuture`，但没有表达 Result、Cancellation、Nullable value 三种不同状态。调用方以为依赖成功后才执行，实际要理解 `getNow(null)!!` 的实现细节。

影响：

- 前置任务失败会在下游变成 NPE。
- 合并任务结果和 Unit 语义不一致。
- 拼写错误已经变成公共契约，直接重命名会破坏下游。

更好的方向：

- 引入内部 `TaskOutcome<T>`，把 success/failure/cancelled/nullable value 分开。
- 新增安全链式 Interface：`thenApplyResult` 或 `thenRunSuccess`，旧 `thenRun*` 保持兼容并 deprecated。
- `CombindMode` 使用 typealias 兼容迁移到 `CombinedMode`。

## F3. GUI SessionStorage 是过宽全局 Module

证据：

- `currentGuiManager` 在读锁内请求写锁：`modules/gui/src/main/kotlin/city/newnan/gui/session/SessionStorage.kt:51`、`SessionStorage.kt:53`。
- 全局事件监听只允许 `SessionStorage.currentGuiManager == guiManager` 的 manager 处理：`modules/gui/src/main/kotlin/city/newnan/gui/event/GlobalEventListener.kt:65` 到 `GlobalEventListener.kt:112`。
- 离线玩家清理使用 `!Bukkit.getPlayer(it)?.isOnline!!`：`SessionStorage.kt:284`。
- `runInMainThread` 没有 GuiManager 时取任意 enabled plugin 调度：`SessionStorage.kt:239`。

问题：

`SessionStorage` 把多插件 GUI 注册、事件分发、Session 存储、ChatInput、主线程调度都放在一个单例 Interface 里。它不是 deep Module，而是全局共享状态容器；调用方必须理解“当前 GuiManager”的隐藏选择规则。

影响：

- `currentGuiManager` 有死锁风险。
- 多插件注册 GUI 时，非当前 manager 的事件被过滤掉，后注册插件可能无法收到 GUI 事件。
- 清理任务可能因离线玩家返回 null 而 NPE。
- 任意 enabled plugin 作为调度 owner 会污染生命周期归属。

更好的方向：

- 以 player/session 为事件路由事实源，而不是全局 current manager。
- 把 `SessionStore`、`ChatInputRegistry`、`GuiEventRouter`、`GuiSchedulerOwner` 拆成内部 Module，外部仍保留 `GuiManager` 作为主 Interface。
- `GuiManager` close 时只释放自己 owns 的 session/page/handler，避免跨插件清理。

## F4. GUI Page/Component 生命周期共享资源被重复关闭

证据：

- `BasePage.destroyInternal()` 设置 `closed = true` 后调用 `clearComponents()`：`modules/gui/src/main/kotlin/city/newnan/gui/page/BasePage.kt:328`。
- `BaseComponent.close()` 会关闭 `scheduler`：`modules/gui/src/main/kotlin/city/newnan/gui/component/base/BaseComponent.kt:124`、`BaseComponent.kt:133`。
- `Page.scheduler` 实际来自 page：`BaseComponent.kt:43` 到 `BaseComponent.kt:45`。
- `Session.close()` 先设置 `closed = true` 再调用 `clearPages()`，而 `clearPages()` 遇到 closed 直接 return：`modules/gui/src/main/kotlin/city/newnan/gui/session/Session.kt:280`、`Session.kt:284`、`Session.kt:287`、`Session.kt:295`。
- `Session.goto()` 读锁计算 index，之后写锁使用旧 index：`Session.kt:196`、`Session.kt:197`、`Session.kt:216`。

问题：

Page 和 Component 的资源 ownership 不清楚。Component 的 Interface 暴露为独立 Terminable，但它关闭了 page 级 scheduler，这是上层共享资源。Session close 和 Page destroy 也都出现“先标记 closed，再执行清理”的反向顺序。

影响：

- 关闭一个 Component 可能关闭整个 Page 的 scheduler，影响同页其他组件。
- Session close 可能不销毁页面，造成 Page/Component 资源泄漏。
- Page destroy 设置 closed 后再清组件，若组件清理路径依赖页面状态会被短路。

更好的方向：

- Component 只能关闭自己 bind 的资源，不关闭 page scheduler。
- Page 拥有 page scheduler；Session 拥有 page 栈；GuiManager 拥有 Session 归属。
- close 流程统一成 `closing` -> release children -> mark `closed`，不要用同一个 `closed` 同时表示“禁止新操作”和“已完成释放”。

## F5. Config 路径语义和缓存语义不集中

证据：

- `Config.parsePath()` 是私有实现：`modules/config/src/main/kotlin/city/newnan/config/Config.kt:59`。
- 尾部转义没有落盘到 `currentPart`：`Config.kt:62`、`Config.kt:71`、`Config.kt:84`。
- `JsonExtensions` 使用 `path.split(".")`：`modules/config/src/main/kotlin/city/newnan/config/extensions/JsonExtensions.kt:27`、`JsonExtensions.kt:188`、`JsonExtensions.kt:234`、`JsonExtensions.kt:278`、`JsonExtensions.kt:305`、`JsonExtensions.kt:330`。
- 非 ObjectNode 会静默变空对象：`modules/config/src/main/kotlin/city/newnan/config/ConfigManager.kt:309`、`ConfigManager.kt:544`、`ConfigManager.kt:611`。
- `saveInternal(Config)` 写缓存时 key 只有 `"${file.canonicalPath}:"`：`ConfigManager.kt:435`，但 `get()` 查的是带类型名的 key：`ConfigManager.kt:282`。
- `deepMerge()` 每层 `existing.deepCopy()`：`ConfigManager.kt:654`。

问题：

Config Module 的 Interface 看似是“路径字符串 + 类型安全 parse”，但路径、缓存、合并这三个核心语义散落在多个 Implementation 中。调用方无法从 Interface 判断 `a\\.b` 在不同扩展方法中是否一致，也无法知道非法根节点会被清空。

影响：

- 带点 key、尾部反斜杠 key 行为不一致。
- 非对象配置根节点被静默覆盖为 empty object，数据安全风险高。
- 保存 Config 后缓存可能不生效或污染，reload 行为不可靠。
- 大配置 deepMerge 有额外空间/时间成本。

更好的方向：

- 抽出公开内部 Module `ConfigPath`，所有 path Interface 复用。
- 对非 ObjectNode fail fast；如果确实只支持对象根，写入文档和异常类型。
- 统一 cache key builder，所有 get/parse/save/clear 走同一函数。
- deepMerge 原地补缺失键，返回是否 changed，避免用结构相等判断变更。

## F6. i18n 模板替换和 reload 健康状态过于宽松

证据：

- 初始化时先 `guessMajorLanguage()`，但此时 languageMap 还为空：`modules/i18n/src/main/kotlin/city/newnan/i18n/LanguageManager.kt:73`、`LanguageManager.kt:214`。
- `reloadAll()` 收集错误但返回 this，不抛出：`LanguageManager.kt:226`、`LanguageManager.kt:250`。
- `processTemplate()` 在 matcher 遍历期间修改 `processedText`：`LanguageManager.kt:285`、`LanguageManager.kt:314`。
- `setupLanguageManager()` 创建、注册、设置 provider、reload 都在一个扩展函数中：`modules/i18n/src/main/kotlin/city/newnan/i18n/extensions/BasePluginExtensions.kt:31`、`BasePluginExtensions.kt:41`、`BasePluginExtensions.kt:44`。

问题：

i18n 的 Interface 把注册、选择语言、设置 provider、reload 健康状态揉在一起。调用方无法知道 reload 是否部分失败；模板替换 Implementation 又让 matcher 和被匹配文本状态分离。

影响：

- 重复模板变量或替换文本包含模板语法时可能产生非预期替换。
- 语言 reload 失败后插件继续运行，用户看到 key 或旧值。
- auto detect 在注册前执行，实际价值低。

更好的方向：

- 用 `Matcher.appendReplacement/appendTail` 或 Kotlin sequence 构建替换结果。
- `reloadAll()` 返回 `ReloadReport` 或在 strict 模式抛聚合异常。
- `setupLanguageManager()` 保持便捷入口，但底层拆出注册、选择、provider wiring 三个内部步骤，方便测试。

## F7. Network 同时过度设计和设计不足

证据：

- 全局 `httpClientManagers` 只按 name 隔离：`modules/network/src/main/kotlin/city/newnan/network/NetworkExtensions.kt:15`、`NetworkExtensions.kt:20`。
- `BasePlugin.networkManager` 又有另一套 `mutableMapOf<BasePlugin, HttpClientManager>()`：`modules/network/src/main/kotlin/city/newnan/network/extensions/BasePluginExtensions.kt:12`。
- `NetworkDSLBuilder.client()` 构建 config 后调用私有 `createHttpClient()`：`modules/network/src/main/kotlin/city/newnan/network/NetworkDSL.kt:25`、`NetworkDSL.kt:29`。
- 私有 `createHttpClient(name, config)` 忽略 config，直接返回 `getHttpClient(name)`：`NetworkDSL.kt:238`。
- `BasePlugin.httpGet` 每次新建 builder：`NetworkDSL.kt:201`、`NetworkDSL.kt:202`。
- `SimpleNetworkMonitor.close()` 空实现：`NetworkDSL.kt:160`、`NetworkDSL.kt:166`。
- `TerminableHttpClientWrapper.close()` 关闭底层共享 client：`modules/network/src/main/kotlin/city/newnan/network/TerminableNetworkExtensions.kt:72`、`TerminableNetworkExtensions.kt:82`。
- 每次 `terminableGet/Post/...` 都创建 wrapper：`TerminableNetworkExtensions.kt:104`、`TerminableNetworkExtensions.kt:178`、`TerminableNetworkExtensions.kt:194`、`TerminableNetworkExtensions.kt:210`、`TerminableNetworkExtensions.kt:226`、`TerminableNetworkExtensions.kt:242`、`TerminableNetworkExtensions.kt:258`、`TerminableNetworkExtensions.kt:293`。
- `downloadToByteArray()` 先 `client.get()`，再检查 `Content-Length`，最后 `readBytes()`：`modules/network/src/main/kotlin/city/newnan/network/NetworkUtils.kt:98`、`NetworkUtils.kt:110`。

问题：

Network Module 有太多 Interface：全局函数、BasePlugin 扩展、DSL builder、Terminable wrapper、manager。多数只是 pass-through Adapter，但没有把真正复杂的 ownership、config、cancellation、streaming size limit 封装起来。

影响：

- 插件 A 和插件 B 都用 `"default"` 时可能共享 manager/client。
- wrapper 关闭共享 client 会影响同名客户端的其他使用者。
- DSL 配置看似生效，实际被忽略。
- monitor 绑定到生命周期但 close 不释放任何任务。
- 下载大小限制无法防止未知 Content-Length 的大响应占满内存。

更好的方向：

- 选定唯一外部 Interface：`BasePlugin.networkManager` 或 `plugin.network {}`，不要两套归属。
- HttpClient key 应包含 plugin identity + logical name + config fingerprint。
- Terminable wrapper 只拥有 Job，不拥有共享 HttpClient；底层 client 由 manager close。
- 删除或降级 pass-through DSL；保留有真实 Leverage 的接口，例如 typed request、retry、streaming download、lifecycle-bound jobs。

## F8. 测试和验证 Interface 不足

证据：

- 根构建文件禁用所有 Test task：`build.gradle.kts:73`。
- 文档仍把测试作为质量保证方向，但当前 build 不执行 tests。

问题：

这些 Module 大量涉及生命周期、并发、缓存和资源释放，但测试 Interface 被关闭。架构风险不能靠 `buildAllPlugins` 发现。

更好的方向：

- 至少恢复 modules 层纯 JVM 测试：config path/cache、i18n template、network ownership key、scheduler state machine。
- Bukkit/Paper 相关行为单独走 integration 或 runServer smoke，不要阻塞纯单元测试。
