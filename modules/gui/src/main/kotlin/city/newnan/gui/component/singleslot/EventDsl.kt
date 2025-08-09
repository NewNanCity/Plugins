package city.newnan.gui.component.singleslot

import city.newnan.gui.event.*

/**
 * 单槽位组件事件DSL扩展
 *
 * 为SingleSlotComponent提供组件特定的事件处理方法
 * 每个方法都会注入单槽位组件相关的上下文信息，如坐标等
 */

/**
 * 设置点击事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onClick(handler: (ClickEventContext, Int, Int) -> Unit) {
    eventHandlers.onClickHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置左键点击事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onLeftClick(handler: (ClickEventContext, Int, Int) -> Unit) {
    eventHandlers.onLeftClickHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置右键点击事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onRightClick(handler: (ClickEventContext, Int, Int) -> Unit) {
    eventHandlers.onRightClickHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置Shift点击事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onShiftClick(handler: (ClickEventContext, Int, Int) -> Unit) {
    eventHandlers.onShiftClickHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置Shift左键点击事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onShiftLeftClick(handler: (ClickEventContext, Int, Int) -> Unit) {
    eventHandlers.onShiftLeftClickHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置Shift右键点击事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onShiftRightClick(handler: (ClickEventContext, Int, Int) -> Unit) {
    eventHandlers.onShiftRightClickHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置中键点击事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onMiddleClick(handler: (ClickEventContext, Int, Int) -> Unit) {
    eventHandlers.onMiddleClickHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置双击事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onDoubleClick(handler: (ClickEventContext, Int, Int) -> Unit) {
    eventHandlers.onDoubleClickHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置拖拽事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onDrag(handler: (DragEventContext, Int, Int) -> Unit) {
    eventHandlers.onDragHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置单个物品拖拽事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onDragSingle(handler: (DragEventContext, Int, Int) -> Unit) {
    eventHandlers.onDragSingleHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置平均分配拖拽事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onDragEven(handler: (DragEventContext, Int, Int) -> Unit) {
    eventHandlers.onDragEvenHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置销毁事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onDestroy(handler: (DestroyEventContext, Int, Int) -> Unit) {
    eventHandlers.onDestroyHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

// 制作相关事件DSL

/**
 * 设置制作事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onCraft(handler: (CraftEventContext, Int, Int) -> Unit) {
    eventHandlers.onCraftHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置准备制作事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onPrepareCraft(handler: (PrepareCraftEventContext, Int, Int) -> Unit) {
    eventHandlers.onPrepareCraftHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置准备铁砧事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onPrepareAnvil(handler: (PrepareAnvilEventContext, Int, Int) -> Unit) {
    eventHandlers.onPrepareAnvilHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置准备砂轮事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onPrepareGrindstone(handler: (PrepareGrindstoneEventContext, Int, Int) -> Unit) {
    eventHandlers.onPrepareGrindstoneHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置准备锻造事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onPrepareSmithing(handler: (PrepareSmithingEventContext, Int, Int) -> Unit) {
    eventHandlers.onPrepareSmithingHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置锻造物品事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onSmithItem(handler: (SmithItemEventContext, Int, Int) -> Unit) {
    eventHandlers.onSmithItemHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

// 熔炉相关事件DSL

/**
 * 设置冶炼事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onSmelt(handler: (SmeltEventContext, Int, Int) -> Unit) {
    eventHandlers.onSmeltHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置熔炉燃烧事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onFurnaceBurn(handler: (FurnaceBurnEventContext, Int, Int) -> Unit) {
    eventHandlers.onFurnaceBurnHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置熔炉提取事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onFurnaceExtract(handler: (FurnaceExtractEventContext, Int, Int) -> Unit) {
    eventHandlers.onFurnaceExtractHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置熔炉开始冶炼事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onFurnaceStartSmelt(handler: (FurnaceStartSmeltEventContext, Int, Int) -> Unit) {
    eventHandlers.onFurnaceStartSmeltHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

// 酿造相关事件DSL

/**
 * 设置酿造事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onBrew(handler: (BrewEventContext, Int, Int) -> Unit) {
    eventHandlers.onBrewHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置酿造燃料事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onBrewingFuel(handler: (BrewingFuelEventContext, Int, Int) -> Unit) {
    eventHandlers.onBrewingFuelHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

// 特殊Inventory事件DSL

/**
 * 设置物品移动事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onItemMove(handler: (ItemMoveEventContext, Int, Int) -> Unit) {
    eventHandlers.onItemMoveHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置物品拾取事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onPickupItem(handler: (PickupItemEventContext, Int, Int) -> Unit) {
    eventHandlers.onPickupItemHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置创造模式事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onCreative(handler: (CreativeEventContext, Int, Int) -> Unit) {
    eventHandlers.onCreativeHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

// 其他事件DSL

/**
 * 设置丢弃事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onDrop(handler: (DropEventContext, Int, Int) -> Unit) {
    eventHandlers.onDropHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置交易选择事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onTradeSelect(handler: (TradeSelectEventContext, Int, Int) -> Unit) {
    eventHandlers.onTradeSelectHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

// Book相关事件DSL

/**
 * 设置书籍翻页事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onBookPageTurn(handler: (BookPageTurnEventContext, Int, Int) -> Unit) {
    eventHandlers.onBookPageTurnHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置书籍编辑事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onBookEdit(handler: (BookEditEventContext, Int, Int) -> Unit) {
    eventHandlers.onBookEditHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}

/**
 * 设置书籍签名事件处理器
 * 注入单槽位组件特定的上下文：x、y（坐标）
 */
fun SingleSlotComponent.onBookSign(handler: (BookSignEventContext, Int, Int) -> Unit) {
    eventHandlers.onBookSignHandler = { context ->
        handler.invoke(context, getX(), getY())
    }
}
