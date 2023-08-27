package city.newnan.deathcost

import city.newnan.deathcost.config.ConfigFile
import city.newnan.deathcost.config.CostStage
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.message.MessageManager
import me.lucko.helper.Events
import me.lucko.helper.plugin.ExtendedJavaPlugin
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent

class PluginMain : ExtendedJavaPlugin() {
    private var costStages: List<CostStage> = emptyList()
    private val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.None)
        }
    }
    private val messageManager: MessageManager by lazy { MessageManager(this) }
    private lateinit var economy: Economy
    private var targetAccount: OfflinePlayer? = null
    private var sendDeathMessageToPlayer: Boolean = false
    private var sendDeathMessageToBroadcast: Boolean = false
    private var sendDeathMessageToConsole: Boolean = false

    override fun enable() {
        if (server.pluginManager.getPlugin("Vault") == null) {
            throw Exception("Vault not found!")
        }
        economy = server.servicesManager.getRegistration(Economy::class.java)?.provider
            ?: throw Exception("Vault economy service not found!")

        reload()
        messageManager setPlayerPrefix "§7[§6牛腩小镇§7] §f"
        Events.subscribe(PlayerDeathEvent::class.java, EventPriority.MONITOR)
            .handler {
                val player = it.entity
                val cost = fineMoney(player)
                if (sendDeathMessageToPlayer) {
                    messageManager.printf(player, "&c你重生了, 消耗 {0,number,#.##} ₦, 使用/back可快速回到死亡地点.", cost)
                }
                if (sendDeathMessageToBroadcast) {
                    server.broadcastMessage(messageManager.sprintf(
                        "&7玩家 {0} 死亡, 扣除 {1,number,#.##} ₦.", player.displayName, cost))
                }
                if (sendDeathMessageToConsole) {
                    messageManager.printf("玩家 {0} 死亡, 扣除 {1,number,#.##} ₦.", player.displayName, cost)
                }
            }
            .bindWith(this)
    }

    private fun reload() {
        configManager.cache?.clear()
        configManager touch "config.yml"
        configManager.parse<ConfigFile>("config.yml").also {
            if (it.deathCost.useSimpleMode) {
                // 简单扣费模式
                if (it.deathCost.simpleMode == null) {
                    messageManager.warn("简单扣费模式启用, 但是没有设置扣费模式, 将不会有任何扣费.")
                    costStages = emptyList()
                } else {
                    costStages = listOf(CostStage(
                        -1.0,
                        it.deathCost.simpleMode.cost,
                        it.deathCost.simpleMode.ifPercent
                    ))
                }
            } else {
                if (it.deathCost.complexMode == null) {
                    messageManager.warn("复杂扣费模式启用, 但是没有设置扣费模式, 将不会有任何扣费.")
                    costStages = emptyList()
                } else {
                    costStages = it.deathCost.complexMode
                }
            }
            targetAccount = it.deathCost.targetAccount?.let { name ->
                if (name.isBlank()) return@let null
                Bukkit.getOfflinePlayers().find { p -> p.name == name }?.also { p ->
                    messageManager.info("设置扣费转账账户为: ${p.name}")
                }
            }
            sendDeathMessageToPlayer = it.deathMessage.playerEnable
            sendDeathMessageToBroadcast = it.deathMessage.broadcastEnable
            sendDeathMessageToConsole = it.deathMessage.consoleEnable
        }
    }

    /**
     * 死亡罚款
     * @param player 死亡的玩家
     * @return 扣的款数(调用方法就已经扣款，不用再扣一次)
     */
    private fun fineMoney(player: Player): Double {
        // 如果玩家有死亡不扣钱权限
        if (player.hasPermission("deathcost.bypass")) {
            return 0.0
        }

        // 获得玩家的现金
        val bal: Double = economy.getBalance(player)
        var cost = 0.0
        var preMax = 0.0
        // 遍历扣费阶梯
        for (stage in costStages) {
            // 获取阶梯上限
            val max = stage.max
            if (bal <= preMax) {
                break
            }
            // 获取数值
            var value = stage.cost
            if (stage.ifPercent) {
                value *= bal - preMax
            }
            preMax = if (max == -1.0) 0.0 else max
            cost += value
        }

        // 扣钱
        if (cost > 0) {
            economy.withdrawPlayer(player, cost)
            targetAccount?.let { economy.depositPlayer(it, cost) }
        }
        return cost
    }
}
