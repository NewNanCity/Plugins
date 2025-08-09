package city.newnan.newnanmain.gui

import city.newnan.gui.component.paginated.onLeftClick
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dsl.*
import city.newnan.newnanmain.NewNanMainPlugin
import city.newnan.newnanmain.i18n.LanguageKeys
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 玩家前缀管理GUI
 *
 * 显示玩家可用的前缀，支持：
 * - 查看所有可用前缀
 * - 激活/禁用前缀
 * - 实时预览前缀效果
 * - 自动清理无效前缀
 *
 * @author NewNanCity
 * @since 2.0.0
 */
fun openPlayerPrefixGui(plugin: NewNanMainPlugin, player: Player, target: OfflinePlayer) {
    val prefixManager = plugin.prefixManager
    val config = prefixManager.getPlayerPrefixConfig(target)

    plugin.openPage(InventoryType.CHEST, 54, player,
        format(LanguageKeys.Gui.Prefix.TITLE, target.name ?: "Unknown")) {

        // 装饰边框
        borderFillComponent(0, 5, 9, 6) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 玩家头像和信息
        slotComponent(x = 4, y = 0) {
            render {
                val currentPrefixText = if (config.current != null) {
                    val namespace = config.current!!
                    val key = config.available[namespace]
                    val prefixText = if (key != null) prefixManager.getGlobalPrefix(namespace, key) else null
                    if (prefixText != null) {
                        formatPlain(LanguageKeys.Gui.Prefix.PLAYER_INFO_CURRENT, prefixText)
                    } else formatPlain(LanguageKeys.Gui.Prefix.PLAYER_INFO_CURRENT_NONE)
                } else formatPlain(LanguageKeys.Gui.Prefix.PLAYER_INFO_CURRENT_NONE)
                val part1 = currentPrefixText
                val part2 = formatPlain(LanguageKeys.Gui.Prefix.PLAYER_INFO_AVAILABLE_COUNT, config.available.size)
                val part3 = formatPlain(LanguageKeys.Gui.Prefix.PLAYER_INFO_SELECT_PROMPT)
                skull(target, target.name ?: "Unknown", "$part1\n$part2\n\n$part3")
            }
        }

        // 使用分页组件显示前缀
        val paginatedComp = paginatedComponent(
            startX = 1, startY = 1,
            width = 7, height = 4,
            data = {
                // 准备前缀数据
                val prefixData = mutableListOf<PrefixData>()
                val namespacesToDelete = mutableListOf<String>()

                config.available.forEach { entry ->
                    val namespace = entry.key
                    val key = entry.value
                    val prefixText = prefixManager.getGlobalPrefix(namespace, key)
                    if (prefixText == null) {
                        namespacesToDelete.add(namespace)
                    } else {
                        prefixData.add(PrefixData(namespace, key, prefixText, namespace == config.current))
                    }
                }

                // 清理无效前缀
                if (namespacesToDelete.isNotEmpty()) {
                    namespacesToDelete.forEach { namespace -> prefixManager.removePlayerPrefix(target, namespace) }
                    plugin.messager.printf(player, LanguageKeys.Gui.Prefix.CLEAN_INVALID, namespacesToDelete.size)
                }
                prefixData
            }
        ) {
            renderEmptySlot { item(Material.BARRIER) { name(LanguageKeys.Gui.Prefix.NO_PREFIXES) } }

            render { context ->
                val prefixData = context.item ?: return@render null
                val material = if (prefixData.isActive) Material.LIME_WOOL else Material.RED_WOOL

                item(material) {
                    name(prefixData.prefixText)
                    val nsLine = formatPlain(LanguageKeys.Gui.Prefix.ITEM_NAMESPACE_LINE, prefixData.namespace)
                    val keyLine = formatPlain(LanguageKeys.Gui.Prefix.ITEM_KEY_LINE, prefixData.key)
                    val statusLine = formatPlain(if (prefixData.isActive) LanguageKeys.Gui.Prefix.ITEM_STATUS_ENABLED else LanguageKeys.Gui.Prefix.ITEM_STATUS_DISABLED)
                    val actionLine = formatPlain(if (prefixData.isActive) LanguageKeys.Gui.Prefix.ITEM_ACTION_DISABLE else LanguageKeys.Gui.Prefix.ITEM_ACTION_ENABLE)
                    lore("$nsLine\n$keyLine\n$statusLine\n\n$actionLine")
                }
            }

            // 左键点击切换前缀状态
            onLeftClick { _, _, prefixData ->
                if (prefixData != null) {
                    if (prefixData.isActive) {
                        // 禁用当前前缀
                        prefixManager.deactivatePlayerPrefix(target)
                        plugin.messager.printf(player, LanguageKeys.Prefix.REMOVED)
                    } else {
                        // 启用选中的前缀
                        prefixManager.activatePlayerPrefix(target, prefixData.namespace)
                        plugin.messager.printf(player, LanguageKeys.Prefix.ACTIVATED, prefixData.prefixText)
                    }

                    // 刷新界面
                    this@paginatedComponent.clearCache()
                    this@openPage.show()
                }
            }
        }

        // 上一页按钮
        slotComponent(x = 2, y = 5) {
            render {
                urlSkull("bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9") {
                    name(LanguageKeys.Gui.Common.PREVIOUS)
                }
            }
            onLeftClick { _, _, _ ->
                paginatedComp.previousPage()
            }
        }

        // 下一页按钮
        slotComponent(x = 6, y = 5) {
            render {
                urlSkull("19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf") {
                    name(LanguageKeys.Gui.Common.NEXT)
                }
            }
            onLeftClick { _, _, _ ->
                paginatedComp.nextPage()
            }
        }

        // 返回按钮
        slotComponent(x = 8, y = 5) {
            render {
                item(Material.ARROW) {
                    name(LanguageKeys.Gui.Common.BACK)
                }
            }
            onLeftClick { _, _, _ ->
                back()
            }
        }
    }
}

/**
 * 前缀数据类
 */
data class PrefixData(
    val namespace: String,
    val key: String,
    val prefixText: String,
    val isActive: Boolean
)
