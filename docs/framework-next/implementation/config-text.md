# Config And Text

## 是什么

Config 和 Text 是插件对管理员和玩家暴露的长期接口。

Config 负责 typed data class、默认文件、注释、校验、reload 和报告。Text 负责 typed message catalog、locale、MiniMessage/Legacy/plain、placeholder 和渲染。

第一屏 API 必须是：

```kotlin
config<MarketConfig>("config.yml") {
    preserveComments()
    unknownKeys.warn()
}

messages<MarketText>("lang/{locale}.yml") {
    fallbackLocale = "zh_CN"
    format = TextFormat.MiniMessage
    legacyInput = LegacyInput.Disabled
}
```

## 相关工作和取舍

### Jackson

Jackson 对 Kotlin data class、JSON/YAML 转换、现有代码复用很强，但不天然解决管理员注释保留和配置升级体验。

### Configurate

Configurate 的 node model 和 transformation 适合复杂配置树和迁移，但 node API 对普通业务代码偏底层。

### Okaeri Configs

Okaeri 的 typed class、durable comments、小体积很适合 Minecraft 插件配置。缺点是 reload transaction、snapshot/report 仍需要框架托管。

结论：核心 API 是 typed config snapshot 和 reload report；默认生产 adapter 优先 comment-preserving YAML。Jackson/Configurate/Okaeri 都只能影响 adapter。

### Adventure / MiniMessage

MiniMessage 适合作为可编辑文本格式，但业务不应保存 MiniMessage 字符串作为状态。业务应调用 typed text interface。

## 开发者用法

```kotlin
data class MarketConfig(
    val maxBuyAmount: Int = 64,
    val welcomeEnabled: Boolean = true,
    @RestartRequired val storage: StorageConfig = StorageConfig()
)

interface MarketText {
    fun welcome(player: String): Component
    fun notEnoughMoney(balance: Money, price: Money): Component
    val commandGive: Component
    val argAmount: Component
}

class MarketModule(
    private val config: MarketConfig,
    private val text: MarketText
) : Module
```

模块拿到的是当前生效 snapshot 的 typed view。reload 时框架创建新 snapshot，通过 module reload 事务替换。

## 内部方案

Config pipeline：

```text
Config declaration
-> schema generation
-> default file generation/merge
-> async read
-> parse to immutable data class
-> validate
-> restart-required diff
-> ConfigSnapshot
-> ConfigReloadReport
```

Text pipeline：

```text
Text interface
-> KSP catalog schema
-> lang file validation
-> MiniMessage/Legacy/plain parser
-> placeholder resolver registry
-> TextCatalogSnapshot
-> generated or proxy implementation
```

默认策略：

- unknown config key: warning，strict/test mode error。
- unused catalog key: warning，strict/test mode error。
- missing catalog key: error unless fallback covers it。
- Legacy input: 默认 disabled，显式 compatibility mode 才允许。
- placeholder resolver: owner scoped，owner close 自动注销。
- comments: 默认保留。

## 高级逃生口

高级 API 可以存在：

- `ConfigRuntime`。
- `ConfigSnapshot<T>`。
- `ConfigSchemaDescriptor`。
- `MessageKey`。
- `MessageTemplate<T>`。
- `TextRuntime`。

但它们不进入 first tutorial 或 best-practice sample。

## 测试

- data class default generation。
- comment-preserving merge。
- unknown key report。
- validation failure 保留旧 snapshot。
- restart required diff。
- missing/unused text key。
- invalid MiniMessage。
- placeholder resolver owner cleanup。
- generated schema/catalog golden tests。

