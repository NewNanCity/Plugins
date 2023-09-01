package city.newnan.bettercommandblock

import city.newnan.bettercommandblock.config.ConfigFile
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.Events
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.plugin.ExtendedJavaPlugin
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.CommandBlock
import org.bukkit.command.BlockCommandSender
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.server.ServerCommandEvent
import java.io.File
import java.util.*

val executeCommands = hashSetOf(
    "execute",
    "minecraft:execute",
)

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
    var blockedCommands: HashSet<String> = hashSetOf()

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

        Events.subscribe(ServerCommandEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .handler {
                if (it.sender !is BlockCommandSender) return@handler
                val cs = it.command.split(" ", limit = 2)
                val command = cs[0].lowercase(Locale.getDefault())
                if (executeCommands.contains(command) && cs.size > 1) {
                    val commands = cs[1].split(" ")
                    val indexOfRun = commands.indexOf("run")
                    if (commands.size - indexOfRun < 2) return@handler
                    val commandRun = commands[indexOfRun + 1]
                    if (blockedCommands.contains(commandRun)) {
                        blockCommand((it.sender as BlockCommandSender).block, it.command)
                        it.isCancelled = true
                    }
                } else if (blockedCommands.contains(command)) {
                    blockCommand((it.sender as BlockCommandSender).block, it.command)
                    it.isCancelled = true
                }
            }
            .bindWith(this)
    }

    override fun disable() {
        commandManager.unregisterCommands()
    }

    fun reload() {
        // config.yml
        configManager touch "config.yml"
        blockedCommands = configManager.parse<ConfigFile>("config.yml").blockedCommands
    }

    fun blockCommand(block: Block, command: String) {
        val world = block.world.name
        val location = block.location
        File(INSTANCE.dataFolder, "blocked-commands.log")
            .appendText("[${java.time.LocalDateTime.now()}] ${block.type} 在 $world 的 (${location.blockX
            }, ${location.blockY}, ${location.blockZ}) 处执行了被禁止的命令: /$command\n")
        block.type = org.bukkit.Material.AIR
    }
}
