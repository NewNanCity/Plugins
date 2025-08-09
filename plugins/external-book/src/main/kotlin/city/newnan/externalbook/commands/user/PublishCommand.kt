package city.newnan.externalbook.commands.user

import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runSync
import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.book.Book
import city.newnan.externalbook.book.applyBook
import city.newnan.externalbook.book.findBookID
import city.newnan.externalbook.config.ExternalBookConfig
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
 * 发布命令
 *
 * 将已注册的书与笔发布为不可编辑的成书。
 * 只有书籍的原作者或有bypass权限的人可以发布。
 * 完整保留原有PublishCommand的逻辑。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class PublishCommand(private val plugin: ExternalBookPlugin) {

    /**
     * 发布命令处理方法
     */
    @Command("externalbook|book publish|static")
    @CommandDescription(LanguageKeys.Commands.Publish.DESCRIPTION)
    @Permission("externalbook.use")
    fun publishCommand(sender: CommandSender) {
        // 检查是否为玩家
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        // 获取玩家手上的书 - 只检查书与笔
        val item = sender.inventory.itemInMainHand.takeIf { it.type == Material.WRITABLE_BOOK }
            ?: sender.inventory.itemInOffHand.takeIf { it.type == Material.WRITABLE_BOOK } ?: run {
                plugin.messager.printf(sender, LanguageKeys.Commands.Publish.INVALID_ITEM)
                return
            }

        // 获取图书管理员实例
        val librarian = plugin.librarian ?: run {
            plugin.logger.error(LanguageKeys.Commands.Common.LOG_LIBRARIAN_NOT_AVAILABLE)
            plugin.messager.printf(sender, LanguageKeys.Commands.Common.LIBRARIAN_NOT_AVAILABLE)
            return@publishCommand
        }

        // 查找书籍UUID
        val bookIds = item.findBookID(written = false)
        val bookId = bookIds.second ?: bookIds.first?.let { librarian.getUlid(it) } ?: run {
            plugin.runSync<Unit> {
                plugin.messager.printf(sender, LanguageKeys.Commands.Publish.NOT_FOUND)
            }
            return@publishCommand
        }

        plugin.runAsync<Unit> {
            try {
                // 获取书籍数据
                val book = librarian[bookId] ?: run {
                    plugin.runSync<Unit> {
                        plugin.messager.printf(sender, LanguageKeys.Commands.Publish.NOT_FOUND)
                    }
                    return@runAsync
                }

                // 检查权限
                if (book.creator != sender.uniqueId && !sender.hasPermission("externalbook.bypass")) {
                    plugin.runSync<Unit> {
                        plugin.messager.printf(sender, LanguageKeys.Commands.Publish.NOT_AUTHOR)
                    }
                    return@runAsync
                }

                // 创建发布的成书
                val publishedBook = createPublishedBook(book)

                // 添加到玩家背包
                plugin.runSync {
                    addItemToPlayerInventory(sender, publishedBook)
                }
                plugin.logger.info(LanguageKeys.Commands.Publish.LOG_SUCCESS, sender.name, book.title)
            } catch (e: Exception) {
                plugin.runSync<Unit> {
                    plugin.messager.printf(sender, LanguageKeys.Commands.Publish.FAILED)
                }
                plugin.logger.error(LanguageKeys.Commands.Publish.LOG_FAILED, e, sender.name)
            }
        }
    }

    /**
     * 创建发布的成书
     * 完整保留原有逻辑
     */
    private fun createPublishedBook(book: Book): ItemStack {
        return ItemStack(Material.WRITTEN_BOOK).apply {
            itemMeta = (itemMeta as BookMeta?)?.applyBook(
                book = book,
                toWrittenBook = true,
                addModifyInfo = false
            )
        }
    }

    /**
     * 将物品添加到玩家背包，如果背包满了则掉落在地上
     * 完整保留原有逻辑
     */
    private fun addItemToPlayerInventory(player: Player, item: ItemStack) {
        val remainingItems = player.inventory.addItem(item)
        if (remainingItems.isNotEmpty()) {
            // 背包满了，提示玩家并掉落物品
            plugin.messager.printf(player, LanguageKeys.Commands.Publish.INVENTORY_FULL)
            remainingItems.values.forEach { remainingItem ->
                player.world.dropItemNaturally(player.location, remainingItem)
            }
        } else {
            plugin.messager.printf(player, LanguageKeys.Commands.Publish.SUCCESS)
        }
    }
}
