package city.newnan.newnanmain.gui.input

import city.newnan.gui.dsl.chatInput
import city.newnan.newnanmain.NewNanMainPlugin
import city.newnan.newnanmain.i18n.LanguageKeys
import org.bukkit.entity.Player

/**
 * 前缀创建/编辑输入处理器
 *
 * 处理前缀的创建和编辑操作
 *
 * @param plugin 插件实例
 * @param player 玩家
 * @param oldPrefix 要编辑的前缀(null表示创建新前缀)
 * @param setPrefix 回调函数，接收前缀结果
 */
fun handlePrefixInput(
    plugin: NewNanMainPlugin,
    player: Player,
    oldPrefix: Pair<String, String>?,
    setPrefix: (prefix: Pair<String, String>?) -> Unit
) {
    var key: String? = oldPrefix?.first
    var value: String? = oldPrefix?.second

    fun help() {
        plugin.messager.printf(player, LanguageKeys.Input.Prefix.HELP)
    }

    val result = plugin.chatInput(player, hide = true) { input ->
        val argv = input.split(" ").filter { it.isNotEmpty() }
        when (argv[0].lowercase()) {
            "name" -> {
                if (argv.size < 2) {
                    plugin.messager.printf(player, LanguageKeys.Input.Prefix.NAME_REQUIRED)
                    help()
                    return@chatInput false
                }
                key = argv[1]
                plugin.messager.printf(player, LanguageKeys.Input.Prefix.NAME_SET, key)
                false
            }
            "text" -> {
                if (argv.size < 2) {
                    plugin.messager.printf(player, LanguageKeys.Input.Prefix.TEXT_REQUIRED)
                    help()
                    return@chatInput false
                }
                value = argv.subList(1, argv.size).joinToString(" ")
                plugin.messager.printf(player, LanguageKeys.Input.Prefix.TEXT_SET, value)
                false
            }
            "cancel" -> {
                plugin.messager.printf(player, LanguageKeys.Input.Common.CANCELLED)
                setPrefix(null)
                true
            }
            "ok" -> {
                if (key == null) {
                    plugin.messager.printf(player, LanguageKeys.Input.Prefix.NAME_REQUIRED)
                    help()
                    false
                } else if (value == null) {
                    plugin.messager.printf(player, LanguageKeys.Input.Prefix.TEXT_REQUIRED)
                    help()
                    false
                } else {
                    try {
                        setPrefix(key to value)
                        true
                    } catch (e: Exception) {
                        plugin.logger.error("Failed to save prefix", e)
                        plugin.messager.printf(player, LanguageKeys.Input.Prefix.SAVE_FAILED, e.message ?: "Unknown error")
                        false
                    }
                }
            }
            else -> {
                plugin.messager.printf(player, LanguageKeys.Input.Common.UNKNOWN_COMMAND)
                help()
                false
            }
        }
    }

    if (result) {
        if (oldPrefix != null) {
            plugin.messager.printf(player, LanguageKeys.Input.Prefix.EDIT_PROMPT, oldPrefix.first)
        } else {
            plugin.messager.printf(player, LanguageKeys.Input.Prefix.PROMPT)
        }
        // 在开始时显示帮助信息
        help()
    } else {
        plugin.messager.printf(player, LanguageKeys.Input.Common.CHAT_INPUT_BUSY)
        setPrefix(null)
    }
}
