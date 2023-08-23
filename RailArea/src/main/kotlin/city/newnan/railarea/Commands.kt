package city.newnan.railarea

import city.newnan.railarea.gui.openRailAreaGui
import city.newnan.railarea.gui.openRailLineGui
import city.newnan.railarea.gui.openRailLinesGui
import city.newnan.railarea.input.handleAreaInput
import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


@CommandAlias("rail")
object Commands : BaseCommand() {
    @HelpCommand
    @Subcommand("help")
    fun help(sender: CommandSender, help: CommandHelp) {
        help.showHelp()
    }

    @Default
    @Subcommand("gui")
    @Description("显示所有区域")
    @CommandPermission("rail-area.edit")
    fun showGui(sender: Player) {
        val session = PluginMain.INSTANCE.guiManager[sender]
        session.clear()
        openRailLinesGui(session, true) { line ->
            openRailLineGui(session, true, line) { station ->
                openRailAreaGui(session, true, line, station) {}
            }
        }
    }

    @Subcommand("new-area")
    @Description("创建区域")
    @CommandPermission("rail-area.edit")
    fun newArea(sender: Player) {
        val session = PluginMain.INSTANCE.guiManager[sender]
        session.clear()
        handleAreaInput(session, null) { area ->
            if (area != null) {
                PluginMain.INSTANCE.addArea(area)
                PluginMain.INSTANCE.messageManager.printf(sender, "区域已创建!")
                PluginMain.INSTANCE.checkPlayer(sender)
            }
            session.clear()
        }
    }

    @Subcommand("reload")
    @Description("重载插件")
    @CommandPermission("rail-area.reload")
    fun reloadConfig(sender: CommandSender) {
        PluginMain.INSTANCE.reload()
        PluginMain.INSTANCE.messageManager.printf(sender, "插件重载完毕!")
    }
}