package city.newnan.gui.component.singleslot

import city.newnan.core.utils.text.toMiniMessage
import city.newnan.gui.component.base.BaseComponent
import city.newnan.gui.component.base.BaseRenderContext
import city.newnan.gui.event.EventHandlers
import city.newnan.gui.page.BasePage
import org.bukkit.inventory.ItemStack

/**
 * 单槽位组件
 *
 * 用于处理单个槽位的显示和交互，特性：
 * - 固定槽位位置
 * - 简单的物品渲染
 * - 基础事件处理
 */
class SingleSlotComponent(
    page: BasePage,
    private val x: Int,
    private val y: Int
) : BaseComponent<SingleSlotRenderContext>(page) {

    // 事件处理器
    override val eventHandlers = EventHandlers()

    // 渲染函数
    private var renderFunction: RenderFunction<SingleSlotRenderContext>? = null

    // 计算槽位
    private val _slot: Int by lazy {
        val inventoryWidth = page.getInventoryWidth()
        y * inventoryWidth + x
    }

    init {
        // 验证参数
        if (x < 0 || y < 0) {
            throw IllegalArgumentException("Coordinates must be non-negative: x=$x, y=$y")
        }

        // 验证槽位是否在inventory范围内
        val inventoryWidth = page.getInventoryWidth()
        val inventoryHeight = page.inventory.size / inventoryWidth

        if (x >= inventoryWidth || y >= inventoryHeight) {
            throw IllegalArgumentException(
                "Slot coordinates exceed inventory bounds: " +
                "slot=($x,$y), inventory=(0,0,${inventoryWidth - 1},${inventoryHeight - 1})"
            )
        }

        if (_slot >= page.inventory.size) {
            throw IllegalArgumentException(
                "Calculated slot $_slot exceeds inventory size ${page.inventory.size}"
            )
        }
    }

    /**
     * 设置渲染函数
     */
    fun render(function: RenderFunction<SingleSlotRenderContext>) {
        this.renderFunction = function
    }

    /**
     * 设置固定物品
     */
    fun setItem(item: ItemStack?) {
        render { item }
    }

    /**
     * 获取X坐标
     */
    fun getX(): Int = x

    /**
     * 获取Y坐标
     */
    fun getY(): Int = y

    /**
     * 获取槽位号
     */
    fun getSlot(): Int = _slot

    override fun getSlots(): List<Int> {
        return listOf(_slot)
    }

    override fun getRenderContext(slot: Int): SingleSlotRenderContext? {
        return getRenderContext(slot, null)
    }

    override fun getRenderContext(slot: Int, oldItem: ItemStack?): SingleSlotRenderContext? {
        if (slot != this._slot) return null

        return SingleSlotRenderContext(
            x = x,
            y = y,
            slot = slot,
            oldItem = oldItem
        )
    }

    override fun renderSlot(context: SingleSlotRenderContext): ItemStack? {
        return renderFunction?.invoke(context)
    }

    override fun toString(): String {
        return "SingleSlotComponent(x=$x, y=$y, slot=$_slot, page=${page.title.toMiniMessage()})"
    }
}

/**
 * 单槽位渲染上下文
 */
data class SingleSlotRenderContext(
    override val x: Int,
    override val y: Int,
    override val slot: Int,
    override val oldItem: ItemStack?
) : BaseRenderContext()

/**
 * 渲染函数类型别名
 */
typealias RenderFunction<T> = (T) -> ItemStack?
