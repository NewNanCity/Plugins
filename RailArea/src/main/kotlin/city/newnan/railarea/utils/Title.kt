package city.newnan.railarea.utils

import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.Station
import city.newnan.railarea.config.toFMString
import org.bukkit.entity.Player

enum class RailTitleMode {
    UNDER_BOARD, ARRIVE, START,
}

fun Player.sendTitle(station: Station, line: RailLine, reverse: Boolean, mode: RailTitleMode, fadeInt: Int = 0, stay: Int = 20, fadeOut: Int = 0) {
    val i = line.stations.indexOf(station)
    if (i < 0) return
    val next =
        if (reverse) (if (i == 0) (if (line.isCycle) line.stations[line.stations.size - 1] else null) else line.stations[i - 1])
        else (if ((i + 1) >= line.stations.size) (if (line.isCycle) line.stations[0] else null) else line.stations[i + 1])
    fun linesToString(station: Station): String =
        station.lines.filter { it != line }.joinToString("§r§8,§r") { "${it.color.toFMString()}§l${it.name}" }
    when (mode) {
        RailTitleMode.UNDER_BOARD -> {
            var st = "开往 §9${if (reverse) line.stations.first().name else line.stations.last().name}§r 方向"
            st += if (next != null) "§8 | §r下一站: §6${next.name}§r" else "§8 | §r§6终点站"
            if (next != null && next.lines.size > 1) st += "§8 | §r下一站可换乘 ${linesToString(next)}"
            sendTitle("§e§b${station.name}§r", st, fadeInt, stay, fadeOut)
        }
        RailTitleMode.ARRIVE -> {
            var st = "开往 §9${if (reverse) line.stations.first().name else line.stations.last().name}§r 方向"
            st += if (next != null) "§8 | §r下一站: §6${next.name}§r" else "§8 | §r§6终点站"
            if (station.lines.size > 1) st += "§8 | §r可换乘 ${linesToString(station)}"
            sendTitle("§b${station.name}§r §7到了", st, fadeInt, stay, fadeOut)
        }
        RailTitleMode.START -> {
            if (next == null) return
            var st = "开往 §9${if (reverse) line.stations.first().name else line.stations.last().name}§r 方向"
            if (next.lines.size > 1) st += "§8 | §r可换乘 ${linesToString(next)}"
            sendTitle("§7下一站: §r§b${next.name}§r", st, fadeInt, stay, fadeOut)
        }
    }
}