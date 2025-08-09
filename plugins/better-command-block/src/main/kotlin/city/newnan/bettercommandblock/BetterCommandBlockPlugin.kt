package city.newnan.bettercommandblock

import city.newnan.bettercommandblock.commands.CommandRegistry
import city.newnan.bettercommandblock.config.BetterCommandBlockConfig
import city.newnan.bettercommandblock.firewall.CommandBlockFirewallModule
import city.newnan.bettercommandblock.i18n.LanguageKeys
import city.newnan.bettercommandblock.modules.CommandBlockViewModule
import city.newnan.config.extensions.configManager
import city.newnan.core.base.BasePlugin
import city.newnan.core.config.CorePluginConfig
import city.newnan.i18n.extensions.setupLanguageManager
import java.util.*

/**
 * BetterCommandBlock 插件主类
 *
 * 提供增强的命令方块功能，包括：
 * - 命令方块安全控制（阻止危险命令执行）
 * - 命令方块内容查看功能
 * - 扩展命令功能（pick、scoreboard random、execute）
 *
 * 基于现代化的BasePlugin架构，提供完整的生命周期管理和资源管理。
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class BetterCommandBlockPlugin : BasePlugin() {

    companion object {
        lateinit var instance: BetterCommandBlockPlugin
            private set
    }

    lateinit var firewallModule: CommandBlockFirewallModule
        private set
    lateinit var viewModule: CommandBlockViewModule
        private set
    lateinit var commandRegistry: CommandRegistry
        private set

    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()

    /**
     * 获取插件配置
     * 使用 touchWithMerge 确保配置文件完整性，支持配置迁移
     */
    fun getPluginConfig(): BetterCommandBlockConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<BetterCommandBlockConfig>("config.yml")
    }

    override fun onPluginLoad() {
        instance = this
    }

    override fun onPluginEnable() {
        try {
            // 调用重载方法处理可重载的功能
            reloadPlugin()

            // 1. 初始化新的防火墙模块（高优先级，替代旧的安全模块）
            firewallModule = CommandBlockFirewallModule(this)

            // 2. 初始化查看模块（独立功能）
            viewModule = CommandBlockViewModule(this)

            // 3. 初始化命令注册器（依赖前面的模块）
            commandRegistry = CommandRegistry(this)
        } catch (e: Exception) {
            logger.error("BetterCommandBlock Plugin enable failed", e)
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun reloadPlugin() {
        try {
            // 1. 清理配置缓存（必需，否则无法从磁盘加载最新的文件）
            configManager.clearCache()

            // 2. 重新设置语言管理器（必需）
            setupLanguageManager(
                languageFiles = mapOf(
                    Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                    Locale.US to "lang/en_US.yml"
                ),
                majorLanguage = Locale.SIMPLIFIED_CHINESE,
                defaultLanguage = Locale.US
            )

            // 3. 重载所有BaseModule子模块（必需）
            super.reloadPlugin()
        } catch (e: Exception) {
            logger.error(LanguageKeys.Core.Plugin.RELOAD_FAILED, e)
            throw e
        }
    }

    override fun onPluginDisable() {
        // 所有绑定的资源会自动清理
    }
}
