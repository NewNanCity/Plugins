package city.newnan.feefly.event

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * FeeFly插件事件基类
 *
 * 所有FeeFly相关事件的基础类
 *
 * @param player 相关玩家
 * @author NewNanCity
 * @since 2.0.0
 */
abstract class FeeFlyEvent(
    /**
     * 事件相关的玩家
     */
    val player: Player
) : Event() {

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}

/**
 * 玩家开始付费飞行事件
 *
 * 当玩家成功开始付费飞行时触发此事件
 *
 * @param player 开始飞行的玩家
 * @param isFree 是否为免费飞行（有feefly.free权限）
 * @param estimatedDuration 预估飞行时长（秒），基于当前余额计算，免费飞行为-1
 * @author NewNanCity
 * @since 2.0.0
 */
class FlyStartEvent(
    player: Player,
    /**
     * 是否为免费飞行
     */
    val isFree: Boolean,
    /**
     * 预估飞行时长（秒）
     * 基于当前余额和扣费速率计算
     * 免费飞行时为-1
     */
    val estimatedDuration: Int
) : FeeFlyEvent(player) {

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}

/**
 * 玩家结束付费飞行事件
 *
 * 当玩家的付费飞行结束时触发此事件
 *
 * @param player 结束飞行的玩家
 * @param reason 结束原因
 * @param duration 实际飞行时长（毫秒）
 * @param totalCost 总花费金额，免费飞行为0.0
 * @author NewNanCity
 * @since 2.0.0
 */
class FlyEndEvent(
    player: Player,
    /**
     * 飞行结束的原因
     */
    val reason: EndReason,
    /**
     * 实际飞行时长（毫秒）
     */
    val duration: Long,
    /**
     * 总花费金额
     */
    val totalCost: Double
) : FeeFlyEvent(player) {

    /**
     * 飞行结束原因枚举
     */
    enum class EndReason {
        /** 玩家主动关闭 */
        PLAYER_TOGGLE,
        /** 余额不足 */
        INSUFFICIENT_BALANCE,
        /** 玩家死亡 */
        PLAYER_DEATH,
        /** 游戏模式改变 */
        GAMEMODE_CHANGE,
        /** 世界切换权限丢失 */
        WORLD_CHANGE,
        /** 玩家退出服务器 */
        PLAYER_QUIT,
        /** 权限丢失 */
        PERMISSION_LOST,
        /** 插件禁用 */
        PLUGIN_DISABLE,
        /** 管理员强制关闭 */
        ADMIN_FORCE,
        /** 状态验证失败 */
        STATE_VALIDATION_FAILED,
        /** 其他原因 */
        OTHER
    }

    companion object {
        @JvmStatic
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

    override fun getHandlers(): HandlerList = Companion.handlers
}
