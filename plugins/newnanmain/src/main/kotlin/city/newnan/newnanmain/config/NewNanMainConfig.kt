package city.newnan.newnanmain.config

import city.newnan.core.config.CorePluginConfig
import city.newnan.core.logging.LogLevel
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

/**
 * NewNanMain插件配置类
 *
 * @author NewNanCity
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NewNanMainConfig(
    @JsonProperty("debug")
    val debug: Boolean = false,

    @JsonProperty("player-prefix")
    val playerPrefix: String = "§7[§6牛腩小镇§7] §f",

    @JsonProperty("console-prefix")
    val consolePrefix: String = "[NewNanMain] ",

    @JsonProperty("prefix-settings")
    val prefixSettings: PrefixSettings = PrefixSettings(),

    @JsonProperty("teleport-settings")
    val teleportSettings: TeleportSettings = TeleportSettings()
) {
    /**
     * 获取核心配置
     */
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        message.playerPrefix = playerPrefix
        message.consolePrefix = consolePrefix
    }
}

/**
 * 前缀设置配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrefixSettings(
    @JsonProperty("enable-auto-check")
    val enableAutoCheck: Boolean = true,

    @JsonProperty("check-on-join")
    val checkOnJoin: Boolean = true,

    @JsonProperty("check-on-world-change")
    val checkOnWorldChange: Boolean = true
)

/**
 * 传送设置配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TeleportSettings(
    @JsonProperty("enable-teleport")
    val enableTeleport: Boolean = true,

    @JsonProperty("teleport-delay")
    val teleportDelay: Int = 0,

    @JsonProperty("teleport-cooldown")
    val teleportCooldown: Int = 0
)

/**
 * 传送点数据类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TeleportPoint(
    @JsonProperty("name")
    val name: String,

    @JsonProperty("x")
    val x: Int,

    @JsonProperty("y")
    val y: Int,

    @JsonProperty("z")
    val z: Int,

    @JsonProperty("world")
    val world: String,

    @JsonProperty("icon")
    val icon: String,

    @JsonProperty("permission")
    val permission: String? = null
)

/**
 * 玩家前缀配置数据类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PlayerPrefixConfig(
    @JsonProperty("available")
    val available: MutableMap<String, String> = mutableMapOf(),

    @JsonProperty("current")
    var current: String? = null
)

/**
 * 全局前缀配置类型别名
 */
typealias GlobalPrefixConfig = MutableMap<String, MutableMap<String, String>>
