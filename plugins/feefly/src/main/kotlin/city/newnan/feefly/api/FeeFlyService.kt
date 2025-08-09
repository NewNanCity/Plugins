package city.newnan.feefly.api

import city.newnan.feefly.config.FlyingPlayer
import org.bukkit.entity.Player

/**
 * FeeFly插件服务接口
 *
 * 通过Bukkit ServicesManager提供的标准API接口
 * 其他插件可以通过以下方式获取：
 * ```kotlin
 * val feeFlyService = server.servicesManager.getRegistration(FeeFlyService::class.java)?.provider
 * ```
 *
 * @author NewNanCity
 * @since 2.0.0
 */
interface FeeFlyService {

    /**
     * 为玩家开启付费飞行
     *
     * @param player 目标玩家
     * @return 如果成功开启返回true，否则返回false
     */
    fun startFly(player: Player): Boolean

    /**
     * 为玩家关闭付费飞行
     *
     * @param player 目标玩家
     * @return 如果成功关闭返回true，如果玩家本来就不在飞行返回false
     */
    fun cancelFly(player: Player): Boolean

    /**
     * 切换玩家的飞行状态
     *
     * @param player 目标玩家
     * @return 操作后的飞行状态，true表示正在飞行，false表示未飞行
     */
    fun toggleFly(player: Player): Boolean

    /**
     * 检查玩家是否正在使用付费飞行
     *
     * @param player 目标玩家
     * @return 如果玩家正在使用付费飞行返回true
     */
    fun isFlying(player: Player): Boolean

    /**
     * 获取玩家的飞行信息
     *
     * @param player 目标玩家
     * @return 飞行信息，如果玩家未在飞行返回null
     */
    fun getFlyingInfo(player: Player): FlyingPlayer?

    /**
     * 获取所有正在飞行的玩家
     *
     * @return 正在飞行的玩家映射表，键为玩家，值为飞行信息
     */
    fun getFlyingPlayers(): Map<Player, FlyingPlayer>

    /**
     * 获取玩家剩余飞行时间（秒）
     *
     * @param player 目标玩家
     * @return 剩余飞行时间（秒），如果玩家未在飞行或有免费权限返回null
     */
    fun getRemainingTime(player: Player): Int?

    /**
     * 获取玩家已飞行时间（毫秒）
     *
     * @param player 目标玩家
     * @return 已飞行时间（毫秒），如果玩家未在飞行返回null
     */
    fun getFlyDuration(player: Player): Long?

    /**
     * 获取玩家总花费金额
     *
     * @param player 目标玩家
     * @return 总花费金额，如果玩家未在飞行或有免费权限返回null
     */
    fun getTotalCost(player: Player): Double?

    /**
     * 检查玩家是否有免费飞行权限
     *
     * @param player 目标玩家
     * @return 如果有免费飞行权限返回true
     */
    fun hasFreePermission(player: Player): Boolean

    /**
     * 获取当前飞行配置信息
     *
     * @return 包含飞行配置的映射表
     */
    fun getFlyConfig(): Map<String, Any>

    /**
     * 检查服务是否可用
     *
     * @return 如果服务可用返回true
     */
    fun isAvailable(): Boolean
}
