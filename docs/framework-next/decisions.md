# Decisions

本页记录当前设计包默认采用的决策。后续新项目从这些决策开始，不再把旧实现作为默认约束。

## 顶层

| 问题 | 决策 |
| --- | --- |
| 是否兼容旧 `BasePlugin` / `BaseModule` | 不兼容。新框架干净重写。 |
| 是否保留旧 `bind(Terminable)` | 不保留。统一进入 OwnerRuntime。 |
| 目标平台 | Paper 1.20.1+。 |
| Folia | 中期目标。近期只设计 affinity，不承诺 adapter。 |
| Java runtime | Java 17 字节码。 |
| NMS | 第一阶段不依赖。 |
| 样例 | 只有 `best-practice-plugin` 是普通开发者复制目标。dogfood/internal samples 不作为第一入口。 |

## Build

| 问题 | 决策 |
| --- | --- |
| plugin.yml | 由构建期生成，不提交。 |
| descriptor 收集 | Gradle DSL + KSP 为主。 |
| generated descriptor | build artifact，不提交。 |
| golden sample | 提交为测试契约。 |
| permission description | 可选，缺失 warning，strict/test mode error。 |
| command description | 可选，缺失 warning，strict/test mode error。 |

## Runtime

| 问题 | 决策 |
| --- | --- |
| owner id | 全局唯一、时间有序 128-bit id，同时保留 human-readable path。 |
| close timeout | plugin 10s、module 5s、session/page 2s、request/writer 3s。 |
| optional module dependency | 允许。required 缺失 fail module，optional 缺失 degraded。 |
| source location diagnostics | 默认关闭，debug/strict/test mode 开启。 |
| executor pool | AsyncCpu = max(1, cores - 1)，AsyncIo = min(64, max(4, cores * 4))，允许配置覆盖。 |
| coroutine | 作为内部默认 implementation，不直接让业务保存裸 scope。 |

## Event / GUI

| 问题 | 决策 |
| --- | --- |
| event expiry | 公共 API 默认 handled count。seen/matched 留给 diagnostics。 |
| MONITOR | 开发 warning，strict/test fail fast。 |
| async event | 通过 EventAffinityResolver；未知 async event 必须显式声明。 |
| DomainEventBus | 默认同步派发；异步通过 TaskRuntime。 |
| ProtocolLib | Integration adapter 转接，不让 EventRuntime 直接依赖。 |
| Page identity | framework registry identity 是事实源。 |
| named session | 允许，但默认只有 default；显式 named session 必须声明 TTL、owner、最大 stack 深度、关闭策略。 |
| ObjectProtection | deny-by-default。 |

## Config / Text

| 问题 | 决策 |
| --- | --- |
| 默认 config adapter | comment-preserving YAML adapter 优先。 |
| Jackson | 保留为 typed conversion/JSON/复用 adapter，不是第一公共接口。 |
| unknown key | warning，strict/test error。 |
| restart required | schema DSL 或注解声明，reload report 展示。 |
| comments | 必须保留。 |
| MessageKey | KSP 生成/校验 typed catalog，runtime 校验兜底。 |
| Legacy | 默认不自动猜；显式 compatibility mode。 |

## Optional Runtime

| 问题 | 决策 |
| --- | --- |
| Persistence | optional，不进 core；DB driver 由插件引入。 |
| Cache | core 定义 interface，生产 adapter 可用 Caffeine。 |
| Network | Ktor + OkHttp engine 作为默认 adapter 候选，实施前复核版本。 |
| circuit breaker | optional policy provider，不进 core 必需路径。 |
| external service health | 默认不影响 plugin enable；requiredAtStartup 才 fail module。 |
| metrics adapter | Prometheus/JMX 初版不做。 |
| common integrations | 不内建 core，提供可插拔官方 adapter artifact。 |

