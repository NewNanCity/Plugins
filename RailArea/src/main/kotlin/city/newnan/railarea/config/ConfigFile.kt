package city.newnan.railarea.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty


data class WorldSize(val x1: Int, val z1: Int, val x2: Int, val z2: Int)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ConfigFile(
    @JsonProperty("world-size")
    val worldSize: Map<String, WorldSize>
)