package city.newnan.tpa.gui

import city.newnan.gui.component.paginated.onLeftClick
import city.newnan.gui.component.paginated.onRightClick
import city.newnan.gui.component.paginated.onShiftRightClick
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dsl.*
import city.newnan.tpa.TPAPlugin
import city.newnan.tpa.i18n.LanguageKeys
import city.newnan.tpa.modules.TPADirection
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 打开在线玩家选择GUI
 *
 * @param plugin 插件实例
 * @param player 当前玩家
 * @param onPlayerSelect 玩家选择回调
 */
fun openOnlinePlayersGui(
    plugin: TPAPlugin,
    player: Player
) {
    plugin.openPage(
        InventoryType.CHEST,
        size = 54,
        player = player,
        title = LanguageKeys.Gui.PlayerList.TITLE
    ) {
        // 边框装饰
        borderFillComponent(0, 0, 9, 6) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 获取在线玩家列表（排除自己和已屏蔽的玩家）
        fun getPlayerList() = Bukkit.getOnlinePlayers()
            .filter { it.uniqueId != player.uniqueId } // 排除自己
            .filter { !plugin.getBlockManager().isBlocked(player, it) } // 排除已屏蔽的玩家
            .toList()
        var onlinePlayers = getPlayerList()

        // 分页组件显示在线玩家（前5行，45个槽位）
        val paginatedComponent = paginatedComponent(
            startX = 0, startY = 0,
            width = 9, height = 5,
            data = { onlinePlayers }
        ) {
            render { context ->
                val targetPlayer = context.item ?: return@render null
                skull(targetPlayer) {
                    name(format(LanguageKeys.Gui.PlayerList.PLAYER_ENTRY_NAME, targetPlayer.name))
                    lore(formatPlain(
                        LanguageKeys.Gui.PlayerList.PLAYER_ENTRY_LORE,
                        targetPlayer.name,
                        targetPlayer.world.name
                    ))
                }
            }

            onLeftClick { _, _, targetPlayer ->
                if (targetPlayer == null) return@onLeftClick
                plugin.getSessionManager().createSession(player, targetPlayer, TPADirection.REQUESTER_TO_TARGET)
                this@openPage.close()
            }

            onRightClick { _, _, targetPlayer ->
                if (targetPlayer == null) return@onRightClick
                plugin.getSessionManager().createSession(player, targetPlayer, TPADirection.TARGET_TO_REQUESTER)
                this@openPage.close()
            }

            onShiftRightClick { _, _, targetPlayer ->
                if (targetPlayer == null) return@onShiftRightClick
                plugin.getBlockManager().blockPlayer(player, targetPlayer)
                onlinePlayers = getPlayerList()
                this@paginatedComponent.clearCache()
                this@paginatedComponent.update()
            }
        }

        // 如果没有可用玩家，显示提示
        if (onlinePlayers.isEmpty()) {
            slotComponent(4, 2) {
                render {
                    item(Material.BARRIER) {
                        name(LanguageKeys.Gui.PlayerList.NO_PLAYERS_HINT_NAME)
                        lore(LanguageKeys.Gui.PlayerList.NO_PLAYERS_HINT_LORE)
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

        // 展示屏蔽玩家的按钮 （slot 45，第6行第1列，坐标0,5）
        slotComponent(0, 5) {
            render {
                item(Material.EMERALD) {
                    name(LanguageKeys.Gui.PlayerList.SHOW_BLOCKED_PLAYERS_NAME)
                    lore(LanguageKeys.Gui.PlayerList.SHOW_BLOCKED_PLAYERS_LORE)
                }
            }
            onLeftClick { _, _, _ ->
                openBlockListGui(plugin, player)
            }
        }
    }
}
