package city.newnan.railexpress

import city.newnan.core.base.BasePlugin
import city.newnan.core.config.CorePluginConfig
import city.newnan.config.extensions.configManager
import city.newnan.core.event.subscribeEvent
import city.newnan.i18n.extensions.setupLanguageManager
import city.newnan.railexpress.config.RailExpressConfig
import city.newnan.railexpress.command.CommandRegistry
import city.newnan.railexpress.config.WorldGroupConfig
import city.newnan.railexpress.i18n.LanguageKeys
import java.util.*
import org.bukkit.Material
import org.bukkit.entity.Minecart
import org.bukkit.event.EventPriority
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.event.vehicle.VehicleMoveEvent

/**
 * RailExpress插件主类
 *
 * 让矿车在不同方块上拥有不同的最大速度
 * 基于项目标准架构重构，提供现代化的配置管理和事件处理
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class RailExpressPlugin : BasePlugin() {

    companion object {
        lateinit var instance: RailExpressPlugin
            private set

        /**
         * 游戏默认矿车极速
         */
        const val DEFAULT_SPEED = 0.4
    }

    // 世界配置映射
    private val worldConfigs = mutableMapOf<String, WorldGroupConfig>()

    override fun getCoreConfig(): CorePluginConfig = getPluginConfig().getCoreConfig()

    /**
     * 获取插件配置
     */
    fun getPluginConfig(): RailExpressConfig {
        configManager.touchWithMerge("config.yml", createBackup = true)
        return configManager.parse<RailExpressConfig>("config.yml")
    }

    /**
     * 获取插件配置（类型安全的访问方法）
     */
    fun getRailExpressConfig(): RailExpressConfig {
        return getPluginConfig()
    }

    /**
     * 获取世界配置
     */
    fun getWorldConfig(worldName: String): WorldGroupConfig? {
        return worldConfigs[worldName]
    }

    // 命令注册器
    private lateinit var commandRegistry: CommandRegistry

    override fun onPluginEnable() {
        instance = this

        // 注册命令（不可重载的功能）
        commandRegistry = CommandRegistry(this)

        // 注册事件监听器（不可重载的功能）
        registerEventListeners()

        // 调用重载方法处理可重载的功能
        reloadPlugin()
    }

    override fun onPluginDisable() {
        // 清理配置
        worldConfigs.clear()
    }

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

            // 3. 重新加载配置
            loadConfiguration()

            // 4. 重载所有BaseModule子模块（必需）
            super.reloadPlugin()
        } catch (e: Exception) {
            logger.error(LanguageKeys.Core.Config.RELOAD_FAILED, e)
            throw e
        }
    }

    /**
     * 加载配置
     */
    private fun loadConfiguration() {
        val config = getRailExpressConfig()

        // 清理旧配置
        worldConfigs.clear()

        // 加载世界组配置
        config.groups.forEach { group ->
            group.worlds.forEach { worldName ->
                worldConfigs[worldName] = group
            }
            logger.info("Loaded world group: ${group.worlds.joinToString(", ")} allowNonPlayer: ${group.allowNonPlayer} powerRailOnly: ${group.powerRailOnly} blockSpeedMap: ${group.blockSpeedMap.size} items")
        }

        logger.debug(LanguageKeys.Core.Config.WORLDS_LOADED, worldConfigs.size)
    }

    /**
     * 注册事件监听器
     */
    private fun registerEventListeners() {
        // 注册矿车退出事件 - 重置最大速度
        subscribeEvent<VehicleExitEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            filter { it.vehicle is Minecart }
            filter { it.vehicle.isEmpty }
            filter { worldConfigs.containsKey(it.vehicle.world.name) }
            handler { (it.vehicle as Minecart).maxSpeed = DEFAULT_SPEED }
        }

        // 注册矿车移动事件 - 设置最大速度
        subscribeEvent<VehicleMoveEvent> {
            priority(EventPriority.MONITOR)
            filter { it.vehicle is Minecart }
            filter { !it.vehicle.isEmpty }
            filter { worldConfigs.containsKey(it.vehicle.world.name) }
            filter { it.from.blockX != it.to.blockX || it.from.blockY != it.to.blockY || it.from.blockZ != it.to.blockZ || it.from.world != it.to.world }
            handler { (it.vehicle as Minecart).maxSpeed = calculateSpeed(it.vehicle as Minecart) }
        }

        logger.debug(LanguageKeys.Core.Plugin.EVENTS_REGISTERED)
    }

    private val allowedRailTypes = setOf(
        Material.POWERED_RAIL,
        Material.RAIL,
        Material.DETECTOR_RAIL,
        Material.ACTIVATOR_RAIL
    )

    /**
     * 计算矿车在当前位置的速度
     */
    fun calculateSpeed(minecart: Minecart): Double {
        val worldConfig = getWorldConfig(minecart.world.name) ?: return DEFAULT_SPEED
        val currentBlock = minecart.location.block

        // 检查是否只允许动力铁轨
        if (worldConfig.powerRailOnly) {
            if (currentBlock.type != Material.POWERED_RAIL) {
                return DEFAULT_SPEED
            }
        } else {
            // 检查是否在铁轨上
            if (!allowedRailTypes.contains(currentBlock.type)) {
                return DEFAULT_SPEED
            }
        }

        // 检查是否允许非玩家实体
        if (!worldConfig.allowNonPlayer && minecart.passengers.none { it is org.bukkit.entity.Player }) {
            return DEFAULT_SPEED
        }

        // 检查铁轨下方的方块类型
        val blockBelow = currentBlock.getRelative(org.bukkit.block.BlockFace.DOWN)

        // 红石块保持当前最大速度
        if (blockBelow.type == Material.REDSTONE_BLOCK) {
            return minecart.maxSpeed
        }

        // 根据方块类型返回对应速度
        return worldConfig.blockSpeedMap[blockBelow.type] ?: DEFAULT_SPEED
    }
}
