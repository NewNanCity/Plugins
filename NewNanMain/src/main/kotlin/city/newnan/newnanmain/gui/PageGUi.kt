package city.newnan.newnanmain.gui

import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.item.toSkull
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.PaginatedGui
import net.kyori.adventure.text.Component
import org.bukkit.Material

fun pageGui(session: PlayerGuiSession, title: Component): PaginatedGui {
    val gui = Gui.paginated()
        .title(title)
        .rows(6)
        .create()
    gui.setItem(6, 3, ItemBuilder.from("37aee9a75bf0df7897183015cca0b2a7d755c63388ff01752d5f4419fc645".toSkull())
        .name(Component.text("上一页")).asGuiItem { gui.previous() })
    gui.setItem(6, 7, ItemBuilder.from("682ad1b9cb4dd21259c0d75aa315ff389c3cef752be3949338164bac84a96e".toSkull())
        .name(Component.text("下一页")).asGuiItem { gui.next() })
    listOf(1,2,4,5,6,8).forEach { gui.setItem(6, it, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
        .name(Component.text("")).asGuiItem()) }
    gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text(if (session.length > 0) "返回" else "关闭")).asGuiItem {
        session.back()
    })
    gui.setDefaultClickAction { it.isCancelled = true }
    return gui
}