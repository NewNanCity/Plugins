package city.newnan.feefly

/**
 * 飞行玩家
 */
class FlyingPlayer internal constructor(
    /**
     * 玩家开始飞行的时间，毫秒时间戳
     */
    var flyStartTimestamp: Long,
    /**
     * 玩家飞行之前的速度
     */
    var previousFlyingSpeed: Float
)