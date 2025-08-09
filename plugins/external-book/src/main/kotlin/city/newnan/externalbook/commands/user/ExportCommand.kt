package city.newnan.externalbook.commands.user

import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runSync
import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.book.Book
import city.newnan.externalbook.book.applyBook
import city.newnan.externalbook.book.findBookID
import city.newnan.externalbook.gui.authorlist.openAuthorListGui
import city.newnan.externalbook.i18n.LanguageKeys
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 导出命令 将书籍导出为可编辑的书与笔（原书）。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class ExportCommand(private val plugin: ExternalBookPlugin) {

    /**
     * 导出命令处理方法
     */
    @Command("externalbook|book export|origin|edit")
    @CommandDescription(LanguageKeys.Commands.Export.DESCRIPTION)
    @Permission("externalbook.use")
    fun exportCommand(sender: CommandSender) {
        // 检查是否为玩家
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        // 获取玩家手上的书 - 只能是成书
        val item = sender.inventory.itemInMainHand.takeIf { it.type == Material.WRITTEN_BOOK }
            ?: sender.inventory.itemInOffHand.takeIf { it.type == Material.WRITTEN_BOOK } ?: run {
                plugin.messager.printf(sender, LanguageKeys.Commands.Export.INVALID_ITEM)
                return
            }

        // 获取图书管理员实例
        val librarian = plugin.librarian ?: run {
            plugin.logger.error(LanguageKeys.Commands.Common.LOG_LIBRARIAN_NOT_AVAILABLE)
            plugin.messager.printf(sender, LanguageKeys.Commands.Common.LIBRARIAN_NOT_AVAILABLE)
            return@exportCommand
        }

        // 查找书籍UUID
        val bookIds = item.findBookID(written = true, writable = false)
        val bookId = bookIds.second ?: bookIds.first?.let { librarian.getUlid(it) } ?: run {
            plugin.runSync<Unit> {
                plugin.messager.printf(sender, LanguageKeys.Commands.Export.NOT_FOUND)
            }
            return@exportCommand
        }

        // 异步执行查询，避免阻塞主线程
        plugin.runAsync<Unit> {
            try {
                // 获取书籍数据
                val book = librarian[bookId] ?: run {
                    plugin.runSync {
                        plugin.messager.printf(sender, LanguageKeys.Commands.Export.NOT_FOUND)
                    }
                    return@runAsync
                }

                // 检查权限
                if (book.creator != sender.uniqueId && !sender.hasPermission("externalbook.bypass")) {
                    plugin.runSync {
                        plugin.messager.printf(sender, LanguageKeys.Commands.Export.NOT_AUTHOR)
                    }
                    return@runAsync
                }

                // 创建可编辑书籍
                val writableBook = createWritableBook(book)

                // 添加到玩家背包
                plugin.runSync {
                    addItemToPlayerInventory(sender, writableBook)
                }
            } catch (e: Exception) {
                // 异步操作中的异常处理
                plugin.logger.error(LanguageKeys.Commands.Export.LOG_FAILED, e, sender.name)
                plugin.runSync {
                    plugin.messager.printf(sender, LanguageKeys.Commands.Export.FAILED)
                }
            }

            return@runAsync
        }
    }

    /**
     * 创建可编辑书籍
     */
    private fun createWritableBook(book: Book): ItemStack {
        return ItemStack(Material.WRITABLE_BOOK).apply {
            itemMeta = (itemMeta as BookMeta?)?.applyBook(
                book = book,
                toWrittenBook = false,
                addModifyInfo = true
            )
        }
    }

    /**
     * 将物品添加到玩家背包，如果背包满了则掉落在地上
     */
    private fun addItemToPlayerInventory(player: Player, item: ItemStack) {
        val remainingItems = player.inventory.addItem(item)
        if (remainingItems.isNotEmpty()) {
            // 背包满了，提示玩家并掉落物品
            plugin.messager.printf(player, LanguageKeys.Commands.Export.INVENTORY_FULL)
            remainingItems.values.forEach { remainingItem ->
                player.world.dropItemNaturally(player.location, remainingItem)
            }
        } else {
            plugin.messager.printf(player, LanguageKeys.Commands.Export.SUCCESS)
        }
    }
}
