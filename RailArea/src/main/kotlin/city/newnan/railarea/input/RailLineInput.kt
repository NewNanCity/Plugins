package city.newnan.railarea.input

import city.newnan.railarea.PluginMain
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.toColor
import city.newnan.railarea.config.toFMString
import city.newnan.railarea.config.toMaterial
import org.bukkit.Color
import org.bukkit.entity.Player

fun handleRailLineInput(player: Player, oldLine: RailLine?, done: (line: RailLine?) -> Unit) {
    if (lock) {
        PluginMain.INSTANCE.messageManager.printf(player, "&c你正在进行其他输入, 请先取消之!")
        done(null)
        return
    }
    var name: String? = oldLine?.name
    var color: Color? = oldLine?.color
    lock = true
    PluginMain.INSTANCE.messageManager.gets(player) { input ->
        val argv = input.split(" ").filter { it.isNotEmpty() }
        when (argv[0]) {
            "name" -> {
                if (argv.size < 2) {
                    PluginMain.INSTANCE.messageManager.printf(player, "&c请输入名称!")
                    return@gets false
                }
                val nameT = argv.subList(1, argv.size).joinToString(" ")
                if (PluginMain.INSTANCE.lines.containsKey(nameT)) {
                    PluginMain.INSTANCE.messageManager.printf(player, "&c线路 $nameT 已存在!")
                } else {
                    name = nameT
                    PluginMain.INSTANCE.messageManager.printf(player, "已设置名称: $nameT")
                }
            }
            "color" -> {
                if (argv.size < 2) {
                    PluginMain.INSTANCE.messageManager.printf(player, "&c请输入颜色 #RRGGBB 格式!")
                    return@gets false
                }
                if (!argv[1].startsWith("#")) {
                    PluginMain.INSTANCE.messageManager.printf(player, "&c颜色格式错误! 请使用 #RRGGBB 格式!")
                }
                try {
                    color = argv[1].toColor()
                    PluginMain.INSTANCE.messageManager.printf(player, "已设置颜色: ${color!!.toFMString()}${argv[1]}")
                } catch (e: Exception) {
                    PluginMain.INSTANCE.messageManager.printf(player, "&c颜色格式错误! 请使用 #RRGGBB 格式!")
                }
            }
            "cancel" -> {
                PluginMain.INSTANCE.messageManager.printf(player, "已取消")
                lock = false
                done(null)
                return@gets true
            }
            "ok" -> {
                if (color == null || name == null) {
                    PluginMain.INSTANCE.messageManager.printf(player, "&c请先设置名称和颜色!")
                } else {
                    lock = false
                    val line = RailLine(oldLine?.id ?: PluginMain.INSTANCE.nextLineId++, name!!,
                        oldLine?.stations ?: mutableListOf(), color!!, false, color!!.toMaterial())
                    done(line)
                    return@gets true
                }
            }
            else -> {
                PluginMain.INSTANCE.messageManager.printf(player, "&c未知指令! 你现在正处于线路设置模式，可用指令有: name, color, cancel, ok")
            }
        }
        false
    }
}