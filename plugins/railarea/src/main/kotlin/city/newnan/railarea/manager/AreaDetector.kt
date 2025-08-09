package city.newnan.railarea.manager

import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.core.utils.EventEmitter
import city.newnan.railarea.RailAreaPlugin
import city.newnan.railarea.config.RailArea
import city.newnan.railarea.config.WorldSizeConfig
import city.newnan.railarea.spatial.Octree
import city.newnan.railarea.spatial.Point3D
import city.newnan.railarea.spatial.Range3D
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityTeleportEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.vehicle.VehicleCreateEvent
import org.bukkit.event.vehicle.VehicleDestroyEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import java.util.concurrent.ConcurrentHashMap

const val MIN_Y = -64
const val MAX_Y = 320

class AreaDetector(
    val plugin: RailAreaPlugin,
    private val stationStorage: StationStorage
) : BaseModule("AreaDetector", plugin) {
    // 世界八叉树映射
    private val worldOctrees = ConcurrentHashMap<World, Octree<Int>>()

    // 玩家当前所在区域
    private val playerNotOnMinecartInArea = ConcurrentHashMap<Player, Int>()

    // 矿车当前所在区域
    private val minecartInArea = ConcurrentHashMap<Minecart, Int>()

    val onPlayerNotOnMinecartEnterArea = EventEmitter<Pair<Player, Int>>()
    val onPlayerNotOnMinecartExitArea = EventEmitter<Pair<Player, Int>>()
    val onMinecartEnterArea = EventEmitter<Pair<Minecart, Int>>()
    val onMinecartExitArea = EventEmitter<Pair<Minecart, Int>>()
    val onMinecartUpdate = EventEmitter<Pair<Minecart, Int>>()

    init { init() }

    override fun onInit() {
        // 世界加载事件
        subscribeEvent<WorldLoadEvent> {
            priority(EventPriority.MONITOR)
            handler { handleWorldLoad(it.world) }
        }

        // 世界卸载事件
        subscribeEvent<WorldUnloadEvent> {
            priority(EventPriority.MONITOR)
            handler { handleWorldUnload(it.world) }
        }

        stationStorage.onRailAreaReloaded.addListener { handleAreasReload() }
        stationStorage.onRailAreaRemoved.addListener { handleAreaRemoved(it) }
        stationStorage.onRailAreaAdded.addListener { handleAreaAdded(it) }
        stationStorage.onRailAreaUpdated.addListener { handleAreaUpdated(it.first, it.second) }

        // 接下来是一系列检测移动的事件

        // 玩家移动事件
        subscribeEvent<PlayerMoveEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            filter { it.from.blockX != it.to.blockX || it.from.blockY != it.to.blockY || it.from.blockZ != it.to.blockZ }
            handler { handlePlayerLocationUpdate(it.player, it.to) }
        }

        // 玩家传送事件
        subscribeEvent<PlayerTeleportEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            handler { handlePlayerLocationUpdate(it.player, it.to) }
        }

        // 玩家切换世界事件
        subscribeEvent<PlayerChangedWorldEvent> {
            priority(EventPriority.MONITOR)
            handler { handlePlayerLocationUpdate(it.player, it.player.location) }
        }

        // 玩家加入事件
        subscribeEvent<PlayerJoinEvent> {
            priority(EventPriority.MONITOR)
            handler { handlePlayerLocationUpdate(it.player, it.player.location) }
        }

        // 玩家退出事件
        subscribeEvent<PlayerQuitEvent> {
            priority(EventPriority.MONITOR)
            handler { handlePlayerLocationUpdate(it.player, null) }
        }

        // 玩家死亡事件
        subscribeEvent<PlayerDeathEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            handler { handlePlayerLocationUpdate(it.player, null) }
        }

        // 矿车移动事件
        subscribeEvent<VehicleMoveEvent> {
            priority(EventPriority.HIGHEST)
            filter { it.vehicle is Minecart }
            filter { it.from.blockX != it.to.blockX || it.from.blockY != it.to.blockY || it.from.blockZ != it.to.blockZ }
            handler { handleMinecartLocationUpdate(it.vehicle as Minecart, it.to) }
        }

        // 矿车传送事件
        subscribeEvent<EntityTeleportEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            filter { it.entity is Minecart }
            filter { it.to != null && it.from.blockX != it.to!!.blockX || it.from.blockY != it.to!!.blockY || it.from.blockZ != it.to!!.blockZ }
            handler { handleMinecartLocationUpdate(it.entity as Minecart, it.to) }
        }

        // 矿车创建事件
        subscribeEvent<VehicleCreateEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            filter { it.vehicle is Minecart }
            handler { handleMinecartLocationUpdate(it.vehicle as Minecart, it.vehicle.location) }
        }

        // 矿车销毁事件
        subscribeEvent<VehicleDestroyEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            filter { it.vehicle is Minecart }
            handler { handleMinecartLocationUpdate(it.vehicle as Minecart, null) }
        }

        // 矿车上车事件
        subscribeEvent<VehicleEnterEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            filter { it.vehicle is Minecart && it.entered is Player }
            handler {
                val areaId = minecartInArea[it.vehicle] ?: return@handler
                playerNotOnMinecartInArea.remove(it.entered as Player)
                onPlayerNotOnMinecartExitArea.emit(it.entered as Player to areaId)
                onMinecartUpdate.emit(it.vehicle as Minecart to areaId)
            }
        }

        // 矿车下车事件
        subscribeEvent<VehicleExitEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isCancelled }
            filter { it.vehicle is Minecart && it.exited is Player }
            handler {
                val areaId = minecartInArea[it.vehicle] ?: return@handler
                playerNotOnMinecartInArea[it.exited as Player] = areaId
                onMinecartUpdate.emit(it.vehicle as Minecart to areaId)
                onPlayerNotOnMinecartEnterArea.emit(it.exited as Player to areaId)
            }
        }

        // 区块加载事件
        subscribeEvent<ChunkLoadEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.isNewChunk }
            handler { handleChunkLoad(it) }
        }
    }

    // ------------------- 处理函数 -------------------
    private fun handleAreasReload() {
        worldOctrees.keys.toList().forEach {
            handleWorldUnload(it)
            handleWorldLoad(it)
        }
    }

    private fun handleAreaRemoved(area: RailArea) {
        val world = plugin.server.getWorld(area.world) ?: return
        val octree = worldOctrees[world] ?: return
        octree.remove(area.range3D)
        minecartInArea.entries.removeAll { (minecart, areaId) ->
            if (areaId == area.id) {
                onMinecartExitArea.emit(minecart to area.id)
                true
            } else {
                false
            }
        }
        playerNotOnMinecartInArea.entries.removeAll { (player, areaId) ->
            if (areaId == area.id) {
                onPlayerNotOnMinecartExitArea.emit(player to area.id)
                true
            } else {
                false
            }
        }
    }

    private fun handleAreaAdded(area: RailArea) {
        val world = plugin.server.getWorld(area.world) ?: return
        val octree = worldOctrees[world] ?: return
        octree.insert(area.range3D, area.id)
        // 检查玩家和矿车
        world.players.forEach { player ->
            if (area.range3D.contains(player.location)) {
                playerNotOnMinecartInArea[player] = area.id
                onPlayerNotOnMinecartEnterArea.emit(player to area.id)
                if (player.isInsideVehicle && player.vehicle is Minecart) {
                    val minecart = player.vehicle as Minecart
                    minecartInArea[minecart] = area.id
                    onMinecartEnterArea.emit(minecart to area.id)
                }
            }
        }
        world.getNearbyEntities(area.range3D.toBoundingBox()).forEach { entity ->
            if (entity is Minecart) {
                if (area.range3D.contains(entity.location)) {
                    minecartInArea[entity] = area.id
                    onMinecartEnterArea.emit(entity to area.id)
                }
            }
        }
    }

    private fun handleAreaUpdated(oldArea: RailArea, newArea: RailArea) {
        if (oldArea.world != newArea.world || oldArea.range3D != newArea.range3D) {
            handleAreaRemoved(oldArea)
            handleAreaAdded(newArea)
        }
    }

    private fun handleWorldLoad(world: World) {
        // 检查该世界是否有区域配置
        val areaConfig = plugin.getPluginConfig().worldSize[world.name] ?: run {
            return
        }

        // 创建八叉树
        val octree = createOctree(areaConfig)

        // 插入区域
        stationStorage.getAreasInWorld(world.name).filter { !it.softDeleted }.forEach {
            octree.insert(it.range3D, it.id)
        }

        // 刚加载的世界是没有玩家的，所以不需要检查玩家位置
    }

    private fun handleWorldUnload(world: World) {
        worldOctrees.remove(world)?.close()
        playerNotOnMinecartInArea.entries.removeAll { (player, areaId) ->
            val station = stationStorage.getAreaById(areaId) ?: return@removeAll true
            if (station.world == world.name) {
                onPlayerNotOnMinecartExitArea.emit(player to station.id)
                true
            }
            false
        }
        minecartInArea.entries.removeAll { (minecart, areaId) ->
            val station = stationStorage.getAreaById(areaId) ?: return@removeAll true
            if (station.world == world.name) {
                onMinecartExitArea.emit(minecart to station.id)
                true
            }
            false
        }
    }

    // ------------------- 移动检测 -------------------
    private fun handlePlayerLocationUpdate(player: Player, location: Location?) {
        // 如果玩家在车上，不处理，交给矿车检测函数
        if (player.isInsideVehicle && player.vehicle is Minecart) return

        val oldAreaId = playerNotOnMinecartInArea[player]
        val newAreaId = if (location != null) {
            queryArea(oldAreaId, Point3D(location), location.world)
        } else {
            null
        }
        if (newAreaId != null) {
            playerNotOnMinecartInArea[player] = newAreaId
        } else {
            playerNotOnMinecartInArea.remove(player)
        }
        if (oldAreaId != newAreaId) {
            if (oldAreaId != null) {
                onPlayerNotOnMinecartExitArea.emit(player to oldAreaId)
            }
            if (newAreaId != null) {
                onPlayerNotOnMinecartEnterArea.emit(player to newAreaId)
            }
        }
    }

    private fun handleMinecartLocationUpdate(minecart: Minecart, to: Location?) {
        val oldAreaId = minecartInArea[minecart]
        val newAreaId = if (to != null) {
            queryArea(oldAreaId, Point3D(to), to.world)
        } else {
            null
        }
        if (newAreaId != null) {
            minecartInArea[minecart] = newAreaId
        } else {
            minecartInArea.remove(minecart)
        }
        if (oldAreaId != newAreaId) {
            if (oldAreaId != null) {
                onMinecartExitArea.emit(minecart to oldAreaId)
            }
            if (newAreaId != null) {
                onMinecartEnterArea.emit(minecart to newAreaId)
            }
        } else if (newAreaId != null) {
            onMinecartUpdate.emit(minecart to newAreaId)
        }
    }

    private fun handleChunkLoad(event: ChunkLoadEvent) {
        val world = event.world
        val octree = worldOctrees[world] ?: return
        // 检查区块是否与区域相交
        if (octree.queryIntersecting(
                Range3D(
                    event.chunk.x * 16, MIN_Y, event.chunk.z * 16,
                    event.chunk.x * 16 + 15, MAX_Y, event.chunk.z * 16 + 15
                )
            ).isEmpty()
        ) return
        event.chunk.entities.forEach { entity ->
            if (entity is Minecart) {
                handleMinecartLocationUpdate(entity, entity.location)
            }
        }
    }

    // ------------------- 逻辑函数 -------------------
    private fun queryArea(oldAreaId: Int?, point: Point3D, world: World): Int? {
        if (oldAreaId != null) {
            stationStorage.getAreaById(oldAreaId)?.let {
                if (it.world == world.name && it.range3D.contains(point)) return oldAreaId
            }
        }
        return worldOctrees[world]?.firstRange(point)?.second
    }

    fun queryArea(location: Location): Int? {
        val octree = worldOctrees[location.world] ?: return null
        return octree.firstRange(Point3D(location))?.second
    }

    fun getPlayersNotOnMinecart() = playerNotOnMinecartInArea.entries.map { it.key to it.value }

    fun getMinecarts() = minecartInArea.entries.map { it.key to it.value }

    fun getMinecartArea(minecart: Minecart) = minecartInArea[minecart]

    fun getPlayersArea(player: Player) = playerNotOnMinecartInArea[player] ?: run {
        if (player.isInsideVehicle && player.vehicle is Minecart) {
            minecartInArea[player.vehicle as Minecart]
        } else {
            null
        }
    }

    // ------------------- 工具函数 --------------------
    private fun createOctree(worldSize: WorldSizeConfig): Octree<Int> {
        val minX = minOf(worldSize.x1, worldSize.x2)
        val maxX = maxOf(worldSize.x1, worldSize.x2)
        val minZ = minOf(worldSize.z1, worldSize.z2)
        val maxZ = maxOf(worldSize.z1, worldSize.z2)

        val boundary = Range3D(minX, MIN_Y, minZ, maxX, MAX_Y, maxZ)
        val octree = Octree<Int>(boundary, maxDepth = 10, maxItems = 12)
        return octree
    }
}