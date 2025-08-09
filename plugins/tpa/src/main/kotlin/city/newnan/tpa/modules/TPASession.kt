package city.newnan.tpa.modules

import org.bukkit.OfflinePlayer

/**
 * TPA传送会话数据类
 *
 * 表示一个传送请求的会话信息，包含请求者、目标玩家、传送方向等信息。
 *
 * @param id 会话唯一标识符
 * @param requester 发起请求的玩家
 * @param target 目标玩家
 * @param direction 传送方向
 * @param expiredTime 过期时间戳（毫秒）
 * @param createTime 创建时间戳（毫秒）
 *
 * @author AI Assistant
 * @since 2.0.0
 */
data class TPASession(
    val id: Long,
    val requester: OfflinePlayer,
    val target: OfflinePlayer,
    val direction: TPADirection,
    var expiredTime: Long,
    val createTime: Long = System.currentTimeMillis()
) {

    /**
     * 检查会话是否已过期
     */
    fun isExpired(): Boolean = System.currentTimeMillis() > expiredTime

    /**
     * 检查会话是否已被标记为无效（expiredTime设为0）
     */
    fun isInvalid(): Boolean = expiredTime == 0L

    /**
     * 标记会话为无效
     */
    fun markInvalid() {
        expiredTime = 0L
    }

    /**
     * 获取传送的源玩家（将要被传送的玩家）
     */
    fun getFromPlayer(): OfflinePlayer = when (direction) {
        TPADirection.REQUESTER_TO_TARGET -> requester
        TPADirection.TARGET_TO_REQUESTER -> target
    }

    /**
     * 获取传送的目标玩家（传送目的地的玩家）
     */
    fun getToPlayer(): OfflinePlayer = when (direction) {
        TPADirection.REQUESTER_TO_TARGET -> target
        TPADirection.TARGET_TO_REQUESTER -> requester
    }

    /**
     * 获取会话类型描述
     */
    fun getTypeDescription(): String = when (direction) {
        TPADirection.REQUESTER_TO_TARGET -> "tpa"
        TPADirection.TARGET_TO_REQUESTER -> "tpahere"
    }

    /**
     * 获取会话的剩余有效时间（秒）
     */
    fun getRemainingSeconds(): Long {
        val remaining = expiredTime - System.currentTimeMillis()
        return if (remaining > 0) remaining / 1000 else 0
    }

    override fun toString(): String {
        return "TPASession(id=$id, ${requester.name}->${target.name}, type=${getTypeDescription()}, expired=${isExpired()})"
    }
}