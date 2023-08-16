package city.newnan.fundation

import city.newnan.fundation.config.ConfigFile
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.message.MessageManager
import me.lucko.helper.Events
import me.lucko.helper.plugin.ExtendedJavaPlugin
import net.ess3.api.events.UserBalanceUpdateEvent
import net.milkbowl.vault.economy.Economy
import org.bukkit.OfflinePlayer
import org.bukkit.event.EventPriority
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit

data class TransferOther(val account: OfflinePlayer, val amount: BigDecimal, val time: Long) {
    fun compareTo(other: TransferOther): Int {
        return (time - other.time).toInt()
    }
}
data class TransferSelf(val amount: BigDecimal, val time: Long) {
    fun compareTo(other: TransferSelf): Int {
        return (time - other.time).toInt()
    }
}

class PluginMain : ExtendedJavaPlugin() {
    private val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.None)
        }
    }
    private val messageManager: MessageManager by lazy { MessageManager(this) }
    private lateinit var economy: Economy
    private var targetAccount: OfflinePlayer? = null
    // 红黑树
    private val otherTransfers = TreeSet<TransferOther>()
    private val selfTransfers = TreeSet<TransferSelf>()

    override fun enable() {
        if (server.pluginManager.getPlugin("Vault") == null) {
            throw Exception("Vault not found!")
        }
        economy = server.servicesManager.getRegistration(Economy::class.java)?.provider
            ?: throw Exception("Vault economy service not found!")

        reload()
        messageManager setPlayerPrefix "§7[§6牛腩小镇§7] §f"

        if (targetAccount == null) {
            logger.warning("Target account not found!")
            return
        }

        Events.subscribe(UserBalanceUpdateEvent::class.java, EventPriority.MONITOR)
            .handler { event ->
                // 清理过期内容
                val now = System.currentTimeMillis()
                val expired = now - TimeUnit.SECONDS.toMillis(5)
                while (!otherTransfers.isEmpty() && otherTransfers.first().time < expired)
                    otherTransfers.pollFirst()
                while (!selfTransfers.isEmpty() && selfTransfers.first().time < expired)
                    selfTransfers.pollFirst()


                if (event.player == targetAccount) {
                    // 流入是别人的流出
                    val amount = event.newBalance - event.oldBalance
                    // 查看转入交易中有没有金额一致的
                    val transfer = otherTransfers.find { t -> t.amount == amount }
                    if (transfer == null) {
                        selfTransfers.add(TransferSelf(-amount, now))
                    } else {
                        otherTransfers.remove(transfer)
                        handleTransfer(transfer.account, -amount)
                    }
                } else {
                    // 流出是别人的流入
                    val amount = event.newBalance - event.oldBalance
                    // 查看转出交易中有没有金额一致的
                    val transfer = selfTransfers.find { t -> t.amount == amount }
                    if (transfer == null) {
                        otherTransfers.add(TransferOther(event.player, amount, now))
                    } else {
                        selfTransfers.remove(transfer)
                        handleTransfer(event.player, amount)
                    }
                }
            }
    }

    private fun reload() {
        configManager touch "config.yml"
        configManager.parse<ConfigFile>("config.yml").also {
            targetAccount = it.target?.let { name ->
                if (name.isBlank()) return@let null
                server.offlinePlayers.find { p -> p.name == name }?.also { p ->
                    messageManager.info("设置基金账户为: ${p.name}")
                }
            }
        }
    }

    // 其他人->目标：正
    // 目标->其他人：负
    private fun handleTransfer(account: OfflinePlayer, amount: BigDecimal) {

    }
}
