package city.newnan.tpa.commands.user

import city.newnan.tpa.TPAPlugin
import city.newnan.tpa.gui.openOnlinePlayersGui
import city.newnan.tpa.i18n.LanguageKeys
import city.newnan.tpa.modules.TPADirection
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * TPA传送命令
 *
 * 提供向其他玩家发送传送请求的功能
 *
 * @author AI Assistant
 * @since 2.0.0
 */
class TPACommand(private val plugin: TPAPlugin) {

    @Command("tpathere|tpa [player]")
    @CommandDescription(LanguageKeys.Commands.TPAThere.DESCRIPTION)
    @Permission("tpa.use")
    fun tpaThereCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.TPAThere.PLAYER_ARG) targetPlayer: Player?
    ) {
        // 发送人必须是玩家
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Common.PLAYER_ONLY)
            return
        }

        // 如果没有指定玩家，打开GUI
        if (targetPlayer == null) {
            openOnlinePlayersGui(plugin, sender)
            return
        }

        if (sender == targetPlayer) {
            plugin.messager.printf(sender, LanguageKeys.Commands.TPAThere.SELF_REQUEST)
            return
        }
        
        plugin.getSessionManager().createSession(sender, targetPlayer, TPADirection.REQUESTER_TO_TARGET)
    }
}