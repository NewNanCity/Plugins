package city.newnan.guardian

import city.newnan.guardian.gui.openTownGui
import city.newnan.guardian.model.*
import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import co.aikar.commands.annotation.Optional
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.map
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@CommandAlias("guardian")
object Commands : BaseCommand() {
//    @Subcommand("reload")
//    @CommandPermission("guardian.reload")
//    @Description("重载插件")
//    fun reloadCommand(sender: CommandSender) {
//        PluginMain.INSTANCE.reload()
//        PluginMain.INSTANCE.message.printf(sender, "重载完成!")
//    }

    @Default
    @HelpCommand
    @Subcommand("help")
    fun onHelp(sender: CommandSender, help: CommandHelp) {
        help.showHelp()
    }

    @Subcommand("judge|judgemental")
    @CommandAlias("judge|judgemental")
    fun onJudgemental(sender: Player) {
        if (!PluginMain.INSTANCE.judgementalPlayers.contains(sender.uniqueId)) return
        if (PluginMain.INSTANCE.permission.playerInGroup(sender.world.name, sender, PluginMain.INSTANCE.judgementalGroup)) {
            // 如果玩家当前是风纪委员组，就切换回玩家组
            PluginMain.INSTANCE.permission.playerRemoveGroup(sender.world.name, sender, PluginMain.INSTANCE.judgementalGroup)
            sender.gameMode = GameMode.SURVIVAL
            Bukkit.getServer().broadcastMessage("§7[§6牛腩小镇§7]§r 玩家 §e§l${sender.name}§f 加入服务器")
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().consoleSender, "vanish ${sender.name} disable")
        } else {
            // 否则就进入风纪委员组
            PluginMain.INSTANCE.permission.playerAddGroup(sender.world.name, sender, PluginMain.INSTANCE.judgementalGroup)
            sender.gameMode = GameMode.SPECTATOR
            Bukkit.getServer().broadcastMessage("§7[§6牛腩小镇§7]§r 玩家 §e§l${sender.name}§f 退出服务器")
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().consoleSender, "vanish ${sender.name} enable")
        }
    }

    @Subcommand("judgement list|ls")
    @CommandPermission("guardian.judgemental.edit")
    fun onListJudgement(sender: CommandSender) {
        val players = PluginMain.INSTANCE.judgementalPlayers.map { Bukkit.getOfflinePlayer(it) }.filter { it.hasPlayedBefore() }
        PluginMain.INSTANCE.message.also {
            it.printf(sender, "§6风纪委员列表:")
            it.printf(sender, "§f${players.joinToString("§7, §r") { p -> p.name ?: "§c未知" }}§r")
        }
    }

    @Subcommand("judgement add")
    @CommandPermission("guardian.judgemental.edit")
    @CommandCompletion("@players")
    fun onAddJudgement(sender: CommandSender, target: String) {
        val p = Bukkit.getPlayer(target)
        if (p == null) {
            PluginMain.INSTANCE.message.printf(sender, "§c玩家 §f$target §c不存在!")
            return
        }
        if (PluginMain.INSTANCE.judgementalPlayers.contains(p.uniqueId)) {
            PluginMain.INSTANCE.message.printf(sender, "§c玩家 §f$target §c已经是风纪委员了!")
            return
        }
        PluginMain.INSTANCE.judgementalPlayers.add(p.uniqueId)
        PluginMain.INSTANCE.save()
        PluginMain.INSTANCE.message.printf(sender, "§a玩家 §f$target §a已经成为风纪委员!")
    }

    @Subcommand("judgement remove")
    @CommandPermission("guardian.judgemental.edit")
    @CommandCompletion("@players")
    fun onRemoveJudgement(sender: CommandSender, playerName: String) {
        val p = Bukkit.getPlayer(playerName)
        if (p == null) {
            PluginMain.INSTANCE.message.printf(sender, "§c玩家 §f$playerName §c不存在!")
            return
        }
        if (!PluginMain.INSTANCE.judgementalPlayers.contains(p.uniqueId)) {
            PluginMain.INSTANCE.message.printf(sender, "§c玩家 §f$playerName §c不是风纪委员!")
            return
        }
        PluginMain.INSTANCE.judgementalPlayers.remove(p.uniqueId)
        PluginMain.INSTANCE.save()
        PluginMain.INSTANCE.message.printf(sender, "§a玩家 §f$playerName §a已经不再是风纪委员!")
        val player = p.player
        if (player != null && PluginMain.INSTANCE.permission.playerInGroup(player.world.name, player, PluginMain.INSTANCE.judgementalGroup)) {
            player.gameMode = GameMode.SURVIVAL
            Bukkit.getServer().broadcastMessage("§7[§6牛腩小镇§7]§r 玩家 §e§l${p.name}§f 加入服务器")
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().consoleSender, "vanish ${sender.name} disable")
        }
    }

    @Subcommand("lookup")
    @CommandAlias("lookup")
    @CommandCompletion("@players")
    @CommandPermission("guardian.lookup")
    fun onLookup(sender: CommandSender, playerName: String) {
        val p = Bukkit.getPlayer(playerName)
        if (p == null) {
            PluginMain.INSTANCE.message.printf(sender, "§c玩家 §f$playerName §c不存在!")
            return
        }
        val r = playerName.findPlayer()
        if (r == null) {
            PluginMain.INSTANCE.message.printf(sender, "§c玩家 §f$playerName §c不存在!")
            return
        }
        val ips = DBManager.db.playerIps.filter { it.id eq r.id }.map { it.ip }
        PluginMain.INSTANCE.message.also {
            it.printf(sender, "§7昵称:   §f${p.name}")
            it.printf(sender, "§7UUID:    §f${p.uniqueId}")
            it.printf(sender, "§7重生点: §f${p.bedSpawnLocation?.world?.name} ${p.bedSpawnLocation?.blockX} ${p.bedSpawnLocation?.blockY} ${p.bedSpawnLocation?.blockZ}")
            it.printf(sender, "§7在线时长: §f${millisecondToDuration(p.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) * 50L)}")
            it.printf(sender, "§7QQ:      §f${r.qq ?: "无"} ${if (r.qq == null) "" else if (r.inQQGroup) "§a[已加入]" else "§c[未加入]"}")
            it.printf(sender, "§7频道:    §f${r.qqguild ?: "无"} ${if (r.qqguild == null) "" else if (r.inQQGuild) "§a[已加入]" else "§c[未加入]"}")
            it.printf(sender, "§7Discord: §f${r.discord ?: "无"} ${if (r.discord == null) "" else if (r.inDiscord) "§a[已加入]" else "§c[未加入]"}")
            it.printf(sender, "§7第一次登录时间:   §f${if (p.firstPlayed > 0) millisecondToTime(p.firstPlayed) else "§7未登录过"}")
            it.printf(sender, "§7最后一次登录时间: §f${if (p.lastPlayed > 0) millisecondToTime(p.lastPlayed) else "§7未登录过"}")
            if (r.notBan()) it.printf(sender, "§7封禁状态: §a未封禁")
            else if (r.tmpBan()) it.printf(sender, "§7封禁状态: §e临时封禁 §7至 §f${r.banExpire!!.format(expireFormatter)}")
            else if (r.forceBan()) it.printf(sender, "§7封禁状态: §c永久封禁")
            it.printf(sender, "§7小镇: §f${r.town?.name ?: "§7未加入小镇"}")
            it.printf(sender, "§7现金: §f${PluginMain.INSTANCE.economy.getBalance(p)} §7₦")
            it.printf(sender, "§7历史IP: §f${ips.joinToString(", ")}")
        }
    }

    @Subcommand("town")
    @CommandAlias("town")
    fun showTown(sender: Player, @Optional townName: String? = null) {
        if (townName != null && !sender.hasPermission("guardian.town.read.other")) {
            PluginMain.INSTANCE.message.printf(sender, "§c你没有权限查看其他小镇的信息!")
            return
        }
        val player = sender.name.findPlayer()
        val town = if (townName == null) player?.town else townName.findTown()
        if (town == null) {
            if (townName == null) {
                PluginMain.INSTANCE.message.printf(sender, "§c你还没有加入任何小镇!")
            } else {
                PluginMain.INSTANCE.message.printf(sender, "§c小镇 §f${townName} &c不存在!")
            }
            return
        }
        val session = PluginMain.INSTANCE.gui[sender]
        session.clear()
        openTownGui(session, sender, player!!, town)
    }
}

val dataFormatter = SimpleDateFormat("yyyy年M月d日 HH:mm:ss").also {
    it.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
}

fun millisecondToTime(millisecond: Long): String =
    dataFormatter.format(Date(millisecond))

fun millisecondToDuration(millisecond: Long): String {
    val day = TimeUnit.MILLISECONDS.toDays(millisecond)
    val hour = TimeUnit.MILLISECONDS.toHours(millisecond) - day * 24
    val minute = TimeUnit.MILLISECONDS.toMinutes(millisecond) - day * 24 * 60 - hour * 60
    val second = TimeUnit.MILLISECONDS.toSeconds(millisecond) - day * 24 * 60 * 60 - hour * 60 * 60 - minute * 60
    return "${day}天${hour}小时${minute}分钟${second}秒"
}
