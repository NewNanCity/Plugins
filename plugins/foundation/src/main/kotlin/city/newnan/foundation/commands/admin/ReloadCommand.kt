package city.newnan.foundation.commands.admin

import city.newnan.foundation.FoundationPlugin
import city.newnan.foundation.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 重载命令
 *
 * 重载Foundation插件的配置和数据：
 * - 重载配置文件
 * - 重新初始化组件
 * - 错误处理和回滚
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class ReloadCommand(private val plugin: FoundationPlugin) {

    @Command("foundation|fund reload")
    @CommandDescription(LanguageKeys.Commands.Reload.DESCRIPTION)
    @Permission("foundation.reload")
    fun reloadCommand(sender: CommandSender) {
        try {
            plugin.reloadPlugin()
            plugin.messager.printf(sender, LanguageKeys.Commands.Reload.SUCCESS)
        } catch (e: Exception) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.FAILED, e.message ?: "Unknown error")
        }
    }
}
