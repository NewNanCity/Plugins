package city.newnan.railarea.gui.input

import city.newnan.gui.dsl.chatInput
import city.newnan.railarea.RailAreaPlugin
import city.newnan.railarea.config.RailArea
import city.newnan.railarea.config.RailDirection
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.Station
import city.newnan.railarea.gui.openRailLineGui
import city.newnan.railarea.gui.openRailLinesGui
import city.newnan.railarea.gui.openReverseGui
import city.newnan.railarea.i18n.LanguageKeys
import city.newnan.railarea.spatial.Point3D
import city.newnan.railarea.spatial.Range3D
import city.newnan.railarea.utils.getWorldEditPoint
import city.newnan.railarea.utils.getWorldEditSelection
import city.newnan.railarea.utils.isWorldEditAvailable
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player

/**
 * 区域创建/编辑输入处理器
 *
 * 最复杂的输入处理器，处理铁路区域的完整配置
 *
 * @param plugin 插件实例
 * @param player 玩家
 * @param oldArea 要编辑的区域(null表示创建新区域)
 * @param iStation 预设站点
 * @param iLine 预设线路
 * @param iReverse 预设方向
 * @param setRailArea 回调函数，接收区域结果
 */
fun handleAreaInput(
    plugin: RailAreaPlugin,
    player: Player,
    oldArea: RailArea?,
    iStationId: Int? = null,
    iLineId: Int? = null,
    iReverse: Boolean? = null,
    setRailArea: (RailArea?) -> Unit
) {
    var stationId = oldArea?.stationId ?: iStationId
    var railLineId = oldArea?.lineId ?: iLineId
    var area = oldArea?.range3D
    var world = oldArea?.world ?: player.world.name
    var stopPoint = oldArea?.stopPoint
    var direction = oldArea?.direction
    var reverse = oldArea?.reverse ?: iReverse

    fun preview() {
        val station = stationId?.let { plugin.stationStorage.getStationById(it) }
        val railLine = railLineId?.let { plugin.stationStorage.getLineById(it) }
        plugin.messager.sprintfPlain(
            LanguageKeys.Input.Area.PREVIEW,
            station?.name ?: plugin.messager.sprintfPlain(LanguageKeys.Input.Area.PREVIEW_NOT_SET),
            railLine?.name ?: plugin.messager.sprintfPlain(LanguageKeys.Input.Area.PREVIEW_NOT_SET),
            if (reverse != null && railLine != null) {
                if (reverse!!) railLine.stations.first().name else railLine.stations.last().name
            } else plugin.messager.sprintfPlain(LanguageKeys.Input.Area.PREVIEW_NOT_SET),
            world,
            area?.let { "(${it.minX}, ${it.minY}, ${it.minZ}) - (${it.maxX}, ${it.maxY}, ${it.maxZ})" } ?: plugin.messager.sprintfPlain(LanguageKeys.Input.Area.PREVIEW_NOT_SET),
            stopPoint?.let { "(${it.x}, ${it.y}, ${it.z})" } ?: plugin.messager.sprintfPlain(LanguageKeys.Input.Area.PREVIEW_NOT_SET),
            direction?.name ?: plugin.messager.sprintfPlain(LanguageKeys.Input.Area.PREVIEW_NOT_SET)
        ).split("\n").forEach { plugin.messager.printf(player, it) }
    }
    fun help() {
        plugin.messager.sprintfPlain(
            LanguageKeys.Input.Area.HELP
        ).split("\n").forEach { plugin.messager.printf(player, it) }
    }

    help()
    val result = plugin.chatInput(player, hide = true) { input ->
        val argv = input.split(" ").filter { it.isNotEmpty() }
        when (argv[0].lowercase()) {
            "station" -> {
                // 打开GUI选择流程: 线路 → 站点 → 方向
                openRailLinesGui(plugin, player, false) { selectedLineId ->
                    // 进入线路详情页面选择站点
                    openRailLineGui(plugin, player, false, plugin.stationStorage.getLineById(selectedLineId)!!) { selectedStationId ->
                        // 选择方向
                        openReverseGui(plugin, player, plugin.stationStorage.getLineById(selectedLineId)!!) { selectedReverse ->
                            // 设置所有选择的值
                            railLineId = selectedLineId
                            stationId = selectedStationId
                            reverse = selectedReverse
                            val selectedStation = plugin.stationStorage.getStationById(selectedStationId)!!
                            val railLine = plugin.stationStorage.getLineById(railLineId)!!
                            // 这里需要设置station，但由于station是val，我们需要重新设计
                            plugin.messager.printf(player, LanguageKeys.Input.Area.STATION_SELECTED, selectedStation.name)
                            plugin.messager.printf(player, LanguageKeys.Input.Area.DIRECTION_SELECTED,
                                if (selectedReverse) railLine.stations.first().name else railLine.stations.last().name)
                            preview()
                        }
                    }
                }
                false
            }
            "area" -> {
                if (argv.size < 2) {
                    if (!isWorldEditAvailable()) {
                        plugin.messager.printf(player, LanguageKeys.Input.Area.WORLDEDIT_NOT_INSTALLED)
                        return@chatInput false
                    }

                    val selection = player.getWorldEditSelection()
                    if (selection == null) {
                        plugin.messager.printf(player, LanguageKeys.Input.Area.SELECT_AREA_FIRST)
                        return@chatInput false
                    }

                    if (selection.world != player.world) {
                        plugin.messager.printf(player, LanguageKeys.Input.Area.WRONG_WORLD)
                        return@chatInput false
                    }

                    // 检查是否为单点选择
                    val region = selection.region
                    if (region.min.x == region.max.x && region.min.y == region.max.y && region.min.z == region.max.z) {
                        plugin.messager.printf(player, LanguageKeys.Input.Area.SINGLE_POINT_ERROR)
                        return@chatInput false
                    }

                    if (world != selection.world.name) {
                        stopPoint = null
                        direction = null
                    }

                    area = Range3D(
                        region.min.x, region.min.y, region.min.z,
                        region.max.x, region.max.y, region.max.z
                    )
                    world = selection.world.name
                    plugin.messager.printf(player, LanguageKeys.Input.Area.AREA_SET, world, region.min.x, region.min.y, region.min.z, region.max.x, region.max.y, region.max.z)
                    preview()
                } else {
                    // 手动输入坐标模式
                    if (argv.size != 8) {
                        plugin.messager.printf(player, LanguageKeys.Input.Area.AREA_FORMAT_ERROR)
                        return@chatInput false
                    }

                    try {
                        val worldT = Bukkit.getWorld(argv[1])
                        if (worldT == null) {
                            plugin.messager.printf(player, LanguageKeys.Input.Area.WORLD_NOT_EXISTS, argv[1])
                            return@chatInput false
                        }

                        if (world != worldT.name) {
                            stopPoint = null
                            direction = null
                        }

                        val minX = argv[2].toInt()
                        val minY = argv[3].toInt()
                        val minZ = argv[4].toInt()
                        val maxX = argv[5].toInt()
                        val maxY = argv[6].toInt()
                        val maxZ = argv[7].toInt()

                        area = Range3D(minX, minY, minZ, maxX, maxY, maxZ)
                        world = worldT.name
                        plugin.messager.printf(player, LanguageKeys.Input.Area.AREA_SET, world, minX, minY, minZ, maxX, maxY, maxZ)
                        preview()
                    } catch (e: NumberFormatException) {
                        plugin.messager.printf(player, LanguageKeys.Input.Area.AREA_FORMAT_ERROR)
                    }
                }
                false
            }
            "dir", "direction" -> {
                if (world != player.world.name) {
                    plugin.messager.printf(player, LanguageKeys.Input.Area.MUST_IN_AREA_WORLD)
                    return@chatInput false
                }

                try {
                    val facing = player.facing
                    direction = when (facing) {
                        BlockFace.NORTH -> RailDirection.NORTH
                        BlockFace.SOUTH -> RailDirection.SOUTH
                        BlockFace.EAST -> RailDirection.EAST
                        BlockFace.WEST -> RailDirection.WEST
                        else -> {
                            plugin.messager.printf(player, LanguageKeys.Input.Area.FACE_CARDINAL_DIRECTION)
                            return@chatInput false
                        }
                    }
                    plugin.messager.printf(player, LanguageKeys.Input.Area.DIRECTION_SET, direction!!.name)
                    preview()
                } catch (e: Exception) {
                    plugin.messager.printf(player, LanguageKeys.Input.Area.FACE_CARDINAL_DIRECTION)
                }
                false
            }
            "stop" -> {
                if (argv.size < 2) {
                    if (!isWorldEditAvailable()) {
                        plugin.messager.printf(player, LanguageKeys.Input.Area.WORLDEDIT_NOT_INSTALLED)
                        return@chatInput false
                    }

                    val pointSelection = player.getWorldEditPoint()
                    if (pointSelection == null) {
                        plugin.messager.printf(player, LanguageKeys.Input.Area.SELECT_RAIL_FIRST)
                        return@chatInput false
                    }

                    if (pointSelection.world != player.world) {
                        plugin.messager.printf(player, LanguageKeys.Input.Area.WRONG_WORLD)
                        return@chatInput false
                    }

                    if (world != pointSelection.world.name) {
                        plugin.messager.printf(player, LanguageKeys.Input.Area.STOP_MUST_IN_AREA)
                        return@chatInput false
                    }

                    val block = pointSelection.world.getBlockAt(pointSelection.point.x, pointSelection.point.y, pointSelection.point.z)
                    if (block.type != Material.RAIL) {
                        plugin.messager.printf(player, LanguageKeys.Input.Area.MUST_BE_RAIL)
                        return@chatInput false
                    }

                    world = pointSelection.world.name
                    stopPoint = Point3D(
                        pointSelection.point.x,
                        pointSelection.point.y,
                        pointSelection.point.z
                    )
                    plugin.messager.printf(player, LanguageKeys.Input.Area.STOP_POINT_SET, stopPoint!!.x, stopPoint!!.y, stopPoint!!.z)
                    preview()
                } else {
                    // 手动输入坐标模式
                    if (argv.size != 4) {
                        plugin.messager.printf(player, LanguageKeys.Input.Area.STOP_FORMAT_ERROR)
                        return@chatInput false
                    }

                    try {
                        val x = argv[1].toInt()
                        val y = argv[2].toInt()
                        val z = argv[3].toInt()
                        stopPoint = Point3D(x, y, z)
                        plugin.messager.printf(player, LanguageKeys.Input.Area.STOP_POINT_SET, x, y, z)
                        preview()
                    } catch (e: NumberFormatException) {
                        plugin.messager.printf(player, LanguageKeys.Input.Area.STOP_FORMAT_ERROR)
                    }
                }
                false
            }
            "preview" -> {
                preview()
                false
            }
            "cancel" -> {
                plugin.messager.printf(player, LanguageKeys.Input.Common.CANCELLED)
                setRailArea(null)
                true
            }
            "ok" -> {
                if (stationId == null || railLineId == null || reverse == null) {
                    plugin.messager.printf(player, LanguageKeys.Input.Area.STATION_REQUIRED)
                    false
                } else if (area == null) {
                    plugin.messager.printf(player, LanguageKeys.Input.Area.AREA_RANGE_REQUIRED)
                    false
                } else if (stopPoint == null) {
                    plugin.messager.printf(player, LanguageKeys.Input.Area.STOP_POINT_REQUIRED)
                    false
                } else if (direction == null) {
                    plugin.messager.printf(player, LanguageKeys.Input.Area.DIRECTION_REQUIRED)
                    false
                } else {
                    // 验证停靠点是否在区域内
                    if (!area.contains(stopPoint!!)) {
                        plugin.messager.printf(player, LanguageKeys.Input.Area.STOP_MUST_IN_AREA)
                        false
                    } else {
                        try {
                            val railArea = RailArea(
                                id = -1,
                                world = world,
                                range3D = area,
                                direction = direction!!,
                                stopPoint = stopPoint!!,
                                stationId = stationId,
                                lineId = railLineId,
                                reverse = reverse,
                                storage = plugin.stationStorage
                            )
                            setRailArea(railArea)
                            true
                        } catch (e: Exception) {
                            plugin.logger.error("Failed to create area", e)
                            plugin.messager.printf(player, LanguageKeys.Input.Area.CREATE_FAILED, e.message ?: "Unknown error")
                            false
                        }
                    }
                }
            }
            else -> {
                plugin.messager.printf(player, LanguageKeys.Input.Common.UNKNOWN_COMMAND, LanguageKeys.Input.Area.COMMANDS)
                help()
                false
            }
        }
    }

    if (result) {
        if (oldArea != null) {
            plugin.messager.printf(player, LanguageKeys.Input.Area.EDIT_PROMPT, oldArea.station.name, oldArea.line.name)
        } else {
            plugin.messager.printf(player, LanguageKeys.Input.Area.PROMPT)
        }
    } else {
        plugin.messager.printf(player, LanguageKeys.Input.Common.CHAT_INPUT_BUSY)
        setRailArea(null)
    }
}
