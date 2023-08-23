package city.newnan.guardian

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import org.bukkit.command.CommandSender
import java.util.*

@CommandAlias("guardian")
object Commands : BaseCommand() {
    @Subcommand("reload")
    @CommandPermission("mcron.reload")
    @Description("重载插件")
    fun reloadCommand(sender: CommandSender) {
        // PluginMain.INSTANCE.reload()
        // PluginMain.INSTANCE.messageManager.printf(sender, "重载完成!")
    }

    @Default
    @HelpCommand
    @Subcommand("help")
    fun onHelp(sender: CommandSender, help: CommandHelp) {
        help.showHelp()
    }
}