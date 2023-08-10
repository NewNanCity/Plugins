package city.newnan.railarea

import city.newnan.railarea.config.*
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.plugin.ExtendedJavaPlugin
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent

class PluginMain : ExtendedJavaPlugin() {
    companion object {
        lateinit var INSTANCE: PluginMain
            private set
    }
    init { INSTANCE = this }
    private val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.None)
        }
    }
    val messageManager: MessageManager by lazy { MessageManager(this) }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }
    private val worldOctrees: MutableMap<World, Octree> = mutableMapOf()
    private val worldSizes: MutableMap<String, WorldSize> = mutableMapOf()
    private val areaNameMap: MutableMap<String, RailArea> = mutableMapOf()
    private val areaRangeMap: MutableMap<World, MutableMap<Range3D, RailArea>> = mutableMapOf()
    private val inAreaPlayerMap: MutableMap<Player, RailArea> = mutableMapOf()

    private fun getWorldOctree(world: World): Octree? {
        if (!worldOctrees.containsKey(world)) {
            val worldSize = worldSizes[world.name] ?: worldSizes["default"] ?: return null
            val minX = minOf(worldSize.x1, worldSize.x2)
            val minZ = minOf(worldSize.z1, worldSize.z2)
            val maxX = maxOf(worldSize.x1, worldSize.x2)
            val maxZ = maxOf(worldSize.z1, worldSize.z2)
            val worldRange = Range3D(minX, 0, minZ, maxX, 255, maxZ)
            val octree = Octree(worldRange)
            worldOctrees[world] = octree
        }
        return worldOctrees[world]
    }

    override fun enable() {
        reload()
        commandManager.enableUnstableAPI("help")
        commandManager.registerCommand(Commands)
        messageManager setPlayerPrefix "§7[§6牛腩小镇§7] §f"

        Schedulers.async().runRepeating({ _ ->
            inAreaPlayerMap.forEach { (player, area) ->
                player.sendTitle(area.title, area.subTitle, 0, 20, 10)
                if (area.actionBar != null)
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(area.actionBar))
            }
        }, 20, 20).bindWith(this)

        Events.subscribe(PlayerMoveEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .filter(EventFilters.ignoreSameBlock())
            .handler {
                val player = it.player
                val world = player.world
                val oldArea = inAreaPlayerMap[player]
                val point3D = Point3D(player.location.blockX, player.location.blockY, player.location.blockZ)
                if (oldArea != null) {
                    if (oldArea.world == world && oldArea.range3D.contains(point3D)) return@handler
                    else inAreaPlayerMap.remove(player)
                }
                val octree = worldOctrees[world] ?: return@handler
                val range = octree.firstRange(point3D) ?: return@handler
                val area = areaRangeMap[world]!![range] ?: return@handler
                inAreaPlayerMap[player] = area
                player.sendTitle(area.title, area.subTitle, 3, 20, 0)
                if (area.actionBar != null)
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(area.actionBar))
            }
            .bindWith(this)

        Events.subscribe(PlayerChangedWorldEvent::class.java, EventPriority.MONITOR)
            .filter { inAreaPlayerMap.containsKey(it.player) }
            .handler { inAreaPlayerMap.remove(it.player) }

        Events.subscribe(WorldLoadEvent::class.java)
            .handler {  event ->
                configManager touch "areas.yml"
                configManager.parse<AreasWorlds>("areas.yml").also { config ->
                    val world = event.world
                    if (!config.containsKey(world.name)) return@handler
                    val octree = getWorldOctree(world) ?: return@handler
                    areaRangeMap[world] = mutableMapOf()
                    config[world.name]!!.map { it.value.toRailArea(it.key, world) }.onEach {
                        areaNameMap[it.name] = it
                        areaRangeMap[it.world]!![it.range3D] = it
                        octree.insert(it.range3D)
                    }.also { messageManager.info("Loaded ${it.size} areas in world \"${world.name}\"") }
                }
            }

        Events.subscribe(WorldUnloadEvent::class.java)
            .handler { event ->
                worldOctrees.remove(event.world)
                areaRangeMap.remove(event.world)
                    ?.onEach { areaNameMap.remove(it.value.name) }
                    ?.also { messageManager.info("Unloaded ${it.size} areas in world \"${event.world.name}\"") }
            }
    }

    override fun disable() {
        commandManager.unregisterCommands()
    }

    fun reload() {
        configManager.cache?.clear()
        // config.yml
        configManager touch "config.yml"
        val config = configManager.parse<ConfigFile>("config.yml")
        worldSizes.clear()
        worldSizes.putAll(config.worldSize)

        // areas.yml
        configManager touch "areas.yml"
        worldOctrees.clear()
        areaNameMap.clear()
        areaRangeMap.clear()
        inAreaPlayerMap.clear()
        val areasWorlds = configManager.parse<AreasWorlds>("areas.yml")
        for ((worldName, areas) in areasWorlds) {
            val world = Bukkit.getWorld(worldName) ?: continue
            val octree = getWorldOctree(world) ?: continue
            areaRangeMap[world] = mutableMapOf()
            areas.map { it.value.toRailArea(it.key, world) }.onEach {
                areaNameMap[it.name] = it
                areaRangeMap[it.world]!![it.range3D] = it
                octree.insert(it.range3D)
            }.also { messageManager.info("Loaded ${it.size} areas in world \"$worldName\"") }
        }
    }

    private fun save() {
        configManager touch "areas.yml"
        val areasWorlds = AreasWorlds()
        for ((world, areas) in areaRangeMap) {
            val areasMap = AreasWorld()
            for ((_, area) in areas) {
                areasMap[area.name] = RailAreaConfig.valueOf(area)
            }
            areasWorlds[world.name] = areasMap
        }
        configManager.save(areasWorlds, "areas.yml")
    }

    fun addArea(area: RailArea) {
        areaNameMap[area.name] = area
        if (!areaRangeMap.containsKey(area.world))
            areaRangeMap[area.world] = mutableMapOf()
        areaRangeMap[area.world]!![area.range3D] = area
        getWorldOctree(area.world)!!.insert(area.range3D)
        save()
    }

    fun removeArea(name: String) {
        val area = areaNameMap[name] ?: return
        areaNameMap.remove(name)
        areaRangeMap[area.world]?.remove(area.range3D)
        worldOctrees[area.world]?.remove(area.range3D)
        if (areaRangeMap[area.world]?.isEmpty() == true) {
            areaRangeMap.remove(area.world)
            worldOctrees.remove(area.world)
        }
        save()
    }

    fun updateArea(newArea: RailArea) {
        val oldArea = areaNameMap[newArea.name] ?: return
        if (oldArea.world != newArea.world || oldArea.range3D != newArea.range3D) {
            areaNameMap.remove(newArea.name)
            areaRangeMap[oldArea.world]?.remove(oldArea.range3D)
            worldOctrees[oldArea.world]?.remove(oldArea.range3D)
            areaNameMap[newArea.name] = newArea
            if (!areaRangeMap.containsKey(newArea.world))
                areaRangeMap[newArea.world] = mutableMapOf()
            areaRangeMap[newArea.world]!![newArea.range3D] = newArea
            getWorldOctree(newArea.world)!!.insert(newArea.range3D)
            if (areaRangeMap[oldArea.world]?.isEmpty() == true) {
                areaRangeMap.remove(oldArea.world)
                worldOctrees.remove(oldArea.world)
            }
        } else {
            areaNameMap[newArea.name] = newArea
            areaRangeMap[newArea.world]!![newArea.range3D] = newArea
        }
        save()
    }

    fun getArea(name: String): RailArea? {
        return areaNameMap[name]
    }

    fun getArea(world: World, range3D: Range3D): RailArea? {
        return areaRangeMap[world]?.get(range3D)
    }

    fun eachAreas(block: (RailArea) -> Unit) {
        areaNameMap.values.forEach(block)
    }
}
