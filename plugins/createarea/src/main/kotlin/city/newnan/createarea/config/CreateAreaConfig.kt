package city.newnan.createarea.config

import city.newnan.core.config.CorePluginConfig
import city.newnan.core.logging.LogLevel
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

/**
 * CreateArea插件配置类
 *
 * @author NewNanCity
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateAreaConfig(
    @JsonProperty("debug")
    val debug: Boolean = false,

    @JsonProperty("player-prefix")
    val playerPrefix: String = "§7[§6牛腩创造§7] §f",

    @JsonProperty("console-prefix")
    val consolePrefix: String = "[CreateArea] ",

    @JsonProperty("world")
    val world: String = "creative",

    @JsonProperty("default-group")
    val defaultGroup: String = "default",

    @JsonProperty("builder-group")
    val builderGroup: String = "builder",

    @JsonProperty("dynmap-settings")
    val dynmapSettings: DynmapSettings = DynmapSettings()
) {
    /**
     * 获取核心配置
     */
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        logging.logLevel = if (debug) LogLevel.DEBUG else LogLevel.INFO
        logging.fileLoggingEnabled = true
        logging.logFilePrefix = "CreateArea_"

        message.playerPrefix = playerPrefix
        message.consolePrefix = consolePrefix
    }
}

/**
 * Dynmap集成设置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DynmapSettings(
    @JsonProperty("enabled")
    val enabled: Boolean = true,

    @JsonProperty("marker-set-id")
    val markerSetId: String = "NewNanCity.CreateArea",

    @JsonProperty("marker-set-label")
    val markerSetLabel: String = "创造区"
)

/**
 * 创造区域数据类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateArea(
    @JsonProperty("x1")
    val x1: Int,

    @JsonProperty("z1")
    val z1: Int,

    @JsonProperty("x2")
    val x2: Int,

    @JsonProperty("z2")
    val z2: Int
)

/**
 * 创造区域集合类型别名
 */
typealias CreateAreas = Map<UUID, CreateArea>

/**
 * 2D范围数据类
 */
data class Range2D(val minX: Int, val minZ: Int, val maxX: Int, val maxZ: Int) {
    companion object {
        fun valueOf(x1: Int, x2: Int, z1: Int, z2: Int): Range2D {
            return Range2D(minOf(x1, x2), minOf(z1, z2), maxOf(x1, x2), maxOf(z1, z2))
        }
    }
}

/**
 * 带世界信息的2D范围
 */
data class Range2DWorld(val worldName: String, val range: Range2D)
