package city.newnan.tpa.gui

import city.newnan.tpa.PluginMain
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

fun openOnlinePlayersGui(session: PlayerGuiSession, showBlockIcon: Boolean, setPlayer: (OfflinePlayer) -> Unit) {
    session.open(Gui.paginated().rows(6).title(Component.text("§7[§3§l牛腩传送§r§7]§r 选择在线的玩家")).create(), { type, gui, _ ->
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
            if (showBlockIcon) gui.setItem(6, 1, ItemBuilder.from(Material.PLAYER_HEAD)
                .name(Component.text("没有你想找的?"))
                .lore(Component.text("§7这里只展示在线的玩家,"), Component.text("§7如果你想找离线的玩家,"),
                    Component.text("§7请使用 §f/tpa block §a<玩家名> §7指令"))
                .asGuiItem {})
            gui.setDefaultClickAction { it.isCancelled = true }
        }
        gui.clearPageItems()
        Bukkit.getOnlinePlayers()
            .filter { it.uniqueId != session.player.uniqueId
                    && PluginMain.INSTANCE.playerBlockSet[session.player.uniqueId]?.contains(it.uniqueId) != true }
            .forEach { player ->
                gui.addItem(ItemBuilder.from(player.getSkull())
                    .name(Component.text(player.name))
                    .lore(Component.text("§6左键: 屏蔽玩家§r"))
                    .asGuiItem { setPlayer(player) })
            }
        true
    })
}