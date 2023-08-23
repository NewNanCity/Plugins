package city.newnan.railarea.config

import city.newnan.railarea.octree.Point3D
import city.newnan.railarea.octree.Range3D
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import me.lucko.helper.Schedulers
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player


class RailArea(
    val world: World,
    val range3D: Range3D,
    val direction: Direction,
    val stopPoint: Point3D,
    val station: Station,
    val line: RailLine,
    val reverse: Boolean,
) {
    val isTerminal: Boolean
        get() =
            if (reverse) line.stations.size > 0 && line.stations.first() == station
            else line.stations.size > 0 && line.stations.last() == station

    val nextStation: Station?
        get() {
            if (line.stations.size <= 0) return null
            val i = line.stations.indexOf(station)
            if (i < 0) return null
            if (line.isCycle) {
                return if (reverse) (if (i == 0) line.stations.last() else line.stations[i - 1])
                else (if ((i + 1) >= line.stations.size) line.stations.first() else line.stations[i + 1])
            } else {
                if (reverse) {
                    if (i == 0) return if (line.leftReturn) line.stations.first() else null
                    return line.stations[i - 1]
                } else {
                    if ((i + 1) >= line.stations.size) return if (line.rightReturn) line.stations.last() else null
                    return line.stations[i + 1]
                }
            }
        }

    fun teleport(player: Player) {
        Schedulers.sync().run {
            player.teleport(
                org.bukkit.Location(
                    world,
                    stopPoint.x.toDouble() + 0.5,
                    stopPoint.y.toDouble() + 0.3,
                    stopPoint.z.toDouble() + 0.5
                )
            )
        }
    }
}


@JsonInclude(JsonInclude.Include.NON_NULL)
data class RailAreaConfig(
    @JsonProperty("area")
    val areaString: String, // (x,y,z)+(dx,dy,dz)
    val direction: Direction,
    @JsonProperty("stop")
    val stopPointString: String, // (x,y,z)
    val station: Int,
    val line: Int,
    val reverse: Boolean,
) {
    companion object {
        fun valueOf(railArea: RailArea): RailAreaConfig {
            return RailAreaConfig(
                railArea.range3D.toAreaString(),
                railArea.direction,
                railArea.stopPoint.toStopPointString(),
                railArea.station.id,
                railArea.line.id,
                railArea.reverse,
            )
        }
    }

    @get:JsonIgnore
    val range3D: Range3D
        get() {
            // (x,y,z)+(dx,dy,dz) to 6 numbers
            val k = areaString.split("(", ")", "+", ",").filterNot { it.isEmpty() }.map { it.toInt() }
            val x = k[0]
            val y = k[1]
            val z = k[2]
            val dx = k[3]
            val dy = k[4]
            val dz = k[5]
            val minX = if (dx > 0) x else x + dx
            val minY = if (dy > 0) y else y + dy
            val minZ = if (dz > 0) z else z + dz
            val maxX = if (dx > 0) x + dx else x
            val maxY = if (dy > 0) y + dy else y
            val maxZ = if (dz > 0) z + dz else z
            return Range3D(minX, minY, minZ, maxX, maxY, maxZ)
        }

    @get:JsonIgnore
    val stopPoint: Point3D
        get() {
            // (x,y,z) to 3 numbers
            val k = stopPointString.split("(", ")", ",").filterNot { it.isEmpty() }.map { it.toInt() }
            val x = k[0]
            val y = k[1]
            val z = k[2]
            return Point3D(x, y, z)
        }
}

fun Range3D.toAreaString(): String =
    "($minX,$minY,$minZ)+(${maxX - minX},${maxY - minY},${maxZ - minZ})"

fun Point3D.toStopPointString(): String = "($x,$y,$z)"


enum class Direction {
    NORTH, SOUTH, EAST, WEST;

    companion object {
        fun valueOf(value: BlockFace): Direction {
            return when (value) {
                BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH_NORTH_WEST, BlockFace.NORTH -> NORTH
                BlockFace.SOUTH_SOUTH_EAST, BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH -> SOUTH
                BlockFace.EAST_NORTH_EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.EAST -> EAST
                BlockFace.WEST_NORTH_WEST, BlockFace.WEST_SOUTH_WEST, BlockFace.WEST -> WEST
                else -> throw IllegalArgumentException("Cannot decide: $value")
            }
        }
    }
}