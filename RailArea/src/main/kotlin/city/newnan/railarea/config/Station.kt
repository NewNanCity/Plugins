package city.newnan.railarea.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.Color
import org.bukkit.Material
import kotlin.math.abs

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

fun Color.toHexString() = "#${Integer.toHexString(asRGB()).uppercase()}"
fun String.toColor() = Color.fromRGB(substring(1).toInt(16))
fun Color.toFMString() = "§x${Integer.toHexString(asRGB()).toCharArray().joinToString("") { "§$it" }}"

val colorMaterials = arrayOf(
    Color.fromRGB(0x000000) to Material.BLACK_CONCRETE,
    Color.fromRGB(0x0000AA) to Material.BLUE_CONCRETE,
    Color.fromRGB(0x00AA00) to Material.GREEN_CONCRETE,
    Color.fromRGB(0x00AAAA) to Material.CYAN_CONCRETE,
    Color.fromRGB(0xAA0000) to Material.RED_CONCRETE,
    Color.fromRGB(0xAA00AA) to Material.PURPLE_CONCRETE,
    Color.fromRGB(0xFFAA00) to Material.ORANGE_CONCRETE,
    Color.fromRGB(0xAAAAAA) to Material.LIGHT_GRAY_CONCRETE,
    Color.fromRGB(0x555555) to Material.GRAY_CONCRETE,
    Color.fromRGB(0x5555FF) to Material.LIGHT_BLUE_CONCRETE,
    Color.fromRGB(0x55FF55) to Material.LIME_CONCRETE,
    Color.fromRGB(0x55FFFF) to Material.LIGHT_BLUE_CONCRETE,
    Color.fromRGB(0xFF5555) to Material.MAGENTA_CONCRETE,
    Color.fromRGB(0xFF55FF) to Material.PINK_CONCRETE,
    Color.fromRGB(0xFFFF55) to Material.YELLOW_CONCRETE,
    Color.fromRGB(0xFFFFFF) to Material.WHITE_CONCRETE,
)

fun Color.toMaterial(): Material {
    // find nearest Manhattan distance of this color and colorMaterials
    var minDistance = 0xFFFFFF
    var minMaterial = Material.WHITE_CONCRETE
    for ((color, material) in colorMaterials) {
        val distance = abs(color.red) + abs(color.green) + abs(color.blue)
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