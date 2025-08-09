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
 * TPA接受命令
 *
 * 提供接受传送请求的功能
 *
 * @author AI Assistant
 * @since 2.0.0
 */
class AcceptCommand(private val plugin: TPAPlugin) {

    @Command("tpa-accept <id>")
    @CommandDescription(LanguageKeys.Commands.Accept.DESCRIPTION)
    @Permission("tpa.use")
    fun acceptCommand(
        sender: CommandSender,
        @Argument(value = "id", description = LanguageKeys.Commands.Accept.ID_ARG) id: Long
    ) {
        // 发送人必须是玩家
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Common.PLAYER_ONLY)
            return
        }

        plugin.getSessionManager().acceptSession(id, sender) ?: run {
            plugin.messager.printf(sender, LanguageKeys.Commands.Accept.REQUEST_EXPIRED)
        }
    }
}