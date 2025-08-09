package city.newnan.railarea.config

import city.newnan.railarea.manager.StationStorage
import city.newnan.railarea.spatial.Point3D
import city.newnan.railarea.spatial.Range3D
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.util.Vector

/**
 * 铁路区域配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AreaConfig(
    @JsonProperty("range3D") val range3D: Range3D,
    @JsonProperty("direction") val direction: RailDirection,
    @JsonProperty("stop-point") val stopPoint: Point3D,
    @JsonProperty("station") val station: Int,
    @JsonProperty("line") val line: Int,
    @JsonProperty("reverse") val reverse: Boolean
)

private val directions = listOf(
    Vector(1, 0, 0),
    Vector(-1, 0, 0),
    Vector(0, 0, 1),
    Vector(0, 0, -1),
)

/**
 * 铁路区域实体类
 */
data class RailArea(
    val id: Int,
    val world: String,
    val range3D: Range3D,
    val direction: RailDirection,
    val stopPoint: Point3D,
    val stationId: Int,
    val lineId: Int,
    val reverse: Boolean,
    internal val storage: StationStorage
) {
    val station: Station get() = storage.getStationById(stationId) ?: storage.unknownStation

    val line: RailLine get() = storage.getLineById(lineId) ?: storage.unknownLine

    val softDeleted: Boolean get() = stationId == storage.unknownStation.id || lineId == storage.unknownLine.id

    /**
     * 下一站
     */
    @get:JsonIgnore
    val nextStation: Station?
        get() {
            if (line.stations.isEmpty()) return null

            val currentIndex = line.stations.indexOf(station)
            if (currentIndex == -1) return null

            return if (line.isCycle) {
                // 循环线路
                if (reverse) {
                    // 反向：向前一站
                    if (currentIndex == 0) line.stations.last() else line.stations[currentIndex - 1]
                } else {
                    // 正向：向后一站
                    if (currentIndex == line.stations.size - 1) line.stations.first() else line.stations[currentIndex + 1]
                }
            } else {
                // 非循环线路
                if (reverse) {
                    // 反向：向前一站
                    when {
                        currentIndex == 0 -> if (line.leftReturn) line.stations.first() else null
                        else -> line.stations[currentIndex - 1]
                    }
                } else {
                    // 正向：向后一站
                    when {
                        currentIndex == line.stations.size - 1 -> if (line.rightReturn) line.stations.last() else null
                        else -> line.stations[currentIndex + 1]
                    }
                }
            }
        }

    /**
     * 检查位置是否在区域内
     */
    fun contains(location: Location): Boolean {
        return location.world.name == world && range3D.contains(Point3D(location))
    }

    /**
     * 检查点是否在区域内
     */
    fun contains(point: Point3D): Boolean {
        return range3D.contains(point)
    }

    /**
     * 获取停车点位置
     */
    fun getStopLocation(): Location? {
        val worldI = Bukkit.getWorld(world) ?: return null
        return Location(
            worldI,
            stopPoint.x.toDouble() + 0.5,
            stopPoint.y.toDouble() + 0.1,
            stopPoint.z.toDouble() + 0.5
        )
    }

    override fun toString(): String {
        return "RailArea(station=${station.name}, line=${line.name}, reverse=$reverse)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RailArea

        if (world != other.world) return false
        if (range3D != other.range3D) return false
        if (station != other.station) return false
        if (line != other.line) return false
        if (reverse != other.reverse) return false

        return true
    }

    override fun hashCode(): Int {
        var result = world.hashCode()
        result = 31 * result + range3D.hashCode()
        result = 31 * result + station.hashCode()
        result = 31 * result + line.hashCode()
        result = 31 * result + reverse.hashCode()
        return result
    }
}

/**
 * 铁路配置文件结构
 */
data class RailsConfig(
    @JsonProperty("stations") val stations: Map<Int, StationConfig> = emptyMap(),
    @JsonProperty("rail-lines") val railLines: Map<Int, RailLineConfig> = emptyMap(),
    @JsonProperty("areas") val areas: Map<String, List<AreaConfig>> = emptyMap()
)
