package city.newnan.mcpatch.modules

import city.newnan.core.base.BaseModule
import city.newnan.core.scheduler.runSync
import city.newnan.mcpatch.MCPatchPlugin
import city.newnan.mcpatch.i18n.LanguageKeys
import city.newnan.mcpatch.security.SecurityEvent
import city.newnan.mcpatch.security.SecurityEventType
import city.newnan.mcpatch.security.SecuritySeverity
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

/**
 * 反世界下载器模块
 *
 * 检测并阻止玩家使用 World Downloader 模组下载服务器地图。
 * 通过监听插件消息通道来检测 WDL 的使用。
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class AntiWorldDownloadModule(
    moduleName: String,
    val plugin: MCPatchPlugin
) : BaseModule(moduleName, plugin), PluginMessageListener {

    companion object {
        private const val WDL_INIT_CHANNEL = "WDL|INIT"
        private const val WDL_REQUEST_CHANNEL = "WDL|REQUEST"
    }

    // 配置缓存
    private var enabled: Boolean = true
    private var logViolations: Boolean = true

    init {
        init()
    }

    override fun onInit() {
        // 注册插件消息通道
        registerPluginChannels()

        logger.info(LanguageKeys.Log.Info.MODULE_INITIALIZED, moduleName)
    }

    override fun onReload() {
        // 重新加载配置
        val config = plugin.getPluginConfig().modules.antiWorldDownload
        enabled = config.enabled
        logViolations = config.logViolations
    }

    /**
     * 注册插件消息通道
     */
    private fun registerPluginChannels() {
        try {
            plugin.server.messenger.registerIncomingPluginChannel(plugin, WDL_INIT_CHANNEL, this)
            plugin.server.messenger.registerIncomingPluginChannel(plugin, WDL_REQUEST_CHANNEL, this)
        } catch (e: Exception) {
            logger.error("Failed to register plugin message channels", e)
        }
    }

    /**
     * 处理插件消息
     */
    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (!enabled) return

        try {
            when (channel) {
                WDL_INIT_CHANNEL -> handleWDLInit(player, message)
                WDL_REQUEST_CHANNEL -> handleWDLRequest(player, message)
            }
        } catch (e: Exception) {
            logger.error("Error processing plugin message from ${player.name}", e)
        }
    }

    /**
     * 处理 WDL 初始化消息
     */
    private fun handleWDLInit(player: Player, message: ByteArray) {
        logger.warn("Detected WDL initialization from player: ${player.name}")

        // 记录安全事件
        if (logViolations) {
            val securityEvent = SecurityEvent(
                type = SecurityEventType.WORLD_DOWNLOADER_DETECTED,
                severity = SecuritySeverity.HIGH,
                player = player,
                location = player.location,
                message = "Player attempted to initialize World Downloader",
                details = mapOf(
                    "channel" to WDL_INIT_CHANNEL,
                    "messageSize" to message.size
                ),
                moduleName = moduleName
            )
            plugin.securityLoggerModule.logSecurityEvent(securityEvent)
        }

        // 踢出玩家
        kickPlayer(player)
    }

    /**
     * 处理 WDL 请求消息
     */
    private fun handleWDLRequest(player: Player, message: ByteArray) {
        logger.warn("Detected WDL request from player: ${player.name}")

        try {
            // 尝试解析消息内容（WDL 请求通常包含区块坐标等信息）
            val messageContent = String(message, Charsets.UTF_8)
            logger.debug("WDL request content: $messageContent")

            // 记录安全事件
            if (logViolations) {
                val securityEvent = SecurityEvent(
                    type = SecurityEventType.WORLD_DOWNLOADER_DETECTED,
                    severity = SecuritySeverity.CRITICAL,
                    player = player,
                    location = player.location,
                    message = "Player attempted to download world data",
                    details = mapOf(
                        "channel" to WDL_REQUEST_CHANNEL,
                        "messageContent" to messageContent,
                        "messageSize" to message.size
                    ),
                    moduleName = moduleName
                )
                plugin.securityLoggerModule.logSecurityEvent(securityEvent)
            }

            // 踢出玩家
            kickPlayer(player)

        } catch (e: Exception) {
            logger.error("Error parsing WDL request message", e)
            // 即使解析失败，也要踢出玩家
            kickPlayer(player)
        }
    }

    /**
     * 踢出玩家
     */
    private fun kickPlayer(player: Player) {
        try {
            runSync {
                try {
                    player.kick(
                        plugin.messager.sprintf(LanguageKeys.Security.AntiWorldDownload.PLAYER_KICKED),
                        org.bukkit.event.player.PlayerKickEvent.Cause.PLUGIN
                    )

                    // 通知管理员
                    plugin.server.onlinePlayers
                        .filter { it.hasPermission("mc-patch.admin") }
                        .forEach { admin ->
                            plugin.messager.printf(admin, LanguageKeys.Security.AntiWorldDownload.VIOLATION_LOGGED, player.name)
                        }

                } catch (e: Exception) {
                    logger.error("Failed to kick player ${player.name}", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Error in kickPlayer", e)
        }
    }

    override fun onClose() {
        try {
            // 注销插件消息通道
            plugin.server.messenger.unregisterIncomingPluginChannel(plugin, WDL_INIT_CHANNEL, this)
            plugin.server.messenger.unregisterIncomingPluginChannel(plugin, WDL_REQUEST_CHANNEL, this)
            logger.debug("Unregistered WDL plugin message channels")
        } catch (e: Exception) {
            logger.error("Error during module cleanup", e)
        }
    }
}
