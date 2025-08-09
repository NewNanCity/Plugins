package city.newnan.foundation.commands.admin

import city.newnan.foundation.FoundationPlugin
import city.newnan.foundation.i18n.LanguageKeys
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 拨款命令
 *
 * 从基金会向玩家拨款：
 * - 参数验证和余额检查
 * - 安全的经济系统交互
 * - 拨款记录和日志
 * - 通知相关玩家
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class AllocateCommand(private val plugin: FoundationPlugin) {

    @Command("foundation|fund allocate <player> <amount> <reason>")
    @CommandDescription(LanguageKeys.Commands.Allocate.DESCRIPTION)
    @Permission("foundation.allocate")
    fun allocateCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.Allocate.PLAYER) player: OfflinePlayer,
        @Argument(value = "amount", description = LanguageKeys.Commands.Allocate.AMOUNT) amount: Double,
        @Argument(value = "reason", description = LanguageKeys.Commands.Allocate.REASON) reason: String
    ) {
        // 参数验证
        if (!amount.isFinite() || amount <= 0) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Allocate.INVALID_AMOUNT)
            return
        }

        if (reason.isBlank()) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Allocate.INVALID_REASON)
            return
        }

        plugin.transferManager.allocation(sender, player, amount, reason)
    }
}
