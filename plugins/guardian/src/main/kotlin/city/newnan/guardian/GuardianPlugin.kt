package city.newnan.guardian

import city.newnan.config.extensions.configManager
import city.newnan.core.base.BasePlugin
import city.newnan.core.config.CorePluginConfig
import city.newnan.guardian.commands.CommandRegistry
import city.newnan.guardian.config.GuardianConfig
import city.newnan.guardian.i18n.LanguageKeys
import city.newnan.guardian.modules.JudgementalManager
import city.newnan.guardian.modules.PlayerDB
import city.newnan.guardian.modules.PlayerManager
import city.newnan.i18n.extensions.setupLanguageManager
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.permission.Permission
import java.util.*


class GuardianPlugin : BasePlugin() {
    companion object {
        lateinit var INSTANCE: GuardianPlugin
            private set
    }

    lateinit var permission: Permission
    lateinit var economy: Economy

    lateinit var judgementalManager: JudgementalManager
        private set
    lateinit var playerDB: PlayerDB
        private set
    lateinit var playerManager: PlayerManager
        private set

    override fun getCoreConfig(): CorePluginConfig {
        return getPluginConfig().getCoreConfig()
    }

    fun getPluginConfig(): GuardianConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<GuardianConfig>("config.yml")
    }

    override fun onPluginLoad() {
        INSTANCE = this
    }

    private lateinit var commandRegistry: CommandRegistry

    override fun onPluginEnable() {
        try {
            // 语言设置前使用英文日志
            logger.info("Guardian Plugin enabling...")

            if (!isPluginPresent("Vault")) throw Exception("Vault not found!")
            permission = getService(Permission::class.java) ?: throw Exception("Vault permission service not found!")
            economy = getService(Economy::class.java) ?: throw Exception("Vault economy service not found!")

            playerDB = PlayerDB(this)
            playerManager = PlayerManager(this)
            judgementalManager = JudgementalManager(this)

            // 调用重载方法处理可重载的功能
            reloadPlugin()

            // 初始化命令注册器（不可重载）
            commandRegistry = CommandRegistry(this)

            // 语言设置后使用i18n模板
            logger.info(LanguageKeys.Log.Info.PLUGIN_LOADED)

        } catch (e: Exception) {
            logger.error("Guardian Plugin enable failed", e)
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onPluginDisable() {
        // 所有绑定的资源会自动清理
    }

    override fun reloadPlugin() {
        try {
            // 语言设置前使用英文日志
            logger.info("Plugin reloading...")

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

            // 语言设置后使用i18n模板
            logger.info(LanguageKeys.Core.Plugin.RELOADED)
        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.INITIALIZATION_FAILED, e)
            throw e
        }
    }
}