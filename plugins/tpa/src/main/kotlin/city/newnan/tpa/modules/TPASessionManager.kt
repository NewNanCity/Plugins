package city.newnan.tpa.modules

import city.newnan.core.base.BaseModule
import city.newnan.core.scheduler.runAsyncRepeating
import city.newnan.core.scheduler.runSync
import city.newnan.tpa.TPAPlugin
import city.newnan.tpa.modules.TPASession
import city.newnan.tpa.i18n.LanguageKeys
import city.newnan.tpa.modules.TPAMessageSender
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayDeque

/**
 * TPA会话管理器
 *
 * 负责管理所有的TPA传送请求会话，包括：
 * - 创建和存储会话
 * - 自动清理过期会话
 * - 处理会话的接受和拒绝
 * - 冷却时间管理
 *
 * @author AI Assistant
 * @since 2.0.0
 */
class TPASessionManager(
    moduleName: String,
    val plugin: TPAPlugin
) : BaseModule(moduleName, plugin) {

    // ===== 会话存储 =====
    private val sessions = ConcurrentHashMap<Long, TPASession>() // 随机访问O(1)
    private val sessionQueue = ArrayDeque<TPASession>()          // 插入有序，增减O(1)
    private val sessionIdGenerator = AtomicLong(0)

    // ===== 冷却时间管理 =====
    private val coolingDownPlayers = ConcurrentHashMap<UUID, Long>()

    // ===== 消息发送器 =====
    private val messageSender = TPAMessageSender(plugin)

    // ===== 必须：手动调用init()来触发初始化 =====
    init { init() }

    override fun onInit() {
        logger.info(LanguageKeys.Log.Info.MODULE_INITIALIZED, moduleName)

        // 启动定期清理任务
        runAsyncRepeating(0L, 20L * 10) { // 每10秒清理一次
            cleanupExpiredSessions()
        }

        // 启动冷却时间清理任务
        runAsyncRepeating(0L, 20L * 30) { // 每30秒清理一次
            cleanupExpiredCooldowns()
        }
    }

    override fun onReload() {
        // 会话管理器重载时不需要清理现有会话，让它们自然过期
    }

    // ===== 会话管理 =====

    /**
     * 创建新的传送请求会话
     *
     * @param requester 发起请求的玩家
     * @param target 目标玩家
     * @param direction 传送方向
     * @return 创建的会话，如果创建失败则返回null
     */
    fun createSession(requester: Player, target: Player, direction: TPADirection): TPASession? {
        val config = plugin.getPluginConfig()

        // 检查冷却时间
        if (!requester.hasPermission("tpa.no-cool-down")) {
            val cooldownEnd = coolingDownPlayers[requester.uniqueId]
            if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
                val remainingSeconds = (cooldownEnd - System.currentTimeMillis()) / 1000
                plugin.messager.printf(requester, LanguageKeys.Commands.TPA.COOLDOWN_ACTIVE, remainingSeconds)
                return null
            }
        }

        // 检查屏蔽状态
        if (plugin.getBlockManager().isBlocked(target, requester)) {
            plugin.messager.printf(requester, LanguageKeys.Commands.TPA.TARGET_BLOCKED_YOU)
            return null
        }

        // 检查世界限制
        if (config.excludeWorlds.contains(requester.world.name)) {
            plugin.messager.printf(requester, LanguageKeys.Commands.TPA.REQUESTER_WORLD_BLOCKED)
            return null
        }

        if (config.excludeWorlds.contains(target.world.name)) {
            plugin.messager.printf(requester, LanguageKeys.Commands.TPA.TARGET_WORLD_BLOCKED)
            return null
        }

        // 设置冷却时间
        if (!requester.hasPermission("tpa.no-cool-down") && config.coolDownSeconds > 0) {
            coolingDownPlayers[requester.uniqueId] = System.currentTimeMillis() + (config.coolDownSeconds * 1000L)
        }

        // 创建会话
        val sessionId = sessionIdGenerator.incrementAndGet()
        val expiredTime = System.currentTimeMillis() + (config.expiredSeconds * 1000L)
        val session = TPASession(sessionId, requester, target, direction, expiredTime)

        // 存储会话
        sessions[sessionId] = session
        sessionQueue.addLast(session)

        // 发送请求消息给目标玩家
        messageSender.sendRequestMessage(session)

        return session
    }

    /**
     * 接受传送请求
     *
     * @param sessionId 会话ID
     * @param responser 响应的玩家
     * @return 处理的会话，如果处理失败则返回null
     */
    fun acceptSession(sessionId: Long, responser: Player): TPASession? {
        val session = sessions[sessionId] ?: return null

        // 验证响应者
        if (session.target.uniqueId != responser.uniqueId) {
            plugin.messager.printf(responser, LanguageKeys.Commands.Response.NOT_YOUR_REQUEST)
            return null
        }

        // 检查会话状态
        if (session.isInvalid()) return null
        if (session.isExpired()) {
            plugin.messager.printf(responser, LanguageKeys.Commands.Response.REQUEST_EXPIRED)
            session.markInvalid()
            return session
        }

        // 检查玩家在线状态
        if (!session.requester.isOnline) {
            plugin.messager.printf(responser, LanguageKeys.TPA.Teleport.CANCELLED_OFFLINE)
            session.markInvalid()
            return session
        }

        val requester = session.requester.player!!
        val target = session.target.player!!

        // 再次检查世界限制（可能在等待期间改变）
        val config = plugin.getPluginConfig()
        if (config.excludeWorlds.contains(requester.world.name) ||
            config.excludeWorlds.contains(target.world.name)) {
            plugin.messager.printf(requester, LanguageKeys.TPA.Teleport.CANCELLED_WORLD)
            plugin.messager.printf(target, LanguageKeys.TPA.Teleport.CANCELLED_WORLD)
            session.markInvalid()
            return session
        }

        // 标记会话为已处理
        session.markInvalid()

        // 发送接受消息
        plugin.messager.printf(requester, LanguageKeys.TPA.Session.ACCEPTED)
        plugin.messager.printf(target, LanguageKeys.Commands.Response.ACCEPTED)

        // 移除冷却时间（接受请求时移除发起者的冷却）
        coolingDownPlayers.remove(requester.uniqueId)

        // 启动传送倒计时
        startTeleportCountdown(session)

        return session
    }

    /**
     * 拒绝传送请求
     *
     * @param sessionId 会话ID
     * @param responser 响应的玩家
     * @return 处理的会话，如果处理失败则返回null
     */
    fun rejectSession(sessionId: Long, responser: Player): TPASession? {
        val session = sessions[sessionId] ?: return null

        // 验证响应者
        if (session.target.uniqueId != responser.uniqueId) {
            plugin.messager.printf(responser, LanguageKeys.Commands.Response.NOT_YOUR_REQUEST)
            return null
        }

        // 检查会话状态
        if (session.isInvalid()) return null

        // 标记会话为已处理
        session.markInvalid()

        // 发送拒绝消息
        if (session.requester.isOnline) {
            plugin.messager.printf(session.requester.player!!, LanguageKeys.TPA.Session.REJECTED)
        }
        plugin.messager.printf(responser, LanguageKeys.Commands.Response.REJECTED)

        // 移除冷却时间（被拒绝时移除发起者的冷却）
        coolingDownPlayers.remove(session.requester.uniqueId)

        return session
    }

    // ===== 私有方法 =====

    /**
     * 清理过期的会话
     */
    private fun cleanupExpiredSessions() {
        val now = System.currentTimeMillis()
        var cleanedCount = 0

        // 从队列头部移除过期会话
        while (sessionQueue.isNotEmpty() && sessionQueue.first().expiredTime < now) {
            val expiredSession = sessionQueue.removeFirst()
            sessions.remove(expiredSession.id)
            cleanedCount++
        }
    }

    /**
     * 清理过期的冷却时间
     */
    private fun cleanupExpiredCooldowns() {
        val now = System.currentTimeMillis()
        val iterator = coolingDownPlayers.entries.iterator()
        var cleanedCount = 0

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value < now) {
                iterator.remove()
                cleanedCount++
            }
        }
    }

    /**
     * 启动传送倒计时
     */
    private fun startTeleportCountdown(session: TPASession) {
        val config = plugin.getPluginConfig()
        val delaySeconds = config.delaySeconds

        if (delaySeconds <= 0) {
            // 立即传送
            performTeleport(session)
            return
        }

        // 倒计时传送
        var counter = delaySeconds
        runAsyncRepeating(0L, 20L) { task ->
            val fromPlayer = session.getFromPlayer()
            val toPlayer = session.getToPlayer()

            // 检查玩家在线状态
            if (!fromPlayer.isOnline || !toPlayer.isOnline) {
                val onlinePlayer = if (fromPlayer.isOnline) fromPlayer.player else toPlayer.player
                onlinePlayer?.let {
                    plugin.messager.printf(it, LanguageKeys.TPA.Teleport.CANCELLED_OFFLINE)
                }
                task.cancel()
                return@runAsyncRepeating
            }

            val from = fromPlayer.player!!
            val to = toPlayer.player!!

            // 发送倒计时消息
            messageSender.sendCountdownMessage(from, counter, true)
            messageSender.sendCountdownMessage(to, counter, false)

            if (counter <= 0) {
                task.cancel()
                // 在主线程执行传送
                runSync {
                    performTeleport(session)
                }
            } else {
                counter--
            }
        }
    }

    /**
     * 执行传送
     */
    private fun performTeleport(session: TPASession) {
        val fromPlayer = session.getFromPlayer()
        val toPlayer = session.getToPlayer()

        // 最终检查
        if (!fromPlayer.isOnline || !toPlayer.isOnline) {
            val onlinePlayer = if (fromPlayer.isOnline) fromPlayer.player else toPlayer.player
            onlinePlayer?.let {
                plugin.messager.printf(it, LanguageKeys.TPA.Teleport.CANCELLED_OFFLINE)
            }
            return
        }

        val from = fromPlayer.player!!
        val to = toPlayer.player!!

        // 检查世界限制
        val config = plugin.getPluginConfig()
        if (config.excludeWorlds.contains(from.world.name) ||
            config.excludeWorlds.contains(to.world.name)) {
            plugin.messager.printf(from, LanguageKeys.TPA.Teleport.CANCELLED_WORLD)
            plugin.messager.printf(to, LanguageKeys.TPA.Teleport.CANCELLED_WORLD)
            return
        }

        try {
            // 执行传送
            from.teleport(to.location)

            // 发送成功消息
            messageSender.sendTeleportSuccessMessage(from, to)

        } catch (e: Exception) {
            logger.error(LanguageKeys.Log.Error.TELEPORT_ERROR, e)
            plugin.messager.printf(from, LanguageKeys.Core.Error.OPERATION_FAILED, e.message)
            plugin.messager.printf(to, LanguageKeys.Core.Error.OPERATION_FAILED, e.message)
        }
    }
}
