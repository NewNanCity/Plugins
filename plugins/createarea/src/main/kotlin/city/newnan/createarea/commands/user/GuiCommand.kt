package city.newnan.createarea.commands.user

import city.newnan.createarea.CreateAreaPlugin
import city.newnan.createarea.gui.openCreateAreasGui
import city.newnan.createarea.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * GUI命令
 *
 * 打开创造区域管理GUI。
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class GuiCommand(private val plugin: CreateAreaPlugin) {

    /**
     * 打开创造区域GUI
     */
    @Command("createarea|carea gui|list")
    @CommandDescription(LanguageKeys.Commands.Gui.DESCRIPTION)
    @Permission("createarea.gui.all")
    fun guiCommand(sender: CommandSender) {
        // 检查是否为玩家
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        openCreateAreasGui(sender, plugin)
    }
}
