package city.newnan.template

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import org.bukkit.command.CommandSender


@CommandAlias("template")
object Commands : BaseCommand() {
    @Subcommand("reload")
    @CommandPermission("template.reload")
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
}