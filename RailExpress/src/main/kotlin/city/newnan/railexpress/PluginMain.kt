package city.newnan.railexpress

import city.newnan.railexpress.config.ConfigFile
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.i18n.LanguageManager
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.Events
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.plugin.ExtendedJavaPlugin
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import java.util.*

/**
 * 游戏默认矿车极速为0.4，最高为1.5
 */
internal const val DEFAULT_SPEED = 0.4

class PluginMain : ExtendedJavaPlugin() {
    private val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.None)
        }
    }
    internal val messageManager: MessageManager by lazy { MessageManager(this) }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }
    companion object {
        lateinit var INSTANCE: PluginMain
            private set
    }

    init { INSTANCE = this }

    override fun enable() {
        // 初始化ConfigManager
        configManager touch "config.yml"

        // 载入配置
        reload()

        // 初始化MessageManager
        messageManager setPlayerPrefix "§7[§6牛腩小镇§7] §f"

        // 注册指令
        commandManager.enableUnstableAPI("help")
        commandManager.registerCommand(Commands)

        // 注册事件
        Events.subscribe(WorldLoadEvent::class.java, EventPriority.MONITOR)
            .filter { worldName2RailConfig.containsKey(it.world.name) }
            .handler { world2RailConfig[it.world] = worldName2RailConfig[it.world.name]!! }
            .bindWith(this)

        Events.subscribe(WorldUnloadEvent::class.java, EventPriority.MONITOR)
            .handler { world2RailConfig.remove(it.world) }
            .bindWith(this)

        Events.subscribe(VehicleExitEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.vehicle is Minecart }
            .filter { it.vehicle.isEmpty }
            .filter { world2RailConfig.containsKey(it.vehicle.world) }
            .handler { (it.vehicle as Minecart).maxSpeed = DEFAULT_SPEED }
            .bindWith(this)

        val allowedRailType = hashSetOf(
            Material.POWERED_RAIL,
            Material.RAIL,
            Material.DETECTOR_RAIL,
            Material.ACTIVATOR_RAIL
        )

        fun checkSpeed(vehicle: Minecart): Double {
            val curBlock = vehicle.location.block
            val config = world2RailConfig[vehicle.world] ?: return DEFAULT_SPEED
            if (config.powerRailOnly) {
                if (curBlock.type != Material.POWERED_RAIL) return DEFAULT_SPEED
            } else {
                if (!allowedRailType.contains(curBlock.type)) return DEFAULT_SPEED
            }
            if (!config.allowNonPlayer && !vehicle.passengers.any { it is Player }) return DEFAULT_SPEED
            // 看看铁轨下面的方块是什么，赋予相应的速度
            val blockType = curBlock.getRelative(BlockFace.DOWN).type
            if (blockType == Material.REDSTONE_BLOCK) return vehicle.maxSpeed
            return config.blockSpeedMap.getOrDefault(blockType, DEFAULT_SPEED)
        }

        Events.subscribe(VehicleMoveEvent::class.java, EventPriority.MONITOR)
            .filter { it.vehicle is Minecart }
            .filter { !it.vehicle.isEmpty }
            .filter { world2RailConfig.containsKey(it.vehicle.world) }
            .filter { it.from.blockX != it.to.blockX || it.from.blockZ != it.to.blockZ ||
                      it.from.blockY != it.to.blockY || it.from.world != it.to.world }
            .handler { event: VehicleMoveEvent ->
                (event.vehicle as Minecart).also { it.maxSpeed = checkSpeed(it) }
            }
            .bindWith(this)
    }

    private val world2RailConfig = mutableMapOf<World, RailConfig>()
    private val worldName2RailConfig = mutableMapOf<String, RailConfig>()
    internal fun reload() {
        configManager.cache?.clear()
        world2RailConfig.clear()
        configManager touch "config.yml"
        configManager.parse<ConfigFile>("config.yml").also {
            it.groups.forEach { worldGroup ->
                val config = RailConfig(worldGroup.powerRailOnly, worldGroup.allowNonPlayer, worldGroup.blockType)
                worldGroup.world.forEach { worldName ->
                    Bukkit.getWorld(worldName)?.let { w -> world2RailConfig[w] = config }
                    worldName2RailConfig[worldName] = config
                }
            }
        }
    }
}