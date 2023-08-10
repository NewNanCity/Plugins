package city.newnan.feefly

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("feefly")
object Commands : BaseCommand() {
    @HelpCommand
    @Subcommand("help")
    fun help(sender: CommandSender, help: CommandHelp) {
        help.showHelp()
    }

    @Default
    @CommandCompletion("@players")
    fun toggleFly(sender: Player, @Optional target: OnlinePlayer?) {
        if (target == null) {
            if (sender.hasPermission("feefly.self")) {
                PluginMain.INSTANCE.toggleFly(sender)
            } else {
                PluginMain.INSTANCE.messageManager.printf(sender, "&c你没有权限这样做!")
            }
        } else {
            if (sender.hasPermission("feefly.other")) {
                PluginMain.INSTANCE.toggleFly(target.player)
            } else {
                PluginMain.INSTANCE.messageManager.printf(sender, "&c你没有权限这样做!")
            }
        }
    }

    @Subcommand("ls|list")
    @CommandPermission("feefly.list")
    fun listFlyingPlayers(sender: CommandSender) {
        PluginMain.INSTANCE.messageManager.printf(
            sender, "目前飞行人数: ${PluginMain.INSTANCE.flyingPlayers.size}")
        if (PluginMain.INSTANCE.flyingPlayers.size > 0) {
            PluginMain.INSTANCE.messageManager.printf(
                sender, "飞行中: ${
                    PluginMain.INSTANCE.flyingPlayers.map { it.key.name }.joinToString(" ")}"
            )
        }
    }

    @Subcommand("reload")
    @CommandPermission("feefly.reload")
    fun reloadConfig(sender: CommandSender) {
        PluginMain.INSTANCE.reload()
        PluginMain.INSTANCE.messageManager.printf(sender, "插件重载完毕!")
    }
}