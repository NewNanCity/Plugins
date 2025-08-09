package city.newnan.deathcost

import city.newnan.core.base.BasePlugin
import city.newnan.core.config.CorePluginConfig
import city.newnan.config.extensions.configManager
import city.newnan.core.scheduler.runSync
import city.newnan.deathcost.commands.CommandRegistry
import city.newnan.deathcost.config.DeathCostConfig
import city.newnan.deathcost.i18n.LanguageKeys
import city.newnan.deathcost.modules.DeathCostModule
import city.newnan.i18n.extensions.setupLanguageManager
import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.RegisteredServiceProvider
import java.util.*

/**
 * DeathCost 死亡扣费插件
 *
 * 提供基于经济系统的死亡扣费功能，支持：
 * - 简单模式和复杂阶梯扣费
 * - 与Vault经济系统集成
 * - 权限控制和免费死亡
 * - 死亡消息自定义
 * - 转账到指定账户
 * - 国际化支持
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class DeathCostPlugin : BasePlugin() {

    companion object {
        lateinit var instance: DeathCostPlugin
            private set
    }

    /**
     * Vault经济服务
     */
    lateinit var economy: Economy
        private set

    /**
     * 死亡扣费模块
     */
    private lateinit var deathCostModule: DeathCostModule

    /**
     * 命令注册器
     */
    private lateinit var commandRegistry: CommandRegistry

    override fun onPluginEnable() {
        instance = this

        // 检查Vault依赖（不可重载的功能）
        if (!setupEconomy()) {
            logger.error("Vault plugin not found! DeathCost requires Vault for economy support.")
            server.pluginManager.disablePlugin(this)
            return
        }

        // 初始化模块
        deathCostModule = DeathCostModule("DeathCostModule", this)
        commandRegistry = CommandRegistry(this)

        // 调用重载方法处理可重载的功能
        reloadPlugin()
    }

    override fun onPluginDisable() {
        // 所有绑定的资源会自动清理
    }

    /**
     * 设置经济系统
     */
    private fun setupEconomy(): Boolean {
        if (!isPluginPresent("Vault")) return false
        economy = getService(Economy::class.java) ?: return false
        return true
    }

    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()

    /**
     * 获取插件配置
     */
    fun getPluginConfig(): DeathCostConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<DeathCostConfig>("config.yml")
    }

    /**
     * 重载插件配置
     */
    override fun reloadPlugin() {
        try {
            logger.info("Reloading DeathCost configuration...")

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
            logger.error(LanguageKeys.Core.Config.RELOAD_FAILED, e)
            throw e
        }
    }
}
