package city.newnan.foundation.commands.user

import city.newnan.foundation.FoundationPlugin
import city.newnan.foundation.gui.openTopGui
import city.newnan.foundation.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Default
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission

/**
 * 排行榜命令
 *
 * 显示基金会捐赠排行榜：
 * - 分页显示排行榜
 * - 当前玩家高亮显示
 * - 玩家排名提示（不在当前页时）
 * - 详细的统计信息
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class TopCommand(private val plugin: FoundationPlugin) {

    @Command("ftop [page]")
    @Command("foundation|fund top [page]")
    @CommandDescription(LanguageKeys.Commands.Top.DESCRIPTION)
    @Permission("foundation.top")
    fun topCommand(
        sender: CommandSender,
        @Argument(value = "page", description = LanguageKeys.Commands.Top.PAGE) @Default("1") page: Int,
        @Flag(value = "console", aliases = ["c"], description = LanguageKeys.Commands.Top.Console) console: Boolean
        ) {
        if (!console && sender is Player) {
            openTopGui(plugin, sender)
            return
        }

        if (page <= 0) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Top.INVALID_PAGE)
            return
        }
        val offset = (page - 1) * 10
        try {
            val records = plugin.transferManager.getTopDonors(offset, 10)

            if (records.items.isEmpty()) {
                plugin.messager.printf(sender, LanguageKeys.Commands.Top.OUT_OF_RANGE)
                return
            }

            plugin.messager.printf(sender, LanguageKeys.Commands.Top.HEADER, page, records.totalCount)

            records.items.forEachIndexed { index, (uuid, record) ->
                val player = plugin.server.getOfflinePlayer(uuid)
                val name = player.name ?: "Unknown"
                val amount = String.format("%.2f", record.toDouble())

                plugin.messager.printf(sender, LanguageKeys.Commands.Top.ENTRY, index + offset + 1, name, amount)
            }

        } catch (e: Exception) {
            plugin.logger.error(LanguageKeys.Commands.Top.LOG_FAILED, e)
            plugin.messager.printf(sender, LanguageKeys.Core.Error.FAILED, e.message ?: "Unknown error")
        }
    }
}
