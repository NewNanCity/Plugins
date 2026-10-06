package city.newnan.gui.event

import city.newnan.core.event.EventDSLBuilder
import city.newnan.core.event.EventSubscription
import city.newnan.core.utils.text.toPlain
import city.newnan.gui.component.IComponent
import city.newnan.gui.component.IStorageComponent
import city.newnan.gui.dsl.runSyncLater
import city.newnan.gui.manager.GuiManager
import city.newnan.gui.page.BasePage
import city.newnan.gui.page.BookPage
import city.newnan.gui.page.Page
import city.newnan.gui.session.SessionStorage
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryCreativeEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.concurrent.TimeUnit

/**
 * 全局事件监听器
 *
 * 负责监听所有GUI相关的事件，并分发到对应的Page和Component。
 * 使用责任链模式，确保事件被正确处理。
 * 使用plugin的event DSL注册事件，支持自动销毁。
 *
 * 全局唯一实例，由SessionStorage管理。
 */
object GlobalEventListener {
    inline fun <reified T : Event> subscribeEvent(
        guiManager: GuiManager,
        block: EventDSLBuilder<T>.() -> Unit
    ): EventSubscription<T> {
        val builder = EventDSLBuilder(guiManager.plugin, T::class.java)
        builder.block()
        val subscription = builder.build()

        // 自动绑定到插件生命周期
        guiManager.bind(subscription)

        return subscription
    }

    /**
     * 注册事件监听器
     * 使用plugin的event DSL而不是传统的Listener接口
     */
    fun register(guiManager: GuiManager) {
        // 使用events DSL注册各种inventory事件
        subscribeEvent<InventoryClickEvent>(guiManager) {
            priority(EventPriority.LOWEST)
            filter { SessionStorage.currentGuiManager == guiManager }
            handler { event -> onInventoryClick(event) }
        }
        subscribeEvent<InventoryDragEvent>(guiManager) {
            priority(EventPriority.LOWEST)
            filter { SessionStorage.currentGuiManager == guiManager }
            handler { event -> onInventoryDrag(event) }
        }
        subscribeEvent<InventoryCloseEvent>(guiManager) {
            priority(EventPriority.LOWEST)
            filter { SessionStorage.currentGuiManager == guiManager }
            handler { event -> onInventoryClose(event) }
        }
        subscribeEvent<InventoryOpenEvent>(guiManager) {
            priority(EventPriority.LOWEST)
            filter { SessionStorage.currentGuiManager == guiManager }
            handler { event -> onInventoryOpen(event) }
        }
        subscribeEvent<AsyncChatEvent>(guiManager) {
            priority(EventPriority.LOWEST)
            filter { SessionStorage.currentGuiManager == guiManager }
            handler { event -> onAsyncPlayerChat(event) }
        }
        subscribeEvent<PlayerQuitEvent>(guiManager) {
            priority(EventPriority.LOWEST)
            filter { SessionStorage.currentGuiManager == guiManager }
            handler { event -> onPlayerQuit(event) }
        }
        subscribeEvent<PlayerDropItemEvent>(guiManager) {
            priority(EventPriority.LOWEST)
            filter { SessionStorage.currentGuiManager == guiManager }
            handler { event -> onPlayerDropItem(event) }
        }
        subscribeEvent<PlayerEditBookEvent>(guiManager) {
            priority(EventPriority.LOWEST)
            filter { SessionStorage.currentGuiManager == guiManager }
            handler { event -> onPlayerEditBook(event) }
        }

        // 物品保护机制相关事件
        subscribeEvent<InventoryMoveItemEvent>(guiManager) {
            priority(EventPriority.LOWEST)
            filter { SessionStorage.currentGuiManager == guiManager }
            handler { event -> onInventoryMoveItem(event) }
        }
        subscribeEvent<InventoryCreativeEvent>(guiManager) {
            priority(EventPriority.LOWEST)
            filter { SessionStorage.currentGuiManager == guiManager }
            handler { event -> onInventoryCreative(event) }
        }
    }

    fun onInventoryClick(event: InventoryClickEvent) {
        // 检查玩家
        val player = event.whoClicked as? Player ?: return

        // 查找对应的Page
        val page = findPageByInventory(player, event.inventory) ?: return

        // 查找对应的Component
        val component = page.getComponentBySlot(event.slot)

        // 创建事件上下文
        val context = ClickEventContext(
            event = event,
            player = player,
            slot = event.slot,
            clickType = event.click,
            item = event.currentItem
        )

        // 先尝试让Component处理
        if (component != null) {
            try {
                component.handleEvent(context)
            } catch (e: Exception) {
                // 记录事件处理错误
                page.logger.logEventHandlingError(
                    component = component,
                    event = event,
                    error = e,
                    eventType = "CLICK",
                    context = mapOf(
                        "componentType" to (component::class.simpleName ?: "Unknown"),
                        "pageTitle" to page.title,
                        "playerName" to player.name,
                        "slot" to event.slot,
                        "clickType" to event.click.name
                    )
                )
            }
        }

        // 如果Component没有处理或者传播没有停止，让Page处理
        if (!context.isPropagationStopped()) {
            try {
                // Page级别的事件处理
                page.handleEvent(context)
            } catch (e: Exception) {
                // 记录事件处理错误
                page.logger.logPageEventError(
                    event = event,
                    error = e,
                    eventType = "CLICK",
                    context = mapOf(
                        "componentType" to (page::class.simpleName ?: "Unknown"),
                        "pageTitle" to page.title,
                        "playerName" to player.name,
                        "slot" to event.slot,
                        "clickType" to event.click.name
                    )
                )
            }
        }

        // 🛡️ 物品保护机制：实施全面的物品保护
        applyItemProtection(event, page, component)

        // 📦 通知StorageComponent物品变化
        if (component != null && isStorageComponent(component) && !event.isCancelled) {
            try {
                val storageComponent = component as IStorageComponent
                val oldItem = event.currentItem

                // 延迟一tick执行，确保inventory状态已更新
                page.runSyncLater(1L, TimeUnit.MILLISECONDS) {
                    val newItem = event.inventory.getItem(event.slot)
                    storageComponent.handleItemChange(event.slot, oldItem, newItem)
                }
            } catch (e: Exception) {
                page.logger.logEventHandlingError(
                    component = component,
                    event = event,
                    error = e,
                    eventType = "ITEM_CHANGE",
                    context = mapOf(
                        "componentType" to (component::class.simpleName ?: "Unknown"),
                        "pageTitle" to page.title,
                        "playerName" to player.name,
                        "slot" to event.slot
                    )
                )
            }
        }
    }

    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return

        // 查找对应的Page
        val page = findPageByInventory(player, event.inventory) ?: return

        // 创建事件上下文
        val context = DragEventContext(
            event = event,
            player = player,
            dragType = event.type,
            slots = event.inventorySlots,
            items = event.newItems
        )

        // 找到所有相关的Component（只处理StorageComponent）
        val relevantComponents = event.inventorySlots.mapNotNull { slot ->
            val component = page.getComponentBySlot(slot)
            if (component != null && isStorageComponent(component)) component else null
        }.distinct()

        // 让每个相关StorageComponent处理事件
        for (component in relevantComponents) {
            try {
                component.handleEvent(context)
                if (context.isPropagationStopped()) break
            } catch (e: Exception) {
                // 记录事件处理错误
                page.logger.logEventHandlingError(
                    component = component,
                    event = event,
                    error = e,
                    eventType = "DRAG",
                    context = mapOf(
                        "componentType" to (component::class.simpleName ?: "Unknown"),
                        "pageTitle" to page.title,
                        "playerName" to player.name,
                        "slots" to event.inventorySlots.toString(),
                        "dragType" to event.type.name
                    )
                )
            }
        }

        // 如果Component没有处理或者传播没有停止，让Page处理
        if (!context.isPropagationStopped()) {
            try {
                // Page级别的事件处理
                page.handleEvent(context)
            } catch (e: Exception) {
                // 记录事件处理错误
                page.logger.logPageEventError(
                    event = event,
                    error = e,
                    eventType = "DRAG",
                    context = mapOf(
                        "componentType" to (page::class.simpleName ?: "Unknown"),
                        "pageTitle" to page.title,
                        "playerName" to player.name,
                        "slots" to event.inventorySlots.toString(),
                        "dragType" to event.type.name
                    )
                )
            }
        }
        // 🛡️ 物品保护机制：实施拖拽保护
        applyDragProtection(event, page)
        // 📦 通知StorageComponent物品变化
        if (!event.isCancelled && relevantComponents.isNotEmpty()) {
            try {
                // 记录拖拽前的物品状态
                val oldItems = mutableMapOf<Int, ItemStack?>()
                event.inventorySlots.forEach { slot ->
                    oldItems[slot] = event.inventory.getItem(slot)?.clone()
                }

                // 延迟一tick执行，确保inventory状态已更新
                page.runSyncLater(1L, TimeUnit.MILLISECONDS) {
                    event.inventorySlots.forEach { slot ->
                        val component = page.getComponentBySlot(slot)
                        if (component != null && isStorageComponent(component)) {
                            val storageComponent = component as IStorageComponent
                            val oldItem = oldItems[slot]
                            val newItem = event.inventory.getItem(slot)
                            storageComponent.handleItemChange(slot, oldItem, newItem)
                        }
                    }
                }
            } catch (e: Exception) {
                page.logger.logPageEventError(
                    event = event,
                    error = e,
                    eventType = "DRAG_ITEM_CHANGE",
                    context = mapOf(
                        "pageTitle" to page.title,
                        "playerName" to player.name,
                        "slots" to event.inventorySlots.toString()
                    )
                )
            }
        }
    }

    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return

        // 查找对应的Page
        val page = findPageByInventory(player, event.inventory) ?: return        // 创建事件上下文

        val context = HideEventContext(
            player = player,
            reason = event.reason.toString()
        )

        // 通知所有Component
        page.components.forEach { component ->
            try {
                component.handleEvent(context)
            } catch (e: Exception) {
                // 记录事件处理错误
                page.logger.logEventHandlingError(
                    component = component,
                    event = event,
                    error = e,
                    eventType = "CLOSE",
                    context = mapOf(
                        "componentType" to (component::class.simpleName ?: "Unknown"),
                        "pageTitle" to page.title,
                        "playerName" to player.name,
                        "reason" to event.reason.name
                    )
                )
            }
        }

        // 如果Component没有处理或者传播没有停止，让Page处理
        if (!context.isPropagationStopped()) {
            try {
                // Page级别的事件处理
                page.handleEvent(context)
            } catch (e: Exception) {
                // 记录事件处理错误
                page.logger.logPageEventError(
                    event = event,
                    error = e,
                    eventType = "CLOSE",
                    context = mapOf(
                        "componentType" to (page::class.simpleName ?: "Unknown"),
                        "pageTitle" to page.title,
                        "playerName" to player.name,
                        "reason" to event.reason.name
                    )
                )
            }
        }

        // 用户按Esc关闭，或者触发其他插件打开容器界面，直接清空会话
        if (page.isVisible) {
            page.session.clearPages()
        }
    }

    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return

        // 查找对应的Page
        val page = findPageByInventory(player, event.inventory) ?: return        // 创建事件上下文

        val context = ShowEventContext(
            player = player
        )

        // 通知所有Component
        page.components.forEach { component ->
            try {
                component.handleEvent(context)
            } catch (e: Exception) {
                // 记录事件处理错误
                page.logger.logEventHandlingError(
                    component = component,
                    event = event,
                    error = e,
                    eventType = "OPEN",
                    context = mapOf(
                        "componentType" to (component::class.simpleName ?: "Unknown"),
                        "pageTitle" to page.title,
                        "playerName" to player.name
                    )
                )
            }
        }

        // 如果Component没有处理或者传播没有停止，让Page处理
        if (!context.isPropagationStopped()) {
            try {
                // Page级别的事件处理
                page.handleEvent(context)
            } catch (e: Exception) {
                // 记录事件处理错误
                page.logger.logPageEventError(
                    event = event,
                    error = e,
                    eventType = "OPEN",
                    context = mapOf(
                        "componentType" to (page::class.simpleName ?: "Unknown"),
                        "pageTitle" to page.title,
                        "playerName" to player.name
                    )
                )
            }
        }
    }

    fun onAsyncPlayerChat(event: AsyncChatEvent) {
        val player = event.player
        val handler = SessionStorage.getChatInputHandler(player)

        if (handler != null) {
            event.isCancelled = true

            try {
                val message = event.message()
                val shouldEnd = handler.invoke(message)

                if (shouldEnd) {
                    SessionStorage.removeChatInputHandler(player)
                }
            } catch (e: Exception) {
                // 处理异常，清理ChatInput状态
                SessionStorage.removeChatInputHandler(player)
                player.sendMessage("§c输入处理时发生错误，已取消输入。")
                e.printStackTrace()
            }
        }
    }

    fun onPlayerQuit(event: PlayerQuitEvent) {
        // 清理玩家的所有session
        SessionStorage.clearPlayerSessions(event.player)

        // 清理ChatInput处理器
        SessionStorage.clearChatInputHandler(event.player)
    }

    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player

        // 查找玩家当前的Page
        val session = SessionStorage.getSession(player)
        val currentPage = session?.current()

        if (currentPage != null && currentPage.isVisible) {
            // 创建事件上下文
            val context = DropEventContext(
                event = event,
                player = player,
                item = event.itemDrop.itemStack
            )

            // 通知所有Component
            currentPage.components.forEach { component ->
                component.handleEvent(context)
            }

            // 🛡️ 物品保护机制：防止玩家通过丢弃键从GUI中取出物品
            // 如果玩家正在查看GUI，采用保守策略禁止丢弃物品
            // TODO: 实现更精确的物品来源检查
            if (currentPage is BasePage) {
                applyDropProtection(event, currentPage)
            }
        }
    }

    fun onPlayerEditBook(event: PlayerEditBookEvent) {
        val player = event.player

        // 查找玩家当前的BookPage
        val session = SessionStorage.getSession(player)
        val currentPage = session?.current()

        if (currentPage is BookPage && currentPage.isVisible) {
            try {
                // 让BookPage处理编辑事件
                currentPage.handleBookEdited(event)
            } catch (e: Exception) {
                // 记录事件处理错误
                currentPage.logger.logPageEventError(
                    event = event,
                    error = e,
                    eventType = "BOOK_EDIT",
                    context = mapOf(
                        "pageType" to (currentPage::class.simpleName ?: "Unknown"),
                        "pageTitle" to currentPage.title,
                        "playerName" to player.name,
                        "slot" to event.slot,
                        "isSigning" to event.isSigning
                    )
                )
            }
        }
    }

    /**
     * 根据inventory查找对应的Page
     */
    private fun findPageByInventory(player: Player, inventory: Inventory): BasePage? {
        val sessions = SessionStorage.getPlayerSessions(player)

        for (session in sessions.values) {
            val currentPage = session.current()
            if (currentPage is BasePage && currentPage.inventory == inventory) {
                return currentPage
            }
        }

        return null
    }

    /**
     * 处理inventory移动物品事件
     * 防止漏斗等自动化设备从GUI中取出物品
     */
    fun onInventoryMoveItem(event: InventoryMoveItemEvent) {
        // 检查是否有玩家正在使用这个inventory作为GUI
        val sourceInventory = event.source

        // 遍历所有活跃的GUI会话
        SessionStorage.getAllActiveSessions().forEach { (_, session) ->
            val currentPage = session.current()
            if (currentPage is BasePage && currentPage.inventory == sourceInventory) {
                // 如果有玩家正在使用这个inventory作为GUI，禁止自动移动物品
                event.isCancelled = true
                return
            }
        }
    }

    /**
     * 处理创造模式inventory事件
     * 防止创造模式玩家复制GUI中的物品
     */
    fun onInventoryCreative(event: InventoryCreativeEvent) {
        val player = event.whoClicked as? Player ?: return

        // 查找对应的Page
        findPageByInventory(player, event.inventory) ?: return

        // 如果操作的是GUI inventory，禁止创造模式操作
        event.isCancelled = true
    }

    /**
     * 检查组件是否为StorageComponent
     * 只有StorageComponent才允许物品交互
     */
    private fun isStorageComponent(component: IComponent<*>): Boolean {
        return component is IStorageComponent && component.isStorageComponent()
    }

    /**
     * 应用物品保护机制到点击事件
     */
    private fun applyItemProtection(event: InventoryClickEvent, page: Page, component: IComponent<*>?) {
        // 如果用户点击的是自己的背包
        if (event.clickedInventory == event.whoClicked.inventory) {
            // 忽略非自动转移
            if (!event.isShiftClick) return
            event.isCancelled = true
            // 忽略空气
            if (event.cursor.type == Material.AIR || page.size == null) return
            // 自动转移，自动寻找第一个能够接收物品的槽位
            for (slot in 0 until page.size!!) {
                val targetComponent = page.getComponentBySlot(slot)
                if (targetComponent != null && isStorageComponent(targetComponent)) {
                    val storageComponent = targetComponent as IStorageComponent
                    if (storageComponent.canAcceptItem(event.cursor)) {
                        val oldItem = storageComponent.getStoredItem(slot)
                        if (oldItem != null && oldItem.type != Material.AIR) continue
                        val newItem = event.cursor.clone()
                        storageComponent.setStoredItem(slot, newItem)
                        storageComponent.handleItemChange(slot, oldItem, newItem)
                        event.cursor.amount = 0
                        break
                    }
                }
            }
            return
        }

        // 检查是否为存储组件
        val isStorageComponent = component != null && isStorageComponent(component)

        // 对于非存储组件，实施物品保护
        if (!isStorageComponent) {
            event.isCancelled = true
        } else {
            // 对于存储组件，进行额外的权限检查
            val storageComponent = component as IStorageComponent

            when {
                // 检查是否可以放入物品
                event.cursor.type != Material.AIR -> {
                    if (!storageComponent.canAcceptItem(event.cursor)) {
                        event.isCancelled = true
                        return
                    }
                }

                // 检查是否可以取出物品
                event.currentItem != null && event.currentItem!!.type != Material.AIR -> {
                    if (!storageComponent.canTakeItem(event.currentItem)) {
                        event.isCancelled = true
                        return
                    }
                }
            }
        }
    }

    /**
     * 应用拖拽保护机制
     */
    private fun applyDragProtection(event: InventoryDragEvent, page: BasePage) {
        // 检查拖拽是否涉及GUI inventory
        val affectsGuiInventory = event.inventorySlots.any { slot ->
            event.inventory == page.inventory && slot < page.inventory.size
        }
        if (!affectsGuiInventory) return

        // 检查所有涉及的槽位是否都是存储组件
        val allSlotsAreStorage = event.inventorySlots.all { slot ->
            if (event.inventory != page.inventory) return@all true

            val component = page.getComponentBySlot(slot)
            val isStorageComponent = component != null && isStorageComponent(component)

            if (isStorageComponent) {
                val storageComponent = component as IStorageComponent
                storageComponent.canAcceptItem(event.oldCursor)
            } else {
                false
            }
        }

        // 如果不是所有槽位都允许，则取消拖拽
        if (!allSlotsAreStorage) {
            event.isCancelled = true
        }
    }

    /**
     * 应用丢弃保护机制
     */
    private fun applyDropProtection(event: PlayerDropItemEvent, page: BasePage) {
        // 如果玩家正在查看GUI，检查丢弃的物品是否来自GUI
        // 这里我们采用保守策略：如果玩家正在使用GUI，禁止丢弃物品
        // 除非物品明确来自玩家背包

        // 注意：这个检查可能需要根据具体需求调整
        // 目前的实现比较保守，可能会影响正常的物品丢弃

        // TODO: 实现更精确的物品来源检查
        // 暂时不取消事件，让具体的组件决定是否处理
    }
}