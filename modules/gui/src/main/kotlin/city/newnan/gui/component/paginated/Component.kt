package city.newnan.gui.component.paginated

import city.newnan.core.cache.LRUCache
import city.newnan.core.utils.text.toMiniMessage
import city.newnan.gui.component.base.BaseComponent
import city.newnan.gui.component.base.BaseRenderContext
import city.newnan.gui.dataprovider.CacheStrategy
import city.newnan.gui.dataprovider.IDataProvider
import city.newnan.gui.dataprovider.ListDataProvider
import city.newnan.gui.dataprovider.SyncDataProvider
import city.newnan.gui.page.BasePage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.Semaphore
import kotlin.concurrent.withLock
import kotlin.math.ceil

typealias PageChangeHandler = (context: PageChangeContext) -> Unit

/**
 * 页面缓存条目
 * 包含页面数据和状态，作为统一的缓存单元
 * 
 * @param T 数据项类型
 * @param data 页面数据列表
 * @param state 页面状态
 */
data class PageCacheEntry<T>(
    val data: List<T>,
    val state: PageState
) {
    companion object {
        fun <T> loading(): PageCacheEntry<T> = PageCacheEntry(emptyList(), PageState.LOADING)
        fun <T> error(): PageCacheEntry<T> = PageCacheEntry(emptyList(), PageState.ERROR)
        fun <T> loaded(data: List<T>): PageCacheEntry<T> = PageCacheEntry(data, PageState.LOADED)
    }
}

/**
 * 页面状态枚举
 */
enum class PageState {
    NOT_LOADED,  // 未加载
    LOADING,     // 正在加载
    LOADED,      // 已加载
    ERROR        // 加载失败
}

/**
 * 分页组件
 *
 * 支持大量数据的分页展示，特性：
 * - 基于 DataProvider 的灵活数据访问
 * - 自动分页计算
 * - 页面导航控制
 * - 动态数据更新
 * - 空槽位处理
 *
 * @param T 数据项类型
 */
class PaginatedComponent<T>(
    page: BasePage,
    private val startX: Int,
    private val startY: Int,
    private val width: Int,
    private val height: Int,
    private var dataProvider: IDataProvider<T>
) : BaseComponent<PaginatedRenderContext<T>>(page) {
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
        data: () -> List<T>,
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


    // 渲染函数
    private var renderFunction: RenderFunction<PaginatedRenderContext<T>>? = null

    // 空槽位渲染函数
    private var emptySlotRenderFunction: RenderFunction<PaginatedRenderContext<T>>? = null

    // 加载中渲染函数（用于无限分页）
    private var loadingSlotRenderFunction: RenderFunction<PaginatedRenderContext<T>>? = null

    // 当前页码（从0开始）
    private var currentPage = 0

    // 固定的逻辑页面大小（用于数据请求和分页计算）
    private val logicalItemsPerPage: Int = width * height
    
    // 实际可显示的项目数量（考虑槽位覆盖）
    val pageSize: Int get() = availableSlots.size

    // 组件状态
    private val componentState = AtomicReference(ComponentState.INIT)

    // 数据总大小缓存
    @Volatile
    private var totalDataSize: Int = -1

    // 页面缓存（使用 LRUCache 统一管理数据和状态）
    private var pageCache: LRUCache<Int, PageCacheEntry<T>>

    // 是否为无限分页模式
    private val isInfiniteMode: Boolean get() = totalDataSize < 0
    
    // 操作锁，确保异步操作的原子性
    private val operationLock = ReentrantLock()

    // 总页数（基于逻辑页面大小）
    val totalPages: Int get() = if (isInfiniteMode) Int.MAX_VALUE else ceil(totalDataSize.toDouble() / logicalItemsPerPage).toInt()

    // 当前页的数据
    private val currentPageData: List<T> get() {
        val strategy = dataProvider.getCacheStrategy()
        return if (strategy == CacheStrategy.NO_CACHE) {
            // NO_CACHE策略：从临时数据获取
            val tempData = noCacheTemporaryData
            if (tempData != null && tempData.first == currentPage) {
                tempData.second
            } else {
                emptyList()
            }
        } else {
            // 其他策略：从缓存获取
            pageCache[currentPage]?.data ?: emptyList()
        }
    }

    var pageChangeHandler: PageChangeHandler? = null

    // 计算所有可能的槽位（不考虑组件覆盖）
    private val potentialSlots: List<Int> by lazy {
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

    // 实际可用的槽位（考虑组件覆盖情况）
    private val availableSlots: List<Int> get() {
        return potentialSlots.filter { slot ->
            val ownerComponent = page.getComponentBySlot(slot)
            ownerComponent == null || ownerComponent == this
        }
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
                "Paginated area exceeds inventory bounds: " +
                "area=($startX,$startY,${startX + width - 1},${startY + height - 1}), " +
                "inventory=(0,0,${inventoryWidth - 1},${inventoryHeight - 1})"
            )
        }

        // 根据缓存策略初始化缓存
        pageCache = createPageCache()
        bind(pageCache) // 绑定到组件生命周期

        // 初始化数据
        refreshData()
    }

    /**
     * 根据缓存策略创建页面缓存
     */
    private fun createPageCache(): LRUCache<Int, PageCacheEntry<T>> {
        val strategy = dataProvider.getCacheStrategy()
        val capacity = when (strategy) {
            CacheStrategy.NO_CACHE -> 0
            CacheStrategy.CURRENT_PAGE_ONLY -> 1
            CacheStrategy.MULTI_PAGE -> 5
            CacheStrategy.AGGRESSIVE -> 20
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
                pageCache.clear()
            }
            CacheStrategy.CURRENT_PAGE_ONLY -> {
                // 只保留当前页
                val currentEntry = pageCache[currentPage]
                pageCache.clear()
                if (currentEntry != null) {
                    pageCache[currentPage] = currentEntry
                }
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
                    // 加载当前页数据
                    loadPageData(currentPage)
                },
                onFailure = { error ->
                    // 处理数据加载错误
                    totalDataSize = 0
                    pageCache.clear()
                    componentState.set(ComponentState.ERROR)
                    update()
                }
            )
        }
    }

    /**
     * 加载指定页面数据
     */
    private fun loadPageData(page: Int) {
        val strategy = dataProvider.getCacheStrategy()

        // NO_CACHE策略：每次都重新加载
        if (strategy == CacheStrategy.NO_CACHE) {
            loadPageDataInternal(page, forceReload = true)
            return
        }

        val currentEntry = pageCache[page]
        if (currentEntry != null && (currentEntry.state == PageState.LOADING || currentEntry.state == PageState.LOADED)) {
            // 缓存命中，直接返回
            return
        }

        loadPageDataInternal(page, forceReload = false)
    }

    // NO_CACHE策略的临时数据存储
    @Volatile
    private var noCacheTemporaryData: Pair<Int, List<T>>? = null

    /**
     * 内部页面数据加载方法
     */
    private fun loadPageDataInternal(page: Int, forceReload: Boolean) {
        val strategy = dataProvider.getCacheStrategy()

        // 如果强制重新加载或者是NO_CACHE策略，清除现有缓存
        if (forceReload || strategy == CacheStrategy.NO_CACHE) {
            pageCache.remove(page)
        }

        // 非NO_CACHE策略才设置loading状态到缓存
        if (strategy != CacheStrategy.NO_CACHE) {
            pageCache[page] = PageCacheEntry.loading()
        }

        val offset = page * logicalItemsPerPage
        dataProvider.fetchItems(offset, logicalItemsPerPage) { dataResult ->
            dataResult.fold(
                onSuccess = { data ->
                    // 根据缓存策略决定是否缓存
                    if (strategy != CacheStrategy.NO_CACHE) {
                        pageCache[page] = PageCacheEntry.loaded(data)
                        // LRU缓存会自动管理大小，无需手动调用
                    } else {
                        // NO_CACHE策略：真正不缓存，只用临时变量存储当前页数据
                        if (page == currentPage) {
                            noCacheTemporaryData = Pair(page, data)
                        }
                    }

                    // 如果是当前页，立即更新显示
                    if (page == currentPage) {
                        update()
                    }
                },
                onFailure = { error ->
                    if (strategy != CacheStrategy.NO_CACHE) {
                        pageCache[page] = PageCacheEntry.error()
                    } else {
                        // NO_CACHE策略：不缓存错误状态，清空临时数据
                        if (page == currentPage) {
                            noCacheTemporaryData = null
                        }
                    }

                    if (page == currentPage) {
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
            val oldCache = this.pageCache

            // 创建新缓存
            val newCache = createPageCache()
            bind(newCache)

            // 原子性地更新所有状态
            this.dataProvider = newDataProvider
            this.currentPage = 0
            this.totalDataSize = -1
            this.pageCache = newCache

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
     * 清空缓存
     */
    fun clearCache() {
        pageCache.clear()
    }

    /**
     * 刷新当前页
     */
    fun refresh() {
        val strategy = dataProvider.getCacheStrategy()
        if (strategy == CacheStrategy.NO_CACHE) {
            // NO_CACHE策略：强制重新加载
            loadPageDataInternal(currentPage, forceReload = true)
        } else {
            // 其他策略：清除当前页缓存后重新加载
            pageCache.remove(currentPage)
            loadPageData(currentPage)
        }
    }

    /**
     * 刷新指定页面
     */
    fun refreshPage(page: Int) {
        val strategy = dataProvider.getCacheStrategy()
        if (strategy == CacheStrategy.NO_CACHE) {
            // NO_CACHE策略：强制重新加载
            loadPageDataInternal(page, forceReload = true)
        } else {
            // 其他策略：清除指定页缓存后重新加载
            pageCache.remove(page)
            loadPageData(page)
        }
    }

    /**
     * 预加载指定页面
     */
    fun preloadPage(page: Int) {
        if (page >= 0) {
            val strategy = dataProvider.getCacheStrategy()

            // NO_CACHE策略：不进行预加载
            if (strategy == CacheStrategy.NO_CACHE) {
                return
            }

            val entry = pageCache[page]
            if (entry == null || (entry.state != PageState.LOADING && entry.state != PageState.LOADED)) {
                loadPageData(page)
            }
        }
    }

    /**
     * 根据缓存策略预加载邻近页面
     */
    private fun preloadAdjacentPages() {
        val strategy = dataProvider.getCacheStrategy()

        when (strategy) {
            CacheStrategy.NO_CACHE, CacheStrategy.CURRENT_PAGE_ONLY -> {
                // 不预加载
                return
            }
            CacheStrategy.MULTI_PAGE -> {
                // 预加载前后各1页
                preloadPage(currentPage + 1)
                if (currentPage > 0) {
                    preloadPage(currentPage - 1)
                }
            }
            CacheStrategy.AGGRESSIVE -> {
                // 预加载前后各2页
                for (i in -2..2) {
                    val targetPage = currentPage + i
                    if (targetPage >= 0 && targetPage != currentPage) {
                        preloadPage(targetPage)
                    }
                }
            }
        }
    }

    /**
     * 设置渲染函数
     */
    fun render(function: RenderFunction<PaginatedRenderContext<T>>) {
        this.renderFunction = function
    }

    /**
     * 设置空槽位渲染函数
     */
    fun renderEmptySlot(function: RenderFunction<PaginatedRenderContext<T>>) {
        this.emptySlotRenderFunction = function
    }

    /**
     * 设置加载中渲染函数（用于无限分页）
     */
    fun renderLoadingSlot(function: RenderFunction<PaginatedRenderContext<T>>) {
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
     * 跳转到指定页
     */
    fun goToPage(page: Int): Boolean {
        val newPage = if (isInfiniteMode) {
            page.coerceAtLeast(0)  // 无限模式：只限制最小值
        } else {
            page.coerceIn(0, totalPages - 1)  // 有限模式：限制范围
        }

        if (newPage != currentPage) {
            val previousPage = currentPage
            currentPage = newPage

            // 加载新页面数据
            loadPageData(currentPage)

            // 根据缓存策略预加载邻近页面
            preloadAdjacentPages()

            // 立即更新显示
            update()

            // 触发页面变更事件
            pageChangeHandler?.invoke(
                PageChangeContext(
                    previousPage = previousPage,
                    currentPage = currentPage,
                    previousIndex = previousPage * logicalItemsPerPage,
                    currentIndex = currentPage * logicalItemsPerPage,
                    currentSize = currentPageData.size,
                    pageSize = logicalItemsPerPage,
                    totalPages = if (isInfiniteMode) -1 else totalPages,
                    totalItems = if (isInfiniteMode) -1L else totalDataSize.toLong()
                )
            )
            return true
        }
        return false
    }

    /**
     * 下一页
     */
    fun nextPage(): Boolean {
        return operationLock.withLock {
            if (isInfiniteMode) {
                // 无限模式：同步检查是否还能获取更多数据
                val nextPageOffset = (currentPage + 1) * logicalItemsPerPage
                val semaphore = Semaphore(0)
                var canTurnPage = false
                
                dataProvider.canFetchMore(nextPageOffset) { canFetchResult ->
                    canFetchResult.fold(
                        onSuccess = { canFetch ->
                            if (canFetch) {
                                canTurnPage = goToPage(currentPage + 1)
                            }
                        },
                        onFailure = { /* 忽略错误，不进行翻页 */ }
                    )
                    semaphore.release()
                }
                
                // 等待异步操作完成
                semaphore.acquire()
                canTurnPage
            } else {
                // 有限模式：检查页面边界
                if (currentPage < totalPages - 1) {
                    goToPage(currentPage + 1)
                } else {
                    false
                }
            }
        }
    }

    /**
     * 上一页
     */
    fun previousPage(): Boolean {
        return if (currentPage > 0) {
            goToPage(currentPage - 1)
        } else {
            false
        }
    }

    /**
     * 第一页
     */
    fun firstPage(): Boolean {
        return goToPage(0)
    }

    /**
     * 最后一页（仅适用于有限分页）
     */
    fun lastPage(): Boolean {
        return if (isInfiniteMode) {
            false  // 无限模式没有最后一页
        } else {
            goToPage(totalPages - 1)
        }
    }

    /**
     * 获取当前页码
     */
    fun getCurrentPage(): Int = currentPage

    /**
     * 检查是否有下一页（同步方法，可能阻塞）
     */
    fun hasNextPage(): Boolean = if (isInfiniteMode) {
        // 无限模式：使用缓存的结果或返回true
        // 注意：这是一个同步方法，不适合进行异步检查
        true  // 简化处理，总是返回true让用户尝试
    } else {
        // 有限模式：检查页面边界
        currentPage < totalPages - 1
    }

    /**
     * 异步检查是否有下一页
     */
    fun hasNextPageAsync(callback: (Boolean) -> Unit) {
        if (isInfiniteMode) {
            val nextPageOffset = (currentPage + 1) * logicalItemsPerPage
            dataProvider.canFetchMore(nextPageOffset) { result ->
                result.fold(
                    onSuccess = { canFetch -> callback(canFetch) },
                    onFailure = { callback(false) }
                )
            }
        } else {
            callback(currentPage < totalPages - 1)
        }
    }

    /**
     * 检查是否有上一页
     */
    fun hasPreviousPage(): Boolean = currentPage > 0

    /**
     * 检查页面是否正在加载
     */
    fun isPageLoading(page: Int): Boolean = pageCache[page]?.state == PageState.LOADING

    /**
     * 检查页面是否已缓存
     */
    fun isPageCached(page: Int): Boolean = pageCache[page]?.state == PageState.LOADED

    /**
     * 获取页面状态
     */
    fun getPageState(page: Int): PageState = pageCache[page]?.state ?: PageState.NOT_LOADED

    /**
     * 获取组件状态
     */
    fun getComponentState(): ComponentState = componentState.get()

    /**
     * 获取当前缓存策略
     */
    fun getCacheStrategy(): CacheStrategy = dataProvider.getCacheStrategy()

    /**
     * 获取分页缓存统计信息
     */
    fun getPaginatedCacheStats(): String {
        val stats = getCacheStatsDetailed()
        return "PaginatedComponent Cache - Strategy: ${stats.strategy}, Pages: ${stats.cachedPages}/${stats.maxCacheSize}, Hit Rate: ${"%.2f".format(stats.cacheHitRate * 100)}%, Cached Pages: ${stats.cachedPageNumbers}"
    }

    /**
     * 获取详细的缓存统计信息
     */
    fun getCacheStatsDetailed(): CacheStats {
        return CacheStats(
            strategy = getCacheStrategy(),
            cachedPages = pageCache.size,
            maxCacheSize = pageCache.capacity,
            cacheHitRate = calculateCacheHitRate(),
            cachedPageNumbers = pageCache.keys.toList()
        )
    }

    /**
     * 计算缓存命中率（简化实现）
     */
    private fun calculateCacheHitRate(): Double {
        // 简化实现：基于当前缓存页数和缓存容量的比例
        return if (pageCache.capacity == 0) 0.0
               else pageCache.size.toDouble() / pageCache.capacity.coerceAtLeast(1)
    }

    /**
     * 强制清理缓存（除了当前页）
     */
    fun forceClearCache(keepCurrentPage: Boolean = true) {
        if (keepCurrentPage) {
            val currentEntry = pageCache[currentPage]
            pageCache.clear()
            if (currentEntry != null) {
                pageCache[currentPage] = currentEntry
            }
        } else {
            clearCache()
        }
    }

    override fun getSlots(): List<Int> {
        return potentialSlots // 返回所有可能的槽位，由 BasePage 处理覆盖逻辑
    }

    override fun getRenderContext(slot: Int): PaginatedRenderContext<T>? {
        return getRenderContext(slot, null)
    }

    override fun getRenderContext(slot: Int, oldItem: ItemStack?): PaginatedRenderContext<T>? {
        // 检查槽位是否属于这个组件
        if (!potentialSlots.contains(slot)) return null
        
        // 检查槽位是否被其他组件覆盖
        val ownerComponent = page.getComponentBySlot(slot)
        if (ownerComponent != null && ownerComponent != this) return null

        // 计算在可用槽位中的索引（统一使用可用槽位索引）
        val availableSlotIndex = availableSlots.indexOf(slot)
        if (availableSlotIndex == -1) return null // 槽位不可用

        val inventoryWidth = page.getInventoryWidth()
        val x = slot % inventoryWidth
        val y = slot / inventoryWidth
        val relativeX = x - startX
        val relativeY = y - startY
        
        // 统一使用可用槽位索引进行数据访问和计算
        val renderIndex = availableSlotIndex
        val globalIndex = currentPage * availableSlots.size + renderIndex
        val item = if (renderIndex < currentPageData.size) currentPageData[renderIndex] else null
        
        val pageState = pageCache[currentPage]?.state ?: PageState.NOT_LOADED
        val isLoading = pageState == PageState.LOADING
        val isPageCached = pageState == PageState.LOADED

        return PaginatedRenderContext(
            x = x,
            y = y,
            slot = slot,
            oldItem = oldItem,
            relativeX = relativeX,
            relativeY = relativeY,
            index = renderIndex, // 渲染索引（在可用槽位中的位置）
            pageIndex = currentPage,
            item = item,
            globalIndex = globalIndex, // 全局索引（基于可用槽位大小）
            isLoading = isLoading,
            isPageCached = isPageCached,
            isInfiniteMode = isInfiniteMode,
            pageState = pageState,
            componentState = componentState.get()
        )
    }

    override fun renderSlot(context: PaginatedRenderContext<T>): ItemStack? {
        return when {
            // 组件级别错误状态
            context.componentState == ComponentState.ERROR -> {
                // 可以显示错误图标或空槽位
                emptySlotRenderFunction?.invoke(context)
            }
            // 页面正在加载且没有数据时显示加载中
            context.pageState == PageState.LOADING && context.item == null -> {
                loadingSlotRenderFunction?.invoke(context)
            }
            // 页面加载失败
            context.pageState == PageState.ERROR -> {
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
        return "PaginatedComponent(startX=$startX, startY=$startY, width=$width, height=$height, currentPage=$currentPage, mode=${
            if(isInfiniteMode) "infinite" else "finite"}, totalPages=${if(isInfiniteMode) "∞" else totalPages
            }, dataSize=$totalDataSize, logicalPageSize=$logicalItemsPerPage, availableSlots=${availableSlots.size}, currentPageDataSize=${currentPageData.size}, cachedPages=${pageCache.size}, page=${page.title.toMiniMessage()})"
    }

    override fun close() {
        super.close()
    }
}

/**
 * 分页渲染上下文
 */
data class PaginatedRenderContext<T>(
    override val x: Int,
    override val y: Int,
    override val slot: Int,
    override val oldItem: ItemStack?,
    val relativeX: Int,
    val relativeY: Int,
    val index: Int,
    val pageIndex: Int,
    val item: T?,
    val globalIndex: Int,
    val isLoading: Boolean = false,
    val isPageCached: Boolean = false,
    val isInfiniteMode: Boolean = false,
    val pageState: PageState = PageState.NOT_LOADED,
    val componentState: PaginatedComponent.ComponentState = PaginatedComponent.ComponentState.INIT
) : BaseRenderContext()

/**
 * 页面变更上下文
 *
 * @param previousPage 之前的页码（从0开始，-1表示首次加载）
 * @param currentPage 当前页码（从0开始）
 * @param previousIndex 之前的全局索引（-1表示首次加载）
 * @param currentIndex 当前的全局索引
 * @param currentSize 当前页的实际大小
 * @param pageSize 每页的期望大小
 * @param totalPages 总页数（无限分页时为-1）
 * @param totalItems 总条目数（无限分页时为-1）
 */
data class PageChangeContext(
    val previousPage: Int,
    val currentPage: Int,
    val previousIndex: Int,
    val currentIndex: Int,
    val currentSize: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalItems: Long
)

/**
 * 渲染函数类型别名
 */
typealias RenderFunction<T> = (T) -> ItemStack?

/**
 * 缓存统计信息
 */
data class CacheStats(
    val strategy: CacheStrategy,
    val cachedPages: Int,
    val maxCacheSize: Int,
    val cacheHitRate: Double,
    val cachedPageNumbers: List<Int>
)
