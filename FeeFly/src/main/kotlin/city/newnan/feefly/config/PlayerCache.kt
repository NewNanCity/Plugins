package city.newnan.feefly.config

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class PlayerCache(val players: Map<UUID, FlyingPlayer> = emptyMap())

/**
 * 飞行玩家
 */
data class FlyingPlayer(
    /**
     * 玩家开始飞行的时间，毫秒时间戳
     */
    @JsonProperty("start-time")
    val flyStartTimestamp: Long,
    /**
     * 玩家飞行之前的速度
     */
    @JsonProperty("previous-speed")
    val previousFlyingSpeed: Float
)