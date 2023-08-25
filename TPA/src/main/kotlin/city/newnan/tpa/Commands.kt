package city.newnan.tpa

import city.newnan.tpa.gui.openBlockListGui
import city.newnan.tpa.gui.openOnlinePlayersGui
import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import co.aikar.commands.annotation.Optional
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

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
    fun block(sender: Player, playerName: String) {
        val player = Bukkit.getPlayer(playerName)
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
        val id = UUID.fromString(uuid)
        val player = Bukkit.getOfflinePlayer(id)
        if (!player.hasPlayedBefore()) {
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
        val session = PluginMain.INSTANCE.guiManager[sender]
        session.clear()
        openBlockListGui(session)
    }

    @Default
    @Subcommand("there")
    @CommandAlias("tpathere")
    @Description("向某玩家发送传送请求, 使自己传送至对方所在位置")
    @CommandPermission("tpa.user")
    @CommandCompletion("@players")
    fun tpa(sender: Player, @Optional target: OnlinePlayer? = null) {
        if (target == null) {
            val session = PluginMain.INSTANCE.guiManager[sender]
            session.clear()
            openOnlinePlayersGui(session, false) {
                if (!it.isOnline || it.uniqueId == session.player.uniqueId) return@openOnlinePlayersGui
                PluginMain.INSTANCE.request(sender, it.player!!, false)
                session.back()
            }
        } else {
            if (sender == target.player) PluginMain.INSTANCE.messageManager.printf(sender, "?啥")
            else PluginMain.INSTANCE.request(sender, target.player, false)
        }
    }

    @Subcommand("here")
    @CommandAlias("tpahere")
    @Description("向某玩家发送传送请求, 使对方传送至自己所在位置")
    @CommandPermission("tpa.user")
    fun tpaHere(sender: Player, @Optional target: OnlinePlayer? = null) {
        if (target == null) {
            val session = PluginMain.INSTANCE.guiManager[sender]
            session.clear()
            openOnlinePlayersGui(session, false) {
                if (!it.isOnline || it.uniqueId == session.player.uniqueId) return@openOnlinePlayersGui
                PluginMain.INSTANCE.request(sender, it.player!!, false)
                session.back()
            }
        } else {
            if (sender == target.player) PluginMain.INSTANCE.messageManager.printf(sender, "?啥")
            else PluginMain.INSTANCE.request(sender, target.player, true)
        }
    }

    @Private
    @Subcommand("accept-tpa-request-by-id-private")
    @Description("接受某玩家的传送请求")
    @CommandPermission("tpa.user")
    fun accept(sender: Player, id: Long) {
        if (PluginMain.INSTANCE.responseYes(id, sender) == null) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c请求已失效!")
        }
    }

    @Private
    @Subcommand("refuse-tpa-request-by-id-private")
    @Description("拒绝某玩家的传送请求")
    @CommandPermission("tpa.user")
    fun refuse(sender: Player, id: Long) {
        if (PluginMain.INSTANCE.responseNo(id, sender) == null) {
            PluginMain.INSTANCE.messageManager.printf(sender, "§c请求已失效!")
        }
    }
}