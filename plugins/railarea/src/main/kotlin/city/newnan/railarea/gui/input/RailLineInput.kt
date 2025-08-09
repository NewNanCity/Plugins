package city.newnan.railarea.gui.input

import city.newnan.gui.dsl.chatInput
import city.newnan.railarea.RailAreaPlugin
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.parseHexColor
import city.newnan.railarea.config.toHexString
import city.newnan.railarea.config.toMaterial
import city.newnan.railarea.i18n.LanguageKeys
import org.bukkit.Color
import org.bukkit.entity.Player

/**
 * 线路创建/编辑输入处理器
 *
 * 处理线路的创建和编辑操作，支持完整的线路属性设置
 *
 * @param plugin 插件实例
 * @param player 玩家
 * @param oldLine 要编辑的线路(null表示创建新线路)
 * @param onLineUpdate 回调函数，接收线路结果
 */
fun handleRailLineInput(
    plugin: RailAreaPlugin,
    player: Player,
    oldLine: RailLine?,
    onLineUpdate: (line: RailLine?) -> Unit
) {
    var name: String? = oldLine?.name
    var color: Color? = oldLine?.color

    fun help() {
        plugin.messager.printf(player, LanguageKeys.Input.RailLine.HELP)
    }

    help()
    val result = plugin.chatInput(player, hide = true) { input ->
        val argv = input.split(" ").filter { it.isNotEmpty() }
        when (argv[0].lowercase()) {
            "name" -> {
                if (argv.size < 2) {
                    plugin.messager.printf(player, LanguageKeys.Input.RailLine.NAME_REQUIRED)
                    return@chatInput false
                }
                val nameT = argv.subList(1, argv.size).joinToString(" ")
                if (plugin.stationStorage.getLineByName(nameT) != null) {
                    plugin.messager.printf(player, LanguageKeys.Input.RailLine.NAME_EXISTS, nameT)
                } else {
                    name = nameT
                    plugin.messager.printf(player, LanguageKeys.Input.RailLine.NAME_SET, nameT)
                }
                false
            }
            "color" -> {
                if (argv.size < 2) {
                    plugin.messager.printf(player, LanguageKeys.Input.RailLine.COLOR_REQUIRED)
                    return@chatInput false
                }
                if (!argv[1].startsWith("#")) {
                    plugin.messager.printf(player, LanguageKeys.Input.RailLine.COLOR_FORMAT_ERROR)
                    return@chatInput false
                }
                try {
                    color = argv[1].parseHexColor()
                    plugin.messager.printf(player, LanguageKeys.Input.RailLine.COLOR_SET, color.toHexString(), argv[1])
                } catch (e: Exception) {
                    plugin.messager.printf(player, LanguageKeys.Input.RailLine.COLOR_FORMAT_ERROR)
                }
                false
            }
            "cancel" -> {
                plugin.messager.printf(player, LanguageKeys.Input.Common.CANCELLED)
                onLineUpdate(null)
                true
            }
            "ok" -> {
                if (color == null || name == null) {
                    plugin.messager.printf(player, LanguageKeys.Input.RailLine.NAME_COLOR_REQUIRED)
                    false
                } else {
                    onLineUpdate(if (oldLine != null) {
                        oldLine.copy(
                            name = name,
                            color = color,
                            colorMaterial = color.toMaterial()
                        )
                    } else {
                        RailLine(
                            id = -1,
                            name = name,
                            color = color,
                            isCycle = false,
                            colorMaterial = color.toMaterial(),
                            leftReturn = false,
                            rightReturn = false,
                            stationIds = emptyList(),
                            storage = plugin.stationStorage
                        )
                    })
                    true
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
        plugin.messager.printf(player, LanguageKeys.Input.RailLine.PROMPT)
    } else {
        plugin.messager.printf(player, LanguageKeys.Input.Common.CHAT_INPUT_BUSY)
        onLineUpdate(null)
    }
}
