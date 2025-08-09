package city.newnan.core.config

import city.newnan.core.logging.LogLevel
import city.newnan.core.utils.text.ComponentParseMode

/**
 * Core插件配置基类
 *
 * 提供核心功能的配置模板，避免直接依赖Bukkit的config系统。
 * 用户可以继承此类并提供自己的配置实现。
 */
open class CorePluginConfig {

    /**
     * 日志配置
     */
    open class LoggingConfig {
        /**
         * 日志等级
         * 默认值：INFO
         */
        open var logLevel: LogLevel = LogLevel.INFO

        /**
         * 是否启用文件日志
         * 默认值：false（插件默认只输出到服务器的logs/latest.log）
         * 注意：设置为true会在插件目录下创建logs文件夹和日志文件
         */
        open var fileLoggingEnabled: Boolean = false

        /**
         * 日志文件保留天数
         * 默认值：0，即无限期保留
         */
        open var logRetentionDays: Int = 0

        /**
         * 日志文件名前缀
         * 默认值：无
         */
        open var logFilePrefix: String = ""

        /**
         * 日志文件类型
         */
        enum class LogFileType {
            TEXT, JSONL
        }

        /**
         * 日志文件类型
         * 默认值：TEXT
         */
        open var logFileType: LogFileType = LogFileType.TEXT
    }

    /**
     * 消息管理配置
     */
    open class MessageConfig {
        /**
         * 玩家消息前缀
         * 默认值：空字符串
         */
        open var playerPrefix: String = ""

        /**
         * 控制台消息前缀
         * 默认值：空字符串（将使用插件名称）
         */
        open var consolePrefix: String = ""

        /**
         * 默认的消息解析模式
         * 默认值：Auto，即同时支持Legacy和MiniMessage
         */
        open var defaultParser: ComponentParseMode = ComponentParseMode.Auto
    }

    /**
     * 获取日志配置
     */
    open fun getLoggingConfig(): LoggingConfig = LoggingConfig()

    /**
     * 获取消息管理配置
     */
    open fun getMessageConfig(): MessageConfig = MessageConfig()

    companion object {
        fun build(builder: (DefaultCorePluginConfig.() -> Unit)): DefaultCorePluginConfig {
            return DefaultCorePluginConfig().apply(builder)
        }
    }
}

data class DefaultCorePluginConfig(
    val logging: LoggingConfig = LoggingConfig(),
    val message: MessageConfig = MessageConfig()
) : CorePluginConfig() {
    override fun getLoggingConfig(): LoggingConfig = logging
    override fun getMessageConfig(): MessageConfig = message
}