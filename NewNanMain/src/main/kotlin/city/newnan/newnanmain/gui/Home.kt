package city.newnan.newnanmain.gui

import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.gui.UpdateType
import city.newnan.violet.item.toSkull
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material

fun openHomeGui(session: PlayerGuiSession) {
    var next = 0L
    fun debounce(call: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now < next) return
        next = now + 5000L
        call()
    }

    session.open(Gui.gui().rows(6).title(Component.text("§7[§3§l牛腩小镇§r§7]§r 主页")).create(), { type, gui, _ ->
        if (type != UpdateType.Init) return@open false
        gui.setDefaultClickAction { it.isCancelled = true }
        for (row in 1..6) for (col in 1..9) {
            gui.setItem(row, col, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                .name(Component.text("")).asGuiItem())
        }
        gui.setItem(2, 2, ItemBuilder.from(Material.COMPASS)
            .name(Component.text("§r§f传送")).asGuiItem {
                debounce { openTeleportGui(session) }
            }
        )
        gui.setItem(2, 4, ItemBuilder.from(Material.NAME_TAG)
            .name(Component.text("§r§f称号")).asGuiItem {
                debounce { openPlayerPrefixGui(session, session.player) }
            }
        )
        gui.setItem(2, 6, ItemBuilder.from(Material.FIREWORK_ROCKET)
            .name(Component.text("§r§f飞行")).asGuiItem {
                debounce {
                    session.player.performCommand("fly")
                    session.back()
                }
            }
        )
        gui.setItem(2, 8, ItemBuilder.from(Material.ENDER_PEARL)
            .name(Component.text("§r§fTPA")).asGuiItem {
                debounce { session.player.performCommand("tpa") }
            }
        )
        gui.setItem(3, 3, ItemBuilder.from(Material.WRITABLE_BOOK)
            .name(Component.text("§r§f牛腩书局")).asGuiItem {
                debounce { session.player.performCommand("book") }
            }
        )

        gui.setItem(3, 5, ItemBuilder.from(Material.WOODEN_AXE)
            .name(Component.text("§r§f创造区")).asGuiItem {
                debounce {
                    session.player.performCommand("ctp")
                    session.back()
                }
            }
        )
        gui.setItem(3, 7, ItemBuilder.from(Material.EMERALD)
            .name(Component.text("§r§f慈善榜")).asGuiItem {
                debounce { session.player.performCommand("ftop") }
            }
        )
        gui.setItem(4, 2, ItemBuilder.from(Material.OAK_SIGN)
            .name(Component.text("§r§f成就")).asGuiItem {
                debounce { session.player.performCommand("aach list") }
            }
        )
        gui.setItem(4, 4, ItemBuilder.from("cf7cdeefc6d37fecab676c584bf620832aaac85375e9fcbff27372492d69f".toSkull())
            .name(Component.text("§r§f§f小镇")).asGuiItem {
                debounce { session.player.performCommand("town") }
            }
        )
        gui.setItem(4, 6, ItemBuilder.from(Material.WRITTEN_BOOK)
            .name(Component.text("§r§f新人指南")).asGuiItem {
                debounce {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "book open ${session.player.name} b7cc4a26-ab98-400c-bd89-809ea964d2a4")
                }
            }
        )
        gui.setItem(4, 8, ItemBuilder.from(Material.TNT)
            .name(Component.text("§r§f熊服查询CO I")).asGuiItem {
                debounce {
                    session.player.performCommand("co i")
                    session.back()
                }
            }
        )
        if (session.player.hasPermission("newnan.admin")) {
            gui.setItem(6, 8, ItemBuilder.from(Material.COMMAND_BLOCK)
                .name(Component.text("§r§f管理")).asGuiItem {
                    debounce { openAdminGui(session) }
                }
            )
        }
        gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text(if (session.length > 0) "返回" else "关闭")).asGuiItem {
            session.back()
        })
        true
    })
}