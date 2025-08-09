package city.newnan.tpa.config

import city.newnan.core.config.CorePluginConfig
import city.newnan.core.logging.LogLevel
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * TPA插件配置类
 * 
 * 包含所有TPA相关的配置项，支持热重载和配置验证。
 * 
 * @author AI Assistant
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TPAConfig(
    
    @JsonProperty("enable")
    val enable: Boolean = true,
    
    @JsonProperty("cool-down-seconds")
    val coolDownSeconds: Int = 15,
    
    @JsonProperty("delay-seconds") 
    val delaySeconds: Int = 3,
    
    @JsonProperty("expired-seconds")
    val expiredSeconds: Int = 60,
    
    @JsonProperty("exclude-worlds")
    val excludeWorlds: Set<String> = emptySet(),
    
    @JsonProperty("debug")
    val debug: Boolean = false,
    
    @JsonProperty("player-prefix")
    val playerPrefix: String = "§7[§6TPA§7] §f",

    @JsonProperty("console-prefix")
    val consolePrefix: String = "[TPA] ",
    
) {
    
    /**
     * 获取核心配置
     * 使用推荐的CorePluginConfig.build DSL模式
     */
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        // 日志配置
        logging.logLevel = if (debug) LogLevel.DEBUG else LogLevel.INFO
        
        // 消息配置
        message.playerPrefix = playerPrefix
        message.consolePrefix = consolePrefix
    }
}