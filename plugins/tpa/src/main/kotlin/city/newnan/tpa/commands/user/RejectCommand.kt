package city.newnan.tpa.commands.user

import city.newnan.tpa.TPAPlugin
import city.newnan.tpa.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * TPA拒绝命令
 *
 * 提供拒绝传送请求的功能
 *
 * @author AI Assistant
 * @since 2.0.0
 */
class RejectCommand(private val plugin: TPAPlugin) {

    @Command("tpa-reject <id>")
    @CommandDescription(LanguageKeys.Commands.Reject.DESCRIPTION)
    @Permission("tpa.use")
    fun rejectCommand(
        sender: CommandSender,
        @Argument(value = "id", description = LanguageKeys.Commands.Reject.ID_ARG) id: Long
    ) {
        // 发送人必须是玩家
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Common.PLAYER_ONLY)
            return
        }

        plugin.getSessionManager().rejectSession(id, sender) ?: run {
            plugin.messager.printf(sender, LanguageKeys.Commands.Reject.REQUEST_EXPIRED)
        }
    }
}