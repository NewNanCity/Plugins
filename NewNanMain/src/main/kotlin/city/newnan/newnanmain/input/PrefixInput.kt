package city.newnan.newnanmain.input

import city.newnan.newnanmain.PluginMain
import city.newnan.violet.gui.PlayerGuiSession

fun handlePrefixInput(session: PlayerGuiSession, oldPrefix: Pair<String, String>? = null, setPrefix: (Pair<String, String>?) -> Unit) {
    val player = session.player
    var key: String? = oldPrefix?.first
    var value: String? = oldPrefix?.second
    session.chatInput { input ->
        val argv = input.split(" ").filter { it.isNotEmpty() }
        when (argv[0]) {
            "name" -> {
                if (argv.size < 2) {
                    PluginMain.INSTANCE.messageManager.printf(player, "§c请输入名称!")
                } else {
                    key = argv[1]
                    PluginMain.INSTANCE.messageManager.printf(player, "§a已设置名称为: $key")
                }
            }
            "text" -> {
                if (argv.size < 2) {
                    player.sendMessage("请输入新的头衔内容, 支持颜色码如 &c 或者 &#223344")
                } else {
                    value = argv.subList(1, argv.size).joinToString(" ")
                    PluginMain.INSTANCE.messageManager.printf(player, "§a已设置内容为: $value")
                }
            }
            "cancel" -> {
                PluginMain.INSTANCE.messageManager.printf(player, "已取消")
                setPrefix(null)
                return@chatInput true
            }
            "ok" -> {
                if (key == null || value == null) {
                    PluginMain.INSTANCE.messageManager.printf(player, "§c请先设置名称和内容!")
                } else {
                    setPrefix(key!! to value!!)
                    return@chatInput true
                }
            }
            else -> {
                PluginMain.INSTANCE.messageManager.printf(player, "§c未知指令! 你现在正处于称号编辑模式，可用指令有: name, text, cancel, ok")
            }
        }
        false
    }.also { result ->
        if (result) {
            session.player.sendMessage("请输入新的头衔内容, 可用指令有: name, text, cancel, ok")
            session.hide()
        } else {
            PluginMain.INSTANCE.messageManager.printf(session.player, "§c已有其他内容待输入, 请取消之")
        }
    }
}