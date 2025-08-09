package city.newnan.mcron

import city.newnan.core.base.BasePlugin
import city.newnan.core.config.CorePluginConfig
import city.newnan.config.extensions.configManager
import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runSync
import city.newnan.i18n.extensions.setupLanguageManager
import city.newnan.mcron.commands.CommandRegistry
import city.newnan.mcron.config.MCronConfig
import city.newnan.mcron.i18n.LanguageKeys
import city.newnan.mcron.modules.CronManager
import city.newnan.mcron.modules.ServerEventManager
import java.util.*

/**
 * MCron插件主类
 *
 * 现代化的Minecraft定时任务插件，支持cron表达式和多种调度模式。
 * 基于项目标准架构，集成了配置管理、国际化、GUI等功能。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class MCronPlugin : BasePlugin() {

    companion object {
        lateinit var instance: MCronPlugin
            private set
    }

    // 监听器
    private lateinit var serverEventManager: ServerEventManager

    // 命令注册器
    private lateinit var commandRegistry: CommandRegistry

    // Cron管理器
    lateinit var cronManager: CronManager
        private set

    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()

    /**
     * 获取插件配置
     */
    fun getPluginConfig(): MCronConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<MCronConfig>("config.yml")
    }

    override fun onPluginEnable() {
        instance = this

        // 初始化Cron管理器
        cronManager = CronManager(this)

        // 初始化服务器事件监听器
        serverEventManager = ServerEventManager(this)

        // 调用重载方法处理可重载的功能
        reloadPlugin()

        // 初始化命令
        commandRegistry = CommandRegistry(this)
    }

    override fun onPluginDisable() {
        // 所有绑定的资源会自动清理
    }

    /**
     * 重载插件配置
     */
    override fun reloadPlugin() {
        try {
            // 1. 清理配置缓存
            configManager.clearCache()

            // 2. 重新设置语言管理器（必需）
            setupLanguageManager(
                languageFiles = mapOf(
                    Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                    Locale.US to "lang/en_US.yml"
                ),
                mergeWithTemplate = true,
                createBackup = false,
                majorLanguage = Locale.SIMPLIFIED_CHINESE,
                defaultLanguage = Locale.US
            )

            // 3. 重新加载配置
            // 设置时区
            System.setProperty("user.timezone", getPluginConfig().timezone.getZoneOffset().id)

            // 4. 重载所有BaseModule子模块（必需）
            super.reloadPlugin()
        } catch (e: Exception) {
            logger.error(LanguageKeys.Core.Config.RELOAD_FAILED, e)
            throw e
        }
    }

    /**
     * 执行命令列表
     */
    fun executeCommands(commands: Collection<String>, context: String = "UNKNOWN") {
        if (commands.isEmpty()) return

        runAsync {
            commands.forEach { command ->
                try {
                    when {
                        command.startsWith($$"$sleep$ ") -> {
                            // 处理睡眠命令
                            handleSleepCommand(command)
                        }
                        command.startsWith($$"$async$ ") -> {
                            // 异步执行命令
                            val actualCommand = command.substring(8)
                            runAsync {
                                server.dispatchCommand(server.consoleSender, actualCommand)
                            }
                        }
                        command.startsWith($$"$sync$ ") -> {
                            // 同步执行命令
                            val actualCommand = command.substring(7)
                            runSync {
                                server.dispatchCommand(server.consoleSender, actualCommand)
                            }
                        }
                        else -> {
                            // 默认同步执行
                            runSync {
                                server.dispatchCommand(server.consoleSender, command)
                            }
                        }
                    }

                    logger.info(LanguageKeys.Commands.Executed.COMMAND, command, context) // <purple>$<bold>{0}</bold><$/purple> <green>{1}</green>
                } catch (e: Exception) {
                    logger.error(LanguageKeys.Commands.Executed.FAILED, e, command, context, e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * 处理睡眠命令
     */
    private fun handleSleepCommand(command: String) {
        val sleepArg = command.substring(8).trim()
        val sleepTime = parseSleepTime(sleepArg)

        if (sleepTime > 0) {
            Thread.sleep(sleepTime)
        }
    }

    /**
     * 解析睡眠时间
     */
    private fun parseSleepTime(sleepArg: String): Long {
        return try {
            when {
                sleepArg.endsWith("ms") -> sleepArg.dropLast(2).toLong()
                sleepArg.endsWith("s") -> sleepArg.dropLast(1).toLong() * 1000
                sleepArg.endsWith("m") -> sleepArg.dropLast(1).toLong() * 60 * 1000
                sleepArg.endsWith("h") -> sleepArg.dropLast(1).toLong() * 60 * 60 * 1000
                sleepArg.endsWith("d") -> sleepArg.dropLast(1).toLong() * 24 * 60 * 60 * 1000
                else -> sleepArg.toLong()
            }
        } catch (e: NumberFormatException) {
            logger.warn(LanguageKeys.Business.Common.INVALID_SLEEP_TIME, sleepArg)
            0L
        }
    }
}
