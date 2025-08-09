package city.newnan.createarea

import city.newnan.config.extensions.configManager
import city.newnan.core.base.BasePlugin
import city.newnan.core.config.CorePluginConfig
import city.newnan.i18n.extensions.setupLanguageManager
import city.newnan.createarea.commands.CommandRegistry
import city.newnan.createarea.config.CreateAreaConfig
import city.newnan.createarea.i18n.LanguageKeys
import city.newnan.createarea.modules.CreateAreaManager
import java.util.*

/**
 * CreateArea插件主类
 *
 * 提供创造区域管理功能，包括：
 * - 创造区域的创建、删除、传送
 * - WorldEdit集成选择区域
 * - Dynmap集成显示区域
 * - Vault权限系统集成
 * - GUI界面管理
 * - 完整的国际化支持
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CreateAreaPlugin : BasePlugin() {

    // ===== 模块声明 =====
    private lateinit var createAreaManager: CreateAreaManager
    private lateinit var commandRegistry: CommandRegistry

    override fun onPluginLoad() {

    }

    override fun onPluginEnable() {
        // 调用重载方法
        reloadPlugin()

        // 初始化模块（按依赖顺序）
        createAreaManager = CreateAreaManager("CreateAreaManager", this)

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
    fun getPluginConfig(): CreateAreaConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<CreateAreaConfig>("config.yml")
    }

    /**
     * 获取核心配置
     * 使用推荐的getCoreConfig()方法模式
     */
    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()

    // ===== 模块访问器 =====

    /**
     * 获取创造区域管理器
     */
    fun getCreateAreaManager(): CreateAreaManager = createAreaManager
}
