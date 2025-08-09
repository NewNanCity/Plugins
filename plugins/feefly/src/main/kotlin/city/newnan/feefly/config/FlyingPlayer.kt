package city.newnan.feefly.config

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

/**
 * 玩家缓存数据
 *
 * 用于持久化飞行玩家状态，支持服务器重启后恢复
 *
 * @author NewNanCity
 * @since 2.0.0
 */
data class PlayerCache(
    /**
     * 飞行玩家映射
     * Key: 玩家UUID
     * Value: 飞行玩家数据
     */
    @JsonProperty("players")
    val players: Map<UUID, FlyingPlayer> = emptyMap()
)

/**
 * 飞行玩家数据
 *
 * 记录玩家的飞行状态和相关信息
 *
 * @author NewNanCity
 * @since 2.0.0
 */
data class FlyingPlayer(
    /**
     * 玩家开始飞行的时间戳（毫秒）
     */
    @JsonProperty("start-time")
    val flyStartTimestamp: Long,

    /**
     * 玩家飞行之前的速度
     * 用于恢复玩家原始飞行速度
     */
    @JsonProperty("previous-speed")
    val previousFlyingSpeed: Float,

    /**
     * 玩家飞行之前是否允许飞行
     * 用于恢复玩家原始飞行状态
     */
    @JsonProperty("previous-allow-flight")
    val previousAllowFlight: Boolean = false,

    /**
     * 玩家飞行之前是否正在飞行
     * 用于恢复玩家原始飞行状态
     */
    @JsonProperty("previous-flying")
    val previousFlying: Boolean = false
) {
    /**
     * 计算飞行持续时间（毫秒）
     */
    fun getFlyDuration(): Long = System.currentTimeMillis() - flyStartTimestamp

    /**
     * 计算飞行持续时间（秒）
     */
    fun getFlyDurationSeconds(): Long = getFlyDuration() / 1000
}
