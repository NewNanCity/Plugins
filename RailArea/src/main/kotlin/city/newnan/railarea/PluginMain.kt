package city.newnan.railarea

import city.newnan.railarea.config.*
import city.newnan.railarea.octree.Octree
import city.newnan.railarea.octree.Point3D
import city.newnan.railarea.octree.Range3D
import city.newnan.railarea.utils.RailTitleMode
import city.newnan.railarea.utils.sendTitle
import city.newnan.railarea.utils.showBoard
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.gui.GuiManager
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.plugin.ExtendedJavaPlugin
import me.lucko.helper.terminable.Terminable
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.*
import org.bukkit.event.vehicle.VehicleDestroyEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import org.bukkit.event.vehicle.VehicleMoveEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import org.bukkit.util.Vector
import java.io.File
import java.util.Locale

data class Note(val sound: Sound, val pitch: Float, val delta: Long, val volume: Float)

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
    val guiManager: GuiManager by lazy { GuiManager(this) }
    val messageManager: MessageManager by lazy { MessageManager(this) }

    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }
    private val worldOctrees = mutableMapOf<World, Octree>()
    private val worldSizes = mutableMapOf<String, WorldSize>()
    private val areaRangeMap = mutableMapOf<World, MutableMap<Range3D, RailArea>>()
    private val inAreaPlayerMap = mutableMapOf<Player, RailArea>()
    private val inAreaMinecartMap = mutableMapOf<Minecart, RailArea>()
    private val waitingMinecartMap = mutableMapOf<Minecart, Terminable>()
    val hasBoardPlayers = mutableSetOf<Player>()
    private var WAITING_COUNT_DOWN: Int = 20
    private var RUN_WARNING_THRESHOLD: Int = 12
    private var DEFAULT_SPEED: Double = 1.0
    private var START_WARNING_SOUND = Sound.BLOCK_NOTE_BLOCK_BIT
    private val arriveMusic = mutableListOf<Note>()
    private val startMusic = mutableListOf<Note>()
    val stations = mutableMapOf<String, Station>()
    val lines = mutableMapOf<String, RailLine>()
    val lineStationAreas = mutableMapOf<Pair<Station, RailLine>, MutableSet<RailArea>>()
    val unknownStation = Station(0, "§8未知§r")
    val unknownLine = RailLine(0, "§8未知§r", mutableListOf(), Color.WHITE, false, Material.CHEST,
        leftReturn = false,
        rightReturn = false
    )
    var nextStationId = 1
    var nextLineId = 1

    private fun onAreaEnter(player: Player, area: RailArea) {
        if (player.isInsideVehicle && player.vehicle is Minecart) {
            player.sendTitle(area, RailTitleMode.ARRIVE, 5, 200, 0)
        } else {
            player.sendTitle(area, RailTitleMode.UNDER_BOARD, 5, 40, 0)
            if (area.nextStation == null) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("§c本站为终点站, 请到对侧站台乘车"))
            } else {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, *ComponentBuilder()
                    .append("请右击铁轨, 自动搭乘 ").color(net.md_5.bungee.api.ChatColor.RED)
                    .append(area.line.name).bold(true).color(net.md_5.bungee.api.ChatColor.of(area.line.color.toHexString()))
                    .append(" 列车").color(net.md_5.bungee.api.ChatColor.RED).bold(false).create()
                )
            }
        }
    }

    private fun onAreaExit(player: Player, area: RailArea) {}

    private fun onAreaStay(player: Player, area: RailArea) {
        if (player.isInsideVehicle && player.vehicle is Minecart) {
            if (waitingMinecartMap.containsKey(player.vehicle as Minecart)) {
                player.sendTitle(area, RailTitleMode.ARRIVE, 0, 40, 0)
            }
            // Or do noting
        } else {
            player.sendTitle(area, RailTitleMode.UNDER_BOARD, 0, 40, 0)
            if (area.nextStation == null) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("§c本站为终点站, 请到对侧站台乘车"))
            } else {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, *ComponentBuilder()
                    .append("请右击铁轨, 自动搭乘 ").color(net.md_5.bungee.api.ChatColor.RED)
                    .append(area.line.name).bold(true).color(net.md_5.bungee.api.ChatColor.of(area.line.color.toHexString()))
                    .append(" 列车").color(net.md_5.bungee.api.ChatColor.RED).bold(false).create()
                )
            }
        }
    }

    override fun enable() {
        reload()
        commandManager.enableUnstableAPI("help")
        commandManager.registerCommand(Commands)
        commandManager.locales.setDefaultLocale(Locale.SIMPLIFIED_CHINESE)
        messageManager setPlayerPrefix "§7[§6牛腩轨交§7] §f"

        Schedulers.async().runRepeating({ _ ->
            inAreaPlayerMap.forEach { (player, area) -> onAreaStay(player, area) }
            inAreaMinecartMap.forEach { (minecart, area) ->
                if (minecart.isEmpty) {
                    inAreaMinecartMap.remove(minecart)
                    waitingMinecartMap.remove(minecart)?.close()
                    minecart.remove()
                    return@forEach
                }
                if (waitingMinecartMap.containsKey(minecart)) return@forEach
                if (minecart.velocity.lengthSquared() < 5e-2) waitMinecart(minecart, area)
            }
        }, 20, 20).bindWith(this)

        server.onlinePlayers.forEach { checkPlayer(it) }

        // 用户移动
        Events.subscribe(PlayerTeleportEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .handler { checkPlayer(it.player, it.to) }
            .bindWith(this)
        Events.subscribe(PlayerMoveEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .filter(EventFilters.ignoreSameBlock())
            .handler { checkPlayer(it.player, it.to) }
            .bindWith(this)
        Events.subscribe(PlayerJoinEvent::class.java, EventPriority.MONITOR)
            .handler { checkPlayer(it.player) }
            .bindWith(this)
        Events.subscribe(PlayerChangedWorldEvent::class.java, EventPriority.MONITOR)
            .handler { checkPlayer(it.player) }
            .bindWith(this)
        Events.subscribe(PlayerQuitEvent::class.java, EventPriority.MONITOR)
            .handler {
                if (hasBoardPlayers.remove(it.player)) {
                    it.player.scoreboard = Bukkit.getScoreboardManager()!!.mainScoreboard
                }
                inAreaPlayerMap.remove(it.player)
            }
            .bindWith(this)
        Events.subscribe(PlayerInteractEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .filter { inAreaPlayerMap.containsKey(it.player) }
            .filter { it.action == Action.RIGHT_CLICK_BLOCK }
            .filter { it.clickedBlock?.type == Material.RAIL }
            .handler {
                // summon a minecart and teleport player into it
                val player = it.player
                val world = player.world
                val area = inAreaPlayerMap[player] ?: return@handler
                if (area.nextStation != null) {
                    val location = Location(area.world, area.stopPoint.x.toDouble() + 0.5,
                        area.stopPoint.y.toDouble() + 0.1, area.stopPoint.z.toDouble() + 0.5)
                    val minecart = world.spawn(location, Minecart::class.java)
                    minecart.addPassenger(player)
                    inAreaMinecartMap[minecart] = area
                    waitMinecart(minecart, area)
                }
                it.setCancelled(true)
            }
            .bindWith(this)

        // 等待状态的矿车禁止水平移动
        Events.subscribe(VehicleMoveEvent::class.java, EventPriority.HIGHEST)
            .filter { it.vehicle is Minecart }
            .filter { waitingMinecartMap.containsKey(it.vehicle) }
            .handler { it.vehicle.velocity = Vector(0.0, it.vehicle.velocity.y, 0.0) }
            .bindWith(this)

        // 矿车销毁
        Events.subscribe(VehicleDestroyEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.vehicle is Minecart }
            .handler {
                waitingMinecartMap.remove(it.vehicle)?.close()
                inAreaMinecartMap.remove(it.vehicle)
            }
            .bindWith(this)

        // 矿车下来人
        Events.subscribe(VehicleExitEvent::class.java, EventPriority.MONITOR)
            .filter(EventFilters.ignoreCancelled())
            .filter { it.vehicle is Minecart }
            .handler {
                val world = it.vehicle.world
                val minecart = it.vehicle as Minecart
                var isPassenger = false
                if (it.exited is Player) {
                    if (hasBoardPlayers.remove(it.exited as Player)) {
                        (it.exited as Player).scoreboard = Bukkit.getScoreboardManager()!!.mainScoreboard
                        isPassenger = true
                    }
                    checkPlayer(it.exited as Player)
                }
                // 此时 minecart.isEmpty 是 false，下一个 tick 才会为真
                if (isPassenger) {
                    // 如果之前上面的是乘客, 则销毁
                    inAreaMinecartMap.remove(minecart)
                    waitingMinecartMap.remove(minecart)?.close()
                    minecart.remove()
                } else {
                    // 空车如果在区域内，就销毁
                    val point3D = Point3D(minecart.location.blockX, minecart.location.blockY, minecart.location.blockZ)
                    inAreaMinecartMap.remove(minecart)?.also { minecart.remove() }
                    queryArea(point3D, world)?.also { minecart.remove() }
                    waitingMinecartMap.remove(minecart)?.close()
                }

            }
            .bindWith(this)

        Events.subscribe(VehicleMoveEvent::class.java, EventPriority.MONITOR)
            .filter { it.vehicle is Minecart }
            .filter { !waitingMinecartMap.containsKey(it.vehicle) }
            .filter { it.from.blockX != it.to.blockX || it.from.blockZ != it.to.blockZ ||
                      it.from.blockY != it.to.blockY || it.from.world != it.to.world }
            .handler {
                val world = it.vehicle.world
                val minecart = it.vehicle as Minecart
                val point3D = Point3D(it.vehicle.location.blockX, it.vehicle.location.blockY, it.vehicle.location.blockZ)
                if (minecart.isEmpty) {
                    // 空车如果在区域内，就销毁
                    inAreaMinecartMap.remove(minecart)?.also { minecart.remove() }
                    queryArea(point3D, world)?.also { minecart.remove() }
                    waitingMinecartMap.remove(minecart)?.close()
                } else {
                    // 旧范围
                    inAreaMinecartMap[minecart]?.also { area ->
                        if (area.world == world && area.range3D.contains(point3D)) {
                            // 停靠站点检测
                            if (point3D.x == area.stopPoint.x && point3D.z == area.stopPoint.z
                                && minecart.passengers.any { p -> p is Player }) {
                                waitMinecart(minecart, area)
                                return@handler
                            }
                            // 阻止逆行
                            when (area.direction) {
                                Direction.NORTH -> {
                                    if (minecart.velocity.z > 0)
                                        minecart.velocity = minecart.velocity.setZ(-minecart.velocity.z)
                                }
                                Direction.SOUTH -> {
                                    if (minecart.velocity.z < 0)
                                        minecart.velocity = minecart.velocity.setZ(-minecart.velocity.z)
                                }
                                Direction.EAST -> {
                                    if (minecart.velocity.x < 0)
                                        minecart.velocity = minecart.velocity.setX(-minecart.velocity.x)
                                }
                                Direction.WEST -> {
                                    if (minecart.velocity.x > 0)
                                        minecart.velocity = minecart.velocity.setX(-minecart.velocity.x)
                                }
                            }
                            return@handler
                        }
                        var hasPassenger = false
                        inAreaMinecartMap.remove(minecart)
                        waitingMinecartMap.remove(minecart)?.close()
                        // 坐的可能不是人
                        minecart.passengers.forEach { passenger ->
                            if (passenger is Player) {
                                inAreaPlayerMap.remove(passenger)
                                onAreaExit(passenger, area)
                                hasPassenger = true
                            }
                        }
                        if (hasPassenger) {
                            playMusic(startMusic) {
                                if (minecart.isDead) {
                                    return@playMusic null
                                }
                                if (minecart.isEmpty) {
                                    minecart.remove()
                                    return@playMusic null
                                }
                                minecart.location
                            }
                        }
                    }
                    // 新范围
                    queryArea(point3D, world)?.also { area ->
                        var hasPassenger = false
                        inAreaMinecartMap[minecart] = area
                        // 坐的可能不是人
                        minecart.passengers.forEach { passenger ->
                            if (passenger is Player) {
                                inAreaPlayerMap[passenger] = area
                                onAreaEnter(passenger, area)
                                hasPassenger = true
                            }
                        }
                        if (hasPassenger) {
                            playMusic(arriveMusic) {
                                if (minecart.isDead) {
                                    return@playMusic null
                                }
                                if (minecart.isEmpty) {
                                    minecart.remove()
                                    return@playMusic null
                                }
                                minecart.location
                            }
                        }
                        // 阻止逆行
                        when (area.direction) {
                            Direction.NORTH -> {
                                if (minecart.velocity.z > 0)
                                    minecart.velocity = minecart.velocity.setZ(-minecart.velocity.z)
                            }
                            Direction.SOUTH -> {
                                if (minecart.velocity.z < 0)
                                    minecart.velocity = minecart.velocity.setZ(-minecart.velocity.z)
                            }
                            Direction.EAST -> {
                                if (minecart.velocity.x < 0)
                                    minecart.velocity = minecart.velocity.setX(-minecart.velocity.x)
                            }
                            Direction.WEST -> {
                                if (minecart.velocity.x > 0)
                                    minecart.velocity = minecart.velocity.setX(-minecart.velocity.x)
                            }
                        }
                        // 停靠站点检测
                        if (hasPassenger && point3D.x == area.stopPoint.x && point3D.z == area.stopPoint.z) {
                            waitMinecart(minecart, area)
                        }
                    }
                }
            }
            .bindWith(this)

        Events.subscribe(WorldLoadEvent::class.java)
            .handler { event ->
                configManager touch "rails.yml"
                configManager.parse<RailsConfig>("rails.yml").also { config ->
                    val world = event.world
                    if (!config.areas.containsKey(world.name)) return@handler
                    val octree = getWorldOctree(world) ?: return@handler
                    areaRangeMap[world] = mutableMapOf()
                    val stationIdMap = stations.mapKeys { it.value.id }
                    val lineIdMap = lines.mapKeys { it.value.id }
                    config.areas[world.name]!!.map {
                        val station = stationIdMap[it.station] ?: unknownStation
                        val line = lineIdMap[it.line] ?: unknownLine
                        RailArea(world, it.range3D, it.direction, it.stopPoint, station, line, it.reverse)
                    }.onEach {
                        areaRangeMap[it.world]!![it.range3D] = it
                        octree.insert(it.range3D)
                        lineStationAreas.getOrPut(it.station to it.line) { mutableSetOf() }.add(it)
                    }.also { messageManager.info("Loaded ${it.size} areas in world \"${world.name}\"") }
                }
            }
            .bindWith(this)

        Events.subscribe(WorldUnloadEvent::class.java)
            .handler { event ->
                worldOctrees.remove(event.world)
                areaRangeMap.remove(event.world)
                    ?.also { messageManager.info("Unloaded ${it.size} areas in world \"${event.world.name}\"") }
                inAreaMinecartMap.forEach {
                    if (it.key.world == event.world) inAreaMinecartMap.remove(it.key)
                }
                waitingMinecartMap.forEach {
                    if (it.key.world == event.world) waitingMinecartMap.remove(it.key)?.close()
                }
            }
            .bindWith(this)

        Events.subscribe(ChunkLoadEvent::class.java)
            .filter { !it.isNewChunk && worldOctrees.containsKey(it.world) }
            .handler {
                it.chunk.entities.forEach { entity ->
                    if (entity is Minecart) {
                        val point3D = Point3D(entity.location.blockX, entity.location.blockY, entity.location.blockZ)
                        queryArea(point3D, entity.world)?.also { area ->
                            if (entity.isEmpty) {
                                entity.remove()
                                return@forEach
                            }
                            inAreaMinecartMap[entity] = area
                            // 坐的可能不是人
                            entity.passengers.forEach { passenger ->
                                if (passenger is Player) {
                                    inAreaPlayerMap[passenger] = area
                                    onAreaEnter(passenger, area)
                                }
                            }
                        }
                    }
                }
            }
            .bindWith(this)
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
        WAITING_COUNT_DOWN = config.waitingSeconds * 4
        RUN_WARNING_THRESHOLD = config.startWarningSecond * 4
        DEFAULT_SPEED = config.startSpeed
        arriveMusic.clear()
        var lastPitchTick = 0L
        config.arriveMusic.forEach {
            try {
                // parse string like  0 C4 BLOCK_NOTE_BLOCK_PLING
                val (tickS, pitch, sound, volume) = it.split(" ")
                val tick = tickS.toLong()
                arriveMusic.add(Note(Sound.valueOf(sound), pitch.toPitch(), tick - lastPitchTick, volume.toFloat()))
                lastPitchTick = tick
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        startMusic.clear()
        lastPitchTick = 0L
        config.startMusic.forEach {
            try {
                val (tickS, pitch, sound, volume) = it.split(" ")
                val tick = tickS.toLong()
                startMusic.add(Note(Sound.valueOf(sound), pitch.toPitch(), tick - lastPitchTick, volume.toFloat()))
                lastPitchTick = tick
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // areas.yml
        configManager touch "rails.yml"
        worldOctrees.clear()
        areaRangeMap.clear()
        inAreaPlayerMap.clear()
        inAreaMinecartMap.clear()
        lineStationAreas.clear()
        stations.clear()
        lines.clear()
        nextLineId = 1
        nextStationId = 1
        val railConfig = configManager.parse<RailsConfig>("rails.yml")
        // Stations and lines
        val stationIdMap = mutableMapOf<Int, Station>()
        val lineIdMap = mutableMapOf<Int, RailLine>()
        railConfig.stations.toSortedMap().forEach { (id, station) ->
            nextStationId = maxOf(nextStationId, id + 1)
            stations[station.name] = Station(id, station.name)
            stationIdMap[id] = stations[station.name]!!
        }
        railConfig.railLines.toSortedMap().forEach { (id, lineC) ->
            val stations = lineC.stations.mapNotNull { stationIdMap[it] }.toMutableList()
            val color = lineC.color.toColor()
            nextLineId = maxOf(nextLineId, id + 1)
            val line = RailLine(id, lineC.name, stations, color, lineC.isCycle, lineC.colorMaterial, lineC.leftReturn, lineC.rightReturn)
            stations.forEach { station -> station.lines.add(line) }
            lines[line.name] = line
            lineIdMap[id] = line
        }
        // Rail areas
        for ((worldName, areas) in railConfig.areas) {
            val world = Bukkit.getWorld(worldName) ?: continue
            val octree = getWorldOctree(world) ?: continue
            areaRangeMap[world] = mutableMapOf()
            areas.map {
                val station = stationIdMap[it.station]
                val line = lineIdMap[it.line]
                if (station != null && line != null && station.lines.contains(line) && line.stations.contains(station)) {
                    return@map RailArea(world, it.range3D, it.direction, it.stopPoint, station, line, it.reverse)
                } else {
                    return@map RailArea(world, it.range3D, it.direction, it.stopPoint, unknownStation, unknownLine, it.reverse)
                }
            }.onEach {
                if (it.station != unknownStation && it.line != unknownLine) {
                    areaRangeMap[it.world]!![it.range3D] = it
                    octree.insert(it.range3D)
                }
                lineStationAreas.getOrPut(it.station to it.line) { mutableSetOf() }.add(it)
            }.also { messageManager.info("Loaded ${it.size} areas in world \"$worldName\"") }
            if (areaRangeMap[world]!!.isEmpty()) {
                areaRangeMap.remove(world)
                worldOctrees.remove(world)
            }
        }
    }

    fun save() {
        // Area worlds
        val areasWorlds = mutableMapOf<String, List<RailAreaConfig>>()
        for ((world, areas) in areaRangeMap) {
            areasWorlds[world.name] = areas.map { RailAreaConfig.valueOf(it.value) }
        }
        val stationMap = sortedMapOf<Int, StationConfig>()
        stations.forEach { (_, station) -> stationMap[station.id] = StationConfig(station.name) }
        val lineMap = sortedMapOf<Int, RailLineConfig>()
        this.lines.forEach { (_, line) ->
            lineMap[line.id] = RailLineConfig(line.name, line.stations.map { it.id }, line.color.toHexString(),
                line.isCycle, line.colorMaterial, line.leftReturn, line.rightReturn)
        }

        // copy rails.yml to rails-backup.yml
        val railFile = File(dataFolder, "rails.yml")
        if (railFile.length() > 10L) {
            val backupFile = File(dataFolder, "rails-backup.yml")
            railFile.copyTo(backupFile, true)
        }
        configManager.save(
            RailsConfig(
                stations = stationMap,
                railLines = lineMap,
                areas = areasWorlds,
            ), "rails.yml"
        )
    }

    private fun queryArea(point: Point3D, world: World): RailArea? {
        val octree = worldOctrees[world] ?: return null
        val range = octree.firstRange(point) ?: return null
        return areaRangeMap[world]!![range]
    }

    fun checkPlayer(player: Player, location: Location? = null) {
        val world = location?.world ?: player.world
        val l = location ?: player.location
        val point3D = Point3D(l.blockX, l.blockY, l.blockZ)
        // 旧范围
        inAreaPlayerMap[player]?.also { area ->
            if (area.world == world && area.range3D.contains(point3D)) return@checkPlayer
            inAreaPlayerMap.remove(player)
            onAreaExit(player, area)
        }
        // 新范围
        queryArea(point3D, world)?.also { area ->
            inAreaPlayerMap[player] = area
            onAreaEnter(player, area)
        }
    }

    private fun waitMinecart(minecart: Minecart, area: RailArea) {
        if (waitingMinecartMap.containsKey(minecart)) return
        if (area.nextStation == null) {
            minecart.passengers.forEach {
                if (it is Player) messageManager.printf(it, "§c列车已到达终点站, 感谢您的乘坐!")
                minecart.removePassenger(it)
                it.teleport(Location(area.world, area.stopPoint.x.toDouble()+0.5, area.stopPoint.y.toDouble()+0.3, area.stopPoint.z.toDouble()+0.5))
            }
            minecart.remove()
            return
        }
        showBoard(true, area, minecart)

        // 发车倒计时
        var countDown = WAITING_COUNT_DOWN
        var countSeconds = WAITING_COUNT_DOWN / 4.0
        minecart.velocity = Vector(0, 0, 0)
        waitingMinecartMap[minecart] = Schedulers.async().runRepeating({ task ->
            if (minecart.isEmpty) {
                minecart.remove()
                task.close()
                waitingMinecartMap.remove(minecart)
                return@runRepeating
            }
            if (minecart.isDead) {
                task.close()
                waitingMinecartMap.remove(minecart)
                return@runRepeating
            }
            countDown--
            if (countDown <= 0) {
                showBoard(false, area, minecart)
                task.close()
                Schedulers.sync().run {
                    waitingMinecartMap.remove(minecart)
                    when (area.direction) {
                        Direction.NORTH -> minecart.velocity = Vector(0.0, 0.0, -DEFAULT_SPEED)
                        Direction.SOUTH -> minecart.velocity = Vector(0.0, 0.0, DEFAULT_SPEED)
                        Direction.EAST -> minecart.velocity = Vector(DEFAULT_SPEED, 0.0, 0.0)
                        Direction.WEST -> minecart.velocity = Vector(-DEFAULT_SPEED, 0.0, 0.0)
                    }
                    minecart.passengers.forEach {
                        if (it is Player) {
                            it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("§c列车已启动, 请扶好站稳, 注意安全"))
                            it.sendTitle(area, RailTitleMode.START, 5, 100, 0)
                        }
                    }
                }
                return@runRepeating
            }
            if (countDown in 2 .. RUN_WARNING_THRESHOLD) {
                minecart.world.playSound(minecart.location, START_WARNING_SOUND, 1.0f, "C4".toPitch())
            }
            countSeconds -= 0.25
            minecart.passengers.forEach {
                if (it is Player) it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("列车已到站停靠，您可下车，或留在车上待§e ${countSeconds.toInt()} §r秒后自动发车"))
            }
        }, 0, 5)
    }

    fun addArea(area: RailArea, save: Boolean = true) {
        if (!areaRangeMap.containsKey(area.world)) areaRangeMap[area.world] = mutableMapOf()
        areaRangeMap[area.world]!![area.range3D] = area
        getWorldOctree(area.world)!!.insert(area.range3D)
        lineStationAreas.getOrPut(area.station to area.line) { mutableSetOf() }.add(area)
        if (save) save()
    }

    fun removeArea(area: RailArea, save: Boolean = true) {
        areaRangeMap[area.world]?.remove(area.range3D)
        worldOctrees[area.world]?.remove(area.range3D)
        if (areaRangeMap[area.world]?.isEmpty() == true) {
            areaRangeMap.remove(area.world)
            worldOctrees.remove(area.world)
        }
        lineStationAreas[area.station to area.line]?.also {
            it.remove(area)
            if (it.size == 0) lineStationAreas.remove(area.station to area.line)
        }
        if (save) save()
    }

    fun updateArea(oldArea: RailArea, newArea: RailArea, save: Boolean = true) {
        if (oldArea.world != newArea.world || oldArea.range3D != newArea.range3D ||
            oldArea.station == unknownStation || oldArea.line == unknownLine ||
            newArea.station == unknownStation || newArea.line == unknownLine) {
            areaRangeMap[oldArea.world]?.remove(oldArea.range3D)
            worldOctrees[oldArea.world]?.remove(oldArea.range3D)
            if (newArea.station != unknownStation && newArea.line != unknownLine) {
                areaRangeMap.getOrPut(newArea.world) { mutableMapOf() }[newArea.range3D] = newArea
                getWorldOctree(newArea.world)!!.insert(newArea.range3D)
            }
            if (areaRangeMap[oldArea.world]?.isEmpty() == true) {
                areaRangeMap.remove(oldArea.world)
                worldOctrees.remove(oldArea.world)
            }
        } else {
            areaRangeMap.getOrPut(newArea.world) { mutableMapOf() }[newArea.range3D] = newArea
        }
        lineStationAreas[oldArea.station to oldArea.line]?.also {
            it.remove(oldArea)
            if (it.size == 0) lineStationAreas.remove(oldArea.station to oldArea.line)
        }
        lineStationAreas.getOrPut(newArea.station to newArea.line) { mutableSetOf() }.add(newArea)
        if (save) save()
    }

    fun addRailLine(railLine: RailLine, save: Boolean = true) {
        lines[railLine.name] = railLine
        if (save) save()
    }

    fun addStation(station: Station, save: Boolean = true) {
        stations[station.name] = station
        if (save) save()
    }

    fun removeStation(station: Station, save: Boolean = true) {
        stations.remove(station.name)
        station.lines.forEach {
            it.stations.remove(station)
            lineStationAreas.remove(station to it)?.forEach { area ->
                updateArea(area, RailArea(area.world, area.range3D,
                    area.direction, area.stopPoint, unknownStation, unknownLine, area.reverse), false
                )
            }
        }
        if (save) save()
    }

    fun removeLine(line: RailLine, save: Boolean = true) {
        lines.remove(line.name)
        line.stations.forEach {
            it.lines.remove(line)
            lineStationAreas.remove(it to line)?.forEach { area ->
                updateArea(area, RailArea(area.world, area.range3D,
                    area.direction, area.stopPoint, unknownStation, unknownLine, area.reverse), false)
            }
        }
        if (save) save()
    }

    fun updateStation(oldStation: Station, newStation: Station, save: Boolean = true) {
        stations.remove(oldStation.name)
        stations[newStation.name] = newStation
        // 规定 station 的 line 不能改变
        oldStation.lines.forEach {
            val index = it.stations.indexOf(oldStation)
            if (index != -1) it.stations[index] = newStation
            lineStationAreas.remove(oldStation to it)?.forEach { area ->
                updateArea(area, RailArea(area.world, area.range3D,
                    area.direction, area.stopPoint, newStation, area.line, area.reverse), false)
            }
        }
        if (save) save()
    }

    fun updateLine(oldLine: RailLine, newLine: RailLine, save: Boolean = true) {
        lines.remove(oldLine.name)
        lines[newLine.name] = newLine
        // line.station 可以改变
        newLine.stations.forEach {
            if (it.lines.add(newLine)) {
                lineStationAreas.remove(it to oldLine)?.forEach { area ->
                    updateArea(area, RailArea(area.world, area.range3D,
                        area.direction, area.stopPoint, area.station, newLine, area.reverse), false)
                }
            }
        }
        oldLine.stations.forEach {
            it.lines.remove(oldLine)
            lineStationAreas.remove(it to oldLine)?.forEach { area ->
                updateArea(area, RailArea(area.world, area.range3D,
                    area.direction, area.stopPoint, unknownStation, unknownLine, area.reverse), false)
            }
        }
        if (save) save()
    }

    private fun getWorldOctree(world: World): Octree? {
        return worldOctrees.getOrPut(world) {
            val worldSize = worldSizes[world.name] ?: /* worldSizes["default"] ?: */ return null
            val minX = minOf(worldSize.x1, worldSize.x2)
            val minZ = minOf(worldSize.z1, worldSize.z2)
            val maxX = maxOf(worldSize.x1, worldSize.x2)
            val maxZ = maxOf(worldSize.z1, worldSize.z2)
            val worldRange = Range3D(minX, 0, minZ, maxX, 255, maxZ)
            Octree(worldRange)
        }
    }

    private fun playMusic(music: Iterable<Note>, getLocation: () -> Location?) {
        // get iterator of music
        val iter = music.iterator()
        fun playNext() {
            if (iter.hasNext()) {
                val note = iter.next()
                if (note.delta <= 0) {
                    val l = getLocation()
                    if (l != null && l.world != null) {
                        l.world!!.playSound(l, note.sound, note.volume, note.pitch)
                        playNext()
                    }
                } else {
                    Schedulers.sync().runLater({
                        val l = getLocation()
                        if (l != null && l.world != null) {
                            l.world!!.playSound(l, note.sound, note.volume, note.pitch)
                            playNext()
                        }
                    }, note.delta).bindWith(this)
                }
            }
        }
        playNext()
    }
}
