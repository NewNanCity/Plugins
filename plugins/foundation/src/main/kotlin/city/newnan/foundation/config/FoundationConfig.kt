package city.newnan.foundation.config

import city.newnan.config.database.JacksonHikariCPConfig
import city.newnan.core.config.CorePluginConfig
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.math.BigDecimal
import java.util.*

/**
 * Foundation插件配置
 *
 * 继承CorePluginConfig，提供完整的插件配置支持
 *
 * @author NewNanCity
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class FoundationConfig(
    /**
     * 基金目标账户
     * 如果设置，转账将进入此账户
     * 如果为空或null，插件将不会工作
     */
    @JsonProperty("target-account")
    val targetAccount: String? = null,

    /**
     * 玩家消息前缀
     */
    @JsonProperty("player-message-prefix")
    val playerMessagePrefix: String = "&7[&6牛腩基金&7] &f",

    /**
     * 控制台消息前缀
     */
    @JsonProperty("console-message-prefix")
    val consoleMessagePrefix: String = "[Foundation] ",

    /**
     * 转账检测配置
     */
    @JsonProperty("transfer-detection")
    val transferDetection: TransferDetectionConfig = TransferDetectionConfig(),

    /**
     * 数据存储配置
     */
    @JsonProperty("data-storage")
    val dataStorage: DataStorageConfig = DataStorageConfig(),
) {
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        message.playerPrefix = playerMessagePrefix
        message.consolePrefix = consoleMessagePrefix
    }
}

/**
 * 转账检测配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TransferDetectionConfig(
    /**
     * 转账匹配过期时间（毫秒）
     * 默认：5000ms (5秒)
     */
    @JsonProperty("expire-milliseconds")
    val expireMilliseconds: Long = 5000,

    /**
     * 是否启用转账检测
     */
    @JsonProperty("enabled")
    val enabled: Boolean = true,
)

enum class DataStorageMode {
    @JsonProperty("file")
    FILE,
    @JsonProperty("database")
    DATABASE
}

/**
 * 数据存储配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DataStorageConfig(
    /**
     * 存储模式
     */
    @JsonProperty("mode")
    val mode: DataStorageMode = DataStorageMode.FILE,

    /**
     * 文件存储配置
     */
    @JsonProperty("file-storage")
    val fileStorage: FileStorageConfig = FileStorageConfig(),

    /**
     * 数据库存储配置
     */
    @JsonProperty("database-storage")
    val databaseStorage: DatabaseStorageConfig = DatabaseStorageConfig()
)

/**
 * 文件存储配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class FileStorageConfig(
    /**
     * 数据文件名
     */
    @JsonProperty("data-file")
    val dataFile: String = "data.csv",

    /**
     * 拨款日志文件名
     */
    @JsonProperty("allocate-log-file")
    val allocateLogFile: String = "allocate.csv",

    /**
     * 自动保存间隔（tick）
     * 默认：1200 tick (60秒)
     */
    @JsonProperty("auto-save-interval-ticks")
    val autoSaveIntervalTicks: Long = 1200L,

    /**
     * 排行榜缓存过期时间（毫秒）
     * 默认：1800000ms (30分钟)
     */
    @JsonProperty("ranking-cache-expire-milliseconds")
    val rankingCacheExpireMilliseconds: Long = 1800000L
)

/**
 * 转账记录数据类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TransferRecord(
    /**
     * 玩家UUID
     */
    @JsonProperty("id")
    val id: UUID,

    /**
     * 主动转账金额
     */
    @JsonProperty("active")
    val active: BigDecimal,

    /**
     * 被动转账金额
     */
    @JsonProperty("passive")
    val passive: BigDecimal
)

/**
 * 转账记录CSV字符串版本（用于文件读写）
 */
@JsonPropertyOrder("id", "active", "passive")
data class TransferRecordStr(
    val id: String,
    val active: String,
    val passive: String
)

/**
 * 拨款日志记录数据类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AllocationLogRecord(
    /**
     * 拨款日期
     */
    @JsonProperty("date")
    val date: String,

    /**
     * 操作者UUID（管理员）
     */
    @JsonProperty("who")
    val who: String,

    /**
     * 目标玩家UUID
     */
    @JsonProperty("target")
    val target: UUID,

    /**
     * 拨款金额
     */
    @JsonProperty("amount")
    val amount: Double,

    /**
     * 拨款原因
     */
    @JsonProperty("reason")
    val reason: String
)

/**
 * 数据库存储配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DatabaseStorageConfig(
    /**
     * 转账记录表名
     */
    @JsonProperty("transfer-table-name")
    val transferTableName: String = "transfer_records",

    /**
     * 拨款日志表名
     */
    @JsonProperty("allocation-table-name")
    val allocationTableName: String = "allocation_logs",

    /**
     * 表前缀
     */
    @JsonProperty("table-prefix")
    val tablePrefix: String = "",

    /**
     * 批量保存间隔（tick）
     * 默认：200 tick (10秒)
     */
    @JsonProperty("batch-save-interval-ticks")
    val batchSaveIntervalTicks: Long = 200L,
) : JacksonHikariCPConfig()