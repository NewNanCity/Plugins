# GUI

## 是什么

GuiRuntime 是 Minecraft inventory UI runtime，不是 Inventory helper。它负责：

- screen registry。
- player session。
- named session。
- page stack。
- slot policy。
- component/list/input/output/storage。
- click/drag/close/open/quit routing。
- render diff。
- async data generation。
- object protection。
- diagnostics。

第一屏写法：

```kotlin
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

        list(slots = 10..43, data = shops.visibleItems(player.uniqueId), key = { it.id }) { item ->
            icon(item.material) {
                name(item.displayName)
                lore(text.buyButton(item, item.price))
            }

            leftClick {
                buy(player, item, amount = 1)
                refresh()
            }
        }
    }
}
```

## 相关工作和取舍

### 原生 Inventory + Listener

控制力强，但 slot routing、安全、分页、close cleanup、玩家退出都要重复写。只作为 adapter 底层。

### SmartInvs

manager、page system、slot iterator、click action 有价值。缺点是和 owner/task/text/diagnostics 不统一。

### Triumph GUI

per-item/per-slot action、paginated、scrolling、storage GUI 体验好。我们吸收业务侧简洁表达，但内部要更 runtime 化。

### InventoryFramework / pane-based

pane/layout 思路适合复杂 GUI。我们吸收 layout 概念，但不优先引入 XML。

### devnatan inventory-framework

强调平台问题和安全鲁棒性由框架处理。我们吸收这个目标，但 Paper 1.20.1+ 优先，不把跨平台作为近期主目标。

## slot 类型

| 类型 | 语义 |
| --- | --- |
| button | 展示对象，不允许玩家拿走。点击触发 action。 |
| list item | 带 stable key 的动态行。支持 stale generation 丢弃。 |
| input slot | 玩家可以放入物品。必须声明 allowPut/allowTake/onChange/onClose。 |
| output slot | 框架提供物品，玩家领取后 commit，一次性语义要防刷。 |
| storage slot | 明确 opt-in 的真实存储格。默认不允许。 |
| decoration | 展示，不响应。 |

ObjectProtection 默认 deny-by-default。任何可交互物品都必须显式声明。

## 用户 hack 和安全边界

必须防护：

- shift-click。
- number-key hotbar swap。
- double click collect。
- drag。
- drop key。
- cursor item。
- creative mode 行为差异。
- close 时 cursor/item return。
- inventory move/hopper 类非玩家移动。
- lag 下重复点击导致 double commit。

输出格领取必须使用 commit token 或 generation id，防止同一奖励被领取多次。

## 内部方案

```text
screen declaration
-> screen descriptor
-> session owner
-> page owner
-> component tree
-> render model generation
-> main-thread ItemStack materialization
-> slot diff apply
-> event adapter normalizes click/drag/close
-> route to page/component owner
-> policy/action/commit
-> diagnostics
```

缓存策略：

- 缓存 static ItemDescriptor。
- 含 player/locale/dynamic placeholder 的最终 ItemStack 默认不长期缓存。
- list data 用 stable key 和 generation id。
- refresh 只更新 dirty component/slot。

Page identity：

- framework registry identity 是事实源。
- InventoryHolder/persistent key 只是 Paper adapter 定位手段。

Named session：

- 默认只有 `default`。
- 业务显式创建 named session 时必须声明 owner、TTL、最大 stack 深度、关闭策略。

Title update：

- adapter 支持平台差异。
- 默认 fallback 为 reopen。

## 测试

- click/drag/close/open routing。
- shift-click/hotbar/drop 防护。
- input slot allow/deny/return。
- output slot one-time commit。
- stale generation rejection。
- page stack back/open。
- player quit closes sessions。
- Paper adapter materializes only on main affinity。

