package city.newnan.newnanmain.gui.input

import city.newnan.gui.dsl.chatInput
import city.newnan.newnanmain.NewNanMainPlugin
import city.newnan.newnanmain.i18n.LanguageKeys
import org.bukkit.entity.Player

/**
 * 确认/取消输入处理器
 *
 * 处理简单的是/否确认操作
 *
 * @param plugin 插件实例
 * @param player 玩家
 * @param text 提示文本
 * @param setYes 回调函数，接收确认结果
 */
fun handleYesInput(
    plugin: NewNanMainPlugin,
    player: Player,
    text: String,
    setYes: (yes: Boolean) -> Unit
) {
    val result = plugin.chatInput(player, hide = true) { input ->
        setYes(input.lowercase() == "y" || input.lowercase() == "yes")
        true // 结束输入
    }

    if (result) {
        plugin.messager.printf(player, text)
    } else {
        plugin.messager.printf(player, LanguageKeys.Input.Common.CHAT_INPUT_BUSY)
        setYes(false)
    }
}
