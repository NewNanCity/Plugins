package city.newnan.guardian.modules

import city.newnan.core.base.BaseModule
import city.newnan.core.scheduler.runAsyncRepeating
import city.newnan.core.utils.text.toMiniMessage
import city.newnan.guardian.GuardianPlugin
import city.newnan.guardian.sdk.models.HeartbeatRequest
import city.newnan.guardian.utils.getProtocol
import org.bukkit.Bukkit

internal fun normalizeTps(value: Double): Double? {
    if (!value.isFinite()) return null
    return value.coerceIn(0.0, 20.0)
}

class HeartbeatManager(val plugin: GuardianPlugin) : BaseModule("HeartbeatManager", plugin) {
    private var heartbeatActive = false
    private var serverId = -1
    private var enableHeartbeat = true
    private val version = "${Bukkit.getName()} ${Bukkit.getBukkitVersion().split('-', limit=2)[0]} (${getProtocol()})"

    init { init() }

    override fun onInit() {
    }

    override fun onReload() {
        serverId = plugin.getPluginConfig().serverId
        enableHeartbeat = plugin.getPluginConfig().serverHeartbeat

        // 启动心跳任务
        startHeartbeatTask()
    }

    private fun startHeartbeatTask() {
        if (heartbeatActive) return
        var lastRttMs = -1L
        var nextHeartbeatTime = 0L
        var retryDelayMs = 1_000L  // 初始延迟1秒
        val maxRetryDelayMs = 60_000L  // 最大延迟1分钟

        fun sendHeartbeat() {
            val requestTime = System.currentTimeMillis()
            val resp = plugin.nnmClient.monitor.heartbeat(
                serverId,
                HeartbeatRequest(
                    currentPlayers = plugin.server.onlinePlayers.size,
                    maxPlayers = plugin.server.maxPlayers,
                    tps = normalizeTps(plugin.server.tps.firstOrNull() ?: Double.NaN),
                    version = version,
                    motd = plugin.server.motd().toMiniMessage(),
                    rttMs = if (lastRttMs >= 0) lastRttMs else null
                )
            )
            val responseTime = System.currentTimeMillis()
            lastRttMs = (responseTime - requestTime) - (resp.responseAt - resp.receivedAt)
            nextHeartbeatTime = requestTime + resp.expireDurationMs - 10_000L
            // 成功后重置延迟
            retryDelayMs = 1_000L
        }

        heartbeatActive = true
        runAsyncRepeating(0L, 20L) { task ->
            if (serverId <= 0 || !enableHeartbeat) {
                task.cancel()
                heartbeatActive = false
                return@runAsyncRepeating
            }
            if (System.currentTimeMillis() < nextHeartbeatTime) {
                return@runAsyncRepeating
            }
            try {
                sendHeartbeat()
            } catch (e: Exception) {
                plugin.logger.error("Failed to send heartbeat", e)
                // 指数回退重试，最长延迟1分钟
                nextHeartbeatTime = System.currentTimeMillis() + retryDelayMs
                retryDelayMs = (retryDelayMs * 2).coerceAtMost(maxRetryDelayMs)
            }
        }
    }
}
