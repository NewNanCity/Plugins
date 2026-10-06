# Architecture And Best Practice

## 总体架构

新框架分成两个 seam：

```text
Build time
└── FrameworkSpecCompiler
    ├── plugin metadata
    ├── command descriptors
    ├── permission descriptors
    ├── config schema
    ├── text catalog schema
    └── plugin.yml / paper-plugin.yml

Runtime
└── PluginRuntime
    ├── LifecycleRuntime
    ├── OwnerRuntime
    ├── ModuleRuntime
    ├── TaskRuntime
    ├── EventRuntime
    ├── CommandRuntime
    ├── ConfigRuntime
    ├── TextRuntime
    ├── GuiRuntime
    ├── CacheRuntime
    ├── PersistenceRuntime
    ├── NetworkRuntime
    ├── IntegrationRuntime
    └── DiagnosticsRuntime
```

构建期负责收集代码声明并生成静态产物。运行期负责 owner、scope、affinity、policy、snapshot、report 和 diagnostics。

## 核心概念

| 概念 | 含义 |
| --- | --- |
| Owner | 所有长期资源的归属。module、task、event、GUI session、request、cache、writer 都必须属于一个 owner。 |
| Scope | 一段行为的生命周期边界，例如 module scope、player scope、command scope、event scope、page scope。 |
| Affinity | 代码应在哪个 Minecraft 执行域运行：Main、AsyncIo、AsyncCpu、Region、Entity。 |
| Policy | timeout、retry、rate limit、cache ttl、batch flush、main-thread budget、event backpressure。 |
| Snapshot | 某个时刻的生效事实，例如 config snapshot、message catalog snapshot、capability snapshot。 |
| Report | 一次操作的结构化结果，例如 reload report、close report、task outcome、generation report。 |
| Diagnostics | 面向开发者和服主的可观察输出。 |

## 目标最佳实践代码

这段代码是目标 API 草案，不代表当前旧仓库已经实现。它是后续新项目设计和测试的第一事实源。

```kotlin
class MarketPlugin : PaperPlugin({
    id = "market"
    name = "Market"
    version = "1.0.0"
    apiVersion = "1.20.1+"

    config<MarketConfig>("config.yml") {
        preserveComments()
        unknownKeys.warn()
    }

    messages<MarketText>("lang/{locale}.yml") {
        fallbackLocale = "zh_CN"
        format = TextFormat.MiniMessage
        legacyInput = LegacyInput.Disabled
    }

    module<MarketModule>("market")
})

data class MarketConfig(
    val maxBuyAmount: Int = 64,
    val sessionTtlMinutes: Int = 10,
    val welcomeEnabled: Boolean = true,
    @RestartRequired val storage: StorageConfig = StorageConfig()
)

interface MarketText {
    fun welcome(player: String): Component
    fun gave(sender: String, target: String, item: Material, amount: Int): Component
    fun shopTitle(player: String): Component
    fun buyButton(item: ShopItem, price: Money): Component
    fun buySuccess(item: ShopItem, amount: Int, total: Money): Component
    fun notEnoughMoney(balance: Money, price: Money): Component

    val commandMarket: Component
    val commandGive: Component
    val argTarget: Component
    val argItem: Component
    val argAmount: Component
    val optionSilent: Component
}

class MarketModule(
    private val config: MarketConfig,
    private val text: MarketText,
    private val shops: ShopService,
    private val economy: EconomyService
) : Module {

    override fun build() = module("market") {
        command("/market") {
            describe(text.commandMarket)
            permission("market.open", default = true)
            sender<Player>()

            run { ctx: CommandContext<Player> ->
                screens.market.open(ctx.sender, page = "home")
            }
        }

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

        on<PlayerJoinEvent>("welcome") {
            filter("welcome-enabled") { config.welcomeEnabled }
            requirePermission("market.use")

            run {
                val profile = tasks.io("load-profile") {
                    shops.loadProfile(player.uniqueId)
                }
                player.sendMessage(text.welcome(profile.displayName))
            }
        }

        on<PlayerMoveEvent>("market-region-hint") {
            priority(EventPriority.LOW)
            filter("block-changed") { from.block != to.block }
            requirePermission("market.use")
            filter("inside-market") { shops.isMarketRegion(to.snapshot()) }
            throttle(250.milliseconds, key = { player.uniqueId })

            run {
                shops.updateNearbyHint(player, to.snapshot())
            }
        }

        on<PlayerMoveEvent>("save-last-market-position") {
            filter("block-changed") { from.block != to.block }
            filter("inside-market") { shops.isMarketRegion(to.snapshot()) }

            debounce(1.seconds, key = { player.uniqueId }) {
                snapshot {
                    LastPositionSnapshot(player.uniqueId, to.snapshot())
                }
            }

            run { snapshot: LastPositionSnapshot ->
                shops.saveLastPosition(snapshot.playerId, snapshot.location)
            }
        }

        every(5.minutes) {
            shops.flushDirtySnapshots()
        }

        screen("market") {
            defaultSession {
                ttl = config.sessionTtlMinutes.minutes
                maxStackDepth = 8
                closeOnQuit()
            }

            page("home") {
                title { text.shopTitle(player.name) }
                rows = 6

                inventoryPolicy {
                    top.denyByDefault()
                    player.allowVanilla()
                    repairOnDeniedAction()
                    returnCursorOnClose()
                }

                list(
                    slots = 10..43,
                    data = shops.visibleItems(player.uniqueId),
                    key = { item -> item.id }
                ) { item ->
                    icon(item.material) {
                        name(item.displayName)
                        lore(text.buyButton(item, item.price))
                    }

                    leftClick {
                        buy(player, item, amount = 1)
                        refresh()
                    }

                    shiftLeftClick {
                        buy(player, item, amount = config.maxBuyAmount)
                        refresh()
                    }

                    rightClick {
                        open("details", item.id)
                    }
                }
            }
        }
    }

    private suspend fun give(sender: Player, target: Player, item: Material, amount: Int, silent: Boolean) {
        shops.give(target.uniqueId, item, amount)
        sender.sendMessage(text.gave(sender.name, target.name, item, amount))
        if (!silent) broadcast(text.gave(sender.name, target.name, item, amount))
    }
}
```

## 第一屏必须隐藏的东西

普通插件作者不应该在第一案例里看到：

- `services.get<T>()`。
- `RuntimeServices`。
- `ConfigRuntime` / `ConfigKey`。
- `TextRuntime` / `MessageKey`。
- `FrameworkPlugin`、`PluginSpec`、artifact provider。
- 手写 `plugin.yml` / `paper-plugin.yml`。
- 手写 command descriptor provider。
- 手写 `registerEvents` / `HandlerList.unregisterAll`。
- 手写 `BukkitTask`、裸 `CoroutineScope`、裸 thread pool。
- 直接使用 Paper GUI adapter。

这些都可以作为高级 API、测试 API 或内部实现存在。

## 模块依赖和注入

模块构造函数表达业务依赖：

```kotlin
class MarketModule(
    private val config: MarketConfig,
    private val text: MarketText,
    private val shops: ShopService,
    private val economy: EconomyService
) : Module
```

框架负责：

- 从 plugin declaration 创建 config/text snapshot。
- 解析 required/optional integrations。
- 建立 module owner。
- 注入模块依赖。
- 启动模块声明里的 command/event/task/gui。

## Artifact split

建议新项目模块拆分：

| Artifact | 内容 |
| --- | --- |
| `runtime-core` | owner、lifecycle、diagnostics 基础、snapshot/report/policy 类型 |
| `runtime-plugin` | `PaperPlugin` / `Module` facade、module dependency graph |
| `runtime-task` | affinity、structured task、timeout、join/race、scheduler adapter seam |
| `runtime-event` | scoped event、filters、priority、backpressure、domain events |
| `runtime-command` | command descriptor、argument model、help/error/suggestion contracts |
| `runtime-config` | config schema、snapshot、reload report |
| `runtime-text` | typed catalog、MiniMessage/Legacy/plain format model |
| `runtime-gui` | session/page/slot/render/event routing neutral model |
| `adapter-paper` | Paper 1.20.1+ JavaPlugin bridge、events、commands、inventory |
| `adapter-cloud` | optional command backend |
| `adapter-config-yaml` | comment-preserving YAML |
| `adapter-cache-caffeine` | optional Caffeine implementation |
| `adapter-network-ktor` | optional Ktor client |
| `adapter-integrations-*` | Vault、LuckPerms、PlaceholderAPI、ProtocolLib 等 |
| `framework-gradle-plugin` | metadata/schema/catalog generation and verification |
| `samples/best-practice-plugin` | 唯一推荐给普通开发者复制的样例 |
| `samples/dogfood-plugin` | 内部集成深度样例，不作为第一阅读入口 |

## 启动流程

```text
Paper reads static yml
-> JavaPlugin bridge loads packaged descriptors
-> create plugin owner and runtime
-> load config snapshots and text catalogs
-> resolve integrations
-> instantiate modules
-> start module declarations
-> register commands/events/gui adapters
-> publish StartReport
```

## Reload 流程

```text
admin command
-> create command owner
-> async load config/text candidates
-> validate schema/catalog
-> build ReloadPlan
-> apply module reload in dependency order
-> invalidate cache by scope/version
-> keep old snapshot on failure
-> render ReloadReport
-> store diagnostics
```

## 关闭流程

```text
disable plugin
-> stop accepting new events/commands/tasks
-> close owner tree bottom-up
-> drain critical writers within timeout
-> cancel remaining tasks/listeners/sessions/requests
-> export CloseReport
-> assert no active framework resource remains
```

