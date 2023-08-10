package city.newnan.feefly.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ConfigFile(
    @JsonProperty("tick-per-count")
    val tickPerCount: Long = 20,
    @JsonProperty("cost-per-count")
    val costPerCount: Double = 0.3,
    @JsonProperty("fly-speed")
    val flySpeed: Float = 0.05f,
    @JsonProperty("target-account")
    val targetAccount: String? = null
)