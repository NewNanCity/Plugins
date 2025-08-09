package city.newnan.railarea.gui

import city.newnan.gui.component.linefill.LineFillDirection
import city.newnan.gui.component.paginated.onLeftClick
import city.newnan.gui.component.paginated.onRightClick
import city.newnan.gui.component.paginated.onShiftLeftClick
import city.newnan.gui.component.paginated.onShiftRightClick
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dsl.*
import city.newnan.railarea.RailAreaPlugin
import city.newnan.railarea.config.toHexString
import city.newnan.railarea.config.toMaterial
import city.newnan.railarea.gui.input.handleRailLineInput
import city.newnan.railarea.gui.input.handleYesInput
import city.newnan.railarea.i18n.LanguageKeys
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 线路列表GUI，有两个用途：
 * 1. 所有线路的编辑器 (editable=true) /rail 指令的GUI入口
 * 2. 线路选择器 (editable=false) 供其他GUI选择线路时使用
 *
 * @param plugin 插件实例
 * @param player 当前玩家
 * @param editable 是否可编辑模式，false则退化为线路选择器
 * @param onLineSelect 选择线路的回调函数，编辑器模式下不触发
 */
fun openRailLinesGui(
    plugin: RailAreaPlugin,
    player: Player,
    editable: Boolean,
    onLineSelect: (Int) -> Unit = {}
) {
    val title = if (editable) {
        LanguageKeys.Gui.RailLines.TITLE_EDIT
    } else {
        LanguageKeys.Gui.RailLines.TITLE_SELECT
    }

    plugin.openPage(InventoryType.CHEST, 54, player, title) {
        // 底部装饰 - 黑色玻璃板
        lineFillComponent(0, 5, 9, LineFillDirection.HORIZONTAL) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 分页组件显示线路 (前5行，45个槽位)
        val paginatedComponent = paginatedComponent(
            startX = 0, startY = 0,
            width = 9, height = 5,
            data = { plugin.stationStorage.getAllLines() }
        ) {
            render { context ->
                val line = context.item ?: return@render null

                // 使用线路对应颜色的混凝土材料
                item(line.colorMaterial) {
                    name(format(LanguageKeys.Gui.RailLines.LINE_NAME, line.color.toHexString(), line.name)) // <color:{0}>{1}

                    val baseLore = formatPlain(
                        LanguageKeys.Gui.RailLines.LINE_LORE,
                        line.id,
                        line.stations.size,
                        formatPlain(if (line.isCycle) LanguageKeys.Gui.Common.CYCLE_LINE else LanguageKeys.Gui.Common.NO_CYCLE_LINE) // "环线" else "往返线"
                    )

                    val operationsLore = if (editable) {
                        formatPlain(LanguageKeys.Gui.RailLines.LINE_OPERATIONS_EDIT)
                    } else {
                        formatPlain(LanguageKeys.Gui.RailLines.LINE_OPERATIONS_SELECT)
                    }

                    lore("$baseLore$operationsLore")
                }
            }

            // 普通点击 - 选择线路进入下一级
            onLeftClick { context, index, line ->
                if (line != null) {
                    if (editable) {
                        // 编辑模式：进入线路详情页面
                        openRailLineGui(plugin, player, true, line)
                    } else {
                        // 选择模式：直接选择线路
                        onLineSelect(line.id)
                    }
                }
            }

            // 右键 - 切换环线/往返线模式（仅编辑模式）
            if (editable) {
                onRightClick { context, index, line ->
                    if (line != null) {
                        plugin.stationStorage.updateLine(line.copy(isCycle = !line.isCycle))
                        plugin.messager.printf(player, LanguageKeys.Gui.RailLines.LINE_UPDATED, line.color.toHexString(), line.name)
                        // 刷新缓存
                        this@paginatedComponent.clearCache()
                        this@paginatedComponent.update()
                    }
                }
            }

            // Shift+左键 - 编辑线路（仅编辑模式）
            if (editable) {
                onShiftLeftClick { context, index, line ->
                    if (line != null) {
                        handleRailLineInput(plugin, player, line) { updatedLine ->
                            if (updatedLine != null) {
                                plugin.stationStorage.updateLine(updatedLine)
                                plugin.messager.printf(
                                    player,
                                    LanguageKeys.Gui.RailLines.LINE_UPDATED,
                                    updatedLine.color.toHexString(), updatedLine.name
                                )
                                // 刷新缓存
                                this@paginatedComponent.clearCache()
                            }
                            // 重新打开GUI
                            this@openPage.show()
                        }
                    }
                }
            }

            // Shift+右键 - 删除线路（仅编辑模式）
            if (editable) {
                onShiftRightClick { context, index, line ->
                    if (line != null) {
                        plugin.messager.printf(
                            player,
                            LanguageKeys.Gui.RailLines.DELETE_CONFIRM,
                            line.color.toHexString(), line.name
                        )

                        val confirmMessage = formatPlain(
                            LanguageKeys.Gui.RailLines.DELETE_CONFIRM,
                            line.color.toHexString(), line.name
                        )
                        handleYesInput(plugin, player, confirmMessage) { confirmed ->
                            if (confirmed) {
                                try {
                                    plugin.stationStorage.removeLine(line.id)
                                    plugin.messager.printf(player, LanguageKeys.Gui.RailLines.LINE_DELETED, line.name)
                                    // 刷新缓存
                                    this@paginatedComponent.clearCache()
                                } catch (e: Exception) {
                                    plugin.logger.error("Failed to delete line", e)
                                    plugin.messager.printf(player, LanguageKeys.Input.RailLine.SAVE_FAILED, e.message ?: "Unknown error")
                                }
                            }
                            // 重新打开GUI
                            this@openPage.show()
                        }
                    }
                }
            }
        }

        // 添加线路按钮 (仅编辑模式)
        if (editable) {
            slotComponent(0, 5) {
                render {
                    urlSkull("9a2d891c6ae9f6baa040d736ab84d48344bb6b70d7f1a280dd12cbac4d777") {
                        name(LanguageKeys.Gui.RailLines.ADD_LINE)
                    }
                }
                onLeftClick { _, _, _ ->
                    handleRailLineInput(plugin, player, null) { line ->
                        if (line != null) {
                            plugin.stationStorage.addLine(
                                name = line.name,
                                color = line.color,
                                isCycle = false,
                                colorMaterial = line.color.toMaterial()
                            )
                            plugin.messager.printf(
                                player,
                                LanguageKeys.Gui.RailLines.LINE_ADDED,
                                line.color.toHexString(), line.name
                            )
                            // 刷新缓存
                            paginatedComponent.clearCache()
                        }
                        // 重新打开GUI
                        this@openPage.show()
                    }
                }
            }

            // 游离区域入口按钮 (仅编辑模式且存在游离区域时)
            val freeAreas = plugin.stationStorage.getAreas(plugin.stationStorage.unknownStation, plugin.stationStorage.unknownLine)
            if (freeAreas.isNotEmpty()) {
                slotComponent(5, 5) {
                    render {
                        urlSkull("b0253144515d14243cd176cf7a9c4ff1af49010e5716ca88a7adfd13d9bee165") {
                            name(LanguageKeys.Gui.RailLines.FREE_AREA_MANAGEMENT) // <yellow>游离区域</yellow>
                            // 多行，使用 |
                            // <gray>这些区域由于其所在的线路或站点被删除,</gray>
                            // <gray>处于游离状态, 等待分配</gray>
                            //
                            // <green>左键: 打开游离区域管理</green>
                            lore(LanguageKeys.Gui.RailLines.FREE_AREA_MANAGEMENT_LORE)
                        }
                    }
                    onLeftClick { _, _, _ ->
                        openFreeAreaGui(plugin, player)
                    }
                }
            }
        }

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