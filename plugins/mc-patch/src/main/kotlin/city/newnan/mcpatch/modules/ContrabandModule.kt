package city.newnan.mcpatch.modules

import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runSync
import city.newnan.mcpatch.MCPatchPlugin
import city.newnan.mcpatch.i18n.LanguageKeys
import city.newnan.mcpatch.security.SecurityEvent
import city.newnan.mcpatch.security.SecurityEventType
import city.newnan.mcpatch.security.SecuritySeverity
import org.bukkit.Material
import org.bukkit.block.BlockState
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * 违禁物品检测模块
 *
 * 检测和清理玩家容器中的违禁物品，包括：
 * - 系统方块（基岩、屏障等）
 * - 危险物品（末影水晶等）
 * - 流体方块（水、岩浆等）
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class ContrabandModule(
    moduleName: String,
    val plugin: MCPatchPlugin
) : BaseModule(moduleName, plugin) {

    // 配置缓存
    private var enabled: Boolean = true
    private val containerMaterials: Set<Material> = hashSetOf(
        // 潜影盒
        Material.SHULKER_BOX,
        Material.WHITE_SHULKER_BOX,
        Material.ORANGE_SHULKER_BOX,
        Material.MAGENTA_SHULKER_BOX,
        Material.LIGHT_BLUE_SHULKER_BOX,
        Material.YELLOW_SHULKER_BOX,
        Material.LIME_SHULKER_BOX,
        Material.PINK_SHULKER_BOX,
        Material.GRAY_SHULKER_BOX,
        Material.LIGHT_GRAY_SHULKER_BOX,
        Material.CYAN_SHULKER_BOX,
        Material.PURPLE_SHULKER_BOX,
        Material.BLUE_SHULKER_BOX,
        Material.BROWN_SHULKER_BOX,
        Material.GREEN_SHULKER_BOX,
        Material.RED_SHULKER_BOX,
        Material.BLACK_SHULKER_BOX,
        // 收纳袋
        Material.BUNDLE
    )
    private var blockedMaterials: Set<Material> = emptySet()
    private var autoRemove: Boolean = true

    init {
        init()
    }

    override fun onInit() {
        // 注册事件监听器
        registerEventHandlers()

        logger.info(LanguageKeys.Log.Info.MODULE_INITIALIZED, moduleName)
    }

    override fun onReload() {
        // 重新加载配置
        val config = plugin.getPluginConfig().modules.contraband
        enabled = config.enabled
        blockedMaterials = config.blockedMaterials
        autoRemove = config.autoRemove
    }

    /**
     * 注册事件处理器
     */
    private fun registerEventHandlers() {
        // 监听玩家加入事件，检查背包
        subscribeEvent<PlayerJoinEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.player.hasPermission("mc-patch.bypass.contraband") }
            handler { event ->
                if (!enabled) return@handler

                try {
                    // 异步检查玩家背包
                    runAsync {
                        checkPlayerInventory(event.player)
                    }
                } catch (e: Exception) {
                    logger.error("Error checking player inventory on join", e)
                }
            }
        }

        // 监听玩家拾取事件，检查拾取物
        subscribeEvent<EntityPickupItemEvent> {
            priority(EventPriority.LOWEST)
            filter { !it.isCancelled }
            filter { it.entity is Player }
            filter { !(it.entity as Player).hasPermission("mc-patch.bypass.contraband") }
            handler { event ->
                if (!enabled) return@handler

                try {
                    if (blockedMaterials.contains(event.item.itemStack.type)) {
                        event.item.remove()
                        event.isCancelled = true
                        // 记录安全事件
                        val securityEvent = SecurityEvent(
                            type = SecurityEventType.CONTRABAND_FOUND,
                            severity = SecuritySeverity.MEDIUM,
                            player = event.entity as Player,
                            location = event.entity.location,
                            message = "Player picked up contraband item",
                            details = mapOf(
                                "itemType" to event.item.itemStack.type.name,
                                "itemAmount" to event.item.itemStack.amount
                            ),
                            moduleName = moduleName
                        )
                        plugin.securityLoggerModule.logSecurityEvent(securityEvent)
                    }
                } catch (e: Exception) {
                    logger.error("Error checking item pickup", e)
                }
            }
        }

        // 监听玩家丢东西事件，检查丢弃物
        subscribeEvent<PlayerDropItemEvent> {
            priority(EventPriority.LOWEST)
            filter { !it.isCancelled }
            filter { !it.player.hasPermission("mc-patch.bypass.contraband") }
            handler { event ->
                if (!enabled) return@handler

                try {
                    if (blockedMaterials.contains(event.itemDrop.itemStack.type)) {
                        event.itemDrop.remove()
                        event.isCancelled = true
                        // 记录安全事件
                        val securityEvent = SecurityEvent(
                            type = SecurityEventType.CONTRABAND_FOUND,
                            severity = SecuritySeverity.MEDIUM,
                            player = event.player,
                            location = event.player.location,
                            message = "Player dropped contraband item",
                            details = mapOf(
                                "itemType" to event.itemDrop.itemStack.type.name,
                                "itemAmount" to event.itemDrop.itemStack.amount
                            ),
                            moduleName = moduleName
                        )
                        plugin.securityLoggerModule.logSecurityEvent(securityEvent)
                    }
                } catch (e: Exception) {
                    logger.error("Error checking item drop", e)
                }
            }
        }

        // 监听容器打开事件，检查容器内容
        subscribeEvent<InventoryOpenEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            filter { when (it.inventory.holder) { // 只看真实存在的容器
                is BlockState -> true
                is Entity -> true
                else -> false
            } }
            handler { event ->
                if (!enabled) return@handler

                try {
                    val player = event.player as? Player ?: return@handler

                    // 检查玩家是否有绕过权限
                    if (player.hasPermission("mc-patch.bypass.contraband")) {
                        return@handler
                    }

                    // 异步检查容器内容
                    runAsync {
                        checkInventory(event.inventory, player)
                    }
                } catch (e: Exception) {
                    logger.error("Error checking inventory on open", e)
                }
            }
        }
    }

    /**
     * 检查玩家背包
     */
    private fun checkPlayerInventory(player: Player) {
        try {
            // 检查背包
            val inventory = player.inventory
            val contrabandItems1 = findContrabandItems(inventory)
            if (contrabandItems1.isNotEmpty()) {
                handleContrabandFound(player, inventory, contrabandItems1, "player inventory")
            }

            // 检查末影箱
            val enderChest = player.enderChest
            val contrabandItems2 = findContrabandItems(enderChest)
            if (contrabandItems2.isNotEmpty()) {
                handleContrabandFound(player, enderChest, contrabandItems2, "ender chest")
            }
        } catch (e: Exception) {
            logger.error("Error checking player inventory for ${player.name}", e)
        }
    }

    /**
     * 检查容器
     */
    private fun checkInventory(inventory: Inventory, player: Player) {
        try {
            val contrabandItems = findContrabandItems(inventory)

            if (contrabandItems.isNotEmpty()) {
                val containerType = inventory.type.name.lowercase().replace("_", " ")
                handleContrabandFound(player, inventory, contrabandItems, containerType)
            }
        } catch (e: Exception) {
            logger.error("Error checking inventory for ${player.name}", e)
        }
    }

    /**
     * 查找违禁物品
     */
    private fun findContrabandItems(inventory: Inventory): List<Pair<Int, ItemStack>> {
        val contrabandItems = mutableListOf<Pair<Int, ItemStack>>()

        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i)
            if (item != null && item.type != Material.AIR) {
                if (blockedMaterials.contains(item.type)) {
                    contrabandItems.add(i to item)
                }
                // 本来还要判断 containerMaterial，但是因为里面的东西，玩家是不能直接使用的，必须打开对应的界面，所以不用检测
            }
        }

        return contrabandItems
    }

    /**
     * 处理发现的违禁物品
     */
    private fun handleContrabandFound(
        player: Player,
        inventory: Inventory,
        contrabandItems: List<Pair<Int, ItemStack>>,
        containerType: String
    ) {
        try {
            logger.warn("Found ${contrabandItems.size} contraband items in ${player.name}'s $containerType")

            // 记录安全事件
            val securityEvent = SecurityEvent(
                type = SecurityEventType.CONTRABAND_FOUND,
                severity = SecuritySeverity.MEDIUM,
                player = player,
                location = player.location,
                message = "Found ${contrabandItems.size} contraband items in $containerType",
                details = mapOf(
                    "containerType" to containerType,
                    "itemCount" to contrabandItems.size,
                    "items" to contrabandItems.map { "${it.second.type}x${it.second.amount}" }
                ),
                moduleName = moduleName
            )
            plugin.securityLoggerModule.logSecurityEvent(securityEvent)

            // 自动移除违禁物品
            if (autoRemove) {
                runSync {
                    removeContrabandItems(inventory, contrabandItems)

                    // 通知玩家
                    val message = plugin.messager.sprintf(
                        LanguageKeys.Security.Contraband.ITEM_REMOVED,
                        player.name,
                        contrabandItems.size
                    )
                    player.sendMessage(message)
                }
            }

            // 通知管理员
            notifyAdministrators(player, contrabandItems, containerType)

        } catch (e: Exception) {
            logger.error("Error handling contraband items", e)
        }
    }

    /**
     * 移除违禁物品
     */
    private fun removeContrabandItems(inventory: Inventory, contrabandItems: List<Pair<Int, ItemStack>>) {
        try {
            for ((slot, item) in contrabandItems) {
                inventory.setItem(slot, null)
                logger.debug("Removed contraband item: ${item.type}x${item.amount} from slot $slot")
            }
        } catch (e: Exception) {
            logger.error("Error removing contraband items", e)
        }
    }

    /**
     * 通知管理员
     */
    private fun notifyAdministrators(
        player: Player,
        contrabandItems: List<Pair<Int, ItemStack>>,
        containerType: String
    ) {
        try {
            val itemList = contrabandItems.joinToString(", ") { "${it.second.type}x${it.second.amount}" }

            plugin.server.onlinePlayers
                .filter { it.hasPermission("mc-patch.admin") }
                .forEach { admin ->
                    plugin.messager.printf(
                        admin,
                        LanguageKeys.Security.Contraband.ITEMS_FOUND,
                        player.name,
                        contrabandItems.size
                    )
                }
        } catch (e: Exception) {
            logger.error("Error notifying administrators", e)
        }
    }
}
