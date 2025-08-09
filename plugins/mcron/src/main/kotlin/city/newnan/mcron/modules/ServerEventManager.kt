package city.newnan.mcron.modules

import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.mcron.MCronPlugin
import city.newnan.mcron.i18n.LanguageKeys
import org.bukkit.event.EventPriority
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.plugin.Plugin
import java.time.ZonedDateTime
import kotlin.math.abs

/**
 * 服务器事件监听器
 *
 * 监听服务器和插件相关事件，执行对应的定时任务。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class ServerEventManager(private val plugin: MCronPlugin) : BaseModule("ServerEventManager", plugin) {

    init { init() }

    override fun onInit() {
        subscribeEvent<ServerLoadEvent> {
            priority(EventPriority.MONITOR)
            handler { handleServerLoad() }
        }

        subscribeEvent<PluginEnableEvent> {
            priority(EventPriority.MONITOR)
            handler { handlePluginEnable(it.plugin) }
        }

        subscribeEvent<PluginDisableEvent> {
            priority(EventPriority.MONITOR)
            handler { handlePluginDisable(it.plugin) }
        }

        logger.debug(LanguageKeys.Core.Plugin.LISTENERS_REGISTERED)
    }

    fun handleServerLoad() {
        val config = plugin.getPluginConfig()

        // 执行服务器启动完成时的任务
        val nowDateTime = ZonedDateTime.now(plugin.cronManager.timezoneOffset)
        val now = System.currentTimeMillis()
        config.tasks.onServerReady.forEach { (cronExpression, commands) ->
            try {
                // 检查是否应该立即执行（1秒内）
                val nextTime = plugin.cronManager.createTask(cronExpression, commands).getNextTime(nowDateTime)
                if (abs(nextTime - now) < 1000L) {
                    plugin.executeCommands(commands, "SERVER_READY")
                }
            } catch (e: Exception) {
                plugin.logger.warn(LanguageKeys.Business.Server.READY_TASK_FAILED, cronExpression, e.message ?: "Unknown error")
            }
        }

        plugin.logger.info(LanguageKeys.Business.Server.LOADED)
    }

    /**
     * 插件启用事件
     */
    fun handlePluginEnable(enabledPlugin: Plugin) {
        val config = plugin.getPluginConfig()
        val pluginName = enabledPlugin.name

        // 执行插件启用时的任务
        config.tasks.onPluginEnable[pluginName]?.let { commands ->
            plugin.executeCommands(commands, "PLUGIN_ENABLE:$pluginName")
        }
    }

    /**
     * 插件禁用事件
     */
    fun handlePluginDisable(enabledPlugin: Plugin) {
        val config = plugin.getPluginConfig()
        val pluginName = enabledPlugin.name

        // 执行插件禁用时的任务
        config.tasks.onPluginDisable[pluginName]?.let { commands ->
            plugin.executeCommands(commands, "PLUGIN_DISABLE:$pluginName")
        }
    }
}