package city.newnan.railarea.gui

import city.newnan.railarea.config.RailLine
import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.gui.UpdateType
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import net.kyori.adventure.text.Component
import org.bukkit.Material

fun openReverseGui(session: PlayerGuiSession, line: RailLine, setReverse: (Boolean) -> Unit) {
    session.open(Gui.gui().title(Component.text("§7[§3§l牛腩轨道交通§r§7]§r 选择方向")).rows(6).create(), { type, gui, _ ->
        if (type == UpdateType.Init) {
            gui.setItem(1, 1, ItemBuilder.from(Material.NETHER_STAR)
                .name(Component.text("开往 ${line.stations.last().name} 方向")).asGuiItem {
                    setReverse(false)
                })
            gui.setItem(1, 2, ItemBuilder.from(Material.NETHER_STAR)
                .name(Component.text("开往 ${line.stations.first().name} 方向")).asGuiItem {
                    setReverse(true)
                })
            gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER)
                .name(Component.text("返回")).asGuiItem { session.back() })
            gui.setDefaultClickAction { it.isCancelled = true }
            return@open true
        }
        false
    })
}