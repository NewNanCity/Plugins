package city.newnan.powertools

import city.newnan.config.extensions.configManager
import city.newnan.core.base.BasePlugin
import city.newnan.core.config.CorePluginConfig
import city.newnan.i18n.extensions.setupLanguageManager
import city.newnan.powertools.commands.CommandRegistry
import city.newnan.powertools.config.PowerToolsConfig
import city.newnan.powertools.i18n.LanguageKeys
import java.util.*

/**
 * PowerTools插件主类
 *
 * 提供实用工具功能，包括：
 * - 头颅获取功能（URL和玩家名）
 * - 配置热重载
 * - 完整的国际化支持
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class PowerToolsPlugin : BasePlugin() {

    // ===== 模块声明 =====
    private lateinit var commandRegistry: CommandRegistry

    override fun onPluginLoad() {

    }

    override fun onPluginEnable() {
        // 调用重载方法
        reloadPlugin()

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

    // ===== 配置管理 =====

    /**
     * 获取插件配置
     * 使用标准的touchWithMerge + parse模式
     */
    fun getPluginConfig(): PowerToolsConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<PowerToolsConfig>("config.yml")
    }

    /**
     * 获取核心配置
     * 使用推荐的getCoreConfig()方法模式
     */
    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()
}
