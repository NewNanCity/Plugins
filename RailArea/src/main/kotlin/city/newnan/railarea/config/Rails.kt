package city.newnan.railarea.config

import com.fasterxml.jackson.annotation.JsonProperty

data class RailsConfig(
    val stations: Map<Int, StationConfig> = emptyMap(),
    @JsonProperty("rail-lines")
    val railLines: Map<Int, RailLineConfig> = emptyMap(),
    val areas: Map<String, List<RailAreaConfig>> = emptyMap(),
)