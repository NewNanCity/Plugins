package city.newnan.externalbook.commands.user

import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.gui.authorbooks.openPlayerBooksGui
import city.newnan.externalbook.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * GUI命令
 *
 * 打开书籍管理GUI界面，显示玩家的所有书籍。
 * 完整保留原有GuiCommand的逻辑。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class GuiCommand(private val plugin: ExternalBookPlugin) {

    /**
     * GUI命令处理方法
     */
    @Command("externalbook|book gui")
    @CommandDescription(LanguageKeys.Commands.Gui.DESCRIPTION)
    @Permission("externalbook.use")
    fun guiCommand(sender: CommandSender) {
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        try {
            plugin.messager.printf(sender, LanguageKeys.Commands.Gui.OPENING)
            openPlayerBooksGui(plugin, sender, sender)
            plugin.logger.info(LanguageKeys.Commands.Gui.LOG_OPENED, sender.name)
        } catch (e: Exception) {
            plugin.logger.error(LanguageKeys.Commands.Gui.LOG_FAILED, e, sender.name)
            plugin.messager.printf(sender, LanguageKeys.Commands.Gui.FAILED)
        }
    }
}
