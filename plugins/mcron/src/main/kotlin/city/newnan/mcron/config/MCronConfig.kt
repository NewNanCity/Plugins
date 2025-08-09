package city.newnan.mcron.config

import city.newnan.core.config.CorePluginConfig
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.ZoneOffset

/**
 * MCron插件配置
 *
 * 继承CorePluginConfig，提供完整的插件配置支持
 *
 * @author NewNanCity
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MCronConfig(
    @JsonProperty("player-message-prefix")
    val playerMessagePrefix: String = "&7[&6MCron&7] &f",

    @JsonProperty("console-message-prefix")
    val consoleMessagePrefix: String = "[MCron] ",

    @JsonProperty("file-logging")
    val fileLogging: Boolean = false,

    @JsonProperty("timezone")
    val timezone: TimezoneConfig = TimezoneConfig(),

    @JsonProperty("tasks")
    val tasks: TasksConfig = TasksConfig(),
) {
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        message.playerPrefix = playerMessagePrefix
        message.consolePrefix = consoleMessagePrefix
        logging.fileLoggingEnabled = fileLogging
        if (fileLogging) {
            logging.logFilePrefix = "mcron_"
            logging.logFileType = CorePluginConfig.LoggingConfig.LogFileType.JSONL
        }
    }
}

/**
 * 时区配置
 */
data class TimezoneConfig(
    @JsonProperty("offset")
    val offset: String = "+8",

    @JsonProperty("auto-detect")
    val autoDetect: Boolean = false
) {
    /**
     * 获取ZoneOffset对象
     */
    fun getZoneOffset(): ZoneOffset {
        val currentOffset = if (autoDetect) ZoneOffset.systemDefault().id else offset
        return try {
            when {
                currentOffset == "Z" -> ZoneOffset.UTC
                currentOffset.matches(Regex("[+-]\\d{1,2}")) -> {
                    val hours = currentOffset.toInt()
                    ZoneOffset.ofHours(hours)
                }
                currentOffset.matches(Regex("[+-]\\d{2}:\\d{2}")) -> {
                    ZoneOffset.of(currentOffset)
                }
                currentOffset.matches(Regex("[+-]\\d{4}")) -> {
                    val hours = currentOffset.substring(1, 3).toInt()
                    val minutes = currentOffset.substring(3, 5).toInt()
                    val totalMinutes = hours * 60 + minutes
                    if (currentOffset.startsWith("-")) {
                        ZoneOffset.ofTotalSeconds(-totalMinutes * 60)
                    } else {
                        ZoneOffset.ofTotalSeconds(totalMinutes * 60)
                    }
                }
                else -> ZoneOffset.of(currentOffset)
            }
        } catch (e: Exception) {
            ZoneOffset.ofHours(8) // 默认东八区
        }
    }
}

/**
 * 任务配置
 */
data class TasksConfig(
    @JsonProperty("on-server-ready")
    val onServerReady: Map<String, List<String>> = emptyMap(),

    @JsonProperty("on-plugin-enable")
    val onPluginEnable: Map<String, List<String>> = emptyMap(),

    @JsonProperty("on-plugin-disable")
    val onPluginDisable: Map<String, List<String>> = emptyMap(),

    @JsonProperty("scheduled-tasks")
    val scheduledTasks: Map<String, List<String>> = mapOf(
        "0 30 8 * * MON-FRI" to listOf("broadcast 工作日早上好！"),
        "0 30 8 * * SAT,SUN" to listOf("broadcast 周末早上好！"),
        "0 0 0 1 1 *" to listOf("broadcast 新年快乐！")
    )
)