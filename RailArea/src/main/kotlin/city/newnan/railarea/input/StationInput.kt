package city.newnan.railarea.input

import city.newnan.railarea.PluginMain
import city.newnan.railarea.config.Station
import org.bukkit.entity.Player

fun handleStationInput(player: Player, oldStation: Station?, done: (station: Station?) -> Unit) {
    var name: String? = oldStation?.name
    if (lock) {
        PluginMain.INSTANCE.messageManager.printf(player, "&c你正在进行其他输入, 请先取消之!")
        done(null)
        return
    }
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
                if (PluginMain.INSTANCE.stations.containsKey(nameT)) {
                    PluginMain.INSTANCE.messageManager.printf(player, "&c站点 $nameT 已存在!")
                } else {
                    name = nameT
                    PluginMain.INSTANCE.messageManager.printf(player, "已设置名称: $nameT")
                }
            }
            "cancel" -> {
                PluginMain.INSTANCE.messageManager.printf(player, "已取消")
                lock = false
                done(null)
                return@gets true
            }
            "ok" -> {
                if (name == null) {
                    PluginMain.INSTANCE.messageManager.printf(player, "&c请先设置名称!")
                } else {
                    val station = Station(oldStation?.id ?: PluginMain.INSTANCE.nextStationId++, name!!,
                        oldStation?.lines ?: mutableSetOf())
                    lock = false
                    done(station)
                    return@gets true
                }
            }
            else -> {
                PluginMain.INSTANCE.messageManager.printf(player, "&c未知指令! 你现在正处于站点设置模式，可用指令有: name, cancel, ok")
            }
        }
        false
    }
}