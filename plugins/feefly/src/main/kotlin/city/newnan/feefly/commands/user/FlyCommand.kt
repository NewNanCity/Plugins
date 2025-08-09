package city.newnan.feefly.commands.user

import city.newnan.feefly.FeeFlyPlugin
import city.newnan.feefly.i18n.LanguageKeys
import org.bukkit.block.CommandBlock
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.CommandDescription
import org.incendo.cloud.annotations.Default
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.bukkit.data.SinglePlayerSelector
import org.incendo.cloud.type.Either

/**
 * 飞行切换命令
 *
 * 核心飞行功能命令，支持：
 * - 为自己切换飞行状态
 * - 为其他玩家切换飞行状态（需要额外权限）
 * - 付费飞行和免费飞行的处理
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class FlyCommand(private val plugin: FeeFlyPlugin) {

    @Command("fly [target]")
    @Command("feefly fly [target]")
    @CommandDescription(LanguageKeys.Commands.Fly.DESCRIPTION)
    @Permission("feefly.self")
    fun flyCommand(
        sender: CommandSender,
        @Argument(value = "target", description = LanguageKeys.Commands.Fly.PLAYER_OR_SELECTOR) player: Either<Player, SinglePlayerSelector>?
    ) {
        // 获取目标
        val targetPlayer = player?.mapEither(
                { it },
                { it.single() }
        ) ?: run {
            if (sender !is Player) {
                plugin.messager.printf(sender, LanguageKeys.Commands.Fly.PLAYER_ONLY)
                return@flyCommand
            }
            sender
        }

        if (targetPlayer != sender) {
            // 为其他玩家切换飞行
            if (sender !is CommandBlock && sender !is ConsoleCommandSender && !sender.hasPermission("feefly.other")) {
                plugin.messager.printf(sender, LanguageKeys.Core.Error.NO_PERMISSION)
                return
            }

            plugin.flyManager.toggleFly(targetPlayer)
            plugin.messager.printf(sender, LanguageKeys.Commands.Fly.TOGGLED_FOR_PLAYER, targetPlayer.name)
            plugin.logger.info(LanguageKeys.Commands.Fly.LOG_TOGGLED_BY_ADMIN, sender.name, targetPlayer.name)
        } else {
            // 为自己切换飞行
            plugin.flyManager.toggleFly(targetPlayer)
            plugin.logger.info(LanguageKeys.Commands.Fly.LOG_TOGGLED_SELF, sender.name)
        }
    }
}