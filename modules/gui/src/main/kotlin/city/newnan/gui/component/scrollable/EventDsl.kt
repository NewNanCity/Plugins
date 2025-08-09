package city.newnan.gui.component.scrollable

import city.newnan.gui.event.*

/**
 * 可滚动组件事件DSL扩展
 *
 * 为ScrollableComponent提供组件特定的事件处理方法
 * 每个方法都会注入可滚动组件相关的上下文信息，如滚动偏移、数据索引等
 */

/**
 * 设置点击事件处理器
 * 注入可滚动组件特定的上下文：visibleIndex（可见索引）、dataIndex（数据索引）、item（数据项）、isLoading（是否加载中）
 */
fun <T> ScrollableComponent<T>.onClick(handler: (ClickEventContext, Int, Int, T?, Boolean) -> Unit) {
    eventHandlers.onClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? ScrollableRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.visibleIndex, renderContext.dataIndex, renderContext.item, renderContext.isLoading)
        }
    }
}

/**
 * 设置左键点击事件处理器
 * 注入可滚动组件特定的上下文：visibleIndex（可见索引）、dataIndex（数据索引）、item（数据项）、isLoading（是否加载中）
 */
fun <T> ScrollableComponent<T>.onLeftClick(handler: (ClickEventContext, Int, Int, T?, Boolean) -> Unit) {
    eventHandlers.onLeftClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? ScrollableRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.visibleIndex, renderContext.dataIndex, renderContext.item, renderContext.isLoading)
        }
    }
}

/**
 * 设置右键点击事件处理器
 * 注入可滚动组件特定的上下文：visibleIndex（可见索引）、dataIndex（数据索引）、item（数据项）、isLoading（是否加载中）
 */
fun <T> ScrollableComponent<T>.onRightClick(handler: (ClickEventContext, Int, Int, T?, Boolean) -> Unit) {
    eventHandlers.onRightClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? ScrollableRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.visibleIndex, renderContext.dataIndex, renderContext.item, renderContext.isLoading)
        }
    }
}

/**
 * 设置Shift点击事件处理器
 * 注入可滚动组件特定的上下文：visibleIndex（可见索引）、dataIndex（数据索引）、item（数据项）、isLoading（是否加载中）
 */
fun <T> ScrollableComponent<T>.onShiftClick(handler: (ClickEventContext, Int, Int, T?, Boolean) -> Unit) {
    eventHandlers.onShiftClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? ScrollableRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.visibleIndex, renderContext.dataIndex, renderContext.item, renderContext.isLoading)
        }
    }
}

/**
 * 设置Shift左键点击事件处理器
 * 注入可滚动组件特定的上下文：visibleIndex（可见索引）、dataIndex（数据索引）、item（数据项）、isLoading（是否加载中）
 */
fun <T> ScrollableComponent<T>.onShiftLeftClick(handler: (ClickEventContext, Int, Int, T?, Boolean) -> Unit) {
    eventHandlers.onShiftLeftClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? ScrollableRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.visibleIndex, renderContext.dataIndex, renderContext.item, renderContext.isLoading)
        }
    }
}

/**
 * 设置Shift右键点击事件处理器
 * 注入可滚动组件特定的上下文：visibleIndex（可见索引）、dataIndex（数据索引）、item（数据项）、isLoading（是否加载中）
 */
fun <T> ScrollableComponent<T>.onShiftRightClick(handler: (ClickEventContext, Int, Int, T?, Boolean) -> Unit) {
    eventHandlers.onShiftRightClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? ScrollableRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.visibleIndex, renderContext.dataIndex, renderContext.item, renderContext.isLoading)
        }
    }
}

/**
 * 设置中键点击事件处理器
 * 注入可滚动组件特定的上下文：visibleIndex（可见索引）、dataIndex（数据索引）、item（数据项）、isLoading（是否加载中）
 */
fun <T> ScrollableComponent<T>.onMiddleClick(handler: (ClickEventContext, Int, Int, T?, Boolean) -> Unit) {
    eventHandlers.onMiddleClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? ScrollableRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.visibleIndex, renderContext.dataIndex, renderContext.item, renderContext.isLoading)
        }
    }
}

/**
 * 设置双击事件处理器
 * 注入可滚动组件特定的上下文：visibleIndex（可见索引）、dataIndex（数据索引）、item（数据项）、isLoading（是否加载中）
 */
fun <T> ScrollableComponent<T>.onDoubleClick(handler: (ClickEventContext, Int, Int, T?, Boolean) -> Unit) {
    eventHandlers.onDoubleClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? ScrollableRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.visibleIndex, renderContext.dataIndex, renderContext.item, renderContext.isLoading)
        }
    }
}

/**
 * 设置拖拽事件处理器
 * 注入可滚动组件特定的上下文：visibleIndex（可见索引）、dataIndex（数据索引）、item（数据项）、isLoading（是否加载中）
 */
fun <T> ScrollableComponent<T>.onDrag(handler: (DragEventContext, Int, Int, T?, Boolean) -> Unit) {
    eventHandlers.onDragHandler = { context ->
        // 对于拖拽事件，我们需要找到第一个相关的槽位
        val relevantSlot = context.slots.firstOrNull { containsSlot(it) }
        if (relevantSlot != null) {
            val renderContext = getRenderContext(relevantSlot) as? ScrollableRenderContext<T>
            if (renderContext != null) {
                handler.invoke(context, renderContext.visibleIndex, renderContext.dataIndex, renderContext.item, renderContext.isLoading)
            }
        }
    }
}

/**
 * 设置单个物品拖拽事件处理器
 * 注入可滚动组件特定的上下文：visibleIndex（可见索引）、dataIndex（数据索引）、item（数据项）、isLoading（是否加载中）
 */
fun <T> ScrollableComponent<T>.onDragSingle(handler: (DragEventContext, Int, Int, T?, Boolean) -> Unit) {
    eventHandlers.onDragSingleHandler = { context ->
        val relevantSlot = context.slots.firstOrNull { containsSlot(it) }
        if (relevantSlot != null) {
            val renderContext = getRenderContext(relevantSlot) as? ScrollableRenderContext<T>
            if (renderContext != null) {
                handler.invoke(context, renderContext.visibleIndex, renderContext.dataIndex, renderContext.item, renderContext.isLoading)
            }
        }
    }
}

/**
 * 设置平均分配拖拽事件处理器
 * 注入可滚动组件特定的上下文：visibleIndex（可见索引）、dataIndex（数据索引）、item（数据项）、isLoading（是否加载中）
 */
fun <T> ScrollableComponent<T>.onDragEven(handler: (DragEventContext, Int, Int, T?, Boolean) -> Unit) {
    eventHandlers.onDragEvenHandler = { context ->
        val relevantSlot = context.slots.firstOrNull { containsSlot(it) }
        if (relevantSlot != null) {
            val renderContext = getRenderContext(relevantSlot) as? ScrollableRenderContext<T>
            if (renderContext != null) {
                handler.invoke(context, renderContext.visibleIndex, renderContext.dataIndex, renderContext.item, renderContext.isLoading)
            }
        }
    }
}

/**
 * 设置销毁事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onDestroy(handler: (DestroyEventContext, Int, Int) -> Unit) {
    eventHandlers.onDestroyHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

// 制作相关事件DSL

/**
 * 设置制作事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onCraft(handler: (CraftEventContext, Int, Int) -> Unit) {
    eventHandlers.onCraftHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

/**
 * 设置准备制作事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onPrepareCraft(handler: (PrepareCraftEventContext, Int, Int) -> Unit) {
    eventHandlers.onPrepareCraftHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

/**
 * 设置准备铁砧事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onPrepareAnvil(handler: (PrepareAnvilEventContext, Int, Int) -> Unit) {
    eventHandlers.onPrepareAnvilHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

/**
 * 设置准备砂轮事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onPrepareGrindstone(handler: (PrepareGrindstoneEventContext, Int, Int) -> Unit) {
    eventHandlers.onPrepareGrindstoneHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

/**
 * 设置准备锻造事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onPrepareSmithing(handler: (PrepareSmithingEventContext, Int, Int) -> Unit) {
    eventHandlers.onPrepareSmithingHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

/**
 * 设置锻造物品事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onSmithItem(handler: (SmithItemEventContext, Int, Int) -> Unit) {
    eventHandlers.onSmithItemHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

// 熔炉相关事件DSL

/**
 * 设置冶炼事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onSmelt(handler: (SmeltEventContext, Int, Int) -> Unit) {
    eventHandlers.onSmeltHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

/**
 * 设置熔炉燃烧事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onFurnaceBurn(handler: (FurnaceBurnEventContext, Int, Int) -> Unit) {
    eventHandlers.onFurnaceBurnHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

/**
 * 设置熔炉提取事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onFurnaceExtract(handler: (FurnaceExtractEventContext, Int, Int) -> Unit) {
    eventHandlers.onFurnaceExtractHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

/**
 * 设置熔炉开始冶炼事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onFurnaceStartSmelt(handler: (FurnaceStartSmeltEventContext, Int, Int) -> Unit) {
    eventHandlers.onFurnaceStartSmeltHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

// 酿造相关事件DSL

/**
 * 设置酿造事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onBrew(handler: (BrewEventContext, Int, Int) -> Unit) {
    eventHandlers.onBrewHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

/**
 * 设置酿造燃料事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onBrewingFuel(handler: (BrewingFuelEventContext, Int, Int) -> Unit) {
    eventHandlers.onBrewingFuelHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

// 特殊Inventory事件DSL

/**
 * 设置物品移动事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onItemMove(handler: (ItemMoveEventContext, Int, Int) -> Unit) {
    eventHandlers.onItemMoveHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

/**
 * 设置物品拾取事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onPickupItem(handler: (PickupItemEventContext, Int, Int) -> Unit) {
    eventHandlers.onPickupItemHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

/**
 * 设置创造模式事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onCreative(handler: (CreativeEventContext, Int, Int) -> Unit) {
    eventHandlers.onCreativeHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

// 其他事件DSL

/**
 * 设置丢弃事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onDrop(handler: (DropEventContext, Int, Int) -> Unit) {
    eventHandlers.onDropHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

/**
 * 设置交易选择事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onTradeSelect(handler: (TradeSelectEventContext, Int, Int) -> Unit) {
    eventHandlers.onTradeSelectHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

// Book相关事件DSL

/**
 * 设置书籍翻页事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onBookPageTurn(handler: (BookPageTurnEventContext, Int, Int) -> Unit) {
    eventHandlers.onBookPageTurnHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

/**
 * 设置书籍编辑事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onBookEdit(handler: (BookEditEventContext, Int, Int) -> Unit) {
    eventHandlers.onBookEditHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}

/**
 * 设置书籍签名事件处理器
 * 注入可滚动组件特定的上下文：当前滚动偏移、最大滚动偏移
 */
fun <T> ScrollableComponent<T>.onBookSign(handler: (BookSignEventContext, Int, Int) -> Unit) {
    eventHandlers.onBookSignHandler = { context ->
        handler.invoke(context, getScrollOffset(), maxScrollOffset)
    }
}