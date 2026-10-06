# Command

## 是什么

CommandRuntime 把命令的四类事实合成一个声明：

1. 玩家看到的命令形态。
2. 构建期静态 metadata。
3. 运行时解析、校验、补全和执行。
4. help/error/i18n/diagnostics。

第一屏写法：

```kotlin
command("/market give <target> <item> [amount=1] [--silent]") {
    describe(text.commandGive)
    permission("market.give")
    sender<Player>()

    arguments {
        arg("target") {
            onlinePlayer()
            describe(text.argTarget)
        }
        arg("item") {
            material()
            suggest { shops.materialSuggestions(sender) }
            describe(text.argItem)
        }
        arg("amount") {
            range(1..config.maxBuyAmount)
            describe(text.argAmount)
        }
    }

    options {
        flag("silent") {
            describe(text.optionSilent)
        }
    }

    run { ctx: CommandContext<Player>, target: Player, item: Material, amount: Int, silent: Boolean ->
        give(ctx.sender, target, item, amount, silent)
    }
}
```

## 相关工作和取舍

### Bukkit CommandExecutor

简单但 metadata、tab complete、permission、usage/help 分散。只作为底层加载事实，不作为业务接口。

### Incendo Cloud

Cloud 的 parser、suggestion、execution coordinator、annotations、exception handling 很完整，适合作为主要 adapter。但 Cloud 不是 build-time plugin metadata 的完整事实源，也不应该泄漏到业务接口。

### Aikar ACF

ACF 的价值是声明式体验：DI、validation、completion、help、conditions 放在方法声明附近。我们吸收体验，不绑定依赖。

### CommandAPI

CommandAPI 的强类型参数和 Brigadier/vanilla UI 适配有参考价值。我们吸收类型、校验、补全思路，但仍要生成 static yml。

## 设计取舍

- 命令字符串只表达玩家可见结构：required、optional、default、option flag。
- 参数类型、转换、联合类型、枚举、校验、补全放在 `arguments/options` 中。
- 不强制在字符串里写 `<target:fully.qualified.Player>`，避免第一屏臃肿。
- `sender<T>()` 使用 sender，不用 source，避免概念混乱。
- `CommandContext<TSender>` 承载 sender、label、raw input、locale、owner、diagnostics、reply helpers。

## 支持的参数

基础类型：

- int。
- float/double。
- bool。
- string。
- enum。

Minecraft 常见对象：

- Player / onlinePlayer。
- OfflinePlayer。
- World。
- LocationSnapshot。
- Material。
- Entity。
- UUID。
- NamespacedKey。

高级类型：

- `oneOf<T>` 联合类型。
- `literal("@self")`。
- custom converter。
- custom validator。
- async suggestion provider。

## 内部方案

```text
Command DSL
-> CommandDescriptor
-> build-time yml root command and permission
-> runtime adapter registration
-> parser/suggestion/help/error renderer
-> execution owner
-> handler affinity
-> diagnostics report
```

suggestion policy：

- 150ms warning。
- 300ms soft timeout。
- 500ms hard cap。
- 默认 50 条，硬上限 200 条。
- 超限截断并记录 diagnostics。

错误渲染：

- permission denied。
- player-only / console-only。
- parse failed。
- validation failed。
- suggestion timeout。
- handler timeout。
- internal error。

## 测试

- usage string parser。
- required/optional/default/option flag。
- sender policy。
- union type。
- enum alias。
- custom converter and validator。
- suggestion timeout and limit。
- descriptor -> yml golden。
- runtime adapter registration。
- localized error rendering。

