package city.newnan.mcron.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PlayerCron(
    @JsonProperty("on-join")
    val onJoin: MutableList<String> = mutableListOf(),
)

typealias PlayerCrons = MutableMap<UUID, PlayerCron>