package city.newnan.feefly

import city.newnan.core.base.BasePlugin
import city.newnan.core.config.CorePluginConfig
import city.newnan.config.extensions.configManager
import city.newnan.feefly.api.FeeFlyService
import city.newnan.feefly.api.FeeFlyServiceImpl
import city.newnan.feefly.commands.CommandRegistry
import city.newnan.feefly.config.FeeFlyConfig
import city.newnan.feefly.i18n.LanguageKeys
import city.newnan.feefly.manager.FlyManager
import city.newnan.i18n.extensions.setupLanguageManager
import net.milkbowl.vault.economy.Economy
import java.util.*

/**
 * FeeFly 付费飞行插件
 *
 * 提供基于经济系统的飞行功能，支持：
 * - 按时间收费的飞行模式
 * - 与Vault经济系统集成
 * - 权限控制和免费飞行
 * - 飞行状态持久化
 * - 多种触发条件管理
 * - 国际化支持
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class FeeFlyPlugin : BasePlugin() {

    companion object {
        lateinit var instance: FeeFlyPlugin
            private set
    }

    /**
     * Vault经济服务
     */
    lateinit var economy: Economy
        private set

    /**
     * 飞行管理器
     */
    lateinit var flyManager: FlyManager
        private set

    /**
     * 命令注册器
     */
    private lateinit var commandRegistry: CommandRegistry

    /**
     * FeeFly服务实现
     */
    private lateinit var feeFlyService: FeeFlyServiceImpl

    override fun onPluginEnable() {
        instance = this

        // 检查Vault依赖
        if (!setupEconomy()) {
            logger.error("Vault plugin not found! FeeFly requires Vault for economy support.")
            server.pluginManager.disablePlugin(this)
            return
        }

        // 调用重载方法处理可重载的功能
        reloadPlugin()

        // 初始化模块
        flyManager = FlyManager("FlyManager", this)

        // 创建并注册服务
        feeFlyService = FeeFlyServiceImpl(this)
        server.servicesManager.register(FeeFlyService::class.java, feeFlyService, this, org.bukkit.plugin.ServicePriority.Normal)

        // 注册命令（不可重载的功能）
        commandRegistry = CommandRegistry(this)
    }

    override fun onPluginDisable() {
        server.servicesManager.unregisterAll(this)

        // 所有绑定的资源会自动清理
    }

    /**
     * 设置经济系统
     */
    private fun setupEconomy(): Boolean {
        if (server.pluginManager.getPlugin("Vault") == null) {
            return false
        }

        economy = getService(Economy::class.java) ?: return false
        return true
    }

    fun getPluginConfig(): FeeFlyConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<FeeFlyConfig>("config.yml")
    }

    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()

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
                majorLanguage = Locale.SIMPLIFIED_CHINESE,
                defaultLanguage = Locale.US
            )

            // 3. 重载所有BaseModule子模块（必需）
            super.reloadPlugin()
        } catch (e: Exception) {
            logger.error(LanguageKeys.Core.Config.RELOAD_FAILED, e)
            throw e
        }
    }
}
