package city.newnan.railarea.utils

import city.newnan.railarea.config.RailArea
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.Station
import city.newnan.railarea.config.toFMString
import org.bukkit.entity.Player

enum class RailTitleMode {
    UNDER_BOARD, ARRIVE, START,
}

fun Player.sendTitle(area: RailArea, mode: RailTitleMode, fadeInt: Int = 0, stay: Int = 20, fadeOut: Int = 0) {
    val next = area.nextStation
    fun linesToString(station: Station): String =
        station.lines.filter { it != area.line }.joinToString("§r§8,§r") { "${it.color.toFMString()}§l${it.name}" }
    when (mode) {
        RailTitleMode.UNDER_BOARD -> {
            var st = "开往 §9${if (area.reverse) area.line.stations.first().name else area.line.stations.last().name}§r 方向"
            st += if (next != null) "§8 | §r下一站: §6${next.name}§r" else "§8 | §r§6终点站"
            sendTitle("§e§b${area.station.name}§r", st, fadeInt, stay, fadeOut)
        }
        RailTitleMode.ARRIVE -> {
            var st = "开往 §9${if (area.reverse) area.line.stations.first().name else area.line.stations.last().name}§r 方向"
            if (area.station.lines.size > 1) st += "§8 | §r可换乘 ${linesToString(area.station)}"
            st += if (next != null) "§8 | §r下一站: §6${next.name}§r" else "§8 | §r§6终点站"
            sendTitle("§b${area.station.name}§r §7到了", st, fadeInt, stay, fadeOut)
        }
        RailTitleMode.START -> {
            if (next == null) return
            var st = "开往 §9${if (area.reverse) area.line.stations.first().name else area.line.stations.last().name}§r 方向"
            if (next.lines.size > 1) st += "§8 | §r可换乘 ${linesToString(next)}"
            sendTitle("§7下一站: §r§b${next.name}§r", st, fadeInt, stay, fadeOut)
        }
    }
}