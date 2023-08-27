package city.newnan.newnanmain.gui

import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.gui.UpdateType
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import net.kyori.adventure.text.Component
import org.bukkit.Material

fun openAdminGui(session: PlayerGuiSession) {
    var next = 0L
    fun debounce(call: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now < next) return
        next = now + 5000L
        call()
    }

    session.open(Gui.gui().rows(6).title(Component.text("§7[§3§l牛腩小镇§r§7]§r 管理界面")).create(), { type, gui, _ ->
        if (type != UpdateType.Init) return@open false
        gui.setDefaultClickAction { it.isCancelled = true }
        for (row in 1..6) for (col in 1..9) {
            gui.setItem(row, col, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                .name(Component.text("")).asGuiItem())
        }
        gui.setItem(1, 1, ItemBuilder.from(Material.COMPASS)
            .name(Component.text("§r§f传送系统")).asGuiItem {
                debounce { openTeleportGui(session, true) }
            }
        )
        gui.setItem(1, 2, ItemBuilder.from(Material.NAME_TAG)
            .name(Component.text("§r§f称号系统")).asGuiItem {
                debounce { openGlobalPrefixNamespacesGui(session) }
            }
        )
        gui.setItem(1, 3, ItemBuilder.from(Material.WRITTEN_BOOK)
            .name(Component.text("§r§f牛腩书局")).asGuiItem {
                debounce { session.player.performCommand("book admin") }
            }
        )
        gui.setItem(1, 4, ItemBuilder.from(Material.WOODEN_AXE)
            .name(Component.text("§r§f创造区")).asGuiItem {
                debounce {
                    session.player.performCommand("createarea gui")
                    session.back()
                }
            }
        )
        gui.setItem(1, 5, ItemBuilder.from(Material.RAIL)
            .name(Component.text("§r§f铁路系统")).asGuiItem {
                debounce { session.player.performCommand("rail") }
            }
        )
        gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text(if (session.length > 0) "返回" else "关闭")).asGuiItem {
            session.back()
        })
        true
    })
}