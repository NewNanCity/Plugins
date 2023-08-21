package city.newnan.railarea.input

import city.newnan.railarea.PluginMain
import org.bukkit.entity.Player

fun handleYesInput(player: Player, done: (yes: Boolean) -> Unit) {
    if (inputLocks.contains(player.uniqueId)) {
        PluginMain.INSTANCE.messageManager.printf(player, "§c你正在进行其他输入, 请先取消之!")
        done(false)
        return
    }
    inputLocks.add(player.uniqueId)
    PluginMain.INSTANCE.messageManager.gets(player) { input ->
        PluginMain.INSTANCE.messageManager.printf(player, "已取消")
        inputLocks.remove(player.uniqueId)
        done(input == "Y")
        true
    }
}