package city.newnan.gui.dataprovider

/**
 * 数据获取结果回调
 */
typealias DataProviderCallback<T> = (result: Result<T>) -> Unit

/**
 * 数据提供器接口
 */
interface IDataProvider<T> {
    /**
     * 获取数据总数
     *
     * @param callback 结果回调
     */
    fun getSize(callback: DataProviderCallback<Int>)

    /**
     * 获取指定范围的数据项
     *
     * @param offset 偏移量（从0开始）
     * @param limit 获取数量限制
     * @param callback 结果回调
     */
    fun fetchItems(offset: Int, limit: Int, callback: DataProviderCallback<List<T>>)

    /**
     * 检查是否还能获取更多数据
     *
     * @param offset 偏移量
     * @param callback 结果回调
     */
    fun canFetchMore(offset: Int, callback: DataProviderCallback<Boolean>)

    /**
     * 获取建议的缓存策略
     */
    fun getCacheStrategy(): CacheStrategy
}