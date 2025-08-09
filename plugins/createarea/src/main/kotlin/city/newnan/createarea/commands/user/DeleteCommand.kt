package city.newnan.createarea.commands.user

import city.newnan.createarea.CreateAreaPlugin
import city.newnan.createarea.i18n.LanguageKeys
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 删除命令
 *
 * 删除创造区域。
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class DeleteCommand(private val plugin: CreateAreaPlugin) {

    /**
     * 删除自己的创造区域
     */
    @Command("createarea|carea delete|remove|del")
    @CommandDescription(LanguageKeys.Commands.Delete.DESCRIPTION)
    @Permission("createarea.delete.self")
    fun deleteSelfCommand(sender: CommandSender) {
        // 检查是否为玩家
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        val manager = plugin.getCreateAreaManager()
        
        if (manager.deleteArea(sender)) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Delete.SUCCESS_SELF)
        } else {
            plugin.messager.printf(sender, LanguageKeys.Commands.Delete.AREA_NOT_FOUND_SELF)
        }
    }

    /**
     * 删除指定玩家的创造区域
     */
    @Command("createarea|carea delete|remove|del <target>")
    @CommandDescription(LanguageKeys.Commands.Delete.DESCRIPTION)
    @Permission("createarea.delete.other")
    fun deleteOtherCommand(
        sender: CommandSender,
        @Argument(value = "target", description = LanguageKeys.Commands.Delete.PLAYER_ARG)
        targetName: String
    ) {
        // 查找目标玩家
        val targetPlayer = Bukkit.getOfflinePlayers().find { it.name == targetName }
        if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_NOT_FOUND, targetName)
            return
        }

        val manager = plugin.getCreateAreaManager()
        
        if (manager.deleteArea(targetPlayer)) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Delete.SUCCESS_OTHER, targetName)
        } else {
            plugin.messager.printf(sender, LanguageKeys.Commands.Delete.AREA_NOT_FOUND_OTHER, targetName)
        }
    }

    /**
     * CDEL命令别名 - 删除自己的创造区域
     */
    @Command("cdel")
    @CommandDescription(LanguageKeys.Commands.Delete.DESCRIPTION)
    @Permission("createarea.delete.self")
    fun cdelCommand(sender: CommandSender) {
        deleteSelfCommand(sender)
    }

    /**
     * CDEL命令别名 - 删除指定玩家的创造区域
     */
    @Command("cdel <target>")
    @CommandDescription(LanguageKeys.Commands.Delete.DESCRIPTION)
    @Permission("createarea.delete.other")
    fun cdelOtherCommand(
        sender: CommandSender,
        @Argument(value = "target", description = LanguageKeys.Commands.Delete.PLAYER_ARG)
        targetName: String
    ) {
        deleteOtherCommand(sender, targetName)
    }
}
