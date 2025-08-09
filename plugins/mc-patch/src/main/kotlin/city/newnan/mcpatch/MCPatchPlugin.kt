package city.newnan.mcpatch

import city.newnan.config.extensions.configManager
import city.newnan.core.base.BasePlugin
import city.newnan.core.config.CorePluginConfig
import city.newnan.i18n.extensions.setupLanguageManager
import city.newnan.mcpatch.config.MCPatchConfig
import city.newnan.mcpatch.i18n.LanguageKeys
import city.newnan.mcpatch.modules.*
import java.util.*

/**
 * MCPatch 插件主类
 *
 * 全面的 Minecraft 服务器安全与稳定性保护插件。
 * 提供多层次的安全防护，包括：
 * - 反世界下载器保护
 * - 违禁物品检测和清理
 * - 崩服漏洞防护
 * - 反物品复制检测
 * - 性能保护和优化
 * - 网络安全防护
 * - 完整的安全日志系统
 *
 * 基于现代化的BasePlugin架构，提供完整的生命周期管理和资源管理。
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class MCPatchPlugin : BasePlugin() {

    companion object {
        lateinit var instance: MCPatchPlugin
            private set
    }

    // 使用 lateinit 声明所有安全模块，在 onPluginEnable 中初始化
    lateinit var antiWorldDownloadModule: AntiWorldDownloadModule
        private set
    lateinit var contrabandModule: ContrabandModule
        private set
    lateinit var antiCrashModule: AntiCrashModule
        private set
    lateinit var securityLoggerModule: SecurityLoggerModule
        private set

    override fun getCoreConfig(): CorePluginConfig {
        return getPluginConfig().getCoreConfig()
    }

    /**
     * 获取插件配置
     * 使用 touchWithMerge 确保配置文件完整性，支持配置迁移
     */
    fun getPluginConfig(): MCPatchConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<MCPatchConfig>("config.yml")
    }

    override fun onPluginLoad() {
        instance = this
    }

    override fun onPluginEnable() {
        try {
            // 按依赖顺序初始化所有安全模块
            // 1. 首先初始化日志系统（其他模块可能需要使用）
            securityLoggerModule = SecurityLoggerModule("SecurityLogger", this)
            
            // 2. 初始化核心安全模块
            antiWorldDownloadModule = AntiWorldDownloadModule("AntiWorldDownload", this)
            contrabandModule = ContrabandModule("Contraband", this)
            antiCrashModule = AntiCrashModule("AntiCrash", this)

            // 调用重载方法处理可重载的功能
            reloadPlugin()
        } catch (e: Exception) {
            logger.error("MCPatch Plugin enable failed", e)
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
        logger.info(LanguageKeys.Core.Plugin.DISABLED)
        // 所有绑定的资源会自动清理
    }
}
