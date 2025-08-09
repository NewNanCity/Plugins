package city.newnan.deathcost.commands.admin

import city.newnan.deathcost.DeathCostPlugin
import city.newnan.deathcost.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 重载命令
 *
 * 用于重新加载插件配置和语言文件
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class ReloadCommand(private val plugin: DeathCostPlugin) {

    @Command("deathcost reload")
    @CommandDescription(LanguageKeys.Commands.Reload.DESCRIPTION)
    @Permission("deathcost.reload")
    fun reloadCommand(sender: CommandSender) {
        try {
            plugin.reloadPlugin()
            plugin.messager.printf(sender, LanguageKeys.Commands.Reload.SUCCESS)
        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Core.Config.RELOAD_FAILED, e.message ?: "Unknown error")
            plugin.logger.error("Config reload failed", e)
        }
    }
}