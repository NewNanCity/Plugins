package city.newnan.externalbook.commands.admin

import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runSync
import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.book.readBook
import city.newnan.externalbook.i18n.LanguageKeys
import com.github.f4b6a3.ulid.Ulid
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 打开书籍命令
 *
 * 为指定玩家打开指定UUID的书籍。
 * 需要管理员权限。
 * 完整保留原有OpenCommand的逻辑。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class OpenCommand(private val plugin: ExternalBookPlugin) {

    /**
     * 打开命令处理方法
     */
    @Command("externalbook|book open <player> <ulid>")
    @CommandDescription(LanguageKeys.Commands.Open.DESCRIPTION)
    @Permission("externalbook.open")
    fun openCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.Open.PLAYER) target: Player,
        @Argument(value = "ulid", description = LanguageKeys.Commands.Open.ULID) ulidString: String
    ) {
        try {
            val librarian = plugin.librarian ?: run {
                plugin.logger.error(LanguageKeys.Commands.Common.LOG_LIBRARIAN_NOT_AVAILABLE)
                plugin.messager.printf(sender, LanguageKeys.Commands.Common.LIBRARIAN_NOT_AVAILABLE)
                return@openCommand
            }

            // 验证书籍ID格式
            val bookId = try {
                Ulid.from(ulidString)
            } catch (e: IllegalArgumentException) {
                plugin.messager.printf(sender, LanguageKeys.Core.Common.INVALID_BOOK_ID)
                return@openCommand
            }

            // 异步执行数据库查询，避免阻塞主线程
            plugin.runAsync<Unit> {
                try {
                    // 获取书籍数据
                    val book = librarian[bookId] ?: run {
                        plugin.runSync<Unit> {
                            plugin.messager.printf(sender, LanguageKeys.Commands.Open.BOOK_NOT_FOUND)
                        }
                        return@runAsync
                    }

                    // 回到主线程进行书籍打开操作
                    plugin.runSync {
                        // 为目标玩家打开书籍
                        book.readBook(target)

                        // 发送成功消息
                        plugin.messager.printf(sender, LanguageKeys.Commands.Open.SUCCESS, target.name, book.title)
                        plugin.logger.info(LanguageKeys.Commands.Open.LOG_SUCCESS, sender.name, target.name, book.title)
                    }

                } catch (e: Exception) {
                    // 异步操作中的异常处理
                    plugin.logger.error(LanguageKeys.Commands.Open.LOG_FAILED, e, sender.name, target.name)
                    plugin.runSync<Unit> {
                        plugin.messager.printf(sender, LanguageKeys.Commands.Open.FAILED)
                    }
                }
            }

        } catch (e: IllegalArgumentException) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.INVALID_UUID)
        } catch (e: Exception) {
            plugin.logger.error(LanguageKeys.Commands.Open.LOG_FAILED, e, sender.name, target.name)
            plugin.messager.printf(sender, LanguageKeys.Commands.Open.FAILED)
        }
    }
}