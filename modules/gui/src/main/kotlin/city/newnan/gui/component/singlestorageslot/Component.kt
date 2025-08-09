package city.newnan.gui.component.singlestorageslot

import city.newnan.core.utils.text.toMiniMessage
import city.newnan.gui.component.base.BaseComponent
import city.newnan.gui.component.base.BaseRenderContext
import city.newnan.gui.component.IStorageComponent
import city.newnan.gui.event.EventHandlers
import city.newnan.gui.page.BasePage
import org.bukkit.inventory.ItemStack

/**
 * 单存储槽位组件
 *
 * 用于处理单个可存储物品的槽位，特性：
 * - 固定槽位位置
 * - 允许玩家放入/取出物品
 * - 存储状态管理
 * - 物品验证
 */
class SingleStorageSlotComponent(
    page: BasePage,
    private val x: Int,
    private val y: Int
) : BaseComponent<SingleStorageSlotRenderContext>(page), IStorageComponent {

    // 事件处理器
    override val eventHandlers = EventHandlers()

    // 物品验证函数
    private var itemValidator: ItemValidator? = null

    // 物品变化监听器
    private var itemChangeListener: ItemChangeListener? = null

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
     * 设置物品验证函数
     * 用于验证玩家是否可以放入特定物品
     */
    fun setItemValidator(validator: ItemValidator) {
        this.itemValidator = validator
    }

    /**
     * 设置物品变化监听器
     * 当槽位中的物品发生变化时调用
     */
    fun onItemChange(listener: ItemChangeListener) {
        this.itemChangeListener = listener
    }

    /**
     * 检查物品是否可以放入
     */
    override fun canAcceptItem(item: ItemStack?): Boolean {
        return itemValidator?.invoke(item) ?: true
    }

    override fun handleItemChange(
        slot: Int,
        oldItem: ItemStack?,
        newItem: ItemStack?
    ) {
        this.itemChangeListener?.invoke(oldItem, newItem)
    }

    /**
     * 获取当前存储的物品
     */
    override fun getStoredItem(slot: Int): ItemStack? {
        if (page.getComponentBySlot(slot) != this) return null
        return page.inventory.getItem(slot)
    }

    /**
     * 设置存储的物品
     */
    override fun setStoredItem(slot: Int, item: ItemStack?) {
        if (page.getComponentBySlot(slot) != this) return
        page.inventory.setItem(slot, item)
    }

    /**
     * 获取当前存储的物品
     */
    fun getStoredItem(): ItemStack? = getStoredItem(_slot)

    /**
     * 设置存储的物品
     */
    fun setStoredItem(item: ItemStack?) = setStoredItem(_slot, item)

    /**
     * 清空存储的物品
     */
    fun clearStoredItem() {
        setStoredItem(_slot, null)
    }

    /**
     * 检查是否为存储组件
     */
    override fun isStorageComponent(): Boolean = true

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

    override fun getRenderContext(slot: Int): SingleStorageSlotRenderContext? {
        return getRenderContext(slot, null)
    }

    override fun getRenderContext(slot: Int, oldItem: ItemStack?): SingleStorageSlotRenderContext? {
        if (slot != this._slot) return null

        return SingleStorageSlotRenderContext(
            x = x,
            y = y,
            slot = slot,
            oldItem = oldItem,
            storedItem = getStoredItem(_slot)
        )
    }

    override fun renderSlot(context: SingleStorageSlotRenderContext): ItemStack? {
        return context.storedItem
    }

    override fun toString(): String {
        return "SingleStorageSlotComponent(x=$x, y=$y, slot=$_slot, hasItem=${getStoredItem(_slot) != null}, page=${page.title.toMiniMessage()})"
    }
}

/**
 * 单存储槽位渲染上下文
 */
data class SingleStorageSlotRenderContext(
    override val x: Int,
    override val y: Int,
    override val slot: Int,
    override val oldItem: ItemStack?,
    val storedItem: ItemStack?
) : BaseRenderContext()

/**
 * 物品验证函数类型别名
 * 返回true表示可以接受该物品，false表示拒绝
 */
typealias ItemValidator = (ItemStack?) -> Boolean

/**
 * 物品变化监听器类型别名
 * 当槽位中的物品发生变化时调用
 */
typealias ItemChangeListener = (oldItem: ItemStack?, newItem: ItemStack?) -> Unit
