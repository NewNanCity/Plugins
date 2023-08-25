package city.newnan.betterbook.gui

import city.newnan.betterbook.PluginMain
import city.newnan.betterbook.book.*
import city.newnan.betterbook.book.applyBook
import city.newnan.violet.gui.PlayerGuiSession
import city.newnan.violet.gui.UpdateType
import city.newnan.violet.item.toSkull
import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import me.lucko.helper.Schedulers
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import java.util.*

val playerBooksCache = mutableMapOf<UUID, MutableMap<UUID, Book>>()

fun openPlayerBooksGui(session: PlayerGuiSession, target: OfflinePlayer? = null) {
    if (target != null && (!target.hasPlayedBefore() || target.name == null)) {
        PluginMain.INSTANCE.message.printf(session.player, "§c该玩家从未进入过服务器!")
        return
    }
    val playerId = target?.uniqueId ?: session.player.uniqueId
    val playerName = target?.name ?: session.player.name
    var lastClicked = 0L

    // 因为全是IO, 所以要缓存
    val books = playerBooksCache.getOrPut(playerId) {
        val books = mutableMapOf<UUID, Book>()
        Librarian.playerBooks[playerId]?.forEach {
            val book = Librarian[it, false] ?: return@forEach
            books[it] = book
        }
        // 缓存半小时
        Schedulers.sync().runLater({
            playerBooksCache.remove(playerId)
        }, 36000L).bindWith(PluginMain.INSTANCE)
        books
    }

    session.open(Gui.paginated().rows(6)
        .title(Component.text("§7[§3§l牛腩书局§r§7]§r $playerName 的所有书籍")).create(), { type, gui, _ ->
            if (type == UpdateType.Init) {
                gui.setItem(6, 1, ItemBuilder.from("9a2d891c6ae9f6baa040d736ab84d48344bb6b70d7f1a280dd12cbac4d777".toSkull())
                    .name(Component.text("§6添加/修改")).asGuiItem {
                        openEditBookGui(session, playerId, null) { newBook ->
                            val bookId = Librarian.nextAvailableUUID
                            Librarian += bookId to newBook
                            books[bookId] = newBook
                            session.back()
                        }
                    })
                gui.setItem(6, 3, ItemBuilder.from("37aee9a75bf0df7897183015cca0b2a7d755c63388ff01752d5f4419fc645".toSkull())
                    .name(Component.text("上一页")).asGuiItem { gui.previous() })
                gui.setItem(6, 7, ItemBuilder.from("682ad1b9cb4dd21259c0d75aa315ff389c3cef752be3949338164bac84a96e".toSkull())
                    .name(Component.text("下一页")).asGuiItem { gui.next() })
                gui.setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Component.text("关闭")).asGuiItem {
                    session.back()
                })
                listOf(2,4,5,6,8).forEach { gui.setItem(6, it, ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE)
                .name(Component.text("")).asGuiItem()) }
                gui.setDefaultClickAction { it.isCancelled = true }
            }
            gui.clearPageItems()
            books.forEach { (bookId, book) ->
                gui.addItem(ItemBuilder.from(ItemStack(Material.WRITTEN_BOOK).also { item ->
                    item.itemMeta = (item.itemMeta as BookMeta?)?.also { meta ->
                        meta.title = book.title
                        meta.author = playerName
                        meta.generation = BookMeta.Generation.COPY_OF_COPY
                        meta.lore = listOf(
                            "§r§6创建时间 §r§7${dateFormatter.format(book.created)}§r",
                            "§r§6修改时间 §r§7${dateFormatter.format(book.modified)}§r",
                            "§r§6最后修改 §r§7${Bukkit.getOfflinePlayer(book.modifier).name ?: "秩名"}§r",
                            "",
                            "§d左键: 打印成书",
                            "§d右键: 编辑",
                            "§cshift+右键: 删除",
                            "§8预览: §r" + (book.pages.firstOrNull()?.let { it1 ->
                                "§r§7${it1.substring(0, minOf(35, it1.length)).replace('\n', ' ')}...§r"
                            } ?: "§r§8无内容§r")
                        )
                    }
                }).asGuiItem {
                    val now = System.currentTimeMillis()
                    if (now - lastClicked < 1000L) return@asGuiItem
                    lastClicked = now
                    if (it.isLeftClick) {
                        val amount = if (it.isShiftClick) 64 else 1
                        if (session.player.inventory.addItem(ItemStack(Material.WRITTEN_BOOK, amount).apply {
                                itemMeta = (itemMeta as BookMeta?)?.applyBook(book, bookId, toWrittenBook = true, addModifyInfo = false)
                            }).size > 0) {
                            PluginMain.INSTANCE.message.printf(session.player, "§c背包已满，无法出版图书!")
                        }
                    } else {
                        if (it.isShiftClick) {
                            session.chatInput { input ->
                                if (input == "Y") {
                                    val item = book.toOriginalWritableBook()
                                    val result = session.player.inventory.addItem(item)
                                    if (result.size > 0) {
                                        PluginMain.INSTANCE.message.printf(session.player, "§c背包已满，无法导出原书!")
                                    }
                                    Librarian -= bookId to book
                                    books.remove(bookId)
                                    PluginMain.INSTANCE.message.printf(session.player, "§a已删除!")
                                } else {
                                    PluginMain.INSTANCE.message.printf(session.player, "§c已取消删除!")
                                }
                                session.show()
                                true
                            }.also { result ->
                                if (result) {
                                    PluginMain.INSTANCE.message.printf(session.player, "§c你正在删除一本已经登记的书, 该书删除后, 将无法恢复, 游戏内所有该书的成书副本也会被删除! §6但在删除之后, 会给你一本该书的可编辑原件, 还可再次导入. §f输入 §aY §f确认删除, 输入其他内容则取消删除")
                                    session.hide()
                                } else {
                                    PluginMain.INSTANCE.message.printf(session.player, "§c你正在进行其他输入, 请先取消之!")
                                }
                            }
                        } else {
                            openEditBookGui(session, playerId, bookId to book) { newBook ->
                                Librarian += bookId to newBook
                                books[bookId] = newBook
                                session.back()
                            }
                        }
                    }
                })
            }
            true
        }
    )
}