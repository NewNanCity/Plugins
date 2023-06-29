package city.newnan.deathcost

import city.newnan.violet.config.ConfigManager
import city.newnan.violet.message.MessageManager
import me.lucko.helper.Events
import me.lucko.helper.plugin.ExtendedJavaPlugin
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent

class PluginMain : ExtendedJavaPlugin() {

    private var costStages: List<CostStage> = listOf()
    private val configManager: ConfigManager by lazy { ConfigManager(this) }
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
        messageManager setPlayerPrefix "[牛腩小镇]"
        Events.subscribe(PlayerDeathEvent::class.java)
            .handler {
                val player = it.entity
                val cost = fineMoney(player)
                if (sendDeathMessageToPlayer) {
                    messageManager.printf(player, "&c你重生了, 消耗 {1,number,#.##} ₦, 使用/back可快速回到死亡地点.",
                        cost)
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
        configManager touch "config.yml"
        configManager["config.yml"]!!.let { rootNode ->
            rootNode.getNode("cash-cost").let { costNode ->
                if (costNode.getNode("use-simple-mode").getBoolean(true)) {
                    // 简单扣费模式
                    costStages = listOf(CostStage(
                        -1.0,
                        costNode.getNode("simple-mode", "cost").double,
                        costNode.getNode("simple-mode", "if-percent").boolean
                    ))
                } else {
                    // 复杂扣费模式
                    costStages = costNode.getNode("complex-mode").childrenList.map {
                        CostStage(
                            it.getNode("max").double,
                            it.getNode("cost").double,
                            it.getNode("if-percent").boolean
                        )

                    }
                }
                targetAccount = null
                costNode.getNode("target-account").string?.also {
                    Bukkit.getOfflinePlayers().forEach { player ->
                        if (player.name == it) {
                            targetAccount = player
                            return@also
                        }
                    }
                }
            }
            rootNode.getNode("death-message").let {
                sendDeathMessageToPlayer = it.getNode("player-enable").getBoolean(false)
                sendDeathMessageToBroadcast = it.getNode("broadcast-enable").getBoolean(false)
                sendDeathMessageToConsole = it.getNode("console-enable").getBoolean(false)
            }
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

    internal class CostStage(var max: Double, var cost: Double, var ifPercent: Boolean)
}
