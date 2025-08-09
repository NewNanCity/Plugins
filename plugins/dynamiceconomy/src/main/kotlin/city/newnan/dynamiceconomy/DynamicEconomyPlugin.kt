package city.newnan.dynamiceconomy

import city.newnan.config.extensions.configManager
import city.newnan.core.base.BasePlugin
import city.newnan.core.config.CorePluginConfig
import city.newnan.dynamiceconomy.commands.CommandRegistry
import city.newnan.dynamiceconomy.config.DynamicEconomyConfig
import city.newnan.dynamiceconomy.i18n.LanguageKeys
import city.newnan.dynamiceconomy.modules.CommodityManager
import city.newnan.dynamiceconomy.modules.EconomyManager
import city.newnan.dynamiceconomy.modules.WealthManager
import city.newnan.i18n.extensions.setupLanguageManager
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import java.util.*

/**
 * DynamicEconomy插件主类
 *
 * 提供动态经济系统，包括：
 * - 价值资源统计和管理
 * - 动态商品价格系统
 * - 货币发行和国库管理
 * - 商品交易系统
 * - 完整的国际化支持
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class DynamicEconomyPlugin : BasePlugin() {

    // ===== 模块声明 =====
    private lateinit var wealthManager: WealthManager
    private lateinit var economyManager: EconomyManager
    private lateinit var commodityManager: CommodityManager
    private lateinit var commandRegistry: CommandRegistry

    // ===== 外部服务 =====
    var economy: Economy? = null
        private set

    override fun onPluginLoad() {
        logger.info("DynamicEconomy Plugin loading...")
    }

    override fun onPluginEnable() {
        // 调用重载方法
        reloadPlugin()

        // 初始化外部服务
        initializeExternalServices()

        // 初始化模块（按依赖顺序）
        wealthManager = WealthManager("WealthManager", this)
        economyManager = EconomyManager("EconomyManager", this)
        commodityManager = CommodityManager("CommodityManager", this)

        // 初始化命令注册器（不可重载）
        commandRegistry = CommandRegistry(this)
    }

    override fun onPluginDisable() {
        // BasePlugin会自动处理资源清理
    }

    override fun reloadPlugin() {
        try {
            // 1. 清理配置缓存（必需）
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

            // 3. 重载所有子模块（必需）
            super.reloadPlugin()
        } catch (e: Exception) {
            logger.error(LanguageKeys.Core.Plugin.RELOAD_FAILED, e)
            throw e
        }
    }

    /**
     * 初始化外部服务
     */
    private fun initializeExternalServices() {
        // 初始化Vault经济系统
        try {
            if (isPluginPresent("Vault")) {
                economy = getService(Economy::class.java)
                if (economy != null) {
                    logger.info("Vault economy system connected")
                } else {
                    logger.warning(LanguageKeys.Log.Warning.VAULT_NOT_FOUND)
                }
            } else {
                logger.warning(LanguageKeys.Log.Warning.VAULT_NOT_FOUND)
            }
        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.VAULT_ERROR, e)
        }
    }

    // ===== 配置管理 =====

    /**
     * 获取插件配置
     * 使用标准的touchWithMerge + parse模式
     */
    fun getPluginConfig(): DynamicEconomyConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<DynamicEconomyConfig>("config.yml")
    }

    /**
     * 获取核心配置
     * 使用推荐的getCoreConfig()方法模式
     */
    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()

    // ===== 模块访问器 =====

    /**
     * 获取财富管理器
     */
    fun getWealthManager(): WealthManager = wealthManager

    /**
     * 获取经济管理器
     */
    fun getEconomyManager(): EconomyManager = economyManager

    /**
     * 获取商品管理器
     */
    fun getCommodityManager(): CommodityManager = commodityManager
}
