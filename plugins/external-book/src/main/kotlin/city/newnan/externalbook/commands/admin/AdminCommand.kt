package city.newnan.externalbook.commands.admin

import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.gui.authorlist.openAuthorListGui
import city.newnan.externalbook.gui.authorbooks.openPlayerBooksGui
import city.newnan.externalbook.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 管理员命令
 *
 * 管理员相关功能的入口命令。
 * 完整保留原有AdminCommand的逻辑。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class AdminCommand(private val plugin: ExternalBookPlugin) {

    /**
     * 管理员命令处理方法
     */
    @Command("externalbook|book admin")
    @CommandDescription(LanguageKeys.Commands.Admin.DESCRIPTION)
    @Permission("externalbook.admin")
    fun adminCommand(sender: CommandSender) {
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        try {
            plugin.messager.printf(sender, LanguageKeys.Commands.Admin.GUI_OPENING)
            openAuthorListGui(plugin, sender) { target ->
                openPlayerBooksGui(plugin, sender, target)
            }
        } catch (e: Exception) {
            plugin.logger.error(LanguageKeys.Commands.Admin.LOG_FAILED, e, sender.name)
            plugin.messager.printf(sender, LanguageKeys.Commands.Admin.GUI_FAILED)
        }
    }
}
