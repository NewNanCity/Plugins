package city.newnan.guardian.modules

import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runAsyncRepeating
import city.newnan.core.scheduler.runSync
import city.newnan.core.utils.text.ComponentParseMode
import city.newnan.core.utils.text.toComponent
import city.newnan.guardian.GuardianPlugin
import city.newnan.guardian.sdk.models.BanMode
import city.newnan.guardian.sdk.models.BanMode.*
import city.newnan.guardian.sdk.models.GetPlayerRequest
import city.newnan.guardian.sdk.models.GetTownRequest
import city.newnan.guardian.sdk.models.ListPlayersRequest
import city.newnan.guardian.sdk.models.Player
import city.newnan.guardian.sdk.models.PlayerValidateInfo
import city.newnan.guardian.sdk.models.SetPlayersOfflineRequest
import city.newnan.guardian.sdk.models.Town
import city.newnan.guardian.sdk.models.ValidateRequest
import city.newnan.guardian.utils.getProtocol
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
    var serverId: Int = -1
    var playerCheck: Boolean = false
    val serverVersion = Bukkit.getBukkitVersion().split("-", limit = 2).first()
    val serverProtocolVersion = getProtocol()

    val expireFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA)

    init { init() }


    override fun onInit() {
        subscribeEvent<PlayerJoinEvent> {
            priority(EventPriority.MONITOR)
            filter { !it.player.hasPermission("guardian.bypass") }
            handler { event ->
                if (!playerCheck) return@handler
                if (serverId <= 0) return@handler
                runAsync {
                    val resp = plugin.nnmClient.players.validate(ValidateRequest(
                        players = listOf(
                            PlayerValidateInfo(
                                playerName = event.player.name,
                                ip = event.player.address?.address?.hostAddress ?: "127.0.0.1",
                                clientVersion = serverVersion,
                                protocolVersion = "$serverProtocolVersion"
                            )
                        ),
                        serverId = serverId,
                        login = true
                    ))
                    val result = resp.results.first()
                    if (result.playerId != null) {
                    }

                    // 允许的情况
                    if (result.allowed) {
                        if (result.newbie) {
                            // 需要判断的特殊情况：老玩家在系统中没有记录，需要绑定才能允许上线
                            if (inPlayerGroup(event.player)) {
                                runSync {
                                    event.player.kick("请联系群管理进行账号绑定".toComponent(ComponentParseMode.Plain), PlayerKickEvent.Cause.ILLEGAL_ACTION)
                                }
                            }
                            // 如果是新人，允许上线，没有其他操作
                        } else {
                            // 如果是玩家，看看是不是第一次上线，第一次需要欢迎横幅和权限变更
                            if (inNewbieGroup(event.player)) {
                                toPlayerGroup(event.player)
                                runSync {
                                    // 迎新横幅
                                    event.player.playSound(event.player, org.bukkit.Sound.MUSIC_GAME, 1.0f, 1.0f)
                                    event.player.showTitle(
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
                            }
                            // 更新称号系统
                            val townName = run {
                                if (result.playerId == null) return@run null
                                val playerInfo = plugin.nnmClient.players.getPlayer(GetPlayerRequest(id=result.playerId))
                                if (playerInfo.townId == null) return@run null
                                val town = plugin.nnmClient.towns.getTown(GetTownRequest(id=playerInfo.townId))
                                town.town.name
                            }
                            runSync {
                                if (townName != null) {
                                    plugin.server.dispatchCommand(plugin.server.consoleSender,
                                        "newnan prefix player set ${event.player.uniqueId} Town $townName")
                                } else {
                                    plugin.server.dispatchCommand(plugin.server.consoleSender,
                                        "newnan prefix player remove ${event.player.uniqueId} Town")
                                }
                            }
                        }
                        return@runAsync
                    }

                    // 不允许的情况
                    val banList = Bukkit.getBanList<BanList<PlayerProfile>>(BanList.Type.PROFILE)
                    runSync {
                        handleBanMode(banList, result.banMode, result.banExpire, result.banReason, event.player)
                    }
                    return@runAsync
                }
            }
        }
        subscribeEvent<PlayerQuitEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                if (!playerCheck) return@handler
                if (serverId <= 0) return@handler
                runAsync {
                    val playerId = tryGetPlayerIdByName(event.player.name)
                    if (playerId != null) {
                        plugin.nnmClient.playerServers.setPlayersOffline(SetPlayersOfflineRequest(
                            serverId = serverId,
                            playerIds = listOf(playerId)
                        ))
                    }
                }
            }
        }
        // 定期验证
        runAsyncRepeating(20L * 30, 20L * 30) {
            validatePlayers()
        }
    }

    override fun onReload() {
        val config = plugin.getPluginConfig()
        checkGroup = config.checkGroup
        groupWorld = config.groupWorld
        newbieGroup = config.newbieGroup
        playersGroup = config.playersGroup
        serverId = config.serverId
        playerCheck = config.playerCheck

        // 立即验证所有在线玩家
        validatePlayers()
    }

    fun tryGetPlayerIdByName(name: String): Int? {
        // A long-lived name cache survives renames and can retain another
        // player's old name. Resolve identity at the backend for every use.
        val result = plugin.nnmClient.players.listPlayers(ListPlayersRequest(
            name=name,
            page=1,
            pageSize=2,
        ))
        return result.players.singleOrNull()?.id
    }

    private fun inPlayerGroup(player: OfflinePlayer) =
        plugin.permission.playerInGroup(groupWorld, player, playersGroup)

    private fun inNewbieGroup(player: OfflinePlayer) =
        plugin.permission.playerInGroup(groupWorld, player, newbieGroup) && !plugin.permission.playerInGroup(groupWorld, player, playersGroup)

    private fun toPlayerGroup(player: OfflinePlayer) {
        plugin.permission.playerRemoveGroup(groupWorld, player, newbieGroup)
        plugin.permission.playerAddGroup(groupWorld, player, playersGroup)
    }

    private fun handleBanMode(banList: BanList<PlayerProfile>, banMode: BanMode?, banExpire: String?, banReason: String?, player: org.bukkit.entity.Player) {
        when (banMode) {
            TEMPORARY -> {
                // 临时封禁
                val expire = LocalDateTime.parse(banExpire ?: "9999-12-31T23:59:59Z")
                val msg ="你已被临时封禁, 解禁时间: ${expire.format(expireFormatter)}, 原因: ${banReason ?: "未知"}"
                banList.addBan(player.playerProfile, banReason ?: "未知原因",
                    Date.from(expire.atZone(ZoneId.systemDefault()).toInstant()),
                    "南宫寻雪")
                player.kick(msg.toComponent(ComponentParseMode.Plain), PlayerKickEvent.Cause.BANNED)
            }
            PERMANENT -> {
                // 永久封禁
                val msg = "很遗憾，你已被永久封禁, 原因: ${banReason ?: "未知"}"
                banList.addBan(player.playerProfile, banReason ?: "未知原因", null as Date?, "南宫寻雪")
                player.kick(msg.toComponent(ComponentParseMode.Plain), PlayerKickEvent.Cause.BANNED)
            }
            NORMAL, null -> {
                // 未被封禁
                banList.pardon(player.playerProfile)
                // 如果没封禁，但是还是不允许，说明有别的原因
                player.kick((banReason ?: "很遗憾，你已被禁止登录").toComponent(ComponentParseMode.Plain), PlayerKickEvent.Cause.ILLEGAL_ACTION)
            }
        }
    }

    fun getPlayerInfo(player: OfflinePlayer): PlayerInfo {
        val playerRecord = run {
            val result = plugin.nnmClient.players.listPlayers(ListPlayersRequest(
                name=player.name,
                page=1,
                pageSize=1,
            ))
            if (result.players.isNotEmpty()) {
                result.players.first()
            } else {
                null
            }
        }
        val town = playerRecord?.townId?.let { plugin.nnmClient.towns.getTown(GetTownRequest(it)) }
        return PlayerInfo(
            player.name ?: "Unknown",
            player.uniqueId,
            plugin.economy?.getBalance(player) ?: 0.0,
            player.bedSpawnLocation,
            player.firstPlayed,
            player.lastLogin,
            player.getStatistic(Statistic.PLAY_ONE_MINUTE) * 50L,
            playerRecord,
            town?.town
        )
    }

    fun validatePlayers() {
        if (!playerCheck) return
        if (serverId <= 0) return
        val players = plugin.server.onlinePlayers
        val playerNameMap = players.associateBy { it.name }
        val banList = Bukkit.getBanList<BanList<PlayerProfile>>(BanList.Type.PROFILE)
        runAsync {
            // 分批100个
            for (chunk in players.chunked(100)) {
                val resp = plugin.nnmClient.players.validate(ValidateRequest(
                    players = chunk.map { player ->
                        PlayerValidateInfo(
                            playerName = player.name,
                            ip = player.address?.address?.hostAddress ?: "127.0.0.1"
                        )
                    },
                    serverId = serverId,
                    login = false
                ))
                runSync {
                    for (result in resp.results) {
                        val player = playerNameMap[result.playerName]!!
                        // 更新映射缓存
                        if (result.playerId != null) {
                        }
                        if (result.allowed) {
                            if (result.newbie) {
                                // 需要判断的特殊情况：老玩家在系统中没有记录，需要绑定才能允许上线
                                if (inPlayerGroup(player)) {
                                    player.kick("请联系群管理进行账号绑定".toComponent(ComponentParseMode.Plain), PlayerKickEvent.Cause.ILLEGAL_ACTION)
                                }
                                // 如果是新人，允许上线，没有其他操作
                            } else {
                                // 如果是玩家，看看是不是第一次上线，第一次需要欢迎横幅和权限变更
                                if (inNewbieGroup(player)) {
                                    toPlayerGroup(player)
                                    // 迎新横幅
                                    player.playSound(player, org.bukkit.Sound.MUSIC_GAME, 1.0f, 1.0f)
                                    player.showTitle(
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
                            }
                            continue
                        }
                        handleBanMode(banList, result.banMode, result.banExpire, result.banReason, player)
                    }
                }
            }
        }
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
        val town: Town?
    )
}
