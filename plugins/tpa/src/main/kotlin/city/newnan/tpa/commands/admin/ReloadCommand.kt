package city.newnan.tpa.commands.admin

import city.newnan.tpa.TPAPlugin
import city.newnan.tpa.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * TPA重载命令
 *
 * 提供插件配置重载功能，仅限管理员使用
 *
 * @author AI Assistant
 * @since 2.0.0
 */
class ReloadCommand(private val plugin: TPAPlugin) {

    @Command("tpa reload")
    @CommandDescription(LanguageKeys.Commands.Reload.DESCRIPTION)
    @Permission("tpa.reload")
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