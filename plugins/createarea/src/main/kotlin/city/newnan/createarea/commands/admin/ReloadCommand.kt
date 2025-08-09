package city.newnan.createarea.commands.admin

import city.newnan.createarea.CreateAreaPlugin
import city.newnan.createarea.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 重载命令
 *
 * 重载CreateArea插件配置。
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class ReloadCommand(private val plugin: CreateAreaPlugin) {

    /**
     * 重载命令处理方法
     */
    @Command("createarea|carea reload")
    @CommandDescription(LanguageKeys.Commands.Reload.DESCRIPTION)
    @Permission("createarea.reload")
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
