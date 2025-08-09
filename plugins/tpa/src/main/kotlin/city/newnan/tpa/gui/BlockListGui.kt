package city.newnan.tpa.gui

import city.newnan.core.utils.text.ComponentParseMode
import city.newnan.core.utils.text.toComponent
import city.newnan.gui.component.paginated.onLeftClick
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dsl.*
import city.newnan.tpa.TPAPlugin
import city.newnan.tpa.i18n.LanguageKeys
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 打开屏蔽列表管理GUI
 *
 * @param plugin 插件实例
 * @param player 当前玩家
 */
fun openBlockListGui(
    plugin: TPAPlugin,
    player: Player
) {
    plugin.openPage(
        InventoryType.CHEST,
        size = 54,
        player = player,
        title = LanguageKeys.Gui.BlockList.TITLE
    ) {
        // 边框装饰
        borderFillComponent(0, 0, 9, 6) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 获取屏蔽的玩家列表
        fun getBlockedPlayers() =
            plugin.getBlockManager().getBlockedPlayers(player).map { Bukkit.getOfflinePlayer(it) }
        var blockedPlayers = getBlockedPlayers()

        // 分页组件显示屏蔽的玩家（前5行，45个槽位）
        val paginatedComponent = paginatedComponent(
            startX = 0, startY = 0,
            width = 9, height = 5,
            data = { blockedPlayers }
        ) {
            render { context ->
                val offlinePlayer = context.item ?: return@render null
                skull(offlinePlayer) {
                    name((offlinePlayer.name ?: "?").toComponent(ComponentParseMode.Plain))
                    lore(LanguageKeys.Gui.BlockList.UNBLOCK_LORE)
                }
            }

            onLeftClick { context, index, offlinePlayer ->
                // 取消屏蔽玩家
                plugin.getBlockManager().unblockPlayer(player, offlinePlayer!!)

                // 刷新页面
                blockedPlayers = getBlockedPlayers()
                this@paginatedComponent.clearCache()
                this@paginatedComponent.update()
            }
        }

        // 如果没有屏蔽的玩家，显示提示
        if (blockedPlayers.isEmpty()) {
            slotComponent(4, 2) {
                render {
                    item(Material.BARRIER) {
                        name(LanguageKeys.Gui.BlockList.EMPTY_LIST_NAME) // <gray>没有屏蔽任何玩家
                    }
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
