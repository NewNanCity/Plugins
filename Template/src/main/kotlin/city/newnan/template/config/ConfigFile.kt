package city.newnan.template.config

import com.fasterxml.jackson.annotation.JsonProperty


data class ConfigFile(
    @JsonProperty("enable")
    val enable: Boolean
)