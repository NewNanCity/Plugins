package city.newnan.railarea.manager

import city.newnan.core.base.BaseModule
import city.newnan.core.cache.LRUCache
import city.newnan.core.event.subscribeEvent
import city.newnan.core.scheduler.runSync
import city.newnan.core.utils.text.ComponentParseMode
import city.newnan.core.utils.text.toComponent
import city.newnan.railarea.RailAreaPlugin
import city.newnan.railarea.config.RailArea
import city.newnan.railarea.config.Station
import city.newnan.railarea.config.toHexString
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard
import java.util.concurrent.ConcurrentHashMap

class BoardManager(
    val plugin: RailAreaPlugin,
) : BaseModule("BoardManager", plugin) {

    // 分数板缓存
    private val scoreboardCache = LRUCache<String, Scoreboard>(32)

    // 玩家分数板映射
    private val playerScoreboards = ConcurrentHashMap<Player, Scoreboard>()

    override fun onInit() {
        subscribeEvent<PlayerQuitEvent> {
            handler { hideBoard(it.player) }
        }
    }

    override fun onReload() {
        // 重载逻辑
        scoreboardCache.clear()
    }

    override fun onClose() {
        // 关闭逻辑
        scoreboardCache.clear()
        val mainScoreboard = Bukkit.getScoreboardManager().mainScoreboard
        playerScoreboards.forEach { (player, scoreboard) ->
            if (player.scoreboard == scoreboard) player.scoreboard = mainScoreboard
        }
        playerScoreboards.clear()
    }

    fun hideBoard(player: Player) {
        val currentBoard = playerScoreboards.remove(player) ?: return
        if (currentBoard != player.scoreboard)
        player.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
    }

    fun genBoardId(area: RailArea, wait: Boolean): String =
        "line${area.line.id}.station${area.station.id}.${if (wait) "wait" else "ground"}.${if (area.reverse) "reverse" else "forward"}."

    fun showBoard(wait: Boolean, area: RailArea, minecart: Minecart) {
        val boardId = genBoardId(area, wait)
        val scoreboard = scoreboardCache.getOrPut(boardId) { createScoreboard(wait, area) }
        plugin.runSync {
            minecart.passengers.forEach { p ->
                if (p is Player) {
                    p.scoreboard = scoreboard
                    playerScoreboards[p] = scoreboard
                }
            }
        }
    }

    fun createScoreboard(wait: Boolean, area: RailArea): Scoreboard {
        // 创建分数板
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard
        val board = scoreboard.registerNewObjective(
            "rail_board",
            Criteria.DUMMY,
            RailAreaPlugin.instance.messager.sprintf(
                " <dark_gray>==</dark_gray>    <color:{1}><bold>{0}</bold></color:{1}>    <dark_gray>==</dark_gray>",
                area.line.name,
                area.line.color.toHexString()
            )
        ).also { it.displaySlot = DisplaySlot.SIDEBAR }

        // 计算后续站台
        val dots = area.line.stations.size > 9
        val maxStations = if (dots) 7 else 8
        val index = area.line.stations.indexOf(area.station)
        val nextStations: MutableList<Station> = mutableListOf()
        var terminal: Station? = null
        if (area.line.isCycle) {
            if (area.reverse) {
                for (i in index - 1 downTo 0) {
                    if (nextStations.size > maxStations) break
                    nextStations.add(area.line.stations[i])
                }
                for (i in area.line.stations.size - 1 downTo index + 1) {
                    if (nextStations.size > maxStations) break
                    nextStations.add(area.line.stations[i])
                }
            } else {
                for (i in index + 1 until area.line.stations.size) {
                    if (nextStations.size > maxStations) break
                    nextStations.add(area.line.stations[i])
                }
                for (i in 0 until index) {
                    if (nextStations.size > maxStations) break
                    nextStations.add(area.line.stations[i])
                }
            }
        } else {
            if (area.reverse) {
                for (i in index - 1 downTo 0) {
                    if (nextStations.size > maxStations) break
                    nextStations.add(area.line.stations[i])
                }
                terminal = if (area.line.leftReturn) {
                    for (i in 0 until area.line.stations.size) {
                        if (nextStations.size > maxStations) break
                        nextStations.add(area.line.stations[i])
                    }
                    area.line.stations.last()
                } else {
                    area.line.stations.first()
                }
            } else {
                for (i in index + 1 until area.line.stations.size) {
                    if (nextStations.size > maxStations) break
                    nextStations.add(area.line.stations[i])
                }
                terminal = if (area.line.rightReturn) {
                    for (i in area.line.stations.size - 1 downTo 0) {
                        if (nextStations.size > maxStations) break
                        nextStations.add(area.line.stations[i])
                    }
                    area.line.stations.first()
                } else {
                    area.line.stations.last()
                }
            }
        }
        if (nextStations.isEmpty()) terminal = null
        if (area.line.isCycle) terminal = null
        else if (terminal == nextStations.last()) terminal = null
        var scoreIndex = nextStations.size + (if (dots) 1 else 0) + (if (terminal == null) 0 else 1)

        // 渲染路线牌
        val textContained = HashSet<String>()
        // https://www.spigotmc.org/threads/1-16-1-19-scoreboard-objective-score-with-rgb-hex-color.468079/
        fun intersection(text: String, colors: List<Color>) {
            val t = if (textContained.contains(text)) "$text*" else text
            textContained.add(text)
            if (colors.isNotEmpty()) {
                val team = scoreboard.registerNewTeam(t)
                val sb = StringBuilder(" ")
                colors.map { it.toHexString() }.forEach { sb.append("<color:$it>●</color:$it>") }
                team.suffix(sb.toString().toComponent(ComponentParseMode.MiniMessage))
                team.addEntry(t)
            }
            board.getScore(t).score = scoreIndex--
        }

        if (wait) {
            intersection(
                " §6☛ §l${area.station.name}",
                area.station.lines.filter { l -> l != area.line }.map { l -> l.color })
        } else {
            intersection(
                " §8▼ §7§o${area.station.name}",
                area.station.lines.filter { l -> l != area.line }.map { l -> l.color })
        }
        var first = true
        nextStations.forEach { station ->
            if (first) {
                if (wait) {
                    intersection(
                        " §f▽ §l${station.name}",
                        station.lines.filter { l -> l != area.line }.map { l -> l.color })
                } else {
                    intersection(
                        " §6☛ §l${station.name}",
                        station.lines.filter { l -> l != area.line }.map { l -> l.color })
                }
                first = false
            } else {
                intersection(
                    " §f▽ ${station.name}",
                    station.lines.filter { l -> l != area.line }.map { l -> l.color })
            }
        }
        if (terminal != null) {
            if (dots && nextStations.size >= maxStations)
                board.getScore("     §8...     ").score = scoreIndex--
            intersection(
                " ◇ §f§n${terminal.name}",
                terminal.lines.filter { l -> l != area.line }.map { l -> l.color })
        }
        return scoreboard
    }
}