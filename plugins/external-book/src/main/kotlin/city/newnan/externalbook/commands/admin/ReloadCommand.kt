package city.newnan.externalbook.commands.admin

import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 重载命令
 *
 * 重载插件配置。
 * 完整保留原有ReloadCommand的逻辑。
 *
 * @author NewNanCity
 * @since 1.0.0
 */
class ReloadCommand(private val plugin: ExternalBookPlugin) {

    /**
     * 重载命令处理方法
     */
    @Command("externalbook|book reload")
    @CommandDescription(LanguageKeys.Commands.Reload.DESCRIPTION)
    @Permission("externalbook.reload")
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
