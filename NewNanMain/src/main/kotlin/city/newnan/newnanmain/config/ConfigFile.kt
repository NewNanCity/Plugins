package city.newnan.newnanmain.config

import com.fasterxml.jackson.annotation.JsonProperty

data class ConfigFile(
    @JsonProperty("enable")
    val enable: Boolean
)