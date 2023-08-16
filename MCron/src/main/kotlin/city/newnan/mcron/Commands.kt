package city.newnan.mcron

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import org.bukkit.command.CommandSender
import java.util.*

@CommandAlias("cron|mcron")
object Commands : BaseCommand() {
    @Subcommand("reload")
    @CommandPermission("mcron.reload")
    @Description("重载插件")
    fun reloadCommand(sender: CommandSender) {
        PluginMain.INSTANCE.reload()
        PluginMain.INSTANCE.messageManager.printf(sender, "重载完成!")
    }

    @Subcommand("ls|list|show")
    @CommandPermission("mcron.list")
    @Description("列出即将执行的任务")
    fun listCronCommand(sender: CommandSender) {
        val now = System.currentTimeMillis()
        val dateFormatter = PluginMain.INSTANCE.cronManager.dateFormatter
        PluginMain.INSTANCE.messageManager.printf(sender, "所有任务:   §7现在时间${dateFormatter.format(Date(now))}")
        PluginMain.INSTANCE.cronManager.tasks.forEach {
            sender.sendMessage("§a§l${it.expression.expressionString} §r§7(下次执行: ${
                    dateFormatter.format(Date(it.expression.getNextTime(now)))})")
            it.commands.forEach { command -> sender.sendMessage("§7- §r$command") }
        }
        PluginMain.INSTANCE.cronManager.outdatedTasks.forEach {
            sender.sendMessage("§7§m${it.expression.expressionString} §r§7(已过期)")
            it.commands.forEach { command -> sender.sendMessage("§7- $command") }
        }
    }

    @Default
    @HelpCommand
    @Subcommand("help")
    fun onHelp(sender: CommandSender, help: CommandHelp) {
        help.showHelp()
    }
}