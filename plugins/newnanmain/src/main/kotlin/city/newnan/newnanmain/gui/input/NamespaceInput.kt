package city.newnan.newnanmain.gui.input

import city.newnan.gui.dsl.chatInput
import city.newnan.newnanmain.NewNanMainPlugin
import city.newnan.newnanmain.i18n.LanguageKeys
import org.bukkit.entity.Player

/**
 * 命名空间创建/编辑输入处理器
 *
 * 处理命名空间的创建和编辑操作
 *
 * @param plugin 插件实例
 * @param player 玩家
 * @param oldNamespace 要编辑的命名空间(null表示创建新命名空间)
 * @param setNamespace 回调函数，接收命名空间结果
 */
fun handleNamespaceInput(
    plugin: NewNanMainPlugin,
    player: Player,
    oldNamespace: String?,
    setNamespace: (namespace: String?) -> Unit
) {
    var namespace: String? = oldNamespace

    fun help() {
        plugin.messager.printf(player, LanguageKeys.Input.Namespace.HELP)
    }

    val result = plugin.chatInput(player, hide = true) { input ->
        val argv = input.split(" ").filter { it.isNotEmpty() }
        when (argv[0].lowercase()) {
            "name" -> {
                if (argv.size < 2) {
                    plugin.messager.printf(player, LanguageKeys.Input.Namespace.NAME_REQUIRED)
                    help()
                    return@chatInput false
                }
                val nameT = argv[1]
                val prefixManager = plugin.prefixManager
                if (prefixManager.getAllGlobalPrefixes().containsKey(nameT) && nameT != oldNamespace) {
                    plugin.messager.printf(player, LanguageKeys.Input.Namespace.NAME_EXISTS, nameT)
                } else {
                    namespace = nameT
                    plugin.messager.printf(player, LanguageKeys.Input.Namespace.NAME_SET, namespace)
                }
                false
            }
            "cancel" -> {
                plugin.messager.printf(player, LanguageKeys.Input.Common.CANCELLED)
                setNamespace(null)
                true
            }
            "ok" -> {
                if (namespace == null) {
                    plugin.messager.printf(player, LanguageKeys.Input.Namespace.NAME_REQUIRED)
                    help()
                    false
                } else {
                    try {
                        setNamespace(namespace)
                        true
                    } catch (e: Exception) {
                        plugin.logger.error("Failed to save namespace", e)
                        plugin.messager.printf(player, LanguageKeys.Input.Namespace.SAVE_FAILED, e.message ?: "Unknown error")
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
        if (oldNamespace != null) {
            plugin.messager.printf(player, LanguageKeys.Input.Namespace.EDIT_PROMPT, oldNamespace)
        } else {
            plugin.messager.printf(player, LanguageKeys.Input.Namespace.PROMPT)
        }
        // 在开始时显示帮助信息
        help()
    } else {
        plugin.messager.printf(player, LanguageKeys.Input.Common.CHAT_INPUT_BUSY)
        setNamespace(null)
    }
}
