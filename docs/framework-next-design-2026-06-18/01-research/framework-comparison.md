# Event / GUI / Command 封装方式比较

本页比较不同框架或常见模式如何封装 event、GUI、command。目的不是现在选依赖，而是从它们的 Interface 深度、心智负担和失败模式里提炼设计指导。

## 比较维度

每种方案都按这些维度看：

- Interface 深度：调用者学少量概念能否获得大量能力。
- lifecycle：注册、注销、关闭、reload、玩家退出是否自动处理。
- thread affinity：主线程、异步、区域/实体线程是否显式。
- metadata：权限、描述、帮助、静态 yml、诊断是否有同一事实源。
- observability：能否看见 active listener/task/gui/command、耗时和失败。
- escape hatch：遇到 Minecraft 特例时是否能安全退回底层能力。

## Event 封装

### 1. 原生 Bukkit/Paper Listener

形态：

```kotlin
class MyListener : Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onJoin(event: PlayerJoinEvent) {}
}
```

优点：

- 官方语义完整：priority、ignoreCancelled、cancellable、HandlerList 都是平台原生概念。
- 行为透明，几乎没有框架魔法。
- 适合简单、长期存在、插件级别的 listener。

缺点：

- lifecycle 靠开发者记忆：何时注册、注销、避免重复注册。
- 临时作用域难写，例如“只等玩家下一次点击”。
- handler 和任务协作靠手写 task id、BukkitTask、状态 map。
- 诊断弱，很难知道哪个模块注册了什么 listener。

指导意义：

下一代 EventRuntime 必须完整覆盖原生语义，尤其是 `priority` 和 `ignoreCancelled`。不能为了 DSL 简洁丢掉平台事实。

### 2. 当前 subscribeEvent 风格

形态：

```kotlin
subscribeEvent<PlayerJoinEvent> {
    priority(EventPriority.MONITOR)
    filter { it.player.hasPlayedBefore() }
    expireAfter(1)
    handler { event -> ... }
}
```

优点：

- 本地化：listener 写在模块附近，不需要额外类。
- 可绑定 BaseModule 生命周期，比原生 listener 更接近 owner 模型。
- filter、expire、exception handler 提升了常见场景的表达力。

缺点：

- 目前缺 `ignoreCancelled`。
- expiry 语义容易歧义：是看到 N 次事件，还是处理 N 次事件。
- thread affinity 没有显式表达，AsyncChatEvent 这类事件有风险。
- domain event 和第三方 listener 还没有统一 scope。

指导意义：

方向正确，但需要升级成 EventRuntime：subscription 必须是 owner tree 的 child，并且要接入 affinity、diagnostics、backpressure、event-task bridge。

### 3. Reactive / stream 风格

形态：

```kotlin
events.stream<PlayerMoveEvent>()
    .filter { it.player == player }
    .throttle(250.milliseconds)
    .map { it.toLocationSnapshot() }
    .collect(owner = session) { snapshot -> ... }
```

优点：

- 非常适合高频事件：throttle、debounce、coalesce、buffer、drop latest 都自然。
- event 与 task 的协作容易建模。
- 可读性强，能把“事件流 -> 状态变化”表达清楚。

缺点：

- 如果直接暴露完整 reactive 库，Interface 会变浅，开发者需要学太多操作符。
- 线程和取消语义容易复杂。
- Debug stack 与异常路径可能不如普通 handler 直观。

指导意义：

不要把完整 reactive 库当公共 Interface；吸收少数高价值操作符作为 EventRuntime 的能力：`throttle`、`debounce`、`coalesceBy`、`dropIfBusy`、`awaitOne`。

### 4. DomainEventBus

形态：

```kotlin
domainEvents.on<RailAreaUpdated>("refresh-detector", owner = detector) {
    handle { event -> refresh(event.areaId) }
}
```

优点：

- 适合模块内部或插件内部的业务事件。
- 比 Bukkit Event 更轻，不需要 HandlerList。
- 可以统一进入 owner、diagnostics、test harness。

缺点：

- 过度使用会隐藏控制流。
- ordering、transaction、同步/异步策略如果不显式，会变成隐性耦合。

指导意义：

DomainEventBus 应作为 EventRuntime 的一部分，但只用于明确的业务状态变化，不替代普通函数调用。

## GUI 封装

### 1. 原生 Inventory + Listener

形态：自己创建 Inventory，监听 InventoryClick/Drag/Close/Open，手写 slot 到行为的映射。

优点：

- 控制力最强。
- 没有额外抽象，遇到版本差异可以直接处理。
- 简单菜单成本低。

缺点：

- 每个页面都要重写 slot routing、click protection、drag protection、close/open、玩家退出清理。
- 分页、滚动、存储格、异步加载、旧数据丢弃都很容易重复造。
- 安全细节多：shift-click、creative、hopper move、drop key、book edit 等。

指导意义：

原生方式适合作为底层 Adapter，不适合作为业务 Interface。

### 2. SmartInvs 风格

SmartInvs 提供 inventory manager、builder、page system、custom listeners、slot iterator、click actions、tick update 等能力。

优点：

- 把常见 inventory GUI 样板大幅减少。
- page system、slot iterator、click action 是高频刚需。
- 可以作为插件依赖或库使用，说明它把 runtime manager 作为核心概念。

缺点：

- 公开能力偏 UI 操作层，和我们的 module owner、config、i18n、task、diagnostics 不是同一运行时。
- tick update 如果滥用，容易放大主线程成本。
- 老项目生态可能存在版本维护压力。

指导意义：

吸收 manager + page system + slot iterator + click action；避免让每页自己决定 tick update，而应通过 GuiRuntime 的 render diff 与 backpressure 管理。

### 3. Triumph GUI 风格

Triumph GUI 主打简单易用、per item action、slot-specific action、open/close action、paginated、scrolling、storage GUI、item builder。

优点：

- Interface 对业务开发者很友好。
- per-item/per-slot actions 很接近插件开发者的心智。
- paginated/scrolling/storage 是真实需求，不是过度设计。

缺点：

- 简单 Interface 容易隐藏 owner、线程、异步数据和诊断问题。
- 如果 action 直接闭包捕获大量状态，page lifecycle 需要特别严谨。

指导意义：

我们的 GUI Interface 应保持业务侧简单，但内部必须比 Triumph GUI 更 runtime 化：page owner、data generation、render model、diff、task bridge、diagnostics 都要隐藏在深 Module 里。

### 4. IF / pane-based 风格

InventoryFramework 使用 pane 原则：GUI 被分成不同类型的 pane，pane 之间可以交互，也支持从 XML 创建 GUI。

优点：

- pane 是比 slot 更高层的布局概念，适合复杂 GUI。
- XML/声明式布局对静态页面有优势。
- pane 之间交互能减少页面代码里的坐标细节。

缺点：

- pane 模型如果太泛化，Interface 会变大。
- XML 对我们的 Kotlin/i18n/config/runtime 体系未必自然。
- 动态数据、权限过滤、异步加载仍需要额外 runtime 支撑。

指导意义：

吸收 pane/layout 思想，但不一定引入 XML。我们可以用 Kotlin DSL 表达 layout，并让 layout 编译成 slot map。

### 5. devnatan inventory-framework 风格

该库强调 varied interaction treatments、检测并处理平台已知问题、secure/robust API，并覆盖 Bukkit/Spigot/Folia/Minestom 等。

优点：

- 明确把平台问题和安全鲁棒性藏进框架内部，这是深 Module 的方向。
- 跨平台/多线程语义对 Folia 预留有参考价值。

缺点：

- 越强调跨平台，公共 Interface 越容易受平台最小公分母限制。
- 我们的业务框架如果直接追求跨平台，容易分散当前 Paper 优先目标。

指导意义：

吸收“平台已知问题由框架处理”的目标，但保持 Paper 优先、Folia 预留，不把跨平台作为近期 Interface。

## Command 封装

### 1. 原生 Bukkit CommandExecutor + plugin.yml

优点：

- 平台原生，加载模型简单。
- 小命令足够直接。

缺点：

- `plugin.yml`、executor、tab completer、permission、usage/help 分散。
- 参数解析、补全、错误提示全靠手写。
- 难以生成高质量 help 和静态元数据。

指导意义：

只适合作为底层加载事实，不适合作为业务 Interface。

### 2. CommandAPI 风格

CommandAPI 利用 Minecraft 1.13+ command UI，强调 vanilla command、强类型参数、自动补全、执行前类型检查、argument tooltips、函数/execute 支持，也说明 CommandAPI 命令不需要在 `plugin.yml` 中声明。

优点：

- 与 Brigadier/vanilla command UI 贴得近。
- 参数能力强，错误在执行前暴露。
- 对命令块、函数、`/execute` 等原版能力友好。

缺点：

- Builder/DSL 方式对大量业务命令仍然有样板。
- 如果命令不在 plugin.yml，静态元数据和我们希望的 build-time 产物策略需要额外处理。
- 和当前 Cloud annotations 方向不是同一套生态。

指导意义：

吸收强类型参数、执行前校验和 vanilla UI 适配思想；但我们的主要 Interface 仍应偏声明式注解/DSL，并生成静态 yml 元数据。

### 3. Incendo Cloud 风格

Cloud 提供 command manager、immutable command builder、suggestions、execution coordinator、descriptions；annotations 模块支持 annotated methods、parsers、suggestions、exception handlers，也可作为 annotation processor 使用。

优点：

- 当前仓库已广泛使用，生态匹配度高。
- 注解模型贴近用户喜欢的声明式方式。
- suggestion、parser、exception handler、help 和 execution coordinator 是完整命令运行时。
- 能把 i18n description 映射到 RichDescription。

缺点：

- asyncCoordinator 不是业务线程安全的完整答案，handler 内仍可能误用 Bukkit API。
- plugin yml metadata 生成不是 Cloud 自然解决的问题，需要框架补。
- 过度直接暴露 Cloud 类型会让业务和第三方库强耦合。

指导意义：

Cloud 应是 CommandRuntime 的主要 Adapter，但框架 Interface 要补 owner、ThreadAffinity、plugin yml generation、diagnostics。

### 4. Aikar ACF 风格

ACF 的核心价值是把 DI、validation、tab completion、help documentation、syntax advice、stateful conditions 放到 annotations 后面。

优点：

- 设计理念非常贴近“声明式命令”。
- 对业务开发者心智负担低。
- conditions 和 completion 作为注解能力很有指导意义。

缺点：

- 项目仍标注 0.x/beta 语义，且当前仓库已选 Cloud。
- 直接切换框架没有必要。

指导意义：

ACF 是体验参照，不是当前依赖目标。我们应该吸收它的声明式设计：条件、校验、补全、help、权限都可以在方法声明层表达。

## 总体指导

### 我们应该吸收

- Event: 原生 priority/ignoreCancelled 语义 + scoped subscription + 少量 reactive 背压能力。
- GUI: page/component/pane/layout 思想 + per-slot/per-item action + paginated/scrolling/storage + render diff/runtime owner。
- Command: Cloud annotations 主适配 + ACF 声明式体验 + CommandAPI 强参数/Brigadier UI 思路 + build-time static yml。

### 我们应该避免

- 为了简洁丢掉 Bukkit/Paper 原生语义。
- 把第三方框架类型直接变成业务代码必须理解的主 Interface。
- 在 GUI 里暴露过多底层 Inventory 细节，让每个页面处理安全问题。
- 在 command 里只靠 async coordinator，忽略 Bukkit API 线程归属。
- 给 event 暴露完整 reactive 流库，导致 Interface 过宽。
- 为了跨平台牺牲当前 Paper 优先的清晰性。

## 对下一代框架的结论

EventRuntime、GuiRuntime、CommandRuntime 都应该是深 Module：

- 外部 Interface 简单、声明式、贴近 Minecraft 开发者心智。
- 内部 Implementation 统一处理 owner、thread affinity、diagnostics、lifecycle、backpressure。
- 第三方库只作为 Adapter，不作为业务 Interface 的事实源。
- 对于 event/gui/command 的协作，统一通过 owner tree 和 SchedulerFacade 串起来。

## 资料来源

- Paper event listeners: https://docs.papermc.io/paper/dev/event-listeners/
- Bukkit EventHandler Javadocs: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/event/EventHandler.html
- Incendo Cloud core: https://cloud.incendo.org/core/
- Incendo Cloud annotations: https://cloud.incendo.org/annotations/
- Aikar ACF: https://github.com/aikar/commands
- CommandAPI docs: https://docs.commandapi.dev/intro
- InventoryFramework IF: https://github.com/stefvanschie/IF
- SmartInvs: https://github.com/MinusKube/SmartInvs
- Triumph GUI: https://triumphteam.dev/docs/triumph-gui
- devnatan inventory-framework: https://github.com/devnatan/inventory-framework
