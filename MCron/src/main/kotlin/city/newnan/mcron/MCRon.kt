package city.newnan.mcron

import co.aikar.commands.PaperCommandManager
import city.newnan.violet.config.ConfigManager
import city.newnan.violet.config.setListIfNull
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


class MCron : ExtendedJavaPlugin() {
    internal val configManager: ConfigManager by lazy { ConfigManager(this) }
    private val languageManager: LanguageManager by lazy { LanguageManager(this) }
    internal val messageManager: MessageManager by lazy { MessageManager(this) }
    internal val cronManager: CronManager by lazy { CronManager() }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }
    companion object {
        lateinit var INSTANCE: MCron
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
            usePerIssuerLocale(true, false)
            registerCommand(CronCommands)
            locales.loadYamlLanguageFile("config.yml", Locale("config"))
        }

        // 运行Cron
        cronManager.run()
        Events.subscribe(ServerLoadEvent::class.java, EventPriority.MONITOR)
            .handler { executeCommandsByConfig("on-server-ready") }
            .bindWith(this)
        Events.subscribe(PluginEnableEvent::class.java, EventPriority.MONITOR)
            .handler { e: PluginEnableEvent ->
                executeCommandsByConfig("on-plugin-enable", e.plugin.description.name)
            }
            .bindWith(this)
        Events.subscribe(PluginDisableEvent::class.java, EventPriority.MONITOR)
            .handler { e: PluginDisableEvent ->
                executeCommandsByConfig("on-plugin-disable", e.plugin.description.name)
            }
            .bindWith(this)
    }

    override fun disable() {
        commandManager.unregisterCommands()
    }

    fun reload() = cronManager.reload()

    internal fun executeCommandsByConfig(vararg nodePath: Any?) {
        Bukkit.getConsoleSender().run {
            configManager["config.yml"]?.getNode(nodePath)?.setListIfNull()
                ?.getList { obj: Any -> obj.toString() }?.forEach { command ->
                    messageManager.printf("\$msg.execute$", command)
                    Bukkit.dispatchCommand(this, command)
                }
        }
    }
}