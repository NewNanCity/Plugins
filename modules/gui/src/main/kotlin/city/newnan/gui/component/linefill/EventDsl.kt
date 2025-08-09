package city.newnan.gui.component.linefill

import city.newnan.gui.event.*

/**
 * 线条填充组件事件DSL扩展
 *
 * 为LineFillComponent提供组件特定的事件处理方法
 * 每个方法都会注入线条填充组件相关的上下文信息，如索引、方向等
 */

/**
 * 设置点击事件处理器
 * 注入线条填充组件特定的上下文：index（索引）、direction（方向）
 */
fun LineFillComponent.onClick(handler: (ClickEventContext, Int, LineFillDirection) -> Unit) {
    eventHandlers.onClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? LineFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.index, renderContext.direction)
        }
    }
}

/**
 * 设置左键点击事件处理器
 * 注入线条填充组件特定的上下文：index（索引）、direction（方向）
 */
fun LineFillComponent.onLeftClick(handler: (ClickEventContext, Int, LineFillDirection) -> Unit) {
    eventHandlers.onLeftClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? LineFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.index, renderContext.direction)
        }
    }
}

/**
 * 设置右键点击事件处理器
 * 注入线条填充组件特定的上下文：index（索引）、direction（方向）
 */
fun LineFillComponent.onRightClick(handler: (ClickEventContext, Int, LineFillDirection) -> Unit) {
    eventHandlers.onRightClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? LineFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.index, renderContext.direction)
        }
    }
}

/**
 * 设置Shift点击事件处理器
 * 注入线条填充组件特定的上下文：index（索引）、direction（方向）
 */
fun LineFillComponent.onShiftClick(handler: (ClickEventContext, Int, LineFillDirection) -> Unit) {
    eventHandlers.onShiftClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? LineFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.index, renderContext.direction)
        }
    }
}

/**
 * 设置Shift左键点击事件处理器
 * 注入线条填充组件特定的上下文：index（索引）、direction（方向）
 */
fun LineFillComponent.onShiftLeftClick(handler: (ClickEventContext, Int, LineFillDirection) -> Unit) {
    eventHandlers.onShiftLeftClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? LineFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.index, renderContext.direction)
        }
    }
}

/**
 * 设置Shift右键点击事件处理器
 * 注入线条填充组件特定的上下文：index（索引）、direction（方向）
 */
fun LineFillComponent.onShiftRightClick(handler: (ClickEventContext, Int, LineFillDirection) -> Unit) {
    eventHandlers.onShiftRightClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? LineFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.index, renderContext.direction)
        }
    }
}

/**
 * 设置中键点击事件处理器
 * 注入线条填充组件特定的上下文：index（索引）、direction（方向）
 */
fun LineFillComponent.onMiddleClick(handler: (ClickEventContext, Int, LineFillDirection) -> Unit) {
    eventHandlers.onMiddleClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? LineFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.index, renderContext.direction)
        }
    }
}

/**
 * 设置双击事件处理器
 * 注入线条填充组件特定的上下文：index（索引）、direction（方向）
 */
fun LineFillComponent.onDoubleClick(handler: (ClickEventContext, Int, LineFillDirection) -> Unit) {
    eventHandlers.onDoubleClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? LineFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.index, renderContext.direction)
        }
    }
}

/**
 * 设置拖拽事件处理器
 * 注入线条填充组件特定的上下文：index（索引）、direction（方向）
 */
fun LineFillComponent.onDrag(handler: (DragEventContext, Int, LineFillDirection) -> Unit) {
    eventHandlers.onDragHandler = { context ->
        // 对于拖拽事件，我们需要找到第一个相关的槽位
        val relevantSlot = context.slots.firstOrNull { containsSlot(it) }
        if (relevantSlot != null) {
            val renderContext = getRenderContext(relevantSlot) as? LineFillRenderContext
            if (renderContext != null) {
                handler.invoke(context, renderContext.index, renderContext.direction)
            }
        }
    }
}

/**
 * 设置单个物品拖拽事件处理器
 * 注入线条填充组件特定的上下文：index（索引）、direction（方向）
 */
fun LineFillComponent.onDragSingle(handler: (DragEventContext, Int, LineFillDirection) -> Unit) {
    eventHandlers.onDragSingleHandler = { context ->
        val relevantSlot = context.slots.firstOrNull { containsSlot(it) }
        if (relevantSlot != null) {
            val renderContext = getRenderContext(relevantSlot) as? LineFillRenderContext
            if (renderContext != null) {
                handler.invoke(context, renderContext.index, renderContext.direction)
            }
        }
    }
}

/**
 * 设置平均分配拖拽事件处理器
 * 注入线条填充组件特定的上下文：index（索引）、direction（方向）
 */
fun LineFillComponent.onDragEven(handler: (DragEventContext, Int, LineFillDirection) -> Unit) {
    eventHandlers.onDragEvenHandler = { context ->
        val relevantSlot = context.slots.firstOrNull { containsSlot(it) }
        if (relevantSlot != null) {
            val renderContext = getRenderContext(relevantSlot) as? LineFillRenderContext
            if (renderContext != null) {
                handler.invoke(context, renderContext.index, renderContext.direction)
            }
        }
    }
}

/**
 * 设置销毁事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onDestroy(handler: (DestroyEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onDestroyHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

// 制作相关事件DSL

/**
 * 设置制作事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onCraft(handler: (CraftEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onCraftHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

/**
 * 设置准备制作事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onPrepareCraft(handler: (PrepareCraftEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onPrepareCraftHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

/**
 * 设置准备铁砧事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onPrepareAnvil(handler: (PrepareAnvilEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onPrepareAnvilHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

/**
 * 设置准备砂轮事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onPrepareGrindstone(handler: (PrepareGrindstoneEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onPrepareGrindstoneHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

/**
 * 设置准备锻造事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onPrepareSmithing(handler: (PrepareSmithingEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onPrepareSmithingHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

/**
 * 设置锻造物品事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onSmithItem(handler: (SmithItemEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onSmithItemHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

// 熔炉相关事件DSL

/**
 * 设置冶炼事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onSmelt(handler: (SmeltEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onSmeltHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

/**
 * 设置熔炉燃烧事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onFurnaceBurn(handler: (FurnaceBurnEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onFurnaceBurnHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

/**
 * 设置熔炉提取事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onFurnaceExtract(handler: (FurnaceExtractEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onFurnaceExtractHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

/**
 * 设置熔炉开始冶炼事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onFurnaceStartSmelt(handler: (FurnaceStartSmeltEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onFurnaceStartSmeltHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

// 酿造相关事件DSL

/**
 * 设置酿造事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onBrew(handler: (BrewEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onBrewHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

/**
 * 设置酿造燃料事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onBrewingFuel(handler: (BrewingFuelEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onBrewingFuelHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

// 特殊Inventory事件DSL

/**
 * 设置物品移动事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onItemMove(handler: (ItemMoveEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onItemMoveHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

/**
 * 设置物品拾取事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onPickupItem(handler: (PickupItemEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onPickupItemHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

/**
 * 设置创造模式事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onCreative(handler: (CreativeEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onCreativeHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

// 其他事件DSL

/**
 * 设置丢弃事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onDrop(handler: (DropEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onDropHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

/**
 * 设置交易选择事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onTradeSelect(handler: (TradeSelectEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onTradeSelectHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

// Book相关事件DSL

/**
 * 设置书籍翻页事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onBookPageTurn(handler: (BookPageTurnEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onBookPageTurnHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

/**
 * 设置书籍编辑事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onBookEdit(handler: (BookEditEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onBookEditHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}

/**
 * 设置书籍签名事件处理器
 * 注入线条填充组件特定的上下文：线条信息
 */
fun LineFillComponent.onBookSign(handler: (BookSignEventContext, Int, Int, Int, LineFillDirection) -> Unit) {
    eventHandlers.onBookSignHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getLength(), getDirection())
    }
}