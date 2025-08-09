package city.newnan.feefly.commands.admin

import city.newnan.feefly.FeeFlyPlugin
import city.newnan.feefly.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 飞行列表查看命令
 *
 * 显示当前所有正在进行付费飞行的玩家列表，包括：
 * - 玩家名称
 * - 飞行开始时间
 * - 已消耗金额
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class ListCommand(private val plugin: FeeFlyPlugin) {

    @Command("feefly list|ls")
    @CommandDescription(LanguageKeys.Commands.List.DESCRIPTION)
    @Permission("feefly.list")
    fun listCommand(sender: CommandSender) {
        try {
            val flyingPlayers = plugin.flyManager.getFlyingPlayers()

            if (flyingPlayers.isEmpty()) {
                plugin.messager.printf(sender, LanguageKeys.Commands.List.NO_FLYING_PLAYERS)
                return
            }

            plugin.messager.printf(sender, LanguageKeys.Commands.List.FLYING_COUNT, flyingPlayers.size)
            plugin.messager.printf(sender, LanguageKeys.Commands.List.DETAILED_INFO_HEADER)

            flyingPlayers.forEach { (player, flyData) ->
                val startTime = java.time.Instant.ofEpochMilli(flyData.flyStartTimestamp)
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                val duration = flyData.getFlyDurationSeconds()
                plugin.messager.printf(sender, LanguageKeys.Commands.List.PLAYER_DETAIL,
                    player.name, startTime, duration)
            }
        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.OPERATION_FAILED, e.message ?: "Unknown error")
            plugin.logger.error(LanguageKeys.Log.Error.SERVICE_ERROR, e)
        }
    }
}