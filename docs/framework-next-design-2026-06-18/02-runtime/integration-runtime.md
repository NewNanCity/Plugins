# IntegrationRuntime

## 定位

IntegrationRuntime 管第三方插件、服务、库和外部能力的 typed capability。它把 Vault、ProtocolLib、WorldEdit、PlaceholderAPI、LuckPerms、数据库、外部 HTTP 服务等依赖统一描述成可发现、可降级、可诊断的 provider。

Minecraft 插件生态里依赖经常是 soft depend、运行期启停、版本差异和功能缺失。业务 Module 不应该到处 `getPlugin("Vault")`、保存 nullable service，再在每个调用点猜可用性。

## Interface

核心概念：

- `CapabilityKey<T>`: 类型安全能力标识。
- `CapabilityProvider<T>`: provider 注册、探测、创建和关闭。
- `CapabilityHandle<T>`: 当前能力引用和状态。
- `CapabilityState`: available、unavailable、degraded、failed。
- `ProviderHealth`: provider 版本、依赖、错误、最近检查。
- `IntegrationDescriptor`: 构建期/启动期声明的 depend/softDepend。

```kotlin
val economy = ctx.integrations.require(EconomyCapability) {
    owner(ctx.owner)
    failureMessage(Keys.Errors.EconomyUnavailable)
}

ctx.integrations.optional(PlaceholderCapability).ifAvailable { placeholders ->
    placeholders.register("server_balance") { player -> economy.balance(player).toString() }
}
```

Interface 不变量：

- capability 必须有 typed key。
- require/optional 语义必须显式。
- provider 状态变化必须进入 diagnostics。
- provider unavailable 时不能返回裸 null。
- capability handle 不允许跨 owner 长期保存而不注册引用。
- 外部插件对象只能通过 Adapter 暴露受控 Interface。

## Capability State

```kotlin
sealed interface CapabilityState {
    data object Available : CapabilityState
    data class Unavailable(val reason: UnavailableReason) : CapabilityState
    data class Degraded(val reason: String) : CapabilityState
    data class Failed(val error: Throwable) : CapabilityState
}
```

状态来源：

- plugin present/not present。
- service registered/not registered。
- version supported/unsupported。
- permission or config disabled。
- provider health check failed。
- runtime exception threshold exceeded。

## Provider Flow

```text
descriptor declared
-> discover external plugin/service
-> validate version and feature set
-> create Adapter
-> publish CapabilityHandle
-> monitor health
-> update state on provider changes
-> close handle with owner
```

软依赖行为：

- optional capability 缺失时，业务可继续运行但功能关闭。
- required capability 缺失时，Module 初始化失败或进入 degraded，由声明决定。
- state change 要通知订阅者，避免业务保存过期 provider。

## Integration Descriptor

Descriptor 与 Plugin Spec Generation 协作：

```kotlin
integrations {
    softDepend("Vault") {
        capability(EconomyCapability)
        minVersion("1.7")
    }

    optional("PlaceholderAPI") {
        capability(PlaceholderCapability)
        enabledByConfig(Keys.Config.Placeholder.Enabled)
    }
}
```

Descriptor 输出：

- static depend/softDepend。
- capability key。
- provider plugin name。
- version requirement。
- startup requirement。
- degraded behavior。

## Implementation

Implementation 隐藏：

- Bukkit PluginManager 查询。
- ServicesManager 查询。
- provider version parsing。
- Adapter creation。
- capability registry。
- state transition。
- provider event listener。
- health check。
- dependent owner notification。

业务 Module 不直接依赖 Vault/ProtocolLib/WorldEdit 类型，除非该 Module 本身就是对应 integration Adapter。

## Seam 和 Adapter

外部 Seam：

- `IntegrationRuntime` Interface -> Bukkit service/plugin discovery。
- `CapabilityKey<T>` -> typed provider Interface。
- `CapabilityProvider<T>` -> third-party plugin Adapter。
- `IntegrationDescriptor` -> Plugin Spec Generation。

Adapter 示例：

- `VaultEconomyAdapter`。
- `LuckPermsPermissionAdapter`。
- `PlaceholderApiAdapter`。
- `ProtocolLibPacketAdapter`。
- `WorldEditRegionAdapter`。
- `TestCapabilityAdapter`。

Adapter 要求：

- 只暴露框架定义的最小 Interface。
- 捕获第三方异常并映射成 capability report。
- version/feature 不满足时进入 unavailable 或 degraded。
- provider close/unregister 后 handle 不再返回 stale provider。

## 协作关系

- Plugin Spec Generation: depend/softDepend 静态元数据。
- Lifecycle/OwnerRuntime: capability handle 归属 owner。
- ConfigRuntime: integration enable/disable 和 policy。
- EventRuntime: provider state change event。
- CommandRuntime: admin health 和功能开关反馈。
- DiagnosticsRuntime: provider health 和 state transition。
- TaskRuntime: provider health check 调度。

## Diagnostics

Integration diagnostics 应包含：

- capability key。
- provider name。
- provider version。
- required/optional。
- state。
- owner consumers。
- last health check。
- last error summary。
- state transition history。

示例：

```text
capability economy provider=Vault state=degraded required=false
reason=provider present but no economy service registered
consumers=plugin:shop/module:price, plugin:tpa/module:cost
```

## Harness

`IntegrationHarness` 覆盖：

- required capability available 时 Module 初始化通过。
- required capability missing 时返回声明的失败模式。
- optional capability missing 时功能关闭但不失败。
- provider version unsupported 进入 unavailable。
- provider runtime error threshold 后进入 degraded。
- state change 通知订阅者。
- owner close 释放 capability reference。
- diagnostics 输出 provider health。

## 开放问题

1. capability key 是否由框架集中声明，还是允许插件定义私有 key。
2. required capability 缺失时默认 fail module 还是 fail plugin。
3. provider state change 是否需要广播成 EventRuntime 事件。
4. 是否内建 Vault/LuckPerms/PlaceholderAPI 等常见 Adapter。
