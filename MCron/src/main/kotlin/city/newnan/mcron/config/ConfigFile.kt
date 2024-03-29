package city.newnan.mcron.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ConfigFile(
    val version: String,
    @JsonProperty("timezone-offset")
    val timezoneOffset: String = "Z",
    @JsonProperty("on-server-ready")
    val onServerReady: Map<String, Array<String>> = emptyMap(),
    @JsonProperty("on-plugin-ready")
    val onPluginReady: Map<String, Array<String>> = emptyMap(),
    @JsonProperty("on-plugin-disable")
    val onPluginDisable: Map<String, Array<String>> = emptyMap(),
    @JsonProperty("schedule-tasks")
    val scheduleTasks: Map<String, Array<String>> = emptyMap(),
)
