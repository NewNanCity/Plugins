package city.newnan.gui.event

import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap

/**
 * 事件处理器类型别名
 *
 * 定义各种事件处理器的函数签名
 */

// 点击事件处理器
typealias ClickHandler = (ClickEventContext) -> Unit
typealias LeftClickHandler = (ClickEventContext) -> Unit
typealias RightClickHandler = (ClickEventContext) -> Unit
typealias ShiftClickHandler = (ClickEventContext) -> Unit
typealias ShiftLeftClickHandler = (ClickEventContext) -> Unit
typealias ShiftRightClickHandler = (ClickEventContext) -> Unit
typealias MiddleClickHandler = (ClickEventContext) -> Unit
typealias DoubleClickHandler = (ClickEventContext) -> Unit

// 拖拽事件处理器
typealias DragHandler = (DragEventContext) -> Unit
typealias DragSingleHandler = (DragEventContext) -> Unit
typealias DragEvenHandler = (DragEventContext) -> Unit

// 生命周期事件处理器
typealias ShowHandler = (ShowEventContext) -> Unit
typealias HideHandler = (HideEventContext) -> Unit
typealias InitHandler = (InitEventContext) -> Unit
typealias DestroyHandler = (DestroyEventContext) -> Unit

// 制作相关事件处理器
typealias CraftHandler = (CraftEventContext) -> Unit
typealias PrepareCraftHandler = (PrepareCraftEventContext) -> Unit
typealias PrepareAnvilHandler = (PrepareAnvilEventContext) -> Unit
typealias PrepareGrindstoneHandler = (PrepareGrindstoneEventContext) -> Unit
typealias PrepareSmithingHandler = (PrepareSmithingEventContext) -> Unit
typealias SmithItemHandler = (SmithItemEventContext) -> Unit

// 熔炉相关事件处理器
typealias SmeltHandler = (SmeltEventContext) -> Unit
typealias FurnaceBurnHandler = (FurnaceBurnEventContext) -> Unit
typealias FurnaceExtractHandler = (FurnaceExtractEventContext) -> Unit
typealias FurnaceStartSmeltHandler = (FurnaceStartSmeltEventContext) -> Unit

// 酿造相关事件处理器
typealias BrewHandler = (BrewEventContext) -> Unit
typealias BrewingFuelHandler = (BrewingFuelEventContext) -> Unit

// 特殊Inventory事件处理器
typealias ItemMoveHandler = (ItemMoveEventContext) -> Unit
typealias PickupItemHandler = (PickupItemEventContext) -> Unit
typealias CreativeHandler = (CreativeEventContext) -> Unit

// 其他事件处理器
typealias DropHandler = (DropEventContext) -> Unit
typealias TradeSelectHandler = (TradeSelectEventContext) -> Unit

// Book相关事件处理器
typealias BookPageTurnHandler = (BookPageTurnEventContext) -> Unit
typealias BookEditHandler = (BookEditEventContext) -> Unit
typealias BookSignHandler = (BookSignEventContext) -> Unit

// 组件特定事件处理器
// 注意：这些类型别名现在在各自的组件文件中定义，这里保留是为了向后兼容
// 新的组件特定事件上下文类在各自的组件文件中定义

/**
 * 事件处理器容器
 *
 * 存储各种类型的事件处理器，支持去抖功能
 */
class EventHandlers {
    /**
     * 去抖时长（毫秒），默认为200ms
     */
    var debounceTimeMs: Long = 200L

    /**
     * 是否为点击事件启用去抖，默认为true（点击事件通常需要立即响应）
     */
    var enableClickDebounce: Boolean = true

    /**
     * 存储每个事件类型的上次调用时间
     */
    private val lastCallTimes = ConcurrentHashMap<String, Long>()

    // 点击事件处理器
    var onClickHandler: ClickHandler? = null
    var onLeftClickHandler: LeftClickHandler? = null
    var onRightClickHandler: RightClickHandler? = null
    var onShiftClickHandler: ShiftClickHandler? = null
    var onShiftLeftClickHandler: ShiftLeftClickHandler? = null
    var onShiftRightClickHandler: ShiftRightClickHandler? = null
    var onMiddleClickHandler: MiddleClickHandler? = null
    var onDoubleClickHandler: DoubleClickHandler? = null

    // 拖拽事件处理器
    var onDragHandler: DragHandler? = null
    var onDragSingleHandler: DragSingleHandler? = null
    var onDragEvenHandler: DragEvenHandler? = null

    // 生命周期事件处理器
    var onShowHandler: ShowHandler? = null
    var onHideHandler: HideHandler? = null
    var onInitHandler: InitHandler? = null
    var onDestroyHandler: DestroyHandler? = null

    // 制作相关事件处理器
    var onCraftHandler: CraftHandler? = null
    var onPrepareCraftHandler: PrepareCraftHandler? = null
    var onPrepareAnvilHandler: PrepareAnvilHandler? = null
    var onPrepareGrindstoneHandler: PrepareGrindstoneHandler? = null
    var onPrepareSmithingHandler: PrepareSmithingHandler? = null
    var onSmithItemHandler: SmithItemHandler? = null

    // 熔炉相关事件处理器
    var onSmeltHandler: SmeltHandler? = null
    var onFurnaceBurnHandler: FurnaceBurnHandler? = null
    var onFurnaceExtractHandler: FurnaceExtractHandler? = null
    var onFurnaceStartSmeltHandler: FurnaceStartSmeltHandler? = null

    // 酿造相关事件处理器
    var onBrewHandler: BrewHandler? = null
    var onBrewingFuelHandler: BrewingFuelHandler? = null

    // 特殊Inventory事件处理器
    var onItemMoveHandler: ItemMoveHandler? = null
    var onPickupItemHandler: PickupItemHandler? = null
    var onCreativeHandler: CreativeHandler? = null

    // 其他事件处理器
    var onDropHandler: DropHandler? = null
    var onTradeSelectHandler: TradeSelectHandler? = null

    // Book相关事件处理器
    var onBookPageTurnHandler: BookPageTurnHandler? = null
    var onBookEditHandler: BookEditHandler? = null
    var onBookSignHandler: BookSignHandler? = null

    /**
     * 检查是否应该跳过执行（去抖检查）
     * @param eventKey 事件的唯一标识符
     * @return true 如果应该跳过执行，false 如果可以执行
     */
    private fun shouldSkipExecution(eventKey: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastCallTimes[eventKey] ?: 0L

        return if (currentTime - lastTime < debounceTimeMs) {
            true // 时间间隔太短，跳过执行
        } else {
            lastCallTimes[eventKey] = currentTime
            false // 可以执行
        }
    }

    /**
     * 处理点击事件
     * 根据点击类型调用相应的处理器
     */
    fun handleClick(context: ClickEventContext) {
        if (context.isPropagationStopped()) return

        val executeClickLogic = {
            // 调用通用点击处理器
            onClickHandler?.invoke(context)
            if (!context.isPropagationStopped()) {
                // 根据点击类型调用特定处理器
                when {
                    context.isLeftClick && !context.isShiftClick -> {
                        onLeftClickHandler?.invoke(context)
                    }
                    context.isRightClick && !context.isShiftClick -> {
                        onRightClickHandler?.invoke(context)
                    }
                    context.isShiftClick -> {
                        onShiftClickHandler?.invoke(context)
                        if (!context.isPropagationStopped()) {
                            when {
                                context.isLeftClick -> {
                                    onShiftLeftClickHandler?.invoke(context)
                                }
                                context.isRightClick -> {
                                    onShiftRightClickHandler?.invoke(context)
                                }
                                context.isMiddleClick -> {
                                    onMiddleClickHandler?.invoke(context)
                                }
                            }
                        }
                    }
                    context.isMiddleClick -> {
                        onMiddleClickHandler?.invoke(context)
                    }
                    context.isDoubleClick -> {
                        onDoubleClickHandler?.invoke(context)
                    }
                }
            }
        }

        if (enableClickDebounce) {
            if (shouldSkipExecution("click_${context.player.uniqueId}_${context.slot}")) {
                return
            }
        }

        executeClickLogic()
    }

    /**
     * 处理拖拽事件
     */
    fun handleDrag(context: DragEventContext) {
        if (context.isPropagationStopped()) return

        if (shouldSkipExecution("drag_${context.player.uniqueId}")) {
            return
        }

        // 调用通用拖拽处理器
        onDragHandler?.invoke(context)
        if (!context.isPropagationStopped()) {
            // 根据拖拽类型调用特定处理器
            when {
                context.isSingle -> {
                    onDragSingleHandler?.invoke(context)
                }
                context.isEven -> {
                    onDragEvenHandler?.invoke(context)
                }
            }
        }
    }

    /**
     * 处理显示事件
     */
    fun handleShow(context: ShowEventContext) {
        if (shouldSkipExecution("show_${context.player.uniqueId}")) {
            return
        }
        onShowHandler?.invoke(context)
    }

    /**
     * 处理隐藏事件
     */
    fun handleHide(context: HideEventContext) {
        if (shouldSkipExecution("hide_${context.player.uniqueId}")) {
            return
        }
        onHideHandler?.invoke(context)
    }

    /**
     * 处理初始化事件
     */
    fun handleInit(context: InitEventContext) {
        if (shouldSkipExecution("init_${context.player.uniqueId}")) {
            return
        }
        onInitHandler?.invoke(context)
    }

    /**
     * 处理销毁事件
     */
    fun handleDestroy(context: DestroyEventContext) {
        if (shouldSkipExecution("destroy_${context.player.uniqueId}")) {
            return
        }
        onDestroyHandler?.invoke(context)
    }

    /**
     * 处理制作事件
     */
    fun handleCraft(context: CraftEventContext) {
        onCraftHandler?.invoke(context)
    }

    /**
     * 处理准备制作事件
     */
    fun handlePrepareCraft(context: PrepareCraftEventContext) {
        onPrepareCraftHandler?.invoke(context)
    }

    /**
     * 处理准备铁砧事件
     */
    fun handlePrepareAnvil(context: PrepareAnvilEventContext) {
        onPrepareAnvilHandler?.invoke(context)
    }

    /**
     * 处理准备砂轮事件
     */
    fun handlePrepareGrindstone(context: PrepareGrindstoneEventContext) {
        onPrepareGrindstoneHandler?.invoke(context)
    }

    /**
     * 处理准备锻造事件
     */
    fun handlePrepareSmithing(context: PrepareSmithingEventContext) {
        onPrepareSmithingHandler?.invoke(context)
    }

    /**
     * 处理锻造物品事件
     */
    fun handleSmithItem(context: SmithItemEventContext) {
        onSmithItemHandler?.invoke(context)
    }

    /**
     * 处理冶炼事件
     */
    fun handleSmelt(context: SmeltEventContext) {
        onSmeltHandler?.invoke(context)
    }

    /**
     * 处理熔炉燃烧事件
     */
    fun handleFurnaceBurn(context: FurnaceBurnEventContext) {
        onFurnaceBurnHandler?.invoke(context)
    }

    /**
     * 处理熔炉提取事件
     */
    fun handleFurnaceExtract(context: FurnaceExtractEventContext) {
        onFurnaceExtractHandler?.invoke(context)
    }

    /**
     * 处理熔炉开始冶炼事件
     */
    fun handleFurnaceStartSmelt(context: FurnaceStartSmeltEventContext) {
        onFurnaceStartSmeltHandler?.invoke(context)
    }

    /**
     * 处理酿造事件
     */
    fun handleBrew(context: BrewEventContext) {
        onBrewHandler?.invoke(context)
    }

    /**
     * 处理酿造燃料事件
     */
    fun handleBrewingFuel(context: BrewingFuelEventContext) {
        onBrewingFuelHandler?.invoke(context)
    }

    /**
     * 处理物品移动事件
     */
    fun handleItemMove(context: ItemMoveEventContext) {
        onItemMoveHandler?.invoke(context)
    }

    /**
     * 处理物品拾取事件
     */
    fun handlePickupItem(context: PickupItemEventContext) {
        onPickupItemHandler?.invoke(context)
    }

    /**
     * 处理创造模式事件
     */
    fun handleCreative(context: CreativeEventContext) {
        onCreativeHandler?.invoke(context)
    }

    /**
     * 处理丢弃事件
     */
    fun handleDrop(context: DropEventContext) {
        onDropHandler?.invoke(context)
    }

    /**
     * 处理交易选择事件
     */
    fun handleTradeSelect(context: TradeSelectEventContext) {
        onTradeSelectHandler?.invoke(context)
    }

    /**
     * 处理书籍翻页事件
     */
    fun handleBookPageTurn(context: BookPageTurnEventContext) {
        onBookPageTurnHandler?.invoke(context)
    }

    /**
     * 处理书籍编辑事件
     */
    fun handleBookEdit(context: BookEditEventContext) {
        onBookEditHandler?.invoke(context)
    }

    /**
     * 处理书籍签名事件
     */
    fun handleBookSign(context: BookSignEventContext) {
        onBookSignHandler?.invoke(context)
    }

    /**
     * 根据传入的上下文类型自动处理对应的事件
     */
    fun handleEvent(context: EventContext<*>) {
        // 打印context的具体类型
        when (context) {
            is ClickEventContext -> handleClick(context)
            is DragEventContext -> handleDrag(context)
            is ShowEventContext -> handleShow(context)
            is HideEventContext -> handleHide(context)
            is InitEventContext -> handleInit(context)
            is DestroyEventContext -> handleDestroy(context)
            is CraftEventContext -> handleCraft(context)
            is PrepareCraftEventContext -> handlePrepareCraft(context)
            is PrepareAnvilEventContext -> handlePrepareAnvil(context)
            is PrepareGrindstoneEventContext -> handlePrepareGrindstone(context)
            is PrepareSmithingEventContext -> handlePrepareSmithing(context)
            is SmithItemEventContext -> handleSmithItem(context)
            is SmeltEventContext -> handleSmelt(context)
            is FurnaceBurnEventContext -> handleFurnaceBurn(context)
            is FurnaceExtractEventContext -> handleFurnaceExtract(context)
            is FurnaceStartSmeltEventContext -> handleFurnaceStartSmelt(context)
            is BrewEventContext -> handleBrew(context)
            is BrewingFuelEventContext -> handleBrewingFuel(context)
            is ItemMoveEventContext -> handleItemMove(context)
            is PickupItemEventContext -> handlePickupItem(context)
            is CreativeEventContext -> handleCreative(context)
            is DropEventContext -> handleDrop(context)
            is TradeSelectEventContext -> handleTradeSelect(context)
            is BookPageTurnEventContext -> handleBookPageTurn(context)
            is BookEditEventContext -> handleBookEdit(context)
            is BookSignEventContext -> handleBookSign(context)
        }
    }

    /**
     * 清空所有事件处理器和去抖记录
     */
    fun clear() {
        onClickHandler = null
        onLeftClickHandler = null
        onRightClickHandler = null
        onShiftClickHandler = null
        onShiftLeftClickHandler = null
        onShiftRightClickHandler = null
        onMiddleClickHandler = null
        onDoubleClickHandler = null

        onDragHandler = null
        onDragSingleHandler = null
        onDragEvenHandler = null

        onShowHandler = null
        onHideHandler = null
        onInitHandler = null
        onDestroyHandler = null

        onCraftHandler = null
        onPrepareCraftHandler = null
        onPrepareAnvilHandler = null
        onPrepareGrindstoneHandler = null
        onPrepareSmithingHandler = null
        onSmithItemHandler = null

        onSmeltHandler = null
        onFurnaceBurnHandler = null
        onFurnaceExtractHandler = null
        onFurnaceStartSmeltHandler = null

        onBrewHandler = null
        onBrewingFuelHandler = null

        onItemMoveHandler = null
        onPickupItemHandler = null
        onCreativeHandler = null

        onDropHandler = null
        onTradeSelectHandler = null

        onBookPageTurnHandler = null
        onBookEditHandler = null
        onBookSignHandler = null

        // 清理去抖记录
        lastCallTimes.clear()
    }
}
