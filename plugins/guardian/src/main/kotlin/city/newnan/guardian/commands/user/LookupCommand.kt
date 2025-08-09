package city.newnan.guardian.commands.user

import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runSync
import city.newnan.guardian.GuardianPlugin
import city.newnan.guardian.i18n.LanguageKeys
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 玩家详细信息查询命令（lookup/playerinfo别名）
 *
 * 使用PlayerManager获取完整的玩家信息，包括经济、重生点、统计数据等
 *
 * @author Guardian Team
 * @since 2.0.0
 */
class LookupCommand(private val plugin: GuardianPlugin) {

    private val dateFormatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss").also {
        it.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }

    @Command("lookup|playerinfo <player>")
    @Command("guardian lookup|playerinfo <player>")
    @CommandDescription(LanguageKeys.Commands.Lookup.DESCRIPTION)
    @Permission("guardian.lookup")
    fun lookupCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.Lookup.PLAYER) target: OfflinePlayer
    ) {
        val playerName = target.name ?: "Unknown"

        // 检查玩家是否存在
        if (!target.hasPlayedBefore()) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.PLAYER_NOT_FOUND, playerName)
            return
        }

        // 异步处理数据库查询
        plugin.runAsync { _ ->
            try {
                // 使用PlayerManager获取完整的玩家信息
                val playerInfo = plugin.playerManager.getPlayerInfo(target)

                // 回到主线程显示信息
                plugin.runSync { _ ->
                    // 显示详细信息头部
                    plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.HEADER) // "§8==============================================="
                    plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.NAME_LABEL, playerInfo.name) // "§7昵称:   §f${playerInfo.name}"
                    plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.UUID_LABEL, playerInfo.uuid) // "§7UUID:    §f${playerInfo.uuid}"
                    plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.CASH_LABEL, playerInfo.cash) // "§7现金:   §f${playerInfo.cash} §7₦"

                    // 重生点信息
                    if (playerInfo.bedSpawnLocation == null) {
                        plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.BED_SPAWN_NONE) // "§7重生点: §f无"
                    } else {
                        val bed = playerInfo.bedSpawnLocation!!
                        plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.BED_SPAWN_LABEL, bed.world?.name, bed.blockX, bed.blockY, bed.blockZ) // "§7重生点: §f${bed.world?.name} ${bed.blockX} ${bed.blockY} ${bed.blockZ}"
                    }

                    // 登录时间信息
                    val firstPlayed = if (playerInfo.firstPlayed > 0) {
                        millisecondToTime(playerInfo.firstPlayed)
                    } else {
                        plugin.messager.sprintf(LanguageKeys.Commands.Lookup.NEVER_PLAYED) // "§7未登录过"
                    }

                    val lastPlayed = if (playerInfo.lastPlayed > 0) {
                        millisecondToTime(playerInfo.lastPlayed)
                    } else {
                        plugin.messager.sprintf(LanguageKeys.Commands.Lookup.NEVER_PLAYED) // "§7未登录过"
                    }

                    plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.FIRST_PLAYED_LABEL, firstPlayed) // "§7第一次登录时间:   §f$firstPlayed"
                    plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.LAST_PLAYED_LABEL, lastPlayed) // "§7最后一次登录时间: §f$lastPlayed"

                    // 在线时长
                    val playTimeDuration = millisecondToDuration(playerInfo.playTime)
                    plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.PLAY_TIME_LABEL, playTimeDuration) // "§7在线时长: §f$playTimeDuration"

                    // 数据库信息
                    if (playerInfo.record != null) {
                        val record = playerInfo.record!!

                        // QQ信息
                        val qqStatus = if (record.inqqgroup) {
                            plugin.messager.sprintf(LanguageKeys.Commands.Lookup.STATUS_JOINED) // "§a[已加入]"
                        } else {
                            plugin.messager.sprintf(LanguageKeys.Commands.Lookup.STATUS_NOT_JOINED) // "§c[未加入]"
                        }
                        val qqInfo = record.qq?.let { "$it $qqStatus" } ?: plugin.messager.sprintf(LanguageKeys.Commands.Lookup.INFO_NONE) // "无"
                        plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.QQ_LABEL, qqInfo) // "§7QQ:      §f$qqInfo"

                        // 频道信息
                        val guildStatus = if (record.inqqguild) {
                            plugin.messager.sprintf(LanguageKeys.Commands.Lookup.STATUS_JOINED) // "§a[已加入]"
                        } else {
                            plugin.messager.sprintf(LanguageKeys.Commands.Lookup.STATUS_NOT_JOINED) // "§c[未加入]"
                        }
                        val guildInfo = record.qqguild?.let { "$it $guildStatus" } ?: plugin.messager.sprintf(LanguageKeys.Commands.Lookup.INFO_NONE) // "无"
                        plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.GUILD_LABEL, guildInfo) // "§7频道:    §f$guildInfo"

                        // Discord信息
                        val discordStatus = if (record.indiscord) {
                            plugin.messager.sprintf(LanguageKeys.Commands.Lookup.STATUS_JOINED) // "§a[已加入]"
                        } else {
                            plugin.messager.sprintf(LanguageKeys.Commands.Lookup.STATUS_NOT_JOINED) // "§c[未加入]"
                        }
                        val discordInfo = record.discord?.let { "$it $discordStatus" } ?: plugin.messager.sprintf(LanguageKeys.Commands.Lookup.INFO_NONE) // "无"
                        plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.DISCORD_LABEL, discordInfo) // "§7Discord: §f$discordInfo"

                        // 封禁状态
                        when {
                            record.banMode == 0 -> plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.BAN_STATUS_NONE) // "§7封禁状态: §a未封禁"
                            record.banMode == 1 -> {
                                val expireTime = record.banExpire?.toString() ?: "?"
                                plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.BAN_STATUS_TEMP, expireTime) // "§7封禁状态: §e临时封禁 §7至 §f$expireTime"
                            }
                            record.banMode == 2 -> plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.BAN_STATUS_PERM) // "§7封禁状态: §c永久封禁"
                        }

                        // 小镇信息
                        val townName = playerInfo.town?.name ?: plugin.messager.sprintf(LanguageKeys.Commands.Lookup.TOWN_NONE) // "§7未加入小镇"
                        plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.TOWN_LABEL, townName) // "§7小镇: §f$townName"

                        // IP列表
                        plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.IP_LIST_LABEL, playerInfo.ips.joinToString(", ")) // "§7历史IP: §f${playerInfo.ips.joinToString(", ")}"
                    } else {
                        plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.NOT_BOUND) // "§c该玩家未绑定过!"
                    }

                    plugin.messager.printf(sender, LanguageKeys.Commands.Lookup.FOOTER) // "§8==============================================="

                    // 记录日志
                    plugin.logger.info(LanguageKeys.Commands.Lookup.LOG_SUCCESS, sender.name, playerName)
                }

            } catch (e: Exception) {
                plugin.logger.error(LanguageKeys.Commands.Lookup.LOG_FAILED, e, sender.name, playerName)
                plugin.runSync { _ ->
                    plugin.messager.printf(sender, LanguageKeys.Core.Error.OPERATION_FAILED, e.message ?: "Unknown error")
                }
            }
        }
    }

    private fun millisecondToTime(millisecond: Long): String =
        dateFormatter.format(Date(millisecond))

    private fun millisecondToDuration(millisecond: Long): String {
        val day = TimeUnit.MILLISECONDS.toDays(millisecond)
        val hour = TimeUnit.MILLISECONDS.toHours(millisecond) - day * 24
        val minute = TimeUnit.MILLISECONDS.toMinutes(millisecond) - day * 24 * 60 - hour * 60
        val second = TimeUnit.MILLISECONDS.toSeconds(millisecond) - day * 24 * 60 * 60 - hour * 60 * 60 - minute * 60
        return "${day}d ${hour}h${minute}m ${second}s"
    }
}
