# 基于特征的事件上下文系统

GUI1模块的全新事件处理架构，实现了 `EventHandler<T>` 和 `EventContext<T, K>` 的组合设计，其中：
- `T` 与 Component 相关（组件特征）
- `K` 与具体事件相关（事件类型）

## 🎯 设计理念

### 核心概念

1. **组件特征（ComponentFeature）**：组件通过实现特征接口来声明自己的能力
2. **特征注入（Feature Injection）**：组件在事件发生时将特征信息注入到EventContext中
3. **类型安全组合（Type-Safe Composition）**：事件类型 × 组件特征的类型安全组合
4. **动态特征查询（Dynamic Feature Query）**：运行时查询和使用组件特征

### 架构优势

- **极高的灵活性**：可以任意组合事件类型和组件特征
- **类型安全**：编译时检查，避免运行时错误
- **可扩展性**：新的组件特征可以轻松添加
- **解耦合**：事件处理逻辑与具体组件实现解耦

## 🏗️ 架构设计

### 特征接口层次

```kotlin
ComponentFeature (基础特征接口)
    ↓
ClickFeature        // 可点击特征
PaginationFeature<T> // 分页特征
StorageFeature      // 存储特征
AreaFeature         // 区域特征
PageFeature         // 页面特征
```

### 事件上下文组合

```kotlin
AdvancedEventContext<T : ComponentFeature, K : Event>
    ↓
ClickEventContext<T>    // 点击事件 × 组件特征
DragEventContext<T>     // 拖拽事件 × 组件特征
CloseEventContext<T>    // 关闭事件 × 组件特征
```

### 事件处理器类型

```kotlin
FeatureBasedEventHandler<T : ComponentFeature>
    ↓
ClickableEventHandler           // 处理可点击特征
PaginatedEventHandler<T>        // 处理分页特征
StorageEventHandler            // 处理存储特征
MultiFeatureEventHandler       // 处理多特征组合
```

## 💡 使用示例

### 1. 单槽组件的特征事件处理

```kotlin
slotComponent(x = 4, y = 2) {
    render { context ->
        itemUtil.createItemStack(Material.DIAMOND) {
            name("&b钻石 (${context.x}, ${context.y})")
        }
    }

    // 使用基于特征的事件处理器
    onClickableClick { context ->
        // context 是 ClickEventContext<ClickFeature> 类型
        val feature = context.getComponentFeature()
        context.player.sendMessage("&a点击了坐标 (${feature.x}, ${feature.y}) 的钻石！")
        
        // 访问事件特定信息
        if (context.isLeftClick) {
            context.player.sendMessage("&e左键点击")
        }
        
        // 访问组件实例
        context.component.update()
    }

    // 处理不同的点击类型
    onClickableLeftClick { context ->
        context.player.sendMessage("&a左键点击处理")
    }

    onClickableRightClick { context ->
        context.player.sendMessage("&b右键点击处理")
    }

    onClickableDrag { context ->
        // context 是 DragEventContext<ClickFeature> 类型
        val feature = context.getComponentFeature()
        context.player.sendMessage("&c在坐标 (${feature.x}, ${feature.y}) 拖拽物品")
    }
}
```

### 2. 分页组件的多特征事件处理

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
            lore("&7页面索引: $index", "&7全局索引: $globalIndex")
        }
    }

    // 使用分页特征事件处理器
    onPaginatedClick { context ->
        // context 是 ClickEventContext<PaginationFeature<Player>> 类型
        val feature = context.getComponentFeature()
        val player = feature.dataItem
        
        if (feature.hasDataItem()) {
            context.player.sendMessage("&a选择了玩家: ${player!!.name}")
            context.player.sendMessage("&7页面: ${feature.pageIndex}, 索引: ${feature.index}")
            context.player.sendMessage("&7全局索引: ${feature.globalIndex}")
            
            // 使用便捷方法
            if (feature.isLastItemInPage()) {
                context.player.sendMessage("&e这是本页最后一个玩家")
            }
        } else {
            context.player.sendMessage("&c这是一个空槽位")
        }
    }

    // 使用区域特征事件处理器
    onAreaClick { context ->
        // context 是 ClickEventContext<AreaFeature> 类型
        val feature = context.getComponentFeature()
        val (relativeX, relativeY) = feature.toRelativeCoordinate(
            context.slot % 9, context.slot / 9
        ) ?: return@onAreaClick
        
        context.player.sendMessage("&7点击了区域内的相对坐标 ($relativeX, $relativeY)")
    }

    // 便捷的数据项事件处理
    onDataItemClick { context, player, index ->
        context.player.sendMessage("&a点击了第 $index 个玩家: ${player.name}")
        // 可以直接操作玩家对象
        player.sendMessage("&b你被 ${context.player.name} 选择了！")
    }

    // 便捷的空槽位事件处理
    onEmptySlotClick { context, index ->
        context.player.sendMessage("&c点击了第 $index 个空槽位")
    }
}
```

### 3. 存储组件的特征事件处理

```kotlin
storageSlotComponent(x = 4, y = 2) {
    // 使用存储特征事件处理器
    onStorageClick { context ->
        // context 是 ClickEventContext<StorageFeature> 类型
        val feature = context.getComponentFeature()
        val clickedItem = context.clickedItem
        val storedItem = feature.storedItem
        
        context.player.sendMessage("&a存储槽点击事件")
        context.player.sendMessage("&7当前存储: ${storedItem?.type ?: "空"}")
        context.player.sendMessage("&7点击物品: ${clickedItem?.type ?: "空"}")
        
        // 使用特征的便捷方法
        if (feature.canMergeWith(clickedItem)) {
            context.player.sendMessage("&a可以合并物品")
        }
    }

    // 便捷的存储操作事件处理
    onStorageOperation(
        onPut = { context, item ->
            context.player.sendMessage("&a放入了物品: ${item.type}")
            // 执行实际的存储逻辑
            context.component.putItem(item, context.player)
        },
        onTake = { context, storedItem ->
            context.player.sendMessage("&b取出了物品: ${storedItem.type}")
            // 执行实际的取出逻辑
            context.component.takeItem(context.player)
        },
        onSwap = { context, clickedItem, storedItem ->
            context.player.sendMessage("&c交换物品: ${clickedItem.type} <-> ${storedItem?.type}")
            // 执行实际的交换逻辑
        }
    )
}
```

### 4. 多特征组件的事件处理

```kotlin
// 假设有一个组件同时实现了多个特征
class AdvancedSlotComponent : BaseComponent, ClickableComponent, StorageComponent, FeatureProvider {
    
    override fun getFeatures(): Set<ComponentFeature> {
        return setOf(
            getClickFeature(),
            getStorageFeature()
        )
    }
    
    // 在DSL中使用
    fun configure() {
        // 可以同时处理多个特征的事件
        onClickableClick { context ->
            // 处理可点击特征
        }
        
        onStorageClick { context ->
            // 处理存储特征
        }
        
        // 或者使用多特征事件处理
        onMultiFeatureEvent { context ->
            // context 是 MultiFeatureEventContext 类型
            
            // 动态查询特征
            val clickFeature = context.getFeature<ClickFeature>()
            val storageFeature = context.getFeature<StorageFeature>()
            
            if (clickFeature != null && storageFeature != null) {
                // 同时处理两种特征
                context.player.sendMessage("点击了坐标 (${clickFeature.x}, ${clickFeature.y}) 的存储槽")
                if (storageFeature.hasItem()) {
                    context.player.sendMessage("存储槽中有物品: ${storageFeature.storedItem?.type}")
                }
            }
        }
    }
}
```

### 5. 类型安全的事件处理器配置

```kotlin
slotComponent(x = 1, y = 1) {
    // 使用构建器模式配置事件处理器
    configureFeatureEvents<ClickFeature> {
        onClick { context ->
            // 类型安全的点击处理
            val feature = context.getComponentFeature()
            context.player.sendMessage("点击了 (${feature.x}, ${feature.y})")
        }
        
        onLeftClick { context ->
            // 类型安全的左键处理
        }
        
        onDrag { context ->
            // 类型安全的拖拽处理
        }
        
        onClose { context ->
            // 类型安全的关闭处理
        }
    }
}
```

## 🔧 扩展自定义特征

### 创建自定义特征

```kotlin
// 定义自定义特征
data class CustomFeature(
    val customProperty: String,
    val customValue: Int
) : ComponentFeature

// 定义特征接口
interface CustomComponent {
    fun getCustomFeature(): CustomFeature
}

// 创建自定义事件处理器
class CustomEventHandler : FeatureBasedEventHandler<CustomFeature>()

// 定义类型别名
typealias CustomClickHandler = (ClickEventContext<CustomFeature>) -> Unit
```

### 实现自定义组件

```kotlin
class MyCustomComponent : BaseComponent, CustomComponent, FeatureProvider {
    
    override fun getCustomFeature(): CustomFeature {
        return CustomFeature("example", 42)
    }
    
    override fun getFeatures(): Set<ComponentFeature> {
        return setOf(getCustomFeature())
    }
}
```

### 使用自定义特征

```kotlin
myCustomComponent {
    // 使用自定义特征事件处理器
    configureFeatureEvents<CustomFeature> {
        onClick { context ->
            val feature = context.getComponentFeature()
            context.player.sendMessage("自定义属性: ${feature.customProperty}")
            context.player.sendMessage("自定义值: ${feature.customValue}")
        }
    }
}
```

## 🎯 最佳实践

### 1. 特征组合原则

- **单一职责**：每个特征只负责一种能力
- **组合优于继承**：通过组合多个特征来实现复杂功能
- **类型安全**：充分利用泛型系统保证类型安全

### 2. 性能优化

- **特征缓存**：对于不变的特征信息进行缓存
- **延迟计算**：只在需要时计算特征信息
- **批量处理**：对于多特征组件，批量处理相关事件

### 3. 错误处理

- **特征检查**：在使用特征前检查是否存在
- **类型转换**：安全的类型转换和检查
- **异常处理**：妥善处理特征获取失败的情况

这个新的架构提供了前所未有的灵活性和类型安全性，让事件处理变得更加强大和易于扩展！🚀
