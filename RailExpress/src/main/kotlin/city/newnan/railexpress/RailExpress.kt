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
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.event.vehicle.VehicleMoveEvent
import java.io.IOException
import java.util.*


class RailExpress : ExtendedJavaPlugin() {
    internal var configManager: ConfigManager? = null
    private var languageManager: LanguageManager? = null
    internal var messageManager: MessageManager? = null
    override fun load() {
        // 初始化ConfigManager
        configManager = ConfigManager(this)
        configManager!!.touch("config.yml")

        // 初始化LanguageManager
        try {
            val locale = Locale("config")
            languageManager = LanguageManager(this)
                .register(locale, "config.yml")
                .setMajorLanguage(locale)
        } catch (e: LanguageManager.FileNotFoundException) {
            e.printStackTrace()
            onDisable()
        } catch (e: ConfigManager.UnknownConfigFileFormatException) {
            e.printStackTrace()
            onDisable()
        } catch (e: IOException) {
            e.printStackTrace()
            onDisable()
        }

        // 初始化MessageManager
        messageManager = languageManager?.let {
            MessageManager(this)
                .setLanguageProvider(it)
        }
        messageManager?.sprintf("\$msg.prefix$")?.let { messageManager?.setPlayerPrefix(it) }
        instance = this
    }

    override fun enable() {
        // 初始化CommandManager - 不能在load()里面初始化！
        val commandManager = PaperCommandManager(this)
        commandManager.usePerIssuerLocale(true, false)
        try {
            commandManager.locales.loadYamlLanguageFile("config.yml", Locale("config"))
        } catch (e: IOException) {
            e.printStackTrace()
            onDisable()
        } catch (e: InvalidConfigurationException) {
            e.printStackTrace()
            onDisable()
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

        // 载入配置
        reload()
    }

    private val railConfigMap: MutableMap<World, RailConfig> = HashMap()
    internal fun reload() {
        railConfigMap.clear()
        try {
            configManager?.get("config.yml")?.getNode("config")?.let { node ->
                node.childrenList.forEach { config: ConfigurationNode ->
                    val powerRailOnly = config.getNode("power-rail-only").getBoolean(true)
                    val allowNonPlayer = config.getNode("allow-non-player").getBoolean(false)
                    val railConfig = RailConfig(powerRailOnly, allowNonPlayer, config.getNode("block-type"))
                    config.getNode("world").setListIfNull().getList { obj: Any -> obj.toString() }
                        .forEach { world -> Bukkit.getWorld(world)?.let { railConfigMap[it] = railConfig } }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            onDisable()
        } catch (e: ConfigManager.UnknownConfigFileFormatException) {
            e.printStackTrace()
            onDisable()
        }
    }

    companion object {
        var instance: RailExpress? = null
            private set

        /**
         * 游戏默认矿车极速为0.4，最高为1.5
         */
        internal const val DEFAULT_SPEED = 0.4
    }
}