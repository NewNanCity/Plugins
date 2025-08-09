package city.newnan.railarea.config

import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.block.BlockFace

/**
 * 方向枚举
 */
enum class RailDirection {
    @JsonProperty("NORTH") NORTH,
    @JsonProperty("SOUTH") SOUTH,
    @JsonProperty("EAST") EAST,
    @JsonProperty("WEST") WEST;
}