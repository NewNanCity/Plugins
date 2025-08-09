package city.newnan.railarea

import city.newnan.config.extensions.configManager
import city.newnan.core.base.BasePlugin
import city.newnan.i18n.extensions.setupLanguageManager
import city.newnan.railarea.commands.CommandRegistry
import city.newnan.railarea.config.RailAreaConfig
import city.newnan.railarea.i18n.LanguageKeys
import city.newnan.railarea.manager.AreaDetector
import city.newnan.railarea.manager.BoardManager
import city.newnan.railarea.manager.StationStorage
import city.newnan.railarea.manager.TrainManager
import java.util.*

/**
 * RailArea插件主类
 *
 * 实现地铁站机制的Minecraft插件，包括：
 * - 玩家所在站台检测（使用八叉树高效检测）
 * - 到站标题显示
 * - 列车铃声系统
 * - 实时报站（通过分数板）
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class RailAreaPlugin : BasePlugin() {

    companion object {
        lateinit var instance: RailAreaPlugin
            private set
    }

    // 管理器模块
    private lateinit var boardManager: BoardManager
    lateinit var stationStorage: StationStorage
        private set
    private lateinit var areaDetector: AreaDetector
    private lateinit var trainManager: TrainManager
    private lateinit var commandRegistry: CommandRegistry

    override fun onPluginEnable() {
        instance = this

        stationStorage = StationStorage(this)

        // 初始化管理器（按依赖顺序）
        areaDetector = AreaDetector(this, stationStorage)
        boardManager = BoardManager(this)
        trainManager = TrainManager(this, stationStorage, areaDetector, boardManager)

        // 调用重载方法
        reloadPlugin()
        
        // 初始化命令注册器（不可重载）
        commandRegistry = CommandRegistry(this)
    }

    override fun onPluginDisable() {

    }

    override fun reloadPlugin() {
        try {
            // 1. 清理配置缓存
            configManager.clearCache()

            // 2. 重新设置语言管理器
            setupLanguageManager(
                languageFiles = mapOf(
                    Locale.SIMPLIFIED_CHINESE to "lang/zh_CN.yml",
                    Locale.US to "lang/en_US.yml"
                ),
                majorLanguage = Locale.SIMPLIFIED_CHINESE,
                defaultLanguage = Locale.US
            )

            // 3. 重新加载站点数据
            stationStorage.load()

            // 4. 重载所有子模块
            super.reloadPlugin()

        } catch (e: Exception) {
            logger.error(LanguageKeys.Core.Plugin.RELOAD_FAILED, e)
            throw e
        }
    }

    /**
     * 获取插件配置
     */
    fun getPluginConfig(): RailAreaConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<RailAreaConfig>("config.yml")
    }

    override fun getCoreConfig() = getPluginConfig().getCoreConfig()
}
