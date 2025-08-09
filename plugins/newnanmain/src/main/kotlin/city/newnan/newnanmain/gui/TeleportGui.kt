package city.newnan.newnanmain.gui

import city.newnan.gui.component.paginated.onLeftClick
import city.newnan.gui.component.paginated.onRightClick
import city.newnan.gui.component.paginated.onShiftRightClick
import city.newnan.gui.component.singleslot.onLeftClick
import city.newnan.gui.dsl.*
import city.newnan.newnanmain.NewNanMainPlugin
import city.newnan.newnanmain.gui.input.handleTeleportInput
import city.newnan.newnanmain.gui.input.handleYesInput
import city.newnan.newnanmain.i18n.LanguageKeys
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType

/**
 * 传送中心GUI
 *
 * 显示所有可用的传送点，支持：
 * - 分页显示传送点
 * - 权限检查
 * - 冷却时间显示
 * - 传送执行
 *
 * @author NewNanCity
 * @since 2.0.0
 */
fun openTeleportGui(plugin: NewNanMainPlugin, player: Player, editable: Boolean = false) {
    val teleportManager = plugin.teleportManager
    val configuredPoints = if (editable) teleportManager.getAllTeleportPoints() else teleportManager.getTeleportPoints(player)

    val title = if (editable) formatPlain(LanguageKeys.Gui.Teleport.MANAGE_TITLE) else formatPlain(LanguageKeys.Gui.Teleport.TITLE)
    plugin.openPage(InventoryType.CHEST, 54, player, title) {
        // 边框填充
        borderFillComponent(0, 0, 9, 6) {
            fill(Material.BLACK_STAINED_GLASS_PANE)
        }

        // 使用分页组件显示传送点
        val paginatedComp = paginatedComponent(
            startX = 0, startY = 0,
            width = 9, height = 5,
            data = {
                // 准备所有传送点（包括内置传送点）
                val allPoints = mutableListOf<TeleportPointData>()

                // 添加内置传送点（仅在非管理员模式下显示）
                if (!editable) {
                    allPoints.add(TeleportPointData(LanguageKeys.Gui.Teleport.BUILTIN_BED_NAME, Material.RED_BED, LanguageKeys.Gui.Teleport.BUILTIN_BED_DESC, isBuiltIn = true, command = "bed"))
                    allPoints.add(TeleportPointData(LanguageKeys.Gui.Teleport.BUILTIN_HOME_NAME, Material.OAK_DOOR, LanguageKeys.Gui.Teleport.BUILTIN_HOME_DESC, isBuiltIn = true, command = "home"))
                    allPoints.add(TeleportPointData(LanguageKeys.Gui.Teleport.BUILTIN_RESOURCE_NAME, Material.GRASS_BLOCK, LanguageKeys.Gui.Teleport.BUILTIN_RESOURCE_DESC, isBuiltIn = true, command = "resource tp"))
                    allPoints.add(TeleportPointData(LanguageKeys.Gui.Teleport.BUILTIN_RESOURCE_NETHER_NAME, Material.NETHERRACK, LanguageKeys.Gui.Teleport.BUILTIN_RESOURCE_NETHER_DESC, isBuiltIn = true, command = "resource tp nether"))
                }

                // 添加配置的传送点
                configuredPoints.forEach { point ->
                    val material = try {
                        Material.valueOf(point.icon)
                    } catch (e: IllegalArgumentException) {
                        Material.GRASS_BLOCK
                    }
                    allPoints.add(TeleportPointData(point.name, material, "传送到 ${point.name}", point = point))
                }

                allPoints
            }
        ) {
            renderEmptySlot {
                item(Material.BARRIER) { name(LanguageKeys.Gui.Teleport.NO_POINTS) }
            }

            render { context ->
                val pointData = context.item ?: return@render null

                item(pointData.material) {
                    name(pointData.name) // name 已经是 LanguageKey
                    val sb = StringBuilder()
                    if (pointData.isBuiltIn) {
                        sb.append(formatPlain(pointData.description)).append('\n')
                    } else if (pointData.point != null) {
                        val point = pointData.point
                        sb.append(formatPlain(LanguageKeys.Gui.Teleport.WORLD_LINE, point.world)).append('\n')
                        sb.append(formatPlain(LanguageKeys.Gui.Teleport.COORDS_LINE, point.x, point.y, point.z)).append('\n')
                        if (editable && point.permission != null) {
                            sb.append(formatPlain(LanguageKeys.Gui.Teleport.PERMISSION_LINE, point.permission!!)).append('\n')
                        }
                        if (!editable) {
                            val cooldown = teleportManager.getRemainingCooldown(player)
                            if (cooldown > 0) {
                                sb.append(formatPlain(LanguageKeys.Gui.Teleport.COOLDOWN_LINE, cooldown)).append('\n')
                            }
                        }
                    }
                    // 空行
                    sb.append('\n')
                    if (editable && !pointData.isBuiltIn) {
                        sb.append(formatPlain(LanguageKeys.Gui.Teleport.ACTION_TELEPORT)).append('\n')
                        sb.append(formatPlain(LanguageKeys.Gui.Teleport.ACTION_EDIT)).append('\n')
                        sb.append(formatPlain(LanguageKeys.Gui.Teleport.ACTION_DELETE))
                    } else {
                        sb.append(formatPlain(LanguageKeys.Gui.Teleport.ACTION_LEFT_CLICK_TELEPORT))
                    }
                    lore(sb.toString())
                }
            }

            // 左键点击传送
            onLeftClick { _, _, pointData ->
                if (pointData != null) {
                    if (pointData.isBuiltIn && pointData.command != null) {
                        // 内置传送点，执行命令
                        player.performCommand(pointData.command)
                        this@openPage.close()
                    } else if (pointData.point != null) {
                        // 配置的传送点，检查冷却时间
                        val cooldown = teleportManager.getRemainingCooldown(player)
                        if (cooldown > 0) {
                            plugin.messager.printf(player, LanguageKeys.Teleport.COOLDOWN, cooldown)
                        } else {
                            if (teleportManager.teleportPlayer(player, pointData.point.name)) {
                                this@openPage.close()
                            }
                        }
                    }
                }
            }

            // 右键点击处理（管理员模式）
            if (editable) {
                // 右键编辑
                onRightClick { _, _, pointData ->
                    if (pointData != null && !pointData.isBuiltIn && pointData.point != null) {
                        handleTeleportInput(plugin, player, pointData.point) { newPoint ->
                            if (newPoint != null) {
                                teleportManager.removeTeleportPoint(pointData.point.name)
                                if (teleportManager.addTeleportPoint(newPoint)) {
                                    plugin.messager.printf(player, LanguageKeys.Gui.Teleport.UPDATED_SUCCESS, newPoint.name)
                                } else {
                                    plugin.messager.printf(player, LanguageKeys.Gui.Teleport.NAME_EXISTS, newPoint.name)
                                }
                            }
                            this@paginatedComponent.clearCache()
                            this@openPage.show()
                        }
                    }
                }

                // Shift+右键删除
                onShiftRightClick { context, index, pointData ->
                    if (pointData != null && !pointData.isBuiltIn && pointData.point != null) {
                        handleYesInput(plugin, player, formatPlain(LanguageKeys.Gui.Teleport.DELETE_CONFIRM, pointData.name)) { yes ->
                            if (yes) {
                                if (teleportManager.removeTeleportPoint(pointData.point.name)) {
                                    plugin.messager.printf(player, LanguageKeys.Gui.Teleport.DELETED_SUCCESS, pointData.point.name)
                                }
                            }
                            this@paginatedComponent.clearCache()
                            this@openPage.show()
                        }
                    }
                }
            }
        }

        // 上一页按钮
        slotComponent(x = 2, y = 5) {
            render {
                item(Material.ARROW) {
                    name(LanguageKeys.Gui.Common.PREVIOUS)
                }
            }
            onLeftClick { _, _, _ ->
                paginatedComp.previousPage()
            }
        }

        // 下一页按钮
        slotComponent(x = 6, y = 5) {
            render {
                item(Material.ARROW) {
                    name(LanguageKeys.Gui.Common.NEXT)
                }
            }
            onLeftClick { _, _, _ ->
                paginatedComp.nextPage()
            }
        }

        // 管理员模式：添加传送点按钮
        if (editable) {
            slotComponent(x = 4, y = 5) {
                render {
                    item(Material.WRITABLE_BOOK) {
                        name(LanguageKeys.Gui.Teleport.ADD_POINT)
                        val part1 = formatPlain(LanguageKeys.Gui.Teleport.ADD_POINT_DESC_1)
                        val part2 = formatPlain(LanguageKeys.Gui.Teleport.ADD_POINT_DESC_2)
                        val part3 = formatPlain(LanguageKeys.Gui.Teleport.ADD_POINT_DESC_ACTION)
                        lore("${part1}\n${part2}\n\n${part3}")
                    }
                }

                onLeftClick { _, _, _ ->
                    handleTeleportInput(plugin, player, null) { point ->
                        if (point != null) {
                            if (teleportManager.addTeleportPoint(point)) {
                                plugin.messager.printf(player, LanguageKeys.Gui.Teleport.ADDED_SUCCESS, point.name)
                            } else {
                                plugin.messager.printf(player, LanguageKeys.Gui.Teleport.NAME_EXISTS, point.name)
                            }
                        }
                        paginatedComp.clearCache()
                        this@openPage.show()
                    }
                }
            }
        }

        // 返回按钮
        slotComponent(x = 8, y = 5) {
            render {
                item(Material.ARROW) {
                    name(LanguageKeys.Gui.Common.BACK)
                }
            }
            onLeftClick { _, _, _ ->
                back()
            }
        }
    }
}

/**
 * 传送点数据类
 */
data class TeleportPointData(
    val name: String,
    val material: Material,
    val description: String,
    val isBuiltIn: Boolean = false,
    val command: String? = null,
    val point: city.newnan.newnanmain.config.TeleportPoint? = null
)