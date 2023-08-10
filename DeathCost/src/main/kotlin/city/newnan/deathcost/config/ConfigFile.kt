package city.newnan.deathcost.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

data class ConfigFile(
    @JsonProperty("death-cost")
    val deathCost: DeathCostConfig = DeathCostConfig(),
    @JsonProperty("death-message")
    val deathMessage: DeathMessageConfig = DeathMessageConfig(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DeathCostConfig(
    @JsonProperty("target-account")
    val targetAccount: String? = null,
    @JsonProperty("use-simple-mode")
    val useSimpleMode: Boolean = true,
    @JsonProperty("simple-mode")
    val simpleMode: SimpleModeConfig? = null,
    @JsonProperty("complex-mode")
    val complexMode: ComplexModeConfig? = null,
)

data class SimpleModeConfig(
    @JsonProperty("cost")
    val cost: Double,
    @JsonProperty("if-percent")
    val ifPercent: Boolean = false,
)

data class CostStage(
    @JsonProperty("max")
    val max: Double,
    @JsonProperty("cost")
    val cost: Double,
    @JsonProperty("if-percent")
    val ifPercent: Boolean = false,
)

typealias ComplexModeConfig = List<CostStage>

data class DeathMessageConfig(
    @JsonProperty("player-enable")
    val playerEnable: Boolean = true,
    @JsonProperty("broadcast-enable")
    val broadcastEnable: Boolean = false,
    @JsonProperty("console-enable")
    val consoleEnable: Boolean = false,
)
