package city.newnan.createarea.gui

import city.newnan.createarea.CreateAreaPlugin
import city.newnan.createarea.i18n.LanguageKeys
import city.newnan.gui.dsl.openPage
import city.newnan.core.utils.getSkull
import city.newnan.gui.component.paginated.onLeftClick
import city.newnan.gui.component.paginated.onRightClick
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.component.singleslot.onRightClick
import city.newnan.gui.dsl.borderFillComponent
import city.newnan.gui.dsl.item
import city.newnan.gui.dsl.paginatedComponent
import city.newnan.gui.dsl.slotComponent
import city.newnan.gui.dsl.urlSkull
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 创造区域列表GUI
 *
 * 显示所有创造区域的管理界面
 *
 * @author NewNanCity
 * @since 2.0.0
 */
fun openCreateAreasGui(player: Player, plugin: CreateAreaPlugin) {
    val manager = plugin.getCreateAreaManager()

    plugin.openPage(InventoryType.CHEST, 54, player, LanguageKeys.Gui.AreaList.TITLE) {
        borderFillComponent(0, 0, 9, 6) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        val paginatedComponent = paginatedComponent(1, 1, 7, 4, { manager.getAllAreas().entries.toList() }) {
            render { context ->
                val (areaPlayer, area) = context.item ?: return@render null
                item(player.getSkull(1)) {
                    name("<gold>${areaPlayer.name}的创造区</gold>")
                    lore("§7区域范围:")
                    lore("§7  X: §e${area.minX} §7~ §e${area.maxX}")
                    lore("§7  Z: §e${area.minZ} §7~ §e${area.maxZ}")
                    lore("§7大小: §e${area.maxX - area.minX + 1} §7x §e${area.maxZ - area.minZ + 1}")
                    lore("")
                    lore("§e左键点击传送到该区域")
                    if (areaPlayer == player || player.hasPermission("createarea.delete.other")) {
                        lore("§c右键点击删除该区域")
                    }
                }
            }
            onLeftClick { _, _, entry ->
                val (areaPlayer, area) = entry ?: return@onLeftClick
                if (areaPlayer == player || player.hasPermission("createarea.tp.other")) {
                    if (manager.teleportToArea(player, area)) {
                        plugin.messager.printf(player, LanguageKeys.Commands.Tp.SUCCESS_OTHER, areaPlayer.name)
                        this@openPage.close()
                    } else {
                        plugin.messager.printf(player, LanguageKeys.Core.Error.OPERATION_FAILED, "传送失败")
                    }
                } else {
                    plugin.messager.printf(player, LanguageKeys.Core.Error.NO_PERMISSION)
                }
            }

            onRightClick { _, _, entry ->
                val (areaPlayer, _) = entry ?: return@onRightClick
                if (areaPlayer == player || player.hasPermission("createarea.delete.other")) {
                    if (manager.deleteArea(areaPlayer)) {
                        plugin.messager.printf(player, LanguageKeys.Commands.Delete.SUCCESS_OTHER, areaPlayer.name)
                        // 刷新GUI
                        this@paginatedComponent.clearCache()
                        this@paginatedComponent.update()
                    } else {
                        plugin.messager.printf(player, LanguageKeys.Core.Error.OPERATION_FAILED, "删除失败")
                    }
                } else {
                    plugin.messager.printf(player, LanguageKeys.Core.Error.NO_PERMISSION)
                }
            }
        }

        // 上一页按钮（slot 47，第6行第3列，坐标2,5）
        slotComponent(2, 5) {
            render {
                urlSkull("37aee9a75bf0df7897183015cca0b2a7d755c63388ff01752d5f4419fc645") {
                    name(LanguageKeys.Gui.Common.PREVIOUS_PAGE)
                }
            }
            onLeftClick { _, _, _ ->
                paginatedComponent.previousPage()
            }
        }

        // 下一页按钮（slot 51，第6行第7列，坐标6,5）
        slotComponent(6, 5) {
            render {
                urlSkull("682ad1b9cb4dd21259c0d75aa315ff389c3cef752be3949338164bac84a96e") {
                    name(LanguageKeys.Gui.Common.NEXT_PAGE)
                }
            }
            onLeftClick { _, _, _ ->
                paginatedComponent.nextPage()
            }
        }

        // 返回按钮（slot 53，第6行第9列，坐标8,5）
        slotComponent(8, 5) {
            render {
                item(Material.BARRIER) {
                    name(LanguageKeys.Gui.Common.BACK)
                }
            }
            onLeftClick { _, _, _ ->
                this@openPage.close()
            }
        }
    }
}
