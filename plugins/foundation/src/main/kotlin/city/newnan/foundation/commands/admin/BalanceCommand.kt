package city.newnan.foundation.commands.admin

import city.newnan.foundation.FoundationPlugin
import city.newnan.foundation.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 余额命令
 *
 * 查询基金会账户余额：
 * - 显示当前余额
 * - 显示转账摘要
 * - 账户状态信息
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class BalanceCommand(private val plugin: FoundationPlugin) {

    @Command("foundation|fund balance")
    @CommandDescription(LanguageKeys.Commands.Balance.DESCRIPTION)
    @Permission("foundation.balance")
    fun balanceCommand(sender: CommandSender) {
        try {
            val targetAccount = plugin.targetAccount ?: run {
                plugin.messager.printf(sender, LanguageKeys.Commands.Balance.FOUNDATION_ACCOUNT_NOT_SET)
                return
            }

            val economy = plugin.economy
            val balance = economy.getBalance(targetAccount)

            plugin.messager.printf(sender, LanguageKeys.Commands.Balance.HEADER)
            plugin.messager.printf(sender, LanguageKeys.Commands.Balance.ACCOUNT, targetAccount.name ?: "Unknown")
            plugin.messager.printf(sender, LanguageKeys.Commands.Balance.AMOUNT, String.format("%.2f", balance))

            // 显示捐款总数和拨款总数
            val (activateDonations, passiveDonations) = plugin.transferManager.getTotalDonations()
            val totalAllocations = plugin.transferManager.getTotalAllocation()

            plugin.messager.printf(sender, LanguageKeys.Commands.Balance.ACTIVE_DONATIONS, String.format("%.2f", activateDonations + passiveDonations))
            plugin.messager.printf(sender, LanguageKeys.Commands.Balance.PASSIVE_DONATIONS, String.format("%.2f", totalAllocations))

        } catch (e: Exception) {
            plugin.logger.error(LanguageKeys.Commands.Balance.LOG_FAILED, e)
            plugin.messager.printf(sender, LanguageKeys.Core.Error.FAILED, e.message ?: "Unknown error")
        }
    }
}
