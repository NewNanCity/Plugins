package city.newnan.externalbook.gui.authorlist

import city.newnan.externalbook.ExternalBookPlugin
import city.newnan.externalbook.i18n.LanguageKeys
import city.newnan.gui.component.paginated.onLeftClick
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dsl.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 打开作者列表GUI
 *
 * @param plugin 插件实例
 * @param player 当前玩家
 * @param onPlayerSelected 玩家选择回调
 */
fun openAuthorListGui(
    plugin: ExternalBookPlugin,
    player: Player,
    onPlayerSelected: (OfflinePlayer) -> Unit
) {
    plugin.openPage(
        InventoryType.CHEST,
        size = 54,
        player = player,
        title = LanguageKeys.Gui.OnlinePlayers.TITLE
    ) {
        // 边框
        borderFillComponent(0, 0, 9, 6) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        val paginatedComponent = paginatedComponent(
            startX = 0, startY = 0,
            width = 9, height = 5,
            data = {
                (plugin.librarian?.getAuthors() ?: emptySet()).map { uuid ->
                    Bukkit.getOfflinePlayer(uuid)
                }
            }
        ) {
            render { context ->
                val targetPlayer = context.item ?: return@render null
                skull(targetPlayer) {
                    name(targetPlayer.name ?: LanguageKeys.Gui.Common.UNKNOWN_PLAYER)
                }
            }

            onLeftClick { context, index, targetPlayer ->
                if (targetPlayer == null) return@onLeftClick
                onPlayerSelected(targetPlayer)
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
