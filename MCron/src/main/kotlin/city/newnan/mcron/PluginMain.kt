package city.newnan.mcron

import city.newnan.mcron.config.ConfigFile
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.Events
import me.lucko.helper.plugin.ExtendedJavaPlugin
import org.bukkit.Bukkit
import org.bukkit.event.EventPriority
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.event.server.ServerLoadEvent
import kotlin.math.abs


class PluginMain : ExtendedJavaPlugin() {
    internal val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.LRU, 4)
        }
    }
    internal val messageManager: MessageManager by lazy { MessageManager(this) }
    internal val cronManager: CronManager by lazy { CronManager() }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }
    companion object {
        lateinit var INSTANCE: PluginMain
            private set
    }
    init { INSTANCE = this }

    override fun enable() {
        // 初始化ConfigManager
        configManager touch "config.yml"

        // 初始化MessageManager
        messageManager setPlayerPrefix "§7[§6牛腩小镇§7] §r"

        // 初始化CommandManager
        commandManager.enableUnstableAPI("help")
        commandManager.registerCommand(Commands)

        // 运行Cron
        cronManager.run()
        Events.subscribe(ServerLoadEvent::class.java, EventPriority.MONITOR)
            .handler {
                val now = System.currentTimeMillis()
                configManager.parse<ConfigFile>("config.yml").onServerReady.filterKeys {
                    abs(CronExpression(it).getNextTime(now) - now) < 1000L
                }.forEach { (_, commands) -> executeCommands(commands) }
            }
            .bindWith(this)
        Events.subscribe(PluginEnableEvent::class.java, EventPriority.MONITOR)
            .handler {
                configManager.parse<ConfigFile>("config.yml")
                    .onPluginReady[it.plugin.description.name]?.apply { executeCommands(this) }
            }
            .bindWith(this)
        Events.subscribe(PluginDisableEvent::class.java, EventPriority.MONITOR)
            .handler {
                configManager.parse<ConfigFile>("config.yml")
                    .onPluginDisable[it.plugin.description.name]?.apply { executeCommands(this) }
            }
            .bindWith(this)
    }

    override fun disable() {
        commandManager.unregisterCommands()
    }

    fun reload() {
        configManager.cache?.clear()
        cronManager.reload()
    }

    private fun executeCommands(commands: Iterable<String>) {
        Bukkit.getConsoleSender().run {
            commands.forEach { command ->
                messageManager.printf("> $command")
                Bukkit.dispatchCommand(this, command)
            }
        }
    }
}