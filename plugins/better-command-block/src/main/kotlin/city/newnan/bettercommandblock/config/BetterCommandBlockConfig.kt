package city.newnan.bettercommandblock.config

import city.newnan.bettercommandblock.firewall.config.FirewallConfig
import city.newnan.core.config.CorePluginConfig
import city.newnan.core.logging.LogLevel
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * BetterCommandBlock 插件配置类
 *
 * 包含插件的所有配置选项，支持配置热重载和版本迁移。
 * 使用Jackson注解进行序列化/反序列化。
 *
 * @author NewNanCity
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BetterCommandBlockConfig(
    /**
     * 被禁止的命令列表
     * 这些命令将被阻止在命令方块中执行
     */
    @JsonProperty("blocked-commands")
    val blockedCommands: Set<String> = setOf(
        // 权限相关命令
        "op", "minecraft:op", "deop", "minecraft:deop",

        // 封禁相关命令
        "ban", "minecraft:ban", "banlist", "minecraft:banlist",
        "ban-ip", "minecraft:ban-ip", "pardon", "minecraft:pardon",
        "pardon-ip", "minecraft:pardon-ip",

        // 白名单相关命令
        "whitelist", "minecraft:whitelist",

        // 踢出玩家命令
        "kick", "minecraft:kick",

        // 服务器控制命令
        "stop", "minecraft:stop", "save-all", "minecraft:save-all",
        "save-off", "minecraft:save-off", "save-on", "minecraft:save-on",
        "reload", "minecraft:reload",

        // 调试和信息命令
        "debug", "minecraft:debug", "testfor", "minecraft:testfor",
        "seed", "minecraft:seed",

        // 游戏模式命令
        "defaultgamemode", "minecraft:defaultgamemode",

        // 数据包命令
        "datapack", "minecraft:datapack", "data", "minecraft:data",

        // 发布命令
        "publish", "minecraft:publish",

        // Bukkit/Paper 特定命令
        "timings", "bukkit:timings", "plugins", "bukkit:plugins",
        "pl", "bukkit:pl", "bukkit:reload", "rl", "bukkit:rl",
        "ver", "version", "bukkit:ver", "bukkit:version"
    ),

    /**
     * 调试模式
     * 启用时会输出更详细的日志信息
     */
    @JsonProperty("debug")
    val debug: Boolean = false,

    /**
     * 日志配置
     */
    @JsonProperty("logging")
    val logging: LoggingConfig = LoggingConfig(),

    /**
     * 消息配置
     */
    @JsonProperty("message")
    val message: MessageConfig = MessageConfig(),

    /**
     * 防火墙配置
     * 新的高性能命令方块防火墙配置
     */
    @JsonProperty("firewall")
    val firewall: FirewallConfig = FirewallConfig()
) {
    /**
     * 构建核心配置
     * 将插件配置转换为Core模块所需的配置格式
     */
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        // 日志配置
        logging.logLevel = if (debug) LogLevel.DEBUG else when (this@BetterCommandBlockConfig.logging.logLevel.uppercase()) {
            "DEBUG" -> LogLevel.DEBUG
            "INFO" -> LogLevel.INFO
            "WARN" -> LogLevel.WARN
            "ERROR" -> LogLevel.ERROR
            else -> LogLevel.INFO
        }
        logging.fileLoggingEnabled = this@BetterCommandBlockConfig.logging.fileLoggingEnabled
        logging.logFilePrefix = this@BetterCommandBlockConfig.logging.logFilePrefix

        // 消息配置
        message.playerPrefix = this@BetterCommandBlockConfig.message.playerPrefix
        message.consolePrefix = this@BetterCommandBlockConfig.message.consolePrefix
    }
}

/**
 * 日志配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class LoggingConfig(
    /**
     * 是否启用文件日志
     */
    @JsonProperty("file-logging-enabled")
    val fileLoggingEnabled: Boolean = true,

    /**
     * 日志文件前缀
     */
    @JsonProperty("log-file-prefix")
    val logFilePrefix: String = "BetterCommandBlock_",

    /**
     * 日志级别
     * 可选值：TRACE, DEBUG, INFO, WARN, ERROR
     */
    @JsonProperty("log-level")
    val logLevel: String = "INFO"
)

/**
 * 消息配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MessageConfig(
    /**
     * 玩家消息前缀
     */
    @JsonProperty("player-prefix")
    val playerPrefix: String = "§7[§6命令方块加强§7] §f",

    /**
     * 控制台消息前缀
     */
    @JsonProperty("console-prefix")
    val consolePrefix: String = "[BetterCommandBlock] "
)
