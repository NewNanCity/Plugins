package city.newnan.createarea.commands.user

import city.newnan.createarea.CreateAreaPlugin
import city.newnan.createarea.i18n.LanguageKeys
import city.newnan.createarea.utils.WorldEditUtils
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * 设置命令
 *
 * 设置创造区域。
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class SetCommand(private val plugin: CreateAreaPlugin) {

    /**
     * 设置自己的创造区域
     */
    @Command("createarea|carea set")
    @CommandDescription(LanguageKeys.Commands.Set.DESCRIPTION)
    @Permission("createarea.set.self")
    fun setSelfCommand(sender: CommandSender) {
        // 检查是否为玩家
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        // 检查WorldEdit是否可用
        if (!WorldEditUtils.isWorldEditAvailable()) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Set.WORLDEDIT_NOT_FOUND)
            return
        }

        // 获取选择
        val selection = WorldEditUtils.getPlayerSelection(sender)
        if (selection == null) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Set.NO_SELECTION)
            return
        }

        val manager = plugin.getCreateAreaManager()
        manager.updateArea(
            sender, 
            selection.range.minX, 
            selection.range.maxX, 
            selection.range.minZ, 
            selection.range.maxZ
        )

        plugin.messager.printf(sender, LanguageKeys.Commands.Set.SUCCESS_SELF)
    }

    /**
     * 设置指定玩家的创造区域
     */
    @Command("createarea|carea set <target>")
    @CommandDescription(LanguageKeys.Commands.Set.DESCRIPTION)
    @Permission("createarea.set.other")
    fun setOtherCommand(
        sender: CommandSender,
        @Argument(value = "target", description = LanguageKeys.Commands.Set.PLAYER_ARG)
        targetName: String
    ) {
        // 检查是否为玩家
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_ONLY)
            return
        }

        // 检查WorldEdit是否可用
        if (!WorldEditUtils.isWorldEditAvailable()) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Set.WORLDEDIT_NOT_FOUND)
            return
        }

        // 查找目标玩家
        val targetPlayer = Bukkit.getOfflinePlayers().find { it.name == targetName }
        if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) {
            plugin.messager.printf(sender, LanguageKeys.Core.Error.PLAYER_NOT_FOUND, targetName)
            return
        }

        // 获取选择
        val selection = WorldEditUtils.getPlayerSelection(sender)
        if (selection == null) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Set.NO_SELECTION)
            return
        }

        val manager = plugin.getCreateAreaManager()
        manager.updateArea(
            targetPlayer, 
            selection.range.minX, 
            selection.range.maxX, 
            selection.range.minZ, 
            selection.range.maxZ
        )

        plugin.messager.printf(sender, LanguageKeys.Commands.Set.SUCCESS_OTHER, targetName)
    }

    /**
     * CSET命令别名 - 设置自己的创造区域
     */
    @Command("cset")
    @CommandDescription(LanguageKeys.Commands.Set.DESCRIPTION)
    @Permission("createarea.set.self")
    fun csetCommand(sender: CommandSender) {
        setSelfCommand(sender)
    }

    /**
     * CSET命令别名 - 设置指定玩家的创造区域
     */
    @Command("cset <target>")
    @CommandDescription(LanguageKeys.Commands.Set.DESCRIPTION)
    @Permission("createarea.set.other")
    fun csetOtherCommand(
        sender: CommandSender,
        @Argument(value = "target", description = LanguageKeys.Commands.Set.PLAYER_ARG)
        targetName: String
    ) {
        setOtherCommand(sender, targetName)
    }
}
