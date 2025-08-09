package city.newnan.externalbook.config

import city.newnan.config.database.JacksonHikariCPConfig
import city.newnan.core.config.CorePluginConfig
import city.newnan.core.logging.LogLevel
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 外部书籍插件配置
 *
 * @author NewNanCity
 * @since 1.0.0
 */
data class ExternalBookConfig(
    @JsonProperty("debug")
    val debug: Boolean = false,

    @JsonProperty("player-message-prefix")
    val playerMessagePrefix: String = "§7[§6外部书局§7] §f",

    @JsonProperty("console-message-prefix")
    val consoleMessagePrefix: String = "[ExternalBook] ",

    @JsonProperty("storage")
    val storage: StorageConfig = StorageConfig(),
) {
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        logging.logLevel = if (debug) LogLevel.DEBUG else LogLevel.INFO
        message.playerPrefix = playerMessagePrefix
        message.consolePrefix = consoleMessagePrefix
    }
}

enum class StorageMode {
    @JsonProperty("file")
    FILE,

    @JsonProperty("database")
    DATABASE
}

/**
 * 存储配置
 */
data class StorageConfig(
    @JsonProperty("mode")
    val mode: StorageMode = StorageMode.FILE,

    @JsonProperty("database-storage")
    val databaseStorage: DatabaseStorageConfig = DatabaseStorageConfig()
)

/**
 * MySQL存储配置
 */
data class DatabaseStorageConfig(
    @JsonProperty("table-name")
    val tableName: String = "books",

    @JsonProperty("table-prefix")
    val tablePrefix: String = "",
) : JacksonHikariCPConfig()