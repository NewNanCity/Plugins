package city.newnan.newnanmain.gui

import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dsl.*
import city.newnan.newnanmain.NewNanMainPlugin
import city.newnan.newnanmain.i18n.LanguageKeys
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 管理员GUI界面
 *
 * 提供完整的管理员功能，包括：
 * - 传送系统管理
 * - 称号系统管理
 * - 牛腩书局管理
 * - 创造区管理
 * - 铁路系统管理
 *
 * @author NewNanCity
 * @since 2.0.0
 */
fun openAdminGui(plugin: NewNanMainPlugin, player: Player) {
    plugin.openPage(InventoryType.CHEST, 54, player, LanguageKeys.Gui.Admin.TITLE) {
        // 边框填充
        borderFillComponent(0, 0, 9, 6) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 传送系统管理按钮
        slotComponent(x = 1, y = 1) {
            render {
                item(Material.COMPASS) {
                    name(LanguageKeys.Gui.Admin.TELEPORT_SYSTEM)
                }
            }

            onLeftClick { _, _, _ ->
                openTeleportGui(plugin, player, editable = true)
            }
        }

        // 称号系统管理按钮
        slotComponent(x = 2, y = 1) {
            render {
                item(Material.NAME_TAG) {
                    name(LanguageKeys.Gui.Admin.PREFIX_SYSTEM)
                }
            }

            onLeftClick { _, _, _ ->
                openGlobalPrefixNamespacesGui(plugin, player)
            }
        }

        // 牛腩书局管理按钮
        slotComponent(x = 3, y = 1) {
            render {
                item(Material.WRITTEN_BOOK) {
                    name(LanguageKeys.Gui.Admin.BOOK_SYSTEM)
                }
            }

            onLeftClick { _, _, _ ->
                player.performCommand("book admin")
                this@openPage.close()
            }
        }

        // 创造区管理按钮
        slotComponent(x = 4, y = 1) {
            render {
                item(Material.WOODEN_AXE) {
                    name(LanguageKeys.Gui.Admin.CREATE_AREA)
                }
            }

            onLeftClick { _, _, _ ->
                player.performCommand("createarea gui")
                this@openPage.close()
            }
        }

        // 铁路系统管理按钮
        slotComponent(x = 5, y = 1) {
            render {
                item(Material.RAIL) {
                    name(LanguageKeys.Gui.Admin.RAIL_SYSTEM)
                }
            }

            onLeftClick { _, _, _ ->
                player.performCommand("rail")
                this@openPage.close()
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