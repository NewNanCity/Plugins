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
 * 传送命令
 *
 * 传送到创造区域。
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class TpCommand(private val plugin: CreateAreaPlugin) {

    /**
     * 传送到自己的创造区域
     */
    @Command("createarea|carea tp")
    @CommandDescription(LanguageKeys.Commands.Tp.DESCRIPTION)
    @Permission("createarea.tp.self")
    fun tpSelfCommand(sender: CommandSender) {
        // 检查是否为玩家
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        val manager = plugin.getCreateAreaManager()
        val area = manager.getPlayerArea(sender)

        if (area == null) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Tp.AREA_NOT_FOUND_SELF)
            return
        }

        if (manager.teleportToArea(sender, area)) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Tp.SUCCESS_SELF)
        } else {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.OPERATION_FAILED, "传送失败")
        }
    }

    /**
     * 传送到指定玩家的创造区域
     */
    @Command("createarea|carea tp <target>")
    @CommandDescription(LanguageKeys.Commands.Tp.DESCRIPTION)
    @Permission("createarea.tp.other")
    fun tpOtherCommand(
        sender: CommandSender,
        @Argument(value = "target", description = LanguageKeys.Commands.Tp.PLAYER_ARG)
        targetName: String
    ) {
        // 检查是否为玩家
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        // 查找目标玩家
        val targetPlayer = Bukkit.getOfflinePlayers().find { it.name == targetName }
        if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_NOT_FOUND, targetName)
            return
        }

        val manager = plugin.getCreateAreaManager()
        val area = manager.getPlayerArea(targetPlayer)

        if (area == null) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Tp.AREA_NOT_FOUND_OTHER, targetName)
            return
        }

        if (manager.teleportToArea(sender, area)) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Tp.SUCCESS_OTHER, targetName)
        } else {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.OPERATION_FAILED, "传送失败")
        }
    }

    /**
     * CTP命令别名 - 传送到自己的创造区域
     */
    @Command("ctp")
    @CommandDescription(LanguageKeys.Commands.Tp.DESCRIPTION)
    @Permission("createarea.tp.self")
    fun ctpCommand(sender: CommandSender) {
        tpSelfCommand(sender)
    }

    /**
     * CTP命令别名 - 传送到指定玩家的创造区域
     */
    @Command("ctp <target>")
    @CommandDescription(LanguageKeys.Commands.Tp.DESCRIPTION)
    @Permission("createarea.tp.other")
    fun ctpOtherCommand(
        sender: CommandSender,
        @Argument(value = "target", description = LanguageKeys.Commands.Tp.PLAYER_ARG)
        targetName: String
    ) {
        tpOtherCommand(sender, targetName)
    }
}
