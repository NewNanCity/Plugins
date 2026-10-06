# Decision Recommendations

## 定位

本页记录 2026-06-19 这轮拍板结果，以及需要框架侧给出确定方案的问题。它覆盖各 Runtime 文档中的 `开放问题`，后续实现时以本页为准；各 Runtime 文档保留问题原文用于追溯上下文。

状态说明：

- `拍板`: 用户已经明确决定。
- `框架推荐`: 用户要求框架给出方案，本页给出当前推荐。
- `实施前复核`: 涉及第三方库维护状态或平台细节，进入实现前再确认版本和风险。

## 顶层边界

| 问题 | 决策 | 状态 |
| --- | --- | --- |
| 新框架是否兼容旧 `BasePlugin` / `BaseModule` 源码入口 | 不兼容旧入口，不背历史包袱。新框架按干净的最新设计重新定义 Interface。 | 拍板 |
| 旧 `BaseModule.bind(Terminable)` 是否作为 Adapter 暂时保留 | 不保留。资源归属统一进入 OwnerRuntime。 | 拍板 |
| Folia 是近期还是中期目标 | Folia 是中期目标。近期只在 Affinity 模型里预留，不承诺 Folia adapter。 | 拍板 |
| root Test 禁用原因 | 先按历史遗留处理，推测来自旧框架不稳定或 project 配置不稳定。后续要做事实调查。 | 拍板 |
| 是否引入 MockBukkit / Testcontainers | 要引入，这是刚需。目标是减少只靠启动真实 server 测试的成本。 | 拍板 |
| golden sample 是否提交为稳定契约 | 测试用 golden sample 提交仓库；生产生成的 descriptor 不提交。 | 框架推荐 |
| PersistenceRuntime 是否进 core | DB 持久化外置为 optional runtime module，driver 由插件自己引入，避免 framework core 臃肿。 | 拍板 |

## Platform Baseline

| 项 | 决策 |
| --- | --- |
| Minecraft / Paper | 必须覆盖生产服务器 `Minecraft/Paper 1.20.1+`。public Interface 不依赖 1.20.5+ 专属行为。 |
| Paper newer versions | 1.20.5+ 的 Mojang-mapped runtime 只能作为 adapter 优化条件，不能成为核心基线。 |
| Java runtime target | 产物字节码目标使用 Java 17，保证 1.20.1 生产环境可用。构建工具链可以使用 Java 21+，但不能泄漏 Java 21 字节码到运行产物。 |
| Kotlin | 使用当前稳定 Kotlin 版本，JVM target 对齐 Java 17。实现时锁定具体版本，不追 RC。 |
| Gradle | 使用 wrapper 固定版本。新框架 isolated build 不依赖旧 root build 的 Test 状态。 |
| NMS / internals | 第一阶段不依赖 NMS。必须使用 Paper/Bukkit public API；版本差异通过 Adapter 和 capability gate 表达。 |

## Rewrite Isolation

采用“当前 repo + 新分支 + isolated `framework-next/` 子树”的方式开发。

建议目录：

```text
framework-next/
├── settings.gradle.kts
├── build.gradle.kts
├── runtime-core/
├── runtime-task/
├── runtime-event/
├── runtime-command/
├── runtime-config/
├── runtime-text/
├── runtime-gui/
├── runtime-network/
├── runtime-cache/
├── adapters/
├── samples/
└── docs/
```

隔离规则：

- 新框架源码只放 `framework-next/`。
- 旧 `modules/*` 和 `plugins/*` 只读，用作需求考古，不继承 Interface。
- 禁止从旧框架 re-export 类型。
- 禁止为了兼容旧插件保留旧命名。
- 新框架测试只跑 `framework-next` 自己的 Gradle tasks。
- 第一阶段 sample 使用最小 sample，不直接搬 TPA/FeeFly/Foundation 代码。

## Artifact Split

拆分原则：

- 高频、轻依赖、所有插件都需要的抽象进 core。
- 重依赖、场景窄、容易冲突的 Implementation 放 optional artifact。
- 第三方库绑定放 adapter artifact。
- Interface 可以进 core，Implementation 不一定进 core。

建议拆分：

| Artifact | 内容 |
| --- | --- |
| `runtime-core` | owner/lifecycle、diagnostics 基础、RuntimeContext、基础 report/snapshot/policy 类型 |
| `runtime-task` | TaskRuntime Interface、coroutine implementation、fake scheduler harness |
| `runtime-event` | EventRuntime Interface、fake event bus、Bukkit event adapter 后置 |
| `runtime-command` | command descriptor、permission/help/suggestion Interface、Cloud adapter 后置 |
| `runtime-config` | config schema/snapshot/reload report Interface、YAML adapter 后置 |
| `runtime-text` | message key/catalog/audience rendering Interface、Adventure adapter 后置 |
| `runtime-gui` | GUI session/page/component/render/event routing，optional |
| `runtime-network` | NetworkRuntime Interface，Ktor implementation 作为 optional 或 adapter |
| `runtime-cache` | CacheRuntime Interface；Caffeine adapter optional |
| `runtime-persistence` | DB/file persistence、batch writer、snapshot store，optional |
| `adapters-*` | Vault、LuckPerms、PlaceholderAPI、ProtocolLib、Ktor、Caffeine、MockBukkit、Testcontainers 等 |
| `samples:minimal` | 最小样例插件，驱动第一阶段验收 |

## Build-Time Descriptor

### 推荐方案

采用“Gradle Kotlin DSL + KSP 收集 + 生成任务校验”的组合：

1. 插件静态元数据、目标平台、依赖、权限默认值等写在 Gradle Kotlin DSL。
2. command、permission key、message key、config schema 等源码声明由框架自定义注解或 DSL 表达。
3. KSP 收集源码声明，输出 build 中间 descriptor。
4. Gradle `generatePluginMetadata` 任务合并 Gradle DSL 和 KSP descriptor，生成 `plugin.yml` / `paper-plugin.yml`。
5. Gradle `verifyPluginMetadata` 任务检查 jar 内静态 yml 与 descriptor 一致。

原因：

- Gradle 能稳定读取 build script 中的静态插件元数据，不需要执行任意业务代码。
- KSP 适合收集源码里的 command/config/text 声明。
- descriptor 是构建事实源，但不提交仓库，避免生成物噪音。
- golden sample 只作为测试 fixture 提交，用来防止生成格式漂移。

### yml 生成策略

- 如果 Gradle DSL 明确声明目标平台：只生成目标平台文件。
- 如果目标平台未知或声明多平台：同时生成 `plugin.yml` 和 `paper-plugin.yml`。
- 两个文件都很小，同时生成可接受，但校验任务必须明确说明哪个文件被目标平台使用。

## Plugin / Command

| 问题 | 决策 |
| --- | --- |
| Cloud 原生注解还是框架注解 | 定义框架自己的 command Interface 和注解/DSL，再适配 Cloud。Cloud 是 Adapter，不是业务事实源。 |
| descriptor 生成方式 | KSP 为主，Gradle source collector 只作为补充或 fallback。 |
| PluginSpec 初版来源 | 静态插件元数据走 Gradle Kotlin DSL；源码内 command/config/text 声明走 KSP。 |
| permission description key | 可选。缺失时默认用 permission node 或 humanized 文本，并给 warning；strict mode 可升级为 error。 |
| command description key | 可选。缺失时默认用 command literal 或 generated fallback，并给 warning；strict mode 可升级为 error。 |
| generated descriptor | 不提交仓库，只作为 build artifact。 |
| suggestion timeout | 默认 150ms warning，300ms soft timeout，500ms hard cap；可按 command 覆盖。 |
| suggestion result limit | 默认 50 条，硬上限 200 条；超过则截断并进 diagnostics。 |

## Runtime Semantics

| 问题 | 决策 |
| --- | --- |
| owner id | 使用全局唯一、时间有序的 128-bit id，表现形态可用 UUIDv7/ULID-style 短编码；同时保留 human-readable owner path。 |
| close timeout | 默认 plugin 10s、module 5s、session/page 2s、request/writer 3s；critical writer 可显式提高到 10s。 |
| optional Module dependency | 允许。required 缺失 fail module；optional 缺失 module 进入 degraded，并产生 warning/report。 |
| source location diagnostics | 记录，但默认只在 debug mode 或 `diagnostics.sourceLocation=true` 开启。使用注册时 StackWalker/编译期元数据，避免常态运行开销。 |
| Region 输入 | 使用 framework `LocationSnapshot`，包含 world key、block/chunk 坐标、精确坐标、yaw/pitch、entity id 等可选字段；提供 Bukkit `Location`、Block、Chunk、Entity 快捷构造。 |
| executor pool | `AsyncCpu = max(1, cores - 1)`；`AsyncIo = min(64, max(4, cores * 4))`；队列有界，允许 server config 覆盖。 |
| TaskChain | 初版用普通 continuation，Interface 预留 typed DSL 扩展点。 |
| 主线程 budget | 默认 event handler warning 1ms；main/entity/region task warning 2ms、error 5ms；可按 server profile 覆盖。 |
| coroutine | 作为内部默认 Implementation，统一 structured cancellation；同时提供 Future/BukkitTask 等 Adapter。 |

## Event / GUI

| 问题 | 决策 |
| --- | --- |
| Event expiry | 默认只暴露 `handled(n)`。`seen/matched` 不进入初版公共 Interface。 |
| MONITOR read-only | 开发期 warning；strict/test mode fail fast。 |
| async event affinity | 内置 `EventAffinityResolver`。同步 Bukkit event 默认 Main；已知 async event 默认 Source/AsyncEvent，不允许直接触碰 Bukkit mutable state；需要 Bukkit 行为时必须通过 TaskRuntime 显式切 affinity。未知 async event 和 packet callback 必须显式声明或走 Adapter。 |
| DomainEventBus async | DomainEventBus 默认同步派发；异步派发统一通过 TaskRuntime，不另开一套 async dispatch。 |
| ProtocolLib packet listener | 放在 IntegrationRuntime Adapter，转接成框架事件或 capability，不让 EventRuntime 直接依赖 ProtocolLib。 |
| Page identity | 使用 framework registry identity。InventoryHolder/persistent key 只作为平台 Adapter 定位手段。 |
| named session | 允许，但默认只创建 `default`。业务显式创建 named session 时必须声明 owner、TTL、最大 stack 深度和关闭策略。 |
| GUI title 更新 | 支持平台差异 Adapter；默认 fallback 是 reopen。 |
| ItemDescriptor cache | 缓存静态模板和 player-independent ItemDescriptor hash；最终 ItemStack 若含 player/locale/dynamic placeholder 默认不长期缓存，除非声明 cache policy。 |
| ObjectProtection | 默认 deny-by-default。GUI item 是展示对象；可交互 storage slot 必须显式 opt-in。 |

## Config / Text

| 问题 | 决策 |
| --- | --- |
| Config 默认 Adapter | 初版目标是 comment-preserving YAML Adapter。实现优先级：保留管理员注释和顺序 > Jackson 纯对象映射速度。Jackson 保留为 JSON/typed conversion/现有代码复用 Adapter。实施前复核 Okaeri/Configurate/Jackson 组合。 |
| unknown key | 默认 warning + reload report；strict/test mode error。 |
| restart required field | 通过 schema DSL 或注解声明，reload report 展示 changed fields、old/new safe summary 和 `restartRequired=true`。 |
| config comments | 是硬要求。管理员手动维护配置是常见场景。 |
| MessageKey | KSP 从 catalog/schema/源码声明生成 `Keys`，runtime 校验作为防线。 |
| unused catalog key | 默认 warning；strict/test mode error。 |
| Legacy 格式 | 新 catalog 默认不自动猜 Legacy。Legacy 需要 catalog 或 entry 显式声明；可提供 compatibility mode。 |
| placeholder resolver | 支持 owner scope 注册，owner close 自动注销。 |

## Network / Persistence / Cache

| 问题 | 决策 |
| --- | --- |
| Network 默认 Implementation | Ktor Client 作为默认 Implementation，JVM/Paper 默认 OkHttp engine；其他 client 作为 Adapter。实施前复核版本。 |
| circuit breaker | 不放 core 必需路径，作为 optional policy provider；core Interface 预留。 |
| request body diagnostics | 默认不记录 body。只记录 method、redacted URL、headers safe summary、content type、size。debug 且 content type allowlist 时最多记录 4KB redacted body preview。 |
| 外部服务健康是否影响 enable | 默认不影响 plugin enable。只有 client/capability 显式 `requiredAtStartup` 时才 fail module；否则进入 degraded。 |
| Cache 默认 Implementation | CacheRuntime Interface 在 core；默认生产 Adapter 用 Caffeine；内部最小 cache 只用于 bootstrap/test。实施前复核版本。 |
| batch writer drain | 默认 3s；critical writer 可声明 10s；超时后输出 report，并按 policy fail/drop/defer。 |
| 文件型数据 snapshot store | 提供 optional `FileSnapshotStore`：atomic write、version、checksum、backup、report。它不是 DB driver，也不强制所有插件使用。 |

## Diagnostics / Integration

| 问题 | 决策 |
| --- | --- |
| diagnostics command | 作为 core 内建命令，但默认需要 admin permission，可在 config 关闭。 |
| JSON export 目录 | 单插件默认 `plugins/<PluginName>/diagnostics/`；全局导出默认 `plugins/NewNan/diagnostics/`。 |
| report ring buffer | 默认每 plugin 1024 条、每 source 256 条软上限，保留 30 分钟；超过按 ring buffer 淘汰。 |
| Prometheus/JMX Adapter | 初版不提供。 |
| capability key | 框架提供常见 capability key；插件允许定义 namespaced private key，例如 `plugin:tpa:foo`。 |
| required capability 缺失 | 默认 fail module，不 fail whole plugin，除非 plugin 明确声明 startup-critical。 |
| provider state change | 广播为框架 typed domain event，经 EventRuntime 暴露；同时保留 IntegrationRuntime state listener。 |
| 常见 Adapter | 不内建进 core。提供可插拔官方 adapter artifact，例如 Vault、LuckPerms、PlaceholderAPI。 |

## 实施前复核来源

涉及第三方库的推荐在实现前需要复核版本、维护状态和许可证。当前用于形成推荐的源头资料：

- Ktor client: https://ktor.io/docs/client-create-and-configure.html
- Ktor client engines: https://ktor.io/docs/client-engines.html
- Caffeine: https://github.com/ben-manes/caffeine
- MockBukkit: https://github.com/MockBukkit/MockBukkit
- Testcontainers for Java: https://java.testcontainers.org/
- Jackson Kotlin module: https://github.com/FasterXML/jackson-module-kotlin
- Configurate: https://github.com/SpongePowered/Configurate
- Okaeri Configs: https://github.com/OkaeriPoland/okaeri-configs
