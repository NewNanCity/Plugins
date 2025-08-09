package city.newnan.gui.component.patternfill

import city.newnan.gui.event.*

/**
 * 模式填充组件事件DSL扩展
 *
 * 为PatternFillComponent提供组件特定的事件处理方法
 * 每个方法都会注入模式填充组件相关的上下文信息，如字符、相对坐标等
 */

/**
 * 设置点击事件处理器
 * 注入模式填充组件特定的上下文：relativeX、relativeY（相对坐标）、char（字符）、charIndex（字符索引）
 */
fun PatternFillComponent.onClick(handler: (ClickEventContext, Int, Int, Char, Int) -> Unit) {
    eventHandlers.onClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PatternFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.char, renderContext.charIndex)
        }
    }
}

/**
 * 设置左键点击事件处理器
 * 注入模式填充组件特定的上下文：relativeX、relativeY（相对坐标）、char（字符）、charIndex（字符索引）
 */
fun PatternFillComponent.onLeftClick(handler: (ClickEventContext, Int, Int, Char, Int) -> Unit) {
    eventHandlers.onLeftClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PatternFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.char, renderContext.charIndex)
        }
    }
}

/**
 * 设置右键点击事件处理器
 * 注入模式填充组件特定的上下文：relativeX、relativeY（相对坐标）、char（字符）、charIndex（字符索引）
 */
fun PatternFillComponent.onRightClick(handler: (ClickEventContext, Int, Int, Char, Int) -> Unit) {
    eventHandlers.onRightClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PatternFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.char, renderContext.charIndex)
        }
    }
}

/**
 * 设置Shift点击事件处理器
 * 注入模式填充组件特定的上下文：relativeX、relativeY（相对坐标）、char（字符）、charIndex（字符索引）
 */
fun PatternFillComponent.onShiftClick(handler: (ClickEventContext, Int, Int, Char, Int) -> Unit) {
    eventHandlers.onShiftClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PatternFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.char, renderContext.charIndex)
        }
    }
}

/**
 * 设置Shift左键点击事件处理器
 * 注入模式填充组件特定的上下文：relativeX、relativeY（相对坐标）、char（字符）、charIndex（字符索引）
 */
fun PatternFillComponent.onShiftLeftClick(handler: (ClickEventContext, Int, Int, Char, Int) -> Unit) {
    eventHandlers.onShiftLeftClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PatternFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.char, renderContext.charIndex)
        }
    }
}

/**
 * 设置Shift右键点击事件处理器
 * 注入模式填充组件特定的上下文：relativeX、relativeY（相对坐标）、char（字符）、charIndex（字符索引）
 */
fun PatternFillComponent.onShiftRightClick(handler: (ClickEventContext, Int, Int, Char, Int) -> Unit) {
    eventHandlers.onShiftRightClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PatternFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.char, renderContext.charIndex)
        }
    }
}

/**
 * 设置中键点击事件处理器
 * 注入模式填充组件特定的上下文：relativeX、relativeY（相对坐标）、char（字符）、charIndex（字符索引）
 */
fun PatternFillComponent.onMiddleClick(handler: (ClickEventContext, Int, Int, Char, Int) -> Unit) {
    eventHandlers.onMiddleClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PatternFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.char, renderContext.charIndex)
        }
    }
}

/**
 * 设置双击事件处理器
 * 注入模式填充组件特定的上下文：relativeX、relativeY（相对坐标）、char（字符）、charIndex（字符索引）
 */
fun PatternFillComponent.onDoubleClick(handler: (ClickEventContext, Int, Int, Char, Int) -> Unit) {
    eventHandlers.onDoubleClickHandler = { context ->
        val renderContext = getRenderContext(context.slot) as? PatternFillRenderContext
        if (renderContext != null) {
            handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.char, renderContext.charIndex)
        }
    }
}

/**
 * 设置拖拽事件处理器
 * 注入模式填充组件特定的上下文：relativeX、relativeY（相对坐标）、char（字符）、charIndex（字符索引）
 */
fun PatternFillComponent.onDrag(handler: (DragEventContext, Int, Int, Char, Int) -> Unit) {
    eventHandlers.onDragHandler = { context ->
        // 对于拖拽事件，我们需要找到第一个相关的槽位
        val relevantSlot = context.slots.firstOrNull { containsSlot(it) }
        if (relevantSlot != null) {
            val renderContext = getRenderContext(relevantSlot) as? PatternFillRenderContext
            if (renderContext != null) {
                handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.char, renderContext.charIndex)
            }
        }
    }
}

/**
 * 设置单个物品拖拽事件处理器
 * 注入模式填充组件特定的上下文：relativeX、relativeY（相对坐标）、char（字符）、charIndex（字符索引）
 */
fun PatternFillComponent.onDragSingle(handler: (DragEventContext, Int, Int, Char, Int) -> Unit) {
    eventHandlers.onDragSingleHandler = { context ->
        val relevantSlot = context.slots.firstOrNull { containsSlot(it) }
        if (relevantSlot != null) {
            val renderContext = getRenderContext(relevantSlot) as? PatternFillRenderContext
            if (renderContext != null) {
                handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.char, renderContext.charIndex)
            }
        }
    }
}

/**
 * 设置平均分配拖拽事件处理器
 * 注入模式填充组件特定的上下文：relativeX、relativeY（相对坐标）、char（字符）、charIndex（字符索引）
 */
fun PatternFillComponent.onDragEven(handler: (DragEventContext, Int, Int, Char, Int) -> Unit) {
    eventHandlers.onDragEvenHandler = { context ->
        val relevantSlot = context.slots.firstOrNull { containsSlot(it) }
        if (relevantSlot != null) {
            val renderContext = getRenderContext(relevantSlot) as? PatternFillRenderContext
            if (renderContext != null) {
                handler.invoke(context, renderContext.relativeX, renderContext.relativeY, renderContext.char, renderContext.charIndex)
            }
        }
    }
}

/**
 * 设置销毁事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onDestroy(handler: (DestroyEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onDestroyHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

// 制作相关事件DSL

/**
 * 设置制作事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onCraft(handler: (CraftEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onCraftHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置准备制作事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onPrepareCraft(handler: (PrepareCraftEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onPrepareCraftHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置准备铁砧事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onPrepareAnvil(handler: (PrepareAnvilEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onPrepareAnvilHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置准备砂轮事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onPrepareGrindstone(handler: (PrepareGrindstoneEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onPrepareGrindstoneHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置准备锻造事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onPrepareSmithing(handler: (PrepareSmithingEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onPrepareSmithingHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置锻造物品事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onSmithItem(handler: (SmithItemEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onSmithItemHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

// 熔炉相关事件DSL

/**
 * 设置冶炼事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onSmelt(handler: (SmeltEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onSmeltHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置熔炉燃烧事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onFurnaceBurn(handler: (FurnaceBurnEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onFurnaceBurnHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置熔炉提取事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onFurnaceExtract(handler: (FurnaceExtractEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onFurnaceExtractHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置熔炉开始冶炼事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onFurnaceStartSmelt(handler: (FurnaceStartSmeltEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onFurnaceStartSmeltHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

// 酿造相关事件DSL

/**
 * 设置酿造事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onBrew(handler: (BrewEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onBrewHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置酿造燃料事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onBrewingFuel(handler: (BrewingFuelEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onBrewingFuelHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

// 特殊Inventory事件DSL

/**
 * 设置物品移动事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onItemMove(handler: (ItemMoveEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onItemMoveHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置物品拾取事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onPickupItem(handler: (PickupItemEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onPickupItemHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置创造模式事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onCreative(handler: (CreativeEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onCreativeHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

// 其他事件DSL

/**
 * 设置丢弃事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onDrop(handler: (DropEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onDropHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置交易选择事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onTradeSelect(handler: (TradeSelectEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onTradeSelectHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

// Book相关事件DSL

/**
 * 设置书籍翻页事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onBookPageTurn(handler: (BookPageTurnEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onBookPageTurnHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置书籍编辑事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onBookEdit(handler: (BookEditEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onBookEditHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}

/**
 * 设置书籍签名事件处理器
 * 注入模式填充组件特定的上下文：模式信息
 */
fun PatternFillComponent.onBookSign(handler: (BookSignEventContext, Int, Int, Int, Int) -> Unit) {
    eventHandlers.onBookSignHandler = { context ->
        handler.invoke(context, getStartX(), getStartY(), getWidth(), getHeight())
    }
}