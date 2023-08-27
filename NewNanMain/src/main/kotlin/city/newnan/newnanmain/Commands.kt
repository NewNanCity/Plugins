package city.newnan.newnanmain

import city.newnan.newnanmain.gui.openHomeGui
import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


@CommandAlias("newnan|nn")
object Commands : BaseCommand() {
    @Subcommand("reload")
    @CommandPermission("newnanmain.reload")
    @Description("重载插件")
    fun onReload(sender: CommandSender) {
        PluginMain.INSTANCE.reload()
        PluginMain.INSTANCE.messageManager.printf(sender, "插件已重载!")
    }

    @HelpCommand
    @Subcommand("help")
    fun onHelp(sender: CommandSender, help: CommandHelp) {
        help.showHelp()
    }

    @Subcommand("prefix player set")
    @CommandPermission("newnanmain.prefix.player.edit")
    fun onPrefixSet(sender: CommandSender, player: String, namespace: String, key: String) {
        Bukkit.selectEntities(sender, player).filterIsInstance<Player>().firstOrNull()?.also {
            PluginMain.INSTANCE.prefixManager.setPlayerPrefix(it, namespace, key)
        }
    }

    @Subcommand("prefix player remove")
    @CommandPermission("newnanmain.prefix.player.edit")
    fun onPrefixRemove(sender: CommandSender, player: String, namespace: String) {
        Bukkit.selectEntities(sender, player).filterIsInstance<Player>().firstOrNull()?.also {
            PluginMain.INSTANCE.prefixManager.removePlayerPrefix(it, namespace)
        }
    }

    @Subcommand("prefix player activate")
    @CommandPermission("newnanmain.prefix.player.edit")
    fun onPrefixActivate(sender: CommandSender, player: String, namespace: String) {
        Bukkit.selectEntities(sender, player).filterIsInstance<Player>().firstOrNull()?.also {
            PluginMain.INSTANCE.prefixManager.activatePlayerPrefix(it, namespace)
        }
    }

    @Default
    @Subcommand("gui")
    fun openGui(sender: Player) {
        val session = PluginMain.INSTANCE.gui[sender]
        session.clear()
        openHomeGui(session)
    }
}