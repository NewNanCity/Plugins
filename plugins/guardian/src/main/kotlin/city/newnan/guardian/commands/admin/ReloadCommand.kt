package city.newnan.guardian.commands.admin

import city.newnan.guardian.GuardianPlugin
import city.newnan.guardian.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 重载命令实现
 * 
 * 重新加载插件配置和语言文件
 * 
 * @author Guardian Team
 * @since 2.0.0
 */
class ReloadCommand(private val plugin: GuardianPlugin) {

    @Command("guardian reload")
    @CommandDescription(LanguageKeys.Commands.Reload.DESCRIPTION)
    @Permission("guardian.reload")
    fun reloadCommand(sender: CommandSender) {
        try {
            // 显示重载开始信息
            plugin.messager.printf(sender, LanguageKeys.Core.Plugin.RELOADING)
            
            // 执行重载操作
            plugin.reloadPlugin()
            
            // 显示重载成功信息
            plugin.messager.printf(sender, LanguageKeys.Commands.Reload.SUCCESS)
            plugin.logger.info(LanguageKeys.Commands.Reload.LOG_SUCCESS, sender.name)
            
        } catch (e: Exception) {
            // 处理重载失败
            plugin.logger.error(LanguageKeys.Commands.Reload.LOG_FAILED, e, sender.name)
            plugin.messager.printf(sender, LanguageKeys.Commands.Reload.FAILED, e.message ?: "Unknown error")
        }
    }
}
