package city.newnan.feefly.commands.admin

import city.newnan.feefly.FeeFlyPlugin
import city.newnan.feefly.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

class ReloadCommand(private val plugin: FeeFlyPlugin) {

    @Command("feefly reload")
    @CommandDescription(LanguageKeys.Commands.Reload.DESCRIPTION)
    @Permission("feefly.reload")
    fun reloadCommand(sender: CommandSender) {
        try {
            plugin.reloadPlugin()
            plugin.messager.printf(sender, LanguageKeys.Commands.Reload.SUCCESS)
            plugin.logger.info(LanguageKeys.Commands.Reload.LOG_SUCCESS, sender.name)
        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Reload.FAILED, e.message ?: "Unknown error")
            plugin.logger.error(LanguageKeys.Commands.Reload.LOG_FAILED, e, sender.name)
        }
    }
}