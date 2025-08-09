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
import city.newnan.railarea.gui.input.handleAreaInput
import city.newnan.railarea.gui.input.handleYesInput
import city.newnan.railarea.i18n.LanguageKeys
import city.newnan.railarea.utils.ParticleUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 游离区域GUI
 *
 * 重新实现旧版的游离区域页面，显示所有未分配到具体站点-线路的区域
 */

/**
 * 打开游离区域GUI
 *
 * @param plugin 插件实例
 * @param player 玩家
 */
fun openFreeAreaGui(
    plugin: RailAreaPlugin,
    player: Player
) {
    plugin.openPage(InventoryType.CHEST, 54, player, LanguageKeys.Gui.FreeArea.TITLE) {
        // 底部装饰 - 黑色玻璃板
        lineFillComponent(0, 5, 9, LineFillDirection.HORIZONTAL) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 分页组件显示游离区域
        val paginatedComponent = paginatedComponent(
            startX = 0, startY = 0,
            width = 9, height = 5,
            data = { plugin.stationStorage.getAreas(plugin.stationStorage.unknownStation, plugin.stationStorage.unknownLine) }
        ) {
            render { context ->
                val item = context.item ?: return@render null
                item(Material.RAIL) {
                    name("")
                    lore(formatPlain(
                        LanguageKeys.Gui.FreeArea.AREA_LORE,
                        item.world,
                        item.range3D.minX, item.range3D.minY, item.range3D.minZ,
                        item.range3D.maxX, item.range3D.maxY, item.range3D.maxZ,
                        item.stopPoint.x, item.stopPoint.y, item.stopPoint.z,
                        item.direction.name
                    ))
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

            // 右键 - 传送到区域并显示粒子效果
            onRightClick { context, index, area ->
                if (area != null) {
                    try {
                        val world = Bukkit.getWorld(area.world) ?: return@onRightClick

                        // 传送到区域附近的安全点
                        val safeLocation = area.getStopLocation()?.findSafe(50) ?: return@onRightClick
                        player.teleport(safeLocation)

                        plugin.messager.printf(player, LanguageKeys.Gui.FreeArea.TELEPORTED)

                        // 显示粒子效果标记区域边界
                        ParticleUtils.showRegionBoundary(player, world, area.range3D)
                        ParticleUtils.showStopPointMarker(player, world, area.stopPoint.x, area.stopPoint.y, area.stopPoint.z)
                    } catch (e: Exception) {
                        plugin.logger.error("Failed to teleport to free area", e)
                        plugin.messager.printf(player, LanguageKeys.Gui.FreeArea.TELEPORT_FAILED, e.message ?: "Unknown error")
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
                                plugin.messager.printf(player, LanguageKeys.Gui.FreeArea.AREA_UPDATED)
                                // 清空缓存
                                this@paginatedComponent.clearCache()
                            } catch (e: Exception) {
                                plugin.logger.error("Failed to update free area", e)
                                plugin.messager.printf(player, LanguageKeys.Gui.FreeArea.UPDATE_FAILED, e.message ?: "Unknown error")
                            }
                        }
                        // 打开界面
                        this@openPage.show()
                    }
                }
            }

            // Shift+右键 - 删除区域
            onShiftRightClick { context, index, area ->
                if (area != null) {
                    handleYesInput(
                        plugin, player,
                        formatPlain(LanguageKeys.Gui.FreeArea.DELETE_CONFIRM)
                    ) { yes ->
                        if (yes) {
                            try {
                                plugin.stationStorage.removeArea(area.id)
                                plugin.messager.printf(player, LanguageKeys.Gui.FreeArea.AREA_DELETED)
                                // 清空缓存
                                this@paginatedComponent.clearCache()
                            } catch (e: Exception) {
                                plugin.logger.error("Failed to delete free area", e)
                                plugin.messager.printf(player, LanguageKeys.Gui.FreeArea.DELETE_FAILED, e.message ?: "Unknown error")
                            }
                        }
                        // 打开界面
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
                // 关闭GUI，返回到主菜单或调用者
                back()
            }
        }
    }
}
