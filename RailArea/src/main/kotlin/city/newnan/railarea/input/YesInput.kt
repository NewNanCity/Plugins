package city.newnan.railarea.input

import city.newnan.railarea.PluginMain
import city.newnan.violet.gui.PlayerGuiSession

fun handleYesInput(session: PlayerGuiSession, text: String, setYes: (yes: Boolean) -> Unit) {
    val player = session.player
    session.chatInput { input ->
        PluginMain.INSTANCE.messageManager.printf(player, "已取消")
        setYes(input == "Y")
        true
    }.also {
        if (it) {
            PluginMain.INSTANCE.messageManager.printf(player, text)
        } else {
            PluginMain.INSTANCE.messageManager.printf(player, "§c你正在进行其他输入, 请先取消之!")
            setYes(false)
        }
    }
}