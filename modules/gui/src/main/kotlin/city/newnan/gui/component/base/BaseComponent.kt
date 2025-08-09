package city.newnan.gui.component.base

import city.newnan.core.terminable.CompositeTerminable
import city.newnan.core.utils.text.toMiniMessage
import city.newnan.gui.component.IComponent

import city.newnan.gui.event.*
import city.newnan.gui.manager.logging.GuiLogger
import city.newnan.gui.manager.GuiManager
import city.newnan.gui.page.BasePage
import city.newnan.gui.manager.scheduler.GuiScheduler
import org.bukkit.inventory.ItemStack

/**
 * 组件基础实现
 *
 * 提供Component接口的基础实现，包括：
 * - 生命周期管理
 * - 资源管理
 * - 事件处理
 * - 错误处理
 */
abstract class BaseComponent<T : BaseRenderContext>(
    override val page: BasePage,
) : IComponent<T> {

    // 资源管理器
    private val terminableRegistry = CompositeTerminable.create()


    // 事件处理器
    open val eventHandlers = EventHandlers()

    // 调度器
    override val scheduler: GuiScheduler
        get() = page.scheduler

    // 日志管理器
    override val logger: GuiLogger
        get() = guiManager.logger

    // 界面管理器
    override val guiManager: GuiManager
        get() = page.guiManager

    // 组件状态
    private var closed = false

    override fun render() {
        checkNotClosed()
        // 组件不再自己处理渲染，而是通知页面进行渲染
        page.renderSlots(getSlots())
    }

    /**
     * 由页面调用的渲染方法
     * 页面遍历所有格子，获取格子负责的component，调用此方法进行渲染
     *
     * @param slot 要渲染的槽位
     * @param oldItem 当前槽位中的物品
     * @return 渲染后的物品，null表示清空槽位
     */
    override fun renderSlot(slot: Int, oldItem: ItemStack?): ItemStack? {
        checkNotClosed()

        return try {
            // 计算渲染上下文
            val context = createRenderContext(slot, oldItem)
            if (context != null) {
                // 调用渲染处理函数
                renderSlot(context)
            } else {
                null
            }
        } catch (e: Exception) {
            // 渲染失败时返回null
            onRenderError(slot, e)
            null
        }
    }

    /**
     * 创建渲染上下文
     * 使用页面传入的oldItem创建渲染上下文
     */
    private fun createRenderContext(slot: Int, oldItem: ItemStack?): T? {
        // 调用子类的getRenderContext方法，并传入oldItem
        return getRenderContext(slot, oldItem)
    }

    /**
     * 获取指定槽位的渲染上下文（带oldItem参数）
     * 子类应该重写此方法
     */
    protected open fun getRenderContext(slot: Int, oldItem: ItemStack?): T? {
        // 默认实现：调用原来的getRenderContext方法
        return getRenderContext(slot)
    }


    /**
     * 渲染单个槽位
     * 子类需要实现此方法来定义具体的渲染逻辑
     */
    protected abstract fun renderSlot(context: T): ItemStack?


    // 事件处理实现
    override fun handleEvent(context: EventContext<*>) {
        eventHandlers.handleEvent(context)
    }

    // TerminableConsumer 实现
    override fun <T : AutoCloseable> bind(terminable: T): T {
        checkNotClosed()
        return terminableRegistry.bind(terminable)
    }



    // Terminable 实现
    override fun isClosed(): Boolean = closed

    override fun close() {
        if (closed) return

        closed = true

        handleEvent(DestroyEventContext(page.player))

        // 关闭调度器
        try {
            scheduler.close()
        } catch (e: Exception) {
            logger.logComponentCloseError(
                component = this,
                error = e,
                context = mapOf(
                    "componentType" to (this::class.simpleName ?: "Unknown"),
                    "pageTitle" to page.title.toMiniMessage(),
                    "playerName" to page.player.name
                )
            )
        }


        // 清理所有绑定的资源
        terminableRegistry.closeAndReportException()

        // 调用子类清理逻辑
        onClose()
    }

    /**
     * 检查组件是否未关闭
     */
    protected fun checkNotClosed() {
        if (closed) {
            throw IllegalStateException("Component is closed")
        }
    }

    // 生命周期回调方法，子类可重写

    /**
     * 渲染完成时调用
     */
    protected open fun onRenderComplete() {}

    /**
     * 渲染错误时调用
     */
    protected open fun onRenderError(slot: Int, error: Exception) {
        // 记录详细的错误日志
        logger.logComponentRenderError(
            component = this,
            slot = slot,
            error = error,
            context = mapOf(
                "componentType" to (this::class.simpleName ?: "Unknown"),
                "pageTitle" to page.title.toMiniMessage(),
                "playerName" to page.player.name,
                "inventoryType" to page.inventoryType.name,
                "isVisible" to page.isVisible.toString(),
                "componentSlots" to getSlots().size,
            )
        )
    }

    /**
     * 组件关闭时调用
     */
    protected open fun onClose() {}

    override fun toString(): String {
        return "${this::class.simpleName}(page=${page.title.toMiniMessage()}, slots=${getSlots()}, closed=$closed)"
    }
}
