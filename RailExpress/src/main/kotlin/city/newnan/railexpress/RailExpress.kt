package city.newnan.railexpress

import co.aikar.commands.PaperCommandManager
import city.newnan.violet.config.ConfigManager
import city.newnan.violet.config.setListIfNull
import city.newnan.violet.i18n.LanguageManager
import city.newnan.violet.message.MessageManager
import me.lucko.helper.Events
import me.lucko.helper.config.ConfigurationNode
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
import java.util.*

/**
 * 游戏默认矿车极速为0.4，最高为1.5
 */
internal const val DEFAULT_SPEED = 0.4

class RailExpress : ExtendedJavaPlugin() {
    private val configManager: ConfigManager by lazy { ConfigManager(this) }
    private val languageManager: LanguageManager by lazy { LanguageManager(this) }
    internal val messageManager: MessageManager by lazy { MessageManager(this) }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }
    companion object {
        lateinit var INSTANCE: RailExpress
            private set
    }

    init { INSTANCE = this }

    override fun enable() {
        // 初始化ConfigManager
        configManager touch "config.yml"

        // 载入配置
        reload()

        // 初始化LanguageManager
        Locale("config").also {
            languageManager.register(it, "config.yml") setMajorLanguage it
        }

        // 初始化MessageManager
        messageManager setLanguageProvider languageManager
        messageManager setPlayerPrefix(messageManager.sprintf("\$msg.prefix$"))

        // 初始化CommandManager
        commandManager.run {
            usePerIssuerLocale(true, false)
            locales.loadYamlLanguageFile("config.yml", Locale("config"))
        }

        // 注册指令
        commandManager.registerCommand(RailExpressCommand)

        // 注册事件
        Events.subscribe(VehicleExitEvent::class.java, EventPriority.LOWEST)
            .filter(EventFilters.ignoreCancelled())
            .filter { event: VehicleExitEvent -> event.vehicle is Minecart }
            .filter { event: VehicleExitEvent ->
                railConfigMap.containsKey(
                    event.vehicle.world
                )
            }
            .filter { event: VehicleExitEvent ->
                event.vehicle.isEmpty
            }
            .handler { event: VehicleExitEvent ->
                (event.vehicle as Minecart).maxSpeed = DEFAULT_SPEED
            }
            .bindWith(this)
        Events.subscribe(VehicleMoveEvent::class.java, EventPriority.HIGHEST)
            .filter { event: VehicleMoveEvent -> event.vehicle is Minecart }
            .filter { event: VehicleMoveEvent ->
                railConfigMap.containsKey(
                    event.vehicle.world
                )
            }
            .filter { event: VehicleMoveEvent ->
                !event.vehicle.isEmpty
            }
            .handler { event: VehicleMoveEvent ->
                val curBlock = event.vehicle.location.block
                val material = curBlock.type
                val config = railConfigMap[event.vehicle.world]
                var flag = material == Material.POWERED_RAIL
                if (!flag && !config!!.powerRailOnly) {
                    flag =
                        material == Material.RAIL || material == Material.DETECTOR_RAIL || material == Material.ACTIVATOR_RAIL
                }
                if (flag && config?.allowNonPlayer != true) {
                    for (entity in event.vehicle.passengers) {
                        if (entity is Player) {
                            continue
                        }
                        flag = false
                        break
                    }
                }
                if (flag) {
                    // 看看铁轨下面的方块是什么，赋予相应的速度
                    val belowBlock = curBlock.getRelative(BlockFace.DOWN)
                    (event.vehicle as Minecart).maxSpeed = config!!.blockSpeedMap.getOrDefault(
                        belowBlock.type,
                        DEFAULT_SPEED
                    )
                } else (event.vehicle as Minecart).maxSpeed = DEFAULT_SPEED
            }
            .bindWith(this)
    }

    private val railConfigMap: MutableMap<World, RailConfig> = HashMap()
    internal fun reload() {
        railConfigMap.clear()
        configManager["config.yml"]?.getNode("config")?.let { node ->
            node.childrenList.forEach { config: ConfigurationNode ->
                val powerRailOnly = config.getNode("power-rail-only").getBoolean(true)
                val allowNonPlayer = config.getNode("allow-non-player").getBoolean(false)
                val railConfig = RailConfig(powerRailOnly, allowNonPlayer, config.getNode("block-type"))
                config.getNode("world").setListIfNull().getList { obj: Any -> obj.toString() }
                    .forEach { world -> Bukkit.getWorld(world)?.let { railConfigMap[it] = railConfig } }
            }
        }
    }
}