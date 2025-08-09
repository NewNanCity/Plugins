package city.newnan.gui.component

import city.newnan.core.terminable.Terminable
import city.newnan.core.terminable.TerminableConsumer
import city.newnan.gui.component.base.BaseRenderContext
import city.newnan.gui.event.*
import city.newnan.gui.manager.logging.GuiLogger
import city.newnan.gui.manager.GuiManager
import city.newnan.gui.page.Page
import city.newnan.gui.manager.scheduler.GuiScheduler
import org.bukkit.inventory.ItemStack

/**
 * GUI组件接口
 *
 * Component是页面的组件，类似浏览器DOM元素。
 * 每个Page可包含多个Component，Component负责渲染特定区域的内容。
 *
 * 特性：
 * - 层次结构：每个Component属于某个Page
 * - 生命周期管理：实现Terminable和TerminableConsumer
 * - 事件处理：支持各种inventory事件
 * - 渲染机制：基于RenderContext的类型安全渲染
 */
interface IComponent<T : BaseRenderContext> : Terminable, TerminableConsumer {

    /**
     * 组件所属的页面
     */
    val page: Page

    /**
     * GUI日志
     */
    val logger: GuiLogger

    /**
     * 任务调度器
     */
    val scheduler: GuiScheduler

    /**
     * GUI管理器
     */
    val guiManager: GuiManager

    /**
     * 组件占用的槽位列表
     */
    fun getSlots(): List<Int>

    /**
     * 渲染组件
     * 根据当前状态更新inventory中对应的槽位
     */
    fun render()

    /**
     * 由页面调用的渲染方法
     * 页面遍历所有格子，获取格子负责的component，调用此方法进行渲染
     *
     * @param slot 要渲染的槽位
     * @param oldItem 当前槽位中的物品
     * @return 渲染后的物品，null表示清空槽位
     */
    fun renderSlot(slot: Int, oldItem: ItemStack?): ItemStack?

    /**
     * 更新组件
     * 通知页面重新渲染组件的槽位
     */
    fun update() {
        page.renderSlots(getSlots())
    }

    /**
     * 检查指定槽位是否属于此组件
     */
    fun containsSlot(slot: Int): Boolean {
        return getSlots().contains(slot)
    }

    /**
     * 获取指定槽位的渲染上下文
     * 如果槽位不属于此组件则返回null
     */
    fun getRenderContext(slot: Int): T?

    /**
     * 处理事件
     * 组件可以处理发生在其槽位上的事件
     */
    fun handleEvent(context: EventContext<*>)
}
