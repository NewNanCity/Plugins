# Test Strategy

## 定位

Test Strategy 定义下一代框架的测试入口、fake runtime、contract sample、adapter verification 和回归矩阵。目标是让每个 Runtime 在没有真实 Minecraft server 的情况下先能 deterministic 测，再用少量集成样例验证 Bukkit/Paper 绑定。

当前仓库已有 core/config/i18n/gui/network 的单元测试和部分 README 示例，但 root 测试入口存在历史缺口。后续开发不应等到所有代码写完再统一补测试，而是每个 Runtime 先定义 harness，再实现 Interface。

## 测试分层

1. Interface contract tests。
2. Runtime implementation tests。
3. Adapter tests。
4. Cross-runtime flow tests。
5. Plugin sample tests。
6. Build artifact verification。

每一层的职责：

- Interface contract tests: 不依赖 Bukkit，断言语义和不变量。
- Runtime implementation tests: 使用 fake clock/fake scheduler/fake owner。
- Adapter tests: 验证 Paper/Bukkit/Ktor/Hikari 等绑定行为。
- Cross-runtime flow tests: event -> task -> cache -> gui/text 的组合。
- Plugin sample tests: 小插件样例证明声明式体验。
- Build artifact verification: descriptor、plugin.yml、paper-plugin.yml、jar 内元数据。

## Shared Harness

建议先建设共享 harness：

```kotlin
class FrameworkHarness {
    val clock: FakeClock
    val owners: OwnerHarness
    val tasks: TaskHarness
    val events: EventHarness
    val diagnostics: DiagnosticsHarness
}
```

Harness 必须提供：

- fake clock。
- deterministic tick。
- fake owner tree。
- fake scheduler。
- fake event bus。
- fake sender/player/audience。
- fake config files。
- fake network responses。
- fake persistence store。
- diagnostics snapshot。

## Runtime Matrix

| Runtime | Contract tests | Adapter tests | Cross-runtime tests |
| --- | --- | --- | --- |
| Lifecycle/Owner | close order、resource graph、failure aggregation | Bukkit plugin lifecycle | close cancels task/listener/gui |
| Task | affinity、timeout、cancel、backpressure | Paper/Folia scheduler mapping | event -> async -> main/entity |
| Event | scope、priority、filters、expiry | Bukkit listener registration | event task bridge |
| Command | descriptor、permission、help、sender policy | Cloud/Paper command registration | command -> task -> text |
| GUI | session、page、render diff、event routing | Inventory event binding | gui -> task -> cache -> render |
| Plugin Spec | descriptor validation、static yml generation | Gradle/KSP/task wiring | command/config/text descriptor consistency |
| Config | snapshot、reload report、validation | YAML/Jackson/Configurate | config version invalidates cache |
| Text | key、fallback、placeholder、render | Adventure/MiniMessage | command/gui/event messages |
| Network | timeout、retry、rate limit、redaction | Ktor/OkHttp | request -> cache -> diagnostics |
| Persistence/Cache | transaction、flush、ttl、single-flight | Hikari/Caffeine | event enqueue write, close drain |
| Integration | capability state、provider health | Vault/Placeholder/etc. | provider state event -> feature disable |
| Diagnostics | snapshot、report、redaction、sink | command/json/log sink | full health dump |

## Golden Samples

为防止声明式设计漂移，应保留 golden samples：

- `plugin-spec.yml`: DSL 生成的中间 descriptor。
- `paper-plugin.yml`: 静态元数据输出。
- `command-help.txt`: help 渲染摘要。
- `config-reload-report.json`: reload 报告。
- `text-catalog-report.json`: message catalog 报告。
- `diagnostics-health.json`: health dump。

Golden sample 的要求：

- 字段顺序稳定。
- 不包含时间戳或随机 id，或使用 fake clock。
- 不包含本机路径。
- 不包含 secret。

## Build Verification

构建期必须验证：

- descriptor 可以生成静态 yml。
- yml 和 descriptor 一致。
- root command 都有 description 和 permission。
- permission 都有默认值。
- config schema 和 message catalog 能被发现。
- jar 内包含期望元数据。

示例验收命令可以先设计为：

```text
./gradlew :framework:pluginSpecCheck
./gradlew :framework:runtimeContractTest
./gradlew :samples:tpa-next:check
```

命令名只是目标形态，具体 task 名称以后以实现为准。

## Failure Fixtures

每个 Runtime 都应有失败夹具：

- owner close 中途某 resource close failure。
- task timeout。
- event handler throw。
- command permission denied。
- GUI render failure。
- config validation failure。
- text missing placeholder。
- network timeout。
- persistence conflict。
- cache loader failure。
- integration provider missing。
- diagnostics sink failure。

失败夹具必须证明：

- 错误结构化。
- owner/resource 不泄漏。
- report 可被 diagnostics 看到。
- 用户反馈可由 TextRuntime 渲染。
- 后续操作不会进入未知状态。

## Stop Rules

开发某个 Runtime 时出现以下情况应暂停该 Runtime 的实现讨论：

- 需要改公共 Interface 且影响多个 Runtime。
- fake harness 无法表达该 Runtime 的关键语义。
- Adapter 迫使业务 Interface 泄漏第三方类型。
- 测试只能依赖真实 server 才能证明基本语义。
- 为通过测试而把错误吞成日志。

## 开放问题

1. root 测试入口历史禁用原因需要单独确认。
2. 是否引入 MockBukkit 或 Testcontainers。
3. Folia scheduler adapter 的自动化测试边界如何建立。
4. golden sample 是否提交到仓库作为稳定契约。
