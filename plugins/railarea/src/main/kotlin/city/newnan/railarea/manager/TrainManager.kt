package city.newnan.railarea.manager

import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.core.scheduler.ITaskHandler
import city.newnan.core.scheduler.runSyncLater
import city.newnan.core.scheduler.runSyncRepeating
import city.newnan.railarea.RailAreaPlugin
import city.newnan.railarea.config.MusicNote
import city.newnan.railarea.config.RailArea
import city.newnan.railarea.config.RailDirection
import city.newnan.railarea.config.toHexString
import city.newnan.railarea.config.toPitch
import city.newnan.railarea.i18n.LanguageKeys
import city.newnan.railarea.utils.TitleUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent
import org.bukkit.util.Vector
import java.util.concurrent.ConcurrentHashMap

class TrainManager(
    val plugin: RailAreaPlugin,
    private val stationStorage: StationStorage,
    private val areaDetector: AreaDetector,
    private val boardManager: BoardManager
) : BaseModule("TrainManager", plugin) {
    // 配置缓存
    private var waitingCountDown: Int = 20 // 等待时间（tick）
    private var runWarningThreshold: Int = 12 // 发车警告阈值（tick）
    private var defaultSpeed: Double = 1.0 // 默认速度
    private var startWarningSound: Sound = Sound.BLOCK_NOTE_BLOCK_BIT // 发车警告音效
    private var arriveMusic: List<MusicNote> = emptyList() // 到站音乐
    private var startMusic: List<MusicNote> = emptyList() // 发车音乐

    // 要被忽略的位置
    private var ignoringTick: Int = 0
    private val ignoreSpawningMinecart = mutableListOf<Int>()
    private val playerWaitingForMinecart = ConcurrentHashMap<Player, Int>()

    private val waitingMinecarts = ConcurrentHashMap<Minecart, ITaskHandler<Unit>>()

    init { init() }

    override fun onInit() {
        areaDetector.onPlayerNotOnMinecartEnterArea.addListener { handlePlayerNotOnMinecartEnterArea(it.first, it.second) }
        areaDetector.onPlayerNotOnMinecartExitArea.addListener { handlePlayerNotOnMinecartExitArea(it.first, it.second) }
        areaDetector.onMinecartEnterArea.addListener { handleMinecartEnterArea(it.first, it.second) }
        areaDetector.onMinecartExitArea.addListener { handleMinecartExitArea(it.first, it.second) }
        areaDetector.onMinecartUpdate.addListener { handleMinecartUpdate(it.first, it.second) }

        // 玩家右击铁轨事件
        subscribeEvent<PlayerInteractEvent> {
            priority(EventPriority.LOWEST)
            filter { it.action == Action.RIGHT_CLICK_BLOCK }
            filter { it.clickedBlock?.type == Material.RAIL }
            filter { it.player.hasPermission("railarea.use") }
            handler { event ->
                val areaId = areaDetector.queryArea(event.interactionPoint!!) ?: return@handler
                val area = stationStorage.getAreaById(areaId) ?: return@handler
                val player = event.player
                if (area.nextStation != null) {
                    // 生成一辆矿车，剩下交给 MinecartEnterArea 处理
                    playerWaitingForMinecart[player] = areaId
                    spawnIgnoredMinecart(area)
                }
                // 取消事件，无论是否有下一站，以避免用户在这里放置矿车或者方块
                event.isCancelled = true
                event.setUseInteractedBlock(Event.Result.DENY)
                event.setUseItemInHand(Event.Result.DENY)
            }
        }

        // 矿车碰撞事件：区域中的矿车不碰撞
        subscribeEvent<VehicleEntityCollisionEvent> {
            priority(EventPriority.LOWEST)
            filter { !it.isCancelled }
            filter { it.vehicle is Minecart && it.entity is Minecart }
            handler {
                if (waitingMinecarts.containsKey(it.vehicle)) {
                    it.isCancelled = true
                    return@handler
                }
                if (areaDetector.getMinecartArea(it.vehicle as Minecart) != null || areaDetector.getMinecartArea(it.entity as Minecart) != null) {
                    it.isCancelled = true
                }
            }
        }

        // 持续检测任务
        runSyncRepeating(20L, 20L) {
            // 处理玩家在区域内的持续状态
            areaDetector.getPlayersNotOnMinecart().forEach { handlePlayerNotOnMinecartStayInArea(it.first, it.second) }
        }
    }

    override fun onReload() {
        val config = plugin.getPluginConfig()
        waitingCountDown = config.waitingSeconds * 20 // 转换为tick
        runWarningThreshold = config.startWarningSeconds * 20 // 转换为tick
        defaultSpeed = config.startSpeed
        startWarningSound = config.startWarningSound
        arriveMusic = config.arriveMusic
        startMusic = config.startMusic
    }

    // ------------------- 处理函数 -------------------
    private fun handleMinecartEnterArea(minecart: Minecart, areaId: Int) {
        if (minecart.isDead) return
        // 如果矿车是空的，检测是否刚刚生成的、等待玩家上车的矿车，是的话，接玩家上车，否则删除
        if (minecart.isEmpty) {
            if (Bukkit.getServer().currentTick == ignoringTick && ignoreSpawningMinecart.contains(areaId)) {
                ignoreSpawningMinecart.remove(areaId)
                val stopLocation = stationStorage.getAreaById(areaId)?.getStopLocation()
                val waitingPlayer = playerWaitingForMinecart.filter { it.value == areaId }.keys.firstOrNull()
                if (waitingPlayer != null && stopLocation != null) {
                    playerWaitingForMinecart.remove(waitingPlayer)
                    minecart.addPassenger(waitingPlayer) // 触发第一次 MinecartUpdate，假如车辆车是刚生成在停靠点的，会开始等待逻辑
                    minecart.teleport(stopLocation) // 如果矿车位置不在停靠点，触发第二次 MinecartUpdate，如果刚才已经开始了等待逻辑，这里会忽略，否则会开始等待逻辑
                    // 如上的设计能够消除一些极端的边缘情况（比如一辆新的车生成在停靠点之外，或者有多辆车同时生成/进入），提高系统的鲁棒性
                    return
                }
            }
            minecart.remove()
            return
        }
        val area = stationStorage.getAreaById(areaId) ?: return
        // 不是空矿车，更新分数板
        boardManager.showBoard(true, area, minecart)
        // 为乘客显示到站标题
        minecart.passengers.forEach { passenger ->
            if (passenger is Player) {
                TitleUtils.showRailAreaTitle(passenger, area, TitleUtils.TitleMode.ARRIVE, 5, 200, 0)
            }
        }
        // 播放到站音乐
        playMusic(arriveMusic) {
            if (minecart.isDead || minecart.isEmpty) null else minecart.location
        }
    }

    private fun handleMinecartExitArea(minecart: Minecart, areaId: Int) {
        if (minecart.isDead) return
        waitingMinecarts.remove(minecart)?.close()
        if (minecart.isEmpty) {
            minecart.remove()
            return
        }
        val area = stationStorage.getAreaById(areaId) ?: return
        // 为乘客显示发车标题
        minecart.passengers.forEach { passenger ->
            if (passenger is Player) {
                TitleUtils.showRailAreaTitle(passenger, area, TitleUtils.TitleMode.START, 5, 200, 0)
            }
        }
        // 播放发车音乐
        playMusic(startMusic) {
            if (minecart.isDead || minecart.isEmpty) null else minecart.location
        }
    }

    private fun handleMinecartUpdate(minecart: Minecart, areaId: Int) {
        // 触发时机：玩家上下车、载具在区域内移动（一定跨方块）

        if (minecart.isEmpty) { // 空矿车在区域内移动，或者玩家在区域内下车
            minecart.remove()
            waitingMinecarts.remove(minecart)?.close()
            return
        }

        // 略过正在等待的矿车
        if (waitingMinecarts.containsKey(minecart)) return

        // 获取区域信息
        val area = stationStorage.getAreaById(areaId) ?: return

        // 防止逆行
        var velocity = minecart.velocity
        when (area.direction) {
            RailDirection.NORTH -> {
                if (velocity.z > 0) {
                    minecart.velocity = velocity.setZ(-velocity.z)
                }
            }
            RailDirection.SOUTH -> {
                if (velocity.z < 0) {
                    minecart.velocity = velocity.setZ(-velocity.z)
                }
            }
            RailDirection.EAST -> {
                if (velocity.x < 0) {
                    minecart.velocity = velocity.setX(-velocity.x)
                }
            }
            RailDirection.WEST -> {
                if (velocity.x > 0) {
                    minecart.velocity = velocity.setX(-velocity.x)
                }
            }
        }

        // 检查停靠
        var shouldStop = false
        velocity = minecart.velocity
        // 如果脱轨，进入停靠模式
        // 如果在停靠点上就可以进入停靠模式（玩家上车/矿车经过）
        // 如果速度过低，如果在停靠点之后，使其加速驶离区域、在停靠点之前使其进入停靠模式
        if (minecart.location.block.type.name.indexOf("RAIL") == -1) {
            shouldStop = true
        } else if (minecart.location.blockX == area.stopPoint.x && minecart.location.blockZ == area.stopPoint.z) {
            shouldStop = true
        } else if (velocity.length() < 0.1) {
            when (area.direction) {
                RailDirection.NORTH -> {
                    if (minecart.location.blockZ < area.stopPoint.z) {
                        shouldStop = true
                    } else {
                        minecart.velocity = velocity.setZ(-defaultSpeed)
                    }
                }
                RailDirection.SOUTH -> {
                    if (minecart.location.blockZ > area.stopPoint.z) {
                        shouldStop = true
                    } else {
                        minecart.velocity = velocity.setZ(defaultSpeed)
                    }
                }
                RailDirection.EAST -> {
                    if (minecart.location.blockX > area.stopPoint.x) {
                        shouldStop = true
                    } else {
                        minecart.velocity = velocity.setX(defaultSpeed)
                    }
                }
                RailDirection.WEST -> {
                    if (minecart.location.blockX < area.stopPoint.x) {
                        shouldStop = true
                    } else {
                        minecart.velocity = velocity.setX(-defaultSpeed)
                    }
                }
            }
        }

        // 到达停靠点，开始等待
        if (shouldStop) {
            startWaiting(minecart, area)
        }
    }

    private fun handlePlayerNotOnMinecartEnterArea(player: Player, areaId: Int) {
        boardManager.hideBoard(player)
        val area = stationStorage.getAreaById(areaId) ?: return
        handlePlayerNotOnMinecartStayInArea(player, areaId)
    }

    private fun handlePlayerNotOnMinecartExitArea(player: Player, areaId: Int) {
        boardManager.hideBoard(player)
    }

    private fun handlePlayerNotOnMinecartStayInArea(player: Player, areaId: Int) {
        val area = stationStorage.getAreaById(areaId) ?: return
        TitleUtils.showRailAreaTitle(player, area, TitleUtils.TitleMode.UNDER_BOARD, 0, 40, 0)
        if (area.nextStation == null) {
            player.sendActionBar(plugin.messager.sprintf(LanguageKeys.Events.Core.TERMINAL_ACTIONBAR)) // "<red>本站为终点站，请到对侧站台乘车</red>"
        } else {
            val lineColor = area.line.color.toHexString()
            player.sendActionBar(plugin.messager.sprintf(
                 LanguageKeys.Events.Core.NON_TERMINAL_ACTIONBAR, // "<red>请右击铁轨, 自动搭乘</red> <bold><color:{0}>{1}</color:{0}></bold> <red>列车</red>"
                lineColor, area.line.name
            ))
        }
    }

    // ------------------- 工具函数 --------------------
    private fun spawnIgnoredMinecart(area: RailArea): Minecart? {
        val stopLocation = area.getStopLocation() ?: return null
        val currentTick = Bukkit.getServer().currentTick
        if (currentTick != ignoringTick) {
            ignoringTick = currentTick
            ignoreSpawningMinecart.clear()
        }
        ignoreSpawningMinecart.add(area.id)
        return stopLocation.world.spawn(stopLocation, Minecart::class.java)
    }

    private fun startWaiting(minecart: Minecart, area: RailArea) {
        // 如果是终点站，直接下车
        if (area.nextStation == null) {
            minecart.passengers.forEach { passenger ->
                if (passenger is Player) {
                    plugin.messager.printf(passenger, LanguageKeys.Events.Core.ARRIVED_TERMINAL_MESSAGE) // "<green>列车已到达终点站，感谢您的乘坐！</green>"
                }
                passenger.leaveVehicle() // 剩下的逻辑交给 MinecartUpdate 和 PlayerNotOnMinecartEnterArea
            }
            return
        }

        // 更新分数板
        boardManager.showBoard(true, area, minecart)

        // 固定矿车位置
        minecart.velocity = Vector(0, 0, 0)
        minecart.teleport(area.getStopLocation()!!)

        // 开始倒计时
        var countDownTick = waitingCountDown
        waitingMinecarts[minecart] = runSyncRepeating(0L, 1L) { taskHandle ->
            // 已失效
            if (minecart.isDead || minecart.isEmpty) {
                waitingMinecarts.remove(minecart)
                taskHandle.cancel()
                minecart.remove()
                return@runSyncRepeating
            }

            countDownTick--

            // 检查发车
            if (countDownTick <= 0) {
                waitingMinecarts.remove(minecart)
                // 设置矿车速度
                minecart.velocity = when (area.direction) {
                    RailDirection.NORTH -> Vector(0.0, 0.0, -defaultSpeed)
                    RailDirection.SOUTH -> Vector(0.0, 0.0, defaultSpeed)
                    RailDirection.EAST -> Vector(defaultSpeed, 0.0, 0.0)
                    RailDirection.WEST -> Vector(-defaultSpeed, 0.0, 0.0)
                }
                // 显示发车消息
                minecart.passengers.forEach { passenger ->
                    if (passenger is Player) {
                        passenger.sendActionBar(plugin.messager.sprintf(LanguageKeys.Events.Core.START_MESSAGE)) // <green>列车已启动，请扶好站稳，注意安全</green>
                        TitleUtils.showRailAreaTitle(passenger, area, TitleUtils.TitleMode.START, 5, 100, 0)
                    }
                }
                taskHandle.cancel()
                return@runSyncRepeating
            }

            // 持续固定矿车位置
            minecart.velocity = Vector(0, 0, 0)
            minecart.teleport(area.getStopLocation()!!)

            // 音效和信息更新
            if (countDownTick % 10 == 0 && countDownTick <= runWarningThreshold) {
                minecart.world.playSound(minecart.location, startWarningSound, 1.0f, "C4".toPitch())
                minecart.passengers.forEach { passenger ->
                    if (passenger is Player) {
                        passenger.sendActionBar(plugin.messager.sprintf(LanguageKeys.Events.Core.WAITING_MESSAGE, (countDownTick / 20).toString())) // 列车将在 <yellow>{0}</yellow> 秒后发车，您可在车上等待或现在下车
                        TitleUtils.showRailAreaTitle(passenger, area, TitleUtils.TitleMode.ARRIVE, 5, 20, 0)
                    }
                }
            }
        }
    }

    private fun playMusic(music: List<MusicNote>, getLocation: () -> Location?) {
        if (music.isEmpty()) return

        var currentIndex = 0

        fun playNextByIndex() {
            if (currentIndex >= music.size) return

            val note = music[currentIndex]
            val location = getLocation()

            if (location?.world != null) {
                location.world.playSound(location, note.sound, note.volume.toFloat(), note.note.toPitch())

                currentIndex++
                if (currentIndex < music.size) {
                    val nextNote = music[currentIndex]
                    val delay = (nextNote.tick - note.tick).toLong()

                    if (delay > 0) {
                        runSyncLater(delay) { playNextByIndex() }
                    } else {
                        playNextByIndex()
                    }
                }
            }
        }

        playNextByIndex()
    }
}