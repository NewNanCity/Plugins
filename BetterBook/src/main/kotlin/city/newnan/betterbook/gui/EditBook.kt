package city.newnan.betterbook.gui

import city.newnan.betterbook.PluginMain
import city.newnan.betterbook.book.*
import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.gui.UpdateType
import city.newnan.violet.item.toSkull
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import me.lucko.helper.Schedulers
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import java.util.*

fun openEditBookGui(session: PlayerGuiSession, targetId: UUID, oldBook: Pair<UUID, Book>? = null, setBook: (Book) -> Unit) {
    var finish = false
    session.open(Gui.storage().rows(6)
        .title(Component.text("§7[§3§l牛腩书局§r§7]§r ${if (oldBook == null) "录入新书" else "编辑书目: §0§l${oldBook.second.title}"}"))
        .create(), { type, gui, _ ->
            if (type == UpdateType.Init) {
                fun update() {
                    var allowUpdate = false
                    val item = gui.inventory.getItem(24)
                    if (item == null) {
                        gui.setItem(4, 5, ItemBuilder.from(Material.ANVIL)
                            .lore(
                                Component.text("§f请将编辑好内容的[书与笔]或[成书]"),
                                Component.text("§f放在右侧一格,然后再点击铁砧上传!"),
                            )
                            .name(Component.text("§c无法上传")).asGuiItem())
                    } else if ((item.type != Material.WRITABLE_BOOK && item.type != Material.WRITTEN_BOOK) || item.itemMeta == null) {
                        gui.setItem(4, 5, ItemBuilder.from(Material.ANVIL)
                            .lore(Component.text("§f类型有误,请放入[书与笔]或[成书]!"))
                            .name(Component.text("§c无法上传")).asGuiItem())
                    } else {
                        val bookId = item.findBookUUID(written = true, writable = true)
                        val inPlaceBook = if (bookId != null) Librarian[bookId, false] else null
                        if (inPlaceBook != null) {
                            if (oldBook == null) {
                                gui.setItem(4, 5, ItemBuilder.from(Material.ANVIL)
                                    .lore(
                                        Component.text("§f待上传的书是已存在于牛腩书局中的另一本书:"),
                                        Component.text("  标题: §3${inPlaceBook.title}"),
                                        Component.text("  作者: §3${Bukkit.getOfflinePlayer(inPlaceBook.creator).name ?: "§8未知"}"),
                                        Component.text("  创建时间: §3${dateFormatter.format(inPlaceBook.created)}"),
                                        Component.text("§f的可编辑版本, 如你希望复用那本书,"),
                                        Component.text("§f请先征得其作者的同意并获取其书本原件!")
                                    )
                                    .name(Component.text("§c无法上传")).asGuiItem())
                            } else if (oldBook.first != bookId) {
                                gui.setItem(4, 5, ItemBuilder.from(Material.ANVIL)
                                    .lore(
                                        Component.text("§f待上传的书是已存在于牛腩书局中的另一本书:"),
                                        Component.text("  标题: §3${inPlaceBook.title}"),
                                        Component.text("  作者: §3${Bukkit.getOfflinePlayer(inPlaceBook.creator).name ?: "§8未知"}"),
                                        Component.text("  创建时间: §3${dateFormatter.format(inPlaceBook.created)}"),
                                        Component.text("§f的可编辑版本, 如你希望用那本书的内容覆盖这本书,"),
                                        Component.text("§f请先征得其作者的同意并获取其书本原件!")
                                    )
                                    .name(Component.text("§c无法上传")).asGuiItem())
                            } else allowUpdate = true
                        } else allowUpdate = true
                        if (allowUpdate) {
                            gui.setItem(4, 5, ItemBuilder.from(Material.ANVIL)
                                .lore(Component.text("§f点击铁砧, ${if (oldBook == null) "将书上传至牛腩书局" else "更新原来的内容"}!"))
                                .name(Component.text("§a上传")).asGuiItem {
                                    if (finish) return@asGuiItem
                                    val now = Date()
                                    val meta = item.itemMeta!! as BookMeta
                                    val book = Book(
                                        title = if (meta.hasDisplayName()) meta.displayName else meta.title ?: oldBook?.second?.title ?: "《无题》",
                                        creator = oldBook?.second?.creator ?: targetId,
                                        created = oldBook?.second?.created ?: now,
                                        modifier = meta.author?.let { Bukkit.getOfflinePlayers().find{ p -> p.name == it }?.uniqueId } ?: session.player.uniqueId,
                                        modified = now,
                                        pages = meta.pages.toList()
                                    )
                                    finish = true
                                    setBook(book)
                                })
                        }
                    }
                    gui.update() // 会执行clearInventory再set所以会把item弄掉
                    gui.inventory.setItem(24, item)
                    gui.inventory.viewers.forEach { viewer -> (viewer as Player).updateInventory() }
                }

                for (row in 1..6) for (col in 1..9) {
                    if (row == 3 && col == 7) continue
                    gui.setItem(row, col, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                        .name(Component.text("")).asGuiItem())
                }
                gui.setItem(4, 7, ItemBuilder.from("77334cddfab45d75ad28e1a47bf8cf5017d2f0982f6737da22d4972952510661".toSkull())
                    .name(Component.text(""))
                    .lore(Component.text("§8请将编辑好内容的[书与笔]放在上面一格,然后点击铁砧上传")).asGuiItem())
                gui.setDefaultClickAction {
                    if (it.clickedInventory == session.player.inventory) return@setDefaultClickAction
                    if (it.slot == 24) {
                        Schedulers.sync().runLater({ update() }, 1L)
                    } else {
                        it.isCancelled = true
                    }
                }
                if (oldBook != null) {
                    gui.setItem(3, 3, ItemBuilder.from(Material.WRITABLE_BOOK)
                        .name(Component.text("下载一本可以编辑的书"))
                        .lore(
                            Component.text("§e点击后将会获得一本可编辑的书(书与笔),"),
                            Component.text("§e您可以用这本书进行内容编辑,这本书的所有权依旧属于你"),
                            Component.text("§e编辑完成后,将其放入右侧的格子内,点击上传完成编辑"),
                            Component.text("§b如你之前已经下载过同一本书,也可以用之前的"),
                            Component.text("§c注意: 之前下载的书的内容可能不是最新的, 上传覆盖后将无法恢复")
                        )
                        .asGuiItem {
                            // 下载一本可以编辑的书
                            val result = session.player.inventory.addItem(ItemStack(Material.WRITABLE_BOOK).apply {
                                itemMeta = (itemMeta as BookMeta?)?.applyBook(oldBook.second, oldBook.first, toWrittenBook = false, addModifyInfo = true)
                            })
                            if (result.size > 0) {
                                PluginMain.INSTANCE.message.printf(session.player, "§c背包已满，无法导出原书！")
                            }
                        }
                    )
                    gui.setItem(6, 1, ItemBuilder.from(Material.WRITABLE_BOOK)
                        .name(Component.text("导出为书本原件"))
                        .lore(
                            Component.text("§c注意: 和上面的§f[下载可编辑的书]§c不同"),
                            Component.text("§c点击本按钮将生成一本真正意义上的原件,"),
                            Component.text("§c即包含文本内容的普通[书与笔]"),
                            Component.text("§c所生成的书将解除与牛腩书局的任何关联,"),
                            Component.text("§c这就意味着别人可以随意修改这本书的内容,并可将其占为己有"),
                        ).asGuiItem {
                            // 导出为书本原件
                            val item = oldBook.second.toOriginalWritableBook()
                            val result = session.player.inventory.addItem(item)
                            if (result.size > 0) {
                                PluginMain.INSTANCE.message.printf(session.player, "§c背包已满，无法导出原书!")
                            }
                        }
                    )
                } else {
                    gui.setItem(3, 3, ItemBuilder.from(Material.WRITABLE_BOOK)
                        .name(Component.text(""))
                        .lore(
                            Component.text("由于现在是在添加一本新书"),
                            Component.text("因此没有什么可以下载的旧内容"),
                            Component.text("直接将一本[书与笔]或[成书]放在右边的格子即可"),
                        )
                        .asGuiItem()
                    )
                }
                gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("返回")).asGuiItem {
                    session.back()
                })
                update()
            }
            true
        }, { _, gui, _ ->
            if (!finish) {
                val item = gui.inventory.getItem(24) ?: return@open
                val player = session.player
                Schedulers.sync().run {
                    if (player.inventory.addItem(item).size > 0) {
                        // throw item on the ground
                        player.world.dropItem(player.location, item)
                    }
                }
            }
        }
    )
}