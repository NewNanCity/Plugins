package city.newnan.gui.component.singlestorageslot

import city.newnan.gui.event.*
import org.bukkit.inventory.ItemStack

/**
 * 单存储槽位组件事件DSL扩展
 *
 * 为SingleStorageSlotComponent提供组件特定的事件处理方法
 * 每个方法都会注入单存储槽位组件相关的上下文信息，如坐标、存储物品等
 */

/**
 * 设置点击事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onClick(handler: (ClickEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onClickHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置左键点击事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onLeftClick(handler: (ClickEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onLeftClickHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置右键点击事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onRightClick(handler: (ClickEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onRightClickHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置Shift点击事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onShiftClick(handler: (ClickEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onShiftClickHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置Shift左键点击事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onShiftLeftClick(handler: (ClickEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onShiftLeftClickHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置Shift右键点击事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onShiftRightClick(handler: (ClickEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onShiftRightClickHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置中键点击事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onMiddleClick(handler: (ClickEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onMiddleClickHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置双击事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onDoubleClick(handler: (ClickEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onDoubleClickHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置拖拽事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onDrag(handler: (DragEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onDragHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置销毁事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onDestroy(handler: (DestroyEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onDestroyHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置物品放入事件处理器
 * 当玩家尝试放入物品时触发，注入物品信息
 */
fun SingleStorageSlotComponent.onItemPlace(handler: (ClickEventContext, Int, Int, ItemStack?, ItemStack?) -> Unit) {
    onClick { context, x, y, storedItem ->
        val cursorItem = context.event.cursor
        if (cursorItem.type != org.bukkit.Material.AIR) {
            handler.invoke(context, x, y, storedItem, cursorItem)
        }
    }
}

/**
 * 设置物品取出事件处理器
 * 当玩家尝试取出物品时触发，注入物品信息
 */
fun SingleStorageSlotComponent.onItemTake(handler: (ClickEventContext, Int, Int, ItemStack) -> Unit) {
    onClick { context, x, y, storedItem ->
        val cursorItem = context.event.cursor
        if (storedItem != null && cursorItem.type == org.bukkit.Material.AIR) {
            handler.invoke(context, x, y, storedItem)
        }
    }
}

/**
 * 设置物品交换事件处理器
 * 当玩家尝试交换物品时触发，注入物品信息
 */
fun SingleStorageSlotComponent.onItemSwap(handler: (ClickEventContext, Int, Int, ItemStack, ItemStack) -> Unit) {
    onClick { context, x, y, storedItem ->
        val cursorItem = context.event.cursor
        if (storedItem != null && cursorItem.type != org.bukkit.Material.AIR) {
            handler.invoke(context, x, y, storedItem, cursorItem)
        }
    }
}

/**
 * 设置物品验证失败事件处理器
 * 当物品验证失败时触发
 */
fun SingleStorageSlotComponent.onItemValidationFailed(handler: (ClickEventContext, Int, Int, ItemStack) -> Unit) {
    onClick { context, x, y, _ ->
        val cursorItem = context.event.cursor
        if (cursorItem.type != org.bukkit.Material.AIR && !canAcceptItem(cursorItem)) {
            handler.invoke(context, x, y, cursorItem)
        }
    }
}

/**
 * 设置存储状态变化事件处理器
 * 当存储状态发生变化时触发（空->有物品 或 有物品->空）
 */
fun SingleStorageSlotComponent.onStorageStateChange(handler: (ClickEventContext, Int, Int, Boolean, Boolean) -> Unit) {
    onClick { context, x, y, storedItem ->
        val cursorItem = context.event.cursor
        val wasEmpty = storedItem == null
        val willBeEmpty = when {
            cursorItem.type != org.bukkit.Material.AIR -> false // 放入物品
            storedItem != null -> true // 取出物品
            else -> wasEmpty // 无变化
        }

        if (wasEmpty != willBeEmpty) {
            handler.invoke(context, x, y, wasEmpty, willBeEmpty)
        }
    }
}

/**
 * 设置单个物品拖拽事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onDragSingle(handler: (DragEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onDragSingleHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置平均分配拖拽事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onDragEven(handler: (DragEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onDragEvenHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}



// 制作相关事件DSL

/**
 * 设置制作事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onCraft(handler: (CraftEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onCraftHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置准备制作事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onPrepareCraft(handler: (PrepareCraftEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onPrepareCraftHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置准备铁砧事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onPrepareAnvil(handler: (PrepareAnvilEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onPrepareAnvilHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置准备砂轮事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onPrepareGrindstone(handler: (PrepareGrindstoneEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onPrepareGrindstoneHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置准备锻造事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onPrepareSmithing(handler: (PrepareSmithingEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onPrepareSmithingHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置锻造物品事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onSmithItem(handler: (SmithItemEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onSmithItemHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

// 熔炉相关事件DSL

/**
 * 设置冶炼事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onSmelt(handler: (SmeltEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onSmeltHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置熔炉燃烧事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onFurnaceBurn(handler: (FurnaceBurnEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onFurnaceBurnHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置熔炉提取事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onFurnaceExtract(handler: (FurnaceExtractEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onFurnaceExtractHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置熔炉开始冶炼事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onFurnaceStartSmelt(handler: (FurnaceStartSmeltEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onFurnaceStartSmeltHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

// 酿造相关事件DSL

/**
 * 设置酿造事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onBrew(handler: (BrewEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onBrewHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置酿造燃料事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onBrewingFuel(handler: (BrewingFuelEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onBrewingFuelHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

// 特殊Inventory事件DSL

/**
 * 设置物品移动事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onItemMove(handler: (ItemMoveEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onItemMoveHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置物品拾取事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onPickupItem(handler: (PickupItemEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onPickupItemHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置创造模式事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onCreative(handler: (CreativeEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onCreativeHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

// 其他事件DSL

/**
 * 设置丢弃事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onDrop(handler: (DropEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onDropHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置交易选择事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onTradeSelect(handler: (TradeSelectEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onTradeSelectHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

// Book相关事件DSL

/**
 * 设置书籍翻页事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onBookPageTurn(handler: (BookPageTurnEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onBookPageTurnHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置书籍编辑事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onBookEdit(handler: (BookEditEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onBookEditHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}

/**
 * 设置书籍签名事件处理器
 * 注入单存储槽位组件特定的上下文：x、y（坐标）、storedItem（存储的物品）
 */
fun SingleStorageSlotComponent.onBookSign(handler: (BookSignEventContext, Int, Int, ItemStack?) -> Unit) {
    eventHandlers.onBookSignHandler = { context ->
        handler.invoke(context, getX(), getY(), getStoredItem())
    }
}
