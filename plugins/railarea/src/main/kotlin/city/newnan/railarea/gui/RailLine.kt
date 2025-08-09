package city.newnan.railarea.gui

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
import city.newnan.railarea.gui.input.handleYesInput
import city.newnan.railarea.i18n.LanguageKeys
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 打开线路的站点详情GUI，有两个用途：
 * 1. 线路站点编辑器 (editable=true)
 * 2. 站点选择器 (editable=false) 供其他GUI选择站点时使用
 *
 * @param plugin 插件实例
 * @param player 玩家
 * @param editable 是否可编辑模式
 * @param line 要显示的线路
 * @param onStationSelect 选择站点的回调函数
 */
fun openRailLineGui(
    plugin: RailAreaPlugin,
    player: Player,
    editable: Boolean,
    line: RailLine,
    onStationSelect: (stationId: Int) -> Unit = {}
) {
    val title = if (editable) {
        plugin.messager.sprintfPlain(
            LanguageKeys.Gui.RailLine.TITLE_EDIT,
            line.color.toHexString(),
            line.name
        )
    } else {
        LanguageKeys.Gui.RailLine.TITLE_SELECT
    }

    plugin.openPage(InventoryType.CHEST, 54, player, title) {
        // 底部装饰 - 黑色玻璃板
        lineFillComponent(0, 5, 9, LineFillDirection.HORIZONTAL) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 分页组件显示内容
        val paginatedComponent = paginatedComponent(
            startX = 0, startY = 0,
            width = 9, height = 5,
            data = {
                if (!editable || line.isCycle) {
                    line.stations
                } else {
                    // 添加折返设置 (仅非环线且编辑模式显示)
                    listOf("leftReturn") + line.stations + listOf("rightReturn")
                }
            }
        ) {
            render { context ->
                val item = context.item ?: return@render null

                when (item) {
                    "leftReturn" -> {
                        item(if (line.leftReturn) Material.GREEN_CONCRETE else Material.RED_CONCRETE) {
                            name(if (line.leftReturn) LanguageKeys.Gui.RailLine.LEFT_RETURN_YES else LanguageKeys.Gui.RailLine.LEFT_RETURN_NO)
                            lore(LanguageKeys.Gui.RailLine.RETURN_DESCRIPTION)
                        }
                    }
                    "rightReturn" -> {
                        item(if (line.rightReturn) Material.GREEN_CONCRETE else Material.RED_CONCRETE) {
                            name(if (line.rightReturn) LanguageKeys.Gui.RailLine.RIGHT_RETURN_YES else LanguageKeys.Gui.RailLine.RIGHT_RETURN_NO)
                            lore(LanguageKeys.Gui.RailLine.RETURN_DESCRIPTION)
                        }
                    }
                    is Station -> {
                        item(Material.ACTIVATOR_RAIL) {
                            name(format(LanguageKeys.Gui.RailLine.STATION_NAME, item.name)) // <yellow>{0}

                            val baseLore = formatPlain(
                                LanguageKeys.Gui.RailLine.STATION_LORE,
                                item.id
                            )

                            val operationsLore = formatPlain(if (editable) {
                                LanguageKeys.Gui.RailLine.STATION_OPERATIONS_EDIT
                            } else {
                                LanguageKeys.Gui.RailLine.STATION_OPERATIONS_SELECT
                            })

                            val shiftLeft = if (editable && ((line.isCycle && context.index > 0) || (!line.isCycle && context.index > 1))) {
                                LanguageKeys.Gui.RailLine.STATION_OPERATIONS_SHIFT_LEFT // \n<green>Shift+左键: 向左移动站点
                            } else {
                                ""
                            }

                            val shiftRight = if (editable && ((line.isCycle && context.index < line.stations.size - 1) || (!line.isCycle && context.index < line.stations.size - 2))) {
                                LanguageKeys.Gui.RailLine.STATION_OPERATIONS_SHIFT_RIGHT // \n<green>Shift+右键: 向右移动站点
                            } else {
                                ""
                            }

                            lore("$baseLore$operationsLore$shiftLeft$shiftRight")
                        }
                    }

                    else -> null // 不会发生
                }
            }

            // 点击处理
            onLeftClick { context, index, item ->
                when (item) {
                    "leftReturn" -> {
                        if (!editable) return@onLeftClick
                        try {
                            plugin.stationStorage.updateLine(line.copy(leftReturn = !line.leftReturn))
                            plugin.messager.printf(player, LanguageKeys.Gui.RailLine.LEFT_RETURN_UPDATED)
                            // 清空缓存
                            this@paginatedComponent.clearCache()
                        } catch (e: Exception) {
                            plugin.logger.error("Failed to update line", e)
                            plugin.messager.printf(player, LanguageKeys.Input.RailLine.SAVE_FAILED, e.message ?: "Unknown error")
                        }
                        this@paginatedComponent.update()
                    }
                    "rightReturn" -> {
                        if (!editable) return@onLeftClick
                        try {
                            plugin.stationStorage.updateLine(line.copy(rightReturn = !line.rightReturn))
                            plugin.messager.printf(player, LanguageKeys.Gui.RailLine.RIGHT_RETURN_UPDATED)
                            // 清空缓存
                            this@paginatedComponent.clearCache()
                        } catch (e: Exception) {
                            plugin.logger.error("Failed to update line", e)
                            plugin.messager.printf(player, LanguageKeys.Input.RailLine.SAVE_FAILED, e.message ?: "Unknown error")
                        }
                        this@paginatedComponent.update()
                    }
                    is Station -> {
                        if (editable) {
                            // 编辑模式：进入区域管理页面
                            openRailAreasGui(plugin, player, item, line)
                        } else {
                            // 选择模式：选择站点
                            onStationSelect(item.id)
                        }
                    }
                }
            }

            if (editable) {
                // 右键 - 移除站点
                onRightClick { context, index, item ->
                    if (item is Station) {
                        val confirmMessage = formatPlain(
                            LanguageKeys.Gui.RailLine.REMOVE_STATION_CONFIRM,
                            item.name
                        )
                        handleYesInput(plugin, player, confirmMessage) { yes ->
                            if (yes) {
                                try {
                                    plugin.stationStorage.updateLine(line.copy(stationIds = line.stationIds.filter { it != item.id }))
                                    plugin.messager.printf(player, LanguageKeys.Gui.RailLine.STATION_REMOVED, item.name)
                                    // 清空缓存
                                    this@paginatedComponent.clearCache()
                                } catch (e: Exception) {
                                    plugin.logger.error("Failed to remove station from line", e)
                                    plugin.messager.printf(player, LanguageKeys.Input.Station.SAVE_FAILED, e.message ?: "Unknown error")
                                }
                            }
                            // 重新渲染
                            this@paginatedComponent.update()
                        }
                    }
                }

                // Shift+左键 - 向左移动站点
                onShiftLeftClick { context, index, item ->
                    if (item is Station && ((line.isCycle && index > 0) || (!line.isCycle && index > 1))) {
                        try {
                            plugin.stationStorage.updateLine(line.copy(
                                stationIds = line.stationIds.toMutableList().apply {
                                    remove(item.id)
                                    add(index - 1, item.id)
                                }
                            ))
                            // 清空缓存
                            this@paginatedComponent.clearCache()
                            // 重新渲染
                            this@paginatedComponent.update()
                        } catch (e: Exception) {
                            plugin.logger.error("Failed to shift station left in line", e)
                            plugin.messager.printf(player, LanguageKeys.Input.Station.SAVE_FAILED, e.message ?: "Unknown error")
                        }
                    }
                }

                // Shift+右键 - 向右移动站点
                onShiftRightClick { context, index, item ->
                    if (item is Station && ((line.isCycle && index < line.stations.size - 1) || (!line.isCycle && index < line.stations.size - 2))) {
                        try {
                            plugin.stationStorage.updateLine(line.copy(
                                stationIds = line.stationIds.toMutableList().apply {
                                    remove(item.id)
                                    add(index + 1, item.id)
                                }
                            ))
                            // 清空缓存
                            this@paginatedComponent.clearCache()
                            // 重新渲染
                            this@paginatedComponent.update()
                        } catch (e: Exception) {
                            plugin.logger.error("Failed to shift station right in line", e)
                            plugin.messager.printf(player, LanguageKeys.Input.Station.SAVE_FAILED, e.message ?: "Unknown error")
                        }
                    }
                }
            }
        }

        // 添加站点按钮 (仅编辑模式)
        if (editable) {
            slotComponent(0, 5) {
                render {
                    urlSkull("cf7cdeefc6d37fecab676c584bf620832aaac85375e9fcbff27372492d69f") {
                        name(LanguageKeys.Gui.RailLine.ADD_STATION)
                        lore(LanguageKeys.Gui.RailLine.ADD_STATION_LORE)
                    }
                }
                onLeftClick { _, _, _ ->
                    openStationGui(plugin, player, line.stations.toSet()) { stationId ->
                        try {
                            plugin.stationStorage.updateLine(line.copy(stationIds = line.stationIds.toMutableList().apply {
                                add(stationId)
                            }.toList()))
                            plugin.messager.printf(player, LanguageKeys.Gui.RailLine.STATION_ADDED, plugin.stationStorage.getStationById(stationId)!!.name)
                            // 清空缓存
                            paginatedComponent.clearCache()
                        } catch (e: Exception) {
                            plugin.logger.error("Failed to add station to line", e)
                            plugin.messager.printf(player, LanguageKeys.Input.Station.SAVE_FAILED, e.message ?: "Unknown error")
                        }
                        // 重新渲染
                        paginatedComponent.update()
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
                // 返回到线路列表页面
                back()
            }
        }
    }
}
