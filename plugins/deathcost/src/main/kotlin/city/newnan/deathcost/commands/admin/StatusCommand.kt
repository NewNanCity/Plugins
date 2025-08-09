package city.newnan.deathcost.commands.admin

import city.newnan.deathcost.DeathCostPlugin
import city.newnan.deathcost.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 状态命令
 *
 * 显示插件的当前配置和运行状态
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class StatusCommand(private val plugin: DeathCostPlugin) {

    @Command("deathcost status")
    @CommandDescription(LanguageKeys.Commands.Status.DESCRIPTION)
    @Permission("deathcost.status")
    fun statusCommand(sender: CommandSender) {
        try {
            val config = plugin.getPluginConfig()

            // 显示基本信息
            plugin.messager.printf(sender, LanguageKeys.Commands.Status.HEADER)
            plugin.messager.printf(sender, LanguageKeys.Commands.Status.VERSION, plugin.pluginMeta.version)
            plugin.messager.printf(sender, LanguageKeys.Commands.Status.ENABLED,
                if (plugin.isEnabled) LanguageKeys.Business.Common.ENABLED else LanguageKeys.Business.Common.DISABLED)

            // 显示扣费模式
            if (config.deathCost.useSimpleMode) {
                plugin.messager.printf(sender, LanguageKeys.Commands.Status.COST_MODE, LanguageKeys.Commands.Status.MODE_SIMPLE)
                val simpleMode = config.deathCost.simpleMode
                if (simpleMode != null) {
                    plugin.messager.printf(sender, LanguageKeys.Commands.Status.SIMPLE_COST, simpleMode.cost)

                    if (simpleMode.ifPercent) {
                        plugin.messager.printf(sender, LanguageKeys.Commands.Status.COST_PERCENT)
                    } else {
                        plugin.messager.printf(sender, LanguageKeys.Commands.Status.COST_FIXED)
                    }
                }
            } else {
                plugin.messager.printf(sender, LanguageKeys.Commands.Status.COST_MODE, LanguageKeys.Commands.Status.MODE_COMPLEX)
                val complexMode = config.deathCost.complexMode
                if (complexMode != null) {
                    plugin.messager.printf(sender, LanguageKeys.Commands.Status.COMPLEX_TIERS, complexMode.size)
                }
            }

            // 显示目标账户
            val targetAccount = config.deathCost.targetAccount
            if (!targetAccount.isNullOrEmpty()) {
                plugin.messager.printf(sender, LanguageKeys.Commands.Status.TARGET_ACCOUNT, targetAccount)
            } else {
                plugin.messager.printf(sender, LanguageKeys.Commands.Status.NO_TARGET)
            }

            // 显示消息设置
            plugin.messager.printf(sender, LanguageKeys.Commands.Status.PLAYER_MESSAGE,
                if (config.deathMessage.playerEnable) LanguageKeys.Business.Common.ENABLED else LanguageKeys.Business.Common.DISABLED)
            plugin.messager.printf(sender, LanguageKeys.Commands.Status.BROADCAST_MESSAGE,
                if (config.deathMessage.broadcastEnable) LanguageKeys.Business.Common.ENABLED else LanguageKeys.Business.Common.DISABLED)
            plugin.messager.printf(sender, LanguageKeys.Commands.Status.CONSOLE_MESSAGE,
                if (config.deathMessage.consoleEnable) LanguageKeys.Business.Common.ENABLED else LanguageKeys.Business.Common.DISABLED)

        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Status.ERROR, e.message ?: "Unknown error")
            plugin.logger.error(LanguageKeys.Commands.Status.LOG_ERROR, e)
        }
    }
}