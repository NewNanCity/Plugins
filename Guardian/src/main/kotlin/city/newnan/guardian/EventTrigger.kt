package city.newnan.guardian

import city.newnan.guardian.model.*
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import org.bukkit.BanList
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

fun PluginMain.inPlayerGroup(player: OfflinePlayer) =
    permission.playerInGroup(groupWorld, player, playersGroup)
fun PluginMain.inNewbieGroup(player: OfflinePlayer) =
    permission.playerInGroup(groupWorld, player, newbieGroup) && !permission.playerInGroup(groupWorld, player, playersGroup)
fun PluginMain.toPlayerGroup(player: OfflinePlayer) {
    permission.playerRemoveGroup(groupWorld, player, newbieGroup)
    permission.playerAddGroup(groupWorld, player, playersGroup)
}
fun org.bukkit.entity.Player.kickAsync(msg: String) = Schedulers.sync().run { kickPlayer(msg) }

val expireFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA)

fun PluginMain.handleBanMode(player: Player, gamePlayer: org.bukkit.entity.Player, banList: BanList) {
    when (player.banMode.toInt()) {
        0 -> {
            // 未被封禁
            banList.pardon(player.name)
            // 检查权限组，是不是Newbie组，如果是Newbie组 - 变成Player组
            if (inNewbieGroup(gamePlayer)) {
                toPlayerGroup(gamePlayer)
                // 迎新横幅
                Schedulers.sync().run {
                    gamePlayer.sendTitle("§c欢迎来到牛腩小镇", "§b恭喜你正式获得玩家资格！", 5, 40, 6)
                }
            }
            // 不是Newbie组，直接通过
        }
        1 -> {
            // 临时封禁
            val msg ="你已被临时封禁, 解禁时间: ${player.banExpire!!.format(expireFormatter)}"
            banList.addBan(player.name, msg,
                Date.from(player.banExpire!!.atZone(ZoneId.systemDefault()).toInstant()),
                "南宫寻雪")
            gamePlayer.kickAsync(msg)
        }
        2 -> {
            // 永久封禁
            val msg = "很遗憾，你已被永久封禁"
            banList.addBan(player.name, msg, null, "南宫寻雪")
            gamePlayer.kickAsync(msg)
        }
    }
}

fun PluginMain.enableTrigger() {
    Events.subscribe(PlayerJoinEvent::class.java, EventPriority.MONITOR)
        .filter { !it.player.hasPermission("guardian.bypass") }
        .handler {
            // 异步进行检测以免堵塞主线程
            val gamePlayer = it.player
            Schedulers.async().run {
                // 查询绑定信息
                val player = gamePlayer.name.findPlayer()
                if (player == null) {
                    // 没有记录
                    // 检查权限组，是不是Player组
                    if (inPlayerGroup(gamePlayer)) {
                        // 是Player组 - 不允许上线
                        // 这里是异步线程，直接调用用方法会产生` Asynchronous player kick!`的警告
                        gamePlayer.kickAsync("请联系群管理进行账号绑定")
                    }
                    // 如果不是Player组，允许上线
                    return@run
                }
                addPlayerIp(gamePlayer, player)
                // 设置城镇称号
                Schedulers.sync().run {
                    if (player.town == null) {
                        PluginMain.INSTANCE.server.dispatchCommand(PluginMain.INSTANCE.server.consoleSender,
                        "newnan prefix player remove ${gamePlayer.uniqueId} Town")
                    } else {
                        PluginMain.INSTANCE.server.dispatchCommand(PluginMain.INSTANCE.server.consoleSender,
                        "newnan prefix player set ${gamePlayer.uniqueId} Town ${player.town!!.id}")
                    }
                }
                if (player.inOtherServer()) {
                    // 在其他服务器上线
                    gamePlayer.kickAsync("你已在其他服务器上线")
                    return@run
                }
                player.also {
                    player.curServer = DBManager.serverId
                    val banList = Bukkit.getBanList(BanList.Type.NAME)
                    // 是否开启群检查模式
                    if (checkGroup) {
                        // 群检查模式
                        // 有记录
                        // 检查是否在群里
                        if (player.hasJoinGroup()) {
                            // 在群里
                            // 看ban时间是否已失效，失效则取消ban
                            if (player.tmpBan() and (player.banExpire?.isBefore(LocalDateTime.now()) != false)) {
                                player.banExpire = null
                                player.banMode = 0
                            }
                            // 看是不是被封禁玩家，并处理
                            handleBanMode(player, gamePlayer, banList)
                        } else {
                            // 不在群里
                            // 看是不是临时封禁，是的话变成永久封禁
                            if (player.tmpBan()) {
                                player.banMode = 2
                                player.banExpire = null
                            }
                            // 看是不是非ban
                            if (player.notBan()) {
                                // 非ban - 账号冻结玩家
                                banList.pardon(player.name)
                                gamePlayer.kickAsync("你的账号已冻结, 请加入牛腩QQ群或者QQ频道")
                            } else {
                                // 永久封禁
                                val msg = "很遗憾，你已被永久封禁"
                                banList.addBan(player.name, msg, null, "小雪")
                                gamePlayer.kickAsync(msg)
                            }
                        }
                    } else {
                        // 看是不是被封禁玩家，并处理
                        handleBanMode(player, gamePlayer, banList)
                    }
                }.flushChanges()
            }
        }.bindWith(this)

    Events.subscribe(PlayerQuitEvent::class.java, EventPriority.MONITOR)
        .filter { !it.player.hasPermission("guardian.bypass") }
        .handler { it.player.unlockServer() }
        .bindWith(this)
}