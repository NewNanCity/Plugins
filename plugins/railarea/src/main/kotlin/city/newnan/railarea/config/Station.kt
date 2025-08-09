package city.newnan.railarea.config

import city.newnan.railarea.manager.StationStorage
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.Color
import org.bukkit.Material

/**
 * 站点配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class StationConfig(
    @JsonProperty("name") val name: String
)

/**
 * 线路配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class RailLineConfig(
    @JsonProperty("name") val name: String,
    @JsonProperty("stations") val stations: List<Int> = emptyList(),
    @JsonProperty("color") val color: String,
    @JsonProperty("is-cycle") val isCycle: Boolean = false,
    @JsonProperty("color-material") val colorMaterial: Material,
    @JsonProperty("left-return") val leftReturn: Boolean = false,
    @JsonProperty("right-return") val rightReturn: Boolean = false
)

/**
 * 站点实体类
 */
data class Station(
    val id: Int,
    val name: String,
    internal val storage: StationStorage
) {
    override fun hashCode(): Int = id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Station
        return id == other.id
    }

    override fun toString(): String = "Station(id=$id, name='$name')"

    val lines: Set<RailLine> get() = storage.getStationLines(id)
}

/**
 * 线路实体类
 */
data class RailLine(
    val id: Int,
    val name: String,
    val stationIds: List<Int>,
    val color: Color,
    val isCycle: Boolean,
    val colorMaterial: Material,
    val leftReturn: Boolean,
    val rightReturn: Boolean,
    internal val storage: StationStorage
) {
    override fun hashCode(): Int = id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RailLine
        return id == other.id
    }

    override fun toString(): String = "RailLine(id=$id, name='$name')"

    val stations = stationIds.map { storage.getStationById(it)!! }
}
