package city.newnan.railarea.manager

import city.newnan.config.extensions.configManager
import city.newnan.core.utils.EventEmitter
import city.newnan.railarea.RailAreaPlugin
import city.newnan.railarea.config.AreaConfig
import city.newnan.railarea.config.RailArea
import city.newnan.railarea.config.RailDirection
import city.newnan.railarea.config.RailLine
import city.newnan.railarea.config.RailLineConfig
import city.newnan.railarea.config.RailsConfig
import city.newnan.railarea.config.Station
import city.newnan.railarea.config.StationConfig
import city.newnan.railarea.config.parseHexColor
import city.newnan.railarea.config.toHexString
import city.newnan.railarea.config.toMaterial
import city.newnan.railarea.spatial.Point3D
import city.newnan.railarea.spatial.Range3D
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class StationStorage(val plugin: RailAreaPlugin) {
    // ID生成器
    private var nextStationId = 1
    private var nextLineId = 1
    private var nextAreaId = 1

    // 站点、线路和区域数据
    private val stations = ConcurrentHashMap<Int, Station>()
    private val lines = ConcurrentHashMap<Int, RailLine>()
    private val areas = ConcurrentHashMap<Int, RailArea>()

    // 未知站点和线路（用于错误处理）
    val unknownStation = Station(-1, "#未知站点#", this)
    val unknownLine = RailLine(
        id = -1,
        name = "#未知线路#",
        stationIds = mutableListOf(),
        color = org.bukkit.Color.WHITE,
        isCycle = false,
        colorMaterial = org.bukkit.Material.BARRIER,
        leftReturn = false,
        rightReturn = false,
        this
    )

    // 站点-线路-区域映射关系（用于快速查找和清理）
    private val lineStationAreas = ConcurrentHashMap<Pair<Int, Int>, MutableSet<Int>>()

    // 站点包含的线路
    private val stationLines = ConcurrentHashMap<Int, MutableSet<Int>>()

    val onRailAreaReloaded = EventEmitter<Unit>()
    val onRailAreaRemoved = EventEmitter<RailArea>()
    val onRailAreaAdded = EventEmitter<RailArea>()
    val onRailAreaUpdated = EventEmitter<Pair<RailArea, RailArea>>()

    // ---------------- 业务接口 -----------------

    // === 站点管理 ===
    /**
     * 通过ID获取站点
     *
     * @param id 站点ID
     * @return 站点对象，如果不存在则返回null
     */
    fun getStationById(id: Int): Station? = stations[id]

    /**
     * 通过名称获取站点
     *
     * @param name 站点名称
     * @return 站点对象，如果不存在则返回null
     */
    fun getStationByName(name: String): Station? = stations.values.find { it.name == name }

    /**
     * 获取所有站点
     *
     * @return 所有站点的列表
     */
    fun getAllStations() = stations.values.toList()

    /**
     * 添加站点
     *
     * @param name 站点名称
     * @param save 是否保存
     * @return 添加的站点对象
     */
    fun addStation(name: String, save: Boolean = true): Station {
        val station = Station(nextStationId++, name, this)
        stations[station.id] = station
        if (save) save()
        return station
    }

    /**
     * 删除站点
     *
     * @param id 站点ID
     * @param save 是否保存
     * @return 是否删除成功
     */
    fun removeStation(id: Int, save: Boolean = true): Boolean {
        if (!stations.contains(id)) return false

        // 从所有线路中移除该站点
        stationLines[id]?.forEach { lineId ->
            val line = lines[lineId] ?: return@forEach
            lines[lineId] = line.copy(stationIds = line.stationIds.filter { it != id })
        }

        // 清理站点相关的区域为游离
        lineStationAreas.keys.filter { it.first == id }.forEach { key ->
            lineStationAreas.remove(key)?.forEach { areaId ->
                val area = areas[areaId] ?: return@forEach
                onRailAreaRemoved.emit(area)
                areas[areaId] = area.copy(stationId = unknownStation.id, lineId = unknownLine.id)
            }
        }

        stations.remove(id)
        stationLines.remove(id)

        if (save) save()
        return true
    }

    /**
     * 更新站点
     *
     * @param station 站点对象
     * @param save 是否保存
     * @return 是否更新成功
     */
    fun updateStation(station: Station, save: Boolean = true): Boolean {
        if (station.storage != this) return false
        val oldStation = stations[station.id] ?: return false
        if (oldStation == station) return true
        stations[station.id] = oldStation.copy(name = station.name)
        if (save) save()
        return true
    }

    internal fun getStationLines(id: Int): Set<RailLine> = stationLines[id]?.mapNotNull { lines[it] }?.toSet() ?: emptySet()

    // === 线路管理 ===
    /**
     * 通过ID获取线路
     *
     * @param id 线路ID
     * @return 线路对象，如果不存在则返回null
     */
    fun getLineById(id: Int): RailLine? = lines[id]

    /**
     * 通过名称获取线路
     *
     * @param name 线路名称
     * @return 线路对象，如果不存在则返回null
     */
    fun getLineByName(name: String): RailLine? = lines.values.find { it.name == name }

    /**
     * 获取所有线路
     *
     * @return 所有线路的列表
     */
    fun getAllLines() = lines.values.toList()

    /**
     * 添加线路
     *
     * @param name 线路名称
     * @param color 线路颜色
     * @param isCycle 是否为环线
     * @param stationIds 线路包含的站点ID
     * @param colorMaterial 线路颜色材质
     * @param leftReturn 是否左返
     * @param rightReturn 是否右返
     * @param save 是否保存
     * @return 添加的线路对象
     */
    fun addLine(
        name: String,
        color: org.bukkit.Color,
        isCycle: Boolean = false,
        stationIds: MutableList<Int> = mutableListOf(),
        colorMaterial: org.bukkit.Material = color.toMaterial(),
        leftReturn: Boolean = false,
        rightReturn: Boolean = false,
        save: Boolean = true
    ): RailLine {
        val line = RailLine(
            id = nextLineId++,
            name = name,
            stationIds = stationIds,
            color = color,
            isCycle = isCycle,
            colorMaterial = colorMaterial,
            leftReturn = leftReturn,
            rightReturn = rightReturn,
            this
        )
        lines[line.id] = line
        stationIds.forEach { stationId ->
            stationLines.getOrPut(stationId) { ConcurrentHashMap.newKeySet() }.add(line.id)
        }
        if (save) save()
        return line
    }

    /**
     * 删除线路
     *
     * @param id 线路ID
     * @param save 是否保存
     * @return 是否删除成功
     */
    fun removeLine(id: Int, save: Boolean = true): Boolean {
        val line = lines[id] ?: return false

        // 从所有站点中移除该线路
        line.stationIds.forEach { stationId ->
            stationLines[stationId]?.remove(id)
            if (stationLines[stationId]?.isEmpty() == true) {
                stationLines.remove(stationId)
            }
        }

        // 清理线路相关的区域为游离
        lineStationAreas.keys.filter { it.second == id }.forEach { key ->
            lineStationAreas.remove(key)?.forEach { areaId ->
                val area = areas[areaId] ?: return@forEach
                onRailAreaRemoved.emit(area)
                areas[areaId] = area.copy(stationId = unknownStation.id, lineId = unknownLine.id)
            }
        }

        lines.remove(id)

        if (save) save()
        return true
    }

    /**
     * 更新线路
     *
     * @param line 线路对象
     * @param save 是否保存
     * @return 是否更新成功
     */
    fun updateLine(line: RailLine, save: Boolean = true): Boolean {
        if (line.storage != this) return false
        val oldLine = lines[line.id] ?: return false
        if (oldLine == line) return true

        // 站点更新，关注删除的站点，要更新站点的线路列表并更新游离区域
        val oldStationIds = oldLine.stationIds.toSet()
        val newStationIds = line.stationIds.toSet()
        (oldStationIds - newStationIds).forEach { stationId ->
            stationLines[stationId]?.remove(line.id)
            if (stationLines[stationId]?.isEmpty() == true) {
                stationLines.remove(stationId)
            }
            lineStationAreas.remove(stationId to line.id)?.forEach { areaId ->
                val area = areas[areaId] ?: return@forEach
                onRailAreaRemoved.emit(area)
                areas[areaId] = area.copy(stationId = unknownStation.id, lineId = unknownLine.id)
            }
        }
        (newStationIds - oldStationIds).forEach { stationId ->
            stationLines.getOrPut(stationId) { ConcurrentHashMap.newKeySet() }.add(line.id)
        }

        lines[line.id] = oldLine.copy(
            name = line.name,
            stationIds = line.stationIds,
            color = line.color,
            isCycle = line.isCycle,
            colorMaterial = line.colorMaterial,
            leftReturn = line.leftReturn,
            rightReturn = line.rightReturn
        )

        if (save) save()
        return true
    }

    // === 区域管理 ===
    /**
     * 通过ID获取区域
     *
     * @param id 区域ID
     * @return 区域对象，如果不存在则返回null
     */
    fun getAreaById(id: Int): RailArea? = areas[id]

    /**
     * 通过世界名称获取区域
     *
     * @param world 世界名称
     * @return 区域对象列表
     */
    fun getAreasInWorld(world: String) = areas.values.filter { it.world == world }.toList()

    /**
     * 通过站点和线路获取区域
     *
     * @param station 站点对象
     * @param line 线路对象
     * @return 区域对象列表
     */
    fun getAreas(station: Station, line: RailLine) = lineStationAreas[station.id to line.id]?.mapNotNull { areas[it] }?.toList() ?: emptyList()

    /**
     * 添加区域
     *
     * @param world 世界名称
     * @param range3D 区域范围
     * @param direction 区域方向
     * @param stopPoint 停靠点
     * @param stationId 站点ID
     * @param lineId 线路ID
     * @param reverse 是否反向
     * @param save 是否保存
     * @return 是否添加成功
     */
    fun addArea(
        world: String,
        range3D: Range3D,
        direction: RailDirection,
        stopPoint: Point3D,
        stationId: Int,
        lineId: Int,
        reverse: Boolean,
        save: Boolean = true
    ): Boolean {
        val area = RailArea(
            id = nextAreaId++,
            world = world,
            range3D = range3D,
            direction = direction,
            stopPoint = stopPoint,
            stationId = stations[stationId]?.id ?: unknownStation.id,
            lineId = lines[lineId]?.id ?: unknownLine.id,
            reverse = reverse,
            storage = this
        )
        areas[area.id] = area
        lineStationAreas.getOrPut(stationId to lineId) { ConcurrentHashMap.newKeySet() }.add(area.id)
        onRailAreaAdded.emit(area)
        if (save) save()
        return true
    }

    /**
     * 删除区域
     *
     * @param id 区域ID
     * @param save 是否保存
     * @return 是否删除成功
     */
    fun removeArea(id: Int, save: Boolean = true): Boolean {
        val area = areas[id] ?: return false
        onRailAreaRemoved.emit(area)
        lineStationAreas.remove(area.station.id to area.line.id)?.remove(id)
        if (lineStationAreas[area.station.id to area.line.id]?.isEmpty() == true) {
            lineStationAreas.remove(area.station.id to area.line.id)
        }
        areas.remove(id)
        if (save) save()
        return true
    }

    /**
     * 更新区域
     *
     * @param area 区域对象
     * @param save 是否保存
     * @return 是否更新成功
     */
    fun updateArea(area: RailArea, save: Boolean = true): Boolean {
        if (area.storage != this) return false
        val oldArea = areas[area.id] ?: return false
        if (oldArea == area) return true

        val finalArea = if (area.softDeleted) {
            area.copy(
                stationId = unknownStation.id,
                lineId = unknownLine.id
            )
        } else {
            area
        }

        if (!oldArea.softDeleted && finalArea.softDeleted) {
            onRailAreaRemoved.emit(oldArea)
        }

        if (oldArea.station.id != finalArea.station.id || oldArea.line.id != finalArea.line.id) {
            lineStationAreas.remove(oldArea.station.id to oldArea.line.id)?.remove(oldArea.id)
            if (lineStationAreas[oldArea.station.id to oldArea.line.id]?.isEmpty() == true) {
                lineStationAreas.remove(oldArea.station.id to oldArea.line.id)
            }
            lineStationAreas.getOrPut(finalArea.station.id to finalArea.line.id) { ConcurrentHashMap.newKeySet() }.add(finalArea.id)
        }

        areas[finalArea.id] = oldArea.copy(
            world = finalArea.world,
            range3D = finalArea.range3D,
            direction = finalArea.direction,
            stopPoint = finalArea.stopPoint,
            stationId = finalArea.station.id,
            lineId = finalArea.line.id,
            reverse = finalArea.reverse
        )

        if (!finalArea.softDeleted) {
            if (oldArea.softDeleted) {
                onRailAreaAdded.emit(finalArea)
            } else {
                onRailAreaUpdated.emit(oldArea to finalArea)
            }
        }

        if (save) save()
        return true
    }

    // ---------------- 读写实现 ------------------
    /**
     * 保存数据
     */
    fun save() {
        val areaWorlds = mutableMapOf<String, MutableList<AreaConfig>>()
        val stationMap = mutableMapOf<Int, StationConfig>()
        val lineMap = mutableMapOf<Int, RailLineConfig>()

        // ID 重映射
        val stationIdMap = stations.keys.sorted().mapIndexed { index, id -> id to index + 1 }.toMap()
        val lineIdMap = lines.keys.sorted().mapIndexed { index, id -> id to index + 1 }.toMap()

        // 添加站点
        stations.values.forEach { station ->
            stationMap[stationIdMap[station.id]!!] = StationConfig(station.name)
        }

        // 添加线路
        lines.values.forEach { line ->
            lineMap[lineIdMap[line.id]!!] = RailLineConfig(
                name = line.name,
                stations = line.stations.map { station -> stationIdMap[station.id]!! },
                color = line.color.toHexString(),
                isCycle = line.isCycle,
                colorMaterial = line.colorMaterial,
                leftReturn = line.leftReturn,
                rightReturn = line.rightReturn
            )
        }

        // 添加区域
        areas.values.forEach { area ->
            areaWorlds.computeIfAbsent(area.world) { mutableListOf() }.add(
                AreaConfig(
                    range3D = area.range3D,
                    direction = area.direction,
                    stopPoint = area.stopPoint,
                    station = stationIdMap[area.station.id]!!,
                    line = lineIdMap[area.line.id]!!,
                    reverse = area.reverse
                )
            )
        }

        val railsConfig = RailsConfig(
            stations = stationMap,
            railLines = lineMap,
            areas = areaWorlds
        )

        val railFile = File(plugin.dataFolder, "rails.yml")
        var backupFile: File? = null
        try {
            // 先备份
            backupFile = if (railFile.length() > 10L) File(plugin.dataFolder, "rails-backup.yml") else null
            if (backupFile != null) {
                railFile.copyTo(backupFile, true)
            }
            plugin.configManager.save(railsConfig, "rails.yml")
        } catch (e: Exception) {
            backupFile?.copyTo(railFile, true)
            plugin.logger.error("Failed to save stations and lines", e)
        } finally {
            // 删除备份文件
            backupFile?.delete()
        }
    }

    fun load() {
        // 加载rails.yml配置
        plugin.configManager.touchWithMerge("rails.yml", createBackup = true)
        val railsConfig = plugin.configManager.parse<RailsConfig>("rails.yml")

        val newStations = mutableMapOf<Int, Station>()
        val newLines = mutableMapOf<Int, RailLine>()
        val newAreas = mutableMapOf<Int, RailArea>()
        val newlineStationAreas = mutableMapOf<Pair<Int, Int>, MutableSet<Int>>()
        val newStationLines = mutableMapOf<Int, MutableSet<Int>>()
        var newNextStationId = 1
        var newNextLineId = 1
        var newNextAreaId = 1

        // 加载站点
        railsConfig.stations.forEach { (id, stationConfig) ->
            newNextStationId = maxOf(newNextStationId, id + 1)
            val station = Station(id, stationConfig.name, this)
            newStations[id] = station
        }

        // 加载线路
        railsConfig.railLines.forEach { (id, lineConfig) ->
            newNextLineId = maxOf(newNextLineId, id + 1)
            val stationIdList = lineConfig.stations.filter { newStations.containsKey(it) }
            val color = lineConfig.color.parseHexColor()

            val line = RailLine(
                id = id,
                name = lineConfig.name,
                stationIds = stationIdList,
                color = color,
                isCycle = lineConfig.isCycle,
                colorMaterial = lineConfig.colorMaterial,
                leftReturn = lineConfig.leftReturn,
                rightReturn = lineConfig.rightReturn,
                this
            )

            // 将线路添加到站点的线路列表中
            stationIdList.forEach { station -> newStationLines.getOrPut(station) { mutableSetOf() }.add(line.id) }

            newLines[id] = line
        }

        // 加载铁路区域
        railsConfig.areas.forEach { (world, areaConfigs) ->
            // 加载区域
            areaConfigs.forEach { areaConfig: AreaConfig ->
                var station = newStations[areaConfig.station]
                var line = newLines[areaConfig.line]

                // 只有站点和线路都存在，并且线路和站点相关联时，才算合法
                if (station == null || line == null || !station.lines.contains(line) || !line.stations.contains(station)) {
                    station = unknownStation
                    line = unknownLine
                }

                val railArea = RailArea(
                    id = newNextAreaId++,
                    world = world,
                    range3D = areaConfig.range3D,
                    direction = areaConfig.direction,
                    stopPoint = areaConfig.stopPoint,
                    stationId = station.id,
                    lineId = line.id,
                    reverse = areaConfig.reverse,
                    storage = this
                )

                // 添加到站点-线路映射，不合法也添加
                newlineStationAreas.getOrPut(railArea.station.id to railArea.line.id) { ConcurrentHashMap.newKeySet() }.add(railArea.id)
            }
        }

        // 清空现有数据
        stations.clear()
        stations.putAll(newStations)
        lines.clear()
        lines.putAll(newLines)
        areas.clear()
        areas.putAll(newAreas)
        lineStationAreas.clear()
        lineStationAreas.putAll(newlineStationAreas)
        stationLines.clear()
        stationLines.putAll(newStationLines)
        nextStationId = newNextStationId
        nextLineId = newNextLineId
        nextAreaId = newNextAreaId

        onRailAreaReloaded.emit(Unit)
    }
}