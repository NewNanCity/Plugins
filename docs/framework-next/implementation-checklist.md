# Implementation Checklist

这份清单用于后续全新干净项目。只有当代码、样例、测试、文档都满足对应项时，才能标记完成。

## M0: Repository Skeleton

- [ ] 创建独立 Gradle build，不依赖旧 root build。
- [ ] 固定 Java 17 bytecode target。
- [ ] 固定 Kotlin stable version。
- [ ] 添加 `runtime-core`、`runtime-plugin`、`adapter-paper`、`framework-gradle-plugin`。
- [ ] 添加 `samples/best-practice-plugin`，只展示目标第一屏写法。
- [ ] 添加 `samples/dogfood-plugin`，只用于内部集成深度。

## M1: Core Runtime

- [ ] Owner tree。
- [ ] RuntimeResource binding。
- [ ] CloseReport。
- [ ] Diagnostics base。
- [ ] Source location debug switch。
- [ ] Contract tests for close order and leaks。

## M2: Plugin And Build

- [ ] `PaperPlugin({ ... })` facade。
- [ ] `Module` facade。
- [ ] Gradle DSL for plugin metadata。
- [ ] KSP descriptor collection。
- [ ] `paper-plugin.yml` generation。
- [ ] generated artifact verification。
- [ ] golden descriptor tests。

## M3: Config And Text

- [ ] `config<T>("config.yml")` declaration。
- [ ] comment-preserving YAML adapter。
- [ ] typed config injection。
- [ ] reload transaction/report。
- [ ] `messages<T>("lang/{locale}.yml")` declaration。
- [ ] typed text catalog implementation。
- [ ] MiniMessage validation。
- [ ] missing/unused key checks。

## M4: Command

- [ ] usage-shape parser。
- [ ] `sender<T>()` policy。
- [ ] required/optional/default arguments。
- [ ] options and flags。
- [ ] int/float/bool/string。
- [ ] Player/World/Material/LocationSnapshot/UUID。
- [ ] enum and alias。
- [ ] union type。
- [ ] custom converter/validator/suggestion。
- [ ] descriptor/yml generation。
- [ ] help/error/i18n rendering。

## M5: Event And Task

- [ ] module-scope `on<T>()` without manual owner。
- [ ] priority and ignoreCancelled。
- [ ] MONITOR warning/strict guard。
- [ ] ordered filters and permission helper。
- [ ] once-by-handled。
- [ ] throttle/debounce/snapshot。
- [ ] `tasks.io/cpu/main/entity/region`。
- [ ] timeout/cancel/failure outcome。
- [ ] every/delay。
- [ ] eventScope/await。
- [ ] race/join cleanup。

## M6: GUI

- [ ] `screen("name")` DSL。
- [ ] default and named session。
- [ ] page stack。
- [ ] title and rows。
- [ ] inventoryPolicy。
- [ ] button/list/inputSlot/outputSlot/storageSlot。
- [ ] stable row key。
- [ ] render diff。
- [ ] stale generation rejection。
- [ ] Paper inventory adapter safety tests。

## M7: Optional Runtimes

- [ ] CacheRuntime interface and test implementation。
- [ ] Caffeine adapter decision and implementation。
- [ ] Persistence batch writer。
- [ ] FileSnapshotStore。
- [ ] Ktor network adapter。
- [ ] redaction and request diagnostics。
- [ ] Integration capability registry。
- [ ] Vault/LuckPerms/PAPI adapter only if needed。

## M8: Diagnostics And Testing

- [ ] `/plugin diag` Paper command。
- [ ] JSON export。
- [ ] owner/task/event/gui/cache/network/capability sources。
- [ ] MockBukkit adapter tests。
- [ ] release gate task。
- [ ] Paper 1.20.1+ smoke plan。
- [ ] docs link check。
- [ ] API boundary scan: first-screen samples do not expose low-level runtime primitives。

