package city.newnan.railarea.gui

import city.newnan.railarea.config.RailLine
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import me.lucko.helper.Schedulers
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

fun showReverseGui(player: Player, line: RailLine, done: (reverse: Boolean?) -> Unit) {
    val gui = Gui.gui()
        .title(Component.text("选择方向"))
        .rows(6)
        .create()
    gui.setItem(1, 1, ItemBuilder.from(Material.NETHER_STAR)
        .name(Component.text("开往 ${line.stations.last().name} 方向")).asGuiItem {
            done(false)
            gui.close(player)
        })
    gui.setItem(1, 2, ItemBuilder.from(Material.NETHER_STAR)
        .name(Component.text("开往 ${line.stations.first().name} 方向")).asGuiItem {
            done(true)
            gui.close(player)
        })
    gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER)
        .name(Component.text("返回")).asGuiItem {
            done(null)
            gui.close(player)
        })
    gui.setDefaultClickAction { it.isCancelled = true }
    Schedulers.sync().runLater({ gui.open(player) }, 1)
}