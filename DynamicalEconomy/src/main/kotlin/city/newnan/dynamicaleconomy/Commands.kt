package city.newnan.dynamicaleconomy

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import org.bukkit.command.CommandSender


@CommandAlias("dynamicaleconomy|de")
object Commands : BaseCommand() {
    @Subcommand("reload")
    @CommandPermission("dynamicaleconomy.reload")
    @Description("重载插件")
    fun onReload(sender: CommandSender) {
        PluginMain.INSTANCE.reload()
        PluginMain.INSTANCE.messageManager.printf(sender, "插件已重载!")
    }

    @Default
    @HelpCommand
    @Subcommand("help")
    fun onHelp(sender: CommandSender, help: CommandHelp) {
        help.showHelp()
    }

    @Subcommand("issue")
    @CommandPermission("dynamicaleconomy.issue")
    fun onIssue(sender: CommandSender, amount: Double) {
        PluginMain.INSTANCE.issueCurrency(amount)
        PluginMain.INSTANCE.messageManager.printf(sender, "已发行${amount}货币!")
    }
}