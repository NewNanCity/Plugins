package city.newnan.mcpatch.modules

import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.mcpatch.MCPatchPlugin
import city.newnan.mcpatch.i18n.LanguageKeys
import city.newnan.mcpatch.security.SecurityEvent
import city.newnan.mcpatch.security.SecurityEventType
import city.newnan.mcpatch.security.SecuritySeverity
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockPlaceEvent

const val MIN_Y = -63
const val MAX_Y = 319

/**
 * 反崩服保护模块
 *
 * 防止各种可能导致服务器崩溃的操作，包括：
 * - 发射器边界漏洞保护
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class AntiCrashModule(
    moduleName: String,
    val plugin: MCPatchPlugin
) : BaseModule(moduleName, plugin) {

    // 配置缓存
    private var dispenserProtection: Boolean = true

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
        val config = plugin.getPluginConfig().modules.antiCrash
        dispenserProtection = config.dispenserProtection
    }

    /**
     * 注册事件处理器
     */
    private fun registerEventHandlers() {
        // 监听方块放置事件，防止发射器边界漏洞
        subscribeEvent<BlockPlaceEvent> {
            priority(EventPriority.LOWEST)
            filter { !it.isCancelled }
            filter { it.block.type == Material.DISPENSER }
            handler { event ->
                if (!dispenserProtection) return@handler
                
                try {
                    val player = event.player
                    
                    // 检查玩家是否有绕过权限
                    if (player.hasPermission("mc-patch.bypass.anti-crash")) {
                        return@handler
                    }
                    
                    // 检查是否在危险位置放置发射器
                    if (isDangerousDispenserLocation(event)) {
                        handleDangerousDispenserPlacement(event)
                    }
                } catch (e: Exception) {
                    logger.error("Error in BlockPlaceEvent handler", e)
                }
            }
        }
    }

    /**
     * 检查是否在危险位置放置发射器
     */
    private fun isDangerousDispenserLocation(event: BlockPlaceEvent): Boolean {
        val location = event.block.location
        val y = location.blockY
        
        // 检查是否在世界边界附近
        return y <= MIN_Y || y >= MAX_Y
    }

    /**
     * 处理危险的发射器放置
     */
    private fun handleDangerousDispenserPlacement(event: BlockPlaceEvent) {
        try {
            val player = event.player
            val location = event.block.location
            
            logger.warn("Blocked dangerous dispenser placement by ${player.name} at Y=${location.blockY}")
            
            // 取消事件
            event.isCancelled = true
            
            // 记录安全事件
            val securityEvent = SecurityEvent(
                type = SecurityEventType.CRASH_ATTEMPT_BLOCKED,
                severity = SecuritySeverity.HIGH,
                player = player,
                location = location,
                message = "Blocked dangerous dispenser placement at world boundary",
                details = mapOf(
                    "blockType" to Material.DISPENSER.name,
                    "yLevel" to location.blockY,
                    "reason" to "dispenser_boundary_exploit"
                ),
                moduleName = moduleName
            )
            plugin.securityLoggerModule.logSecurityEvent(securityEvent)
            
            // 通知玩家
            val message = plugin.messager.sprintf(LanguageKeys.Security.AntiCrash.DISPENSER_BLOCKED)
            player.sendMessage(message)
            
            // 通知管理员
            notifyAdministrators(player, "dangerous dispenser placement", location)
            
        } catch (e: Exception) {
            logger.error("Error handling dangerous dispenser placement", e)
        }
    }

    /**
     * 通知管理员
     */
    private fun notifyAdministrators(player: Player, action: String, location: org.bukkit.Location) {
        try {
            val adminMessage = "§c[MCPatch] §f玩家 ${player.name} 尝试进行危险操作: $action " +
                    "在位置 (${location.blockX}, ${location.blockY}, ${location.blockZ})"
            
            plugin.server.onlinePlayers
                .filter { it.hasPermission("mc-patch.admin") }
                .forEach { admin ->
                    admin.sendMessage(adminMessage)
                }
        } catch (e: Exception) {
            logger.error("Error notifying administrators", e)
        }
    }
}
