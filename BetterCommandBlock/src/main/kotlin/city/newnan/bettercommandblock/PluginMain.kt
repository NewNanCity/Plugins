package city.newnan.bettercommandblock

import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.Events
import me.lucko.helper.plugin.ExtendedJavaPlugin
import org.bukkit.Material
import org.bukkit.block.CommandBlock
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
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

        Events.subscribe(PlayerInteractEvent::class.java, EventPriority.HIGHEST)
            .filter { !it.player.isOp && it.action == Action.RIGHT_CLICK_BLOCK && it.clickedBlock!!.type == Material.COMMAND_BLOCK }
            .filter { it.player.hasPermission("better-command-block.read") }
            .handler {
                val commandBlock = it.clickedBlock!!.state as CommandBlock
                messageManager.printf(it.player, "§6命令方块的命令为: §f${commandBlock.command}")
                it.isCancelled = true
            }
            .bindWith(this)
    }

    override fun disable() {
        commandManager.unregisterCommands()
    }

    fun reload() {
//        // config.yml
//        configManager touch "config.yml"
//        val config = configManager.parse<ConfigFile>("config.yml")
    }
}
