package city.newnan.mcron.commands.admin

import city.newnan.mcron.MCronPlugin
import city.newnan.mcron.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 重载命令
 *
 * 重载MCron插件配置，包括：
 * - 重新加载配置文件
 * - 重新初始化管理器
 * - 重新启动定时任务
 * - 更新运行时设置
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class ReloadCommand(private val plugin: MCronPlugin) {

    /**
     * 重载命令处理方法
     *
     * @param sender 命令发送者
     */
    @Command("mcron|cron reload")
    @CommandDescription(LanguageKeys.Commands.Reload.DESCRIPTION)
    @Permission("mcron.reload")
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
