# 灵活可滚动组件架构

## 概述

新的可滚动组件架构支持三种数据获取模式，让调用者可以根据具体需求选择最适合的执行方式：

1. **同步模式** - 数据立即获取，适合内存操作
2. **异步模式** - 数据通过回调获取，适合IO操作
3. **协程模式** - 使用现有的DataProvider接口（向后兼容）

## 核心特性

### 状态管理

组件提供了完善的状态管理机制：

- **组件状态**: `INIT` → `LOADING` → `READY` / `ERROR`
- **行状态**: `NOT_LOADED` → `LOADING` → `LOADED` / `ERROR`

### 异步回调处理

由于异步操作可能存在加载空隙，组件妥善管理了各种状态：

- 加载中状态的正确显示
- 错误状态的处理
- 缓存状态的管理
- 并发安全的状态更新

### 智能滚动控制

- 垂直滚动支持
- 滚动边界检查
- 无限滚动模式
- 可见区域管理

## 数据提供器架构

### IDataProvider 接口

```kotlin
interface IDataProvider<T> {
    fun getSize(callback: DataProviderCallback<Int>)
    fun fetchItems(offset: Int, limit: Int, callback: DataProviderCallback<List<T>>)
    fun canFetchMore(offset: Int, callback: DataProviderCallback<Boolean>)
    fun getCacheStrategy(): CacheStrategy
}
```

### 具体实现

1. **SyncDataProvider** - 同步数据提供器
2. **AsyncDataProvider** - 异步数据提供器
3. **ListDataProvider** - 基于List的数据提供器

## 使用方式

### 1. 简单List可滚动列表

```kotlin
val component = scrollable(1, 1, 7, 6, listOf("Item1", "Item2", "Item3"))
```

### 2. 同步数据可滚动列表

```kotlin
val component = syncScrollable(
    startX = 1, startY = 1, width = 7, height = 6,
    sizeProvider = { 1000 },
    itemProvider = { offset, limit ->
        (offset until (offset + limit).coerceAtMost(1000)).map { "Item $it" }
    }
)
```

### 3. 异步数据可滚动列表

```kotlin
val component = asyncScrollable<String>(
    startX = 1, startY = 1, width = 7, height = 6,
    sizeProvider = { callback ->
        runAsync {
            // 模拟数据库查询
            val count = database.getCount()
            callback(Result.success(count))
        }
    },
    itemProvider = { offset, limit, callback ->
        runAsync {
            // 模拟数据库查询
            val items = database.getItems(offset, limit)
            callback(Result.success(items))
        }
    }
)
```

### 4. 构建器DSL

```kotlin
val component = scrollable<String> {
    startX = 1
    startY = 1
    width = 7
    height = 6
    cacheStrategy = CacheStrategy.MULTI_PAGE
    
    asyncData(
        sizeProvider = { callback -> /* 异步获取总数 */ },
        itemProvider = { offset, limit, callback -> /* 异步获取数据 */ }
    )
}
```

## 渲染配置

### 基本渲染

```kotlin
component.render { context ->
    ItemStack(Material.PAPER).apply {
        itemMeta = itemMeta?.apply {
            setDisplayName("§a${context.item}")
            lore = listOf(
                "§7行: ${context.dataRow}",
                "§7状态: ${context.rowState}"
            )
        }
    }
}
```

### 状态感知渲染

```kotlin
component.render { context ->
    val material = when {
        context.componentState == ComponentState.ERROR -> Material.BARRIER
        context.rowState == RowState.ERROR -> Material.REDSTONE_BLOCK
        context.isLoading -> Material.YELLOW_STAINED_GLASS
        else -> Material.EMERALD
    }
    
    ItemStack(material).apply {
        itemMeta = itemMeta?.apply {
            setDisplayName("§a${context.item}")
            lore = buildList {
                add("§7组件状态: ${context.componentState}")
                add("§7行状态: ${context.rowState}")
                if (context.isLoading) add("§e正在加载...")
            }
        }
    }
}
```

### 加载中和空槽位渲染

```kotlin
component.renderLoadingSlot {
    ItemStack(Material.CLOCK).apply {
        itemMeta = itemMeta?.apply {
            setDisplayName("§e加载中...")
        }
    }
}

component.renderEmptySlot {
    ItemStack(Material.GRAY_STAINED_GLASS_PANE)
}
```

## 滚动控制

### 基本滚动操作

```kotlin
// 向上滚动
component.scrollUp(lines = 1)

// 向下滚动
component.scrollDown(lines = 1)

// 滚动到指定位置
component.scrollTo(offset = 10)

// 滚动到顶部
component.scrollToTop()

// 滚动到底部
component.scrollToBottom()
```

### 滚动状态检查

```kotlin
if (component.canScrollUp()) {
    // 可以向上滚动
}

if (component.canScrollDown()) {
    // 可以向下滚动
}

// 异步检查
component.canScrollDownAsync { canScroll ->
    if (canScroll) {
        // 可以向下滚动
    }
}
```

### 滚动事件监听

```kotlin
component.scrollChangeHandler = { context ->
    println("滚动从 ${context.previousOffset} 到 ${context.currentOffset}")
    println("可见范围: ${context.visibleStartIndex} - ${context.visibleEndIndex}")
}
```

## 缓存策略详解

### 缓存策略类型

#### 1. NO_CACHE - 无缓存
- **适用场景**: 实时数据、频繁变化的数据
- **行为**: 每次都重新获取数据，不保存任何缓存
- **最大缓存大小**: 0

#### 2. CURRENT_PAGE_ONLY - 可见区域缓存
- **适用场景**: 大数据集、内存受限环境
- **行为**: 只缓存当前可见区域的行
- **最大缓存大小**: 可见行数

#### 3. MULTI_PAGE - 多行缓存
- **适用场景**: 中等数据集、用户经常滚动
- **行为**: 缓存多行，使用LRU算法管理缓存
- **预加载**: 自动预加载邻近2行
- **最大缓存大小**: 可见行数 × 3

#### 4. AGGRESSIVE - 激进缓存
- **适用场景**: 小数据集、性能要求高的场景
- **行为**: 缓存大量行，积极预加载
- **预加载**: 自动预加载邻近5行
- **最大缓存大小**: 可见行数 × 10

## 状态检查

### 组件状态

```kotlin
if (component.isReady()) {
    // 组件已准备就绪
}

if (component.isLoading()) {
    // 组件正在加载
}

if (component.hasError()) {
    // 组件有错误
}
```

### 行状态

```kotlin
if (component.isVisibleAreaReady()) {
    // 当前可见区域已加载完成
}

if (component.isRowLoading(rowIndex)) {
    // 指定行正在加载
}

val rowState = component.getRowState(rowIndex)
```

### 缓存监控

```kotlin
// 检查缓存状态
if (component.isCacheEnabled()) {
    println("缓存已启用")
}

// 获取缓存效率描述
val efficiency = component.getCacheEfficiencyDescription()
println("缓存效率: $efficiency")

// 打印详细缓存统计
component.printCacheStats()
```

## 最佳实践

### 1. 选择合适的执行模式
- **内存数据** → 同步模式
- **IO操作** → 异步模式
- **复杂异步流程** → 协程模式

### 2. 选择合适的缓存策略
- **实时数据** → `NO_CACHE`
- **大数据集** → `CURRENT_PAGE_ONLY`
- **中等数据集** → `MULTI_PAGE`
- **小数据集** → `AGGRESSIVE`

### 3. 状态管理
- 始终检查组件和行状态
- 为不同状态提供合适的视觉反馈
- 处理错误状态

### 4. 性能优化
- 根据数据特性选择缓存策略
- 监控缓存命中率，调整策略
- 避免不必要的数据重新加载
- 使用预加载提升用户体验

### 5. 用户体验
- 提供加载中的视觉反馈
- 处理错误状态的显示
- 合理的可见区域大小设置
- 根据用户行为调整预加载策略

## 向后兼容性

新架构完全向后兼容，现有的使用方式仍然有效：

```kotlin
// 旧的使用方式仍然有效
val component = ScrollableComponent(page, 1, 1, 7, 6, listOf("Item1", "Item2"))
```

## 注意事项

1. 异步回调可能存在竞态条件，组件内部已做好并发安全处理
2. 状态变更会自动触发界面更新
3. 组件关闭时会自动清理所有资源
4. 错误处理已内置，但建议在数据提供器中也做好错误处理
5. 无限滚动模式需要正确实现 `canFetchMore` 方法
