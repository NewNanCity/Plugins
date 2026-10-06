# GuiRuntime

## 定位

GuiRuntime 是 UI runtime，不是 Inventory helper。它把 Inventory event routing、player session、page stack、component tree、render diff、async data provider、chat input、object protection 和 diagnostics 收进一个深 Module。

现有 GUI 已经包含 session、page、component、paginated、scrollable、data provider、storage slot、object protection、chat input 等能力。下一代设计不应该把它退化成“几个 slot 回调”，而应承认它是 Minecraft 插件里的小型 UI runtime。

## 当前压力

现有代码和业务场景已经证明这些需求：

- 一个玩家可能经历多页面、多 session、返回栈和聊天输入。
- 页面需要分页、滚动、异步加载、旧请求丢弃。
- InventoryClick/Drag/Close/Open、AsyncChatEvent、PlayerQuitEvent 都要路由到正确页面。
- item protection、storage slot、shift-click、drag、drop、creative 等安全细节不能让每个页面重复处理。
- `SessionStorage.currentGuiManager` 这种全局当前值会让 event routing owner 不清。
- `BaseComponent.close()` 不能误关 page scheduler。

## Interface

调用者需要理解的核心概念：

- `GuiSession`: 玩家级 UI 会话 owner。
- `GuiPage`: 一个打开或可打开的页面。
- `GuiComponent`: page 内的交互区域或数据区域。
- `RenderModel`: 可异步生成的轻量 UI 描述。
- `RenderPatch`: 主线程应用到 Inventory 的 slot diff。
- `DataProvider<T>`: sync/async/windowed/streaming 数据源。
- `GuiActionContext`: 点击、拖拽、关闭、聊天输入等事件上下文。

业务示例：

```kotlin
ctx.gui.open(player, chestGui(Keys.Gui.BookList, rows = 6)) {
    paginated("books") {
        slots(rect(x = 1, y = 1, width = 7, height = 4))
        data { page, size -> books.findPage(page, size) }
        item { book -> BookItemDescriptor(book) }
        onClick { ctx, book ->
            ctx.commands.execute(ctx.player, "book open ${book.id}")
        }
    }
}
```

Interface 不变量：

- 每个 session/page/component 必须有 owner。
- event routing 必须按 inventory/page/component owner，不依赖 global current manager。
- async data result 必须带 generation id，旧 generation 不能覆盖新页面。
- Bukkit `Inventory` 和 `ItemStack` materialize 必须在正确 affinity。
- component close 只关闭自己的 owner，不关闭 page/session scheduler。
- page close 必须取消 page 下的 task、event scope、chat input 和 data loading。

## Runtime 结构

```text
GuiRuntime
├── SessionRegistry
├── PageRegistry
├── EventRouter
├── ComponentTree
├── RenderEngine
├── DataRuntime
├── ChatInputRuntime
├── ObjectProtection
└── GuiDiagnostics
```

## Session 和 Page

Session 是 player scope 下的 UI owner：

```text
player:{uuid}
└── gui-session:default
    ├── page:home
    └── page:book-list
```

Page 是当前 open inventory 的 owner：

- 绑定 player。
- 绑定 inventory identity。
- 持有 component tree。
- 持有 page generation。
- 持有 page-level tasks。
- 持有 page-scoped event subscriptions。

Page stack：

```kotlin
ctx.gui.session(player).push(BookListPage())
ctx.gui.session(player).replace(BookDetailPage(bookId))
ctx.gui.session(player).back()
ctx.gui.session(player).close(CloseReason.UserClosed)
```

## Event Routing

Inventory event 先进入 EventRuntime，再交给 GuiRuntime routing：

```text
InventoryClickEvent
-> EventRuntime subscription owner=GuiRuntime
-> GuiRuntime.EventRouter
-> PageRegistry.resolve(event.inventory)
-> ComponentTree.hitTest(slot)
-> component action
```

Routing key 优先级：

1. framework-owned InventoryHolder / page id。
2. Inventory identity -> page registry。
3. player open inventory -> active page。
4. fallback diagnostic，拒绝不明来源事件。

禁区：

- 不使用 global current manager 判断事件归属。
- 不在每个 page 手写全套 InventoryClick/Drag/Close 保护。
- 不让 event handler 直接执行重 I/O。

## Render Model 和 Diff

渲染分两段：

```text
state changed
-> mark dirty component
-> AsyncCpu/AsyncIo build RenderModel
-> Entity/Main materialize ItemStack
-> compute RenderPatch
-> apply changed slots
-> record RenderReport
```

`RenderModel` 是轻量、不可变、可跨线程的数据：

```kotlin
data class SlotModel(
    val slot: Int,
    val item: ItemDescriptor,
    val actionId: ActionId?,
    val protection: SlotProtection
)
```

`ItemDescriptor` 不应长期持有 mutable Bukkit 对象。它描述 material、name key、lore key、amount、flags、custom model data 等；真正的 `ItemStack` 在正确 affinity materialize。

优化目标：

- 不整页重建，只应用 diff。
- layout 预编译，避免每次刷新重新算坐标。
- static text/template 预编译。
- 高频刷新合并。
- ItemStack 创建数进入 diagnostics。

## DataRuntime

GUI 数据源有四类：

- sync small data：内存小列表。
- async page data：数据库、文件、网络查询。
- windowed data：分页/滚动只取窗口。
- streaming data：逐批追加或刷新。

Interface：

```kotlin
paginated<Book>("books") {
    pageSize(28)
    data {
        affinity(Affinity.AsyncIo)
        cache(ttl = 5.seconds, key = { playerId to query })
        load { page, size -> books.findPage(query, page, size) }
    }
    item { book -> BookItemDescriptor(book) }
}
```

DataRuntime 要处理：

- generation id。
- cancellation token。
- loading/empty/error state。
- stale result drop。
- window cache。
- backpressure。
- timeout。

## Component Tree

Component 是 page 内的交互 Module：

- single slot。
- rect/line/fill。
- paginated list。
- scrollable list。
- storage slot。
- button。
- input prompt。
- custom component。

Component Interface 关注：

- layout。
- render model。
- event handling。
- close。
- diagnostics tags。

Component Implementation 隐藏：

- slot mapping。
- dirty tracking。
- action id registry。
- item protection。
- child component traversal。

## Chat Input

聊天输入也是 GUI scope 的一部分，不应散落在全局 map：

```kotlin
ctx.gui.chatInput(player, "rename-book") {
    owner(page.owner)
    prompt(Keys.Gui.RenamePrompt)
    timeout(30.seconds)
    validate { input -> BookTitle.validate(input) }
    onSubmit { title -> rename(title) }
    onCancel { ctx.text.send(player, Keys.Gui.RenameCancelled) }
}
```

要求：

- 玩家退出、page close、timeout 时自动取消。
- AsyncChatEvent affinity 显式处理。
- 输入 validation 失败可继续等待。
- permission 可复验。
- diagnostics 记录 active prompts。

## ObjectProtection

ObjectProtection 统一处理：

- shift-click。
- drag。
- double-click collect。
- drop key。
- number key。
- hopper/move item edge cases。
- creative 模式特殊行为。
- storage slot 允许的取放规则。

业务只声明 slot policy：

```kotlin
storageSlot("deposit") {
    allowPut { item -> item.type == Material.DIAMOND }
    allowTake { actor -> actor.hasPermission("bank.withdraw") }
}
```

## Implementation

GuiRuntime 内部隐藏：

- SessionStorage。
- InventoryHolder/page id 绑定。
- Inventory event subscription。
- page stack。
- component tree traversal。
- render dirty tracking。
- render generation。
- ItemStack materialization。
- async data cancellation。
- chat input subscription。
- object protection rules。
- slow render diagnostics。

## Seam 和 Adapter

外部 Seam：

- `GuiRuntime` Interface -> Bukkit/Paper Inventory API。
- `EventRouter` -> EventRuntime subscription。
- `RenderEngine` -> Bukkit ItemStack materialization。
- `TextRuntime` -> title/name/lore rendering。

Adapter：

- `BukkitInventoryAdapter`: create/open/close/update Inventory。
- `PaperComponentAdapter`: Adventure/MiniMessage title support。
- `EventRuntimeGuiAdapter`: Inventory/Chat/PlayerQuit event bridge。
- `TestGuiAdapter`: fake inventory and event router。

SmartInvs、Triumph GUI、IF、devnatan inventory-framework 是相关工作参考，不进入业务 Interface。

## 协作关系

- Lifecycle/OwnerRuntime: session/page/component 都是 owner。
- TaskRuntime: async data、render model、materialize/apply diff 按 affinity 执行。
- EventRuntime: Inventory/Chat/PlayerQuit 事件进入 GuiRuntime routing。
- CommandRuntime: command 打开 GUI，GUI action 也可触发 command。
- ConfigRuntime: GUI layout、page size、feature toggle 可来自 snapshot。
- TextRuntime: title、button、lore、error/loading/empty state。
- CacheRuntime: page/window/render template cache。
- DiagnosticsRuntime: open pages、slow render、stale drops、active prompts。

## Diagnostics

Gui diagnostics 应包含：

- open sessions。
- active pages。
- page stack depth。
- component count。
- render generation。
- dirty component count。
- last render duration。
- updated slot count。
- ItemStack materialization count。
- data load latency。
- stale result drops。
- active chat prompts。
- protection blocked action count。

示例：

```text
gui page book-list player=uuid
components=8 generation=42 render=3.4ms updatedSlots=12 staleDrops=2
data books latency=41ms cacheHit=true
```

## Harness

`GuiHarness` 需要覆盖：

- page open/close 创建和释放 owner。
- InventoryClickEvent 路由到正确 component。
- 未知 inventory event 被拒绝并诊断。
- async data generation 旧结果不覆盖新页面。
- component close 不关闭 page scheduler。
- page close 取消 data task 和 chat input。
- render diff 只更新变化 slot。
- storage slot protection。
- PlayerQuit 关闭 player scope。
- AsyncChatEvent 输入 timeout/cancel/submit。

## 开放问题

1. Page identity 优先使用 InventoryHolder、persistent key，还是 registry identity。
2. 是否允许一个玩家多个 named session 同时存在。
3. GUI title 更新策略是否统一通过 reopen，还是支持平台差异 Adapter。
4. ItemDescriptor 到 ItemStack 的缓存粒度如何设定。
5. ObjectProtection 的默认安全策略是 deny-by-default 还是 mimic vanilla。
