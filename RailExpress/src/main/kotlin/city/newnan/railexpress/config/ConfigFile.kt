package city.newnan.railexpress.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ConfigFile(
    val version: String,
    val config: List<ExpressWorldGroup> = emptyList(),
    val msg: Map<String, String> = emptyMap(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExpressWorldGroup(
    val world: List<String>,
    @JsonProperty("allow-non-player")
    val allowNonPlayer: Boolean = false,
    @JsonProperty("power-rail-only")
    val powerRailOnly: Boolean = true,
    @JsonProperty("block-type")
    val blockType: Map<String, Double> = emptyMap(),
)

