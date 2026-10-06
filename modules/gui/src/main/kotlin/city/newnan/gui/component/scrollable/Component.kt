package city.newnan.gui.component.scrollable

import city.newnan.core.utils.text.toMiniMessage
import city.newnan.gui.component.base.BaseComponent
import city.newnan.gui.component.base.BaseRenderContext
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

    // 当前可见区域的数据
    @Volatile
    private var visibleData: Map<Int, List<T>> = emptyMap()

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
                    // 加载当前可见区域的数据
                    loadVisibleData()
                },
                onFailure = { error ->
                    // 处理数据加载错误
                    totalDataSize = 0
                    visibleData = emptyMap()
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
        val offset = row * itemsPerRow
        dataProvider.fetchItems(offset, itemsPerRow) { dataResult ->
            dataResult.fold(
                onSuccess = { data ->
                    // 如果是当前可见区域，更新数据并刷新显示
                    if (row >= scrollOffset && row < scrollOffset + visibleRows) {
                        val newVisibleData = visibleData.toMutableMap()
                        newVisibleData[row] = data
                        visibleData = newVisibleData
                        update()
                    }
                },
                onFailure = { error ->
                    // 如果是当前可见区域，清空数据并刷新显示
                    if (row >= scrollOffset && row < scrollOffset + visibleRows) {
                        val newVisibleData = visibleData.toMutableMap()
                        newVisibleData.remove(row)
                        visibleData = newVisibleData
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
            this.scrollOffset = 0
            this.totalDataSize = -1
            this.visibleData = emptyMap()

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
     * 清空数据（便利方法）
     */
    fun clearData() {
        setData(emptyList())
    }

    /**
     * 刷新当前可见区域
     */
    fun refresh() {
        // 清空当前可见数据并立即更新显示
        visibleData = emptyMap()
        update()
        // 重新加载所有可见行数据
        for (row in scrollOffset until scrollOffset + visibleRows) {
            loadRowData(row)
        }
    }

    /**
     * 刷新指定行
     */
    fun refreshRow(row: Int) {
        // 如果是可见行，先清空数据并更新显示
        if (row >= scrollOffset && row < scrollOffset + visibleRows) {
            val newVisibleData = visibleData.toMutableMap()
            newVisibleData.remove(row)
            visibleData = newVisibleData
            update()
        }
        // 重新加载数据
        loadRowData(row)
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

            // 清空旧的可见数据并立即更新显示
            visibleData = emptyMap()
            update()

            // 加载新的可见区域数据
            loadVisibleData()

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
     * 获取指定行的数据
     */
    fun getRowData(row: Int): List<T> = visibleData[row] ?: emptyList()

    /**
     * 获取组件状态
     */
    fun getComponentState(): ComponentState = componentState.get()

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
        val rowData = visibleData[dataRow] ?: emptyList()
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
            isInfiniteMode = isInfiniteMode,
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
        return "ScrollableComponent(startX=$startX, startY=$startY, width=$width, height=$height, scrollOffset=$scrollOffset, maxOffset=${
            if(isInfiniteMode) "∞" else maxScrollOffset}, mode=${if(isInfiniteMode) "infinite" else "finite"}, totalRows=${
                if(isInfiniteMode) "∞" else totalRows}, dataSize=$totalDataSize, visibleDataRows=${visibleData.size}, page=${page.title.toMiniMessage()})"
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
    val isInfiniteMode: Boolean = false,
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
