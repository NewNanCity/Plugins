package city.newnan.foundation.commands.user

import city.newnan.core.scheduler.runSync
import city.newnan.foundation.FoundationPlugin
import city.newnan.foundation.i18n.LanguageKeys
import org.bukkit.block.CommandBlock
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.bukkit.data.SinglePlayerSelector
import org.incendo.cloud.type.Either
import java.math.BigDecimal

/**
 * 捐赠命令
 *
 * 处理玩家向基金会捐赠金钱的功能：
 * - 参数验证和余额检查
 * - 安全的经济系统交互
 * - 转账记录和日志
 * - 成功通知和广播
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class DonateCommand(private val plugin: FoundationPlugin) {

    @Command("foundation|fund donate <amount> [player]")
    @CommandDescription(LanguageKeys.Commands.Donate.DESCRIPTION)
    @Permission(value = ["foundation.donate", "foundation.donate.other"], mode = Permission.Mode.ANY_OF)
    fun donateCommand(
        sender: CommandSender,
        @Argument(value = "amount", description = LanguageKeys.Commands.Donate.AMOUNT) amount: Double,
        @Argument(value = "player", description = LanguageKeys.Commands.Donate.PLAYER_OR_SELECTOR) player: Either<Player, SinglePlayerSelector>?
    ) {
        // 获取捐赠者
        val targetPlayer = player?.mapEither(
                { it },
                { it.single() }
        ) ?: run {
            if (sender !is Player) {
                plugin.messager.printf(sender, LanguageKeys.Commands.Donate.PLAYER_ONLY)
                return@donateCommand
            }
            sender
        }

        // 参数验证
        if (!amount.isFinite() || amount <= 0) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Donate.INVALID_AMOUNT)
            return
        }

        if (sender != targetPlayer) {
            if (sender !is ConsoleCommandSender && sender !is CommandBlock && !sender.hasPermission("foundation.donate.other")) {
                plugin.messager.printf(sender, LanguageKeys.Core.Error.NO_PERMISSION)
                return
            }
        } else {
            if (!sender.hasPermission("foundation.donate")) {
                plugin.messager.printf(sender, LanguageKeys.Core.Error.NO_PERMISSION)
                return
            }
        }

        val result = plugin.transferManager.activeTransfer(targetPlayer, amount)
        plugin.runSync {
            plugin.messager.printf(sender, result.messageKey, *result.messageArgs)
        }
    }
}
