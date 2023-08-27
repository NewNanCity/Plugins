package city.newnan.newnanmain

import city.newnan.newnanmain.config.ConfigFile
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.gui.GuiManager
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.plugin.ExtendedJavaPlugin
import net.milkbowl.vault.chat.Chat
import java.util.Locale

class PluginMain : ExtendedJavaPlugin() {
    companion object {
        lateinit var INSTANCE: PluginMain
            private set
    }
    init { INSTANCE = this }
    val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.LRU, 32)
        }
    }
    val gui: GuiManager by lazy { GuiManager(this) }
    val teleportManager: TeleportManager by lazy { TeleportManager(this) }
    val prefixManager: PrefixManager by lazy { PrefixManager(this) }
    val messageManager: MessageManager by lazy { MessageManager(this) }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }
    lateinit var chat: Chat

    override fun enable() {
        if (server.pluginManager.getPlugin("Vault") == null) throw Exception("Vault not found!")
        chat = server.servicesManager.getRegistration(Chat::class.java)?.provider
            ?: throw Exception("Vault permission service not found!")

        reload()
        commandManager.enableUnstableAPI("help")
        commandManager.registerCommand(Commands)
        commandManager.locales.setDefaultLocale(Locale.SIMPLIFIED_CHINESE)
        messageManager setPlayerPrefix "§7[§6牛腩小镇§7] §f"

        prefixManager.enable()
        teleportManager.enable()
    }

    override fun disable() {
        commandManager.unregisterCommands()
    }

    fun reload() {
        prefixManager.reload()
        teleportManager.reload()
    }
}
