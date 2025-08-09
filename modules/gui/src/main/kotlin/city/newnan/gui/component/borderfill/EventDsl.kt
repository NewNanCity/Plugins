package city.newnan.gui.component.borderfill

import city.newnan.gui.event.*

/**
 * 边框填充组件事件DSL扩展
 *
 * 为BorderFillComponent提供组件特定的事件处理方法
 * 每个方法都会注入边框填充组件相关的上下文信息，如边框类型、相对坐标等
 */

/**
 * 设置点击事件处理器
 * 注入边框填充组件特定的上下文：relativeX、relativeY（相对坐标）、borderType（边框类型）
 */
fun BorderFillComponent.onClick(handler: (ClickEventContext, Int, Int, BorderType) -> Unit) {
    eventHandlers.onClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? BorderFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.borderType)
        }
    }
}

/**
 * 设置左键点击事件处理器
 * 注入边框填充组件特定的上下文：relativeX、relativeY（相对坐标）、borderType（边框类型）
 */
fun BorderFillComponent.onLeftClick(handler: (ClickEventContext, Int, Int, BorderType) -> Unit) {
    eventHandlers.onLeftClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? BorderFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.borderType)
        }
    }
}

/**
 * 设置右键点击事件处理器
 * 注入边框填充组件特定的上下文：relativeX、relativeY（相对坐标）、borderType（边框类型）
 */
fun BorderFillComponent.onRightClick(handler: (ClickEventContext, Int, Int, BorderType) -> Unit) {
    eventHandlers.onRightClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? BorderFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.borderType)
        }
    }
}

/**
 * 设置Shift点击事件处理器
 * 注入边框填充组件特定的上下文：relativeX、relativeY（相对坐标）、borderType（边框类型）
 */
fun BorderFillComponent.onShiftClick(handler: (ClickEventContext, Int, Int, BorderType) -> Unit) {
    eventHandlers.onShiftClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? BorderFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.borderType)
        }
    }
}

/**
 * 设置Shift左键点击事件处理器
 * 注入边框填充组件特定的上下文：relativeX、relativeY（相对坐标）、borderType（边框类型）
 */
fun BorderFillComponent.onShiftLeftClick(handler: (ClickEventContext, Int, Int, BorderType) -> Unit) {
    eventHandlers.onShiftLeftClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? BorderFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.borderType)
        }
    }
}

/**
 * 设置Shift右键点击事件处理器
 * 注入边框填充组件特定的上下文：relativeX、relativeY（相对坐标）、borderType（边框类型）
 */
fun BorderFillComponent.onShiftRightClick(handler: (ClickEventContext, Int, Int, BorderType) -> Unit) {
    eventHandlers.onShiftRightClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? BorderFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.borderType)
        }
    }
}

/**
 * 设置中键点击事件处理器
 * 注入边框填充组件特定的上下文：relativeX、relativeY（相对坐标）、borderType（边框类型）
 */
fun BorderFillComponent.onMiddleClick(handler: (ClickEventContext, Int, Int, BorderType) -> Unit) {
    eventHandlers.onMiddleClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? BorderFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.borderType)
        }
    }
}

/**
 * 设置双击事件处理器
 * 注入边框填充组件特定的上下文：relativeX、relativeY（相对坐标）、borderType（边框类型）
 */
fun BorderFillComponent.onDoubleClick(handler: (ClickEventContext, Int, Int, BorderType) -> Unit) {
    eventHandlers.onDoubleClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? BorderFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.borderType)
        }
    }
}

/**
 * 设置拖拽事件处理器
 * 注入边框填充组件特定的上下文：relativeX、relativeY（相对坐标）、borderType（边框类型）
 */
fun BorderFillComponent.onDrag(handler: (DragEventContext, Int, Int, BorderType) -> Unit) {
    eventHandlers.onDragHandler = { context ->
        // 对于拖拽事件，我们需要找到第一个相关的槽位
        val relevantSlot = context.slots.firstOrNull { containsSlot(it) }
        if (relevantSlot != null) {
            val renderContext = getRenderContext(relevantSlot) as? BorderFillRenderContext
            if (renderContext != null) {
                handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.borderType)
            }
        }
    }
}

/**
 * 设置单个物品拖拽事件处理器
 * 注入边框填充组件特定的上下文：relativeX、relativeY（相对坐标）、borderType（边框类型）
 */
fun BorderFillComponent.onDragSingle(handler: (DragEventContext, Int, Int, BorderType) -> Unit) {
    eventHandlers.onDragSingleHandler = { context ->
        val relevantSlot = context.slots.firstOrNull { containsSlot(it) }
        if (relevantSlot != null) {
            val renderContext = getRenderContext(relevantSlot) as? BorderFillRenderContext
            if (renderContext != null) {
                handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.borderType)
            }
        }
    }
}

/**
 * 设置平均分配拖拽事件处理器
 * 注入边框填充组件特定的上下文：relativeX、relativeY（相对坐标）、borderType（边框类型）
 */
fun BorderFillComponent.onDragEven(handler: (DragEventContext, Int, Int, BorderType) -> Unit) {
    eventHandlers.onDragEvenHandler = { context ->
        val relevantSlot = context.slots.firstOrNull { containsSlot(it) }
        if (relevantSlot != null) {
            val renderContext = getRenderContext(relevantSlot) as? BorderFillRenderContext
            if (renderContext != null) {
                handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.borderType)
            }
        }
    }
}

/**
 * 设置销毁事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onDestroy(handler: (DestroyEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onDestroyHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

// 制作相关事件DSL

/**
 * 设置制作事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onCraft(handler: (CraftEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onCraftHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置准备制作事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onPrepareCraft(handler: (PrepareCraftEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onPrepareCraftHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置准备铁砧事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onPrepareAnvil(handler: (PrepareAnvilEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onPrepareAnvilHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置准备砂轮事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onPrepareGrindstone(handler: (PrepareGrindstoneEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onPrepareGrindstoneHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置准备锻造事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onPrepareSmithing(handler: (PrepareSmithingEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onPrepareSmithingHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置锻造物品事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onSmithItem(handler: (SmithItemEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onSmithItemHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

// 熔炉相关事件DSL

/**
 * 设置冶炼事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onSmelt(handler: (SmeltEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onSmeltHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置熔炉燃烧事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onFurnaceBurn(handler: (FurnaceBurnEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onFurnaceBurnHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置熔炉提取事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onFurnaceExtract(handler: (FurnaceExtractEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onFurnaceExtractHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置熔炉开始冶炼事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onFurnaceStartSmelt(handler: (FurnaceStartSmeltEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onFurnaceStartSmeltHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

// 酿造相关事件DSL

/**
 * 设置酿造事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onBrew(handler: (BrewEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onBrewHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置酿造燃料事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onBrewingFuel(handler: (BrewingFuelEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onBrewingFuelHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

// 特殊Inventory事件DSL

/**
 * 设置物品移动事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onItemMove(handler: (ItemMoveEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onItemMoveHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置物品拾取事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onPickupItem(handler: (PickupItemEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onPickupItemHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置创造模式事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onCreative(handler: (CreativeEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onCreativeHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

// 其他事件DSL

/**
 * 设置丢弃事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onDrop(handler: (DropEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onDropHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置交易选择事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onTradeSelect(handler: (TradeSelectEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onTradeSelectHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

// Book相关事件DSL

/**
 * 设置书籍翻页事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onBookPageTurn(handler: (BookPageTurnEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onBookPageTurnHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置书籍编辑事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onBookEdit(handler: (BookEditEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onBookEditHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置书籍签名事件处理器
 * 注入边框填充组件特定的上下文：区域信息
 */
fun BorderFillComponent.onBookSign(handler: (BookSignEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onBookSignHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}