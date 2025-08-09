package city.newnan.gui.component.paginated

import city.newnan.gui.event.*

/**
 * 分页组件事件DSL扩展
 *
 * 为PaginatedComponent提供组件特定的事件处理方法
 * 每个方法都会注入分页组件相关的上下文信息，如index、item等
 */

/**
 * 设置点击事件处理器
 * 注入分页组件特定的上下文：index（相对索引）、item（数据项）
 */
fun <T> PaginatedComponent<T>.onClick(handler: (ClickEventContext, Int, T?) -> Unit) {
    eventHandlers.onClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PaginatedRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.globalIndex, renderContext.item)
        }
    }
}

/**
 * 设置左键点击事件处理器
 * 注入分页组件特定的上下文：index（相对索引）、item（数据项）
 */
fun <T> PaginatedComponent<T>.onLeftClick(handler: (ClickEventContext, Int, T?) -> Unit) {
    eventHandlers.onLeftClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PaginatedRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.globalIndex, renderContext.item)
        }
    }
}

/**
 * 设置右键点击事件处理器
 * 注入分页组件特定的上下文：index（相对索引）、item（数据项）
 */
fun <T> PaginatedComponent<T>.onRightClick(handler: (ClickEventContext, Int, T?) -> Unit) {
    eventHandlers.onRightClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PaginatedRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.globalIndex, renderContext.item)
        }
    }
}

/**
 * 设置Shift点击事件处理器
 * 注入分页组件特定的上下文：index（相对索引）、item（数据项）
 */
fun <T> PaginatedComponent<T>.onShiftClick(handler: (ClickEventContext, Int, T?) -> Unit) {
    eventHandlers.onShiftClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PaginatedRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.globalIndex, renderContext.item)
        }
    }
}

/**
 * 设置Shift左键点击事件处理器
 * 注入分页组件特定的上下文：index（相对索引）、item（数据项）
 */
fun <T> PaginatedComponent<T>.onShiftLeftClick(handler: (ClickEventContext, Int, T?) -> Unit) {
    eventHandlers.onShiftLeftClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PaginatedRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.globalIndex, renderContext.item)
        }
    }
}

/**
 * 设置Shift右键点击事件处理器
 * 注入分页组件特定的上下文：index（相对索引）、item（数据项）
 */
fun <T> PaginatedComponent<T>.onShiftRightClick(handler: (ClickEventContext, Int, T?) -> Unit) {
    eventHandlers.onShiftRightClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PaginatedRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.globalIndex, renderContext.item)
        }
    }
}

/**
 * 设置中键点击事件处理器
 * 注入分页组件特定的上下文：index（相对索引）、item（数据项）
 */
fun <T> PaginatedComponent<T>.onMiddleClick(handler: (ClickEventContext, Int, T?) -> Unit) {
    eventHandlers.onMiddleClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PaginatedRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.globalIndex, renderContext.item)
        }
    }
}

/**
 * 设置双击事件处理器
 * 注入分页组件特定的上下文：index（相对索引）、item（数据项）
 */
fun <T> PaginatedComponent<T>.onDoubleClick(handler: (ClickEventContext, Int, T?) -> Unit) {
    eventHandlers.onDoubleClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PaginatedRenderContext<T>
        if (renderContext != null) {
            handler.invoke(context, renderContext.globalIndex, renderContext.item)
        }
    }
}

/**
 * 设置拖拽事件处理器
 * 注入分页组件特定的上下文：index（相对索引）、item（数据项）
 */
fun <T> PaginatedComponent<T>.onDrag(handler: (DragEventContext, Int, T?) -> Unit) {
    eventHandlers.onDragHandler = { context ->
        // 对于拖拽事件，我们需要找到第一个相关的槽位
        val relevantSlot = context.slots.firstOrNull { containsSlot(it) }
        if (relevantSlot != null) {
            val renderContext = getRenderContext(relevantSlot) as? PaginatedRenderContext<T>
            if (renderContext != null) {
                handler.invoke(context, renderContext.globalIndex, renderContext.item)
            }
        }
    }
}

/**
 * 设置单个物品拖拽事件处理器
 * 注入分页组件特定的上下文：index（相对索引）、item（数据项）
 */
fun <T> PaginatedComponent<T>.onDragSingle(handler: (DragEventContext, Int, T?) -> Unit) {
    eventHandlers.onDragSingleHandler = { context ->
        val relevantSlot = context.slots.firstOrNull { containsSlot(it) }
        if (relevantSlot != null) {
            val renderContext = getRenderContext(relevantSlot) as? PaginatedRenderContext<T>
            if (renderContext != null) {
                handler.invoke(context, renderContext.globalIndex, renderContext.item)
            }
        }
    }
}

/**
 * 设置平均分配拖拽事件处理器
 * 注入分页组件特定的上下文：index（相对索引）、item（数据项）
 */
fun <T> PaginatedComponent<T>.onDragEven(handler: (DragEventContext, Int, T?) -> Unit) {
    eventHandlers.onDragEvenHandler = { context ->
        val relevantSlot = context.slots.firstOrNull { containsSlot(it) }
        if (relevantSlot != null) {
            val renderContext = getRenderContext(relevantSlot) as? PaginatedRenderContext<T>
            if (renderContext != null) {
                handler.invoke(context, renderContext.globalIndex, renderContext.item)
            }
        }
    }
}

/**
 * 设置销毁事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onDestroy(handler: (DestroyEventContext, Int, Int) -> Unit) {
    eventHandlers.onDestroyHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

// 制作相关事件DSL

/**
 * 设置制作事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onCraft(handler: (CraftEventContext, Int, Int) -> Unit) {
    eventHandlers.onCraftHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

/**
 * 设置准备制作事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onPrepareCraft(handler: (PrepareCraftEventContext, Int, Int) -> Unit) {
    eventHandlers.onPrepareCraftHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

/**
 * 设置准备铁砧事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onPrepareAnvil(handler: (PrepareAnvilEventContext, Int, Int) -> Unit) {
    eventHandlers.onPrepareAnvilHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

/**
 * 设置准备砂轮事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onPrepareGrindstone(handler: (PrepareGrindstoneEventContext, Int, Int) -> Unit) {
    eventHandlers.onPrepareGrindstoneHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

/**
 * 设置准备锻造事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onPrepareSmithing(handler: (PrepareSmithingEventContext, Int, Int) -> Unit) {
    eventHandlers.onPrepareSmithingHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

/**
 * 设置锻造物品事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onSmithItem(handler: (SmithItemEventContext, Int, Int) -> Unit) {
    eventHandlers.onSmithItemHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

// 熔炉相关事件DSL

/**
 * 设置冶炼事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onSmelt(handler: (SmeltEventContext, Int, Int) -> Unit) {
    eventHandlers.onSmeltHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

/**
 * 设置熔炉燃烧事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onFurnaceBurn(handler: (FurnaceBurnEventContext, Int, Int) -> Unit) {
    eventHandlers.onFurnaceBurnHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

/**
 * 设置熔炉提取事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onFurnaceExtract(handler: (FurnaceExtractEventContext, Int, Int) -> Unit) {
    eventHandlers.onFurnaceExtractHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

/**
 * 设置熔炉开始冶炼事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onFurnaceStartSmelt(handler: (FurnaceStartSmeltEventContext, Int, Int) -> Unit) {
    eventHandlers.onFurnaceStartSmeltHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

// 酿造相关事件DSL

/**
 * 设置酿造事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onBrew(handler: (BrewEventContext, Int, Int) -> Unit) {
    eventHandlers.onBrewHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

/**
 * 设置酿造燃料事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onBrewingFuel(handler: (BrewingFuelEventContext, Int, Int) -> Unit) {
    eventHandlers.onBrewingFuelHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

// 特殊Inventory事件DSL

/**
 * 设置物品移动事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onItemMove(handler: (ItemMoveEventContext, Int, Int) -> Unit) {
    eventHandlers.onItemMoveHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

/**
 * 设置物品拾取事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onPickupItem(handler: (PickupItemEventContext, Int, Int) -> Unit) {
    eventHandlers.onPickupItemHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

/**
 * 设置创造模式事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onCreative(handler: (CreativeEventContext, Int, Int) -> Unit) {
    eventHandlers.onCreativeHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

// 其他事件DSL

/**
 * 设置丢弃事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onDrop(handler: (DropEventContext, Int, Int) -> Unit) {
    eventHandlers.onDropHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

/**
 * 设置交易选择事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onTradeSelect(handler: (TradeSelectEventContext, Int, Int) -> Unit) {
    eventHandlers.onTradeSelectHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

// Book相关事件DSL

/**
 * 设置书籍翻页事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onBookPageTurn(handler: (BookPageTurnEventContext, Int, Int) -> Unit) {
    eventHandlers.onBookPageTurnHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

/**
 * 设置书籍编辑事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onBookEdit(handler: (BookEditEventContext, Int, Int) -> Unit) {
    eventHandlers.onBookEditHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

/**
 * 设置书籍签名事件处理器
 * 注入分页组件特定的上下文：当前页码、总页数
 */
fun <T> PaginatedComponent<T>.onBookSign(handler: (BookSignEventContext, Int, Int) -> Unit) {
    eventHandlers.onBookSignHandler = { context ->
        handler.invoke(context, getCurrentPage(), totalPages)
    }
}

/**
 * 设置页面变更事件处理器
 */
fun <T> PaginatedComponent<T>.onPageChange(handler: PageChangeHandler) {
    pageChangeHandler = handler
}
