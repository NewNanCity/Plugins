package city.newnan.deathcost.modules

import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.deathcost.DeathCostPlugin

import city.newnan.deathcost.config.CostStage
import city.newnan.deathcost.config.DeathCostConfig
import city.newnan.deathcost.i18n.LanguageKeys
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent
import java.util.UUID
import kotlin.math.round

/**
 * 死亡扣费模块
 *
 * 负责处理玩家死亡时的扣费逻辑，支持：
 * - 简单模式和复杂阶梯扣费
 * - 权限检查和免费死亡
 * - 转账到指定账户
 * - 死亡消息发送
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class DeathCostModule(
    moduleName: String,
    val plugin: DeathCostPlugin
) : BaseModule(moduleName, plugin) {

    private var costStages: List<CostStage> = emptyList()
    private var targetAccount: OfflinePlayer? = null
    private var sendDeathMessageToPlayer: Boolean = false
    private var sendDeathMessageToBroadcast: Boolean = false
    private var sendDeathMessageToConsole: Boolean = false

    init { init() }

    override fun onInit() {
        // 注册事件监听器（自动绑定到模块）
        subscribeEvent<PlayerDeathEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            handler { event ->
                val player = event.entity
                val cost = calculateAndDeductCost(player)

                // 发送死亡消息
                sendDeathMessages(player, cost)
            }
        }

        logger.debug(LanguageKeys.Business.Module.INITIALIZED)
    }

    override fun onReload() {
        try {
            loadConfig(plugin.getPluginConfig())
        } catch (e: Exception) {
            logger.error(LanguageKeys.Business.Module.CONFIG_LOAD_FAILED, e)
            throw e
        }
        logger.debug(LanguageKeys.Business.Module.RELOADED)
    }

    /**
     * 加载配置数据
     */
    private fun loadConfig(config: DeathCostConfig) {
        val deathCostSettings = config.deathCost

        // 加载扣费阶梯
        costStages = if (deathCostSettings.useSimpleMode) {
            // 简单扣费模式
            val simpleMode = deathCostSettings.simpleMode
            if (simpleMode == null) {
                logger.warn(LanguageKeys.Business.Warning.SIMPLE_MODE_NO_CONFIG)
                emptyList()
            } else {
                listOf(CostStage(-1.0, simpleMode.cost, simpleMode.ifPercent))
            }
        } else {
            // 复杂扣费模式
            val complexMode = deathCostSettings.complexMode
            if (complexMode.isNullOrEmpty()) {
                logger.warn(LanguageKeys.Business.Warning.COMPLEX_MODE_NO_CONFIG)
                emptyList()
            } else {
                complexMode
            }
        }

        // 加载目标账户
        targetAccount = deathCostSettings.targetAccount?.let { name ->
            if (name.isBlank()) return@let null
            try {
                val uuid = UUID.fromString(name)
                val account = Bukkit.getOfflinePlayer(uuid)
                logger.info(LanguageKeys.Core.Config.TARGET_ACCOUNT_SET, "[UUID: $name]")
                account
            } catch (e: IllegalArgumentException) {
                val account = Bukkit.getOfflinePlayer(name)
                if (!account.hasPlayedBefore()) throw Exception("Player $name not found")
                logger.info(LanguageKeys.Core.Config.TARGET_ACCOUNT_SET, name)
                account
            }
        }

        // 加载消息设置
        val messageSettings = config.deathMessage
        sendDeathMessageToPlayer = messageSettings.playerEnable
        sendDeathMessageToBroadcast = messageSettings.broadcastEnable
        sendDeathMessageToConsole = messageSettings.consoleEnable
    }

    /**
     * 计算并扣除死亡费用
     *
     * @param player 死亡的玩家
     * @return 扣除的金额
     */
    private fun calculateAndDeductCost(player: Player): Double {
        // 如果玩家有死亡不扣钱权限
        if (player.hasPermission("deathcost.bypass")) {
            return 0.0
        }

        // 获得玩家的现金
        val balance: Double = plugin.economy.getBalance(player)
        var cost = 0.0
        var preMax = 0.0

        // 遍历扣费阶梯
        for (stage in costStages) {
            // 获取阶梯上限
            val max = stage.max
            if (balance <= preMax) {
                break
            }

            // 获取数值
            var value = stage.cost
            if (stage.ifPercent) {
                value *= (balance - preMax)
            }

            preMax = if (max == -1.0) balance else max
            cost += value
        }

        // 四舍五入保留2位小数
        cost = round(cost * 100) / 100.0

        // 扣钱
        if (cost > 0) {
            plugin.economy.withdrawPlayer(player, cost)
            targetAccount?.let { plugin.economy.depositPlayer(it, cost) }
        }

        return cost
    }

    /**
     * 发送死亡消息
     */
    private fun sendDeathMessages(player: Player, cost: Double) {
        if (cost <= 0 && player.hasPermission("deathcost.bypass")) {
            if (sendDeathMessageToPlayer) {
                plugin.messager.printf(player, LanguageKeys.Business.Death.NO_COST)
            }
            return
        }

        if (sendDeathMessageToPlayer) {
            plugin.messager.printf(player, LanguageKeys.Business.Death.COST_DEDUCTED, cost)
        }

        if (sendDeathMessageToBroadcast) {
            // 使用现代化的广播方式
            val message = plugin.messager.sprintf(LanguageKeys.Business.Death.BROADCAST_MESSAGE, player.name, cost)
            plugin.server.onlinePlayers.forEach { onlinePlayer ->
                onlinePlayer.sendMessage(message)
            }
        }

        if (sendDeathMessageToConsole) {
            plugin.messager.printf(LanguageKeys.Business.Death.CONSOLE_MESSAGE, player.name, cost)
        }
    }
}
