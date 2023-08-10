package city.newnan.railarea.config

import city.newnan.railarea.Range3D
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.World


class RailArea(
    val name: String,
    val world: World,
    val range3D: Range3D,
    val title: String,
    val subTitle: String?,
    val actionBar: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RailAreaConfig(
    @JsonProperty("area")
    val areaString: String, // (x,y,z)+(dx,dy,dz)
    val title: String,
    @JsonProperty("subtitle")
    val subTitle: String?,
    @JsonProperty("actionbar")
    val actionBar: String?,
) {
    companion object {
        fun valueOf(railArea: RailArea): RailAreaConfig {
            return RailAreaConfig(
                railArea.range3D.toAreaString(),
                railArea.title,
                railArea.subTitle,
                railArea.actionBar
            )
        }
    }

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

    fun toRailArea(name: String, world: World): RailArea {
        return RailArea(name, world, range3D, title, subTitle, actionBar)
    }
}

fun Range3D.toAreaString(): String {
    return "($minX,$minY,$minZ)+(${maxX - minX},${maxY - minY},${maxZ - minZ})"
}

typealias AreasWorld = LinkedHashMap<String, RailAreaConfig>

typealias AreasWorlds = LinkedHashMap<String, AreasWorld>