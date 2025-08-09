# 统一事件API系统

GUI1模块的统一事件API实现了完全一致的开发者体验：**所有组件都有相同的事件处理方法名，但接收包含组件特定信息的不同上下文对象**。

## 🎯 设计目标

### 核心理念

1. **统一的方法名**：所有组件都有 `onLeftClick`、`onRightClick`、`onDrag`、`onClose` 等相同的方法
2. **组件特定信息**：不同组件的处理函数接收包含组件相关额外信息的上下文对象
3. **完全一致的体验**：开发者使用相同的API，但自动获得组件特定的丰富信息

### 实现效果

```kotlin
// 所有组件都有相同的方法名
slotComponent { onLeftClick { context -> /* SlotClickContext */ } }
paginatedComponent { onLeftClick { context -> /* PaginatedClickContext<T> */ } }
storageComponent { onLeftClick { context -> /* StorageClickContext */ } }

// 方法名完全相同，但上下文对象包含组件特定信息！
```

## 🏗️ 架构设计

### 统一事件上下文层次

```kotlin
UnifiedEventContext<T : Event>  // 基础统一上下文
    ↓
SlotClickContext               // 单槽组件点击上下文
PaginatedClickContext<T>       // 分页组件点击上下文
StorageClickContext           // 存储组件点击上下文

SlotDragContext               // 单槽组件拖拽上下文
PaginatedDragContext<T>       // 分页组件拖拽上下文
StorageDragContext           // 存储组件拖拽上下文

// 每种事件类型都有对应的组件特定上下文
```

### 统一事件处理器

```kotlin
// 每个组件类型都有自己的事件处理器
SlotEventHandler              // 处理SlotXXXContext
PaginatedEventHandler<T>      // 处理PaginatedXXXContext<T>
StorageEventHandler          // 处理StorageXXXContext

// 但所有处理器都有相同的方法名
```

### 统一DSL接口

```kotlin
// 所有组件都有相同的扩展函数名
fun SingleSlotComponent.onLeftClick(handler: (SlotClickContext) -> Unit)
fun <T> PaginatedComponent<T>.onLeftClick(handler: (PaginatedClickContext<T>) -> Unit)
fun StorageComponent.onLeftClick(handler: (StorageClickContext) -> Unit)
```

## 💡 使用示例

### 1. 单槽组件 - 自动包含坐标信息

```kotlin
slotComponent(x = 4, y = 2) {
    render { context ->
        itemUtil.createItemStack(Material.DIAMOND) {
            name("&b钻石按钮 (${context.x}, ${context.y})")
        }
    }

    // 统一的方法名：onLeftClick
    // 接收SlotClickContext，自动包含x, y, slot信息
    onLeftClick { context ->
        context.player.sendMessage("&e左键点击坐标 (${context.x}, ${context.y})")
        context.player.sendMessage("&7槽位: ${context.slot}")
        context.player.sendMessage("&7点击的物品: ${context.clickedItem?.type}")
    }

    // 统一的方法名：onRightClick
    onRightClick { context ->
        context.player.sendMessage("&b右键点击坐标 (${context.x}, ${context.y})")
        context.player.sendMessage("&7是否Shift点击: ${context.isShiftClick}")
    }

    // 统一的方法名：onDrag
    onDrag { context ->
        context.player.sendMessage("&c在坐标 (${context.x}, ${context.y}) 拖拽物品")
        context.player.sendMessage("&7拖拽类型: ${context.dragType}")
    }

    // 统一的方法名：onClose
    onClose { context ->
        context.player.sendMessage("&7关闭了包含坐标 (${context.x}, ${context.y}) 的GUI")
        context.player.sendMessage("&7关闭原因: ${context.reason}")
    }
}
```

### 2. 分页组件 - 自动包含分页信息

```kotlin
val players = Bukkit.getOnlinePlayers().toList()

paginatedComponent<Player>(
    startX = 1, startY = 1,
    width = 7, height = 4,
    data = players
) {
    renderData { player, index, globalIndex ->
        itemUtil.createItemStack(Material.PLAYER_HEAD) {
            name("&a${player.name}")
            skull(player)
        }
    }

    // 统一的方法名：onLeftClick
    // 接收PaginatedClickContext<Player>，自动包含分页相关信息
    onLeftClick { context ->
        context.player.sendMessage("&e左键点击分页组件")
        context.player.sendMessage("&7页面: ${context.pageIndex + 1}/${context.totalPages}")
        context.player.sendMessage("&7索引: ${context.index}")
        context.player.sendMessage("&7全局索引: ${context.globalIndex}")
        context.player.sendMessage("&7相对坐标: (${context.relativeX}, ${context.relativeY})")

        if (context.hasItem()) {
            val targetPlayer = context.item!! // 类型安全：Player类型
            context.player.sendMessage("&a选择了玩家: ${targetPlayer.name}")
            targetPlayer.sendMessage("&b${context.player.name} 选择了你！")
        } else {
            context.player.sendMessage("&c这是一个空槽位")
        }
    }

    // 统一的方法名：onRightClick
    onRightClick { context ->
        if (context.hasItem()) {
            val targetPlayer = context.item ?: return@render null
            context.player.sendMessage("&b右键查看玩家: ${targetPlayer.name}")
            context.player.sendMessage("&7位置: ${targetPlayer.location}")
        }
    }

    // 统一的方法名：onDrag
    onDrag { context ->
        context.player.sendMessage("&c在分页组件中拖拽")
        context.player.sendMessage("&7页面: ${context.pageIndex + 1}")
        if (context.hasItem()) {
            context.player.sendMessage("&7拖拽的数据: ${context.item}")
        }
    }

    // 便捷方法：只处理有数据项的点击
    onItemClick { context, player, index ->
        context.player.sendMessage("&a便捷处理：点击了第 ${index + 1} 个玩家 ${player.name}")
        // 类型安全：player确保是Player类型
    }
}
```

### 3. 存储组件 - 自动包含存储信息

```kotlin
storageSlotComponent(x = 4, y = 2) {

    // 统一的方法名：onLeftClick
    // 接收StorageClickContext，自动包含storedItem等存储信息
    onLeftClick { context ->
        context.player.sendMessage("&e左键点击存储槽 (${context.x}, ${context.y})")
        context.player.sendMessage("&7当前存储: ${context.storedItem?.type ?: "空"}")
        context.player.sendMessage("&7点击物品: ${context.clickedItem?.type ?: "空"}")
        context.player.sendMessage("&7最大堆叠: ${context.maxStackSize}")

        // 使用存储特定的便捷方法
        if (context.canMergeWithStored()) {
            context.player.sendMessage("&a可以合并物品")
        }

        val clickedItem = context.clickedItem
        val storedItem = context.storedItem

        when {
            clickedItem != null && storedItem == null -> {
                context.player.sendMessage("&a放入物品: ${clickedItem.type}")
                // 执行存储逻辑
            }
            clickedItem == null && storedItem != null -> {
                context.player.sendMessage("&b取出物品: ${storedItem.type}")
                // 执行取出逻辑
            }
        }
    }

    // 统一的方法名：onRightClick
    onRightClick { context ->
        if (context.hasStoredItem()) {
            context.player.sendMessage("&b右键取出一半物品")
            val storedItem = context.storedItem!!
            context.player.sendMessage("&7取出 ${storedItem.amount / 2} 个 ${storedItem.type}")
        }
    }

    // 统一的方法名：onDrag
    onDrag { context ->
        context.player.sendMessage("&c不能拖拽到存储槽")
        context.player.sendMessage("&7存储状态: ${if (context.hasStoredItem()) "有物品" else "空"}")
        context.stopPropagation() // 阻止拖拽
    }

    // 便捷的存储操作处理
    onStorageOperation(
        onPut = { context, item ->
            context.player.sendMessage("&a放入物品: ${item.type}")
        },
        onTake = { context, storedItem ->
            context.player.sendMessage("&b取出物品: ${storedItem.type}")
        },
        onSwap = { context, clickedItem, storedItem ->
            context.player.sendMessage("&c交换物品: ${clickedItem.type} <-> ${storedItem?.type}")
        }
    )
}
```

## 🎯 统一体验的优势

### 1. 学习成本极低

```kotlin
// 开发者只需要记住一套方法名
// 无论什么组件，都是这些方法：
onClick, onLeftClick, onRightClick, onShiftClick, onMiddleClick, onDoubleClick
onDrag, onDragSingle, onDragEven
onClose, onOpen

// 完全统一的API！
```

### 2. 智能的上下文信息

```kotlin
// 单槽组件自动提供坐标信息
slotComponent { onLeftClick { context -> context.x, context.y, context.slot } }

// 分页组件自动提供分页信息
paginatedComponent { onLeftClick { context -> context.item, context.index, context.pageIndex } }

// 存储组件自动提供存储信息
storageComponent { onLeftClick { context -> context.storedItem, context.canMergeWithStored() } }
```

### 3. 类型安全保证

```kotlin
paginatedComponent<Player>(data = players) {
    onLeftClick { context ->
        // context.item 是 Player? 类型，编译时类型安全
        val player = context.item
        if (player != null) {
            // 编译器知道这里 player 是 Player 类型
            player.sendMessage("Hello!")
        }
    }
}
```

### 4. IDE智能提示

```kotlin
// 无论什么组件，IDE都会提示相同的方法名
component. // IDE提示：onClick, onLeftClick, onRightClick...

// 但上下文对象会根据组件类型提供不同的属性
context. // 根据组件类型提示不同的属性
```

## 🔄 与传统方式的对比

### 传统方式（不统一）

```kotlin
// 不同组件有不同的方法名
slotComponent { onSlotClick { ... } }
paginatedComponent { onPaginatedClick { ... } }
storageComponent { onStorageClick { ... } }

// 开发者需要记住每个组件的特定方法名
// 学习成本高，容易混淆
```

### 统一API方式

```kotlin
// 所有组件都有相同的方法名
slotComponent { onLeftClick { context -> /* SlotClickContext */ } }
paginatedComponent { onLeftClick { context -> /* PaginatedClickContext<T> */ } }
storageComponent { onLeftClick { context -> /* StorageClickContext */ } }

// 方法名完全统一，但上下文信息丰富且类型安全
// 学习成本低，开发体验一致
```

## 🚀 扩展新组件

添加新组件时，只需要：

1. **创建组件特定的上下文类**
2. **创建组件特定的事件处理器**
3. **添加统一的DSL扩展函数**

```kotlin
// 1. 创建上下文
class CustomClickContext(...) : UnifiedEventContext<InventoryClickEvent>(...)

// 2. 创建处理器
class CustomEventHandler { ... }

// 3. 添加DSL
fun CustomComponent.onLeftClick(handler: (CustomClickContext) -> Unit) { ... }

// 新组件自动拥有统一的API！
```

## 🎉 总结

统一事件API系统实现了：

- ✅ **完全统一的方法名**：所有组件都有相同的事件处理方法
- ✅ **丰富的组件信息**：每个组件的上下文包含特定的额外信息
- ✅ **类型安全保证**：编译时类型检查，避免运行时错误
- ✅ **极低的学习成本**：开发者只需要学习一套API
- ✅ **一致的开发体验**：无论什么组件，使用方式完全相同

这就是你想要的**统一开发者体验**！🎯
