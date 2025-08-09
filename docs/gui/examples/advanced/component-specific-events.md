# 组件特定事件上下文系统（重构版）

GUI1模块提供了统一的组件特定事件上下文系统，为不同类型的组件和页面提供丰富的上下文信息，避免重复代码并提供类型安全的事件处理。

## 📋 目录

- [概述](#概述)
- [重构后的架构设计](#重构后的架构设计)
- [统一的组件事件上下文](#统一的组件事件上下文)
- [使用示例](#使用示例)
- [最佳实践](#最佳实践)
- [迁移指南](#迁移指南)

## 🎯 概述

### 设计理念

重构后的事件上下文系统遵循以下核心原则：

1. **统一性**：每个组件类型只有一个事件上下文类，适用于所有事件类型
2. **内聚性**：组件特定的事件上下文定义在对应的组件文件中
3. **一致性**：同一组件对所有事件类型提供相同的额外信息
4. **扩展性**：通用的事件处理机制，易于添加新的组件类型

### 核心改进

- **不再为每种事件类型创建单独的上下文类**
- **组件特定信息对所有事件类型统一提供**
- **代码内聚性更好，相关代码放在一起**
- **更简洁的API和更好的维护性**

## 🏗️ 重构后的架构设计

### 继承层次结构

```kotlin
EventContext<T : Event>
    ↓
ComponentEventContext<T : Event, C : Component>  // 基类（在EventContext.kt中）
    ↓
SlotComponentEventContext                        // 在SingleSlotComponent.kt中
PaginatedComponentEventContext<T>               // 在PaginatedComponent.kt中
StorageComponentEventContext                    // 在SingleStorageSlotComponent.kt中
ChestPageEventContext                           // 在ChestPage.kt中
BookPageEventContext                            // 在BookPage.kt中
```

### 核心设计原则

1. **统一的事件上下文**：每个组件类型只有一个事件上下文类
2. **代码内聚性**：事件上下文类定义在对应的组件/页面文件中
3. **通用事件处理**：所有组件处理相同的事件类型
4. **自动上下文创建**：通过`createComponentEventContext()`方法

### 事件处理流程

```kotlin
1. 事件发生 → BaseComponent.handleEvent()
2. 调用 createComponentEventContext() 创建组件特定上下文
3. 如果返回null，使用基础EventContext
4. 将上下文传递给EventHandlers处理
```

## 🧩 统一的组件事件上下文

### SlotComponentEventContext

为单槽组件的**所有事件类型**提供统一的上下文：

```kotlin
class SlotComponentEventContext(
    event: Event,         // 任何类型的事件
    player: Player,
    component: SingleSlotComponent,
    val x: Int,           // 组件X坐标
    val y: Int,           // 组件Y坐标
    val slot: Int         // 槽位索引
) : ComponentEventContext<Event, SingleSlotComponent>
```

**统一提供的信息**：
- `x`, `y` - 组件在GUI中的坐标
- `slot` - 槽位索引
- `component` - 组件实例引用

**事件特定属性**（根据事件类型自动提供）：
- `clickType`, `clickedItem`, `isLeftClick` 等（点击事件）
- `dragType`, `draggedSlots` 等（拖拽事件）
- `closeReason`（关闭事件）

### PaginatedComponentEventContext

为分页组件的**所有事件类型**提供统一的上下文：

```kotlin
class PaginatedComponentEventContext<T>(
    event: Event,         // 任何类型的事件
    player: Player,
    component: PaginatedComponent<T>,
    val slot: Int,        // 相关槽位
    val x: Int,           // 绝对X坐标
    val y: Int,           // 绝对Y坐标
    val relativeX: Int,   // 相对X坐标
    val relativeY: Int,   // 相对Y坐标
    val index: Int,       // 页面内索引
    val pageIndex: Int,   // 当前页码
    val globalIndex: Int, // 全局索引
    val dataItem: T?      // 数据项
) : ComponentEventContext<Event, PaginatedComponent<T>>
```

**统一提供的信息**：
- 分页相关：`pageIndex`, `index`, `globalIndex`
- 坐标信息：`x`, `y`, `relativeX`, `relativeY`
- 数据项：`dataItem`
- 便捷方法：`hasDataItem()`, `isEmpty()`, `isLastItemInPage()` 等

### StorageComponentEventContext

为存储组件的**所有事件类型**提供统一的上下文：

```kotlin
class StorageComponentEventContext(
    event: Event,         // 任何类型的事件
    player: Player,
    component: SingleStorageSlotComponent,
    val x: Int,           // 组件X坐标
    val y: Int,           // 组件Y坐标
    val slot: Int,        // 槽位索引
    val storedItem: ItemStack? // 存储的物品
) : ComponentEventContext<Event, SingleStorageSlotComponent>
```

**统一提供的信息**：
- 坐标信息：`x`, `y`, `slot`
- 存储状态：`storedItem`
- 便捷方法：`isStorageEmpty()`, `canMergeWithStored()`, `canSwapItems()` 等

### ChestPageEventContext

为箱子页面的**所有事件类型**提供统一的上下文：

```kotlin
class ChestPageEventContext(
    event: Event,         // 任何类型的事件
    player: Player,
    val page: ChestPage,
    val rows: Int,        // 行数
    val columns: Int,     // 列数
    val size: Int         // 总大小
) : ComponentEventContext<Event, ChestPage>
```

**统一提供的信息**：
- 页面尺寸：`rows`, `columns`, `size`
- 坐标转换：`slotToCoordinate()`, `coordinateToSlot()`
- 区域检查：`isClickInArea()`, `isValidCoordinate()` 等

## 💡 使用示例

### 单槽组件事件处理

```kotlin
slotComponent(x = 4, y = 2) {
    render { context ->
        itemUtil.createItemStack(Material.DIAMOND) {
            name("&b钻石 (${context.x}, ${context.y})")
        }
    }

    // 使用组件特定的事件处理器（适用于所有事件类型）
    onSlotClick { context ->
        // context 是 SlotComponentEventContext 类型
        context.player.sendMessage("&a你点击了坐标 (${context.x}, ${context.y}) 的钻石！")

        // 可以直接访问组件实例
        context.component.update()

        // 访问事件特定信息（自动根据事件类型提供）
        if (context.isLeftClick) {
            context.player.sendMessage("&e左键点击")
        }
    }

    // 也可以处理其他事件类型，会自动获得相同的组件特定信息
    onDrag { context ->
        // context 也是 SlotComponentEventContext 类型
        if (context is SlotComponentEventContext) {
            context.player.sendMessage("&c在坐标 (${context.x}, ${context.y}) 拖拽物品")
        }
    }

    onClose { context ->
        // context 也是 SlotComponentEventContext 类型
        if (context is SlotComponentEventContext) {
            context.player.sendMessage("&7关闭了包含坐标 (${context.x}, ${context.y}) 的GUI")
        }
    }
}
```

### 分页组件事件处理

```kotlin
val items = listOf("苹果", "香蕉", "橙子", "葡萄", "草莓")

paginatedComponent<String>(
    startX = 1, startY = 1,
    width = 3, height = 3,
    data = items
) {
    renderData { item, index, globalIndex ->
        itemUtil.createItemStack(Material.APPLE) {
            name("&a$item")
            lore("&7页面索引: $index", "&7全局索引: $globalIndex")
        }
    }

    // 使用分页组件特定的事件处理器（适用于所有事件类型）
    onPaginatedClick { context ->
        // context 是 PaginatedComponentEventContext<String> 类型
        val item = context.dataItem
        if (context.hasDataItem()) {
            context.player.sendMessage("&a你点击了: $item")
            context.player.sendMessage("&7页面: ${context.pageIndex}, 索引: ${context.index}")
            context.player.sendMessage("&7全局索引: ${context.globalIndex}")
            context.player.sendMessage("&7相对坐标: (${context.relativeX}, ${context.relativeY})")

            // 使用便捷方法
            if (context.isLastItemInPage()) {
                context.player.sendMessage("&e这是本页最后一个物品")
            }
        } else {
            context.player.sendMessage("&c这是一个空槽位")
        }
    }

    // 处理拖拽事件，会自动获得相同的分页信息
    onDrag { context ->
        if (context is PaginatedComponentEventContext<*>) {
            context.player.sendMessage("&c在页面 ${context.pageIndex} 拖拽物品")
        }
    }

    // 处理右键事件，可以实现特殊功能
    onRightClick { context ->
        if (context is PaginatedComponentEventContext<*>) {
            val item = context.dataItem
            if (item != null) {
                context.player.sendMessage("&b右键点击了: $item")
                // 可以实现如删除、编辑等功能
            }
        }
    }
}
```

### 存储组件事件处理

```kotlin
storageSlotComponent(x = 4, y = 2) {
    // 使用存储组件特定的事件处理器
    onStorageClick { context ->
        // context 是 StorageComponentClickContext 类型
        val storedItem = context.storedItem
        val clickedItem = context.item

        context.player.sendMessage("&a存储槽点击事件")
        context.player.sendMessage("&7当前存储: ${storedItem?.type ?: "空"}")
        context.player.sendMessage("&7点击物品: ${clickedItem?.type ?: "空"}")

        // 可以直接访问组件的存储方法
        if (context.isLeftClick && clickedItem != null) {
            if (context.component.putItem(clickedItem, context.player)) {
                context.player.sendMessage("&a物品已存储")
            } else {
                context.player.sendMessage("&c无法存储物品")
            }
        }
    }
}
```

## 🎯 最佳实践

### 1. 利用统一的组件事件上下文

```kotlin
// ✅ 推荐：使用组件特定的事件处理器，自动获得丰富信息
slotComponent(x = 1, y = 1) {
    onSlotClick { context ->
        // context 是 SlotComponentEventContext，包含所有组件信息
        val (x, y) = context.x to context.y
        context.player.sendMessage("点击了坐标 ($x, $y)")
    }

    // 所有事件类型都会获得相同的组件信息
    onDrag { context ->
        if (context is SlotComponentEventContext) {
            context.player.sendMessage("在坐标 (${context.x}, ${context.y}) 拖拽")
        }
    }
}

// ❌ 不推荐：使用通用事件处理器
slotComponent(x = 1, y = 1) {
    onClick { context ->
        // 需要手动计算坐标等信息
        val x = context.slot % 9
        val y = context.slot / 9
    }
}
```

### 2. 利用类型安全和便捷方法

```kotlin
paginatedComponent<Player>(data = onlinePlayers) {
    onPaginatedClick { context ->
        // 类型安全的数据访问
        val player = context.dataItem
        if (context.hasDataItem()) {
            // 编译时类型检查
            context.player.sendMessage("&a选择了玩家: ${player!!.name}")

            // 使用便捷方法
            if (context.isLastItemInPage()) {
                context.player.sendMessage("&e这是本页最后一个玩家")
            }
        }
    }
}
```

### 3. 统一处理多种事件类型

```kotlin
storageSlotComponent(x = 4, y = 2) {
    // 点击事件
    onStorageClick { context ->
        when {
            context.isLeftClick && context.clickedItem != null -> {
                // 尝试存储物品
                if (context.component.putItem(context.clickedItem, context.player)) {
                    context.player.sendMessage("&a物品已存储")
                }
            }
            context.isRightClick && context.hasStoredItem() -> {
                // 取出物品
                val taken = context.component.takeItem(context.player)
                if (taken != null) {
                    context.player.sendMessage("&a取出了物品")
                }
            }
        }
    }

    // 拖拽事件 - 会获得相同的存储信息
    onDrag { context ->
        if (context is StorageComponentEventContext) {
            context.player.sendMessage("&c不能拖拽到存储槽")
            context.event.isCancelled = true
        }
    }
}
```

### 4. 页面级别的事件处理

```kotlin
openPage(InventoryType.CHEST, 54, player, "示例GUI") { page ->
    // 页面级别的事件处理会自动获得ChestPageEventContext
    page.onClick { context ->
        if (context is ChestPageEventContext) {
            val (x, y) = context.getClickedCoordinate() ?: return@onClick
            context.player.sendMessage("&7点击了坐标 ($x, $y)")

            // 检查是否在特定区域
            if (context.isClickInArea(1, 1, 7, 4)) {
                context.player.sendMessage("&a点击在主要区域内")
            }
        }
    }
}
```

### 5. 事件类型检查和处理

```kotlin
slotComponent(x = 1, y = 1) {
    // 通用事件处理器，会自动获得组件特定上下文
    onClick { context ->
        when (context) {
            is SlotComponentEventContext -> {
                // 处理单槽组件的点击
                context.player.sendMessage("单槽组件点击: (${context.x}, ${context.y})")
            }
            is ClickEventContext -> {
                // 回退到基础上下文
                context.player.sendMessage("基础点击事件")
            }
        }
    }
}

## 🔄 迁移指南

### 从旧版本迁移

如果你之前使用了组件特定的点击事件处理器，迁移非常简单：

#### 旧版本（事件类型特定）
```kotlin
// 旧版本 - 只能处理点击事件
slotComponent(x = 1, y = 1) {
    onSlotClick { context ->  // SlotComponentClickContext
        context.player.sendMessage("坐标: (${context.x}, ${context.y})")
    }
}
```

#### 新版本（统一事件上下文）
```kotlin
// 新版本 - 可以处理所有事件类型
slotComponent(x = 1, y = 1) {
    onSlotClick { context ->  // SlotComponentEventContext
        context.player.sendMessage("坐标: (${context.x}, ${context.y})")
    }

    // 现在还可以处理其他事件类型
    onDrag { context ->
        if (context is SlotComponentEventContext) {
            context.player.sendMessage("拖拽到坐标: (${context.x}, ${context.y})")
        }
    }
}
```

### 向后兼容性

- **现有的DSL方法继续工作**：`onSlotClick`, `onPaginatedClick`, `onStorageClick`
- **现有的基础事件处理器继续工作**：`onClick`, `onDrag`, `onClose` 等
- **自动上下文升级**：基础事件会自动升级为组件特定上下文（如果可用）

### 新功能优势

1. **统一性**：每个组件只有一个事件上下文类
2. **完整性**：所有事件类型都能获得组件特定信息
3. **内聚性**：相关代码放在一起，更好的代码组织
4. **扩展性**：更容易添加新的组件类型

## 🔧 扩展自定义组件

如果你创建了自定义组件，可以实现组件特定的事件上下文：

```kotlin
class CustomComponent : BaseComponent(page) {

    // 重写创建事件上下文的方法
    override fun createComponentEventContext(event: Event): ComponentEventContext<*, *>? {
        return CustomComponentEventContext(
            event = event,
            player = when (event) {
                is InventoryEvent -> event.view.player as Player
                else -> return null
            },
            component = this,
            // 添加自定义信息
            customData = getCustomData(),
            customProperty = getCustomProperty()
        )
    }
}

// 在同一文件中定义组件特定的事件上下文
class CustomComponentEventContext(
    event: Event,
    player: Player,
    component: CustomComponent,
    val customData: String,
    val customProperty: Int
) : ComponentEventContext<Event, CustomComponent>(event, player, component) {

    // 添加便捷的属性和方法
    val clickType: ClickType? = (event as? InventoryClickEvent)?.click
    val isLeftClick: Boolean = clickType == ClickType.LEFT

    fun getCustomInfo(): String = "Custom: $customData ($customProperty)"
}
```

### 设计优势

- **类型安全**：编译时检查，避免运行时错误
- **统一接口**：所有事件类型使用相同的上下文
- **丰富信息**：每个组件都有适合的上下文信息
- **代码内聚**：相关代码放在一起
- **易于维护**：更简洁的API和更好的可读性
- **向后兼容**：现有代码继续工作
