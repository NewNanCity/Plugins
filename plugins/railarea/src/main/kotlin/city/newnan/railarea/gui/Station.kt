package city.newnan.railarea.gui

import city.newnan.gui.component.LineFillDirection
import city.newnan.gui.component.paginated.onLeftClick
import city.newnan.gui.component.paginated.onShiftLeftClick
import city.newnan.gui.component.paginated.onShiftRightClick
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dsl.*
import city.newnan.railarea.RailAreaPlugin
import city.newnan.railarea.config.Station
import city.newnan.railarea.config.toHexString
import city.newnan.railarea.gui.input.handleStationInput
import city.newnan.railarea.gui.input.handleYesInput
import city.newnan.railarea.i18n.LanguageKeys
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 站点管理GUI
 *
 * 重新实现旧版的站点管理页面，显示所有可用站点
 */

/**
 * 打开站点管理GUI
 *
 * @param plugin 插件实例
 * @param player 玩家
 * @param excludeStations 要排除的站点集合(已在线路中的站点)
 * @param setStation 选择站点的回调函数
 */
fun openStationGui(
    plugin: RailAreaPlugin,
    player: Player,
    excludeStations: Set<Station> = emptySet(),
    setStation: (Int) -> Unit
) {
    val title = LanguageKeys.Gui.Station.TITLE

    plugin.openPage(InventoryType.CHEST, 54, player, title) {
        // 底部装饰 - 黑色玻璃板
        lineFillComponent(0, 5, 9, LineFillDirection.HORIZONTAL) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 分页组件显示站点
        val paginatedComponent = paginatedComponent(
            startX = 0, startY = 0,
            width = 9, height = 5,
            // 获取所有站点，排除已在线路中的站点
            data = { plugin.stationStorage.getAllStations().filter { it !in excludeStations } }
        ) {
            render { context ->
                val station = context.item ?: return@render null
                item(Material.ACTIVATOR_RAIL) {
                    name(station.name)

                    val baseLore = formatPlain(
                        LanguageKeys.Gui.Station.STATION_LORE,
                        station.id,
                        station.lines.size
                    )

                    val deleteAction = formatPlain(if (station.lines.isEmpty()) {
                        LanguageKeys.Gui.Station.STATION_DELETE_OPERATION
                    } else {
                        LanguageKeys.Gui.Station.STATION_DELETE_OPERATION_IN_USE
                    })

                    val operationsLore = formatPlain(
                        LanguageKeys.Gui.Station.STATION_OPERATIONS,
                        deleteAction
                    )

                    lore("$baseLore$operationsLore")
                }
            }

            // 普通点击 - 选择站点
            onLeftClick { context, index, station ->
                if (station != null) {
                    setStation(station.id)
                }
            }

            // Shift+左键 - 编辑站点名称
            onShiftLeftClick { context, index, station ->
                if (station != null) {
                    handleStationInput(plugin, player, station) { newStation ->
                        if (newStation != null) {
                            plugin.messager.printf(player, LanguageKeys.Gui.Station.STATION_UPDATED, newStation.name)
                            plugin.stationStorage.updateStation(newStation)
                            // 清除缓存
                            this@paginatedComponent.clearCache()
                        }
                        // 重新打开GUI
                        this@openPage.show()
                    }
                }
            }

            // Shift+右键 - 删除站点 (仅当站点未被任何线路使用时)
            onShiftRightClick { context, index, station ->
                if (station != null) {
                    if (station.lines.isNotEmpty()) {
                        plugin.messager.printf(
                            player,
                            LanguageKeys.Gui.Station.STATION_IN_USE,
                            station.name,
                            station.lines.joinToString("<gray>,</gray>") { "<color:${it.color.toHexString()}><bold>${it.name}</bold></color:${it.color.toHexString()}>" }
                        )
                    } else {
                        val confirmMessage = plugin.messager.sprintfPlain(
                            LanguageKeys.Gui.Station.DELETE_CONFIRM,
                            station.name
                        )
                        handleYesInput(plugin, player, confirmMessage) { yes ->
                            if (yes) {
                                try {
                                    plugin.stationStorage.removeStation(station.id)
                                    plugin.messager.printf(player, LanguageKeys.Gui.Station.STATION_DELETED, station.name)
                                    // 清除缓存
                                    this@paginatedComponent.clearCache()
                                } catch (e: Exception) {
                                    plugin.logger.error("Failed to delete station", e)
                                    plugin.messager.printf(player, LanguageKeys.Input.Station.SAVE_FAILED, e.message ?: "Unknown error")
                                }
                            }
                            // 重新打开GUI
                            this@openPage.show()
                        }
                    }
                }
            }
        }

        // 创建站点按钮
        slotComponent(0, 5) {
            render {
                urlSkull("9a2d891c6ae9f6baa040d736ab84d48344bb6b70d7f1a280dd12cbac4d777") {
                    name(LanguageKeys.Gui.Station.CREATE_STATION)
                    lore(LanguageKeys.Gui.Station.CREATE_STATION_LORE)
                }
            }
            onLeftClick { _, _, _ ->
                handleStationInput(plugin, player, null) { station ->
                    if (station != null) {
                        plugin.messager.printf(player, LanguageKeys.Gui.Station.STATION_CREATED, station.name)
                        plugin.stationStorage.addStation(station.name)
                        // 清除缓存
                        paginatedComponent.clearCache()
                    }
                    // 重新打开GUI
                    this@openPage.show()
                }
            }
        }

        // 导航按钮
        // 上一页按钮 - 位置 (2,5)
        slotComponent(2, 5) {
            render {
                urlSkull("bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9") {
                    name(LanguageKeys.Gui.Common.PREVIOUS_PAGE)
                }
            }
            onLeftClick { _, _, _ ->
                paginatedComponent.previousPage()
            }
        }

        // 下一页按钮 - 位置 (6,5)
        slotComponent(6, 5) {
            render {
                urlSkull("19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf") {
                    name(LanguageKeys.Gui.Common.NEXT_PAGE)
                }
            }
            onLeftClick { _, _, _ ->
                paginatedComponent.nextPage()
            }
        }

        // 返回按钮 - 位置 (8,5)
        slotComponent(8, 5) {
            render {
                item(Material.BARRIER) {
                    name(LanguageKeys.Gui.Common.BACK)
                }
            }
            onLeftClick { _, _, _ ->
                // 关闭GUI，返回到调用者
                back()
            }
        }
    }
}
