package city.newnan.gui.component.scrollable

import city.newnan.gui.dataprovider.*
import city.newnan.gui.page.BasePage

/**
 * 可滚动组件的DSL扩展
 *
 * 提供便利的API来创建支持三种执行模式的可滚动组件
 */

// ==================== BasePage扩展方法 ====================

/**
 * 创建同步可滚动组件
 *
 * 数据获取操作立即执行，适合：
 * - 访问内存中的数据
 * - 简单的计算操作
 * - 不涉及IO的快速操作
 */
fun <T> BasePage.syncScrollable(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    sizeProvider: () -> Int,
    itemProvider: (offset: Int, limit: Int) -> List<T>,
    hasMoreProvider: (offset: Int) -> Boolean = { offset ->
        val size = sizeProvider()
        if (size < 0) true else offset < size
    },
    cacheStrategy: CacheStrategy = CacheStrategy.CURRENT_PAGE_ONLY
): ScrollableComponent<T> {
    val dataProvider = SyncDataProvider(sizeProvider, itemProvider, hasMoreProvider, cacheStrategy)
    return ScrollableComponent(this, startX, startY, width, height, dataProvider)
}

/**
 * 创建异步可滚动组件
 *
 * 数据获取操作通过回调执行，适合：
 * - 文件IO操作
 * - 网络请求
 * - 数据库查询
 * - 复杂计算
 */
fun <T> BasePage.asyncScrollable(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    sizeProvider: (callback: DataProviderCallback<Int>) -> Unit,
    itemProvider: (offset: Int, limit: Int, callback: DataProviderCallback<List<T>>) -> Unit,
    hasMoreProvider: (offset: Int, callback: DataProviderCallback<Boolean>) -> Unit = { offset, callback ->
        sizeProvider { sizeResult ->
            sizeResult.fold(
                onSuccess = { totalSize ->
                    val canFetch = if (totalSize < 0) true else offset < totalSize
                    callback(Result.success(canFetch))
                },
                onFailure = { callback(Result.success(false)) }
            )
        }
    },
    cacheStrategy: CacheStrategy = CacheStrategy.CURRENT_PAGE_ONLY
): ScrollableComponent<T> {
    val dataProvider = AsyncDataProvider(sizeProvider, itemProvider, hasMoreProvider, cacheStrategy)
    return ScrollableComponent(this, startX, startY, width, height, dataProvider)
}

/**
 * 从List创建可滚动组件
 */
fun <T> BasePage.scrollableFromList(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    items: List<T>,
    cacheStrategy: CacheStrategy = CacheStrategy.CURRENT_PAGE_ONLY
): ScrollableComponent<T> {
    val dataProvider = ListDataProvider(items, cacheStrategy)
    return ScrollableComponent(this, startX, startY, width, height, dataProvider)
}

/**
 * 从同步函数创建可滚动组件
 */
fun <T> BasePage.scrollableFromFunction(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    itemsProvider: () -> List<T>,
    cacheStrategy: CacheStrategy = CacheStrategy.CURRENT_PAGE_ONLY
): ScrollableComponent<T> {
    val dataProvider = SyncDataProvider(itemsProvider)
    return ScrollableComponent(this, startX, startY, width, height, dataProvider)
}

// ==================== 便利方法 ====================

/**
 * 创建可滚动组件（自动选择最适合的方式）
 */
fun <T> BasePage.scrollable(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    items: List<T>
): ScrollableComponent<T> {
    return scrollableFromList(startX, startY, width, height, items)
}

/**
 * 创建可滚动组件（自动选择最适合的方式）
 */
fun <T> BasePage.scrollable(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    itemsProvider: () -> List<T>
): ScrollableComponent<T> {
    return scrollableFromFunction(startX, startY, width, height, itemsProvider)
}

/**
 * 创建可滚动组件（使用自定义数据提供器）
 */
fun <T> BasePage.scrollable(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    dataProvider: IDataProvider<T>
): ScrollableComponent<T> {
    return ScrollableComponent(this, startX, startY, width, height, dataProvider)
}

// ==================== 构建器DSL ====================

/**
 * 可滚动组件构建器
 */
class ScrollableBuilder<T>(private val page: BasePage) {
    var startX: Int = 0
    var startY: Int = 0
    var width: Int = 9
    var height: Int = 6
    var cacheStrategy: CacheStrategy = CacheStrategy.CURRENT_PAGE_ONLY

    private var dataProvider: IDataProvider<T>? = null

    /**
     * 使用List数据
     */
    fun data(items: List<T>) {
        dataProvider = ListDataProvider(items, cacheStrategy)
    }

    /**
     * 使用同步函数数据
     */
    fun data(itemsProvider: () -> List<T>) {
        dataProvider = SyncDataProvider(itemsProvider)
    }

    /**
     * 使用同步分页数据
     */
    fun syncData(
        sizeProvider: () -> Int,
        itemProvider: (offset: Int, limit: Int) -> List<T>,
        hasMoreProvider: (offset: Int) -> Boolean = { offset ->
            val size = sizeProvider()
            if (size < 0) true else offset < size
        }
    ) {
        dataProvider = SyncDataProvider(sizeProvider, itemProvider, hasMoreProvider, cacheStrategy)
    }

    /**
     * 使用异步分页数据
     */
    fun asyncData(
        sizeProvider: (callback: DataProviderCallback<Int>) -> Unit,
        itemProvider: (offset: Int, limit: Int, callback: DataProviderCallback<List<T>>) -> Unit,
        hasMoreProvider: (offset: Int, callback: DataProviderCallback<Boolean>) -> Unit = { offset, callback ->
            sizeProvider { sizeResult ->
                sizeResult.fold(
                    onSuccess = { totalSize ->
                        val canFetch = if (totalSize < 0) true else offset < totalSize
                        callback(Result.success(canFetch))
                    },
                    onFailure = { callback(Result.success(false)) }
                )
            }
        }
    ) {
        dataProvider = AsyncDataProvider(sizeProvider, itemProvider, hasMoreProvider, cacheStrategy)
    }

    /**
     * 使用自定义数据提供器
     */
    fun dataProvider(provider: IDataProvider<T>) {
        dataProvider = provider
    }

    fun build(): ScrollableComponent<T> {
        val provider = dataProvider ?: error("Data provider is required")
        return ScrollableComponent(page, startX, startY, width, height, provider)
    }
}

/**
 * DSL构建器函数
 */
inline fun <T> BasePage.scrollable(block: ScrollableBuilder<T>.() -> Unit): ScrollableComponent<T> {
    return ScrollableBuilder<T>(this).apply(block).build()
}

// ==================== 状态检查扩展 ====================

/**
 * 检查可滚动组件是否准备就绪
 */
fun <T> ScrollableComponent<T>.isReady(): Boolean {
    return getComponentState() == ScrollableComponent.ComponentState.READY
}

/**
 * 检查可滚动组件是否正在加载
 */
fun <T> ScrollableComponent<T>.isLoading(): Boolean {
    return getComponentState() == ScrollableComponent.ComponentState.LOADING
}

/**
 * 检查可滚动组件是否有错误
 */
fun <T> ScrollableComponent<T>.hasError(): Boolean {
    return getComponentState() == ScrollableComponent.ComponentState.ERROR
}

/**
 * 检查当前可见区域是否准备就绪
 */
fun <T> ScrollableComponent<T>.isVisibleAreaReady(): Boolean {
    val currentOffset = getScrollOffset()
    for (row in currentOffset until currentOffset + visibleRows) {
        if (getRowState(row) != RowState.LOADED) {
            return false
        }
    }
    return true
}

// ==================== 缓存管理扩展 ====================

/**
 * 检查是否启用了缓存
 */
fun <T> ScrollableComponent<T>.isCacheEnabled(): Boolean {
    return getCacheStrategy() != CacheStrategy.NO_CACHE
}

/**
 * 检查是否使用多行缓存
 */
fun <T> ScrollableComponent<T>.isMultiRowCache(): Boolean {
    val strategy = getCacheStrategy()
    return strategy == CacheStrategy.MULTI_PAGE || strategy == CacheStrategy.AGGRESSIVE
}
