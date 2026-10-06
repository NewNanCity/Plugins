# New Project Bootstrap

这份文档描述后续全新干净项目如何开工。它不是旧仓库迁移计划，也不要求保留旧 `modules/*` 或 `plugins/*` 的实现细节。

## Outcome

新项目第一阶段结束时，应该具备：

- 独立 Gradle build。
- Paper 1.20.1+ / Java 17 runtime 基线。
- `runtime-core`、`runtime-plugin`、`adapter-paper`、`framework-gradle-plugin` 基础模块。
- 一个可构建的 `samples/best-practice-plugin`。
- 一套能证明 owner/lifecycle/build artifact 的 contract tests。
- 文档和代码都以 `docs/framework-next/02-architecture-and-best-practice.md` 的目标写法为事实源。

## Repository Shape

建议新项目目录：

```text
framework-next/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
├── runtime-core/
├── runtime-plugin/
├── runtime-task/
├── runtime-event/
├── runtime-command/
├── runtime-config/
├── runtime-text/
├── runtime-gui/
├── runtime-cache/
├── runtime-persistence/
├── runtime-network/
├── adapter-paper/
├── adapter-cloud/
├── adapter-config-yaml/
├── adapter-cache-caffeine/
├── adapter-network-ktor/
├── framework-gradle-plugin/
├── samples/
│   ├── best-practice-plugin/
│   └── dogfood-plugin/
└── docs/
```

第一阶段不必一次创建所有 optional adapter，但包名和边界应从一开始预留。

## Module Baseline

| Module | 第一阶段必须有 | 第一阶段不做 |
| --- | --- | --- |
| `runtime-core` | owner、resource、report、diagnostics base | Paper API |
| `runtime-plugin` | `PaperPlugin` facade、`Module` facade、module declaration model | 旧 BasePlugin adapter |
| `adapter-paper` | JavaPlugin bridge、Paper 1.20.1+ descriptor loading | Folia adapter |
| `framework-gradle-plugin` | 生成 `paper-plugin.yml` 的最小任务 | KSP 全量代码生成 |
| `samples/best-practice-plugin` | facade-first 最小插件 | 低层 runtime 教学 |

## Build Baseline

建议：

```kotlin
kotlin {
    jvmToolchain(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
```

原则：

- 构建工具可以用 Java 21。
- 发布产物必须是 Java 17 bytecode。
- 不使用 `latest` 或动态版本。
- Gradle wrapper 固定版本。
- 所有生成产物进 `build/generated`，不提交。

## First Week Plan

### Day 1: Skeleton

- 创建 Gradle multi-project。
- 加入 `runtime-core`、`runtime-plugin`、`adapter-paper`、`framework-gradle-plugin`。
- 加入最小 test setup。
- 写 `Owner`, `RuntimeResource`, `CloseReport`。

Verification:

```powershell
.\gradlew.bat check
```

### Day 2: Plugin Facade

- 实现 `PaperPlugin({ ... })` declaration model。
- 实现 `Module` declaration model。
- 让 sample 写出目标第一屏 shell。

Verification:

```powershell
.\gradlew.bat :samples:best-practice-plugin:test
```

### Day 3: Build Artifact

- 实现 Gradle DSL。
- 生成最小 `paper-plugin.yml`。
- 加入 golden descriptor test。

Verification:

```powershell
.\gradlew.bat :framework-gradle-plugin:test :samples:best-practice-plugin:jar
```

### Day 4: Config/Text Skeleton

- `config<T>()` 和 `messages<T>()` 先生成 descriptor。
- runtime injection 可以先用 fake implementation。

Verification:

```powershell
.\gradlew.bat :runtime-config:test :runtime-text:test :samples:best-practice-plugin:test
```

### Day 5: Command/Event/Task Skeleton

- 命令先支持 root command 和 handler。
- event/task 先支持 owner-bound registration 和 close cleanup。

Verification:

```powershell
.\gradlew.bat check
```

## Stop Rules

立即停下并重新设计，而不是继续补 wrapper：

- first sample 需要 `services.get<T>()` 才能写业务。
- first sample 出现 `ConfigRuntime`、`TextRuntime`、provider interface、descriptor writer。
- command metadata、handler、permission、help 需要维护两份事实。
- event/task 需要业务手动传 `owner`。
- GUI 必须直接使用 Paper Inventory adapter 才能工作。
- 为兼容旧代码引入旧命名或旧生命周期。

