package city.newnan.mcpatch.config

import city.newnan.core.config.CorePluginConfig
import city.newnan.core.logging.LogLevel
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType

/**
 * MCPatch 插件配置类
 *
 * 包含插件的所有配置选项，支持配置热重载和版本迁移。
 * 使用Jackson注解进行序列化/反序列化。
 *
 * @author NewNanCity
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MCPatchConfig(
    /**
     * 模块启用配置
     */
    @JsonProperty("modules")
    val modules: ModulesConfig = ModulesConfig(),

    /**
     * 日志配置
     */
    @JsonProperty("logging")
    val logging: LoggingConfig = LoggingConfig(),

    /**
     * 消息配置
     */
    @JsonProperty("message")
    val message: MessageConfig = MessageConfig()
) {
    /**
     * 构建核心配置
     * 将插件配置转换为Core模块所需的配置格式
     */
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        // 日志配置
        logging.logLevel = when (this@MCPatchConfig.logging.logLevel.uppercase()) {
            "DEBUG" -> LogLevel.DEBUG
            "INFO" -> LogLevel.INFO
            "WARN" -> LogLevel.WARN
            "ERROR" -> LogLevel.ERROR
            else -> LogLevel.INFO
        }
        logging.fileLoggingEnabled = this@MCPatchConfig.logging.fileLoggingEnabled
        logging.logFilePrefix = this@MCPatchConfig.logging.logFilePrefix

        // 消息配置
        message.playerPrefix = this@MCPatchConfig.message.playerPrefix
        message.consolePrefix = this@MCPatchConfig.message.consolePrefix
    }
}

/**
 * 模块配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ModulesConfig(
    /**
     * 反世界下载器模块配置
     */
    @JsonProperty("anti-world-download")
    val antiWorldDownload: AntiWorldDownloadConfig = AntiWorldDownloadConfig(),

    /**
     * 违禁物品模块配置
     */
    @JsonProperty("contraband")
    val contraband: ContrabandConfig = ContrabandConfig(),

    /**
     * 反崩服模块配置
     */
    @JsonProperty("anti-crash")
    val antiCrash: AntiCrashConfig = AntiCrashConfig(),
)

/**
 * 反世界下载器配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AntiWorldDownloadConfig(
    @JsonProperty("enabled")
    val enabled: Boolean = true,

    @JsonProperty("log-violations")
    val logViolations: Boolean = true
)

/**
 * 违禁物品配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ContrabandConfig(
    @JsonProperty("enabled")
    val enabled: Boolean = true,

    @JsonProperty("blocked-materials")
    val blockedMaterials: Set<Material> = setOf(
        Material.BEDROCK,
        Material.BARRIER,
        Material.END_CRYSTAL,
        Material.END_PORTAL,
        Material.END_PORTAL_FRAME,
        Material.END_GATEWAY,
        Material.COMMAND_BLOCK,
        Material.CHAIN_COMMAND_BLOCK,
        Material.REPEATING_COMMAND_BLOCK,
        Material.STRUCTURE_BLOCK,
        Material.JIGSAW,
        Material.AIR,
        Material.WATER,
        Material.LAVA
    ),

    @JsonProperty("auto-remove")
    val autoRemove: Boolean = true
)

/**
 * 反崩服配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AntiCrashConfig(
    @JsonProperty("dispenser-protection")
    val dispenserProtection: Boolean = true
)

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
    val logFilePrefix: String = "MCPatch_Security_",

    /**
     * 日志级别
     * 可选值：DEBUG, INFO, WARN, ERROR
     */
    @JsonProperty("log-level")
    val logLevel: String = "INFO",

    /**
     * 是否启用实时监控
     */
    @JsonProperty("real-time-monitoring")
    val realTimeMonitoring: Boolean = true
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
    val playerPrefix: String = "§7[§cMCPatch§7] §f",

    /**
     * 控制台消息前缀
     */
    @JsonProperty("console-prefix")
    val consolePrefix: String = "[MCPatch] "
)
