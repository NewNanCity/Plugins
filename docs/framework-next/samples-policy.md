# Samples Policy

样例是框架 API 的事实源之一。样例如果混乱，框架就会被迫向混乱妥协。

## Sample Types

| Sample | Audience | Purpose | Allowed API |
| --- | --- | --- | --- |
| `best-practice-plugin` | 普通插件开发者 | 可复制的主流写法 | first-screen public API only |
| `dogfood-plugin` | 框架维护者 | 覆盖集成深度和边界场景 | public + advanced API |
| `contract-samples/*` | 测试 | golden artifact 和 API contract | 最小必要 API |
| `adapter-samples/*` | adapter 维护者 | Paper/Cloud/Ktor/Caffeine 等 adapter glue | adapter API |

## Best-Practice Sample Rules

`best-practice-plugin` 必须：

- 展示 `PaperPlugin({ ... })`。
- 展示 typed config data class。
- 展示 typed text catalog interface。
- 展示一体化 `command("/...")`。
- 展示 module-scope `on<T>()`。
- 展示 `tasks` 的自然用法。
- 展示至少一个 `screen/page`。
- 生成 static yml、command descriptor、config schema、text catalog schema。

它禁止：

- `services.get<T>()`。
- `RuntimeServices`。
- `ConfigRuntime` / `ConfigKey`。
- `TextRuntime` / `MessageKey`。
- provider interface。
- manual descriptor writer。
- direct Paper Inventory adapter。
- direct Cloud manager。
- hand-written `plugin.yml`。
- manual `context.owner` threading in business code。

## Dogfood Sample Rules

`dogfood-plugin` 可以更重，用来验证：

- optional integrations。
- diagnostics export。
- cache/persistence/network。
- adapter edge cases。
- failure reports。
- generated artifact convergence。

但它不能变成文档第一入口。任何只在 dogfood 中证明的 first-screen capability，都视为未完成。

## Contract Sample Rules

Contract samples 应该小、稳定、可审查：

- golden `paper-plugin.yml`。
- golden command descriptor。
- golden config schema。
- golden text catalog schema。
- minimal source fixture。

Contract sample 不负责教开发者怎么写业务插件。

## README Ordering

样例目录 README 必须按这个顺序展示：

1. `best-practice-plugin`。
2. 新手 tutorial。
3. dogfood/internal samples。
4. adapter/contract samples。

不要把 minimal sample 放在第一位，除非它也是 facade-first。低层 minimal 很容易误导开发者以为那是推荐写法。

## Source Hygiene Tests

`best-practice-plugin` 必须有 source hygiene tests，检查 forbidden tokens：

```text
services.get<
RuntimeServices
ConfigRuntime
ConfigKey
TextRuntime
MessageKey
FrameworkPlugin
PluginSpec
ArtifactProvider
registerPaper
PaperGuiAdapter
context.owner
```

检查应只针对 first-screen source，不针对 runtime internals 和 advanced tests。

## Promotion Rule

一个 API 能进入 public docs，必须满足：

- 在 `best-practice-plugin` 中自然出现，或明确标为 advanced。
- 有 module-level reference doc。
- 有 contract tests。
- 有 generated artifact 或 runtime diagnostics 证明。

否则只能写在 internal/experimental 文档里。

