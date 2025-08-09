package city.newnan.railarea.config

import city.newnan.core.config.CorePluginConfig
import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.Sound

/**
 * RailArea插件配置
 *
 * @author NewNanCity
 * @since 2.0.0
 */
data class RailAreaConfig(
    /**
     * 插件版本
     */
    @JsonProperty("version")
    val version: String = "2.0.0",

    /**
     * 玩家消息前缀
     */
    @JsonProperty("player-message-prefix")
    val playerMessagePrefix: String = "§7[§6RailArea§7] §f",

    /**
     * 控制台消息前缀
     */
    @JsonProperty("console-message-prefix")
    val consoleMessagePrefix: String = "[RailArea] ",

    /**
     * 世界大小配置
     */
    @JsonProperty("world-size")
    val worldSize: Map<String, WorldSizeConfig> = mapOf(
        "world" to WorldSizeConfig(-10000, -10000, 10000, 10000)
    ),

    /**
     * 等待时间（秒）
     */
    @JsonProperty("waiting-second")
    val waitingSeconds: Int = 5,

    /**
     * 发车警告音效
     */
    @JsonProperty("start-warning-sound")
    val startWarningSound: Sound = Sound.BLOCK_NOTE_BLOCK_BIT,

    /**
     * 发车警告时间（秒）
     */
    @JsonProperty("start-warning-second")
    val startWarningSeconds: Int = 3,

    /**
     * 发车速度
     */
    @JsonProperty("start-speed")
    val startSpeed: Double = 1.0,

    /**
     * 到站音乐
     */
    @JsonProperty("arrive-music")
    val arriveMusic: List<MusicNote> = listOf(
        MusicNote(0, "F3", Sound.BLOCK_NOTE_BLOCK_PLING, 5),
        MusicNote(6, "C♯3", Sound.BLOCK_NOTE_BLOCK_PLING, 5),
        MusicNote(12, "D♯3", Sound.BLOCK_NOTE_BLOCK_PLING, 5),
        MusicNote(18, "G♯2", Sound.BLOCK_NOTE_BLOCK_PLING, 5)
    ),

    /**
     * 发车音乐
     */
    @JsonProperty("start-music")
    val startMusic: List<MusicNote> = listOf(
        MusicNote(0, "C♯3", Sound.BLOCK_NOTE_BLOCK_PLING, 5),
        MusicNote(6, "F3", Sound.BLOCK_NOTE_BLOCK_PLING, 5),
        MusicNote(12, "G♯3", Sound.BLOCK_NOTE_BLOCK_PLING, 5),
        MusicNote(18, "C♯4", Sound.BLOCK_NOTE_BLOCK_PLING, 5)
    ),
) {
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        message.playerPrefix = playerMessagePrefix
        message.consolePrefix = consoleMessagePrefix
    }
}

/**
 * 世界大小配置
 */
data class WorldSizeConfig(
    @JsonProperty("x1") val x1: Int,
    @JsonProperty("z1") val z1: Int,
    @JsonProperty("x2") val x2: Int,
    @JsonProperty("z2") val z2: Int
)

/**
 * 音乐音符
 */
data class MusicNote(
    @JsonProperty("tick") val tick: Int,
    @JsonProperty("note") val note: String,
    @JsonProperty("sound") val sound: Sound,
    @JsonProperty("volume") val volume: Int
)