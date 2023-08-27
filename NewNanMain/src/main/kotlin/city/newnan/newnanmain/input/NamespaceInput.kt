package city.newnan.newnanmain.input

import city.newnan.newnanmain.PluginMain
import city.newnan.violet.gui.PlayerGuiSession

fun handleNamespaceInput(session: PlayerGuiSession, oldNamespace: String? = null, setNamespace: (String?) -> Unit) {
    val player = session.player
    var namespace: String? = oldNamespace
    session.chatInput { input ->
        val argv = input.split(" ").filter { it.isNotEmpty() }
        when (argv[0]) {
            "name" -> {
                if (argv.size < 2) {
                    PluginMain.INSTANCE.messageManager.printf(player, "§c请输入名称!")
                } else {
                    namespace = argv[1]
                    PluginMain.INSTANCE.messageManager.printf(player, "§a已设置名称为: $namespace")
                }
            }
            "cancel" -> {
                PluginMain.INSTANCE.messageManager.printf(player, "已取消")
                setNamespace(null)
                return@chatInput true
            }
            "ok" -> {
                if (namespace == null) {
                    PluginMain.INSTANCE.messageManager.printf(player, "§c请先设置名称!")
                } else {
                    setNamespace(namespace)
                    return@chatInput true
                }
            }
            else -> {
                PluginMain.INSTANCE.messageManager.printf(player, "§c未知指令! 你现在正处于称号组编辑模式，可用指令有: name, cancel, ok")
            }
        }
        false
    }.also { result ->
        if (result) {
            session.player.sendMessage("请输称号组名称, 可用指令有: name, cancel, ok")
            session.hide()
        } else {
            PluginMain.INSTANCE.messageManager.printf(session.player, "§c已有其他内容待输入, 请取消之")
        }
    }
}