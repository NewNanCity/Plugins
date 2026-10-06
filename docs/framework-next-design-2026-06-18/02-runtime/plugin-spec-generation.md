# Plugin Spec Generation

## 定位

Plugin Spec Generation 是构建期 Module。它把插件元数据、root command、permission、dependency、config schema、message catalog 等代码声明收集成 descriptor，并生成静态 `plugin.yml` / `paper-plugin.yml`。

Paper/Bukkit 在加载插件前读取静态 yml，因此这件事不能放到运行期。运行期只校验打包产物和 descriptor 是否一致。

## Interface

调用者需要理解的核心概念：

- `PluginSpec`: 插件静态元数据的事实源。
- `CommandDescriptor`: root command、description、permission、aliases。
- `PermissionDescriptor`: permission node、default、description key。
- `ConfigDescriptor`: config path、schema、template、reload policy。
- `MessageCatalogDescriptor`: locale、path pattern、fallback。
- `GeneratedPluginMetadata`: 构建产物。

```kotlin
pluginSpec {
    id("tpa")
    name("TPA")
    mainClass("city.newnan.tpa.TPAPlugin")
    apiVersion("1.21")

    command("tpa") {
        aliases("tpaccept", "tpdeny")
        permission("newnan.tpa.use")
        descriptionKey(Keys.Command.Tpa.Description)
    }

    permission("newnan.tpa.admin") {
        default(PermissionDefault.OP)
        descriptionKey(Keys.Permission.Admin)
    }

    config<TpaConfig>("config.yml") {
        template("config.yml")
        reloadable(true)
    }

    messages {
        defaultLocale("zh_CN")
        catalog("lang/{locale}.yml")
    }

    softDepend("Vault")
}
```

Interface 不变量：

- `id/name/mainClass/apiVersion` 必须存在。
- root command 必须能映射到静态 yml。
- permission node 必须有默认值。
- command permission 必须存在于 permission catalog，或显式允许外部 permission。
- config path 不能重复。
- descriptor 生成失败必须让构建失败。
- 打包产物必须可校验。

## Descriptor

建议中间产物使用稳定、可 diff 的描述文件：

```yaml
plugin:
  id: tpa
  name: TPA
  main: city.newnan.tpa.TPAPlugin
  apiVersion: "1.21"
commands:
  tpa:
    aliases: [tpaccept, tpdeny]
    descriptionKey: commands.tpa.description
    permission: newnan.tpa.use
permissions:
  newnan.tpa.use:
    default: true
    descriptionKey: permissions.tpa.use
  newnan.tpa.admin:
    default: op
    descriptionKey: permissions.tpa.admin
configs:
  config.yml:
    schema: city.newnan.tpa.TpaConfig
    reloadable: true
messages:
  defaultLocale: zh_CN
  pattern: lang/{locale}.yml
```

descriptor 是 build-time 和 runtime 的共同契约：

- build-time 生成 yml。
- runtime 校验 descriptor 与 jar 内 yml。
- diagnostics 展示插件声明。
- tests 比对 descriptor golden file。

## Build Flow

```text
source declarations
-> collect descriptors
-> validate cross references
-> write descriptor file
-> generate paper-plugin.yml / plugin.yml
-> package into jar
-> verify jar metadata
```

收集方式可选：

- KSP。
- annotation processor。
- Gradle source-set task。
- 显式 Kotlin DSL 输出 descriptor。

当前设计不强制选型，但要求输出 descriptor 一致。

## Implementation

Implementation 隐藏：

- source scanning / symbol processing。
- descriptor merge。
- duplicate command/permission detection。
- yml serialization。
- generated file path 管理。
- jar inspection。
- Gradle task wiring。
- error reporting。

业务插件只维护代码声明，不直接手写静态 yml。

## Seam 和 Adapter

外部 Seam：

- `PluginSpec` Interface -> Gradle/KSP/annotation processor Implementation。
- `CommandDescriptor` -> CommandRuntime。
- `ConfigDescriptor` -> ConfigRuntime。
- `MessageCatalogDescriptor` -> TextRuntime。
- `GeneratedPluginMetadata` -> Paper/Bukkit loader。

Adapter：

- `PaperPluginYmlAdapter`: 生成 `paper-plugin.yml`。
- `BukkitPluginYmlAdapter`: 生成 `plugin.yml`。
- `CloudCommandDescriptorAdapter`: 从 Cloud annotation 提取 command descriptor。
- `FrameworkDslDescriptorAdapter`: 从显式 DSL 生成 descriptor。
- `JarMetadataVerifier`: 打包后校验产物。

## 协作关系

- CommandRuntime: 提供 command descriptor 和 permission requirement。
- ConfigRuntime: 提供 config schema descriptor。
- TextRuntime: 校验 description/message key 是否存在。
- IntegrationRuntime: depend/softDepend 来源。
- DiagnosticsRuntime: runtime 展示 descriptor 和 yml 校验结果。
- TestStrategy: golden descriptor 和 jar verification。

## Diagnostics

构建期 report 应包含：

- generated files。
- root command 数量。
- permission 数量。
- depend/softDepend 列表。
- config schema 列表。
- message catalog 列表。
- descriptor source references。
- warning/error 列表。

典型错误：

- duplicate root command。
- command permission 未声明。
- description key 缺失。
- unknown soft dependency。
- generated yml 与 descriptor 不一致。

## Harness

`PluginSpecHarness` 需要覆盖：

- DSL 生成 descriptor。
- Cloud annotation 生成 descriptor。
- duplicate command fail。
- missing permission fail。
- generated yml snapshot。
- jar metadata verification。
- descriptor 和 yml roundtrip。

## 开放问题

1. 初版优先使用显式 Kotlin DSL，还是直接从 Cloud annotations 收集。
2. 同时生成 `plugin.yml` 和 `paper-plugin.yml`，还是按目标平台生成一个。
3. description key 缺失是 warning 还是 error。
4. generated descriptor 是否提交到仓库，还是只作为 build artifact。
