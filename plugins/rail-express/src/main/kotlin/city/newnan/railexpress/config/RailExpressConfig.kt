package city.newnan.railexpress.config

import city.newnan.core.config.CorePluginConfig
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.bukkit.Material

/**
 * RailExpress插件配置
 *
 * 继承CorePluginConfig，提供完整的插件配置支持
 *
 * @author NewNanCity
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class RailExpressConfig(
    @JsonProperty("version")
    val version: String = "2.0.0",

    @JsonProperty("player-message-prefix")
    val playerMessagePrefix: String = "§7[§6RailExpress§7] §r",

    @JsonProperty("console-message-prefix")
    val consoleMessagePrefix: String = "[RailExpress] ",

    @JsonProperty("groups")
    val groups: List<WorldGroupConfig> = emptyList()
) {
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        message.playerPrefix = playerMessagePrefix
        message.consolePrefix = consoleMessagePrefix
    }
}

/**
 * 世界组配置
 */
data class WorldGroupConfig(
    @JsonProperty("worlds")
    val worlds: List<String>,

    @JsonProperty("allow-non-player")
    val allowNonPlayer: Boolean = false,

    @JsonProperty("power-rail-only")
    val powerRailOnly: Boolean = true,

    @JsonProperty("block-type")
    private val blockType: Map<String, Double> = emptyMap()
) {
    /**
     * 方块速度映射（转换为Material类型）
     */
    val blockSpeedMap: Map<Material, Double> by lazy {
        blockType.mapKeys { (materialName, _) ->
            try {
                Material.valueOf(materialName.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }.filterKeys { it != null }.mapKeys { it.key!! }
    }
}