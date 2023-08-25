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

fun openBlockListGui(session: PlayerGuiSession) {
    session.open(Gui.paginated().rows(6).title(Component.text("§7[§3§l牛腩传送§r§7]§r 已屏蔽的玩家")).create(), { type, gui, _ ->
        if (type == UpdateType.Init) {
            gui.setItem(6, 3, ItemBuilder.from("37aee9a75bf0df7897183015cca0b2a7d755c63388ff01752d5f4419fc645".toSkull())
                .name(Component.text("上一页")).asGuiItem { gui.previous() })
            gui.setItem(6, 7, ItemBuilder.from("682ad1b9cb4dd21259c0d75aa315ff389c3cef752be3949338164bac84a96e".toSkull())
                .name(Component.text("下一页")).asGuiItem { gui.next() })
            listOf(2,4,5,6,8).forEach { gui.setItem(6, it, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                .name(Component.text("")).asGuiItem()) }
            gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("关闭")).asGuiItem {
                session.back()
            })
            gui.setItem(6, 1, ItemBuilder.from("9a2d891c6ae9f6baa040d736ab84d48344bb6b70d7f1a280dd12cbac4d777".toSkull())
                .name(Component.text("添加玩家")).asGuiItem {
                    openOnlinePlayersGui(session, true) {
                        PluginMain.INSTANCE.block(session.player, it)
                        session.back()
                    }
            })
            gui.setDefaultClickAction { it.isCancelled = true }
        }
        gui.clearPageItems()
        PluginMain.INSTANCE.playerBlockSet[session.player.uniqueId]?.forEach {
            val player = Bukkit.getOfflinePlayer(it)
            gui.addItem(ItemBuilder.from(player.getSkull())
                .name(Component.text(player.name ?: "§8未知")).lore(Component.text("§6左键: 解除拉黑§r"))
                .asGuiItem {
                    PluginMain.INSTANCE.unblock(session.player, player)
                    session.refresh()
                })
        }
        true
    })
}