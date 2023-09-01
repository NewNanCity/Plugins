package city.newnan.bettercommandblock.config

import com.fasterxml.jackson.annotation.JsonProperty

data class ConfigFile(
    @JsonProperty("blocked-commands")
    val blockedCommands: HashSet<String> = hashSetOf(),
)