# Plugin And Build-Time Generation

## 是什么

Plugin/build 模块负责把插件的静态事实从代码声明转成 Paper 能读取的构建产物：

- `plugin.yml` / `paper-plugin.yml`。
- commands。
- permissions。
- dependencies / soft dependencies。
- config schema。
- text catalog schema。
- descriptor validation reports。

开发者只维护代码声明，生成物不提交仓库。

## 相关工作和取舍

### Paper plugin.yml

Paper 在加载插件前读取静态 metadata，所以运行时动态生成 plugin metadata 不可靠。结论：plugin metadata 必须 build-time 生成。

### Cloud / ACF / CommandAPI

这些框架都能表达命令运行时能力，但都不是完整 build-time plugin metadata 事实源。结论：命令运行时 adapter 可以用 Cloud，公共事实源必须是框架自己的 descriptor。

### KSP / annotation processor / Gradle source collector

- KSP 适合收集 Kotlin 源码声明和生成 typed accessors。
- Gradle DSL 适合静态插件 metadata，例如 id、main、version、apiVersion、platform。
- source collector 可作为 fallback，但不应是主路径。

推荐：Gradle Kotlin DSL 提供插件静态 metadata，KSP 收集 command/config/text/module 声明，Gradle task 合并 descriptor 并生成 yml。

## 开发者用法

`build.gradle.kts`:

```kotlin
plugins {
    id("city.newnan.framework.paper")
}

newnanPaperPlugin {
    id = "market"
    name = "Market"
    mainClass = "city.newnan.market.MarketPlugin"
    apiVersion = "1.20.1"
    targetPlatform = Paper
}
```

插件代码：

```kotlin
class MarketPlugin : PaperPlugin({
    id = "market"
    name = "Market"
    version = "1.0.0"
    apiVersion = "1.20.1+"

    config<MarketConfig>("config.yml")
    messages<MarketText>("lang/{locale}.yml")
    module<MarketModule>("market")
})
```

## 内部方案

构建期 pipeline：

```text
Gradle DSL
  -> PluginMetadataDescriptor

KSP source processing
  -> CommandDescriptorSet
  -> ConfigSchemaSet
  -> TextCatalogSchemaSet
  -> ModuleDescriptorSet

FrameworkSpecCompiler
  -> merged FrameworkDescriptor
  -> paper-plugin.yml
  -> command-descriptors.json
  -> config-schema.json
  -> text-catalog-schema.json
  -> validation report
```

校验规则：

- Gradle DSL 和源码声明的 plugin id/name/main/apiVersion 必须一致。
- command root 必须能进入 static yml。
- permission description 缺失默认 warning，strict mode error。
- command description 缺失默认 warning，strict mode error。
- generated descriptor 不能提交，只能在 `build/generated` 和 jar 中存在。
- golden sample descriptor 作为测试 fixture 提交。

## 测试

- 纯 descriptor merge tests。
- golden `paper-plugin.yml` tests。
- stale generated file check。
- jar entry verification。
- Gradle plugin functional tests。
- Paper 1.20.1+ smoke: 插件能被 Paper 读取 descriptor 并启用。

