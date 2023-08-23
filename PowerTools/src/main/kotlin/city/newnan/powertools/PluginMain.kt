package city.newnan.powertools

import city.newnan.powertools.config.ConfigFile
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.plugin.ExtendedJavaPlugin
import java.util.*

class PluginMain : ExtendedJavaPlugin() {
    companion object {
        lateinit var INSTANCE: PluginMain
            private set
    }
    init { INSTANCE = this }
    private val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.None)
        }
    }
    val messageManager: MessageManager by lazy { MessageManager(this) }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }

    override fun enable() {
        reload()
        commandManager.enableUnstableAPI("help")
        commandManager.registerCommand(Commands)
        commandManager.locales.setDefaultLocale(Locale.SIMPLIFIED_CHINESE)
        messageManager setPlayerPrefix "§7[§6牛腩小镇§7] §f"
    }

    override fun disable() {
        commandManager.unregisterCommands()
    }

    fun reload() {
        // config.yml
        configManager touch "config.yml"
        val config = configManager.parse<ConfigFile>("config.yml")
    }
}
