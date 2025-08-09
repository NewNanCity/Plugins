package city.newnan.externalbook.commands.user

import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runSync
import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.book.bookUlidNbtKey
import city.newnan.externalbook.book.bookUuidNbtKey
import city.newnan.externalbook.book.findBookID
import city.newnan.externalbook.i18n.LanguageKeys
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 解绑命令
 *
 * 将书籍从图书馆系统中解绑，移除书籍的NBT标记。
 * 只有书籍的原作者或有bypass权限的人可以解绑。
 * 完整保留原有StripCommand的逻辑。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class StripCommand(private val plugin: ExternalBookPlugin) {

    /**
     * 解绑命令处理方法
     */
    @Command("externalbook|book strip|unbind")
    @CommandDescription(LanguageKeys.Commands.Strip.DESCRIPTION)
    @Permission("externalbook.use")
    fun stripCommand(sender: CommandSender) {
        // 检查是否为玩家
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        // 获取玩家手上的书 - 只检查书与笔
        val item = sender.inventory.itemInMainHand.takeIf { it.type == Material.WRITABLE_BOOK || it.type == Material.WRITTEN_BOOK }
            ?: sender.inventory.itemInOffHand.takeIf { it.type == Material.WRITABLE_BOOK || it.type == Material.WRITTEN_BOOK } ?: run {
                plugin.messager.printf(sender, LanguageKeys.Commands.Strip.INVALID_ITEM)
                return
            }

        // 获取图书管理员实例
        val librarian = plugin.librarian ?: run {
            plugin.logger.error(LanguageKeys.Commands.Common.LOG_LIBRARIAN_NOT_AVAILABLE)
            plugin.messager.printf(sender, LanguageKeys.Commands.Common.LIBRARIAN_NOT_AVAILABLE)
            return@stripCommand
        }

        // 查找书籍UUID
        val bookIds = item.findBookID(written = true, writable = true)
        val bookId = bookIds.second ?: bookIds.first?.let { librarian.getUlid(it) } ?: run {
            plugin.runSync<Unit> {
                plugin.messager.printf(sender, LanguageKeys.Commands.Publish.NOT_FOUND)
            }
            return@stripCommand
        }

        plugin.runAsync<Unit> {
            try {

                // 获取书籍数据
                val book = librarian[bookId] ?: run {
                    plugin.runSync<Unit> {
                        plugin.messager.printf(sender, LanguageKeys.Commands.Strip.NOT_FOUND)
                    }
                    return@runAsync
                }

                // 检查权限
                if (book.creator != sender.uniqueId && !sender.hasPermission("externalbook.bypass")) {
                    plugin.runSync<Unit> {
                        plugin.messager.printf(sender, LanguageKeys.Commands.Strip.NOT_AUTHOR)
                    }
                    return@runAsync
                }

                // 移除NBT标记
                plugin.runSync {
                    val itemMeta = item.itemMeta
                    if (itemMeta != null) {
                        itemMeta.persistentDataContainer.remove(bookUuidNbtKey)
                        itemMeta.persistentDataContainer.remove(bookUlidNbtKey)
                        item.itemMeta = itemMeta
                        plugin.messager.printf(sender, LanguageKeys.Commands.Strip.SUCCESS)
                        plugin.logger.info(LanguageKeys.Commands.Strip.LOG_SUCCESS, sender.name, book.title)
                    } else {
                        plugin.messager.printf(sender, LanguageKeys.Commands.Strip.FAILED)
                        plugin.logger.error(LanguageKeys.Commands.Strip.LOG_FAILED, null, sender.name, "ItemMeta is null")
                    }
                }
            } catch (e: Exception) {
                plugin.runSync<Unit> {
                    plugin.messager.printf(sender, LanguageKeys.Commands.Strip.FAILED)
                }
                plugin.logger.error(LanguageKeys.Commands.Strip.LOG_FAILED, e, sender.name)
            }
        }
    }
}
