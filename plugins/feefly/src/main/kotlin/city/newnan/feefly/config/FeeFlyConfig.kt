package city.newnan.feefly.config

import city.newnan.core.config.CorePluginConfig
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * FeeFly插件配置
 *
 * 继承CorePluginConfig，提供完整的插件配置支持
 *
 * @author NewNanCity
 * @since 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class FeeFlyConfig(
    /**
     * 玩家消息前缀
     */
    @JsonProperty("player-message-prefix")
    val playerMessagePrefix: String = "&7[&6牛腩飞行&7] &f",

    /**
     * 控制台消息前缀
     */
    @JsonProperty("console-message-prefix")
    val consoleMessagePrefix: String = "[FeeFly] ",

    /**
     * 目标转账账户
     * 如果设置，扣除的费用将转入此账户
     * 如果为空或null，费用将直接消失
     */
    @JsonProperty("target-account")
    val targetAccount: String? = null,

    /**
     * 飞行配置
     */
    @JsonProperty("flying")
    val flying: FlyingConfig = FlyingConfig(),
) {
    fun getCoreConfig(): CorePluginConfig = CorePluginConfig.build {
        message.playerPrefix = playerMessagePrefix
        message.consolePrefix = consoleMessagePrefix
    }
}

/**
 * 飞行相关配置
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class FlyingConfig(
    /**
     * 每次扣费的间隔（tick）
     * 默认：20 tick（1秒）
     */
    @JsonProperty("tick-per-count")
    val tickPerCount: Long = 20,

    /**
     * 每次扣费的金额
     * 默认：0.3
     */
    @JsonProperty("cost-per-count")
    val costPerCount: Double = 0.3,

    /**
     * 飞行速度
     * 默认：0.05f（正常飞行速度的一半）
     */
    @JsonProperty("fly-speed")
    val flySpeed: Float = 0.05f,

    /**
     * 低余额警告阈值（秒）
     * 当剩余飞行时间少于此值时显示警告
     * 默认：60秒
     */
    @JsonProperty("low-balance-warning-seconds")
    val lowBalanceWarningSeconds: Int = 60,

    /**
     * 命令使用冷却时间（秒）
     * 防止玩家恶意频繁使用命令
     * 默认：3秒
     */
    @JsonProperty("command-cooldown-seconds")
    val commandCooldownSeconds: Int = 3,

    /**
     * 状态验证间隔（秒）
     * 定期验证飞行玩家状态完整性的间隔
     * 默认：30秒
     */
    @JsonProperty("state-validation-interval-seconds")
    val stateValidationIntervalSeconds: Int = 30
)