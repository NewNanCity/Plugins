package city.newnan.gui.event

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

/**
 * 事件上下文基础类
 *
 * 使用泛型事件上下文统一处理不同类型的事件。
 * 支持事件传播控制和冒泡机制。
 *
 * @param T 具体的Bukkit事件类型
 */
open class EventContext<T : Event>(
    val event: T,
    val player: Player,
    private var propagationStopped: Boolean = false
) {
    /**
     * 停止事件传播
     * 阻止事件继续向上冒泡
     */
    fun stopPropagation() {
        propagationStopped = true
    }

    /**
     * 检查事件传播是否已停止
     */
    internal fun isPropagationStopped(): Boolean = propagationStopped
}

/**
 * 点击事件上下文
 * 用于处理InventoryClickEvent
 */
class ClickEventContext(
    event: InventoryClickEvent,
    player: Player,
    val slot: Int,
    val clickType: ClickType,
    val item: ItemStack?
) : EventContext<InventoryClickEvent>(event, player) {

    /**
     * 是否为左键点击
     */
    val isLeftClick: Boolean = clickType == ClickType.LEFT

    /**
     * 是否为右键点击
     */
    val isRightClick: Boolean = clickType == ClickType.RIGHT

    /**
     * 是否为Shift点击
     */
    val isShiftClick: Boolean = clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT

    /**
     * 是否为中键点击
     */
    val isMiddleClick: Boolean = clickType == ClickType.MIDDLE

    /**
     * 是否为双击
     */
    val isDoubleClick: Boolean = clickType == ClickType.DOUBLE_CLICK
}

/**
 * 拖拽事件上下文
 * 用于处理InventoryDragEvent
 */
class DragEventContext(
    event: InventoryDragEvent,
    player: Player,
    val dragType: DragType,
    val slots: Set<Int>,
    val items: Map<Int, ItemStack>
) : EventContext<InventoryDragEvent>(event, player) {

    /**
     * 是否为单个物品拖拽
     */
    val isSingle: Boolean = dragType == DragType.SINGLE

    /**
     * 是否为平均分配拖拽
     */
    val isEven: Boolean = dragType == DragType.EVEN
}

/**
 * 显示事件上下文
 * 当GUI显示给玩家时触发
 */
class ShowEventContext(
    player: Player
) : EventContext<Event>(object : Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = org.bukkit.event.HandlerList()
}, player)

/**
 * 隐藏事件上下文
 * 当GUI从玩家视图中隐藏时触发
 */
class HideEventContext(
    player: Player,
    val reason: String? = null
) : EventContext<Event>(object : Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = org.bukkit.event.HandlerList()
}, player)

/**
 * 初始化事件上下文
 * 当Component或Page初始化时触发
 */
class InitEventContext(
    player: Player
) : EventContext<Event>(object : Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = org.bukkit.event.HandlerList()
}, player)

/**
 * 销毁事件上下文
 * 当Component或Page销毁时触发
 */
class DestroyEventContext(
    player: Player,
    val reason: String? = null
) : EventContext<Event>(object : Event() {
    override fun getHandlers(): org.bukkit.event.HandlerList = org.bukkit.event.HandlerList()
}, player)

/**
 * 制作事件上下文
 * 用于处理CraftItemEvent
 */
class CraftEventContext(
    event: CraftItemEvent,
    player: Player,
    val recipe: org.bukkit.inventory.Recipe?,
    val result: ItemStack?
) : EventContext<CraftItemEvent>(event, player)

/**
 * 准备制作事件上下文
 * 用于处理PrepareItemCraftEvent
 */
class PrepareCraftEventContext(
    event: PrepareItemCraftEvent,
    player: Player,
    val recipe: org.bukkit.inventory.Recipe?,
    val result: ItemStack?
) : EventContext<PrepareItemCraftEvent>(event, player)

/**
 * 熔炉冶炼事件上下文
 * 用于处理FurnaceSmeltEvent
 */
class SmeltEventContext(
    event: FurnaceSmeltEvent,
    player: Player,
    val source: ItemStack,
    val result: ItemStack
) : EventContext<FurnaceSmeltEvent>(event, player)

/**
 * 酿造事件上下文
 * 用于处理BrewEvent
 */
class BrewEventContext(
    event: BrewEvent,
    player: Player,
    val contents: Array<ItemStack?>
) : EventContext<BrewEvent>(event, player)

/**
 * 玩家丢弃物品事件上下文
 * 用于处理PlayerDropItemEvent
 */
class DropEventContext(
    event: PlayerDropItemEvent,
    player: Player,
    val item: ItemStack
) : EventContext<PlayerDropItemEvent>(event, player)

/**
 * 特殊Inventory事件上下文
 */

/**
 * 物品移动事件上下文
 * 用于处理InventoryMoveItemEvent
 */
class ItemMoveEventContext(
    event: InventoryMoveItemEvent,
    player: Player,
    val item: ItemStack,
    val source: Inventory,
    val destination: Inventory
) : EventContext<InventoryMoveItemEvent>(event, player)

/**
 * 物品拾取事件上下文
 * 用于处理InventoryPickupItemEvent
 */
class PickupItemEventContext(
    event: InventoryPickupItemEvent,
    player: Player,
    val item: org.bukkit.entity.Item
) : EventContext<InventoryPickupItemEvent>(event, player)

/**
 * 创造模式事件上下文
 * 用于处理InventoryCreativeEvent
 */
class CreativeEventContext(
    event: InventoryCreativeEvent,
    player: Player,
    val slot: Int,
    val cursor: ItemStack?
) : EventContext<InventoryCreativeEvent>(event, player)

/**
 * 制作相关事件上下文
 */

/**
 * 准备铁砧事件上下文
 * 用于处理PrepareAnvilEvent
 */
class PrepareAnvilEventContext(
    event: PrepareAnvilEvent,
    player: Player,
    val result: ItemStack?,
    val repairCost: Int
) : EventContext<PrepareAnvilEvent>(event, player)

/**
 * 准备砂轮事件上下文
 * 用于处理PrepareGrindstoneEvent
 */
class PrepareGrindstoneEventContext(
    event: PrepareGrindstoneEvent,
    player: Player,
    val result: ItemStack?
) : EventContext<PrepareGrindstoneEvent>(event, player)

/**
 * 准备锻造事件上下文
 * 用于处理PrepareSmithingEvent
 */
class PrepareSmithingEventContext(
    event: PrepareSmithingEvent,
    player: Player,
    val result: ItemStack?
) : EventContext<PrepareSmithingEvent>(event, player)

/**
 * 锻造物品事件上下文
 * 用于处理SmithItemEvent
 */
class SmithItemEventContext(
    event: SmithItemEvent,
    player: Player,
    val recipe: org.bukkit.inventory.SmithingRecipe?,
    val result: ItemStack?
) : EventContext<SmithItemEvent>(event, player)

/**
 * 熔炉相关事件上下文
 */

/**
 * 熔炉燃烧事件上下文
 * 用于处理FurnaceBurnEvent
 */
class FurnaceBurnEventContext(
    event: FurnaceBurnEvent,
    player: Player,
    val fuel: ItemStack,
    val burnTime: Int
) : EventContext<FurnaceBurnEvent>(event, player)

/**
 * 熔炉提取事件上下文
 * 用于处理FurnaceExtractEvent
 */
class FurnaceExtractEventContext(
    event: FurnaceExtractEvent,
    player: Player,
    val itemType: Material,
    val itemAmount: Int,
    val exp: Int
) : EventContext<FurnaceExtractEvent>(event, player)

/**
 * 熔炉开始冶炼事件上下文
 * 用于处理FurnaceStartSmeltEvent
 */
class FurnaceStartSmeltEventContext(
    event: FurnaceStartSmeltEvent,
    player: Player,
    val source: ItemStack,
    val recipe: org.bukkit.inventory.FurnaceRecipe?
) : EventContext<FurnaceStartSmeltEvent>(event, player)

/**
 * 酿造相关事件上下文
 */

/**
 * 酿造台燃料事件上下文
 * 用于处理BrewingStandFuelEvent
 */
class BrewingFuelEventContext(
    event: BrewingStandFuelEvent,
    player: Player,
    val fuel: ItemStack,
    val fuelPower: Int
) : EventContext<BrewingStandFuelEvent>(event, player)

/**
 * 其他事件上下文
 */

/**
 * 交易选择事件上下文
 * 用于处理TradeSelectEvent
 */
class TradeSelectEventContext(
    event: TradeSelectEvent,
    player: Player,
    val merchant: org.bukkit.inventory.Merchant,
    val recipe: org.bukkit.inventory.MerchantRecipe
) : EventContext<TradeSelectEvent>(event, player)

/**
 * Book相关事件上下文
 */

/**
 * 书籍翻页事件上下文
 * 用于处理书籍翻页操作（基于InventoryClickEvent）
 */
class BookPageTurnEventContext(
    event: InventoryClickEvent,
    player: Player,
    val book: ItemStack,
    val bookMeta: BookMeta,
    val currentPage: Int,
    val isNextPage: Boolean // true表示翻到下一页，false表示翻到上一页
) : EventContext<InventoryClickEvent>(event, player)

/**
 * 书籍编辑事件上下文
 * 用于处理PlayerEditBookEvent
 */
class BookEditEventContext(
    event: PlayerEditBookEvent,
    player: Player,
    val slot: Int,
    val previousBookMeta: BookMeta?,
    val newBookMeta: BookMeta?,
    val isSigning: Boolean
) : EventContext<PlayerEditBookEvent>(event, player)

/**
 * 书籍签名事件上下文
 * 用于处理书籍签名完成
 */
class BookSignEventContext(
    event: PlayerEditBookEvent,
    player: Player,
    val slot: Int,
    val title: String,
    val author: String,
    val pages: List<String>
) : EventContext<PlayerEditBookEvent>(event, player)