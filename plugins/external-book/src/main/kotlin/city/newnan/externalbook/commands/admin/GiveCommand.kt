package city.newnan.externalbook.commands.admin

import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runSync
import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.book.Book
import city.newnan.externalbook.book.applyBook
import city.newnan.externalbook.i18n.LanguageKeys
import com.github.f4b6a3.ulid.Ulid
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 给予命令
 *
 * 给玩家指定的书籍。
 * 完整保留原有GiveCommand的逻辑。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class GiveCommand(private val plugin: ExternalBookPlugin) {

    /**
     * 给予命令处理方法
     */
    @Command("externalbook|book give <player> <book-id>")
    @CommandDescription(LanguageKeys.Commands.Give.DESCRIPTION)
    @Permission("externalbook.give")
    fun giveCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.Give.PLAYER) targetPlayer: Player,
        @Argument(value = "book-id", description = LanguageKeys.Commands.Give.BOOK_ID) bookIdString: String
    ) {
        val librarian = plugin.librarian ?: run {
            plugin.logger.error(LanguageKeys.Commands.Common.LOG_LIBRARIAN_NOT_AVAILABLE)
            plugin.messager.printf(sender, LanguageKeys.Commands.Common.LIBRARIAN_NOT_AVAILABLE)
            return@giveCommand
        }

        // 验证书籍ID格式
        val bookId = try {
            Ulid.from(bookIdString)
        } catch (e: IllegalArgumentException) {
            plugin.messager.printf(sender, LanguageKeys.Core.Common.INVALID_BOOK_ID)
            return@giveCommand
        }

        // 异步获取书籍数据
        plugin.runAsync {
            try {
                // 获取书籍数据
                val book = librarian[bookId] ?: run {
                    plugin.runSync<Unit> {
                        plugin.messager.printf(sender, LanguageKeys.Commands.Give.BOOK_NOT_FOUND, bookIdString)
                    }
                    return@runAsync
                }

                // 创建书籍物品
                val bookItem = createBookItem(book)

                // 添加到目标玩家背包
                plugin.runSync {
                    addItemToPlayerInventory(sender, targetPlayer, bookItem, book)
                }
                plugin.logger.info(LanguageKeys.Commands.Give.LOG_SUCCESS, bookIdString, sender.name, targetPlayer.name)
            } catch (e: Exception) {
                plugin.runSync<Unit> {
                    plugin.messager.printf(sender, LanguageKeys.Commands.Give.FAILED, bookIdString)
                }
                plugin.logger.error(LanguageKeys.Commands.Give.LOG_FAILED,  e, bookIdString, sender.name, targetPlayer.name)
            }
            return@runAsync
        }
    }

    /**
     * 创建书籍物品
     */
    private fun createBookItem(book: Book): ItemStack {
        return ItemStack(Material.WRITABLE_BOOK).apply {
            itemMeta = (itemMeta as BookMeta?)?.applyBook(
                book = book,
                toWrittenBook = false,
                addModifyInfo = true
            )
        }
    }

    /**
     * 将物品添加到目标玩家背包，如果背包满了则掉落在地上
     */
    private fun addItemToPlayerInventory(sender: CommandSender, target: Player, item: ItemStack, book: Book) {
        val remainingItems = target.inventory.addItem(item)
        if (remainingItems.isNotEmpty()) {
            // 背包满了，提示并掉落物品
            remainingItems.values.forEach { remainingItem ->
                target.world.dropItemNaturally(target.location, remainingItem)
            }
            plugin.messager.printf(sender, LanguageKeys.Commands.Give.INVENTORY_FULL, target.name)
        }
        // 发送成功消息
        plugin.messager.printf(sender, LanguageKeys.Commands.Give.SUCCESS, target.name, book.title)
        if (sender != target) {
            plugin.messager.printf(target, LanguageKeys.Commands.Give.SUCCESS_GET, book.title)
        }
    }
}
