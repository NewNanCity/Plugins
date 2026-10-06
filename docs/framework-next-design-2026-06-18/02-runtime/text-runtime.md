# TextRuntime

## 定位

TextRuntime 管 message key、catalog、locale、MiniMessage/Legacy/plain render、placeholder、audience adapter 和 message reload report。它把“字符串工具”提升为可审计的文本运行时。

命令反馈、GUI 标题、按钮 lore、事件提示、配置错误、diagnostics 摘要都应该走同一套 message catalog，而不是散落 raw string。

## Interface

核心概念：

- `MessageKey`: 类型安全消息键。
- `MessageCatalogSnapshot`: 当前生效目录。
- `Audience`: 玩家、命令发送者、console、broadcast target。
- `TextRenderContext`: locale、placeholder、format、audience policy。
- `TextReloadReport`: catalog reload 结果。

```kotlin
ctx.text.send(player, Keys.Tpa.RequestSent) {
    arg("target", target.name)
    localeFrom(player)
}

val component = ctx.text.render(Keys.Gui.BookTitle) {
    arg("book", book.title)
    audience(player)
}
```

Interface 不变量：

- 业务代码引用 `MessageKey`，不散落 raw key。
- catalog snapshot 不可变。
- invalid MiniMessage 不替换旧 snapshot。
- missing key、unused key、fallback usage 进入 report。
- renderer 不缓存含动态参数的最终 Component，除非 key 明确可缓存。
- player locale 是可选能力，不能污染所有调用点。

## Message Catalog

目录声明来自 Plugin Spec：

```kotlin
messages {
    defaultLocale("zh_CN")
    catalog("lang/{locale}.yml")
    fallback("zh_CN")
}
```

Catalog entry 示例：

```yaml
commands:
  tpa:
    request-sent: "<green>已向 <target> 发送传送请求"
gui:
  book-list:
    title: "<gold>书籍列表"
```

每条 entry 应有：

- key。
- format: minimessage/legacy/plain。
- placeholders。
- cache policy。
- optional audience hint。

## Render Flow

```text
MessageKey + args + audience
-> resolve locale
-> find catalog entry
-> fallback if needed
-> validate placeholders
-> render MiniMessage/Legacy/plain
-> apply audience adapter
-> return/send Component
```

错误策略：

- missing key 使用 fallback key 或 internal error message。
- invalid format 在 reload 阶段报错，运行期尽量不解析失败。
- missing placeholder 返回 render failure report，不静默空字符串。

## Implementation

Implementation 隐藏：

- catalog file load。
- locale fallback chain。
- MiniMessage parser。
- Legacy serializer。
- tag resolver registry。
- placeholder resolver registry。
- static template cache。
- audience adapter。
- reload report。

业务 Module 不直接操作 MiniMessage parser，也不直接拼接用户可见字符串。

## Seam 和 Adapter

外部 Seam：

- `TextRuntime` Interface -> Adventure/MiniMessage/Legacy Adapter。
- `MessageCatalogDescriptor` -> Plugin Spec Generation。
- `MessageCatalogSnapshot` -> Config/Text reload flow。

Adapter：

- `AdventureComponentAdapter`。
- `MiniMessageAdapter`。
- `LegacyTextAdapter`。
- `BukkitAudienceAdapter`。
- `TestTextAdapter`。

## 协作关系

- CommandRuntime: command description、error、help、reply。
- GuiRuntime: title、button、lore、loading/empty/error state。
- ConfigRuntime: config error and report rendering。
- EventRuntime: timeout/cancel/user feedback。
- DiagnosticsRuntime: admin-facing diagnostics formatting。
- CacheRuntime: template cache。

## Diagnostics

Text diagnostics 应包含：

- loaded locales。
- default/fallback locale。
- key count。
- missing keys。
- unused keys。
- invalid format entries。
- fallback usage count。
- render failures。
- slow render entries。

## Harness

`TextHarness` 需要覆盖：

- catalog load。
- missing key report。
- invalid MiniMessage report。
- fallback chain。
- placeholder validation。
- per-player locale。
- command/gui/event message rendering。
- reload failure keeps old catalog。
- static template cache invalidates on catalog version。

## 开放问题

1. MessageKey 是手写 object、KSP 生成，还是 runtime 校验。
2. catalog unused key 是 warning 还是 strict mode error。
3. 默认是否允许 Legacy 格式自动识别。
4. placeholder resolver 是否支持 owner scope 注册。
