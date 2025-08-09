# 灵活分页组件架构

## 概述

新的分页组件架构支持三种数据获取模式，让调用者可以根据具体需求选择最适合的执行方式：

1. **同步模式** - 数据立即获取，适合内存操作
2. **异步模式** - 数据通过回调获取，适合IO操作
3. **协程模式** - 使用现有的DataProvider接口（向后兼容）

## 核心特性

### 状态管理

组件提供了完善的状态管理机制：

- **组件状态**: `INIT` → `LOADING` → `READY` / `ERROR`
- **页面状态**: `NOT_LOADED` → `LOADING` → `LOADED` / `ERROR`

### 异步回调处理

由于异步操作可能存在加载空隙，组件妥善管理了各种状态：

- 加载中状态的正确显示
- 错误状态的处理
- 缓存状态的管理
- 并发安全的状态更新

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

### 1. 简单List分页

```kotlin
val component = paginated(1, 1, 7, 4, listOf("Item1", "Item2", "Item3"))
```

### 2. 同步数据分页

```kotlin
val component = syncPaginated(
    startX = 1, startY = 1, width = 7, height = 4,
    sizeProvider = { 100 },
    itemProvider = { offset, limit ->
        (offset until (offset + limit).coerceAtMost(100)).map { "Item $it" }
    }
)
```

### 3. 异步数据分页

```kotlin
val component = asyncPaginated<String>(
    startX = 1, startY = 1, width = 7, height = 4,
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
val component = paginated<String> {
    startX = 1
    startY = 1
    width = 7
    height = 4
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
                "§7页面: ${context.pageIndex + 1}",
                "§7状态: ${context.pageState}"
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
        context.pageState == PageState.ERROR -> Material.REDSTONE_BLOCK
        context.isLoading -> Material.YELLOW_STAINED_GLASS
        else -> Material.EMERALD
    }

    ItemStack(material).apply {
        itemMeta = itemMeta?.apply {
            setDisplayName("§a${context.item}")
            lore = buildList {
                add("§7组件状态: ${context.componentState}")
                add("§7页面状态: ${context.pageState}")
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

### 页面状态

```kotlin
if (component.isCurrentPageReady()) {
    // 当前页已加载完成
}

if (component.isPageLoading(pageIndex)) {
    // 指定页面正在加载
}

val pageState = component.getPageState(pageIndex)
```

### 异步状态检查

```kotlin
component.hasNextPageAsync { hasNext ->
    if (hasNext) {
        // 还有下一页
    }
}
```

## 缓存策略详解

### 缓存策略类型

#### 1. NO_CACHE - 无缓存
- **适用场景**: 实时数据、频繁变化的数据
- **行为**: 每次都重新获取数据，不保存任何缓存
- **优点**: 数据始终最新
- **缺点**: 性能开销大，网络请求频繁
- **最大缓存大小**: 0

```kotlin
val component = syncPaginated(
    // ... 其他参数
    cacheStrategy = CacheStrategy.NO_CACHE
)
```

#### 2. CURRENT_PAGE_ONLY - 单页缓存
- **适用场景**: 大数据集、内存受限环境
- **行为**: 只缓存当前页，切换页面时清除旧缓存
- **优点**: 内存占用最小，避免重复加载当前页
- **缺点**: 页面切换时需要重新加载
- **最大缓存大小**: 1

```kotlin
val component = asyncPaginated(
    // ... 其他参数
    cacheStrategy = CacheStrategy.CURRENT_PAGE_ONLY
)
```

#### 3. MULTI_PAGE - 多页缓存
- **适用场景**: 中等数据集、用户经常在邻近页面间切换
- **行为**: 缓存多个页面，使用LRU算法管理缓存
- **预加载**: 自动预加载前后各1页
- **优点**: 页面切换流畅，预加载提升体验
- **缺点**: 内存占用适中
- **最大缓存大小**: 5

```kotlin
val component = paginatedFromList(
    // ... 其他参数
    cacheStrategy = CacheStrategy.MULTI_PAGE
)
```

#### 4. AGGRESSIVE - 激进缓存
- **适用场景**: 小数据集、性能要求高的场景
- **行为**: 缓存大量页面，积极预加载
- **预加载**: 自动预加载前后各2页
- **优点**: 最佳的用户体验，几乎无等待时间
- **缺点**: 内存占用较大
- **最大缓存大小**: 20

```kotlin
val component = paginatedFromFunction(
    // ... 其他参数
    cacheStrategy = CacheStrategy.AGGRESSIVE
)
```

### 缓存管理机制

#### LRU缓存算法
组件使用LRU（Least Recently Used）算法管理缓存：
- 访问页面时更新访问顺序
- 缓存满时移除最久未访问的页面
- 当前页面永远不会被移除

#### 自动预加载
根据缓存策略自动预加载邻近页面：
- `MULTI_PAGE`: 预加载前后各1页
- `AGGRESSIVE`: 预加载前后各2页
- 其他策略: 不预加载

#### 缓存清理
- 页面切换时自动清理过期缓存
- 数据源变更时清空所有缓存
- 支持手动强制清理缓存

## 缓存监控和管理

### 缓存统计信息

```kotlin
val stats = component.getCacheStats()
println("缓存策略: ${stats.strategy}")
println("已缓存页数: ${stats.cachedPages}/${stats.maxCacheSize}")
println("缓存命中率: ${"%.2f".format(stats.cacheHitRate * 100)}%")
```

### 缓存管理方法

```kotlin
// 检查缓存状态
if (component.isCacheEnabled()) {
    println("缓存已启用")
}

// 获取缓存效率描述
val efficiency = component.getCacheEfficiencyDescription()
println("缓存效率: $efficiency")

// 强制清理缓存
component.forceClearCache(keepCurrentPage = true)

// 刷新指定页面
component.refreshPage(pageIndex)

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
- 始终检查组件和页面状态
- 为不同状态提供合适的视觉反馈
- 处理错误状态

### 4. 性能优化
- 根据数据特性选择缓存策略
- 监控缓存命中率，调整策略
- 避免不必要的数据重新加载
- 使用预加载提升用户体验

### 5. 内存管理
- 定期检查缓存使用情况
- 在内存受限环境中使用保守的缓存策略
- 及时清理不需要的缓存

### 6. 用户体验
- 提供加载中的视觉反馈
- 处理错误状态的显示
- 合理的分页大小设置
- 根据用户行为调整预加载策略

## 向后兼容性

新架构完全向后兼容，现有的使用方式仍然有效：

```kotlin
// 旧的使用方式仍然有效
val component = PaginatedComponent(page, 1, 1, 7, 4, listOf("Item1", "Item2"))
```

## 注意事项

1. 异步回调可能存在竞态条件，组件内部已做好并发安全处理
2. 状态变更会自动触发界面更新
3. 组件关闭时会自动清理所有资源
4. 错误处理已内置，但建议在数据提供器中也做好错误处理
