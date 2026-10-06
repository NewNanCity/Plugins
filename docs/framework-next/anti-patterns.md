# Anti-Patterns

本页记录新框架实现和样例中应避免的方向。出现这些信号时，应优先重塑模型，而不是继续加 wrapper。

## First-Screen API 反模式

### 让开发者先拿 service

反模式：

```kotlin
val config = context.services.get<ConfigRuntime>()
```

问题：

- 业务代码需要知道 runtime registry。
- 依赖关系不可读。
- 测试和 diagnostics 难以绑定 owner。

正确方向：

```kotlin
class MarketModule(
    private val config: MarketConfig,
    private val text: MarketText
) : Module
```

### 把低层 runtime 当成第一案例

反模式：

```kotlin
val key = ConfigKey<MarketConfig>("market")
val snapshot = configRuntime.snapshot(key)
```

正确方向：

```kotlin
config<MarketConfig>("config.yml")
```

低层 runtime 可以存在，但只放 advanced docs。

### 多事实源 command

反模式：

```kotlin
pluginSpec { command("market") }
commandDescriptors { command("/market give ...") }
commands.register(binding)
```

问题：

- permission、help、usage、handler 容易漂移。
- build-time 和 runtime 事实不一致。

正确方向：

```kotlin
command("/market give <target> <item> [amount=1] [--silent]") {
    permission("market.give")
    arguments { ... }
    run(::give)
}
```

### 业务手动传 owner

反模式：

```kotlin
events.on<PlayerJoinEvent>("welcome", context.owner) { ... }
tasks.io("load", context.owner) { ... }
```

正确方向：

```kotlin
on<PlayerJoinEvent>("welcome") { ... }
tasks.io("load") { ... }
```

owner 由 module/command/event/page scope 隐式绑定。

## Runtime 反模式

### 用 adapter 掩盖错误模型

如果 facade 只是把分裂事实源包起来，例如 command 仍有三处声明，只是换了好看的函数名，那不是改善，是延迟清算。

判断标准：

- 是否仍要维护多份 metadata。
- 是否仍要手动同步 descriptor 和 runtime。
- 是否 first sample 仍要理解 internal 类型。

### 全局 manager 管所有 GUI

反模式：

```kotlin
GuiManager.current[player] = page
```

问题：

- page/session owner 不清晰。
- 玩家退出、reload、close 顺序难处理。
- 异步数据旧结果可能覆盖新页面。

正确方向：

- screen registry。
- player session owner。
- page owner。
- generation id。
- render diff。

### 裸 CoroutineScope

反模式：

```kotlin
private val scope = CoroutineScope(Dispatchers.IO)
```

问题：

- plugin disable 后任务可能继续跑。
- 异常和取消不进入 diagnostics。
- Bukkit affinity 不受控。

正确方向：

```kotlin
tasks.io("load-profile") { ... }
```

### 长期强引用 Bukkit 对象

避免长期缓存：

- `Player`。
- `Entity`。
- `Inventory`。
- 可变 `ItemStack`。
- Bukkit `Location` 如果跨线程或跨 reload 使用。

优先保存：

- UUID。
- NamespacedKey。
- LocationSnapshot。
- immutable DTO。

## Build 反模式

### 提交生成物

不要提交：

- generated `paper-plugin.yml`。
- generated command descriptors。
- generated config schema。
- generated text catalog schema。

例外：

- golden fixture 用于测试，路径必须明确是 `src/test/resources/golden`。

### 运行时生成 plugin metadata

Paper 读取 plugin metadata 发生在插件启动前。运行时生成 yml 没有意义。必须 build-time 生成。

## Documentation 反模式

### minimal sample 放第一

如果 minimal sample 展示的是底层 runtime，那么它不应该排在第一。第一入口必须是 best-practice。

### 文档宣称未实现能力

可以写目标 API，但必须标明是 target/design。当前可用能力要由样例和测试证明。

### 用实现现状倒推设计

旧代码和实验代码只能作为素材。目标最佳实践和 API contract 才能决定新项目实现方向。

