package city.newnan.railexpress

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import org.bukkit.command.CommandSender


@CommandAlias("railexpress")
object RailExpressCommand : BaseCommand() {
    @Subcommand("reload")
    @CommandPermission("railexpress.reload")
    @Description("{@@msg.help-reload}")
    fun onReload(sender: CommandSender?) {
        RailExpress.instance?.reload()
        RailExpress.instance?.messageManager?.printf(sender, "\$msg.reload$")
    }

    @HelpCommand
    fun onHelp(sender: CommandSender?, help: CommandHelp) {
        RailExpress.instance?.messageManager?.printf(sender, "\$msg.help-head$")
        help.showHelp()
    }
}