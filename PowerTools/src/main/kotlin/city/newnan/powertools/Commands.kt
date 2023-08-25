package city.newnan.powertools

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import org.bukkit.command.CommandSender
import city.newnan.violet.item.getSkull
import city.newnan.violet.item.toSkull
import org.bukkit.Bukkit
import org.bukkit.entity.Player


@CommandAlias("powertools")
object Commands : BaseCommand() {
    @Subcommand("reload")
    @CommandPermission("powertools.reload")
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

    @CommandAlias("skull url")
    @Subcommand("skull|head url")
    @CommandPermission("powertools.head")
    @Description("根据材质URL获取头颅")
    fun onUrlHead(sender: Player, url: String) {
        if (sender.inventory.addItem(url.toSkull()).size > 0) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c你的背包已满!")
            return
        }
        PluginMain.INSTANCE.messageManager.printf(sender, "§a成功获取头颅!")
    }

    @CommandAlias("skull player")
    @Subcommand("skull|head player")
    @CommandPermission("powertools.head")
    @Description("根据玩家名获取头颅")
    @CommandCompletion("@players")
    fun onPlayerHead(sender: Player, playerName: String) {
        Bukkit.getPlayer(playerName)?.let {
            if (sender.inventory.addItem(it.getSkull(1)).size > 0) {
                PluginMain.INSTANCE.messageManager.printf(sender, "§c你的背包已满!")
                return
            }
            PluginMain.INSTANCE.messageManager.printf(sender, "§a成功获取头颅!")
            return
        } ?: run {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c玩家 $playerName 不存在!")
        }
    }
}