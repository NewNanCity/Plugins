package city.newnan.tpa.config

import com.fasterxml.jackson.annotation.JsonProperty


data class ConfigFile(
    @JsonProperty("enable")
    val enable: Boolean = true,
    @JsonProperty("cool-down-second")
    val coolDown: Int = 15,
    @JsonProperty("delay-second")
    val delay: Int = 3,
    @JsonProperty("expired-second")
    val expired: Int = 60,
    @JsonProperty("exclude-world")
    val excludeWorld: Set<String> = emptySet(),
)