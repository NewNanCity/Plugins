package city.newnan.mcron

import city.newnan.mcron.config.ConfigFile
import co.aikar.commands.PaperCommandManager
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.i18n.LanguageManager
import city.newnan.violet.message.MessageManager
import me.lucko.helper.Events
import me.lucko.helper.plugin.ExtendedJavaPlugin
import org.bukkit.Bukkit
import org.bukkit.event.EventPriority
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.event.server.ServerLoadEvent
import java.util.*


class PluginMain : ExtendedJavaPlugin() {
    internal val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.LRU, 4)
        }
    }
    private val languageManager: LanguageManager by lazy { LanguageManager(this) }
    internal val messageManager: MessageManager by lazy { MessageManager(this) }
    internal val cronManager: CronManager by lazy { CronManager() }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }
    companion object {
        lateinit var INSTANCE: PluginMain
            private set
    }
    init {
        INSTANCE = this
    }

    override fun enable() {
        // 初始化ConfigManager
        configManager touch "config.yml"

        // 初始化LanguageManager
        Locale("config").also {
            languageManager.register(it, "config.yml") setMajorLanguage it
        }

        // 初始化MessageManager
        messageManager setLanguageProvider languageManager
        messageManager setPlayerPrefix messageManager.sprintf("\$msg.prefix$")

        // 初始化CommandManager
        commandManager.run {
            enableUnstableAPI("help")
            usePerIssuerLocale(true, false)
            registerCommand(Commands)
            locales.loadYamlLanguageFile("config.yml", Locale("config"))
        }

        // 运行Cron
        cronManager.run()
        Events.subscribe(ServerLoadEvent::class.java, EventPriority.MONITOR)
            .handler {
                executeCommands(configManager.parse<ConfigFile>("config.yml").onServerReady)
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

    fun executeCommands(commands: Iterable<String>) {
        Bukkit.getConsoleSender().run {
            commands.forEach { command ->
                messageManager.printf("\$msg.execute$", command)
                Bukkit.dispatchCommand(this, command)
            }
        }
    }
}