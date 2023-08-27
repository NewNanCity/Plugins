package city.newnan.newnanmain.input

import city.newnan.newnanmain.PluginMain
import city.newnan.newnanmain.TeleportPoint
import city.newnan.violet.gui.PlayerGuiSession

fun handleTeleportInput(session: PlayerGuiSession, oldPrefix: TeleportPoint? = null, setTeleport: (TeleportPoint?) -> Unit) {
    val player = session.player
    var x: Int? = oldPrefix?.x
    var y: Int? = oldPrefix?.y
    var z: Int? = oldPrefix?.z
    var world: String? = oldPrefix?.world
    var icon: String = oldPrefix?.icon ?: "GRASS_BLOCK"
    var name: String? = oldPrefix?.name
    var permission: String? = oldPrefix?.permission
    session.chatInput { input ->
        val argv = input.split(" ").filter { it.isNotEmpty() }
        when (argv[0]) {
            "location" -> {
                x = player.location.blockX
                y = player.location.blockY
                z = player.location.blockZ
                world = player.world.name
                PluginMain.INSTANCE.messageManager.printf(player, "§a已设置坐标为: §6$x, $y, $z §r世界: §6$world")
            }
            "name" -> {
                if (argv.size < 2) {
                    player.sendMessage("请输入新的地点名, 支持颜色码如 &c 或者 &#223344")
                } else {
                    name = argv.subList(1, argv.size).joinToString(" ")
                    PluginMain.INSTANCE.messageManager.printf(player, "§a已设置名称为: §6$name")
                }
            }
            "icon" -> {
                if (argv.size < 2) {
                    player.sendMessage("请输入新的图标, 支持物品名或者头颅贴图URL")
                } else {
                    icon = argv[1]
                    PluginMain.INSTANCE.messageManager.printf(player, "§a已设置图标!")
                }
            }
            "permission" -> {
                permission = if (argv.size < 2) null else argv[1]
                PluginMain.INSTANCE.messageManager.printf(player, "§a已设置权限为: §6$permission")
            }
            "cancel" -> {
                PluginMain.INSTANCE.messageManager.printf(player, "已取消")
                setTeleport(null)
                return@chatInput true
            }
            "ok" -> {
                if (name == null) {
                    PluginMain.INSTANCE.messageManager.printf(player, "§c请先设置传送点名称!")
                } else if (x == null || y == null || z == null || world == null) {
                    PluginMain.INSTANCE.messageManager.printf(player, "§c请先设置传送点坐标!")
                } else {
                    setTeleport(TeleportPoint(name!!, x!!, y!!, z!!, world!!, icon, permission))
                    return@chatInput true
                }
            }
            else -> {
                PluginMain.INSTANCE.messageManager.printf(player, "§c未知指令! 你现在正处于传送点编辑模式，可用指令有: name, location, icon, permission, cancel, ok")
            }
        }
        false
    }.also { result ->
        if (result) {
            session.player.sendMessage("你现在正处于传送点编辑模式，可用指令有: name, location, icon, permission, cancel, ok")
            session.hide()
        } else {
            PluginMain.INSTANCE.messageManager.printf(session.player, "§c已有其他内容待输入, 请取消之")
        }
    }
}