package city.newnan.gui.dataprovider

/**
 * 基于同步方法的数据提供器
 *
 * 将数据提供函数包装为异步DataProvider
 */
open class SyncDataProvider<T>(
    private val sizeProvider: () -> Int,
    private val itemProvider: (offset: Int, limit: Int) -> List<T>,
    private val hasMoreProvider: (offset: Int) -> Boolean = { offset ->
        val size = sizeProvider()
        if (size == -1) true else offset < size
    },
    private val cacheStrategy: CacheStrategy = CacheStrategy.CURRENT_PAGE_ONLY
) : IDataProvider<T> {
    // 便利构造函数：从同步List提供函数构造
    constructor(itemsProvider: () -> List<T>) : this(
        sizeProvider = { itemsProvider().size },
        itemProvider = { offset, limit ->
            val items = itemsProvider()
            val startIndex = offset.coerceAtLeast(0)
            val endIndex = (offset + limit).coerceAtMost(items.size)
            if (startIndex < items.size) {
                items.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
        },
        hasMoreProvider = { offset ->
            val items = itemsProvider()
            offset < items.size
        }
    )

    override fun getSize(callback: DataProviderCallback<Int>) {
        callback(Result.success(sizeProvider()))
    }

    override fun fetchItems(
        offset: Int,
        limit: Int,
        callback: DataProviderCallback<List<T>>
    ) {
        callback(Result.success(itemProvider(offset, limit)))
    }

    override fun canFetchMore(
        offset: Int,
        callback: DataProviderCallback<Boolean>
    ) {
        callback(Result.success(hasMoreProvider(offset)))
    }

    override fun getCacheStrategy(): CacheStrategy {
        return cacheStrategy
    }
}
