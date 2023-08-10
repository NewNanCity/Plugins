package city.newnan.railarea

import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.plugin.ExtendedJavaPlugin

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

    override fun enable() {
        reload()
        commandManager.enableUnstableAPI("help")
        commandManager.registerCommand(Commands)
        messageManager setPlayerPrefix "§7[§6牛腩小镇§7] §f"
    }

    override fun disable() {
        commandManager.unregisterCommands()
    }

    fun reload() {
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
        val areasWorlds = configManager.parse<AreasWorlds>("areas.yml")
        for ((worldName, areas) in areasWorlds) {
            val world = Bukkit.getWorld(worldName) ?: continue
            val octree = getWorldOctree(world) ?: continue
            areaRangeMap[world] = mutableMapOf()
            areas.map { it.value.toRailArea(it.key, world) }.forEach {
                areaNameMap[it.name] = it
                areaRangeMap[it.world]!![it.range3D] = it
                octree.insert(it.range3D)
            }
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
    }

    fun getPlayerInput(player: Player, block: (String) -> Boolean) {
        var done = false
        Events.subscribe(AsyncPlayerChatEvent::class.java, EventPriority.HIGHEST)
            .expireIf { done }
            .filter { it.player == player }
            .handler {
                done = block(it.message)
                it.isCancelled = true
            }
            .bindWith(this)
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

    fun eachAreas(block: (RailArea) -> Unit) {
        areaNameMap.values.forEach(block)
    }
}
