package city.newnan.railarea.utils

import city.newnan.railarea.PluginMain
import city.newnan.railarea.config.RailArea
import city.newnan.railarea.config.Station
import city.newnan.railarea.config.toFMString
import city.newnan.railarea.config.toHexString
import me.lucko.helper.Schedulers
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot

fun showBoard(wait: Boolean, area: RailArea, minecart: Minecart) {
     Schedulers.sync().run {
        Bukkit.getScoreboardManager()?.newScoreboard?.also {
            val n = "rail.${if (wait) "w" else "g"}.l${area.line.id}.s${area.station.id}"
            val name = n.substring(0, n.length.coerceAtMost(16))
            it.getObjective(n)?.unregister()
            val board = it.registerNewObjective(name, "dummy",
                " §8==§r    ${area.line.color.toFMString()}§n§l${area.line.name}§r    §8==§r ")
            board.displaySlot = DisplaySlot.SIDEBAR
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
            if (nextStations.size <= 0) terminal = null
            if (area.line.isCycle) terminal = null
            else if (terminal == nextStations.last()) terminal = null
            var scoreIndex = nextStations.size + (if (dots) 1 else 0) + (if (terminal == null) 0 else 1)

            val textContained = HashSet<String>()
            // https://www.spigotmc.org/threads/1-16-1-19-scoreboard-objective-score-with-rgb-hex-color.468079/
            fun intersection(text: String, colors: List<Color>) {
                try {
                    val t = if (textContained.contains(text)) "$text*" else text
                    textContained.add(text)
                    if (colors.isNotEmpty()) {
                        val team = it.getTeam(t) ?: it.registerNewTeam(t)
                        var suffix = " "
                        colors.forEach { color ->
                            suffix += net.md_5.bungee.api.ChatColor.of(color.toHexString()).toString() + "●"
                        }
                        team.suffix = suffix
                        team.addEntry(t)
                    }
                    board.getScore(t).score = scoreIndex--
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (wait) {
                intersection(" §6☛ §l${area.station.name}", area.station.lines.filter { l -> l != area.line }.map { l -> l.color })
            } else {
                intersection(" §8▼ §7§o${area.station.name}", area.station.lines.filter { l -> l != area.line }.map { l -> l.color })
            }
            var first = true
            nextStations.forEach { station ->
                if (first) {
                    if (wait) {
                        intersection(" §f▽ §l${station.name}", station.lines.filter { l -> l != area.line }.map { l -> l.color })
                    } else {
                        intersection(" §6☛ §l${station.name}", station.lines.filter { l -> l != area.line }.map { l -> l.color })
                    }
                    first = false
                } else {
                    intersection(" §f▽ ${station.name}", station.lines.filter { l -> l != area.line }.map { l -> l.color })
                }
            }
            if (terminal != null) {
                if (dots && nextStations.size >= maxStations)
                    board.getScore("     §8...     ").score = scoreIndex--
                intersection(" ◇ §f§n${terminal.name}", terminal.lines.filter { l -> l != area.line }.map { l -> l.color })
            }

            minecart.passengers.forEach { p ->
                if (p is Player) {
                    p.scoreboard = it
                    PluginMain.INSTANCE.hasBoardPlayers.add(p)
                }
            }
        }
    }
}