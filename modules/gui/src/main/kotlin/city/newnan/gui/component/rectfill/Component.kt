package city.newnan.gui.component.rectfill

import city.newnan.core.utils.text.toMiniMessage
import city.newnan.gui.component.base.BaseComponent
import city.newnan.gui.component.base.BaseRenderContext
import city.newnan.gui.event.EventHandlers
import city.newnan.gui.page.BasePage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * 矩形填充组件
 *
 * 用于在指定矩形区域内填充相同的物品，特性：
 * - 矩形区域填充
 * - 统一物品渲染
 * - 区域事件处理
 */
class RectFillComponent(
    page: BasePage,
    private val startX: Int,
    private val startY: Int,
    private val width: Int,
    private val height: Int
) : BaseComponent<RectFillRenderContext>(page) {

    // 事件处理器
    override val eventHandlers = EventHandlers()

    // 渲染函数
    private var renderFunction: RenderFunction<RectFillRenderContext>? = null

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
                "Rect area exceeds inventory bounds: " +
                "area=($startX,$startY,${startX + width - 1},${startY + height - 1}), " +
                "inventory=(0,0,${inventoryWidth - 1},${inventoryHeight - 1})"
            )
        }
    }

    /**
     * 设置渲染函数
     */
    fun render(function: RenderFunction<RectFillRenderContext>) {
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
        fill(ItemStack(material))
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
     * 获取宽度
     */
    fun getWidth(): Int = width

    /**
     * 获取高度
     */
    fun getHeight(): Int = height

    /**
     * 获取结束X坐标（不包含）
     */
    fun getEndX(): Int = startX + width

    /**
     * 获取结束Y坐标（不包含）
     */
    fun getEndY(): Int = startY + height

    /**
     * 检查坐标是否在区域内
     */
    fun containsCoordinate(x: Int, y: Int): Boolean {
        return x >= startX && x < startX + width && y >= startY && y < startY + height
    }

    override fun getSlots(): List<Int> {
        return _slots
    }

    override fun getRenderContext(slot: Int): RectFillRenderContext? {
        return getRenderContext(slot, null)
    }

    override fun getRenderContext(slot: Int, oldItem: ItemStack?): RectFillRenderContext? {
        if (!_slots.contains(slot)) return null

        val inventoryWidth = page.getInventoryWidth()
        val x = slot % inventoryWidth
        val y = slot / inventoryWidth
        val relativeX = x - startX
        val relativeY = y - startY
        val index = relativeY * width + relativeX

        return RectFillRenderContext(
            x = x,
            y = y,
            slot = slot,
            oldItem = oldItem,
            relativeX = relativeX,
            relativeY = relativeY,
            index = index
        )
    }

    override fun renderSlot(context: RectFillRenderContext): ItemStack? {
        return renderFunction?.invoke(context)
    }

    override fun toString(): String {
        return "RectFillComponent(startX=$startX, startY=$startY, width=$width, height=$height, slots=${_slots.size}, page=${page.title.toMiniMessage()})"
    }
}

/**
 * 矩形填充渲染上下文
 */
data class RectFillRenderContext(
    override val x: Int,
    override val y: Int,
    override val slot: Int,
    override val oldItem: ItemStack?,
    val relativeX: Int,
    val relativeY: Int,
    val index: Int
) : BaseRenderContext()

/**
 * 渲染函数类型别名
 */
typealias RenderFunction<T> = (T) -> ItemStack?
