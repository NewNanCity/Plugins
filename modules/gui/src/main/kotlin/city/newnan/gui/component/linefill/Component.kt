package city.newnan.gui.component.linefill

import city.newnan.core.utils.text.ComponentParseMode
import city.newnan.core.utils.text.toComponent
import city.newnan.core.utils.text.toMiniMessage
import city.newnan.gui.component.base.BaseComponent
import city.newnan.gui.component.base.BaseRenderContext
import city.newnan.gui.event.EventHandlers
import city.newnan.gui.page.BasePage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * 线条填充组件
 *
 * 用于在指定线条上填充相同的物品，特性：
 * - 水平或垂直线条填充
 * - 统一物品渲染
 * - 线条事件处理
 */
class LineFillComponent(
    page: BasePage,
    private val startX: Int,
    private val startY: Int,
    private val length: Int,
    private val direction: LineFillDirection
) : BaseComponent<LineFillRenderContext>(page) {

    // 事件处理器
    override val eventHandlers = EventHandlers()

    // 渲染函数
    private var renderFunction: RenderFunction<LineFillRenderContext>? = null

    // 计算所有槽位
    private val _slots: List<Int> by lazy {
        val result = mutableListOf<Int>()
        val inventoryWidth = page.getInventoryWidth()

        for (i in 0 until length) {
            val (x, y) = when (direction) {
                LineFillDirection.HORIZONTAL -> Pair(startX + i, startY)
                LineFillDirection.VERTICAL -> Pair(startX, startY + i)
            }

            val slot = y * inventoryWidth + x
            if (slot < page.inventory.size && x < inventoryWidth) {
                result.add(slot)
            }
        }
        result
    }

    init {
        // 验证参数
        if (length <= 0) {
            throw IllegalArgumentException("Length must be positive: length=$length")
        }

        if (startX < 0 || startY < 0) {
            throw IllegalArgumentException("Start coordinates must be non-negative: startX=$startX, startY=$startY")
        }

        // 验证线条是否在inventory范围内
        val inventoryWidth = page.getInventoryWidth()
        val inventoryHeight = page.inventory.size / inventoryWidth

        val (endX, endY) = when (direction) {
            LineFillDirection.HORIZONTAL -> Pair(startX + length - 1, startY)
            LineFillDirection.VERTICAL -> Pair(startX, startY + length - 1)
        }

        if (endX >= inventoryWidth || endY >= inventoryHeight) {
            throw IllegalArgumentException(
                "Line exceeds inventory bounds: " +
                "line=($startX,$startY) to ($endX,$endY), " +
                "inventory=(0,0,${inventoryWidth - 1},${inventoryHeight - 1})"
            )
        }
    }

    /**
     * 设置渲染函数
     */
    fun render(function: RenderFunction<LineFillRenderContext>) {
        this.renderFunction = function
    }

    /**
     * 设置固定物品
     */
    fun setItem(item: ItemStack?) {
        render { item }
    }

    /**
     * 填充物品（自动处理已有物品）
     * 如果槽位已有物品（oldItem不为null），则返回原有物品，否则使用新物品
     */
    fun fill(item: ItemStack) {
        render { context -> context.oldItem ?: item }
    }

    /**
     * 填充物品（自动处理已有物品）
     * 如果槽位已有物品（oldItem不为null），则返回原有物品，否则使用新物品
     */
    fun fill(material: Material) {
        fill(ItemStack(material).apply {
            itemMeta = itemMeta?.apply {
                displayName("".toComponent(ComponentParseMode.Plain))
            }
        })
    }

    /**
     * 获取起始X坐标
     */
    fun getStartX(): Int = startX

    /**
     * 获取起始Y坐标
     */
    fun getStartY(): Int = startY

    /**
     * 获取长度
     */
    fun getLength(): Int = length

    /**
     * 获取方向
     */
    fun getDirection(): LineFillDirection = direction

    /**
     * 检查坐标是否在线条上
     */
    fun containsCoordinate(x: Int, y: Int): Boolean {
        return when (direction) {
            LineFillDirection.HORIZONTAL -> y == startY && x >= startX && x < startX + length
            LineFillDirection.VERTICAL -> x == startX && y >= startY && y < startY + length
        }
    }

    override fun getSlots(): List<Int> {
        return _slots
    }

    override fun getRenderContext(slot: Int): LineFillRenderContext? {
        return getRenderContext(slot, null)
    }

    override fun getRenderContext(slot: Int, oldItem: ItemStack?): LineFillRenderContext? {
        if (!_slots.contains(slot)) return null

        val inventoryWidth = page.getInventoryWidth()
        val x = slot % inventoryWidth
        val y = slot / inventoryWidth

        val index = when (direction) {
            LineFillDirection.HORIZONTAL -> x - startX
            LineFillDirection.VERTICAL -> y - startY
        }

        return LineFillRenderContext(
            x = x,
            y = y,
            slot = slot,
            oldItem = oldItem,
            index = index,
            direction = direction
        )
    }

    override fun renderSlot(context: LineFillRenderContext): ItemStack? {
        return renderFunction?.invoke(context)
    }

    override fun toString(): String {
        return "LineFillComponent(startX=$startX, startY=$startY, length=$length, direction=$direction, slots=${_slots.size}, page=${page.title.toMiniMessage()})"
    }
}

/**
 * 线条方向枚举
 */
enum class LineFillDirection {
    HORIZONTAL, // 水平方向
    VERTICAL    // 垂直方向
}

/**
 * 线条填充渲染上下文
 */
data class LineFillRenderContext(
    override val x: Int,
    override val y: Int,
    override val slot: Int,
    override val oldItem: ItemStack?,
    val index: Int,
    val direction: LineFillDirection
) : BaseRenderContext()

/**
 * 渲染函数类型别名
 */
typealias RenderFunction<T> = (T) -> ItemStack?
