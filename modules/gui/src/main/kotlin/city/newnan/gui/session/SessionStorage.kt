package city.newnan.gui.session

import city.newnan.core.utils.text.toPlain
import city.newnan.gui.event.GlobalEventListener
import city.newnan.gui.manager.logging.GuiLogger
import city.newnan.gui.manager.GuiManager
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 全局会话存储管理器
 *
 * 作为单例管理所有GUI相关的全局状态：
 * - 玩家Session集合管理
 * - GuiManager列表管理
 * - ChatInput处理器管理
 *
 * 特性：
 * - 线程安全：使用ConcurrentHashMap和读写锁
 * - 跨插件协作：支持多个插件共享GUI状态
 * - 自动清理：玩家退出时自动清理相关数据
 * - 防内存泄漏：使用WeakReference和定时清理
 */
object SessionStorage {

    // 玩家Session映射 - 每个玩家可以有多个命名session
    private val playerSessions = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Session>>()

    // 注册的GuiManager列表 - 用于事件处理的责任链
    private val guiManagers = CopyOnWriteArrayList<GuiManager>()

    // ChatInput处理器映射 - 每个玩家最多一个活跃的输入处理器
    private val chatInputHandlers = ConcurrentHashMap<UUID, (Component) -> Boolean>()

    // 读写锁用于保护复杂操作
    private val lock = ReentrantReadWriteLock()

    // 默认session名称
    const val DEFAULT_SESSION_NAME = "default"

    private var _currentGuiManager: GuiManager? = null

    val currentGuiManager: GuiManager?
        get() = lock.read {
            if (_currentGuiManager == null) {
                lock.write {
                    // 双重检查锁定模式
                    if (_currentGuiManager == null) {
                        while (guiManagers.isNotEmpty()) {
                            val currentGuiManager = guiManagers.firstOrNull() ?: return@write
                            if (!Bukkit.getPluginManager().isPluginEnabled(currentGuiManager.plugin)) {
                                guiManagers.remove(currentGuiManager)
                            } else {
                                _currentGuiManager = currentGuiManager
                                break
                            }
                        }
                    }
                }
            }
            _currentGuiManager
        }

    /**
     * 获取玩家的指定session
     * 如果不存在则返回null
     */
    fun getSession(player: Player, sessionName: String = DEFAULT_SESSION_NAME): Session? {
        return playerSessions[player.uniqueId]?.get(sessionName)
    }

    /**
     * 获取或创建玩家的指定session
     * 如果不存在则创建新的session
     */
    fun getOrCreateSession(player: Player, sessionName: String = DEFAULT_SESSION_NAME): Session {
        return lock.read {
            playerSessions.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }
                .computeIfAbsent(sessionName) { Session(player, sessionName) }
        }
    }

    /**
     * 设置玩家的session
     * 如果session为空则移除该session
     */
    fun setSession(player: Player, sessionName: String, session: Session?) {
        lock.write {
            if (session == null) {
                playerSessions[player.uniqueId]?.remove(sessionName)
                // 如果玩家没有任何session了，移除玩家记录
                if (playerSessions[player.uniqueId]?.isEmpty() == true) {
                    playerSessions.remove(player.uniqueId)
                }
            } else {
                playerSessions.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }[sessionName] = session
            }
        }
    }

    /**
     * 移除玩家的指定session
     */
    fun removeSession(player: Player, sessionName: String = DEFAULT_SESSION_NAME) {
        setSession(player, sessionName, null)
    }

    /**
     * 获取玩家的所有session
     */
    fun getPlayerSessions(player: Player): Map<String, Session> {
        return playerSessions[player.uniqueId]?.toMap() ?: emptyMap()
    }

    /**
     * 获取所有活跃的会话
     * 返回玩家到默认session的映射
     */
    fun getAllActiveSessions(): Map<UUID, Session> {
        return playerSessions.mapNotNull { (player, sessions) ->
            sessions[DEFAULT_SESSION_NAME]?.let { player to it }
        }.toMap()
    }

    /**
     * 清理玩家的所有session
     * 通常在玩家退出时调用
     */
    fun clearPlayerSessions(player: Player) {
        lock.write {
            playerSessions[player.uniqueId]?.values?.forEach { session ->
                try {
                    session.close()
                } catch (e: Exception) {
                    // 忽略清理时的异常，避免影响其他清理操作
                    _currentGuiManager?.logger?.logError(
                        errorType = GuiLogger.ErrorType.SESSION_MANAGEMENT,
                        message = "Error closing session",
                        error = e,
                        context = mapOf(
                            "playerName" to player.name,
                            "sessionName" to session.name,
                        )
                    )
                }
            }
            playerSessions.remove(player.uniqueId)
        }
    }

    /**
     * 注册GuiManager到管理列表
     * 用于事件处理的责任链模式
     */
    fun registerGuiManager(manager: GuiManager) {
        lock.write {
            if (!guiManagers.contains(manager)) {
                guiManagers.add(manager)
                GlobalEventListener.register(manager)
                _currentGuiManager = guiManagers.firstOrNull()
            }
        }
    }

    /**
     * 注销GuiManager
     */
    fun unregisterGuiManager(manager: GuiManager) {
        lock.write {
            guiManagers.remove(manager)
            _currentGuiManager = guiManagers.firstOrNull()
        }
    }

    /**
     * 获取所有注册的GuiManager
     * 返回只读列表
     */
    fun getGuiManagers(): List<GuiManager> = guiManagers.toList()

    /**
     * 设置玩家的ChatInput处理器
     * 每个玩家最多只能有一个活跃的输入处理器
     */
    @JvmName("setChatInputHandler_String")
    fun setChatInputHandler(player: Player, handler: ((String) -> Boolean)) {
        chatInputHandlers[player.uniqueId] = {
                handler.invoke(it.toPlain())
            }
    }

    /**
     * 设置玩家的ChatInput处理器
     * 每个玩家最多只能有一个活跃的输入处理器
     */
    fun setChatInputHandler(player: Player, handler: ((Component) -> Boolean)) {
        chatInputHandlers[player.uniqueId] = handler
    }

    /**
     * 移除玩家的ChatInput处理器
     */
    fun removeChatInputHandler(player: Player) {
        chatInputHandlers.remove(player.uniqueId)
    }

    /**
     * 获取玩家的ChatInput处理器
     */
    fun getChatInputHandler(player: Player): ((Component) -> Boolean)? {
        return chatInputHandlers[player.uniqueId]
    }

    /**
     * 检查玩家是否有活跃的ChatInput处理器
     */
    fun hasChatInputHandler(player: Player): Boolean {
        return chatInputHandlers.containsKey(player.uniqueId)
    }

    /**
     * 清理玩家的ChatInput处理器
     */
    fun clearChatInputHandler(player: Player) {
        chatInputHandlers.remove(player.uniqueId)
    }

    fun runInMainThread(task: () -> Unit) {
        if (Bukkit.getServer().isPrimaryThread) {
            task()
        } else {
            val plugin = currentGuiManager?.plugin ?: Bukkit.getPluginManager().plugins.first { it.isEnabled }
            Bukkit.getScheduler().runTask(plugin, task)
        }
    }

    /**
     * 获取统计信息
     */
    fun getStats(): SessionStorageStats {
        return SessionStorageStats(
            totalPlayers = playerSessions.size,
            totalSessions = playerSessions.values.sumOf { it.size },
            totalGuiManagers = guiManagers.size,
            activeChatInputs = chatInputHandlers.size
        )
    }

    /**
     * 执行定期清理
     * 清理已关闭的session和无效的引用
     */
    fun performCleanup() {
        lock.write {
            // 清理已关闭的session
            val playersToRemove = mutableListOf<UUID>()

            playerSessions.forEach { (player, sessions) ->
                val sessionsToRemove = mutableListOf<String>()

                sessions.forEach { (name, session) ->
                    if (session.isClosed()) {
                        sessionsToRemove.add(name)
                    }
                }

                sessionsToRemove.forEach { sessions.remove(it) }

                if (sessions.isEmpty()) {
                    playersToRemove.add(player)
                }
            }

            playersToRemove.forEach { playerSessions.remove(it) }

            // 清理离线玩家的ChatInput处理器
            val offlinePlayers = chatInputHandlers.keys.filter { !Bukkit.getPlayer(it)?.isOnline!! }
            offlinePlayers.forEach { chatInputHandlers.remove(it) }
        }
    }

    /**
     * SessionStorage统计信息
     */
    data class SessionStorageStats(
        val totalPlayers: Int,
        val totalSessions: Int,
        val totalGuiManagers: Int,
        val activeChatInputs: Int
    )
}
