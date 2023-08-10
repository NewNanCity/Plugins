package city.newnan.mcron

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import org.bukkit.command.CommandSender


@CommandAlias("cron|mcron")
object Commands : BaseCommand() {
    @Subcommand("reload")
    @CommandPermission("mcron.reload")
    @Description("{@@msg.help-reload}")
    fun reloadCommand(sender: CommandSender?) {
        PluginMain.INSTANCE.reload()
        PluginMain.INSTANCE.messageManager.printf(sender, "\$msg.reload$")
    }

    @Subcommand("ls|list|show")
    @CommandAlias("lscron")
    @CommandPermission("mcron.list")
    @Description("{@@msg.help-list}")
    fun listCronCommand(sender: CommandSender?) {
        PluginMain.INSTANCE.cronManager.listCron(sender)
    }

    @Default
    @HelpCommand
    @Subcommand("help")
    fun onHelp(sender: CommandSender?, help: CommandHelp) {
        PluginMain.INSTANCE.messageManager.printf(sender, "\$msg.help-head$")
        help.showHelp()
    }
}