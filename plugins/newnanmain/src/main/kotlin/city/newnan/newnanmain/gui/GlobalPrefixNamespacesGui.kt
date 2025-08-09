package city.newnan.newnanmain.gui

import city.newnan.gui.component.paginated.onLeftClick
import city.newnan.gui.component.paginated.onShiftRightClick
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dsl.*
import city.newnan.newnanmain.NewNanMainPlugin
import city.newnan.newnanmain.gui.input.handleNamespaceInput
import city.newnan.newnanmain.gui.input.handleYesInput
import city.newnan.newnanmain.i18n.LanguageKeys
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 全局前缀命名空间管理GUI
 *
 * 提供全局前缀命名空间的管理功能，包括：
 * - 查看所有命名空间
 * - 添加新命名空间
 * - 删除命名空间
 * - 进入命名空间管理前缀
 *
 * @author NewNanCity
 * @since 2.0.0
 */
fun openGlobalPrefixNamespacesGui(plugin: NewNanMainPlugin, player: Player) {
    val prefixManager = plugin.prefixManager

    plugin.openPage(InventoryType.CHEST, 54, player, formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_NAMESPACE_MANAGE_TITLE)) {
        // 边框填充
        borderFillComponent(0, 0, 9, 6) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 使用分页组件显示命名空间
        val paginatedComp = paginatedComponent(
            startX = 1, startY = 1,
            width = 7, height = 4,
            data = { prefixManager.getAllGlobalPrefixes().entries.toList() }
        ) {
            render { context ->
                val (namespace, prefixes) = context.item ?: return@render null

                item(Material.CHEST) {
                    name(namespace)
                    val line1 = formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_NAMESPACE_ITEM_COUNT, prefixes.size)
                    val line2 = formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_NAMESPACE_ITEM_ACTION_VIEW)
                    val line3 = formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_NAMESPACE_ITEM_ACTION_DELETE)
                    lore("$line1\n\n$line2\n$line3")
                }
            }

            // 左键点击进入命名空间管理
            onLeftClick { _, _, namespaceData ->
                val (namespace, _) = namespaceData ?: return@onLeftClick
                openGlobalPrefixesGui(plugin, player, namespace)
            }

            // Shift+右键删除命名空间
            onShiftRightClick { _, _, namespaceData ->
                val (namespace, prefixes) = namespaceData ?: return@onShiftRightClick
                handleYesInput(
                    plugin,
                    player,
                    formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_NAMESPACE_DELETE_CONFIRM, namespace)
                ) { yes ->
                    if (yes) {
                        prefixManager.removeGlobalPrefixNamespace(namespace, true)
                        plugin.messager.printf(player, LanguageKeys.Gui.Prefix.GLOBAL_NAMESPACE_DELETED, namespace)
                        // 刷新界面
                        this@paginatedComponent.clearCache()
                    }
                    this@openPage.show()
                }
            }
        }

        // 添加命名空间按钮
        slotComponent(x = 0, y = 5) {
            render {
                item(Material.WRITABLE_BOOK) {
                    name(LanguageKeys.Gui.Prefix.GLOBAL_NAMESPACE_ADD)
                    val p1 = formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_NAMESPACE_ADD_DESC1)
                    val p2 = formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_NAMESPACE_ADD_DESC2)
                    val p3 = formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_NAMESPACE_ADD_ACTION)
                    lore("$p1\n$p2\n\n$p3")
                }
            }

            onLeftClick { _, _, _ ->
                handleNamespaceInput(plugin, player, null) { namespace ->
                    if (namespace != null) {
                        // 创建新的命名空间（如果不存在）
                        if (!prefixManager.getAllGlobalPrefixes().containsKey(namespace)) {
                            prefixManager.setGlobalPrefix(namespace, "_placeholder", "", false)
                            prefixManager.removeGlobalPrefix(namespace, "_placeholder", true)
                            plugin.messager.printf(player, LanguageKeys.Gui.Prefix.GLOBAL_NAMESPACE_CREATED, namespace)
                        } else {
                            plugin.messager.printf(player, LanguageKeys.Gui.Prefix.GLOBAL_NAMESPACE_EXISTS, namespace)
                        }
                    }
                    // 刷新界面
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