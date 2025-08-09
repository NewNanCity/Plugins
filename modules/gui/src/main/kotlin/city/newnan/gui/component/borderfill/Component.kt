package city.newnan.gui.component.borderfill

import city.newnan.core.utils.text.toMiniMessage
import city.newnan.gui.component.base.BaseComponent
import city.newnan.gui.component.base.BaseRenderContext
import city.newnan.gui.event.EventHandlers
import city.newnan.gui.page.BasePage
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * 边框填充组件
 *
 * 用于在指定矩形区域的边框填充物品，特性：
 * - 边框区域填充
 * - 边框类型识别
 * - 角落特殊处理
 * - 边框事件处理
 */
class BorderFillComponent(
    page: BasePage,
    private val startX: Int,
    private val startY: Int,
    private val width: Int,
    private val height: Int
) : BaseComponent<BorderFillRenderContext>(page) {

    // 事件处理器
    override val eventHandlers = EventHandlers()

    // 渲染函数
    private var renderFunction: RenderFunction<BorderFillRenderContext>? = null

    // 计算所有边框槽位
    private val _slots: List<Int> by lazy {
        val result = mutableListOf<Int>()
        val inventoryWidth = page.getInventoryWidth()

        for (y in startY until startY + height) {
            for (x in startX until startX + width) {
                // 只有边框位置才添加到槽位列表
                if (isBorderPosition(x - startX, y - startY)) {
                    val slot = y * inventoryWidth + x
                    if (slot < page.inventory.size) {
                        result.add(slot)
                    }
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
                "Border area exceeds inventory bounds: " +
                "area=($startX,$startY,${startX + width - 1},${startY + height - 1}), " +
                "inventory=(0,0,${inventoryWidth - 1},${inventoryHeight - 1})"
            )
        }
    }

    /**
     * 检查相对坐标是否为边框位置
     */
    private fun isBorderPosition(relativeX: Int, relativeY: Int): Boolean {
        return relativeX == 0 || relativeX == width - 1 || relativeY == 0 || relativeY == height - 1
    }

    /**
     * 获取边框类型
     */
    private fun getBorderType(relativeX: Int, relativeY: Int): BorderType {
        return when {
            relativeX == 0 && relativeY == 0 -> BorderType.TOP_LEFT
            relativeX == width - 1 && relativeY == 0 -> BorderType.TOP_RIGHT
            relativeX == 0 && relativeY == height - 1 -> BorderType.BOTTOM_LEFT
            relativeX == width - 1 && relativeY == height - 1 -> BorderType.BOTTOM_RIGHT
            relativeY == 0 -> BorderType.TOP
            relativeY == height - 1 -> BorderType.BOTTOM
            relativeX == 0 -> BorderType.LEFT
            relativeX == width - 1 -> BorderType.RIGHT
            else -> BorderType.UNKNOWN
        }
    }

    /**
     * 设置渲染函数
     */
    fun render(function: RenderFunction<BorderFillRenderContext>) {
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
     * 检查坐标是否在边框上
     */
    fun containsCoordinate(x: Int, y: Int): Boolean {
        val relativeX = x - startX
        val relativeY = y - startY
        return relativeX >= 0 && relativeX < width &&
               relativeY >= 0 && relativeY < height &&
               isBorderPosition(relativeX, relativeY)
    }

    override fun getSlots(): List<Int> {
        return _slots
    }

    override fun getRenderContext(slot: Int): BorderFillRenderContext? {
        return getRenderContext(slot, null)
    }

    override fun getRenderContext(slot: Int, oldItem: ItemStack?): BorderFillRenderContext? {
        if (!_slots.contains(slot)) return null

        val inventoryWidth = page.getInventoryWidth()
        val x = slot % inventoryWidth
        val y = slot / inventoryWidth
        val relativeX = x - startX
        val relativeY = y - startY
        val borderType = getBorderType(relativeX, relativeY)

        return BorderFillRenderContext(
            x = x,
            y = y,
            slot = slot,
            oldItem = oldItem,
            relativeX = relativeX,
            relativeY = relativeY,
            borderType = borderType
        )
    }

    override fun renderSlot(context: BorderFillRenderContext): ItemStack? {
        return renderFunction?.invoke(context)
    }

    override fun toString(): String {
        return "BorderFillComponent(startX=$startX, startY=$startY, width=$width, height=$height, borderSlots=${_slots.size}, page=${page.title.toMiniMessage()})"
    }
}

/**
 * 边框类型枚举
 */
enum class BorderType {
    TOP,           // 顶部边框
    BOTTOM,        // 底部边框
    LEFT,          // 左侧边框
    RIGHT,         // 右侧边框
    TOP_LEFT,      // 左上角
    TOP_RIGHT,     // 右上角
    BOTTOM_LEFT,   // 左下角
    BOTTOM_RIGHT,  // 右下角
    UNKNOWN        // 未知类型
}

/**
 * 边框填充渲染上下文
 */
data class BorderFillRenderContext(
    override val x: Int,
    override val y: Int,
    override val slot: Int,
    override val oldItem: ItemStack?,
    val relativeX: Int,
    val relativeY: Int,
    val borderType: BorderType
) : BaseRenderContext()

/**
 * 渲染函数类型别名
 */
typealias RenderFunction<T> = (T) -> ItemStack?
