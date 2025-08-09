package city.newnan.guardian.commands.admin

import city.newnan.guardian.GuardianPlugin
import city.newnan.guardian.i18n.LanguageKeys
import org.bukkit.GameMode
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 移除风纪委员命令
 *
 * 将指定玩家从风纪委员列表中移除
 *
 * @author Guardian Team
 * @since 2.0.0
 */
class JudgementalRemoveCommand(private val plugin: GuardianPlugin) {

    @Command("guardian judgemental remove <player>")
    @CommandDescription(LanguageKeys.Commands.JudgementRemove.DESCRIPTION)
    @Permission("guardian.judgemental.edit")
    fun judgementRemoveCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.JudgementRemove.PLAYER) target: OfflinePlayer
    ) {
        try {
            val playerName = target.name ?: "Unknown"

            // 检查玩家是否存在
            if (!target.hasPlayedBefore()) {
                plugin.messager.printf(sender, LanguageKeys.Commands.JudgementRemove.PLAYER_NOT_FOUND, playerName)
                return
            }

            // 检查是否是风纪委员
            if (!plugin.judgementalManager.isJudgemental(target)) {
                plugin.messager.printf(sender, LanguageKeys.Commands.JudgementRemove.NOT_EXISTS, playerName)
                return
            }

            // 移除风纪委员
            plugin.judgementalManager.removeJudgemental(target)

            // 如果玩家在线且处于风纪委员模式，切换回普通模式
            val player = target.player
            if (player != null && plugin.judgementalManager.isInJudgementalMode(player)) {
                plugin.permission.playerRemoveGroup(player.world.name, player, plugin.judgementalManager.judgementalGroup)
                player.gameMode = GameMode.SURVIVAL
                player.allowFlight = false
                // 广播伪装登录消息，让被移除的风纪委员看起来像重新加入了游戏
                plugin.server.broadcast(plugin.messager.sprintf(LanguageKeys.Player.Judgemental.FAKE_LOGIN_BROADCAST_MESSAGE, playerName))
                plugin.server.dispatchCommand(plugin.server.consoleSender, "vanish $playerName disable")
            }

            // 发送成功消息
            plugin.messager.printf(sender, LanguageKeys.Commands.JudgementRemove.SUCCESS, playerName)

            // 记录日志
            plugin.logger.info(LanguageKeys.Commands.JudgementRemove.LOG_SUCCESS, sender.name, playerName)

        } catch (e: Exception) {
            plugin.logger.error(LanguageKeys.Commands.JudgementRemove.LOG_FAILED, e, sender.name, target.name ?: "Unknown")
            plugin.messager.printf(sender, LanguageKeys.Core.Error.OPERATION_FAILED, e.message ?: "Unknown error")
        }
    }
}
