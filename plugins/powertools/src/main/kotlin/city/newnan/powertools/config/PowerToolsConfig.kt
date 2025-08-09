package city.newnan.powertools.config

import city.newnan.core.config.CorePluginConfig
import city.newnan.core.logging.LogLevel
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * PowerTools插件配置类
 *
 * @author NewNanCity
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PowerToolsConfig(
    @JsonProperty("debug")
    val debug: Boolean = false,

    @JsonProperty("player-prefix")
    val playerPrefix: String = "§7[§6PowerTools§7] §f",

    @JsonProperty("console-prefix")
    val consolePrefix: String = "[PowerTools] ",

    @JsonProperty("skull-settings")
    val skullSettings: SkullSettings = SkullSettings()
) {
    /**
     * 获取核心配置
     */
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        logging.logLevel = if (debug) LogLevel.DEBUG else LogLevel.INFO
        message.playerPrefix = playerPrefix
        message.consolePrefix = consolePrefix
    }
}

/**
 * 头颅设置配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SkullSettings(
    @JsonProperty("max-skulls-per-command")
    val maxSkullsPerCommand: Int = 64,

    @JsonProperty("enable-url-skulls")
    val enableUrlSkulls: Boolean = true,

    @JsonProperty("enable-player-skulls")
    val enablePlayerSkulls: Boolean = true
)
