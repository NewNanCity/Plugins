package city.newnan.railarea.gui

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.PaginatedGui
import net.kyori.adventure.text.Component
import org.bukkit.Material

fun pageGui(title: Component): PaginatedGui {
    val gui = Gui.paginated()
        .title(title)
        .rows(6)
        .create()
    gui.setItem(6, 3, ItemBuilder.from(Material.PAPER).name(Component.text("上一页")).asGuiItem {
        gui.previous()
    })
    gui.setItem(6, 7, ItemBuilder.from(Material.PAPER).name(Component.text("下一页")).asGuiItem {
        gui.next()
    })
    listOf(1,2,4,5,6,8).forEach { gui.setItem(6, it, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
        .name(Component.text("")).asGuiItem()) }
    gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("关闭")).asGuiItem {
        gui.close(it.whoClicked)
    })
    gui.setDefaultClickAction { it.isCancelled = true }
    return gui
}