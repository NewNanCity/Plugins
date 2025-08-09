package city.newnan.guardian.modules

import city.newnan.config.extensions.configManager
import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import city.newnan.guardian.GuardianPlugin
import city.newnan.guardian.config.JudgementalPlayers
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

class JudgementalManager(val plugin: GuardianPlugin) : BaseModule("JudgementalManager", plugin) {
    var judgementalGroup: String = ""
        private set

    val judgementalPlayers: MutableSet<UUID> = mutableSetOf()

    init { init() }

    fun dump() {
        plugin.configManager.save(JudgementalPlayers(judgementalPlayers), "judgemental-players.yml")
    }

    override fun onReload() {
        judgementalGroup = plugin.getPluginConfig().judgementalGroup
        plugin.configManager.touchWithMerge("judgemental-players.yml", createBackup = true)
        plugin.configManager.parse<JudgementalPlayers>("judgemental-players.yml").players.also {
            judgementalPlayers.clear()
            judgementalPlayers.addAll(plugin.configManager.parse<JudgementalPlayers>("judgemental-players.yml").players)
        }
    }

    override fun onInit() {
        subscribeEvent<PlayerChangedWorldEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                val player = event.player
                val from = plugin.permission.playerInGroup(event.from.name, player, judgementalGroup)
                val to = plugin.permission.playerInGroup(player.world.name, player, judgementalGroup)
                if (from && !to) {
                    plugin.permission.playerAddGroup(player.world.name, player, judgementalGroup)
                    player.gameMode = GameMode.SPECTATOR
                    plugin.server.dispatchCommand(Bukkit.getServer().consoleSender, "vanish ${player.name} enable")
                } else if (!from && to) {
                    plugin.permission.playerRemoveGroup(event.from.name, player, judgementalGroup)
                    player.gameMode = GameMode.SURVIVAL
                    player.allowFlight = false
                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().consoleSender, "vanish ${player.name} disable")
                }
            }
        }

        subscribeEvent<PlayerQuitEvent> {
            priority(EventPriority.MONITOR)
            handler { event ->
                val player = event.player
                if (plugin.permission.playerInGroup(player.world.name, player, judgementalGroup)) {
                    plugin.permission.playerRemoveGroup(player.world.name, player, judgementalGroup)
                    player.gameMode = GameMode.SURVIVAL
                    player.allowFlight = false
                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().consoleSender, "vanish ${player.name} disable")
                }
            }
        }
    }

    fun togglePlayer(player: Player) {
        if (!isJudgemental(player)) return
        if (plugin.permission.playerInGroup(player.world.name, player, judgementalGroup)) {
            // 如果玩家当前是风纪委员组，就切换回玩家组
            plugin.permission.playerRemoveGroup(player.world.name, player, judgementalGroup)
            player.gameMode = GameMode.SURVIVAL
            player.allowFlight = false
            plugin.server.dispatchCommand(plugin.server.consoleSender, "vanish ${player.name} disable")
        } else {
            // 否则就进入风纪委员组
            plugin.permission.playerAddGroup(player.world.name, player, judgementalGroup)
            player.gameMode = GameMode.SPECTATOR
            plugin.server.dispatchCommand(plugin.server.consoleSender, "vanish ${player.name} enable")
        }
    }

    fun isJudgemental(player: OfflinePlayer) =
        judgementalPlayers.contains(player.uniqueId)

    fun isInJudgementalMode(player: Player) =
        plugin.permission.playerInGroup(player.world.name, player, judgementalGroup)

    fun addJudgemental(player: OfflinePlayer) {
        judgementalPlayers.add(player.uniqueId)
        dump()
    }

    fun removeJudgemental(player: OfflinePlayer) {
        judgementalPlayers.remove(player.uniqueId)
        dump()
    }

    fun listJudgemental() = judgementalPlayers.map { Bukkit.getOfflinePlayer(it) }
}