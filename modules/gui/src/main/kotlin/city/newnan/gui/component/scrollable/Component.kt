package city.newnan.gui.component.scrollable

import city.newnan.core.cache.LRUCache
import city.newnan.core.utils.text.toMiniMessage
import city.newnan.gui.component.base.BaseComponent
import city.newnan.gui.component.base.BaseRenderContext
import city.newnan.gui.dataprovider.CacheStrategy
import city.newnan.gui.dataprovider.IDataProvider
import city.newnan.gui.dataprovider.ListDataProvider
import city.newnan.gui.dataprovider.SyncDataProvider
import city.newnan.gui.event.EventHandlers
import city.newnan.gui.page.BasePage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.Semaphore
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.math.ceil

typealias ScrollChangeHandler = (context: ScrollChangeContext) -> Unit

/**
 * 四元组数据类（用于内部多返回值）
 */
private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * 行缓存条目
 * 包含行数据和状态，作为统一的缓存单元
 * 
 * @param T 数据项类型
 * @param data 行数据列表
 * @param state 行状态
 */
data class RowCacheEntry<T>(
    val data: List<T>,
    val state: RowState
) {
    companion object {
        fun <T> loading(): RowCacheEntry<T> = RowCacheEntry(emptyList(), RowState.LOADING)
        fun <T> error(): RowCacheEntry<T> = RowCacheEntry(emptyList(), RowState.ERROR)
        fun <T> loaded(data: List<T>): RowCacheEntry<T> = RowCacheEntry(data, RowState.LOADED)
    }
}

/**
 * 行状态枚举
 */
enum class RowState {
    NOT_LOADED,  // 未加载
    LOADING,     // 正在加载
    LOADED,      // 已加载
    ERROR        // 加载失败
}

/**
 * 可滚动组件
 *
 * 支持在固定区域内滚动显示大量数据，特性：
 * - 支持三种数据获取模式：同步任务、异步任务、协程
 * - 基于 IDataProvider 的灵活数据访问
 * - 垂直滚动支持
 * - 滚动偏移管理
 * - 可见区域控制
 * - 滚动边界检查
 * - 动态数据更新
 * - 完善的状态管理
 * - 智能缓存策略
 * - 空槽位和加载状态处理
 *
 * @param T 数据项类型
 */
class ScrollableComponent<T>(
    page: BasePage,
    private val startX: Int,
    private val startY: Int,
    private val width: Int,
    private val height: Int,
    private var dataProvider: IDataProvider<T>
) : BaseComponent<ScrollableRenderContext<T>>(page) {
    // 便利构造器
    constructor(
        page: BasePage,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        data: List<T>
    ) : this(page, startX, startY, width, height, ListDataProvider(data))

    constructor(
        page: BasePage,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        data: () -> List<T>
    ) : this(page, startX, startY, width, height, SyncDataProvider(data))

    /**
     * 组件状态枚举
     */
    enum class ComponentState {
        INIT,     // 初始状态
        LOADING,  // 正在加载数据
        READY,    // 数据已准备好
        ERROR     // 数据加载失败
    }


    // 事件处理器
    override val eventHandlers = EventHandlers()

    // 渲染函数
    private var renderFunction: RenderFunction<ScrollableRenderContext<T>>? = null

    // 空槽位渲染函数
    private var emptySlotRenderFunction: RenderFunction<ScrollableRenderContext<T>>? = null

    // 加载中渲染函数（用于无限滚动）
    private var loadingSlotRenderFunction: RenderFunction<ScrollableRenderContext<T>>? = null

    // 当前滚动偏移（从0开始，以行为单位）
    private var scrollOffset = 0

    // 每行显示的项目数量
    val itemsPerRow: Int = width

    // 可见行数
    val visibleRows: Int = height

    // 可见区域内的项目数量
    val visibleItems: Int = width * height

    // 组件状态
    private val componentState = AtomicReference(ComponentState.INIT)

    // 数据总大小缓存
    @Volatile
    private var totalDataSize: Int = -1

    // 行缓存（使用 LRUCache 统一管理数据和状态）
    private var rowCache: LRUCache<Int, RowCacheEntry<T>>

    // 是否为无限滚动模式
    private val isInfiniteMode: Boolean get() = totalDataSize < 0
    
    // 操作锁，确保异步操作的原子性
    private val operationLock = ReentrantLock()

    // 总行数
    val totalRows: Int get() = if (isInfiniteMode) Int.MAX_VALUE else ceil(totalDataSize.toDouble() / itemsPerRow).toInt()

    // 最大滚动偏移
    val maxScrollOffset: Int get() = if (isInfiniteMode) Int.MAX_VALUE else max(0, totalRows - visibleRows)

    var scrollChangeHandler: ScrollChangeHandler? = null

    // 计算所有槽位
    private val _slots: List<Int> by lazy {
        val result = mutableListOf<Int>()
        val inventoryWidth = page.getInventoryWidth()

        for (y in startY until startY + height) {
            for (x in startX until startX + width) {
                val slot = y * inventoryWidth + x
                if (slot < page.inventory.size) {
                    result.add(slot)
                }
            }
        }
        result
    }

    init {
        // 验证参数
        if (width <= 0 || height <= 0) {
            throw IllegalArgumentException("Width and height must be positive: width=$width, height=$height")
        }

        if (startX < 0 || startY < 0) {
            throw IllegalArgumentException("Start coordinates must be non-negative: startX=$startX, startY=$startY")
        }

        // 验证区域是否在inventory范围内
        val inventoryWidth = page.getInventoryWidth()
        val inventoryHeight = page.inventory.size / inventoryWidth

        if (startX + width > inventoryWidth || startY + height > inventoryHeight) {
            throw IllegalArgumentException(
                "Scrollable area exceeds inventory bounds: " +
                "area=($startX,$startY,${startX + width - 1},${startY + height - 1}), " +
                "inventory=(0,0,${inventoryWidth - 1},${inventoryHeight - 1})"
            )
        }

        // 根据缓存策略初始化缓存
        rowCache = createRowCache()
        bind(rowCache) // 绑定到组件生命周期

        // 初始化数据
        refreshData()
    }

    /**
     * 根据缓存策略创建行缓存
     */
    private fun createRowCache(): LRUCache<Int, RowCacheEntry<T>> {
        val strategy = dataProvider.getCacheStrategy()
        val capacity = when (strategy) {
            CacheStrategy.NO_CACHE -> 0
            CacheStrategy.CURRENT_PAGE_ONLY -> visibleRows
            CacheStrategy.MULTI_PAGE -> visibleRows * 3
            CacheStrategy.AGGRESSIVE -> visibleRows * 10
        }
        return LRUCache(capacity)
    }

    /**
     * 根据缓存策略应用清理逻辑
     * 只在必要时调用，避免重复清理
     */
    private fun applyCacheStrategy() {
        val strategy = dataProvider.getCacheStrategy()
        
        when (strategy) {
            CacheStrategy.NO_CACHE -> {
                // 不缓存，立即清空所有缓存
                rowCache.clear()
            }
            CacheStrategy.CURRENT_PAGE_ONLY -> {
                // 只保留当前可见区域的行
                val visibleRowRange = scrollOffset until (scrollOffset + visibleRows)
                val keysToRemove = rowCache.keys.filter { it !in visibleRowRange }
                keysToRemove.forEach { row -> rowCache.remove(row) }
            }
            // MULTI_PAGE 和 AGGRESSIVE 由 LRUCache 自动管理
            else -> {
                // LRUCache 会自动管理缓存大小，无需手动处理
            }
        }
    }


    /**
     * 刷新数据
     */
    private fun refreshData() {
        // 设置组件状态为加载中
        componentState.set(ComponentState.LOADING)

        // 获取数据总大小
        dataProvider.getSize { sizeResult ->
            sizeResult.fold(
                onSuccess = { size ->
                    totalDataSize = size
                    // 根据缓存策略清理缓存（只在数据源变更时）
                    applyCacheStrategy()
                    // 设置组件状态为就绪
                    componentState.set(ComponentState.READY)
                    // 加载当前可见区域的数据
                    loadVisibleData()
                },
                onFailure = { error ->
                    // 处理数据加载错误
                    totalDataSize = 0
                    rowCache.clear()
                    componentState.set(ComponentState.ERROR)
                    update()
                }
            )
        }
    }

    /**
     * 加载当前可见区域的数据
     */
    private fun loadVisibleData() {
        for (row in scrollOffset until scrollOffset + visibleRows) {
            loadRowData(row)
        }
    }

    /**
     * 加载指定行数据
     */
    private fun loadRowData(row: Int) {
        val strategy = dataProvider.getCacheStrategy()

        // NO_CACHE策略：每次都重新加载
        if (strategy == CacheStrategy.NO_CACHE) {
            loadRowDataInternal(row, forceReload = true)
            return
        }

        val currentEntry = rowCache[row]
        if (currentEntry != null && (currentEntry.state == RowState.LOADING || currentEntry.state == RowState.LOADED)) {
            // 缓存命中，直接返回
            return
        }

        loadRowDataInternal(row, forceReload = false)
    }

    // NO_CACHE策略的临时数据存储
    @Volatile
    private var noCacheTemporaryData: MutableMap<Int, List<T>> = mutableMapOf()

    /**
     * 内部行数据加载方法
     */
    private fun loadRowDataInternal(row: Int, forceReload: Boolean) {
        val strategy = dataProvider.getCacheStrategy()

        // 如果强制重新加载或者是NO_CACHE策略，清除现有缓存
        if (forceReload || strategy == CacheStrategy.NO_CACHE) {
            rowCache.remove(row)
        }

        // 非NO_CACHE策略才设置loading状态到缓存
        if (strategy != CacheStrategy.NO_CACHE) {
            rowCache[row] = RowCacheEntry.loading()
        }

        val offset = row * itemsPerRow
        dataProvider.fetchItems(offset, itemsPerRow) { dataResult ->
            dataResult.fold(
                onSuccess = { data ->
                    // 根据缓存策略决定是否缓存
                    if (strategy != CacheStrategy.NO_CACHE) {
                        rowCache[row] = RowCacheEntry.loaded(data)
                        // LRU缓存会自动管理大小，无需手动调用
                    } else {
                        // NO_CACHE策略：真正不缓存，只用临时变量存储可见行数据
                        if (row >= scrollOffset && row < scrollOffset + visibleRows) {
                            synchronized(noCacheTemporaryData) {
                                noCacheTemporaryData[row] = data
                                // 清理不可见行的临时数据
                                val visibleRange = scrollOffset until (scrollOffset + visibleRows)
                                noCacheTemporaryData.keys.removeAll { it !in visibleRange }
                            }
                        }
                    }

                    // 如果是当前可见区域，立即更新显示
                    if (row >= scrollOffset && row < scrollOffset + visibleRows) {
                        update()
                    }
                },
                onFailure = { error ->
                    if (strategy != CacheStrategy.NO_CACHE) {
                        rowCache[row] = RowCacheEntry.error()
                    } else {
                        // NO_CACHE策略：不缓存错误状态，清空临时数据
                        if (row >= scrollOffset && row < scrollOffset + visibleRows) {
                            synchronized(noCacheTemporaryData) {
                                noCacheTemporaryData.remove(row)
                            }
                        }
                    }

                    if (row >= scrollOffset && row < scrollOffset + visibleRows) {
                        update()
                    }
                }
            )
        }
    }

    /**
     * 设置数据提供器
     */
    fun setDataProvider(newDataProvider: IDataProvider<T>) {
        synchronized(this) {
            // 暂停组件状态更新
            componentState.set(ComponentState.LOADING)
            
            // 保存旧缓存的引用
            val oldCache = this.rowCache
            
            // 创建新缓存
            val newCache = createRowCache()
            bind(newCache)
            
            // 原子性地更新所有状态
            this.dataProvider = newDataProvider
            this.scrollOffset = 0
            this.totalDataSize = -1
            this.rowCache = newCache
            
            // 关闭旧缓存
            oldCache.close()
            
            // 刷新数据
            refreshData()
        }
    }

    /**
     * 设置数据列表（便利方法）
     */
    fun setData(newData: List<T>) {
        setDataProvider(ListDataProvider(newData))
    }

    /**
     * 清空数据（便利方法）
     */
    fun clearData() {
        setData(emptyList())
    }

    /**
     * 清空缓存
     */
    fun clearCache() {
        rowCache.clear()
    }

    /**
     * 刷新当前可见区域
     */
    fun refresh() {
        val strategy = dataProvider.getCacheStrategy()
        for (row in scrollOffset until scrollOffset + visibleRows) {
            if (strategy == CacheStrategy.NO_CACHE) {
                // NO_CACHE策略：强制重新加载
                loadRowDataInternal(row, forceReload = true)
            } else {
                // 其他策略：清除行缓存后重新加载
                rowCache.remove(row)
                loadRowData(row)
            }
        }
    }

    /**
     * 刷新指定行
     */
    fun refreshRow(row: Int) {
        val strategy = dataProvider.getCacheStrategy()
        if (strategy == CacheStrategy.NO_CACHE) {
            // NO_CACHE策略：强制重新加载
            loadRowDataInternal(row, forceReload = true)
        } else {
            // 其他策略：清除指定行缓存后重新加载
            rowCache.remove(row)
            loadRowData(row)
        }
    }

    /**
     * 预加载指定行
     */
    fun preloadRow(row: Int) {
        if (row >= 0) {
            val strategy = dataProvider.getCacheStrategy()

            // NO_CACHE策略：不进行预加载
            if (strategy == CacheStrategy.NO_CACHE) {
                return
            }

            val entry = rowCache[row]
            if (entry == null || (entry.state != RowState.LOADING && entry.state != RowState.LOADED)) {
                loadRowData(row)
            }
        }
    }

    /**
     * 根据缓存策略预加载邻近行
     */
    private fun preloadAdjacentRows() {
        val strategy = dataProvider.getCacheStrategy()

        when (strategy) {
            CacheStrategy.NO_CACHE, CacheStrategy.CURRENT_PAGE_ONLY -> {
                // 不预加载
                return
            }
            CacheStrategy.MULTI_PAGE -> {
                // 预加载前后各2行
                for (i in -2..2) {
                    val targetRow = scrollOffset + visibleRows / 2 + i
                    if (targetRow >= 0) {
                        preloadRow(targetRow)
                    }
                }
            }
            CacheStrategy.AGGRESSIVE -> {
                // 预加载前后各5行
                for (i in -5..5) {
                    val targetRow = scrollOffset + visibleRows / 2 + i
                    if (targetRow >= 0) {
                        preloadRow(targetRow)
                    }
                }
            }
        }
    }

    /**
     * 设置渲染函数
     */
    fun render(function: RenderFunction<ScrollableRenderContext<T>>) {
        this.renderFunction = function
    }

    /**
     * 设置空槽位渲染函数
     */
    fun renderEmptySlot(function: RenderFunction<ScrollableRenderContext<T>>) {
        this.emptySlotRenderFunction = function
    }

    /**
     * 设置加载中渲染函数（用于无限滚动）
     */
    fun renderLoadingSlot(function: RenderFunction<ScrollableRenderContext<T>>) {
        this.loadingSlotRenderFunction = function
    }

    /**
     * 设置空槽位为固定物品
     */
    fun setEmptySlotItem(item: ItemStack?) {
        renderEmptySlot { item }
    }

    /**
     * 设置空槽位为固定材料
     */
    fun setEmptySlotMaterial(material: Material) {
        renderEmptySlot { ItemStack(material) }
    }

    /**
     * 设置加载中槽位为固定物品
     */
    fun setLoadingSlotItem(item: ItemStack?) {
        renderLoadingSlot { item }
    }

    /**
     * 设置加载中槽位为固定材料
     */
    fun setLoadingSlotMaterial(material: Material) {
        renderLoadingSlot { ItemStack(material) }
    }

    /**
     * 滚动到指定偏移
     */
    fun scrollTo(offset: Int): Boolean {
        val newOffset = if (isInfiniteMode) {
            offset.coerceAtLeast(0)  // 无限模式：只限制最小值
        } else {
            offset.coerceIn(0, maxScrollOffset)  // 有限模式：限制范围
        }

        if (newOffset != scrollOffset) {
            val previousOffset = scrollOffset
            scrollOffset = newOffset

            // 加载新的可见区域数据
            loadVisibleData()

            // 根据缓存策略预加载邻近行
            preloadAdjacentRows()

            // 立即更新显示
            update()

            // 触发滚动变更事件
            scrollChangeHandler?.invoke(
                ScrollChangeContext(
                    previousOffset = previousOffset,
                    currentOffset = scrollOffset,
                    previousStartIndex = previousOffset * itemsPerRow,
                    currentStartIndex = scrollOffset * itemsPerRow,
                    visibleStartIndex = scrollOffset * itemsPerRow,
                    visibleEndIndex = (scrollOffset + visibleRows) * itemsPerRow - 1,
                    visibleRows = visibleRows,
                    itemsPerRow = itemsPerRow,
                    totalRows = if (isInfiniteMode) -1 else totalRows,
                    totalItems = if (isInfiniteMode) -1L else totalDataSize.toLong()
                )
            )
            return true
        }
        return false
    }

    /**
     * 向上滚动
     */
    fun scrollUp(lines: Int = 1): Boolean {
        return scrollTo(scrollOffset - lines)
    }

    /**
     * 向下滚动
     */
    fun scrollDown(lines: Int = 1): Boolean {
        return operationLock.withLock {
            if (isInfiniteMode) {
                // 无限模式：同步检查是否还能获取更多数据
                val nextRowOffset = (scrollOffset + lines) * itemsPerRow
                val semaphore = Semaphore(0)
                var canScroll = false
                
                dataProvider.canFetchMore(nextRowOffset) { canFetchResult ->
                    canFetchResult.fold(
                        onSuccess = { canFetch ->
                            if (canFetch) {
                                canScroll = scrollTo(scrollOffset + lines)
                            }
                        },
                        onFailure = { /* 忽略错误，不进行滚动 */ }
                    )
                    semaphore.release()
                }
                
                // 等待异步操作完成
                semaphore.acquire()
                canScroll
            } else {
                // 有限模式：检查滚动边界
                scrollTo(scrollOffset + lines)
            }
        }
    }

    /**
     * 滚动到顶部
     */
    fun scrollToTop(): Boolean {
        return scrollTo(0)
    }

    /**
     * 滚动到底部（仅适用于有限滚动）
     */
    fun scrollToBottom(): Boolean {
        return if (isInfiniteMode) {
            false  // 无限模式没有底部
        } else {
            scrollTo(maxScrollOffset)
        }
    }

    /**
     * 获取当前滚动偏移
     */
    fun getScrollOffset(): Int = scrollOffset

    /**
     * 检查是否可以向上滚动
     */
    fun canScrollUp(): Boolean = scrollOffset > 0

    /**
     * 检查是否可以向下滚动（同步方法，可能阻塞）
     */
    fun canScrollDown(): Boolean = if (isInfiniteMode) {
        // 无限模式：使用缓存的结果或返回true
        // 注意：这是一个同步方法，不适合进行异步检查
        true  // 简化处理，总是返回true让用户尝试
    } else {
        // 有限模式：检查滚动边界
        scrollOffset < maxScrollOffset
    }

    /**
     * 异步检查是否可以向下滚动
     */
    fun canScrollDownAsync(callback: (Boolean) -> Unit) {
        if (isInfiniteMode) {
            val nextRowOffset = (scrollOffset + 1) * itemsPerRow
            dataProvider.canFetchMore(nextRowOffset) { result ->
                result.fold(
                    onSuccess = { canFetch -> callback(canFetch) },
                    onFailure = { callback(false) }
                )
            }
        } else {
            callback(scrollOffset < maxScrollOffset)
        }
    }

    /**
     * 检查行是否正在加载
     */
    fun isRowLoading(row: Int): Boolean = rowCache[row]?.state == RowState.LOADING

    /**
     * 检查行是否已缓存
     */
    fun isRowCached(row: Int): Boolean = rowCache[row]?.state == RowState.LOADED

    /**
     * 获取指定行的数据
     */
    fun getRowData(row: Int): List<T> = rowCache[row]?.data ?: emptyList()

    /**
     * 获取行状态
     */
    fun getRowState(row: Int): RowState = rowCache[row]?.state ?: RowState.NOT_LOADED

    /**
     * 获取组件状态
     */
    fun getComponentState(): ComponentState = componentState.get()

    /**
     * 获取当前缓存策略
     */
    fun getCacheStrategy(): CacheStrategy = dataProvider.getCacheStrategy()

    /**
     * 获取滚动组件缓存统计信息
     */
    fun getScrollableCacheStats(): String {
        val stats = getCacheStatsDetailed()
        return "ScrollableComponent Cache - Strategy: ${stats.strategy}, Rows: ${stats.cachedRows}/${stats.maxCacheSize}, Hit Rate: ${"%.2f".format(stats.cacheHitRate * 100)}%, Cached Rows: ${stats.cachedRowNumbers}"
    }

    /**
     * 获取详细的缓存统计信息
     */
    fun getCacheStatsDetailed(): ScrollableCacheStats {
        return ScrollableCacheStats(
            strategy = getCacheStrategy(),
            cachedRows = rowCache.size,
            maxCacheSize = rowCache.capacity,
            cacheHitRate = calculateCacheHitRate(),
            cachedRowNumbers = rowCache.keys.toList(),
            visibleRows = visibleRows
        )
    }

    /**
     * 计算缓存命中率（简化实现）
     */
    private fun calculateCacheHitRate(): Double {
        return if (rowCache.capacity == 0) 0.0
               else rowCache.size.toDouble() / rowCache.capacity.coerceAtLeast(1)
    }

    /**
     * 强制清理缓存（除了当前可见区域）
     */
    fun forceClearCache(keepVisibleRows: Boolean = true) {
        if (keepVisibleRows) {
            val visibleRowData = mutableMapOf<Int, RowCacheEntry<T>>()

            for (row in scrollOffset until scrollOffset + visibleRows) {
                rowCache[row]?.let { visibleRowData[row] = it }
            }

            rowCache.clear()
            rowCache.putAll(visibleRowData)
        } else {
            clearCache()
        }
    }

    override fun getSlots(): List<Int> {
        return _slots
    }

    override fun getRenderContext(slot: Int): ScrollableRenderContext<T>? {
        return getRenderContext(slot, null)
    }

    override fun getRenderContext(slot: Int, oldItem: ItemStack?): ScrollableRenderContext<T>? {
        if (!_slots.contains(slot)) return null

        val inventoryWidth = page.getInventoryWidth()
        val x = slot % inventoryWidth
        val y = slot / inventoryWidth
        val relativeX = x - startX
        val relativeY = y - startY
        val visibleIndex = relativeY * width + relativeX
        val dataRow = scrollOffset + relativeY
        val dataIndex = dataRow * itemsPerRow + relativeX

        // 获取行数据
        val strategy = dataProvider.getCacheStrategy()
        val (rowData, rowState, isLoading, isRowCached) = if (strategy == CacheStrategy.NO_CACHE) {
            // NO_CACHE策略：从临时数据获取
            val tempData = synchronized(noCacheTemporaryData) { noCacheTemporaryData[dataRow] }
            if (tempData != null) {
                Tuple4(tempData, RowState.LOADED, false, true)
            } else {
                Tuple4(emptyList<T>(), RowState.NOT_LOADED, false, false)
            }
        } else {
            // 其他策略：从缓存获取
            val rowEntry = rowCache[dataRow]
            val data = rowEntry?.data ?: emptyList()
            val state = rowEntry?.state ?: RowState.NOT_LOADED
            val loading = state == RowState.LOADING
            val cached = state == RowState.LOADED
            Tuple4(data, state, loading, cached)
        }
        val item = if (relativeX < rowData.size) rowData[relativeX] else null

        return ScrollableRenderContext(
            x = x,
            y = y,
            slot = slot,
            oldItem = oldItem,
            relativeX = relativeX,
            relativeY = relativeY,
            visibleIndex = visibleIndex,
            dataRow = dataRow,
            dataIndex = dataIndex,
            scrollOffset = scrollOffset,
            item = item,
            isLoading = isLoading,
            isRowCached = isRowCached,
            isInfiniteMode = isInfiniteMode,
            rowState = rowState,
            componentState = componentState.get()
        )
    }

    override fun renderSlot(context: ScrollableRenderContext<T>): ItemStack? {
        return when {
            // 组件级别错误状态
            context.componentState == ComponentState.ERROR -> {
                // 可以显示错误图标或空槽位
                emptySlotRenderFunction?.invoke(context)
            }
            // 行正在加载且没有数据时显示加载中
            context.rowState == RowState.LOADING && context.item == null -> {
                loadingSlotRenderFunction?.invoke(context)
            }
            // 行加载失败
            context.rowState == RowState.ERROR -> {
                emptySlotRenderFunction?.invoke(context)
            }
            // 有数据时正常渲染
            context.item != null -> {
                renderFunction?.invoke(context)
            }
            // 空槽位
            else -> {
                emptySlotRenderFunction?.invoke(context)
            }
        }
    }

    override fun toString(): String {
        return "ScrollableComponent(startX=$startX, startY=$startY, width=$width, height=$height, scrollOffset=$scrollOffset, maxOffset=${
            if(isInfiniteMode) "∞" else maxScrollOffset}, mode=${if(isInfiniteMode) "infinite" else "finite"}, totalRows=${
                if(isInfiniteMode) "∞" else totalRows}, dataSize=$totalDataSize, cachedRows=${rowCache.size}, page=${page.title.toMiniMessage()})"
    }

    override fun close() {
        super.close()
    }
}

/**
 * 可滚动渲染上下文
 */
data class ScrollableRenderContext<T>(
    override val x: Int,
    override val y: Int,
    override val slot: Int,
    override val oldItem: ItemStack?,
    val relativeX: Int,
    val relativeY: Int,
    val visibleIndex: Int,
    val dataRow: Int,
    val dataIndex: Int,
    val scrollOffset: Int,
    val item: T?,
    val isLoading: Boolean = false,
    val isRowCached: Boolean = false,
    val isInfiniteMode: Boolean = false,
    val rowState: RowState = RowState.NOT_LOADED,
    val componentState: ScrollableComponent.ComponentState = ScrollableComponent.ComponentState.INIT
) : BaseRenderContext()

/**
 * 滚动变更上下文
 *
 * @param previousOffset 之前的滚动偏移（行数，-1表示首次加载）
 * @param currentOffset 当前滚动偏移（行数）
 * @param previousStartIndex 之前的起始全局索引（-1表示首次加载）
 * @param currentStartIndex 当前的起始全局索引
 * @param visibleStartIndex 当前可见区域起始索引
 * @param visibleEndIndex 当前可见区域结束索引
 * @param visibleRows 可见行数
 * @param itemsPerRow 每行项目数
 * @param totalRows 总行数（无限滚动时为-1）
 * @param totalItems 总条目数（无限滚动时为-1）
 */
data class ScrollChangeContext(
    val previousOffset: Int,
    val currentOffset: Int,
    val previousStartIndex: Int,
    val currentStartIndex: Int,
    val visibleStartIndex: Int,
    val visibleEndIndex: Int,
    val visibleRows: Int,
    val itemsPerRow: Int,
    val totalRows: Int,
    val totalItems: Long
)

/**
 * 渲染函数类型别名
 */
typealias RenderFunction<T> = (T) -> ItemStack?

/**
 * 滚动组件缓存统计信息
 */
data class ScrollableCacheStats(
    val strategy: CacheStrategy,
    val cachedRows: Int,
    val maxCacheSize: Int,
    val cacheHitRate: Double,
    val cachedRowNumbers: List<Int>,
    val visibleRows: Int
)
