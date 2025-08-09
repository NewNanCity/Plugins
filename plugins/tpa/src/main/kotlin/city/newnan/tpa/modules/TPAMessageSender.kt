package city.newnan.tpa.modules

import city.newnan.tpa.TPAPlugin
import city.newnan.tpa.modules.TPASession
import city.newnan.tpa.i18n.LanguageKeys
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

/**
 * TPA消息发送器
 *
 * 负责发送交互式的TPA请求消息，包含可点击的按钮。
 *
 * @author AI Assistant
 * @since 2.0.0
 */
class TPAMessageSender(private val plugin: TPAPlugin) {

    /**
     * 发送传送请求消息给目标玩家
     */
    fun sendRequestMessage(session: TPASession) {
        if (!session.target.isOnline) return

        val target = if (session.target.isOnline) session.target.player ?: return else return

        // 发送分隔线
        plugin.messager.printf(target, LanguageKeys.TPA.SEPARATOR)
        plugin.messager.printf(target,
            when (session.direction) {
                TPADirection.TARGET_TO_REQUESTER -> LanguageKeys.TPA.Request.INCOMING_TPAHERE
                TPADirection.REQUESTER_TO_TARGET -> LanguageKeys.TPA.Request.INCOMING_TPA
            },
            session.requester.name, session.id, session.requester.name
        )
        plugin.messager.printf(target, LanguageKeys.TPA.SEPARATOR)
    }

    /**
     * 发送倒计时消息
     */
    fun sendCountdownMessage(player: Player, seconds: Int, isFrom: Boolean) {
        val messageKey = if (isFrom) {
            LanguageKeys.TPA.Teleport.COUNTDOWN_FROM
        } else {
            LanguageKeys.TPA.Teleport.COUNTDOWN_TO
        }

        player.sendActionBar(plugin.messager.sprintf(messageKey, seconds))
    }

    /**
     * 发送传送成功消息
     */
    fun sendTeleportSuccessMessage(fromPlayer: Player, toPlayer: Player) {
        // 发送给传送者
        val fromMessage = plugin.messager.sprintfPlain(LanguageKeys.TPA.Teleport.SUCCESS_FROM)
        plugin.messager.printf(fromPlayer, fromMessage)

        // 发送给目标玩家
        val toMessage = plugin.messager.sprintfPlain(LanguageKeys.TPA.Teleport.SUCCESS_TO)
        plugin.messager.printf(toPlayer, toMessage)
    }
}