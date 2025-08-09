package city.newnan.guardian.commands.admin

import city.newnan.guardian.GuardianPlugin
import city.newnan.guardian.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription

/**
 * 风纪委员切换命令
 *
 * 允许授权玩家在普通模式和风纪委员模式之间切换
 *
 * @author Guardian Team
 * @since 2.0.0
 */
class JudgementalCommand(private val plugin: GuardianPlugin) {

    @Command("guardian judgemental")
    @Command("judgemental")
    @CommandDescription(LanguageKeys.Commands.Judgemental.DESCRIPTION)
    fun judgementalCommand(sender: CommandSender) {
        // 1. 玩家检查
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        try {
            // 2. 检查是否为授权的风纪委员
            if (!plugin.judgementalManager.isJudgemental(sender)) {
                plugin.messager.printf(sender, LanguageKeys.Commands.Judgemental.NOT_AUTHORIZED)
                return
            }

            // 3. 执行切换操作
            val wasJudgemental = plugin.judgementalManager.isInJudgementalMode(sender)
            plugin.judgementalManager.togglePlayer(sender)
            val isNowJudgemental = plugin.judgementalManager.isInJudgementalMode(sender)

            // 4. 发送反馈消息
            if (isNowJudgemental && !wasJudgemental) {
                // 切换到风纪委员模式
                plugin.messager.printf(sender, LanguageKeys.Commands.Judgemental.ENABLED)
                plugin.logger.info(LanguageKeys.Commands.Judgemental.LOG_ENABLED, sender.name)
                // 广播伪装退出消息，让风纪委员看起来像退出了游戏
                plugin.server.broadcast(plugin.messager.sprintf(LanguageKeys.Player.Judgemental.FAKE_LOGOUT_BROADCAST_MESSAGE, sender.name))
            } else if (!isNowJudgemental && wasJudgemental) {
                // 切换到普通模式
                plugin.messager.printf(sender, LanguageKeys.Commands.Judgemental.DISABLED)
                plugin.logger.info(LanguageKeys.Commands.Judgemental.LOG_DISABLED, sender.name)
                // 广播伪装登录消息，让风纪委员看起来像重新加入了游戏
                plugin.server.broadcast(plugin.messager.sprintf(LanguageKeys.Player.Judgemental.FAKE_LOGIN_BROADCAST_MESSAGE, sender.name))
            }

        } catch (e: Exception) {
            plugin.logger.error(LanguageKeys.Commands.Judgemental.LOG_FAILED, e, sender.name)
            plugin.messager.printf(sender, LanguageKeys.Core.Error.OPERATION_FAILED, e.message ?: "Unknown error")
        }
    }
}
