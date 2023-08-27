package city.newnan.mcron

import city.newnan.mcron.timeiterator.CronExpression
import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import org.bukkit.Bukkit
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
            if (it.expression !is CronExpression) return@forEach
            sender.sendMessage("§a§l${it.expression.expressionString} §r§7(下次执行: ${
                    dateFormatter.format(Date(it.expression.getNextTime(now)))})")
            it.commands.forEach { command -> sender.sendMessage("§7- §r$command") }
        }
        PluginMain.INSTANCE.cronManager.outdatedTasks.forEach {
            if (it.expression !is CronExpression) return@forEach
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

    @Subcommand("player add onjoin")
    @CommandPermission("mcron.player.push")
    @CommandCompletion("@players @nothing")
    fun onAddPlayerOnJoin(sender: CommandSender, playerName: String, command: String) {
        Bukkit.getOfflinePlayers().find { p -> p.name == playerName }?.uniqueId?.run {
            PluginMain.INSTANCE.pushPlayerJoinTask(this, command)
            PluginMain.INSTANCE.messageManager.printf(sender, "已为玩家 §a§l$playerName§r 添加任务, 将会在玩家加入后执行!")
        } ?: run {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c§l找不到玩家 §r§c§l$playerName§r §c!")
        }
    }
}