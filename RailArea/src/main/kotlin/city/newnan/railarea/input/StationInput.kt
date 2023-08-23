package city.newnan.railarea.input

import city.newnan.railarea.PluginMain
import city.newnan.railarea.config.Station
import city.newnan.violet.gui.PlayerGuiSession

fun handleStationInput(session: PlayerGuiSession, oldStation: Station?, setStation: (station: Station?) -> Unit) {
    val player = session.player
    var name: String? = oldStation?.name
    session.chatInput { input ->
        val argv = input.split(" ").filter { it.isNotEmpty() }
        when (argv[0]) {
            "name" -> {
                if (argv.size < 2) {
                    PluginMain.INSTANCE.messageManager.printf(player, "§c请输入名称!")
                    return@chatInput false
                }
                val nameT = argv.subList(1, argv.size).joinToString(" ")
                if (PluginMain.INSTANCE.stations.containsKey(nameT)) {
                    PluginMain.INSTANCE.messageManager.printf(player, "§c站点 $nameT 已存在!")
                } else {
                    name = nameT
                    PluginMain.INSTANCE.messageManager.printf(player, "已设置名称: $nameT")
                }
            }
            "cancel" -> {
                PluginMain.INSTANCE.messageManager.printf(player, "已取消")
                setStation(null)
                return@chatInput true
            }
            "ok" -> {
                if (name == null) {
                    PluginMain.INSTANCE.messageManager.printf(player, "§c请先设置名称!")
                } else {
                    val station = Station(oldStation?.id ?: PluginMain.INSTANCE.nextStationId++, name!!,
                        oldStation?.lines ?: mutableSetOf())
                    setStation(station)
                    return@chatInput true
                }
            }
            else -> {
                PluginMain.INSTANCE.messageManager.printf(player, "§c未知指令! 你现在正处于站点设置模式，可用指令有: name, cancel, ok")
            }
        }
        false
    }.also {
        if (it) {
            if (oldStation != null) {
                PluginMain.INSTANCE.messageManager.printf(player, "开始设置站点 &2${oldStation.name}&r，接下来请设定站点的属性:")
            } else {
                PluginMain.INSTANCE.messageManager.printf(player, "&c请输入站点名称")
            }
        } else {
            PluginMain.INSTANCE.messageManager.printf(player, "§c你正在进行其他输入, 请先取消之!")
            setStation(null)
        }
    }
}