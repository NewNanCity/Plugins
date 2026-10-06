# Roadmap Outline

这份文档是 framework-next 从 0 到 1 的长期地图。它的目的不是记录已经做过什么，而是防止后续自动开发再次跑偏。

任何人或 agent 按这份路线图推进时，都必须满足每个里程碑的验收门再进入下一步。没有通过验收门的能力不能在文档中宣称完成，也不能进入 `best-practice-plugin` 作为推荐写法。

## Global Rules

## 事实源顺序

1. [02-architecture-and-best-practice.md](02-architecture-and-best-practice.md): 最终开发者体验。
2. [api-contracts.md](api-contracts.md): public API 契约。
3. [samples-policy.md](samples-policy.md): 样例治理和 first-screen 边界。
4. 本路线图。
5. [verification-matrix.md](verification-matrix.md): 证据类型和完成标准。
6. [implementation/*](implementation/README.md): 模块实现细节。
7. 当前或未来代码。

代码不能反向改写目标体验。若实现成本显著超出预期，先回到设计文档讨论取舍，而不是让 first-screen API 变重。

## 每个里程碑的固定产物

每个里程碑必须交付：

- public API 或 internal API 的明确归属。
- runtime 或 build-tool 实现。
- unit/contract tests。
- 必要 adapter tests。
- sample 更新，优先 `best-practice-plugin`。
- source hygiene 或 API boundary check。
- README/docs 更新。
- unreleased 记录。

## 每个里程碑的固定验收

进入下一里程碑前必须满足：

```powershell
.\gradlew.bat check
.\gradlew.bat frameworkNextReleaseCheck
```

如果阶段尚未拥有 release gate，则必须至少有等价的 targeted checks，并在阶段文档里写清缺口。

## Stop Rules

出现以下情况立即停止继续实现，先修设计：

- `best-practice-plugin` 需要 `services.get<T>()`、provider、descriptor writer、manual owner、direct adapter 才能表达业务。
- 一个事实需要在两个地方维护，例如 command metadata 和 handler 分离后还要手动同步。
- 测试只能证明底层 kernel，不能证明推荐写法。
- 为了让旧实现可用而污染新 public API。
- 文档说 done，但 sample 或 test 只能证明 partial。

## Stage 0: Direction Freeze And Clean Skeleton

### 目标

把新项目和旧实现彻底隔离，建立最小可构建骨架和第一事实源。

### 什么时候做

新 repo 创建后的第一阶段。任何 runtime 功能实现前先完成。

### 做到什么程度

- 独立 Gradle build。
- Java 17 bytecode target。
- Paper 1.20.1+ baseline 写入 build/docs。
- 初始模块：
  - `runtime-core`
  - `runtime-plugin`
  - `adapter-paper`
  - `framework-gradle-plugin`
  - `samples/best-practice-plugin`
  - `samples/dogfood-plugin`
- `best-practice-plugin` 只允许 facade-first 壳，不要求功能完整。
- 文档复制本目录作为新项目 `docs/framework-next` 初始事实源。

### 测试怎么写

- `BuildBaselineTest`: 检查 JVM target 是 17。
- `ModuleGraphSmokeTest`: 所有 Gradle subproject 可配置。
- `DocsLinkTest`: docs/framework-next 内部链接可达。
- `SampleBoundaryTest`: `best-practice-plugin` 不包含旧框架包名或低层 runtime primitive。

### 验收标准

- `.\gradlew.bat check` 通过。
- `best-practice-plugin` 存在并可编译到空壳。
- 没有旧 `BasePlugin` / `BaseModule` 类型。
- 没有手写 `plugin.yml` 作为主事实源。

## Stage 1: Core Owner And Lifecycle

### 目标

建立所有 runtime 能力共享的 owner tree、resource binding、close report、diagnostics base。

### 什么时候做

所有 task/event/gui/network/cache/persistence 之前。

### 做到什么程度

- `OwnerId` 全局唯一。
- `OwnerPath` human-readable。
- `Owner.child(...)`。
- `Owner.bind(resource)`。
- bottom-up close。
- close timeout。
- idempotent close。
- leak detection。
- `CloseReport` 可序列化。
- diagnostics source registry。

### 测试怎么写

- contract test 不依赖 Bukkit。
- 构造 owner tree，绑定 fake resources，验证关闭顺序。
- 让某个 resource close 失败，验证 report 保留失败并继续关闭其他资源。
- 重复 close 两次，验证第二次无副作用。
- 超时 resource 触发 timeout report。
- debug mode 才记录 source location。

### 验收标准

- `runtime-core:test` 通过。
- owner close order、leak、timeout、failure report 有覆盖。
- Diagnostics 可以 dump owner tree。
- 没有任何 Paper/Bukkit 依赖进入 `runtime-core`。

## Stage 2: Plugin Facade And Build-Time Metadata

### 目标

让插件作者能写出目标第一屏 shell，并能从同一事实源生成静态 Paper metadata。

### 什么时候做

Core owner 完成后立即做。Config/command/event/gui 都依赖这个声明模型。

### 做到什么程度

- `PaperPlugin({ ... })`。
- `config<T>()` descriptor stub。
- `messages<T>()` descriptor stub。
- `module<T>()` descriptor stub。
- Gradle DSL 提供 id/name/main/apiVersion。
- 生成最小 `paper-plugin.yml`。
- jar verification。
- `best-practice-plugin` 使用 shell，不使用 `FrameworkPlugin`、`PluginSpec`、provider interface。

### 测试怎么写

- Gradle functional test：执行生成任务，读取输出 yml。
- golden `paper-plugin.yml`。
- jar entry test：确认 yml 打进 jar。
- mismatch test：Gradle DSL 和源码声明冲突时报错。
- source hygiene test：扫描 best-practice source。

### 验收标准

- `best-practice-plugin` 可生成合法 `paper-plugin.yml`。
- 生成物不提交仓库。
- plugin metadata 只有一个事实源。
- README 明确 best-practice 是复制目标。

## Stage 3: Config And Text First-Screen API

### 目标

让配置和文本以 typed class/interface 出现在模块构造函数里，而不是 runtime key 或 template object。

### 什么时候做

Plugin/build metadata 完成后。Command 和 GUI 的描述文本会依赖 TextRuntime。

### 做到什么程度

- `config<T>("config.yml")` 生成 schema。
- comment-preserving YAML adapter。
- unknown key warning / strict error。
- reload transaction。
- failed reload keeps old snapshot。
- restart-required diff。
- `messages<T>("lang/{locale}.yml")` 生成 catalog schema。
- typed text catalog proxy 或 generated implementation。
- MiniMessage validation。
- missing/unused key reports。

### 测试怎么写

- schema inference test。
- default config file generation test。
- comment preserve/merge test。
- validation failure retention test。
- restart-required field diff test。
- typed text method -> catalog key mapping test。
- invalid MiniMessage test。
- fallback locale test。
- owner-scoped placeholder cleanup test。

### 验收标准

- `MarketModule(private val config: MarketConfig, private val text: MarketText)` 可在 best-practice sample 编译。
- sample 不出现 `ConfigRuntime`、`ConfigKey`、`TextRuntime`、`MessageKey`。
- reload report 能说明 changed/invalid/restartRequired/missing keys。

## Stage 4: Command Runtime And Descriptor Convergence

### 目标

命令声明、权限、描述、参数、校验、补全、help、handler、descriptor、static yml 归到一个 `command("/...")` 块。

### 什么时候做

Config/Text 完成后，因为 command description、arg description、error rendering 依赖 typed text。

### 做到什么程度

- usage-shape parser。
- required `<arg>`。
- optional `[arg]`。
- default `[amount=1]`。
- flags/options `[--silent]`。
- sender policy。
- int/float/bool/string。
- Player/World/Material/LocationSnapshot/UUID。
- enum alias。
- union `oneOf<T>`。
- custom converter/validator。
- async suggestion provider with timeout/limit。
- localized help/error renderer。
- descriptor generation。
- Paper/Cloud adapter registration hidden from sample。

### 测试怎么写

- parser tests for usage strings。
- command descriptor golden tests。
- sender mismatch tests。
- argument conversion tests。
- validation failure tests。
- suggestion timeout and truncation tests。
- help/error localization tests。
- adapter test: MockBukkit command can execute。
- source hygiene: no `registerPaper`, no direct Cloud manager in best-practice。

### 验收标准

- best-practice command looks like target API。
- command root and permission enter generated yml。
- runtime command tree and generated descriptor come from the same declaration。
- all command failures produce structured diagnostics。

## Stage 5: Task Runtime

### 目标

建立 owner-bound structured tasks，并把 affinity、timeout、failure、join/race 和 diagnostics 做成所有 runtime 共享能力。

### 什么时候做

Command runtime 后，Event/GUI/Network/Persistence 都会派生 task。

### 做到什么程度

- `tasks.io/cpu/main/entity/region`。
- timeout。
- cancellation reason。
- failure outcome。
- owner close cancellation。
- main-thread budget diagnostics。
- join。
- race。
- `every` / delay。
- first-screen API 默认返回业务值；`TaskHandle/TaskOutcome` 作为 advanced。

### 测试怎么写

- fake scheduler。
- affinity dispatch tests。
- timeout tests。
- cancellation tests。
- owner close cancels children。
- join success/failure tests。
- race winner cancels losers。
- budget warning tests。

### 验收标准

- normal sample code 不需要手动 unwrap `TaskOutcome`。
- disable plugin 后 active tasks 为 0。
- task diagnostics 能列出 active/queued/completed/failed/timedOut。

## Stage 6: Event Runtime And Event-Task Bridge

### 目标

把 event 变成 scoped passive runtime，并和 task owner/affinity 合并。

### 什么时候做

TaskRuntime 完成后。事件 handler 常派生 task。

### 做到什么程度

- module-scope `on<T>()`。
- priority。
- ignoreCancelled。
- MONITOR read-only warning/strict fail。
- ordered filters。
- permission helper。
- once-by-handled。
- throttle。
- debounce with snapshot。
- async event affinity guard。
- domain events。
- `eventScope/await`。
- `events.race`。
- Paper event adapter。

### 测试怎么写

- fake EventRuntime contract tests。
- filter order and count tests。
- handled-only once test。
- priority order test。
- cancellation/ignoreCancelled adapter tests。
- throttle/debounce clock-controlled tests。
- snapshot test ensuring mutable event is not retained。
- eventScope timeout cleanup。
- race cleanup。
- PlayerQuit-style owner close cleanup。

### 验收标准

- best-practice event code never passes `context.owner`。
- high-frequency event has documented backpressure。
- async event cannot touch Bukkit mutable state without explicit task switch。
- event diagnostics expose seen/matched/handled internally, but public expiry stays handled-based。

## Stage 7: GUI Runtime And Paper Inventory Safety

### 目标

让业务用 `screen/page/slot` 表达 UI，框架处理 session、page、routing、render、slot policy 和 Paper inventory 安全。

### 什么时候做

Event/Task 完成后。GUI click/close/quit 依赖 EventRuntime，async data 依赖 TaskRuntime。

### 做到什么程度

- `screen("name")`。
- default/named session。
- TTL。
- max stack depth。
- page stack。
- title。
- inventoryPolicy。
- button。
- list with stable row key。
- inputSlot。
- outputSlot。
- storageSlot explicit opt-in。
- render diff。
- stale generation rejection。
- Paper adapter for open/click/drag/close/quit。
- object protection deny-by-default。

### 测试怎么写

- neutral GuiRuntime contract tests。
- list stable key and stale generation tests。
- inputSlot allow/deny/return tests。
- outputSlot one-time commit tests。
- shift-click/hotbar/drop/drag protection tests。
- page stack tests。
- player quit closes session。
- MockBukkit/Paper adapter click routing tests。

### 验收标准

- best-practice sample contains one real screen。
- sample never uses direct Paper GUI adapter。
- GUI diagnostics can list open sessions/pages and denied actions。
- known duplication paths are covered by tests。

## Stage 8: Optional Runtime Lane

### 目标

补齐 cache、persistence、network、integration，但不让它们污染 core 或 first-screen minimal path。

### 什么时候做

Core first-screen path稳定后。不要在 command/event/gui 未完成前插入重依赖 optional runtime。

### 做到什么程度

- CacheRuntime interface and in-memory test implementation。
- Caffeine adapter after dependency review。
- Persistence batch writer。
- FileSnapshotStore。
- NetworkRuntime interface。
- Ktor/OkHttp adapter after dependency review。
- redaction。
- circuit breaker optional policy。
- Integration capability registry。
- optional official adapters only when needed。

### 测试怎么写

- cache ttl/size/invalidation。
- cache owner cleanup。
- batch writer merge/flush/drain timeout。
- FileSnapshotStore atomic write/checksum/backup。
- request timeout/retry/cancel/redaction。
- capability available/unavailable/degraded。
- provider state change event。

### 验收标准

- optional artifacts can be omitted without breaking best-practice minimal plugin。
- no DB driver enters core。
- no HTTP client type enters business public API。
- diagnostics show cache/network/writer/capability states。

## Stage 9: Diagnostics And Operator Tools

### 目标

让开发者和服主能看见 framework runtime 状态，而不是靠猜日志。

### 什么时候做

各 runtime 有基本能力后贯通。不要等最后才补 diagnostics。

### 做到什么程度

- owner tree dump。
- active tasks。
- event subscriptions。
- command registry。
- open GUI sessions。
- config/text snapshots。
- cache stats。
- persistence queues。
- network clients/requests。
- integration capabilities。
- recent reports ring buffer。
- `/plugin diag` Paper command。
- JSON export。

### 测试怎么写

- JSON schema tests。
- ring buffer limit tests。
- permission test for diag command。
- export path test。
- each runtime diagnostic source test。
- source location debug mode test。

### 验收标准

- operator can run one command and get useful summary。
- failed reload/command/task/event/gui action appears in diagnostics。
- diagnostics command can be disabled。
- output redacts secrets。

## Stage 10: Release Gates And API Freeze

### 目标

冻结 1.0 前 public API，确保文档、样例、测试、生成物和 Paper smoke 互相证明。

### 什么时候做

所有 first-screen runtime 完成后，optional runtime 至少完成当前声明范围。

### 做到什么程度

- API tier scan。
- forbidden first-screen primitive scan。
- binary/source compatibility posture。
- release gate task。
- docs link check。
- generated artifact check。
- best-practice sample check。
- dogfood integration check。
- Paper 1.20.1+ smoke。
- release candidate audit。

### 测试怎么写

Release gate 聚合：

```powershell
.\gradlew.bat check
.\gradlew.bat frameworkNextReleaseCheck
.\gradlew.bat :samples:best-practice-plugin:verifyGeneratedArtifacts
```

还需要人工或自动记录：

- Paper 1.20.1+ server boot evidence。
- generated jar entries。
- diagnostics command output sample。
- API diff from previous RC。

### 验收标准

- `best-practice-plugin` 是推荐复制目标。
- 所有 public docs 指向可编译 API。
- 所有 target-only docs 明确标记为未来能力。
- release candidate audit 没有 `dx-drift` 行。
- 旧实现没有进入新 public API。

## Milestone Summary

| Stage | Theme | Must prove before moving on |
| --- | --- | --- |
| 0 | Direction and skeleton | 新项目独立、baseline 固定、first sample 无旧接口 |
| 1 | Core owner/lifecycle | 所有资源可归属、关闭、诊断 |
| 2 | Plugin/build | `PaperPlugin` shell 和 static yml 来自同一事实 |
| 3 | Config/text | typed config/text 进入模块构造函数 |
| 4 | Command | 一个 command block 覆盖 metadata、parser、handler、help |
| 5 | Task | owner-bound affinity tasks 可诊断可取消 |
| 6 | Event | scoped event 和 event-task bridge 无 owner 泄漏 |
| 7 | GUI | screen/page/slot DSL 和 Paper safety 可证明 |
| 8 | Optional runtimes | cache/persistence/network/integration 不污染 core |
| 9 | Diagnostics | operator 能看到 runtime 状态 |
| 10 | Release | docs、API、sample、tests、Paper smoke 全部对齐 |

## How Agents Should Use This Outline

每次自动开发前：

1. 选择一个且只能一个 active stage。
2. 读该 stage 对应的 implementation doc。
3. 写本 stage 的小计划和验证命令。
4. 先写或更新测试，再实现。
5. 更新 `best-practice-plugin` 或明确说明为什么只影响 advanced/internal sample。
6. 跑本 stage 验证。
7. 更新 docs、TODO、unreleased。
8. 不满足验收门时，不进入下一 stage。

