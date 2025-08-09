package city.newnan.newnanmain.gui

import city.newnan.gui.component.paginated.onLeftClick
import city.newnan.gui.component.paginated.onRightClick
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dsl.*
import city.newnan.newnanmain.NewNanMainPlugin
import city.newnan.newnanmain.gui.input.handlePrefixInput
import city.newnan.newnanmain.gui.input.handleYesInput
import city.newnan.newnanmain.i18n.LanguageKeys
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 全局前缀管理GUI
 *
 * 提供指定命名空间下前缀的管理功能，包括：
 * - 查看命名空间下的所有前缀
 * - 添加新前缀
 * - 编辑现有前缀
 * - 删除前缀
 *
 * @author NewNanCity
 * @since 2.0.0
 */
fun openGlobalPrefixesGui(plugin: NewNanMainPlugin, player: Player, namespace: String) {
    val prefixManager = plugin.prefixManager

    plugin.openPage(InventoryType.CHEST, 54, player, formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_MANAGE_TITLE, namespace)) {

        // 边框填充
        borderFillComponent(0, 0, 9, 6) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 使用分页组件显示前缀
        val paginatedComp = paginatedComponent(
            startX = 1, startY = 1,
            width = 7, height = 4,
            data = { prefixManager.getAllGlobalPrefixes()[namespace]?.entries?.toList() ?: emptyList() }
        ) {
            render { context ->
                val prefixData = context.item ?: return@render null

                item(Material.NAME_TAG) {
                    name(formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_PREFIX_ITEM_NAME, prefixData.key))
                    val line1 = formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_PREFIX_ITEM_CONTENT, prefixData.value)
                    val line2 = formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_PREFIX_ITEM_ACTION_EDIT)
                    val line3 = formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_PREFIX_ITEM_ACTION_DELETE)
                    lore("$line1\n\n$line2\n$line3")
                }
            }

            // 左键点击编辑前缀
            onLeftClick { _, _, prefixData ->
                if (prefixData != null) {
                    handlePrefixInput(plugin, player, prefixData.key to prefixData.value) { prefix ->
                        if (prefix != null) {
                            // 如果键名改变了，需要先删除旧的
                            if (prefix.first != prefixData.key) {
                                prefixManager.removeGlobalPrefix(namespace, prefixData.key, false)
                            }
                            prefixManager.setGlobalPrefix(namespace, prefix.first, prefix.second, true)
                            plugin.messager.printf(player, LanguageKeys.Gui.Prefix.GLOBAL_PREFIX_UPDATED, prefix.first)
                        }
                        // 刷新界面
                        this@paginatedComponent.clearCache()
                        this@paginatedComponent.update()
                    }
                }
            }

            // 右键点击删除前缀
            onRightClick { _, _, prefixData ->
                if (prefixData != null) {
                    handleYesInput(plugin, player, formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_PREFIX_DELETE_CONFIRM, prefixData.key)) { yes ->
                        if (yes) {
                prefixManager.removeGlobalPrefix(namespace, prefixData.key, true)
                plugin.messager.printf(player, LanguageKeys.Gui.Prefix.GLOBAL_PREFIX_DELETED, prefixData.key)
                        }
                        // 刷新界面
                        this@paginatedComponent.clearCache()
                        this@paginatedComponent.update()
                    }
                }
            }
        }

        // 上一页按钮
    slotComponent(x = 2, y = 5) {
            render {
        item(Material.ARROW) { name(LanguageKeys.Gui.Common.PREVIOUS) }
            }
            onLeftClick { _, _, _ ->
                paginatedComp.previousPage()
            }
        }

        // 下一页按钮
    slotComponent(x = 6, y = 5) {
            render {
        item(Material.ARROW) { name(LanguageKeys.Gui.Common.NEXT) }
            }
            onLeftClick { _, _, _ ->
                paginatedComp.nextPage()
            }
        }

        // 添加前缀按钮
        slotComponent(x = 4, y = 5) {
            render {
                item(Material.WRITABLE_BOOK) {
                    name(LanguageKeys.Gui.Prefix.GLOBAL_PREFIX_ADD)
                    val p1 = formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_PREFIX_ADD_DESC1, namespace)
                    val p2 = formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_PREFIX_ADD_DESC2)
                    val p3 = formatPlain(LanguageKeys.Gui.Prefix.GLOBAL_PREFIX_ADD_ACTION)
                    lore("$p1\n$p2\n\n$p3")
                }
            }

            onLeftClick { _, _, _ ->
                handlePrefixInput(plugin, player, null) { prefix ->
                    if (prefix != null) {
                        // 检查前缀键是否已存在
                        if (prefixManager.getAllGlobalPrefixes()[namespace]?.containsKey(prefix.first) == true) {
                            plugin.messager.printf(player, LanguageKeys.Gui.Prefix.GLOBAL_PREFIX_ADDED, prefix.first) // reuse or create NAME_EXISTS key if needed
                        } else {
                            prefixManager.setGlobalPrefix(namespace, prefix.first, prefix.second, true)
                            plugin.messager.printf(player, LanguageKeys.Gui.Prefix.GLOBAL_PREFIX_ADDED, prefix.first)
                        }
                    }
                    // 刷新界面
                    this@openPage.show()
                }
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