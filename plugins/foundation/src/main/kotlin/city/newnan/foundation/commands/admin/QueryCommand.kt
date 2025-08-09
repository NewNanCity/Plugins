package city.newnan.foundation.commands.admin

import city.newnan.foundation.FoundationPlugin
import city.newnan.foundation.i18n.LanguageKeys
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 查询命令
 *
 * 查询指定玩家的转账记录：
 * - 主动转账统计
 * - 被动转账统计
 * - 玩家在线状态
 * - 详细记录信息
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class QueryCommand(private val plugin: FoundationPlugin) {

    @Command("foundation|fund query <player>")
    @CommandDescription(LanguageKeys.Commands.Query.DESCRIPTION)
    @Permission("foundation.query")
    fun queryCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.Query.PLAYER) player: OfflinePlayer
    ) {
        try {
            val record = plugin.transferManager.getTransferRecord(player.uniqueId) ?: run {
                plugin.messager.printf(sender, LanguageKeys.Commands.Query.NO_RECORD, player.name ?: "Unknown")
                return
            }

            plugin.messager.printf(sender, LanguageKeys.Commands.Query.HEADER, player.name ?: "Unknown")
            plugin.messager.printf(sender, LanguageKeys.Commands.Query.ACTIVE_DONATIONS, String.format("%.2f", record.active))
            plugin.messager.printf(sender, LanguageKeys.Commands.Query.PASSIVE_DONATIONS, String.format("%.2f", record.passive))
            plugin.messager.printf(sender, LanguageKeys.Commands.Query.TOTAL_DONATIONS, String.format("%.2f", record.active + record.passive))

        } catch (e: Exception) {
            plugin.logger.error(LanguageKeys.Commands.Query.LOG_FAILED, e)
            plugin.messager.printf(sender, LanguageKeys.Core.Error.FAILED, e.message ?: "Unknown error")
        }
    }
}
