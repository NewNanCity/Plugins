package city.newnan.externalbook.gui.authorbooks

import city.newnan.core.utils.text.ComponentParseMode
import city.newnan.core.utils.text.toComponent
import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.book.*
import city.newnan.externalbook.gui.editbook.openEditBookGui
import city.newnan.externalbook.i18n.LanguageKeys
import city.newnan.gui.component.linefill.LineFillDirection
import city.newnan.gui.component.paginated.onLeftClick
import city.newnan.gui.component.paginated.onRightClick
import city.newnan.gui.component.paginated.onShiftLeftClick
import city.newnan.gui.component.paginated.onShiftRightClick
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dsl.*
import com.github.f4b6a3.ulid.Ulid
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import java.text.SimpleDateFormat
import java.util.Locale.getDefault

/**
 * 打开玩家书籍GUI
 *
 * @param plugin 插件实例
 * @param player 当前打开gui的玩家
 * @param targetAuthor 待检查的目标玩家
 */
fun openPlayerBooksGui(
    plugin: ExternalBookPlugin,
    player: Player,
    targetAuthor: OfflinePlayer,
) {
    if (!targetAuthor.hasPlayedBefore() || targetAuthor.name == null) {
        plugin.messager.printf(player, LanguageKeys.Gui.PLAYER_NOT_FOUND)
        return
    }

    val authorId = targetAuthor.uniqueId
    val authorName = targetAuthor.name
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    // 使用旧插件的标题格式
    val title = plugin.messager.sprintf(LanguageKeys.Gui.PlayerBooks.TITLE, authorName)

    plugin.openPage(
        InventoryType.CHEST,
        size = 54,
        player = player,
        title = title
    ) {
        // 底部边框
        lineFillComponent(0, 5, 9, LineFillDirection.HORIZONTAL) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 分页组件显示书籍（前5行，45个槽位）
        val paginatedComponent = paginatedComponent(
            startX = 0, startY = 0,
            width = 9, height = 5,
            dataProvider = AuthorBookDataProvider(plugin, authorId)
        ) {
            render { context ->
                val book = context.item ?: return@render null
                item(Material.WRITTEN_BOOK) {
                    meta<BookMeta> {
                        it.title(book.title.toComponent())
                        it.author(authorName?.toComponent() ?: format(LanguageKeys.Gui.PlayerBooks.BookDisplay.ANONYMOUS))
                        it.generation = BookMeta.Generation.COPY_OF_COPY
                        it.lore(listOf(
                            format(LanguageKeys.Gui.PlayerBooks.BookDisplay.CREATED_TIME, dateFormatter.format(book.created)),
                            format(LanguageKeys.Gui.PlayerBooks.BookDisplay.MODIFIED_TIME, dateFormatter.format(book.modified)),
                            format(LanguageKeys.Gui.PlayerBooks.BookDisplay.LAST_MODIFIER,
                                Bukkit.getOfflinePlayer(book.modifier).name ?: formatPlain(LanguageKeys.Gui.PlayerBooks.BookDisplay.ANONYMOUS)),
                            "".toComponent(ComponentParseMode.Plain),
                            format(LanguageKeys.Gui.PlayerBooks.BookDisplay.LEFT_CLICK),
                            format(LanguageKeys.Gui.PlayerBooks.BookDisplay.RIGHT_CLICK),
                            format(LanguageKeys.Gui.PlayerBooks.BookDisplay.SHIFT_RIGHT_CLICK),
                            format(LanguageKeys.Gui.PlayerBooks.BookDisplay.PREVIEW).append(
                                if (book.preview.isNotEmpty()) {
                                    format(LanguageKeys.Gui.PlayerBooks.BookDisplay.PREVIEW_CONTENT, book.preview)
                                } else {
                                    format(LanguageKeys.Gui.PlayerBooks.BookDisplay.NO_CONTENT)
                                }
                            ),
                            format(LanguageKeys.Gui.PlayerBooks.BookDisplay.ULID, book.id.toString())
                        ))
                    }
                }
            }

            fun giveBook(bookId: Ulid, amount: Int) {
                // 异步获取书籍数据，避免阻塞主线程
                runAsync {
                    try {
                        val book = plugin.librarian?.get(bookId)
                        if (book == null) {
                            runSync {
                                plugin.messager.printf(player, LanguageKeys.Gui.BOOK_NOT_FOUND)
                            }
                            return@runAsync
                        }

                        // 生成成书
                        val publishedBook = ItemStack(Material.WRITTEN_BOOK, amount).apply {
                            itemMeta = (itemMeta as BookMeta?)?.applyBook(
                                book,
                                toWrittenBook = true,
                                addModifyInfo = false
                            )
                        }

                        // 回到主线程进行物品操作
                        runSync {
                            // 添加到玩家背包
                            if (player.inventory.addItem(publishedBook).isNotEmpty()) {
                                plugin.messager.printf(player, LanguageKeys.Gui.INVENTORY_FULL)
                            }
                        }
                    } catch (e: Exception) {
                        plugin.logger.error("Failed to get book for printing", e)
                        runSync {
                            plugin.messager.printf(player, LanguageKeys.Gui.OPERATION_FAILED)
                        }
                    }
                }
            }

            // 打印成书 - 一本
            onLeftClick { context, index, bookEntry ->
                if (bookEntry == null) return@onLeftClick
                giveBook(bookEntry.id, 1)
            }

            // 打印成书 - 一组
            onShiftLeftClick { context, index, bookEntry ->
                if (bookEntry == null) return@onShiftLeftClick
                giveBook(bookEntry.id, 64)
            }

            // 编辑
            onRightClick { context, index, bookEntry ->
                val bookId = bookEntry?.id ?: return@onRightClick

                // 异步获取书籍数据，避免阻塞主线程
                runAsync {
                    try {
                        val book = plugin.librarian?.get(bookId)
                        if (book == null) {
                            runSync {
                                plugin.messager.printf(player, LanguageKeys.Gui.BOOK_NOT_FOUND)
                            }
                            return@runAsync
                        }

                        // 回到主线程进行GUI操作
                        runSync {
                            openEditBookGui(plugin, player, authorId, book) { newBook ->
                                // 异步保存书籍
                                if (plugin.librarian == null) return@openEditBookGui false
                                this@paginatedComponent.clearCache()
                                runAsync {
                                    try {
                                        plugin.librarian!! += newBook
                                    } catch (e: Exception) {
                                        plugin.logger.error("Failed to save book", e)
                                        runSync {
                                            plugin.messager.printf(player, LanguageKeys.Gui.SAVE_FAILED)
                                        }
                                    }
                                }
                                true
                            }
                        }
                    } catch (e: Exception) {
                        plugin.logger.error("Failed to get book for editing", e)
                        runSync {
                            plugin.messager.printf(player, LanguageKeys.Gui.OPERATION_FAILED)
                        }
                    }
                }
            }

            // 删除
            onShiftRightClick { context, index, bookEntry ->
                val bookId = bookEntry?.id ?: return@onShiftRightClick

                // 异步获取书籍数据，避免阻塞主线程
                runAsync {
                    try {
                        if (!(plugin.librarian?.contains(bookId) ?: false)) {
                            runSync {
                                plugin.messager.printf(player, LanguageKeys.Gui.BOOK_NOT_FOUND)
                            }
                            return@runAsync
                        }

                        // 回到主线程进行GUI操作
                        runSync {
                            // Shift+右键：删除
                            this@openPage.chatInput { input ->
                                if (input.lowercase(getDefault()) == "y") {
                                    // 异步执行删除操作
                                    runAsync {
                                        if (plugin.librarian == null) return@runAsync
                                        try {
                                            val book = plugin.librarian!![bookId]
                                            plugin.librarian!! -= bookId
                                            if (book == null) return@runAsync
                                            val originalBook = book.toOriginalWritableBook()
                                            runSync(false) {
                                                val result = player.inventory.addItem(originalBook)
                                                if (result.isNotEmpty()) {
                                                    plugin.messager.printf(player, LanguageKeys.Gui.PlayerBooks.DELETE_INVENTORY_FULL)
                                                    result.values.forEach { remainingItem ->
                                                        player.world.dropItem(player.location, remainingItem)
                                                    }
                                                }
                                                plugin.messager.printf(player, LanguageKeys.Gui.PlayerBooks.DELETE_SUCCESS)
                                            }
                                        } catch (e: Exception) {
                                            plugin.logger.error("Failed to delete book", e)
                                            runSync {
                                                plugin.messager.printf(player, LanguageKeys.Gui.DELETE_FAILED)
                                            }
                                        }
                                    }
                                } else {
                                    plugin.messager.printf(player, LanguageKeys.Gui.PlayerBooks.DELETE_CANCELLED)
                                }
                                this@openPage.show()
                                true
                            }.also { result ->
                                if (result) {
                                    plugin.messager.printf(player, LanguageKeys.Gui.PlayerBooks.DELETE_CONFIRM_MESSAGE)
                                    this@openPage.hide()
                                } else {
                                    plugin.messager.printf(player, LanguageKeys.Gui.PlayerBooks.CHAT_INPUT_BUSY)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        plugin.logger.error("Failed to get book for deletion", e)
                        runSync {
                            plugin.messager.printf(player, LanguageKeys.Gui.OPERATION_FAILED)
                        }
                    }
                }
            }
        }

        // 添加/修改按钮（slot 45，第6行第1列，坐标0,5）
        slotComponent(0, 5) {
            render {
                urlSkull("9a2d891c6ae9f6baa040d736ab84d48344bb6b70d7f1a280dd12cbac4d777") {
                    name(LanguageKeys.Gui.PlayerBooks.ADD_MODIFY)
                }
            }
            onLeftClick { _, _, _ ->
                openEditBookGui(plugin, player, authorId, null) { newBook ->
                    // 异步保存新书籍，避免阻塞主线程
                    if (plugin.librarian == null) return@openEditBookGui false
                    paginatedComponent.clearCache()
                    runAsync {
                        try {
                            plugin.librarian!! += newBook
                        } catch (e: Exception) {
                            plugin.logger.error("Failed to save new book", e)
                            runSync {
                                plugin.messager.printf(player, LanguageKeys.Gui.SAVE_FAILED)
                            }
                        }
                    }
                    true
                }
            }
        }

        // 上一页按钮（slot 47，第6行第3列，坐标2,5）
        slotComponent(2, 5) {
            render {
                urlSkull("37aee9a75bf0df7897183015cca0b2a7d755c63388ff01752d5f4419fc645") {
                    name(LanguageKeys.Gui.Common.PREVIOUS_PAGE)
                }
            }
            onLeftClick { _, _, _ ->
                paginatedComponent.previousPage()
            }
        }

        // 下一页按钮（slot 51，第6行第7列，坐标6,5）
        slotComponent(6, 5) {
            render {
                urlSkull("682ad1b9cb4dd21259c0d75aa315ff389c3cef752be3949338164bac84a96e") {
                    name(LanguageKeys.Gui.Common.NEXT_PAGE)
                }
            }
            onLeftClick { _, _, _ ->
                paginatedComponent.nextPage()
            }
        }

        // 关闭按钮（slot 53，第6行第9列，坐标8,5）
        slotComponent(8, 5) {
            render {
                item(Material.BARRIER) {
                    name(LanguageKeys.Gui.PlayerBooks.CLOSE)
                }
            }
            onLeftClick { _, _, _ ->
                this@openPage.back()
            }
        }
    }
}