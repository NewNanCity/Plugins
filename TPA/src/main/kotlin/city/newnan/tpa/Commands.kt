package city.newnan.tpa

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("tpa")
object Commands : BaseCommand() {
    @Subcommand("reload")
    @CommandPermission("tpa.reload")
    @Description("重载插件")
    fun reloadCommand(sender: CommandSender) {
        PluginMain.INSTANCE.reload()
        PluginMain.INSTANCE.messageManager.printf(sender, "重载完成!")
    }

    @HelpCommand
    @Subcommand("help")
    fun onHelp(sender: CommandSender, help: CommandHelp) {
        help.showHelp()
    }

    @Subcommand("block")
    @Description("屏蔽某玩家的传送请求")
    @CommandPermission("tpa.user")
    fun block(sender: Player, name: String) {
        val player = Bukkit.getOfflinePlayers().find { it.name == name }
        if (player != null) {
            if (sender == player) PluginMain.INSTANCE.messageManager.printf(sender, "?啥")
            else {
                PluginMain.INSTANCE.block(sender, player)
                PluginMain.INSTANCE.messageManager.printf(sender, "§a已屏蔽玩家 §f§l${player.name}§a 的TPA请求")
                PluginMain.INSTANCE.messageManager.printf(sender,
                    "你可以使用 §a/tpa blocklist§r 查看屏蔽列表, 或者使用 §a/tpa unblock ${player.name}§r 解除屏蔽")
            }
        } else {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c玩家不存在!")
        }
    }

    @Subcommand("unblock")
    @Description("取消屏蔽某玩家的传送请求")
    @CommandPermission("tpa.user")
    fun unblock(sender: Player, uuid: String) {
        val player = Bukkit.getOfflinePlayers().find { it.uniqueId.toString() == uuid }
        if (player != null) {
            if (sender == player) PluginMain.INSTANCE.messageManager.printf(sender, "?啥")
            else {
                PluginMain.INSTANCE.unblock(sender, player)
                PluginMain.INSTANCE.messageManager.printf(sender, "§a已取消屏蔽玩家 §f§l${player.name}§a 的TPA请求")
            }
        } else {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c玩家不存在!")
        }
    }

    @Subcommand("blocklist")
    @Description("查看屏蔽列表")
    @CommandPermission("tpa.user")
    fun blocklist(sender: Player) {
        PluginMain.INSTANCE.playerBlockSet[sender.uniqueId]?.let {
            PluginMain.INSTANCE.messageManager.printf(sender, "§a屏蔽列表: §f${
                it.mapNotNull { id -> Bukkit.getOfflinePlayer(id).name }.joinToString("§8, §f")}")
        } ?: PluginMain.INSTANCE.messageManager.printf(sender, "§a屏蔽列表: §8无")
    }

    @Default
    @CommandAlias("there")
    @Description("向某玩家发送传送请求, 使自己传送至对方所在位置")
    @CommandPermission("tpa.user")
    fun tpa(sender: Player, target: Player) {
        if (sender == target) PluginMain.INSTANCE.messageManager.printf(sender, "?啥")
        else PluginMain.INSTANCE.request(sender, target, false)
    }

    @Subcommand("here")
    @CommandAlias("tpahere")
    @Description("向某玩家发送传送请求, 使对方传送至自己所在位置")
    @CommandPermission("tpa.user")
    fun tpaHere(sender: Player, target: Player) {
        if (sender == target) PluginMain.INSTANCE.messageManager.printf(sender, "?啥")
        else PluginMain.INSTANCE.request(sender, target, true)
    }

    @Private
    @Subcommand("accept-tpa-request-by-id-private")
    @Description("接受某玩家的传送请求")
    @CommandPermission("tpa.user")
    fun accept(sender: Player, id: Long) {
        if (PluginMain.INSTANCE.responseYes(id) == null) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c请求已失效!")
        }
    }

    @Private
    @Subcommand("refuse-tpa-request-by-id-private")
    @Description("拒绝某玩家的传送请求")
    @CommandPermission("tpa.user")
    fun refuse(sender: Player, id: Long) {
        if (PluginMain.INSTANCE.responseNo(id) == null) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c请求已失效!")
        }
    }
}