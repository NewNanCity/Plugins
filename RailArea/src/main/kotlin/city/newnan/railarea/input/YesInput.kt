package city.newnan.railarea.input

import city.newnan.railarea.PluginMain
import org.bukkit.entity.Player

fun handleYesInput(player: Player, done: (yes: Boolean) -> Unit) {
    if (lock) {
        PluginMain.INSTANCE.messageManager.printf(player, "&c你正在进行其他输入, 请先取消之!")
        done(false)
        return
    }
    lock = true
    PluginMain.INSTANCE.messageManager.gets(player) { input ->
        PluginMain.INSTANCE.messageManager.printf(player, "已取消")
        lock = false
        done(input == "Y")
        true
    }
}