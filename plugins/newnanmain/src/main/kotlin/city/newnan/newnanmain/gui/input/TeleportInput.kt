package city.newnan.newnanmain.gui.input

import city.newnan.gui.dsl.chatInput
import city.newnan.newnanmain.NewNanMainPlugin
import city.newnan.newnanmain.config.TeleportPoint
import city.newnan.newnanmain.i18n.LanguageKeys
import org.bukkit.entity.Player

/**
 * 传送点创建/编辑输入处理器
 *
 * 处理传送点的创建和编辑操作
 *
 * @param plugin 插件实例
 * @param player 玩家
 * @param oldPoint 要编辑的传送点(null表示创建新传送点)
 * @param setPoint 回调函数，接收传送点结果
 */
fun handleTeleportInput(
    plugin: NewNanMainPlugin,
    player: Player,
    oldPoint: TeleportPoint?,
    setPoint: (point: TeleportPoint?) -> Unit
) {
    var name: String? = oldPoint?.name
    var x: Int? = oldPoint?.x
    var y: Int? = oldPoint?.y
    var z: Int? = oldPoint?.z
    var world: String? = oldPoint?.world
    var icon: String = oldPoint?.icon ?: "GRASS_BLOCK"
    var permission: String? = oldPoint?.permission

    fun help() {
        plugin.messager.printf(player, LanguageKeys.Input.Teleport.HELP)
    }

    val result = plugin.chatInput(player, hide = true) { input ->
        val argv = input.split(" ").filter { it.isNotEmpty() }
        when (argv[0].lowercase()) {
            "name" -> {
                if (argv.size < 2) {
                    plugin.messager.printf(player, LanguageKeys.Input.Teleport.NAME_REQUIRED)
                    help()
                    return@chatInput false
                }
                val nameT = argv.subList(1, argv.size).joinToString(" ")
                val teleportManager = plugin.teleportManager
                if (teleportManager.getAllTeleportPoints().any { it.name == nameT && it.name != oldPoint?.name }) {
                    plugin.messager.printf(player, LanguageKeys.Input.Teleport.NAME_EXISTS, nameT)
                } else {
                    name = nameT
                    plugin.messager.printf(player, LanguageKeys.Input.Teleport.NAME_SET, nameT)
                }
                false
            }
            "location" -> {
                x = player.location.blockX
                y = player.location.blockY
                z = player.location.blockZ
                world = player.world.name
                plugin.messager.printf(player, LanguageKeys.Input.Teleport.LOCATION_SET, x!!, y!!, z!!, world!!)
                false
            }
            "icon" -> {
                if (argv.size < 2) {
                    plugin.messager.printf(player, LanguageKeys.Input.Teleport.ICON_REQUIRED)
                    help()
                    return@chatInput false
                }
                icon = argv[1]
                plugin.messager.printf(player, LanguageKeys.Input.Teleport.ICON_SET, icon)
                false
            }
            "permission" -> {
                permission = if (argv.size < 2) null else argv[1]
                plugin.messager.printf(player, LanguageKeys.Input.Teleport.PERMISSION_SET, permission ?: "无")
                false
            }
            "cancel" -> {
                plugin.messager.printf(player, LanguageKeys.Input.Common.CANCELLED)
                setPoint(null)
                true
            }
            "ok" -> {
                if (name == null) {
                    plugin.messager.printf(player, LanguageKeys.Input.Teleport.NAME_REQUIRED)
                    help()
                    false
                } else if (x == null || y == null || z == null || world == null) {
                    plugin.messager.printf(player, LanguageKeys.Input.Teleport.LOCATION_REQUIRED)
                    help()
                    false
                } else {
                    try {
                        setPoint(TeleportPoint(
                            name = name,
                            x = x,
                            y = y,
                            z = z,
                            world = world,
                            icon = icon,
                            permission = permission
                        ))
                        true
                    } catch (e: Exception) {
                        plugin.logger.error("Failed to save teleport point", e)
                        plugin.messager.printf(player, LanguageKeys.Input.Teleport.SAVE_FAILED, e.message ?: "Unknown error")
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
        if (oldPoint != null) {
            plugin.messager.printf(player, LanguageKeys.Input.Teleport.EDIT_PROMPT, oldPoint.name)
        } else {
            plugin.messager.printf(player, LanguageKeys.Input.Teleport.PROMPT)
        }
        // 在开始时显示帮助信息
        help()
    } else {
        plugin.messager.printf(player, LanguageKeys.Input.Common.CHAT_INPUT_BUSY)
        setPoint(null)
    }
}
