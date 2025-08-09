package city.newnan.newnanmain

import city.newnan.config.extensions.configManager
import city.newnan.core.base.BasePlugin
import city.newnan.core.config.CorePluginConfig
import city.newnan.i18n.extensions.setupLanguageManager
import city.newnan.newnanmain.commands.CommandRegistry
import city.newnan.newnanmain.config.NewNanMainConfig
import city.newnan.newnanmain.i18n.LanguageKeys
import city.newnan.newnanmain.modules.PrefixManager
import city.newnan.newnanmain.modules.TeleportManager
import net.milkbowl.vault.chat.Chat
import org.bukkit.Bukkit
import java.util.*

/**
 * NewNanMain插件主类
 *
 * 提供牛腩小镇的核心功能，包括：
 * - 前缀管理系统
 * - 传送点管理
 * - GUI界面
 * - Vault聊天系统集成
 * - 完整的国际化支持
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class NewNanMainPlugin : BasePlugin() {

    // ===== 模块声明 =====
    lateinit var prefixManager: PrefixManager
        private set
    lateinit var teleportManager: TeleportManager
        private set
    private lateinit var commandRegistry: CommandRegistry

    // ===== 外部服务 =====
    var chat: Chat? = null
        private set

    override fun onPluginLoad() {
        logger.info("NewNanMain Plugin loading...")
    }

    override fun onPluginEnable() {
        // 调用重载方法
        reloadPlugin()

        // 初始化外部服务
        initializeExternalServices()

        // 初始化模块（按依赖顺序）
        prefixManager = PrefixManager("PrefixManager", this)
        teleportManager = TeleportManager("TeleportManager", this)

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
        // 初始化Vault聊天系统
        try {
            if (isPluginPresent("Vault")) {
                chat = getService(Chat::class.java)
                if (chat != null) {
                    logger.info("Vault chat system connected")
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
    fun getPluginConfig(): NewNanMainConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<NewNanMainConfig>("config.yml")
    }

    /**
     * 获取核心配置
     * 使用推荐的getCoreConfig()方法模式
     */
    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()
}
