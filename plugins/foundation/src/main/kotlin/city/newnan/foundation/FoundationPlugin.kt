package city.newnan.foundation

import city.newnan.core.base.BasePlugin
import city.newnan.core.config.CorePluginConfig
import city.newnan.config.extensions.configManager
import city.newnan.foundation.commands.CommandRegistry
import city.newnan.foundation.config.FoundationConfig
import city.newnan.foundation.i18n.LanguageKeys
import city.newnan.foundation.manager.TransferManager
import city.newnan.i18n.extensions.setupLanguageManager
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.RegisteredServiceProvider
import java.util.*

/**
 * Foundation 牛腩基金插件
 *
 * 提供基于经济系统的转账监控和基金管理功能，支持：
 * - 自动检测玩家间转账
 * - 转账记录统计和存储
 * - CSV数据导入导出
 * - 与Vault和Essentials集成
 * - 基金账户管理
 * - 国际化支持
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class FoundationPlugin : BasePlugin() {

    companion object {
        lateinit var instance: FoundationPlugin
            private set
    }

    /**
     * Vault经济服务
     */
    lateinit var economy: Economy
        private set

    /**
     * 基金目标账户
     */
    var targetAccount: OfflinePlayer? = null
        private set

    /**
     * 转账管理器
     */
    lateinit var transferManager: TransferManager
        private set

    /**
     * 命令注册器
     */
    private lateinit var commandRegistry: CommandRegistry

    override fun onPluginEnable() {
        instance = this

        logger.info("Foundation Plugin enabling...")

        // 检查依赖（不可重载的功能）
        if (!setupDependencies()) {
            logger.error("Dependency check failed! Foundation requires Vault and Economy provider.")
            server.pluginManager.disablePlugin(this)
            return
        }

        // 初始化模块
        transferManager = TransferManager("TransferManager", this)

        // 调用重载方法处理可重载的功能
        reloadPlugin()

        // 初始化命令注册器（不可重载）
        commandRegistry = CommandRegistry(this)
    }

    override fun onPluginDisable() {
        // 所有绑定的资源会自动清理
    }

    /**
     * 设置依赖
     */
    private fun setupDependencies(): Boolean {
        // 检查Vault
        if (server.pluginManager.getPlugin("Vault") == null) {
            logger.error("Vault plugin not found!")
            return false
        }

        val rsp: RegisteredServiceProvider<Economy>? = server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            logger.error("Vault economy provider not found!")
            return false
        }

        economy = rsp.provider

        return true
    }

    fun getPluginConfig(): FoundationConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<FoundationConfig>("config.yml")
    }

    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()

    /**
     * 重载插件配置
     */
    override fun reloadPlugin() {
        try {
            // 清理配置缓存
            configManager.clearCache()

            // 重新设置语言管理器
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

            val config = getPluginConfig()

            // 重新加载目标账户
            logger.info("Target account: ${config.targetAccount}")
            targetAccount = config.targetAccount?.let { name ->
                if (name.isBlank()) return@let null
                try {
                    val uuid = UUID.fromString(name)
                    val account = Bukkit.getOfflinePlayer(uuid)
                    logger.info(LanguageKeys.Core.Plugin.TARGET_ACCOUNT_SET, "[UUID: $name]")
                    account
                } catch (e: IllegalArgumentException) {
                    val account = Bukkit.getOfflinePlayer(name)
                    if (!account.hasPlayedBefore()) throw Exception("Player $name not found")
                    logger.info(LanguageKeys.Core.Plugin.TARGET_ACCOUNT_SET, name)
                    account
                }
            }

            if (targetAccount == null) {
                logger.warn(LanguageKeys.Core.Plugin.TARGET_ACCOUNT_NOT_SET)
            }

            // 重载所有BaseModule子模块（必需）
            super.reloadPlugin()
        } catch (e: Exception) {
            logger.error(LanguageKeys.Core.Config.RELOAD_FAILED, e)
            throw e
        }
    }
}
