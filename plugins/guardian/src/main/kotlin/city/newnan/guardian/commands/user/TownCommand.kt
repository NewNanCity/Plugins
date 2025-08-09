package city.newnan.guardian.commands.user

import city.newnan.core.scheduler.runAsync
import city.newnan.core.scheduler.runSync
import city.newnan.guardian.GuardianPlugin
import city.newnan.guardian.gui.openTownGui
import city.newnan.guardian.i18n.LanguageKeys
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Default

/**
 * 小镇信息查询命令
 *
 * 查看小镇信息和打开小镇管理GUI
 *
 * @author Guardian Team
 * @since 2.0.0
 */
class TownCommand(private val plugin: GuardianPlugin) {

    @Command("guardian|gd town [town-name]")
    @Command("town [town-name]")
    @CommandDescription(LanguageKeys.Commands.Town.DESCRIPTION)
    fun townCommand(
        sender: CommandSender,
        @Argument(value = "town-name", description = LanguageKeys.Commands.Town.TOWN_NAME) @Default("") townName: String
    ) {
        // 1. 玩家检查
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        // 2. 权限检查和参数处理
        val targetTownName = if (townName.isEmpty()) {
            null // 让GUI查询玩家自己的小镇
        } else {
            // 检查是否有权限查看其他小镇
            if (!sender.isOp && !sender.hasPermission("guardian.town.read.other")) {
                plugin.messager.printf(sender, LanguageKeys.Commands.Common.NO_PERMISSION)
                return
            }
            townName
        }

        // 3. 直接打开GUI，让GUI自己查询数据
        try {
            plugin.messager.printf(sender, LanguageKeys.Commands.Town.GUI_OPENING)

            // 打开小镇管理GUI，传递玩家和目标小镇名（可能为null）
            openTownGui(plugin, sender, targetTownName)

            // 记录成功日志
            plugin.logger.info(LanguageKeys.Commands.Town.LOG_SUCCESS, sender.name, targetTownName ?: "own")

        } catch (e: Exception) {
            plugin.logger.error(LanguageKeys.Commands.Town.LOG_FAILED, e, sender.name, targetTownName ?: "own")
            plugin.messager.printf(sender, LanguageKeys.Commands.Town.GUI_FAILED)
        }
    }
}
