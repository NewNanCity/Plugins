package city.newnan.mcron.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ConfigFile(
    val version: String,
    @JsonProperty("timezone-offset")
    val timezoneOffset: String = "Z",
    @JsonProperty("on-server-ready")
    val onServerReady: List<String> = emptyList(),
    @JsonProperty("on-plugin-ready")
    val onPluginReady: Map<String, List<String>> = emptyMap(),
    @JsonProperty("on-plugin-disable")
    val onPluginDisable: Map<String, List<String>> = emptyMap(),
    @JsonProperty("schedule-tasks")
    val scheduleTasks: Map<String, List<String>> = emptyMap(),
    val msg: Map<String, String> = emptyMap()
)
