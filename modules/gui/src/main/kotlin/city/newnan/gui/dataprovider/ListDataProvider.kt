package city.newnan.gui.dataprovider

/**
 * 基于列表的数据提供器
 *
 * 将现有的List包装为异步DataProvider
 */
class ListDataProvider<T>(
    private val items: List<T>,
    private val cacheStrategy: CacheStrategy = CacheStrategy.CURRENT_PAGE_ONLY
) : IDataProvider<T> {
    override fun getSize(callback: DataProviderCallback<Int>) {
        callback(Result.success(items.size))
    }

    override fun fetchItems(
        offset: Int,
        limit: Int,
        callback: DataProviderCallback<List<T>>
    ) {
        val startIndex = offset.coerceAtLeast(0)
        val endIndex = (offset + limit).coerceAtMost(items.size)
        val result = if (startIndex < items.size) {
            items.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
        callback(Result.success(result))
    }

    override fun canFetchMore(
        offset: Int,
        callback: DataProviderCallback<Boolean>
    ) {
        callback(Result.success(offset < items.size))
    }

    override fun getCacheStrategy(): CacheStrategy = cacheStrategy
}