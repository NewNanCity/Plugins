package city.newnan.gui.component.base

import org.bukkit.inventory.ItemStack

/**
 * 渲染上下文密封类
 *
 * 使用密封类统一不同Component的渲染参数，提供类型安全的参数传递。
 * 每种Component类型都有对应的渲染上下文，包含该类型特有的参数。
 */
abstract class BaseRenderContext {
    /**
     * X坐标（列，从0开始）
     */
    abstract val x: Int

    /**
     * Y坐标（行，从0开始）
     */
    abstract val y: Int

    /**
     * 槽位索引
     */
    abstract val slot: Int

    /**
     * 上次渲染的物品
     * 首次渲染时为null，后续渲染时为上次的结果
     */
    abstract val oldItem: ItemStack?
}