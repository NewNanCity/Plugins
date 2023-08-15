package city.newnan.fundation

import city.newnan.fundation.config.ConfigFile
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.message.MessageManager
import me.lucko.helper.Events
import me.lucko.helper.plugin.ExtendedJavaPlugin
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent

class PluginMain : ExtendedJavaPlugin() {
    private val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.None)
        }
    }
    private val messageManager: MessageManager by lazy { MessageManager(this) }
    private lateinit var economy: Economy
    private var targetAccount: OfflinePlayer? = null

    override fun enable() {
        if (server.pluginManager.getPlugin("Vault") == null) {
            throw Exception("Vault not found!")
        }
        economy = server.servicesManager.getRegistration(Economy::class.java)?.provider
            ?: throw Exception("Vault economy service not found!")

        reload()
        messageManager setPlayerPrefix "§7[§6牛腩小镇§7] §f"
    }

    private fun reload() {
        configManager touch "config.yml"
        configManager.parse<ConfigFile>("config.yml").also {
            if (it.target != null)
                targetAccount = server.offlinePlayers.find { p -> p.name == it.target }?.also { p ->
                    if (!economy.hasAccount(p)) economy.createPlayerAccount(p) }
        }
    }
}
