package city.newnan.gui.page

import city.newnan.core.utils.text.toMiniMessage
import city.newnan.gui.component.IComponent
import city.newnan.gui.dsl.runSync
import city.newnan.gui.event.DestroyEventContext
import city.newnan.gui.event.EventContext
import city.newnan.gui.event.EventHandlers
import city.newnan.gui.event.InitEventContext
import city.newnan.gui.manager.logging.GuiLogger
import city.newnan.gui.manager.GuiManager
import city.newnan.gui.manager.scheduler.GuiScheduler
import city.newnan.gui.session.Session
import city.newnan.gui.session.SessionStorage
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * GUI页面基础实现
 *
 * 提供Page接口的基础实现，包括：
 * - 生命周期管理
 * - 组件管理
 * - 事件处理
 * - 渲染机制
 */
abstract class BasePage(
    override val player: Player,
    override val session: Session,
    override val guiManager: GuiManager,
    override val title: net.kyori.adventure.text.Component,
    override val inventoryType: InventoryType,
    override val size: Int = 0,
    private val builder: BasePage.() -> Unit = {}
) : Page {
    // 组件列表 - 线程安全
    private val _components = CopyOnWriteArrayList<IComponent<*>>()
    override val components: List<IComponent<*>> get() = _components.toList()

    // 槽位到组件的映射
    private val slotComponentMap = mutableMapOf<Int, IComponent<*>>()

    // 读写锁保护映射操作
    private val lock = ReentrantReadWriteLock()

    // 调度器
    override val scheduler: GuiScheduler by lazy {
        GuiScheduler(guiManager)
    }

    // 日志管理器
    override val logger: GuiLogger
        get() = guiManager.logger

    // 页面级别的事件处理器
    internal val eventHandlers = EventHandlers()

    // 页面状态
    private var _isVisible = false
    override val isVisible: Boolean get() = _isVisible

    abstract val inventory: Inventory

    private var closed = false
    private var initialized = false

    // ==================== 生命周期方法 ====================

    /**
     * 内部初始化方法，由Session调用
     * 仅在页面创建时调用一次
     */
    override fun initInternal() {
        if (initialized) return

        try {
            // 初始化资源
            onInit()
            initialized = true
            // 调用builder进行页面配置
            builder()
            eventHandlers.handleEvent(InitEventContext(player))
        } catch (e: Exception) {
            logger.logPageLifecycleError(
                page = this,
                operation = "INIT",
                error = e,
                context = mapOf(
                    "pageType" to (this::class.simpleName ?: "Unknown"),
                    "inventoryType" to inventoryType.name
                )
            )
            throw e
        }
    }

    /**
     * 内部显示方法，由Session调用
     * Page不应该直接调用此方法
     */
    override fun showInternal() {
        checkNotClosed()
        checkInitialized()

        if (!_isVisible) {
            try {
                _isVisible = true
                onShow()
                update() // 渲染所有组件
                player.openInventory(inventory)
            } catch (e: Exception) {
                logger.logPageLifecycleError(
                    page = this,
                    operation = "SHOW",
                    error = e,
                    context = mapOf(
                        "pageType" to (this::class.simpleName ?: "Unknown"),
                        "inventoryType" to inventoryType.name,
                        "componentCount" to _components.size
                    )
                )
                _isVisible = false
                throw e
            }
        }
    }

    /**
     * 内部隐藏方法，由Session调用
     * Page不应该直接调用此方法
     */
    override fun hideInternal() {
        if (_isVisible) {
            try {
                _isVisible = false
                onHide()
                if (player.openInventory.topInventory == inventory) {
                    player.closeInventory()
                }
            } catch (e: Exception) {
                logger.logPageLifecycleError(
                    page = this,
                    operation = "HIDE",
                    error = e,
                    context = mapOf(
                        "pageType" to (this::class.simpleName ?: "Unknown"),
                        "inventoryType" to inventoryType.name
                    )
                )
                throw e
            }
        }
    }

    override fun update() {
        checkNotClosed()
        checkInitialized()
        lock.write {
            // 清空inventory
            inventory.clear()

            // 重新渲染所有槽位
            val allSlots = (0 until inventory.size).toList()
            renderSlots(allSlots)
        }
    }

    override fun renderSlots(slots: List<Int>) {
        checkNotClosed()
        checkInitialized()
        // 在同步任务中获取物品
        runSync {
            lock.write {
                // 页面遍历所有格子，获取格子负责的component，调用component的渲染方法
                slots.forEach { slot ->
                    val component = slotComponentMap[slot] as IComponent<*>?
                    try {
                        if (component != null) {
                            // 获取当前槽位中的物品作为oldItem
                            val oldItem = inventory.getItem(slot)

                            // 调用组件的渲染方法
                            val newItem = component.renderSlot(slot, oldItem)

                            // 应用渲染结果
                            inventory.setItem(slot, newItem)
                        }
                    } catch (e: Exception) {
                        // 记录渲染错误但不中断其他槽位的渲染
                        if (component != null) {
                            onRenderError(component, e)
                        } else {
                            logger.logPageRenderError(
                                page = this,
                                slot = slot,
                                error = e,
                                context = mapOf(
                                    "pageType" to (this::class.simpleName ?: "Unknown"),
                                    "inventoryType" to inventoryType.name,
                                    "totalComponents" to _components.size
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun addComponent(component: IComponent<*>) {
        checkNotClosed()
        checkInitialized()
        lock.write {
            if (!_components.contains(component)) {
                _components.add(component)

                // 更新槽位映射 - 后定义的组件覆盖前面的
                component.getSlots().forEach { slot ->
                    val previousComponent = slotComponentMap[slot]
                    if (previousComponent != null && previousComponent != component) {
                        // 记录覆盖信息
                        logger.logComponentOverride(
                            page = this,
                            slot = slot,
                            previousComponent = previousComponent,
                            newComponent = component
                        )
                    }
                    slotComponentMap[slot] = component
                }

                // 如果页面已显示，立即渲染新组件的槽位
                if (_isVisible) {
                    try {
                        renderSlots(component.getSlots())
                    } catch (e: Exception) {
                        onRenderError(component, e)
                    }
                }

                onComponentAdded(component)
            }
        }
    }

    override fun removeComponent(component: IComponent<*>) {
        lock.write {
            if (_components.remove(component)) {
                // 关闭组件
                try {
                    component.close()
                } catch (e: Exception) {
                    // 忽略关闭时的异常
                    logger.logComponentCloseError(
                        component = component,
                        error = e,
                        context = mapOf(
                            "componentType" to (component::class.simpleName ?: "Unknown"),
                            "pageTitle" to title.toMiniMessage(),
                            "playerName" to player.name
                        )
                    )
                }

                // 清理槽位映射
                component.getSlots().forEach { slot ->
                    if (slotComponentMap[slot] != component) return
                    slotComponentMap.remove(slot)
                    inventory.let {
                        // 清空对应的inventory槽位
                        if (slot < it.size) {
                            it.setItem(slot, null)
                        }
                    }
                }

                onComponentRemoved(component)
            }
        }
    }

    override fun getComponentBySlot(slot: Int): IComponent<*>? {
        return lock.read { slotComponentMap[slot] as IComponent<*>? }
    }

    override fun clearComponents() {
        lock.write {
            val componentsToRemove = _components.toList()
            componentsToRemove.forEach { removeComponent(it) }
        }
    }

    override fun getPositionInSession(): Int {
        val pages = session.getAllPages()
        return pages.indexOf(this)
    }

    override fun chatInput(hide: Boolean, handler: (input: String) -> Boolean): Boolean {
        checkNotClosed()
        checkInitialized()

        val previousHandler = SessionStorage.getChatInputHandler(player)
        return if (previousHandler == null) {
            if (hide) {
                session.hide()
            }
            SessionStorage.setChatInputHandler(player, handler)
            true
        } else {
            false
        }
    }

    override fun handleEvent(context: EventContext<*>) {
        eventHandlers.handleEvent(context)
    }

    override fun destroyInternal() {
        if (closed) return

        try {
            // 标记为已关闭
            closed = true

            // 隐藏页面
            if (_isVisible) {
                hideInternal()
            }

            eventHandlers.handleEvent(DestroyEventContext(player))

            // 清理所有组件
            clearComponents()

            // 关闭调度器
            try {
                scheduler.close()
            } catch (e: Exception) {
                logger.logPageLifecycleError(
                    page = this,
                    operation = "DESTROY_SCHEDULER",
                    error = e,
                    context = mapOf(
                        "pageType" to (this::class.simpleName ?: "Unknown")
                    )
                )
            }

            // 调用子类清理逻辑
            onDestroy()

        } catch (e: Exception) {
            logger.logPageLifecycleError(
                page = this,
                operation = "DESTROY",
                error = e,
                context = mapOf(
                    "pageType" to (this::class.simpleName ?: "Unknown"),
                    "inventoryType" to inventoryType.name
                )
            )
            throw e
        }
    }

    // ==================== 检查方法 ====================

    /**
     * 检查页面是否未关闭
     */
    private fun checkNotClosed() {
        if (closed) {
            throw IllegalStateException("Page is closed")
        }
    }

    /**
     * 检查页面是否已初始化
     */
    private fun checkInitialized() {
        if (!initialized) {
            throw IllegalStateException("Page is not initialized")
        }
    }

    // ==================== 生命周期回调方法，子类可重写 ====================

    /**
     * 页面初始化时调用
     */
    protected open fun onInit() {}

    /**
     * 页面显示时调用
     */
    protected open fun onShow() {}

    /**
     * 页面隐藏时调用
     */
    protected open fun onHide() {}

    /**
     * 页面销毁时调用
     */
    protected open fun onDestroy() {}

    /**
     * 组件添加时调用
     */
    protected open fun onComponentAdded(component: IComponent<*>) {}

    /**
     * 组件移除时调用
     */
    protected open fun onComponentRemoved(component: IComponent<*>) {}

    /**
     * 组件渲染错误时调用
     */
    protected open fun onRenderError(component: IComponent<*>, error: Exception) {
        // 记录详细的错误日志
        logger.logComponentRenderError(
            component = component,
            slot = -1, // 页面级别的渲染错误，没有特定槽位
            error = error,
            context = mapOf(
                "pageType" to (this::class.simpleName ?: "Unknown"),
                "componentType" to (component::class.simpleName ?: "Unknown"),
                "inventoryType" to inventoryType.name,
                "componentSlots" to component.getSlots().size,
                "totalComponents" to _components.size
            )
        )
    }

    /**
     * 获取inventory的宽度
     */
    fun getInventoryWidth(): Int {
        return when (inventoryType) {
            InventoryType.CHEST -> 9
            InventoryType.DISPENSER -> 3
            InventoryType.DROPPER -> 3
            InventoryType.HOPPER -> 5
            InventoryType.WORKBENCH -> 3
            InventoryType.FURNACE -> 3
            InventoryType.BLAST_FURNACE -> 3
            InventoryType.SMOKER -> 3
            InventoryType.BREWING -> 3
            InventoryType.ANVIL -> 3
            InventoryType.SMITHING -> 3
            InventoryType.ENCHANTING -> 2
            InventoryType.GRINDSTONE -> 3
            InventoryType.CARTOGRAPHY -> 3
            InventoryType.LOOM -> 4
            InventoryType.STONECUTTER -> 2
            InventoryType.BEACON -> 1
            InventoryType.LECTERN -> 1
            InventoryType.SHULKER_BOX -> 9
            InventoryType.BARREL -> 9
            InventoryType.ENDER_CHEST -> 9
            InventoryType.COMPOSTER -> 1
            InventoryType.CHISELED_BOOKSHELF -> 6
            InventoryType.JUKEBOX -> 1
            else -> throw IllegalArgumentException("Unsupported inventory type: $inventoryType")
        }
    }

    override fun toString(): String {
        return "BasePage(player=${player.name}, title=${title.toMiniMessage()}, type=$inventoryType, size=$size, visible=$_isVisible, closed=$closed, components=${_components.size})"
    }
}
