package city.newnan.betterbook.gui

import city.newnan.betterbook.book.Librarian
import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.gui.UpdateType
import city.newnan.violet.item.getSkull
import city.newnan.violet.item.toSkull
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer

fun openOnlinePlayersGui(session: PlayerGuiSession, setPlayer: (OfflinePlayer) -> Unit) {
    session.open(Gui.paginated().rows(6).title(Component.text("§7[§3§l牛腩书局§r§7]§r 书局成员")).create(), { type, gui, _ ->
        if (type == UpdateType.Init) {
            gui.setItem(6, 3, ItemBuilder.from("37aee9a75bf0df7897183015cca0b2a7d755c63388ff01752d5f4419fc645".toSkull())
                .name(Component.text("上一页")).asGuiItem { gui.previous() })
            gui.setItem(6, 7, ItemBuilder.from("682ad1b9cb4dd21259c0d75aa315ff389c3cef752be3949338164bac84a96e".toSkull())
                .name(Component.text("下一页")).asGuiItem { gui.next() })
            listOf(1,2,4,5,6,8).forEach { gui.setItem(6, it, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                .name(Component.text("")).asGuiItem()) }
            gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("返回")).asGuiItem {
                session.back()
            })
            gui.setDefaultClickAction { it.isCancelled = true }
        }
        gui.clearPageItems()
        Librarian.playerBooks.forEach { (uuid, _) ->
            val player = Bukkit.getOfflinePlayer(uuid)
            gui.addItem(ItemBuilder.from(player.getSkull())
                .name(Component.text(player.name ?: "§8未知"))
                .asGuiItem { setPlayer(player) })
        }
        true
    })
}