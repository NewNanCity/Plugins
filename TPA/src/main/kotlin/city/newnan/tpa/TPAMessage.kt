package city.newnan.tpa

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text

fun sendRequest(session: Session) {
    val target = if (session.target.isOnline) session.target.player ?: return else return
    val tpa = !session.targetToRequester

    val acceptButton = TextComponent(" [接受]").also {
        it.color = ChatColor.GREEN
        it.isBold = true
        it.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpa accept-tpa-request-by-id-private ${session.id}")
        it.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(
            if (tpa) "§a接受请求, 对方将传送到你身边!" else "§a接受请求, 你将传送到对方身边!"
        ))
    }

    val refuseButton = TextComponent(" [拒绝]").also {
        it.color = ChatColor.RED
        it.isBold = true
        it.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpa refuse-tpa-request-by-id-private ${session.id}")
        it.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("§c拒绝对方的请求!"))
    }

    val blockButton = TextComponent(" [拉黑]").also {
        it.color = ChatColor.DARK_GRAY
        it.isBold = true
        it.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpa block ${session.requester.name}")
        it.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("§e该玩家以后的传送请求都将不会提示!"))
    }

    val requestText = TextComponent(if (tpa) "            §6${session.requester.name} §r§f想来你这里瞧瞧~"
                                    else     "            §6${session.requester.name} §r§f邀请你去他那边做客~").also {
        it.addExtra(acceptButton)
        it.addExtra(refuseButton)
        it.addExtra(blockButton)
    }

    PluginMain.INSTANCE.messageManager.printf(target, false, "§7§l§m-----------------------------------------------------------------------")
    target.spigot().sendMessage(requestText)
    PluginMain.INSTANCE.messageManager.printf(target, false, "§7§l§m-----------------------------------------------------------------------")
}