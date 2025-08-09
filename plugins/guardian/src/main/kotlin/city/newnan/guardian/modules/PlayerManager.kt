package city.newnan.guardian.modules

import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runSync
import city.newnan.core.utils.text.ComponentParseMode
import city.newnan.core.utils.text.toComponent
import city.newnan.guardian.GuardianPlugin
import city.newnan.guardian.model.BanMode
import city.newnan.guardian.model.Player
import city.newnan.guardian.model.Town
import com.destroystokyo.paper.profile.PlayerProfile
import net.kyori.adventure.title.Title
import org.bukkit.BanList
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.Statistic
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID

class PlayerManager(val plugin: GuardianPlugin) : BaseModule("PlayerManager", plugin) {
    var checkGroup: Boolean = true
    var groupWorld: String = ""
    var newbieGroup: String = ""
    var playersGroup: String = ""

    val expireFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA)

    init { init() }

    override fun onInit() {
        subscribeEvent<PlayerJoinEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.player.hasPermission("guardian.bypass") }
            handler { event ->
                runAsync {
                    // 查询绑定信息
                    val player = plugin.playerDB.onPlayerLogin(event.player)
                    if (player == null) {
                        // 没有记录
                        // 检查权限组，是不是Player组
                        if (inPlayerGroup(event.player)) {
                            // 是Player组 - 不允许上线
                            runSync {
                                event.player.kick("请联系群管理进行账号绑定".toComponent(ComponentParseMode.Plain), PlayerKickEvent.Cause.ILLEGAL_ACTION)
                            }
                        }
                        // 如果不是Player组，允许上线
                        return@runAsync
                    }
                    // 设置城镇称号
                    if (player.town != null) {
                        runSync {
                            plugin.server.dispatchCommand(plugin.server.consoleSender,
                                "newnan prefix player set ${event.player.uniqueId} Town ${player.town}")
                        }
                    } else {
                        runSync {
                            plugin.server.dispatchCommand(plugin.server.consoleSender,
                                "newnan prefix player remove ${event.player.uniqueId} Town")
                        }
                    }
                    // 在其他服务器上线
                    if (player.curServer != plugin.playerDB.serverId) {
                        runSync {
                            event.player.kick("你已在其他服务器上线".toComponent(ComponentParseMode.Plain), PlayerKickEvent.Cause.ILLEGAL_ACTION)
                        }
                        return@runAsync
                    }
                    player.also {
                        val banList = Bukkit.getBanList<BanList<PlayerProfile>>(BanList.Type.PROFILE)
                        // 是否开启群检查模式
                        if (checkGroup) {
                            // 群检查模式
                            // 有记录
                            // 检查是否在群里
                            if (player.hasJoinGroup) {
                                // 在群里
                                // 看ban时间是否已失效，失效则取消ban
                                if (player.tempBanned and (player.banExpire == null || player.banExpire.isBefore(
                                        LocalDateTime.now()))) {
                                    plugin.playerDB.updatePlayerStatus(player.id, BanMode.NOT_BANNED.value, null, true)
                                }
                                // 看是不是被封禁玩家，并处理
                                handleBanMode(player, event.player, banList)
                            } else {
                                // 不在群里
                                // 看是不是临时封禁，是的话变成永久封禁
                                if (player.tempBanned) {
                                    plugin.playerDB.updatePlayerStatus(player.id, BanMode.PERMANENT_BANNED.value, null, false)
                                }
                                // 看是不是非ban
                                if (!player.banned) {
                                    // 非ban - 账号冻结玩家
                                    banList.pardon(event.player.playerProfile)
                                    runSync {
                                        event.player.kick("你的账号已冻结, 请加入牛腩QQ群或者QQ频道".toComponent(ComponentParseMode.Plain), PlayerKickEvent.Cause.ILLEGAL_ACTION)
                                    }
                                } else {
                                    // 永久封禁
                                    handleBanMode(player, event.player, banList)
                                }
                            }
                        } else {
                            // 看是不是被封禁玩家，并处理
                            handleBanMode(player, event.player, banList)
                        }
                    }
                    return@runAsync
                }
            }
        }
        subscribeEvent<PlayerQuitEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                runAsync {
                    event.player.resetPlayerTime()
                    plugin.playerDB.onPlayerLogout(event.player)
                }
            }
        }
    }

    override fun onReload() {
        val config = plugin.getPluginConfig()
        checkGroup = config.checkGroup
        groupWorld = config.groupWorld
        newbieGroup = config.newbieGroup
        playersGroup = config.playersGroup
    }

    fun inPlayerGroup(player: OfflinePlayer) =
        plugin.permission.playerInGroup(groupWorld, player, playersGroup)

    fun inNewbieGroup(player: OfflinePlayer) =
        plugin.permission.playerInGroup(groupWorld, player, newbieGroup) && !plugin.permission.playerInGroup(groupWorld, player, playersGroup)

    fun toPlayerGroup(player: OfflinePlayer) {
        plugin.permission.playerRemoveGroup(groupWorld, player, newbieGroup)
        plugin.permission.playerAddGroup(groupWorld, player, playersGroup)
    }

    fun handleBanMode(player: Player, gamePlayer: org.bukkit.entity.Player, banList: BanList<PlayerProfile>) {
        runSync {
            when (player.banMode) {
                0 -> {
                    // 未被封禁
                    banList.pardon(gamePlayer.playerProfile)
                    // 检查权限组，是不是Newbie组，如果是Newbie组 - 变成Player组
                    if (inNewbieGroup(gamePlayer)) {
                        toPlayerGroup(gamePlayer)
                        // 迎新横幅
                        gamePlayer.playSound(gamePlayer, org.bukkit.Sound.MUSIC_GAME, 1.0f, 1.0f)
                        gamePlayer.showTitle(
                            Title.title(
                                "§c欢迎来到牛腩小镇".toComponent(),
                                "§b恭喜你正式获得玩家资格！".toComponent(),
                                Title.Times.times(
                                    Duration.ofMillis(1000),
                                    Duration.ofMillis(7000),
                                    Duration.ofMillis(2000)
                                )
                            )
                        )
                    }
                    // 不是Newbie组，直接通过
                }
                1 -> {
                    // 临时封禁
                    val msg ="你已被临时封禁, 解禁时间: ${player.banExpire!!.format(expireFormatter)}"
                    banList.addBan(gamePlayer.playerProfile, msg,
                        Date.from(player.banExpire!!.atZone(ZoneId.systemDefault()).toInstant()),
                        "南宫寻雪")
                    gamePlayer.kick(msg.toComponent(ComponentParseMode.Plain), PlayerKickEvent.Cause.BANNED)
                }
                2 -> {
                    // 永久封禁
                    val msg = "很遗憾，你已被永久封禁"
                    banList.addBan(gamePlayer.playerProfile, msg, null as Date?, "南宫寻雪")
                    gamePlayer.kick(msg.toComponent(ComponentParseMode.Plain), PlayerKickEvent.Cause.BANNED)
                }
            }
        }
    }

    fun getPlayerInfo(player: OfflinePlayer): PlayerInfo {
        val playerRecord = player.name?.let { plugin.playerDB.getPlayerByName(it) }
        val town = playerRecord?.town?.let { plugin.playerDB.getTownById(it) }
        val ips = playerRecord?.id?.let { plugin.playerDB.getPlayerIps(it) } ?: emptyList()
        return PlayerInfo(
            player.name ?: "Unknown",
            player.uniqueId,
            plugin.economy.getBalance(player),
            player.bedSpawnLocation,
            player.firstPlayed,
            player.lastLogin,
            player.getStatistic(Statistic.PLAY_ONE_MINUTE) * 50L,
            playerRecord,
            town,
            ips
        )
    }

    data class PlayerInfo(
        val name: String,
        val uuid: UUID,
        val cash: Double,
        val bedSpawnLocation: Location?,
        val firstPlayed: Long,
        val lastPlayed: Long,
        val playTime: Long,
        val record: Player?,
        val town: Town?,
        val ips: List<String>
    )
}