package city.newnan.mcron

import co.aikar.commands.PaperCommandManager
import city.newnan.violet.config.ConfigManager
import city.newnan.violet.config.setListIfNull
import city.newnan.violet.i18n.LanguageManager
import city.newnan.violet.message.MessageManager
import me.lucko.helper.Events
import me.lucko.helper.plugin.ExtendedJavaPlugin
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.event.EventPriority
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.event.server.ServerLoadEvent
import java.io.IOException
import java.util.*


open class MCron : ExtendedJavaPlugin() {
    internal var configManager: ConfigManager? = null
    private var languageManager: LanguageManager? = null
    internal var messageManager: MessageManager? = null
    internal var cronManager: CronManager? = null
    override fun load() {
        // 初始化ConfigManager
        configManager = ConfigManager(this).apply {
            touch("config.yml")
        }

        // 初始化LanguageManager
        try {
            val locale = Locale("config")
            languageManager = LanguageManager(this)
                .register(locale, "config.yml")
                .setMajorLanguage(locale)
        } catch (e: LanguageManager.FileNotFoundException) {
            e.printStackTrace()
            onDisable()
        } catch (e: ConfigManager.UnknownConfigFileFormatException) {
            e.printStackTrace()
            onDisable()
        } catch (e: IOException) {
            e.printStackTrace()
            onDisable()
        }

        // 初始化MessageManager
        messageManager = languageManager?.let {
            MessageManager(this).setLanguageProvider(it)
        }
        messageManager?.setPlayerPrefix(messageManager!!.sprintf("\$msg.prefix$"))
        instance = this
    }

    override fun enable() {
        // 初始化CommandManager - 不能在load()里面初始化！
        val commandManager = PaperCommandManager(this)
        commandManager.usePerIssuerLocale(true, false)
        try {
            commandManager.locales.loadYamlLanguageFile("config.yml", Locale("config"))
        } catch (e: IOException) {
            e.printStackTrace()
            onDisable()
        } catch (e: InvalidConfigurationException) {
            e.printStackTrace()
            onDisable()
        }

        // 注册指令
        commandManager.registerCommand(CronCommands)

        // 注册Cron管理器
        cronManager = CronManager()
        Events.subscribe(ServerLoadEvent::class.java, EventPriority.MONITOR)
            .handler { onServerStartup() }
        Events.subscribe(PluginEnableEvent::class.java, EventPriority.MONITOR)
            .handler { e: PluginEnableEvent ->
                onPluginEnable(
                    e.plugin.description.name
                )
            }
        Events.subscribe(PluginDisableEvent::class.java, EventPriority.MONITOR)
            .handler { e: PluginDisableEvent ->
                onPluginDisable(
                    e.plugin.description.name
                )
            }
    }

    override fun disable() {}
    fun reload() = cronManager!!.reload()

    internal fun executeCommandsByConfig(vararg nodePath: Any?) {
        try {
            val sender: CommandSender = Bukkit.getConsoleSender()
            configManager?.get("config.yml")?.getNode(nodePath)?.setListIfNull()
                ?.getList { obj: Any -> obj.toString() }?.forEach { command ->
                    messageManager?.printf("\$msg.execute$", command)
                    Bukkit.dispatchCommand(sender, command)
                }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ConfigManager.UnknownConfigFileFormatException) {
            e.printStackTrace()
        }
    }

    private fun onServerStartup() {
        executeCommandsByConfig("on-server-ready")
    }

    private fun onPluginEnable(pluginName: String) {
        executeCommandsByConfig("on-plugin-enable", pluginName)
    }

    private fun onPluginDisable(pluginName: String) {
        executeCommandsByConfig("on-plugin-disable", pluginName)
    }

    companion object {
        var instance: MCron? = null
            private set
    }
}