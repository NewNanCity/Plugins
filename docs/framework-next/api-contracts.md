# Public API Contracts

本页把目标第一屏 API 写成契约草案。后续实现可以调整细节，但不能违背这些开发者体验约束。

## API Tier

| Tier | 说明 | 示例 |
| --- | --- | --- |
| Public first-screen | 普通插件作者首先使用，必须自然简洁 | `PaperPlugin`, `Module`, `command`, `on`, `screen`, `config`, `messages` |
| Public advanced | 高级场景、逃生口、测试辅助 | `ConfigRuntime`, `TextRuntime`, `TaskHandle`, descriptor APIs |
| Internal | runtime/adapter/build tool 内部实现 | service registry internals、adapter bridge、descriptor writer |

任何 first-screen sample 都禁止使用 internal API。

## Plugin Declaration

```kotlin
abstract class PaperPlugin(
    declaration: PaperPluginSpec.() -> Unit
) : JavaPlugin()

class PaperPluginSpec {
    var id: String
    var name: String
    var version: String
    var apiVersion: String

    inline fun <reified T : Any> config(
        path: String,
        block: ConfigSpec<T>.() -> Unit = {}
    )

    inline fun <reified T : Any> messages(
        pathPattern: String,
        block: TextSpec<T>.() -> Unit = {}
    )

    inline fun <reified T : Module> module(
        id: String,
        block: ModuleSpec<T>.() -> Unit = {}
    )
}
```

Rules:

- Declaration must be readable at build time without starting a Minecraft server.
- Plugin metadata has one source of truth.
- Generated yml is build artifact only.

## Module

```kotlin
interface Module {
    fun build(): ModuleDeclaration
}

fun module(
    id: String,
    block: ModuleScope.() -> Unit
): ModuleDeclaration
```

`ModuleScope` exposes first-screen operations:

```kotlin
interface ModuleScope {
    val tasks: TaskFacade
    val diagnostics: DiagnosticsFacade
    val screens: ScreenRegistry

    fun command(usage: String, block: CommandSpec.() -> Unit)
    inline fun <reified E : Any> on(name: String? = null, block: EventSpec<E>.() -> Unit)
    fun every(period: Duration, block: suspend TaskScope.() -> Unit)
    fun screen(name: String, block: ScreenSpec.() -> Unit)
}
```

Rules:

- `ModuleScope` owns resources implicitly.
- No public first-screen API should require `context.owner`.
- Module constructors express business dependencies, not runtime plumbing.

## Config

```kotlin
class ConfigSpec<T : Any> {
    fun preserveComments()
    val unknownKeys: UnknownKeyPolicyBuilder
    fun validate(block: (T) -> Unit)
}
```

Runtime contract:

- Module receives typed `T`, not mutable runtime config object.
- Reload produces `ConfigReloadReport`.
- Failed reload keeps old snapshot.

## Text

```kotlin
class TextSpec<T : Any> {
    var fallbackLocale: String
    var format: TextFormat
    var legacyInput: LegacyInput
    fun placeholders(block: PlaceholderSpec.() -> Unit)
}
```

Runtime contract:

- Module receives typed catalog interface `T`.
- Catalog functions return Adventure `Component`.
- Missing/unused/invalid keys are validated.

## Command

```kotlin
class CommandSpec {
    fun describe(text: Component)
    fun permission(node: String, default: Boolean = false)
    inline fun <reified S : CommandSender> sender()
    fun arguments(block: ArgumentsSpec.() -> Unit)
    fun options(block: OptionsSpec.() -> Unit)
    fun <S : CommandSender> run(block: suspend CommandContext<S>.() -> Unit)
}
```

Typed handler overloads may support:

```kotlin
run { ctx: CommandContext<Player>, target: Player, item: Material, amount: Int, silent: Boolean -> ... }
run(::give)
```

Rules:

- Command declaration owns metadata, permission, arguments, suggestions, validation, help, and handler.
- Lower-level descriptor APIs stay advanced.

## Event

```kotlin
class EventSpec<E : Any> {
    fun priority(priority: EventPriority)
    fun ignoreCancelled(value: Boolean = true)
    fun filter(name: String? = null, predicate: E.() -> Boolean)
    fun requirePermission(permission: String)
    fun once()
    fun throttle(duration: Duration, key: E.() -> Any = { Unit })
    fun debounce(duration: Duration, key: E.() -> Any = { Unit }, block: DebounceSpec<E>.() -> Unit)
    fun run(block: suspend EventScope.(E) -> Unit)
}
```

Rules:

- Event subscriptions are owner-bound.
- Delayed handlers must use snapshots for mutable platform events.
- Async events cannot touch Bukkit mutable state without explicit task affinity.

## Task

```kotlin
interface TaskFacade {
    suspend fun <T> io(name: String, block: suspend () -> T): T
    suspend fun <T> cpu(name: String, block: suspend () -> T): T
    suspend fun <T> main(name: String, block: suspend () -> T): T
    suspend fun <T> entity(entity: Entity, name: String, block: suspend () -> T): T
    suspend fun <A, B> join(first: DeferredTask<A>, second: DeferredTask<B>): Pair<A, B>
}
```

Advanced APIs may expose `TaskHandle` and `TaskOutcome`, but first-screen code should not need to unwrap them for normal flows.

## GUI

```kotlin
class ScreenSpec {
    fun defaultSession(block: SessionSpec.() -> Unit)
    fun page(name: String, block: PageSpec.() -> Unit)
}

class PageSpec {
    var rows: Int
    fun title(block: PageContext.() -> Component)
    fun inventoryPolicy(block: InventoryPolicySpec.() -> Unit)
    fun button(slot: Int, material: Material, block: ButtonSpec.() -> Unit)
    fun <T : Any> list(slots: IntRange, data: DataSource<T>, key: (T) -> Any, block: ListItemSpec<T>.(T) -> Unit)
    fun inputSlot(name: String, slot: Int, block: InputSlotSpec.() -> Unit)
    fun outputSlot(name: String, slot: Int, block: OutputSlotSpec.() -> Unit)
}
```

Rules:

- ObjectProtection defaults to deny-by-default.
- Input/output/storage slots must be explicit.
- Paper Inventory adapter is internal.

