package city.newnan.bettercommandblock.modules

import city.newnan.bettercommandblock.BetterCommandBlockPlugin
import city.newnan.bettercommandblock.i18n.LanguageKeys
import city.newnan.core.base.BaseModule
import city.newnan.core.event.subscribeEvent
import org.bukkit.Material
import org.bukkit.block.CommandBlock
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

/**
 * 命令方块查看模块
 *
 * 负责处理玩家右键查看命令方块内容的功能。
 * 当玩家右键点击命令方块时，如果玩家有相应权限，会显示命令方块中的命令内容。
 *
 * 功能特性：
 * - 权限检查：只有拥有 better-command-block.read 权限的玩家才能查看
 * - 安全检查：只有非OP玩家才会触发查看功能（OP可以直接编辑）
 * - 国际化支持：所有消息都支持多语言
 *
 * @author NewNanCity
 * @since 2.0.0
 */
class CommandBlockViewModule(
    val plugin: BetterCommandBlockPlugin
) : BaseModule("CommandBlockView", plugin) {

    companion object {
        /**
         * 查看命令方块内容的权限节点
         */
        private const val VIEW_PERMISSION = "better-command-block.read"
    }

    init {
        init()
    }

    override fun onInit() {
        // 监听玩家与命令方块的交互事件
        subscribeEvent<PlayerInteractEvent> {
            priority(EventPriority.HIGHEST)
            filter { it.action == Action.RIGHT_CLICK_BLOCK }
            filter { it.clickedBlock?.type == Material.COMMAND_BLOCK }
            filter { it.useInteractedBlock() != Event.Result.DENY }
            filter { !it.player.isOp } // 只对非OP玩家生效，OP可以直接编辑
            filter { it.player.hasPermission(VIEW_PERMISSION) }
            handler { event ->
                try {
                    handleCommandBlockView(event)
                } catch (e: Exception) {
                    logger.error(LanguageKeys.Log.Error.EVENT_HANDLING_FAILED, e)
                    messager.printf(event.player, LanguageKeys.Core.Error.OPERATION_FAILED, e.message)
                }
            }
        }
    }

    /**
     * 处理命令方块查看事件
     */
    private fun handleCommandBlockView(event: PlayerInteractEvent) {
        val player = event.player
        val block = event.clickedBlock!!

        logger.debug(LanguageKeys.Log.Debug.PLAYER_INTERACTION, player.name)

        try {
            // 获取命令方块状态
            val commandBlock = block.state as CommandBlock
            val command = commandBlock.command

            // 显示命令内容
            if (command.isNotEmpty()) {
                messager.printf(player, LanguageKeys.View.COMMAND_CONTENT, command)
            } else {
                messager.printf(player, LanguageKeys.View.COMMAND_CONTENT, "<empty>")
            }

            // 取消事件，防止其他插件处理
            event.isCancelled = true

            logger.debug("Player ${player.name} viewed command block at " +
                        "(${block.x}, ${block.y}, ${block.z}): $command")

        } catch (e: Exception) {
            logger.error("Failed to handle command block view for player ${player.name}", e)
            messager.printf(player, LanguageKeys.Core.Error.OPERATION_FAILED, e.message)
        }
    }
}
