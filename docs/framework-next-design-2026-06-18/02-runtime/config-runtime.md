# ConfigRuntime

## 定位

ConfigRuntime 管 typed config schema、文件读写、template merge、validation、snapshot、reload report 和 cache invalidation。它不是“多格式配置工具”，而是配置事实源和 reload transaction。

业务 Module 不直接到处 parse 文件，也不手写 `clearCache -> touchWithMerge -> parse -> setupLanguageManager` 这类流程。它只读取当前 `ConfigSnapshot<T>`，并在 reload 时接收新 snapshot。

## Interface

核心概念：

- `ConfigDescriptor<T>`: 构建期/启动期声明。
- `ConfigSchema<T>`: 类型、默认值、validation、restart requirement。
- `ConfigSnapshot<T>`: 当前生效配置。
- `ConfigReloadReport`: 一次 reload 的结构化报告。
- `ConfigVersion`: cache invalidation 和 diagnostics 的版本。

```kotlin
data class ConfigSnapshot<T : Any>(
    val value: T,
    val version: ConfigVersion,
    val loadedAt: Instant,
    val source: ConfigPath,
    val report: ConfigLoadReport
)

interface ConfigRuntime {
    fun <T : Any> snapshot(key: ConfigKey<T>): ConfigSnapshot<T>
    fun reload(scope: ConfigReloadScope = ConfigReloadScope.All): ConfigReloadReport
}
```

Interface 不变量：

- snapshot 不可变。
- reload 成功前旧 snapshot 继续生效。
- validation failure 不替换 snapshot。
- reload report 必须说明每个文件的状态。
- 文件 I/O 默认不在主线程执行。
- 应用 snapshot 和触发 Bukkit 行为必须通过正确 affinity。

## Config Descriptor

```kotlin
config<TpaConfig>("config.yml") {
    template("config.yml")
    format(ConfigFormat.Yaml)
    reloadable(true)
    validate { cfg ->
        require(cfg.timeout > 0.seconds) { Keys.Config.Errors.TimeoutPositive }
    }
}
```

Descriptor 包含：

- path。
- schema type。
- template resource。
- format。
- reloadable。
- validation。
- deprecated keys。
- restart required fields。
- owner/module id。

## Reload Flow

```text
reload request
-> create reload owner
-> read files on AsyncIo
-> merge template/defaults
-> parse and validate
-> build ConfigSnapshot candidates
-> compute diff
-> apply snapshots atomically
-> invalidate CacheRuntime scopes
-> notify modules
-> emit ConfigReloadReport
```

失败策略：

- 单个 non-critical config failure 可只拒绝该 snapshot。
- critical config failure 保留旧 snapshot 并返回 failed report。
- parse/validation error 必须带 path、key、expected、actual、message key。

## Report

`ConfigReloadReport` 至少包含：

- loaded files。
- created files。
- merged defaults。
- backups created。
- unknown keys。
- deprecated keys。
- validation errors。
- restart required changes。
- unchanged files。
- duration。
- final status。

示例：

```text
config reload tpa
config.yml loaded version=42 changed=3 unknown=1 deprecated=0
status=applied duration=38ms
```

## Implementation

Implementation 隐藏：

- file path canonicalization。
- resource template copy。
- async file I/O。
- parser/serializer selection。
- schema validation。
- merge/diff。
- snapshot registry。
- rollback to old snapshot。
- report aggregation。
- cache invalidation trigger。

业务 Module 不直接依赖 YAML/Jackson/Configurate/Okaeri。

## Seam 和 Adapter

外部 Seam：

- `ConfigRuntime` Interface -> file format Adapter。
- `ConfigDescriptor` -> Plugin Spec Generation。
- `ConfigSnapshot` -> Module reload Interface。

Adapter：

- `YamlConfigAdapter`。
- `JacksonConfigAdapter`。
- `ConfigurateAdapter`。
- `OkaeriConfigAdapter`。
- `TestConfigAdapter`。

Adapter 可以处理格式细节，但不能改变 snapshot/report 语义。

## 协作关系

- Lifecycle/OwnerRuntime: reload owner 和 module reload 顺序。
- TaskRuntime: I/O 和 parse off-thread。
- CacheRuntime: config version 驱动 invalidation。
- TextRuntime: config error 用 message key 渲染。
- DiagnosticsRuntime: 保存最近 reload report。
- Plugin Spec Generation: descriptor 来源。

## Diagnostics

Config diagnostics 应回答：

- 当前 snapshot version。
- 每个 config path 的 schema type。
- 最近 load/reload duration。
- validation error。
- unknown/deprecated key。
- restart required pending changes。
- snapshot 是否来自旧版本保留。

## Harness

`ConfigHarness` 需要覆盖：

- missing file 创建默认文件。
- template merge 保留用户值。
- unknown key report。
- deprecated key report。
- validation failure 保留旧 snapshot。
- reload success 替换 version。
- cache invalidation scope。
- file I/O 不走 main affinity。
- report 可序列化。

## 开放问题

1. 初版默认 Adapter 用 Jackson/YAML 还是 Configurate/Okaeri。
2. unknown key 默认 warning 还是 strict mode error。
3. restart required field 如何声明和展示。
4. config comments 保留是否是硬要求。
