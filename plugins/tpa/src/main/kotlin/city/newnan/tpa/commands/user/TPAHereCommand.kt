package city.newnan.tpa.commands.user

import city.newnan.tpa.TPAPlugin
import city.newnan.tpa.i18n.LanguageKeys
import city.newnan.tpa.modules.TPADirection
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * TPA Here传送命令
 *
 * 提供邀请其他玩家传送到自己位置的功能
 *
 * @author AI Assistant
 * @since 2.0.0
 */
class TPAHereCommand(private val plugin: TPAPlugin) {

    @Command("tpahere <player>")
    @CommandDescription(LanguageKeys.Commands.TPAHere.DESCRIPTION)
    @Permission("tpa.use")
    fun tpaHereCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.TPAHere.PLAYER_ARG) targetPlayer: Player
    ) {
        // 发送人必须是玩家
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Common.PLAYER_ONLY)
            return
        }

        if (sender == targetPlayer) {
            plugin.messager.printf(sender, LanguageKeys.Commands.TPAHere.SELF_REQUEST)
            return
        }
        
        plugin.getSessionManager().createSession(sender, targetPlayer, TPADirection.TARGET_TO_REQUESTER)
    }
}