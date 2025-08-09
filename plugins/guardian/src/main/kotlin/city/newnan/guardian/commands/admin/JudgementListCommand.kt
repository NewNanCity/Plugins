package city.newnan.guardian.commands.admin

import city.newnan.guardian.GuardianPlugin
import city.newnan.guardian.i18n.LanguageKeys
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 风纪委员列表命令
 * 
 * 显示所有授权的风纪委员列表
 * 
 * @author Guardian Team
 * @since 2.0.0
 */
class JudgementListCommand(private val plugin: GuardianPlugin) {

    @Command("guardian|gd judgement list|ls")
    @CommandDescription(LanguageKeys.Commands.JudgementList.DESCRIPTION)
    @Permission("guardian.judgemental.edit")
    fun judgementListCommand(sender: CommandSender) {
        try {
            // 获取所有风纪委员玩家
            val players = plugin.judgementalManager.judgementalPlayers
                .map { Bukkit.getOfflinePlayer(it) }
                .filter { it.hasPlayedBefore() }

            // 显示列表
            plugin.messager.printf(sender, LanguageKeys.Commands.JudgementList.HEADER)
            
            if (players.isEmpty()) {
                plugin.messager.printf(sender, LanguageKeys.Commands.JudgementList.EMPTY)
            } else {
                val playerNames = players.joinToString("§7, §r") { p -> p.name ?: "§c未知" }
                plugin.messager.printf(sender, "§f$playerNames§r")
            }

            // 记录日志
            plugin.logger.info(LanguageKeys.Commands.JudgementList.LOG_SUCCESS, sender.name, players.size)

        } catch (e: Exception) {
            plugin.logger.error("Failed to list judgemental players", e)
            plugin.messager.printf(sender, LanguageKeys.Core.Error.OPERATION_FAILED, e.message ?: "Unknown error")
        }
    }
}
