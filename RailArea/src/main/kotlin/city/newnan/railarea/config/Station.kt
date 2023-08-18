package city.newnan.railarea.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.Color
import org.bukkit.Material

@JsonInclude(JsonInclude.Include.NON_NULL)
data class StationConfig(val name: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RailLineConfig(
    val name: String,
    val stations: List<Int> = emptyList(),
    val color: String,
    @JsonProperty("is-cycle")
    val isCycle: Boolean = false,
    @JsonProperty("color-material")
    val colorMaterial: Material,
)

data class Station(val id: Int, var name: String, val lines: MutableSet<RailLine> = mutableSetOf()) {
    override fun hashCode(): Int  = id
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Station
        if (id != other.id) return false
        if (name != other.name) return false
        if (lines != other.lines) return false
        return true
    }
}

data class RailLine(
    val id: Int,
    var name: String,
    val stations: MutableList<Station> = mutableListOf(),
    var color: Color,
    var isCycle: Boolean,
    var colorMaterial: Material,
) {
    override fun hashCode(): Int  = id
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RailLine
        if (id != other.id) return false
        if (name != other.name) return false
        if (stations != other.stations) return false
        if (color != other.color) return false
        if (isCycle != other.isCycle) return false
        if (colorMaterial != other.colorMaterial) return false
        return true
    }
}

fun Color.toHexString() = "#${"%06X".format(asRGB())}"
fun String.toColor() = Color.fromRGB(substring(1).toInt(16))
fun Color.toFMString() = "§x${"%06X".format(asRGB()).toCharArray().joinToString("") { "§$it" }}"

val colorMaterials = arrayOf(
    Color.fromRGB(0xcdd3d4) to Material.WHITE_CONCRETE,
    Color.fromRGB(0xdd5700) to Material.ORANGE_CONCRETE,
    Color.fromRGB(0xa32699) to Material.MAGENTA_CONCRETE,
    Color.fromRGB(0x1983c4) to Material.LIGHT_BLUE_CONCRETE,
    Color.fromRGB(0xf2ac0b) to Material.YELLOW_CONCRETE,
    Color.fromRGB(0x54a20c) to Material.LIME_CONCRETE,
    Color.fromRGB(0xd35d89) to Material.PINK_CONCRETE,
    Color.fromRGB(0x2c3034) to Material.GRAY_CONCRETE,
    Color.fromRGB(0x75756b) to Material.LIGHT_GRAY_CONCRETE,
    Color.fromRGB(0x0b7082) to Material.CYAN_CONCRETE,
    Color.fromRGB(0x5b1495) to Material.PURPLE_CONCRETE,
    Color.fromRGB(0x212388) to Material.BLUE_CONCRETE,
    Color.fromRGB(0x583215) to Material.BROWN_CONCRETE,
    Color.fromRGB(0x3f5219) to Material.GREEN_CONCRETE,
    Color.fromRGB(0x871515) to Material.RED_CONCRETE,
    Color.fromRGB(0x010103) to Material.BLACK_CONCRETE,
)

fun Color.toMaterial(): Material {
    // find nearest Manhattan distance of this color and colorMaterials
    var minDistance = 0xFFFFFF
    var minMaterial = Material.WHITE_CONCRETE
    val r = red
    val g = green
    val b = blue
    for ((color, material) in colorMaterials) {
        val dr = color.red - r
        val dg = color.green - g
        val db = color.blue - b
        // Lab distance
        val rmean = (color.red + r) shr 1
        val distance = (512 + rmean) * dr * dr + (dg * dg shl 10) + (767 - rmean) * db * db
        if (distance == 0) {
            return material
        }
        if (distance < minDistance) {
            minDistance = distance
            minMaterial = material
        }
    }
    return minMaterial
}