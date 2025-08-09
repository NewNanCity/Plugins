package city.newnan.guardian.config

import city.newnan.config.database.JacksonHikariCPConfig
import city.newnan.core.config.CorePluginConfig
import city.newnan.core.logging.LogLevel
import com.fasterxml.jackson.annotation.JsonProperty

data class GuardianConfig(
    @JsonProperty("debug")
    val debug: Boolean = false,

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

    @JsonProperty("player-message-prefix")
    val playerMessagePrefix: String = "§7[§6牛腩小镇§7] §f",

    @JsonProperty("console-message-prefix")
    val consoleMessagePrefix: String = "[Guardian] ",

    @JsonProperty("database")
    val database: JacksonHikariCPConfig = JacksonHikariCPConfig(),
) {
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        logging.logLevel = if (debug) LogLevel.DEBUG else LogLevel.INFO
        message.playerPrefix = playerMessagePrefix
        message.consolePrefix = consoleMessagePrefix
    }
}