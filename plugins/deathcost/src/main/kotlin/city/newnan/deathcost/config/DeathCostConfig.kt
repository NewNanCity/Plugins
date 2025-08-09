package city.newnan.deathcost.config

import city.newnan.core.config.CorePluginConfig
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DeathCost插件配置
 *
 * 继承CorePluginConfig，提供完整的插件配置支持
 *
 * @author NewNanCity
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DeathCostConfig(
    /**
     * 玩家消息前缀
     */
    @JsonProperty("player-message-prefix")
    val playerMessagePrefix: String = "&7[&6牛腩小镇&7] &f",

    /**
     * 控制台消息前缀
     */
    @JsonProperty("console-message-prefix")
    val consoleMessagePrefix: String = "[DeathCost] ",

    /**
     * 死亡扣费配置
     */
    @JsonProperty("death-cost")
    val deathCost: DeathCostSettings = DeathCostSettings(),

    /**
     * 死亡消息配置
     */
    @JsonProperty("death-message")
    val deathMessage: DeathMessageSettings = DeathMessageSettings(),

) {
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        message.playerPrefix = playerMessagePrefix
        message.consolePrefix = consoleMessagePrefix
    }
}

/**
 * 死亡扣费设置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DeathCostSettings(
    /**
     * 目标转账账户
     * 如果设置，扣除的费用将转入此账户
     * 如果为空或null，费用将直接消失
     */
    @JsonProperty("target-account")
    val targetAccount: String? = null,

    /**
     * 是否使用简单模式
     * true: 使用简单模式（固定扣费）
     * false: 使用复杂模式（阶梯扣费）
     */
    @JsonProperty("use-simple-mode")
    val useSimpleMode: Boolean = false,

    /**
     * 简单模式配置
     */
    @JsonProperty("simple-mode")
    val simpleMode: SimpleModeConfig? = SimpleModeConfig(),

    /**
     * 复杂模式配置（阶梯扣费）
     */
    @JsonProperty("complex-mode")
    val complexMode: List<CostStage>? = listOf(
        CostStage(5000.0, 0.0, false),
        CostStage(10000.0, 20.0, false),
        CostStage(50000.0, 0.001, true),
        CostStage(-1.0, 0.0024, true)
    )
)

/**
 * 简单模式配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SimpleModeConfig(
    /**
     * 扣费金额
     */
    @JsonProperty("cost")
    val cost: Double = 50.0,

    /**
     * 是否按百分比扣费
     * true: cost为百分比（0.01 = 1%）
     * false: cost为固定金额
     */
    @JsonProperty("if-percent")
    val ifPercent: Boolean = false
)

/**
 * 扣费阶梯
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CostStage(
    /**
     * 阶梯上限
     * -1.0 表示无上限
     */
    @JsonProperty("max")
    val max: Double,

    /**
     * 扣费金额或百分比
     */
    @JsonProperty("cost")
    val cost: Double,

    /**
     * 是否按百分比扣费
     */
    @JsonProperty("if-percent")
    val ifPercent: Boolean = false
)

/**
 * 死亡消息设置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DeathMessageSettings(
    /**
     * 是否向玩家发送死亡消息
     */
    @JsonProperty("player-enable")
    val playerEnable: Boolean = true,

    /**
     * 是否广播死亡消息
     */
    @JsonProperty("broadcast-enable")
    val broadcastEnable: Boolean = false,

    /**
     * 是否向控制台发送死亡消息
     */
    @JsonProperty("console-enable")
    val consoleEnable: Boolean = false
)
