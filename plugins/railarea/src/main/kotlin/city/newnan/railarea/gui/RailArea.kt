package city.newnan.railarea.gui

import city.newnan.core.utils.findSafe
import city.newnan.gui.component.linefill.LineFillDirection
import city.newnan.gui.component.paginated.onLeftClick
import city.newnan.gui.component.paginated.onRightClick
import city.newnan.gui.component.paginated.onShiftLeftClick
import city.newnan.gui.component.paginated.onShiftRightClick
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dsl.*
import city.newnan.railarea.RailAreaPlugin
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.Station
import city.newnan.railarea.config.toHexString
import city.newnan.railarea.gui.input.handleAreaInput
import city.newnan.railarea.gui.input.handleYesInput
import city.newnan.railarea.i18n.LanguageKeys
import city.newnan.railarea.utils.ParticleUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 打开区域管理GUI
 *
 * @param plugin 插件实例
 * @param player 玩家
 * @param station 站点
 * @param line 线路
 */
fun openRailAreasGui(
    plugin: RailAreaPlugin,
    player: Player,
    station: Station,
    line: RailLine
) {
    val title = plugin.messager.sprintf(LanguageKeys.Gui.RailArea.TITLE, line.color.toHexString(), station.name, line.name)

    plugin.openPage(InventoryType.CHEST, 54, player, title) {
        // 底部装饰 - 黑色玻璃板
        lineFillComponent(0, 5, 9, LineFillDirection.HORIZONTAL) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 分页组件显示区域
        val paginatedComp = paginatedComponent(
            startX = 0, startY = 0,
            width = 9, height = 5,
            data = { plugin.stationStorage.getAreas(station, line) }
        ) {
            render { context ->
                val area = context.item ?: return@render null
                item(Material.RAIL) {
                    name(format(
                        LanguageKeys.Gui.RailArea.AREA_NAME,
                        station.name,
                        line.color.toHexString(),
                        if (area.reverse) line.stations.first().name else line.stations.last().name,
                        area.direction.name
                    )) // <white>{0} <color:{1}>{2}</color:{1}> 开往<gold>{3}<white>方向

                    // 计算上下一站
                    val stationIndex = line.stations.indexOf(area.station)
                    val leftStation = if (stationIndex == 0) null else line.stations[stationIndex - 1]
                    val rightStation = if (stationIndex == line.stations.size - 1) null else line.stations[stationIndex + 1]
                    val previousStation = if (area.reverse) rightStation else leftStation
                    val nextStation = if (area.reverse) leftStation else rightStation
                    val previousStationText = if (previousStation == null) {
                        formatPlain(LanguageKeys.Gui.RailArea.START_STATION) // <purple>本站为起点站</purple>
                    } else {
                        formatPlain(LanguageKeys.Gui.RailArea.PREVIOUS_STATION, previousStation.name) // <gray>上一站: <purple>{0}</purple>
                    }
                    val nextStationText = if (nextStation == null) {
                        formatPlain(LanguageKeys.Gui.RailArea.END_STATION) // <purple>本站为终点站</purple>
                    } else {
                        formatPlain(LanguageKeys.Gui.RailArea.NEXT_STATION, nextStation.name) // <gray>下一站: <purple>{0}</purple>
                    }

                    // 设置lore
                    lore(
                        formatPlain(
                            LanguageKeys.Gui.RailArea.AREA_LORE,
                            previousStationText,
                            nextStationText,
                            area.world,
                            area.range3D.minX, area.range3D.minY, area.range3D.minZ,
                            area.range3D.maxX, area.range3D.maxY, area.range3D.maxZ,
                            area.stopPoint.x, area.stopPoint.y, area.stopPoint.z,
                            area.direction.name
                        )
                    )
                }
            }

            // 左键点击 - 显示粒子效果
            onLeftClick { context, index, area ->
                if (area != null) {
                    val world = Bukkit.getWorld(area.world) ?: return@onLeftClick
                    // 显示粒子效果标记区域边界
                    ParticleUtils.showRegionBoundary(player, world, area.range3D)
                    ParticleUtils.showStopPointMarker(player, world, area.stopPoint.x, area.stopPoint.y, area.stopPoint.z)
                }
            }

            // 右键点击 - 传送到区域并显示粒子效果
            onRightClick { context, index, area ->
                if (area != null) {
                    try {
                        val world = Bukkit.getWorld(area.world) ?: return@onRightClick

                        // 传送到区域附近的安全点
                        val safeLocation = area.getStopLocation()?.findSafe(50) ?: return@onRightClick
                        player.teleport(safeLocation)

                        plugin.messager.printf(player, LanguageKeys.Gui.RailArea.TELEPORTED)

                        // 显示粒子效果标记区域边界
                        ParticleUtils.showRegionBoundary(player, world, area.range3D)
                        ParticleUtils.showStopPointMarker(player, world, area.stopPoint.x, area.stopPoint.y, area.stopPoint.z)
                    } catch (e: Exception) {
                        plugin.logger.error("Failed to teleport to area", e)
                        plugin.messager.printf(player, LanguageKeys.Gui.RailArea.TELEPORT_FAILED, e.message ?: "Unknown error")
                    }
                }
            }

            // Shift+左键 - 编辑区域
            onShiftLeftClick { context, index, area ->
                if (area != null) {
                    handleAreaInput(plugin, player, area) { newArea ->
                        if (newArea != null) {
                            try {
                                plugin.stationStorage.updateArea(newArea)
                                plugin.messager.printf(player, LanguageKeys.Gui.RailArea.AREA_UPDATED)
                                // 清除缓存
                                this@paginatedComponent.clearCache()
                            } catch (e: Exception) {
                                plugin.logger.error("Failed to update area", e)
                                plugin.messager.printf(player, LanguageKeys.Gui.RailArea.AREA_UPDATE_FAILED, e.message ?: "Unknown error")
                            }
                        }
                        // 打开GUI
                        this@openPage.show()
                    }
                }
            }

            // Shift+右键 - 删除区域
            onShiftRightClick { context, index, area ->
                if (area != null) {
                    handleYesInput(
                        plugin, player,
                        formatPlain(LanguageKeys.Gui.RailArea.DELETE_CONFIRM, area.stopPoint.x, area.stopPoint.y, area.stopPoint.z)
                    ) { yes ->
                        if (yes) {
                            try {
                                plugin.stationStorage.removeArea(area.id)
                                plugin.messager.printf(player, LanguageKeys.Gui.RailArea.AREA_DELETED)
                                // 清除缓存
                                this@paginatedComponent.clearCache()
                            } catch (e: Exception) {
                                plugin.logger.error("Failed to delete area", e)
                                plugin.messager.printf(player, LanguageKeys.Gui.RailArea.AREA_DELETE_FAILED, e.message ?: "Unknown error")
                            }
                        }
                        // 打开GUI
                        this@openPage.show()
                    }
                }
            }
        }

        // 添加区域按钮
        slotComponent(0, 5) {
            render {
                item(Material.EMERALD_BLOCK) {
                    name(LanguageKeys.Gui.RailArea.ADD_AREA)
                    lore(LanguageKeys.Gui.RailArea.ADD_AREA_LORE)
                }
            }
            onLeftClick { _, _, _ ->
                // 先选择方向
                openReverseGui(plugin, player, line) { reverse ->
                    // 创建区域
                    handleAreaInput(
                        plugin, player, null,
                        iStationId = station.id,
                        iLineId = line.id,
                        iReverse = reverse,
                    ) { area ->
                        if (area != null) {
                            try {
                                plugin.stationStorage.addArea(
                                    world = area.world,
                                    range3D = area.range3D,
                                    direction = area.direction,
                                    stopPoint = area.stopPoint,
                                    stationId = area.station.id,
                                    lineId = area.line.id,
                                    reverse = area.reverse
                                )
                                plugin.messager.printf(player, LanguageKeys.Gui.RailArea.AREA_ADDED)
                                // 清除缓存
                                paginatedComp.clearCache()
                            } catch (e: Exception) {
                                plugin.logger.error("Failed to add area", e)
                                plugin.messager.printf(player, LanguageKeys.Gui.RailArea.AREA_ADD_FAILED, e.message ?: "Unknown error")
                            }
                        }
                        // 打开GUI
                        this@openPage.show()
                    }
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
                paginatedComp.previousPage()
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
                paginatedComp.nextPage()
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
                // 返回到线路详情页面
                back()
            }
        }
    }
}
