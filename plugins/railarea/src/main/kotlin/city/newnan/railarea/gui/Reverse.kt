package city.newnan.railarea.gui

import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dsl.*
import city.newnan.railarea.RailAreaPlugin
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.i18n.LanguageKeys
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 方向选择GUI
 *
 * 重新实现旧版的方向选择页面
 */

/**
 * 打开方向选择GUI
 *
 * @param plugin 插件实例
 * @param player 玩家
 * @param line 线路
 * @param setReverse 选择方向的回调函数
 */
fun openReverseGui(
    plugin: RailAreaPlugin,
    player: Player,
    line: RailLine,
    setReverse: (Boolean) -> Unit
) {
    plugin.openPage(
        InventoryType.CHEST,
        size = 54,
        player = player,
        title = LanguageKeys.Gui.Reverse.TITLE
    ) {
        // 开往终点站方向 (位置: 1,1) - 按照旧版布局
        slotComponent(1, 1) {
            render {
                item(Material.NETHER_STAR) {
                    name(plugin.messager.sprintfPlain(
                        LanguageKeys.Gui.Reverse.SELECTION_NAME, // 开往 <gold>{0}</gold> 方向
                        line.stations.last().name
                    ))
                }
            }
            onLeftClick { _, _, _ ->
                setReverse(false)
                back()
            }
        }

        // 开往起点站方向 (位置: 1,2) - 按照旧版布局
        slotComponent(1, 2) {
            render {
                item(Material.NETHER_STAR) {
                    name(plugin.messager.sprintfPlain(
                        LanguageKeys.Gui.Reverse.SELECTION_NAME, // 开往 <gold>{0}</gold> 方向
                        line.stations.first().name
                    ))
                }
            }
            onLeftClick { _, _, _ ->
                setReverse(true)
                back()
            }
        }

        // 返回按钮 (位置: 8,5) - 对应旧版的(6,9)位置
        slotComponent(8, 5) {
            render {
                item(Material.BARRIER) {
                    name(LanguageKeys.Gui.Common.BACK)
                }
            }
            onLeftClick { _, _, _ ->
                back()
            }
        }
    }
}
