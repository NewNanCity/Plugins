package city.newnan.mcron

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import org.bukkit.command.CommandSender


@CommandAlias("cron|mcron")
object CronCommands : BaseCommand() {
    @Subcommand("reload")
    @CommandPermission("mcron.reload")
    @Description("{@@msg.help-reload}")
    fun reloadCommand(sender: CommandSender?) {
        MCron.INSTANCE.reload()
        MCron.INSTANCE.messageManager.printf(sender, "\$msg.reload$")
    }

    @Subcommand("ls|list|show")
    @CommandAlias("lscron")
    @CommandPermission("mcron.list")
    @Description("{@@msg.help-list}")
    fun listCronCommand(sender: CommandSender?) {
        MCron.INSTANCE.cronManager.listCron(sender)
    }

    @HelpCommand
    @Default
    fun onHelp(sender: CommandSender?, help: CommandHelp) {
        MCron.INSTANCE.messageManager.printf(sender, "\$msg.help-head$")
        help.showHelp()
    }
}