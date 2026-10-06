# Phase 1 Implementation Plan

## Goal

建立一个与旧框架隔离的 `framework-next/` 子树，并用最小 sample 验证下一代框架的最小闭环：OwnerRuntime、DiagnosticsRuntime、TaskRuntime、EventRuntime、PluginSpec/Command descriptor、Config/Text snapshot。

## Baseline

- Minecraft/Paper: 必须覆盖 `1.20.1+`。
- Java runtime bytecode: target `17`。
- Build toolchain: 可用 Java 21+，但产物不能依赖 Java 21 字节码。
- Kotlin: 使用稳定版本，JVM target `17`。
- Platform API: 第一阶段只使用 Paper/Bukkit public API，不使用 NMS。
- Folia: 中期目标；第一阶段只保留 Affinity 模型和测试占位，不实现 Folia adapter。

## Architecture

第一阶段采用 isolated included build 思路。新框架不改旧 `modules/*`，不继承旧 `BasePlugin/BaseModule`，不从旧框架 re-export 类型。

目标结构：

```text
framework-next/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
├── runtime-core/
├── runtime-task/
├── runtime-event/
├── runtime-command/
├── runtime-config/
├── runtime-text/
├── adapters/
│   ├── adapter-paper/
│   └── test-mockbukkit/
└── samples/
    └── minimal/
```

## Non-Goals

- 不替换旧插件。
- 不改旧 `modules/core`、`modules/config`、`modules/gui`。
- 不实现 GUI Runtime。
- 不实现 NetworkRuntime。
- 不实现 DB PersistenceRuntime。
- 不实现 Vault/LuckPerms/PlaceholderAPI/ProtocolLib Adapter。
- 不承诺 Folia adapter。

## Task Order

### Task 1: Isolated Build Skeleton

交付：

- `framework-next/settings.gradle.kts`
- `framework-next/build.gradle.kts`
- Gradle wrapper 或明确复用 repo wrapper 的策略。
- Java/Kotlin toolchain 和 JVM target `17`。
- `runtime-core` 空模块。
- `samples/minimal` 空插件模块。

验收：

- `framework-next` 自己的 `check` task 可执行。
- 不依赖旧 root build 的 Test 状态。
- `samples/minimal` 能生成 jar skeleton。

### Task 2: Runtime Core Types

交付：

- `OwnerId`
- `OwnerPath`
- `RuntimeContext`
- `RuntimeModule`
- `Policy`
- `Snapshot`
- `Report`
- `RuntimeClock`

验收：

- core contract tests 不依赖 Bukkit。
- id 全局唯一，human-readable path 可诊断。
- report 可序列化为 deterministic JSON。

### Task 3: OwnerRuntime

交付：

- owner tree。
- owner state。
- resource binding。
- close order。
- close timeout。
- close report。

验收：

- parent close 关闭 child。
- owner close 后拒绝新 resource。
- close failure 聚合到 report。
- close timeout 分层默认值生效。

### Task 4: DiagnosticsRuntime Skeleton

交付：

- `DiagnosticSource`
- `DiagnosticReport`
- report ring buffer。
- redaction pipeline。
- JSON snapshot。
- test sink。

验收：

- report 可按 owner/source/severity 查询。
- secret redaction 生效。
- ring buffer 淘汰可测。
- JSON snapshot 不包含随机时间，或使用 fake clock。

### Task 5: TaskRuntime Contract

交付：

- `Affinity`
- `TaskSpec`
- `TaskHandle`
- `TaskOutcome`
- coroutine implementation。
- fake scheduler harness。

验收：

- owner close 取消 task。
- timeout 返回 `TimedOut`。
- dependency failure 不进入后续 stage。
- main budget warning 进入 diagnostics。

### Task 6: EventRuntime Contract

交付：

- scoped subscription。
- priority、ignoreCancelled、filter。
- `handled(n)` expiry。
- event-task bridge。
- fake event bus。

验收：

- owner close 注销 listener。
- `handled(1)` 在 handler 实际执行一次后过期。
- MONITOR read-only 在 dev mode warning，在 strict/test mode fail fast。
- async event 必须通过 affinity resolver 或显式声明。

### Task 7: PluginSpec Descriptor And Golden Sample

交付：

- plugin metadata descriptor。
- command descriptor。
- permission descriptor。
- config/text descriptor placeholder。
- golden descriptor fixture。
- generated yml golden fixture。

验收：

- descriptor 不提交生产 build artifact，只提交测试 fixture。
- missing optional description key 产生 warning 和 fallback。
- duplicate command fail。
- generated yml snapshot 稳定。

### Task 8: CommandRuntime Descriptor

交付：

- 框架自己的 command Interface。
- permission policy。
- sender policy。
- suggestion timeout/result limit policy。
- fake sender harness。

验收：

- permission denied 走 TextRuntime key。
- suggestion 超时和截断进入 diagnostics。
- command handler 不直接暴露 Cloud 类型。

### Task 9: Config/Text Snapshot

交付：

- config schema/snapshot/reload report。
- text catalog/snapshot/render report。
- owner-scoped placeholder resolver。
- strict/test mode validation。

验收：

- config validation failure 保留旧 snapshot。
- unknown key 默认 warning，strict/test mode error。
- missing placeholder 返回 render failure。
- owner close 自动注销 placeholder resolver。

### Task 10: Minimal Sample Closure

交付：

- `samples/minimal` 展示一个完整流程：
  - descriptor 生成静态 yml。
  - command 触发 task。
  - task 读取 config snapshot。
  - text 渲染反馈。
  - diagnostics 导出 health JSON。

验收：

- sample 不手写 `plugin.yml` / `paper-plugin.yml`。
- owner close 后无 task/listener/resource 泄漏。
- health dump golden sample 稳定。
- 能用 MockBukkit 或 fake harness 验证核心行为。

## Stop Rules

出现以下情况暂停并回到设计文档：

- 为了跑 sample 需要继承旧 `BasePlugin/BaseModule`。
- 需要修改旧 `modules/*` 才能完成第一阶段。
- Paper 1.20.1 兼容要求与新 Interface 冲突。
- 某个 Adapter 要求第三方类型进入 core Interface。
- 测试只能靠真实 server 才能验证核心语义。

## Verification Commands

命令名以实现时实际 Gradle task 为准，目标形态如下：

```text
./gradlew -p framework-next check
./gradlew -p framework-next :runtime-core:test
./gradlew -p framework-next :runtime-task:test
./gradlew -p framework-next :runtime-event:test
./gradlew -p framework-next :samples:minimal:check
```

## Open Implementation Checks

这些不是架构阻塞，但实现前必须复核：

- Paper 1.20.1 API 和当前 Paper API 的差异。
- MockBukkit 对目标 Paper API 的覆盖程度。
- Kotlin/KSP/Gradle 版本组合。
- comment-preserving YAML adapter 的具体库选择。
- Java 17 bytecode target 下所有依赖是否可用。
