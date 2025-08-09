package city.newnan.railarea.gui.input

import city.newnan.gui.dsl.chatInput
import city.newnan.railarea.RailAreaPlugin
import city.newnan.railarea.config.Station
import city.newnan.railarea.i18n.LanguageKeys
import org.bukkit.entity.Player

/**
 * 站点创建/编辑输入处理器
 *
 * 处理站点的创建和编辑操作
 *
 * @param plugin 插件实例
 * @param player 玩家
 * @param oldStation 要编辑的站点(null表示创建新站点)
 * @param setStation 回调函数，接收站点结果
 */
fun handleStationInput(
    plugin: RailAreaPlugin,
    player: Player,
    oldStation: Station?,
    setStation: (station: Station?) -> Unit
) {
    var name: String? = oldStation?.name

    fun help() {
        plugin.messager.printf(player, LanguageKeys.Input.Station.HELP)
    }

    val result = plugin.chatInput(player, hide = true) { input ->
        val argv = input.split(" ").filter { it.isNotEmpty() }
        when (argv[0].lowercase()) {
            "name" -> {
                if (argv.size < 2) {
                    plugin.messager.printf(player, LanguageKeys.Input.RailLine.NAME_REQUIRED)
                    return@chatInput false
                }
                val nameT = argv.subList(1, argv.size).joinToString(" ")
                if (plugin.stationStorage.getStationByName(nameT) != null) {
                    plugin.messager.printf(player, LanguageKeys.Input.Station.NAME_EXISTS, nameT)
                } else {
                    name = nameT
                    plugin.messager.printf(player, LanguageKeys.Input.Station.NAME_SET, nameT)
                }
                false
            }
            "cancel" -> {
                plugin.messager.printf(player, LanguageKeys.Input.Common.CANCELLED)
                setStation(null)
                true
            }
            "ok" -> {
                if (name == null) {
                    plugin.messager.printf(player, LanguageKeys.Input.Station.NAME_REQUIRED)
                    false
                } else {
                    try {
                        setStation(if (oldStation != null) {
                            oldStation.copy(name = name)
                        } else {
                            Station(
                                id = -1,
                                name = name,
                                storage = plugin.stationStorage
                            )
                        })
                        true
                    } catch (e: Exception) {
                        plugin.logger.error("Failed to save station", e)
                        plugin.messager.printf(player, LanguageKeys.Input.Station.SAVE_FAILED, e.message ?: "Unknown error")
                        false
                    }
                }
            }
            else -> {
                plugin.messager.printf(player, LanguageKeys.Input.Common.UNKNOWN_COMMAND)
                help()
                false
            }
        }
    }

    if (result) {
        if (oldStation != null) {
            plugin.messager.printf(player, LanguageKeys.Input.Station.EDIT_PROMPT, oldStation.name)
        } else {
            plugin.messager.printf(player, LanguageKeys.Input.Station.PROMPT)
        }
    } else {
        plugin.messager.printf(player, LanguageKeys.Input.Common.CHAT_INPUT_BUSY)
        setStation(null)
    }
}
