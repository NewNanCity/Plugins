package city.newnan.gui.dsl

import city.newnan.gui.event.*
import city.newnan.gui.page.BasePage
import city.newnan.gui.page.BookPage

/**
 * Page的事件处理DSL扩展
 *
 * 提供简洁的DSL语法来设置页面的各种事件处理器
 */

// 去抖配置DSL
/**
 * 设置去抖时长
 * @param timeMs 去抖时长（毫秒）
 */
fun BasePage.debounceTime(timeMs: Long) {
    eventHandlers.debounceTimeMs = timeMs
}

/**
 * 启用点击事件去抖
 */
fun BasePage.enableClickDebounce() {
    eventHandlers.enableClickDebounce = true
}

/**
 * 禁用点击事件去抖
 */
fun BasePage.disableClickDebounce() {
    eventHandlers.enableClickDebounce = false
}

/**
 * 设置点击事件去抖开关
 * @param enabled 是否启用点击事件去抖
 */
fun BasePage.clickDebounce(enabled: Boolean) {
    eventHandlers.enableClickDebounce = enabled
}

/**
 * 去抖配置DSL块
 */
fun BasePage.debounce(block: DebounceConfig.() -> Unit) {
    val config = DebounceConfig(eventHandlers)
    config.block()
}

/**
 * 去抖配置类
 */
class DebounceConfig(private val eventHandlers: city.newnan.gui.event.EventHandlers) {
    /**
     * 设置去抖时长
     */
    var timeMs: Long
        get() = eventHandlers.debounceTimeMs
        set(value) {
            eventHandlers.debounceTimeMs = value
        }

    /**
     * 设置点击事件去抖开关
     */
    var clickEnabled: Boolean
        get() = eventHandlers.enableClickDebounce
        set(value) {
            eventHandlers.enableClickDebounce = value
        }
}

// 点击事件DSL
fun BasePage.onPageSlotClick(handler: ClickHandler) {
    eventHandlers.onClickHandler = handler
}

fun BasePage.onPageSlotLeftClick(handler: LeftClickHandler) {
    eventHandlers.onLeftClickHandler = handler
}

fun BasePage.onPageSlotRightClick(handler: RightClickHandler) {
    eventHandlers.onRightClickHandler = handler
}

fun BasePage.onPageSlotShiftClick(handler: ShiftClickHandler) {
    eventHandlers.onShiftClickHandler = handler
}

fun BasePage.onPageSlotShiftLeftClick(handler: ShiftLeftClickHandler) {
    eventHandlers.onShiftLeftClickHandler = handler
}

fun BasePage.onPageSlotShiftRightClick(handler: ShiftRightClickHandler) {
    eventHandlers.onShiftRightClickHandler = handler
}

fun BasePage.onPageSlotMiddleClick(handler: MiddleClickHandler) {
    eventHandlers.onMiddleClickHandler = handler
}

fun BasePage.onPageSlotDoubleClick(handler: DoubleClickHandler) {
    eventHandlers.onDoubleClickHandler = handler
}

// 拖拽事件DSL
fun BasePage.onPageSlotDrag(handler: DragHandler) {
    eventHandlers.onDragHandler = handler
}

fun BasePage.onPageSlotDragSingle(handler: DragSingleHandler) {
    eventHandlers.onDragSingleHandler = handler
}

fun BasePage.onPageSlotDragEven(handler: DragEvenHandler) {
    eventHandlers.onDragEvenHandler = handler
}

// 生命周期事件DSL
fun BasePage.onPageShow(handler: ShowHandler) {
    eventHandlers.onShowHandler = handler
}

fun BasePage.onPageHide(handler: HideHandler) {
    eventHandlers.onHideHandler = handler
}

fun BasePage.onPageInit(handler: InitHandler) {
    eventHandlers.onInitHandler = handler
}

fun BasePage.onPageDestroy(handler: DestroyHandler) {
    eventHandlers.onDestroyHandler = handler
}

// 制作相关事件DSL
fun BasePage.onCraft(handler: CraftHandler) {
    eventHandlers.onCraftHandler = handler
}

fun BasePage.onPrepareCraft(handler: PrepareCraftHandler) {
    eventHandlers.onPrepareCraftHandler = handler
}

fun BasePage.onPrepareAnvil(handler: PrepareAnvilHandler) {
    eventHandlers.onPrepareAnvilHandler = handler
}

fun BasePage.onPrepareGrindstone(handler: PrepareGrindstoneHandler) {
    eventHandlers.onPrepareGrindstoneHandler = handler
}

fun BasePage.onPrepareSmithing(handler: PrepareSmithingHandler) {
    eventHandlers.onPrepareSmithingHandler = handler
}

fun BasePage.onSmithItem(handler: SmithItemHandler) {
    eventHandlers.onSmithItemHandler = handler
}

// 熔炉相关事件DSL
fun BasePage.onSmelt(handler: SmeltHandler) {
    eventHandlers.onSmeltHandler = handler
}

fun BasePage.onFurnaceBurn(handler: FurnaceBurnHandler) {
    eventHandlers.onFurnaceBurnHandler = handler
}

fun BasePage.onFurnaceExtract(handler: FurnaceExtractHandler) {
    eventHandlers.onFurnaceExtractHandler = handler
}

fun BasePage.onFurnaceStartSmelt(handler: FurnaceStartSmeltHandler) {
    eventHandlers.onFurnaceStartSmeltHandler = handler
}

// 酿造相关事件DSL
fun BasePage.onBrew(handler: BrewHandler) {
    eventHandlers.onBrewHandler = handler
}

fun BasePage.onBrewingFuel(handler: BrewingFuelHandler) {
    eventHandlers.onBrewingFuelHandler = handler
}

// 特殊Inventory事件DSL
fun BasePage.onItemMove(handler: ItemMoveHandler) {
    eventHandlers.onItemMoveHandler = handler
}

fun BasePage.onPickupItem(handler: PickupItemHandler) {
    eventHandlers.onPickupItemHandler = handler
}

fun BasePage.onCreative(handler: CreativeHandler) {
    eventHandlers.onCreativeHandler = handler
}

// 其他事件DSL
fun BasePage.onDrop(handler: DropHandler) {
    eventHandlers.onDropHandler = handler
}

fun BasePage.onTradeSelect(handler: TradeSelectHandler) {
    eventHandlers.onTradeSelectHandler = handler
}

// Book相关事件DSL
fun BasePage.onBookPageTurn(handler: BookPageTurnHandler) {
    eventHandlers.onBookPageTurnHandler = handler
}

fun BasePage.onBookEdit(handler: BookEditHandler) {
    eventHandlers.onBookEditHandler = handler
}

fun BasePage.onBookSign(handler: BookSignHandler) {
    eventHandlers.onBookSignHandler = handler
}

/**
 * BookPage的事件处理DSL扩展
 */

// Book相关事件DSL for BookPage
fun BookPage.onBookPageTurn(handler: BookPageTurnHandler) {
    eventHandlers.onBookPageTurnHandler = handler
}

fun BookPage.onBookEdit(handler: BookEditHandler) {
    eventHandlers.onBookEditHandler = handler
}

fun BookPage.onBookSign(handler: BookSignHandler) {
    eventHandlers.onBookSignHandler = handler
}
