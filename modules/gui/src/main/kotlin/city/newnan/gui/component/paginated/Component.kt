package city.newnan.gui.component.paginated

import city.newnan.core.utils.text.toMiniMessage
import city.newnan.gui.component.base.BaseComponent
import city.newnan.gui.component.base.BaseRenderContext
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

    // 当前页的数据
    @Volatile
    private var currentPageData: List<T> = emptyList()

    // 是否为无限分页模式
    private val isInfiniteMode: Boolean get() = totalDataSize < 0

    // 操作锁，确保异步操作的原子性
    private val operationLock = ReentrantLock()

    // 总页数（基于逻辑页面大小）
    val totalPages: Int get() = if (isInfiniteMode) Int.MAX_VALUE else ceil(totalDataSize.toDouble() / logicalItemsPerPage).toInt()

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

        // 初始化数据
        refreshData()
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
                    // 设置组件状态为就绪
                    componentState.set(ComponentState.READY)
                    // 加载当前页数据
                    loadPageData(currentPage)
                },
                onFailure = { error ->
                    // 处理数据加载错误
                    totalDataSize = 0
                    currentPageData = emptyList()
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
        val offset = page * logicalItemsPerPage
        dataProvider.fetchItems(offset, logicalItemsPerPage) { dataResult ->
            dataResult.fold(
                onSuccess = { data ->
                    // 如果是当前页，更新数据并刷新显示
                    if (page == currentPage) {
                        currentPageData = data
                        update()
                    }
                },
                onFailure = { error ->
                    // 如果是当前页，清空数据并刷新显示
                    if (page == currentPage) {
                        currentPageData = emptyList()
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

            // 原子性地更新所有状态
            this.dataProvider = newDataProvider
            this.currentPage = 0
            this.totalDataSize = -1
            this.currentPageData = emptyList()

            // 立即更新显示加载状态
            update()

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
     * 刷新当前页
     */
    fun refresh() {
        // 清空当前数据并立即更新显示
        currentPageData = emptyList()
        update()
        // 重新加载数据
        loadPageData(currentPage)
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

            // 清空当前页数据并立即更新显示
            currentPageData = emptyList()
            update()

            // 加载新页面数据
            loadPageData(currentPage)

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
     * 获取组件状态
     */
    fun getComponentState(): ComponentState = componentState.get()

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
            isInfiniteMode = isInfiniteMode,
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
            // 组件正在加载且没有数据时显示加载中
            context.componentState == ComponentState.LOADING && context.item == null -> {
                loadingSlotRenderFunction?.invoke(context)
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
            }, dataSize=$totalDataSize, logicalPageSize=$logicalItemsPerPage, availableSlots=${availableSlots.size}, currentPageDataSize=${currentPageData.size}, page=${page.title.toMiniMessage()})"
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
    val isInfiniteMode: Boolean = false,
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
