# Public Interface Surface Map

## 定位

本页把当前 core/modules 中已经暴露给业务插件的能力归类，并映射到下一代框架的 Runtime 归属。它不是要求照搬当前类名，而是帮助后续开发识别哪些能力已经被使用、哪些边界应该收深、哪些能力需要保留为稳定 Interface。

## 本轮源码线索

本页依据本地 README 和源码轻量扫面整理，重点线索包括：

- `modules/core/README.md`
- `modules/config/README.md`
- `modules/i18n/README.md`
- `modules/gui/README.md`
- `modules/network/README.md`
- `modules/core/src/main/kotlin/city/newnan/core/base/BasePlugin.kt`
- `modules/core/src/main/kotlin/city/newnan/core/base/BaseModule.kt`
- `modules/core/src/main/kotlin/city/newnan/core/terminable/`
- `modules/core/src/main/kotlin/city/newnan/core/scheduler/`
- `modules/core/src/main/kotlin/city/newnan/core/event/`
- `modules/core/src/main/kotlin/city/newnan/core/message/`
- `modules/config/src/main/kotlin/city/newnan/config/ConfigManager.kt`
- `modules/i18n/src/main/kotlin/city/newnan/i18n/LanguageManager.kt`
- `modules/gui/src/main/kotlin/city/newnan/gui/`
- `modules/network/src/main/kotlin/city/newnan/network/`

## Surface Map

| 当前能力来源 | 典型形态 | 新 Runtime 归属 | 处理方向 |
| --- | --- | --- | --- |
| `BasePlugin` | plugin lifecycle、logger、messager、configManager、service、task、listener | Lifecycle/OwnerRuntime + RuntimeContext | 保留“插件上下文”概念，但拆出 owner、runtime registry、diagnostics |
| `BaseModule` | moduleName、child module、onInit/onReload/onClose、resource close | Lifecycle/OwnerRuntime | 升级为 owner tree，明确 state、close order、resource graph |
| `Terminable` family | `Terminable`、`TerminableConsumer`、`CompositeTerminable`、task/listener wrapper | Lifecycle/OwnerRuntime | 保留资源归属思想，补充 close report、failure aggregation、diagnostics |
| scheduler | `IScheduler`、`ITaskHandler`、`runSync/runAsync/runLater/runRepeating` | TaskRuntime | 从 sync/async 升级为 affinity + policy + outcome + cancellation |
| event | `subscribeEvent`、builder、filters、expiry | EventRuntime | 保留 scoped subscription，补齐 ignoreCancelled、affinity、backpressure、event-task bridge |
| command | Cloud annotation usage in plugins | CommandRuntime + Plugin Spec Generation | 继续走声明式命令，补静态 yml 生成、descriptor、owner、diagnostics |
| message/text | `MessageManager`、`IMessager`、text utilities、LanguageProvider | TextRuntime + DiagnosticsRuntime | 从即时消息工具升级为 catalog、key、audience、render report |
| config | `ConfigManager`、format registry、parse/save/touchWithMerge、cache | ConfigRuntime + CacheRuntime | 从文件工具升级为 schema、snapshot、reload transaction、invalidation |
| i18n | `LanguageManager`、locale registry、translation cache | TextRuntime | 合并到 message catalog、fallback、placeholder、reload report |
| gui | `GuiManager`、`SessionStorage`、`Session`、`Page`、component、SmartCache | GuiRuntime + CacheRuntime | 保留 session/page/component，补 owner、render diff、data provider、diagnostics |
| network | Ktor client manager、HTTP DSL、typed request、monitor、retry、terminable client | NetworkRuntime | 收敛到 named client、policy、redaction、health、request outcome |
| cache | LRU/LFU/Infinite、SmartCache、translation cache、config cache | CacheRuntime | 统一 TTL、max size、version invalidation、stats、single-flight |
| database/storage patterns | plugin managers 中手写 load/save/cache | PersistenceRuntime | 抽出 repository executor、batch writer、flush lifecycle、close drain |
| logging/performance | enhanced logger、performance monitor | DiagnosticsRuntime | 统一为 report、health dump、structured sink |
| service discovery | Bukkit services and plugin-specific services | IntegrationRuntime | 升级为 typed capability、provider health、required/optional state |

## Stable Interface Candidates

后续开发应优先稳定这些概念，而不是优先稳定具体类名：

- `Owner`: 所有资源、task、event、GUI session、client、cache 的归属。
- `RuntimeContext`: 业务 Module 获取 runtime 能力的唯一入口。
- `Affinity`: Bukkit/Paper/Folia 执行域表达。
- `Policy`: timeout、retry、rate limit、cache ttl、flush、backpressure。
- `Snapshot`: config、text、capability、diagnostics 的当前事实。
- `Report`: reload、failure、slow task、health、flush、request 的结构化输出。
- `Descriptor`: plugin metadata、command、permission、config、message、integration 的构建期事实源。
- `Adapter`: Paper/Bukkit/Ktor/Hikari/Cloud/Adventure/third-party provider 的绑定层。

## Internal Leakage To Reduce

当前能力中有几类容易泄漏到业务心智：

- 业务代码直接知道 scheduler/task wrapper 的具体实现。
- 业务代码直接保存 listener/task/client/cache 等长期对象。
- 配置 reload 的文件、parse、cache、language setup 顺序散落在插件内。
- GUI render、data load、event routing 和 session cleanup 交织。
- 网络请求的 retry、timeout、redaction 和 owner close 没有统一事实源。
- config-as-cache 或 hand-written map 缺少 flush lifecycle 和 diagnostics。

下一代设计应把这些复杂性收回 Runtime Implementation，只把意图、policy 和 outcome 暴露给业务。

## Development Implication

开发时不应先做一个“大 BasePlugin”。更稳的顺序是：

1. 先实现 OwnerRuntime 和 RuntimeContext skeleton。
2. 用 DiagnosticsRuntime 和 Harness 证明 owner/resource/task/report 能被观测。
3. 再逐个 Runtime 接入：Task、Event、Command、Plugin Spec、Config/Text、GUI、Network、Persistence/Cache、Integration。
4. 每接入一个 Runtime，都补 contract tests 和一个最小 sample。

详细执行切片见 [../02-runtime/development-slices.md](../02-runtime/development-slices.md)。
