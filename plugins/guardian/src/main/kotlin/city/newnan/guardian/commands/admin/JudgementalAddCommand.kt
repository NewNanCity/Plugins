package city.newnan.guardian.commands.admin

import city.newnan.guardian.GuardianPlugin
import city.newnan.guardian.i18n.LanguageKeys
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 添加风纪委员命令
 * 
 * 将指定玩家添加到风纪委员列表
 * 
 * @author Guardian Team
 * @since 2.0.0
 */
class JudgementalAddCommand(private val plugin: GuardianPlugin) {
    @Command("guardian judgement add <player>")
    @CommandDescription(LanguageKeys.Commands.JudgementAdd.DESCRIPTION)
    @Permission("guardian.judgemental.edit")
    fun judgementAddCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.JudgementAdd.PLAYER) target: OfflinePlayer
    ) {
        try {
            val playerName = target.name ?: "Unknown"
            
            // 检查玩家是否存在
            if (!target.hasPlayedBefore()) {
                plugin.messager.printf(sender, LanguageKeys.Commands.JudgementAdd.PLAYER_NOT_FOUND, playerName)
                return
            }

            // 检查是否已经是风纪委员
            if (plugin.judgementalManager.isJudgemental(target)) {
                plugin.messager.printf(sender, LanguageKeys.Commands.JudgementAdd.ALREADY_EXISTS, playerName)
                return
            }

            // 添加风纪委员
            plugin.judgementalManager.addJudgemental(target)
            
            // 发送成功消息
            plugin.messager.printf(sender, LanguageKeys.Commands.JudgementAdd.SUCCESS, playerName)
            
            // 记录日志
            plugin.logger.info(LanguageKeys.Commands.JudgementAdd.LOG_SUCCESS, sender.name, playerName)

        } catch (e: Exception) {
            plugin.logger.error(LanguageKeys.Commands.JudgementAdd.LOG_FAILED, e, sender.name, target.name ?: "Unknown")
            plugin.messager.printf(sender, LanguageKeys.Core.Error.OPERATION_FAILED, e.message ?: "Unknown error")
        }
    }
}
