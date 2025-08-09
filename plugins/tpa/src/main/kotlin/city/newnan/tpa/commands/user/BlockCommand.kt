package city.newnan.tpa.commands.user

import city.newnan.tpa.TPAPlugin
import city.newnan.tpa.i18n.LanguageKeys
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Permission

/**
 * TPA屏蔽命令
 *
 * 提供屏蔽特定玩家传送请求的功能
 *
 * @author AI Assistant
 * @since 2.0.0
 */
class BlockCommand(private val plugin: TPAPlugin) {

    @Command("tpa-block <player>")
    @CommandDescription(LanguageKeys.Commands.Block.DESCRIPTION)
    @Permission("tpa.use")
    fun blockCommand(
        sender: CommandSender,
        @Argument(value = "player", description = LanguageKeys.Commands.Block.PLAYER_ARG) targetPlayer: OfflinePlayer
    ) {
        // 发送人必须是玩家
        if (sender !is Player) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Common.PLAYER_ONLY)
            return
        }
        
        if (sender.uniqueId == targetPlayer.uniqueId) {
            plugin.messager.printf(sender, LanguageKeys.Commands.Block.SELF_BLOCK)
            return
        }
        
        plugin.getBlockManager().blockPlayer(sender, targetPlayer)
        plugin.messager.printf(sender, LanguageKeys.Commands.Block.SUCCESS, targetPlayer.name ?: "?")
        plugin.messager.printf(sender, LanguageKeys.Commands.Block.HINT, targetPlayer.name ?: "?")
    }
}