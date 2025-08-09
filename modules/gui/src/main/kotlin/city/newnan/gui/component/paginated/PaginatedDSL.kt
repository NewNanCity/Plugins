package city.newnan.gui.component.paginated

import city.newnan.gui.dataprovider.*
import city.newnan.gui.page.BasePage

/**
 * 分页组件的DSL扩展
 *
 * 提供便利的API来创建支持三种执行模式的分页组件
 */

// ==================== BasePage扩展方法 ====================

/**
 * 创建同步分页组件
 *
 * 数据获取操作立即执行，适合：
 * - 访问内存中的数据
 * - 简单的计算操作
 * - 不涉及IO的快速操作
 */
fun <T> BasePage.syncPaginated(
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
): PaginatedComponent<T> {
    val dataProvider = SyncDataProvider(sizeProvider, itemProvider, hasMoreProvider, cacheStrategy)
    return PaginatedComponent(this, startX, startY, width, height, dataProvider)
}

/**
 * 创建异步分页组件
 *
 * 数据获取操作通过回调执行，适合：
 * - 文件IO操作
 * - 网络请求
 * - 数据库查询
 * - 复杂计算
 */
fun <T> BasePage.asyncPaginated(
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
): PaginatedComponent<T> {
    val dataProvider = AsyncDataProvider(sizeProvider, itemProvider, hasMoreProvider, cacheStrategy)
    return PaginatedComponent(this, startX, startY, width, height, dataProvider)
}

/**
 * 从List创建分页组件
 */
fun <T> BasePage.paginatedFromList(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    items: List<T>,
    cacheStrategy: CacheStrategy = CacheStrategy.CURRENT_PAGE_ONLY
): PaginatedComponent<T> {
    val dataProvider = ListDataProvider(items, cacheStrategy)
    return PaginatedComponent(this, startX, startY, width, height, dataProvider)
}

/**
 * 从同步函数创建分页组件
 */
fun <T> BasePage.paginatedFromFunction(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    itemsProvider: () -> List<T>,
    cacheStrategy: CacheStrategy = CacheStrategy.CURRENT_PAGE_ONLY
): PaginatedComponent<T> {
    val dataProvider = SyncDataProvider(itemsProvider)
    return PaginatedComponent(this, startX, startY, width, height, dataProvider)
}

// ==================== 便利方法 ====================

/**
 * 创建分页组件（自动选择最适合的方式）
 */
fun <T> BasePage.paginated(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    items: List<T>
): PaginatedComponent<T> {
    return paginatedFromList(startX, startY, width, height, items)
}

/**
 * 创建分页组件（自动选择最适合的方式）
 */
fun <T> BasePage.paginated(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    itemsProvider: () -> List<T>
): PaginatedComponent<T> {
    return paginatedFromFunction(startX, startY, width, height, itemsProvider)
}

/**
 * 创建分页组件（使用自定义数据提供器）
 */
fun <T> BasePage.paginated(
    startX: Int,
    startY: Int,
    width: Int,
    height: Int,
    dataProvider: IDataProvider<T>
): PaginatedComponent<T> {
    return PaginatedComponent(this, startX, startY, width, height, dataProvider)
}

// ==================== 构建器DSL ====================

/**
 * 分页组件构建器
 */
class PaginatedBuilder<T>(private val page: BasePage) {
    var startX: Int = 0
    var startY: Int = 0
    var width: Int = 9
    var height: Int = 4
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

    fun build(): PaginatedComponent<T> {
        val provider = dataProvider ?: error("Data provider is required")
        return PaginatedComponent(page, startX, startY, width, height, provider)
    }
}

/**
 * DSL构建器函数
 */
inline fun <T> BasePage.paginated(block: PaginatedBuilder<T>.() -> Unit): PaginatedComponent<T> {
    return PaginatedBuilder<T>(this).apply(block).build()
}

// ==================== 状态检查扩展 ====================

/**
 * 检查分页组件是否准备就绪
 */
fun <T> PaginatedComponent<T>.isReady(): Boolean {
    return getComponentState() == PaginatedComponent.ComponentState.READY
}

/**
 * 检查分页组件是否正在加载
 */
fun <T> PaginatedComponent<T>.isLoading(): Boolean {
    return getComponentState() == PaginatedComponent.ComponentState.LOADING
}

/**
 * 检查分页组件是否有错误
 */
fun <T> PaginatedComponent<T>.hasError(): Boolean {
    return getComponentState() == PaginatedComponent.ComponentState.ERROR
}

/**
 * 检查当前页是否准备就绪
 */
fun <T> PaginatedComponent<T>.isCurrentPageReady(): Boolean {
    return getPageState(getCurrentPage()) == PageState.LOADED
}

// ==================== 缓存管理扩展 ====================

/**
 * 检查是否启用了缓存
 */
fun <T> PaginatedComponent<T>.isCacheEnabled(): Boolean {
    return getCacheStrategy() != CacheStrategy.NO_CACHE
}

/**
 * 检查是否使用多页缓存
 */
fun <T> PaginatedComponent<T>.isMultiPageCache(): Boolean {
    val strategy = getCacheStrategy()
    return strategy == CacheStrategy.MULTI_PAGE || strategy == CacheStrategy.AGGRESSIVE
}