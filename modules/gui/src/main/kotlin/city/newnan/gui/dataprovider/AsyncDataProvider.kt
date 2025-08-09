package city.newnan.gui.dataprovider

/**
 * 基于异步方法的数据提供器
 *
 * 将数据提供函数包装为异步DataProvider
 */
open class AsyncDataProvider<T>(
    private val sizeProvider: (callback: DataProviderCallback<Int>) -> Unit,
    private val itemProvider: (offset: Int, limit: Int, callback: DataProviderCallback<List<T>>) -> Unit,
    private val hasMoreProvider: (offset: Int, callback: DataProviderCallback<Boolean>) -> Unit = { offset, callback ->
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
    private val cacheStrategy: CacheStrategy = CacheStrategy.CURRENT_PAGE_ONLY
) : IDataProvider<T> {
    override fun getSize(callback: DataProviderCallback<Int>) {
        sizeProvider(callback)
    }

    override fun fetchItems(offset: Int, limit: Int, callback: DataProviderCallback<List<T>>) {
        itemProvider(offset, limit, callback)
    }

    override fun canFetchMore(offset: Int, callback: DataProviderCallback<Boolean>) {
        hasMoreProvider(offset, callback)
    }

    override fun getCacheStrategy(): CacheStrategy = cacheStrategy
}