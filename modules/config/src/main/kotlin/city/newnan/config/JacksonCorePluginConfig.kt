package city.newnan.config

import city.newnan.core.config.CorePluginConfig
import city.newnan.core.logging.LogLevel
import city.newnan.core.utils.text.ComponentParseMode
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 带有Jackson字段映射的CorePluginConfig版本
 *
 * 提供了完整的Jackson注解支持，可以直接用于配置文件序列化和反序列化。
 * 插件可以选择以下三种方式使用：
 *
 * 1. **继承方式**: 插件配置类直接继承此类
 * 2. **组合方式**: 插件配置类包含core字段
 * 3. **自定义方式**: 插件自定义配置，getCoreConfig()时临时构造
 *
 * @author Gk0Wk
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
open class JacksonCorePluginConfig(
    /**
     * 日志配置
     */
    @JsonProperty("logging")
    open val logging: JacksonLoggingConfig = JacksonLoggingConfig(),

    /**
     * 消息配置
     */
    @JsonProperty("message")
    open val message: JacksonMessageConfig = JacksonMessageConfig(),
) : CorePluginConfig() {

    override fun getLoggingConfig(): LoggingConfig = logging

    override fun getMessageConfig(): MessageConfig = message
}

/**
 * 带有Jackson注解的日志配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
open class JacksonLoggingConfig(
    /**
     * 日志等级
     * 默认值：INFO
     */
    @JsonProperty("log-level")
    override var logLevel: LogLevel = LogLevel.INFO,

    /**
     * 是否启用文件日志
     */
    @JsonProperty("file-logging")
    override var fileLoggingEnabled: Boolean = false,

    /**
     * 日志文件保留天数
     */
    @JsonProperty("log-retention-days")
    override var logRetentionDays: Int = 0,

    /**
     * 日志文件名前缀
     */
    @JsonProperty("log-file-prefix")
    override var logFilePrefix: String = "",

    /**
     * 日志文件类型
     */
    @JsonProperty("log-file-type")
    override var logFileType: LogFileType = LogFileType.TEXT
) : CorePluginConfig.LoggingConfig()

/**
 * 带有Jackson注解的消息配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
open class JacksonMessageConfig(
    /**
     * 玩家消息前缀
     */
    @JsonProperty("player-prefix")
    override var playerPrefix: String = "",

    /**
     * 控制台消息前缀
     */
    @JsonProperty("console-prefix")
    override var consolePrefix: String = "",

    /**
     * 默认的消息解析模式
     * 默认值：Auto，即同时支持Legacy和MiniMessage
     */
    @JsonProperty("default-parser")
    override var defaultParser: ComponentParseMode = ComponentParseMode.Auto
) : CorePluginConfig.MessageConfig()
