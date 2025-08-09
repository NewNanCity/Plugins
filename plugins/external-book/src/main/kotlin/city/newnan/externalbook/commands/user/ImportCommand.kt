package city.newnan.externalbook.commands.user

import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runSync
import city.newnan.core.utils.text.toPlain
import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.book.Book
import city.newnan.externalbook.book.Librarian
import city.newnan.externalbook.book.applyBook
import city.newnan.externalbook.book.findBookID
import city.newnan.externalbook.i18n.LanguageKeys
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import java.util.*

/**
 * 导入命令
 *
 * 将玩家手中的书与笔或成书导入到图书馆系统中。
 * 如果书籍已经注册过，则会更新现有书籍。
 * 完整保留原有ImportCommand的逻辑。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class ImportCommand(private val plugin: ExternalBookPlugin) {

    /**
     * 导入命令处理方法
     */
    @Command("externalbook|book import|register")
    @CommandDescription(LanguageKeys.Commands.Import.DESCRIPTION)
    @Permission("externalbook.use")
    fun importCommand(sender: CommandSender) {
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        // 获取玩家手中的书籍 - 只检查书与笔
        val item = sender.inventory.itemInMainHand.takeIf { it.type == Material.WRITABLE_BOOK }
            ?: sender.inventory.itemInOffHand.takeIf { it.type == Material.WRITABLE_BOOK } ?: run {
            plugin.messager.printf(sender, LanguageKeys.Commands.Import.INVALID_ITEM)
            return
        }

        // 检查书籍元数据
        val bookMeta = item.itemMeta as? BookMeta ?: run {
            plugin.messager.printf(sender, LanguageKeys.Commands.Import.INVALID_ITEM)
            return
        }

        // 获取图书管理员实例
        val librarian = plugin.librarian ?: run {
            plugin.logger.error(LanguageKeys.Commands.Common.LOG_LIBRARIAN_NOT_AVAILABLE)
            plugin.messager.printf(sender, LanguageKeys.Commands.Common.LIBRARIAN_NOT_AVAILABLE)
            return@importCommand
        }

        // 检查权限和处理书籍导入
        processBookImport(sender, item, bookMeta, librarian)
    }

    /**
     * 处理书籍导入逻辑
     * 完整保留原有逻辑
     */
    private fun processBookImport(player: Player, item: ItemStack, bookMeta: BookMeta, librarian: Librarian) {
        // 异步执行数据库操作，避免阻塞主线程
        plugin.runAsync<Unit> {
            try {
                // 检查手上的书是否已经注册过
                val placedBookIds = item.findBookID(written = false)
                val placedBookId = placedBookIds.second ?: placedBookIds.first?.let { librarian.getUlid(it) }
                val queriedBook = placedBookId?.let { librarian.get(it, cache = false) }

                // 如果注册过，则检查权限：只有原作者或有bypass权限的人可以更新已注册的书
                if (queriedBook != null) {
                    if (queriedBook.creator != player.uniqueId && !player.hasPermission("externalbook.bypass")) {
                        plugin.runSync<Unit> {
                            plugin.messager.printf(player, LanguageKeys.Commands.Import.NO_PERMISSION)
                        }
                        return@runAsync
                    }
                }

                // 创建或更新书籍
                val newBook = createBookFromMeta(bookMeta, queriedBook, player, librarian)

                // 添加到图书馆
                librarian += newBook

                // 回到主线程更新物品和发送消息
                plugin.runSync {
                    // 更新手中书籍的元数据
                    updateItemMeta(item, bookMeta, newBook)

                    // 发送成功消息
                    val messageKey = if (queriedBook == null) LanguageKeys.Commands.Import.SUCCESS_NEW else LanguageKeys.Commands.Import.SUCCESS_UPDATE
                    plugin.messager.printf(player, messageKey)
                }
                plugin.logger.info(LanguageKeys.Commands.Import.LOG_SUCCESS, player.name, newBook.title)
            } catch (e: Exception) {
                // 异步操作中的异常处理
                plugin.logger.error(LanguageKeys.Commands.Import.LOG_FAILED, e, player.name)
                plugin.runSync<Unit> {
                    plugin.messager.printf(player, LanguageKeys.Commands.Import.FAILED)
                }
            }
        }
    }

    /**
     * 从BookMeta创建Book对象
     * 完整保留原有逻辑
     */
    private fun createBookFromMeta(bookMeta: BookMeta, existingBook: Book?, player: Player, librarian: Librarian): Book {
        val now = Date()
        val title = (bookMeta.displayName() ?: bookMeta.title())?.toPlain() ?: existingBook?.title ?:
            ExternalBookPlugin.instance.stringFormatter.sprintfPlain(true, "<%gui.editbook.upload_button.untitled%>")
        return Book(
            id = existingBook?.id ?: librarian.getNextAvailableUUID(),
            title = title,
            creator = existingBook?.creator ?: player.uniqueId,
            modifier = bookMeta.author()?.let { Bukkit.getOfflinePlayer(it.toPlain()).uniqueId } ?: player.uniqueId,
            created = existingBook?.created ?: now,
            modified = now,
            pages = bookMeta.pages().map { page -> page.toPlain() }
        )
    }

    /**
     * 更新物品元数据
     * 完整保留原有逻辑
     */
    private fun updateItemMeta(item: ItemStack, bookMeta: BookMeta, book: Book) {
        item.itemMeta = bookMeta.applyBook(
            book = book,
            toWrittenBook = false,
            addModifyInfo = true
        )
    }
}
