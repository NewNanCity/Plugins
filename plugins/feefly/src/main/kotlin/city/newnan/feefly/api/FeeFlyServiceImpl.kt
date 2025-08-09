package city.newnan.feefly.api

import city.newnan.feefly.FeeFlyPlugin
import city.newnan.feefly.config.FeeFlyConfig
import city.newnan.feefly.config.FlyingPlayer
import org.bukkit.entity.Player
import kotlin.math.roundToInt

/**
 * FeeFly服务实现类
 *
 * 实现FeeFlyService接口，提供具体的API功能
 *
 * @param plugin FeeFly插件实例
 * @author NewNanCity
 * @since 2.0.0
 */
class FeeFlyServiceImpl(private val plugin: FeeFlyPlugin) : FeeFlyService {

    override fun startFly(player: Player): Boolean {
        val flyManager = plugin.flyManager

        // 检查是否已经在飞行
        if (isFlying(player)) {
            return false
        }

        try {
            // 调用内部方法开启飞行
            flyManager.toggleFly(player)
            // 检查是否成功开启
            return isFlying(player)
        } catch (e: Exception) {
            plugin.logger.warn("Failed to start fly for player ${player.name}", e)
            return false
        }
    }

    override fun cancelFly(player: Player): Boolean {
        val flyManager = plugin.flyManager
        return flyManager.cancelFly(player, true)
    }

    override fun toggleFly(player: Player): Boolean {
        val flyManager = plugin.flyManager
        flyManager.toggleFly(player)
        return isFlying(player)
    }

    override fun isFlying(player: Player): Boolean {
        val flyManager = plugin.flyManager
        return flyManager.getFlyingPlayers().containsKey(player)
    }

    override fun getFlyingInfo(player: Player): FlyingPlayer? {
        val flyManager = plugin.flyManager
        return flyManager.getFlyingPlayers()[player]
    }

    override fun getFlyingPlayers(): Map<Player, FlyingPlayer> {
        val flyManager = plugin.flyManager
        return flyManager.getFlyingPlayers().toMap()
    }

    override fun getRemainingTime(player: Player): Int? {
        if (!isFlying(player)) {
            return null
        }

        // 如果有免费权限，返回null表示无限制
        if (player.hasPermission("feefly.free")) {
            return null
        }

        val economy = plugin.economy
        val balance = economy.getBalance(player)

        if (balance <= 0.0) {
            return 0
        }

        // 计算剩余时间
        val config = plugin.getPluginConfig()
        val costPerSecond = (20.0 / config.flying.tickPerCount) * config.flying.costPerCount

        return (balance / costPerSecond).roundToInt()
    }

    override fun getFlyDuration(player: Player): Long? {
        val flyingInfo = getFlyingInfo(player) ?: return null
        return flyingInfo.getFlyDuration()
    }

    override fun getTotalCost(player: Player): Double? {
        val flyingInfo = getFlyingInfo(player) ?: return null

        // 如果有免费权限，返回0
        if (player.hasPermission("feefly.free")) {
            return 0.0
        }

        val config = plugin.getPluginConfig()
        val costPerSecond = (20.0 / config.flying.tickPerCount) * config.flying.costPerCount
        val durationSeconds = flyingInfo.getFlyDuration() / 1000.0

        return durationSeconds * costPerSecond
    }

    override fun hasFreePermission(player: Player): Boolean {
        return player.hasPermission("feefly.free")
    }

    override fun getFlyConfig(): Map<String, Any> {
        val config = plugin.getPluginConfig()
        val flyingConfig = config.flying

        return mapOf(
            "flySpeed" to flyingConfig.flySpeed,
            "costPerCount" to flyingConfig.costPerCount,
            "tickPerCount" to flyingConfig.tickPerCount,
            "costPerSecond" to ((20.0 / flyingConfig.tickPerCount) * flyingConfig.costPerCount),
            "lowBalanceWarningSeconds" to flyingConfig.lowBalanceWarningSeconds,
            "commandCooldownSeconds" to flyingConfig.commandCooldownSeconds
        )
    }

    override fun isAvailable(): Boolean {
        return plugin.isEnabled
    }
}
