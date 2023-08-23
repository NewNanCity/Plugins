package city.newnan.powertools.config

import com.fasterxml.jackson.annotation.JsonProperty

data class ConfigFile(
    @JsonProperty("enable")
    val enable: Boolean
)