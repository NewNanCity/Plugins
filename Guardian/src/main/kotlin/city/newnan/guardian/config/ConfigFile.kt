package city.newnan.guardian.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ConfigFile(
    @JsonProperty("check-group")
    val checkGroup: Boolean = true,
    @JsonProperty("group-world")
    val groupWorld: String = "world",
    @JsonProperty("newbie-group")
    val newbieGroup: String = "Newbie",
    @JsonProperty("judgemental-group")
    val judgementalGroup: String = "Judgemental",
    @JsonProperty("players-group")
    val playersGroup: String = "Player",
    @JsonProperty("server-address")
    val serverAddress: String,
    @JsonProperty("server-name")
    val serverName: String,
    val mysql: MySQLConfig,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MySQLConfig(
    val host : String,
    val port : Int = 3306,
    val database : String,
    val username : String,
    val password : String,
    val params: Map<String, String> = emptyMap()
)