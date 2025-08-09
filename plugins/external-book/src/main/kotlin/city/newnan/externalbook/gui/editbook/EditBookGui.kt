package city.newnan.externalbook.gui.editbook

import city.newnan.core.utils.text.ComponentParseMode
import city.newnan.core.utils.text.toComponent
import city.newnan.core.utils.text.toPlain
import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.i18n.LanguageKeys
import city.newnan.externalbook.book.*
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dsl.*
import com.github.f4b6a3.ulid.UlidCreator
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import java.text.SimpleDateFormat
import java.util.*

/**
 * 打开书籍编辑GUI
 *
 * @param plugin 插件实例
 * @param player 打开此页面的玩家
 * @param authorId 作者ID
 * @param existingBook 现有书籍（编辑模式）或null（创建模式）
 * @param onBookUpdate 书籍更新回调，返回true会关闭页面
 */
fun openEditBookGui(
    plugin: ExternalBookPlugin,
    player: Player,
    authorId: UUID,
    existingBook: Book?,
    onBookUpdate: (Book) -> Boolean
) {
    // 使用旧插件的标题格式
    val title = if (existingBook != null) {
        plugin.messager.sprintf(LanguageKeys.Gui.EditBook.TITLE_EDIT, existingBook.title)
    } else {
        plugin.messager.sprintf(LanguageKeys.Gui.EditBook.TITLE_NEW)
    }

    var finished = false
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    var placedItem: ItemStack? = null
    var canUpload = false

    plugin.openPage(
        InventoryType.CHEST,
        size = 54,
        player = player,
        title = title
    ) {
        // 填充黑色玻璃边框
        rectFillComponent(0, 0, 9, 6) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 上传按钮（slot 40，第5行第5列，坐标4,4）
        var uploadComponentItem: ItemStack? = null
        val uploadComponent = slotComponent(4, 4) {
            render {
                if (uploadComponentItem != null) {
                    val tmp = uploadComponentItem
                    uploadComponentItem = null
                    return@render tmp
                }

                canUpload = false
                // librarian为空，无法上传
                if (plugin.librarian == null) {
                    return@render item(Material.ANVIL) {
                        name(LanguageKeys.Gui.EditBook.UploadButton.CANNOT_UPLOAD)
                        lore(formatPlain(LanguageKeys.Gui.EditBook.UploadButton.NO_LIBRARIAN_HINT).split("\n"))
                    }
                }
                // 没有放书，无法上传
                if (placedItem == null) {
                    return@render item(Material.ANVIL) {
                        name(LanguageKeys.Gui.EditBook.UploadButton.CANNOT_UPLOAD)
                        lore(formatPlain(LanguageKeys.Gui.EditBook.UploadButton.NO_BOOK_HINT).split("\n"))
                    }
                }
                val tmpPlacedItem = placedItem!!
                // 由于查询是异步的，这里先返回一个默认的加载中状态
                runAsync {
                    // 检查是否是已存在的书籍
                    val placedBookIds = tmpPlacedItem.findBookID(written = true, writable = true)
                    val placedBookId = placedBookIds.second ?: placedBookIds.first?.let { plugin.librarian!!.getUlid(it) }
                    val queriedBook = placedBookId?.let { plugin.librarian!!.get(it, cache = false) }

                    // 没有找到对应的书，但是槽位里是有书的，说明书没有绑定任何记录
                    if (queriedBook == null) {
                        if (existingBook == null) {
                            // 新建模式，书没有绑定任何记录，是正常的，可以上传
                            canUpload = true
                            uploadComponentItem = item(Material.ANVIL) {
                                name(LanguageKeys.Gui.EditBook.UploadButton.CAN_UPLOAD)
                                lore(formatPlain(LanguageKeys.Gui.EditBook.UploadButton.UPLOAD_HINT_NEW).split("\n"))
                            }
                            this@slotComponent.update()
                        } else {
                            // 编辑模式，书没有绑定任何记录，可以上传，覆盖原来的数据
                            canUpload = true
                            uploadComponentItem = item(Material.ANVIL) {
                                name(LanguageKeys.Gui.EditBook.UploadButton.CAN_UPLOAD)
                                lore(formatPlain(LanguageKeys.Gui.EditBook.UploadButton.UPLOAD_HINT_EDIT).split("\n"))
                            }
                            this@slotComponent.update()
                        }
                    } else {
                        if (existingBook == null) {
                            // 新建模式，书有绑定记录，不允许上传
                            uploadComponentItem = item(Material.ANVIL) {
                                name(LanguageKeys.Gui.EditBook.UploadButton.CANNOT_UPLOAD)
                                lore(formatPlain(
                                        LanguageKeys.Gui.EditBook.UploadButton.EXISTING_BOOK_NEW_MODE,
                                        queriedBook.title,
                                        Bukkit.getOfflinePlayer(queriedBook.creator).name ?:
                                            formatPlain(LanguageKeys.Gui.EditBook.UploadButton.UNKNOWN_AUTHOR),
                                        dateFormatter.format(queriedBook.created
                                    )).split('\n'))
                            }
                            this@slotComponent.update()
                        } else if (existingBook.id != placedBookId) {
                            // 编辑模式，书有绑定记录，但是ID对不上，不允许上传
                            uploadComponentItem = item(Material.ANVIL) {
                                name(LanguageKeys.Gui.EditBook.UploadButton.CANNOT_UPLOAD)
                                lore(formatPlain(
                                        LanguageKeys.Gui.EditBook.UploadButton.EXISTING_BOOK_EDIT_MODE,
                                        queriedBook.title,
                                        Bukkit.getOfflinePlayer(queriedBook.creator).name ?:
                                            formatPlain(LanguageKeys.Gui.EditBook.UploadButton.UNKNOWN_AUTHOR),
                                        dateFormatter.format(queriedBook.created
                                    )).split('\n'))
                            }
                            this@slotComponent.update()
                        } else {
                            // 编辑模式，书有绑定记录，且ID对得上，可以上传
                            canUpload = true
                            uploadComponentItem = item(Material.ANVIL) {
                                name(LanguageKeys.Gui.EditBook.UploadButton.CAN_UPLOAD)
                                lore(formatPlain(LanguageKeys.Gui.EditBook.UploadButton.UPLOAD_HINT_EDIT).split("\n"))
                            }
                            this@slotComponent.update()
                        }
                    }
                }
                return@render item(Material.ANVIL) {
                    name(LanguageKeys.Gui.EditBook.UploadButton.CHECKING)
                    lore(formatPlain(LanguageKeys.Gui.EditBook.UploadButton.CHECKING_HINT).split("\n"))
                }
            }

            onLeftClick { _, _, _ ->
                if (!canUpload) return@onLeftClick
                if (plugin.librarian == null) return@onLeftClick
                if (finished) return@onLeftClick

                val bookMeta = placedItem?.itemMeta as? BookMeta
                if (bookMeta == null) return@onLeftClick

                runAsync {
                    // 检查是否是已存在的书籍
                    val placedBookIds = placedItem!!.findBookID(written = true, writable = true)
                    val placedBookId = placedBookIds.second ?: placedBookIds.first?.let { plugin.librarian!!.getUlid(it) }
                    val queriedBook = placedBookId?.let { plugin.librarian!!.get(it, cache = false) }

                    // 如果是创建模式，则不允许使用已存在的书籍
                    if (existingBook == null && queriedBook != null) {
                        return@runAsync
                    }

                    // 如果是编辑模式，则不允许使用其他书籍，只允许未绑定的书或者ID相同的书
                    if (existingBook != null && queriedBook != null && existingBook.id != placedBookId) {
                        return@runAsync
                    }

                    val now = Date()
                    val title = (bookMeta.displayName() ?: bookMeta.title())?.toPlain() ?: existingBook?.title ?:
                        formatPlain(LanguageKeys.Gui.EditBook.UploadButton.UNTITLED)
                    val newBook = Book(
                        id = existingBook?.id ?: UlidCreator.getMonotonicUlid(),
                        title = title,
                        creator = existingBook?.creator ?: authorId,
                        created = existingBook?.created ?: now,
                        modifier = bookMeta.author()?.let { Bukkit.getOfflinePlayer(it.toPlain()).uniqueId } ?: player.uniqueId,
                        modified = now,
                        pages = bookMeta.pages().map { page -> page.toPlain() }
                    )
                    if (onBookUpdate(newBook)) {
                        finished = true
                        this@openPage.back()
                    }
                }
            }
        }

        // 书籍放置槽位（slot 24，第3行第7列，坐标6,2）
        storageSlotComponent(6, 2) {
            // 只允许书与笔和成书
            setItemValidator { item ->
                item == null || item.type == Material.WRITABLE_BOOK || item.type == Material.WRITTEN_BOOK
            }

            // 监听物品变化
            onItemChange { oldItem, newItem ->
                placedItem = newItem
                finished = false
                // 更新上传按钮
                uploadComponent.update()
            }
        }

        // 下载可编辑书籍按钮（slot 20，第3行第3列，坐标2,2）
        if (existingBook != null) {
            slotComponent(2, 2) {
                render {
                    item(Material.WRITABLE_BOOK) {
                        name(LanguageKeys.Gui.EditBook.DownloadEditable.NAME)
                        lore(formatPlain(LanguageKeys.Gui.EditBook.DownloadEditable.LORE).split("\n"))
                    }
                }
                onLeftClick { _, _, _ ->
                    // 下载一本可以编辑的书
                    val editableBook = item(Material.WRITABLE_BOOK) {
                        meta<BookMeta> {
                            it.applyBook(existingBook, toWrittenBook = false, addModifyInfo = true)
                        }
                    }
                    val result = player.inventory.addItem(editableBook)
                    if (result.isNotEmpty()) {
                        plugin.messager.printf(player, LanguageKeys.Gui.EditBook.INVENTORY_FULL_EXPORT)
                    }
                }
            }

            // 导出为原件按钮（slot 45，第6行第1列，坐标0,5）
            slotComponent(0, 5) {
                render {
                    item(Material.WRITABLE_BOOK) {
                        name(LanguageKeys.Gui.EditBook.ExportOriginal.NAME)
                        lore(formatPlain(LanguageKeys.Gui.EditBook.ExportOriginal.LORE).split("\n"))
                    }
                }
                onLeftClick { _, _, _ ->
                    // 导出为书本原件
                    val originalBook = existingBook.toOriginalWritableBook()
                    val result = player.inventory.addItem(originalBook)
                    if (result.isNotEmpty()) {
                        plugin.messager.printf(player, LanguageKeys.Gui.EditBook.INVENTORY_FULL_ORIGINAL)
                    }
                }
            }
        } else {
            // 新建模式的提示
            slotComponent(2, 2) {
                render {
                    item(Material.WRITABLE_BOOK) {
                        name("".toComponent(ComponentParseMode.Plain))
                        lore(formatPlain(LanguageKeys.Gui.EditBook.NewBookHint.LORE).split("\n"))
                    }
                }
            }
        }

        // 返回按钮（slot 53，第6行第9列，坐标8,5）
        slotComponent(8, 5) {
            render {
                item(Material.BARRIER) {
                    name(LanguageKeys.Gui.Common.BACK)
                }
            }
            onLeftClick { _, _, _ ->
                this@openPage.back()
            }
        }

        // 提示头颅（slot 42，第5行第7列，坐标6,4）
        slotComponent(6, 4) {
            render {
                urlSkull("77334cddfab45d75ad28e1a47bf8cf5017d2f0982f6737da22d4972952510661") {
                    name("".toComponent(ComponentParseMode.Plain))
                    lore(listOf(LanguageKeys.Gui.EditBook.UPLOAD_HINT))
                }
            }
        }

        // 关闭时处理未完成的书籍
        onPageDestroy {
            if (!finished && placedItem != null) {
                runSync(false) {
                    val droppedItem = placedItem!!.clone()
                    if (player.inventory.addItem(droppedItem).isNotEmpty()) {
                        // 背包满了就掉落在地上
                        player.world.dropItem(player.location, droppedItem)
                    }
                }
            }
        }
    }
}