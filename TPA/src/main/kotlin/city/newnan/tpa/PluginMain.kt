package city.newnan.tpa

import city.newnan.tpa.config.ConfigFile
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.Schedulers
import me.lucko.helper.plugin.ExtendedJavaPlugin
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.*
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit

class PluginMain : ExtendedJavaPlugin() {
    companion object {
        lateinit var INSTANCE: PluginMain
            private set
    }
    init { INSTANCE = this }
    private val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.None)
        }
    }
    val messageManager: MessageManager by lazy { MessageManager(this) }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }
    private val sessions = mutableMapOf<Long, Session>() // 随机访问O(1)
    private val sessionQueue = ArrayDeque<Session>()     // 插入有序，增减O(1)
    private val coolingDownPlayer = mutableMapOf<UUID, Long>()
    internal var playerBlockSet = mutableMapOf<UUID, MutableSet<UUID>>()
    private var excludeWorlds = setOf<String>()
    private var nextSessionId = 0L
    private var sessionExpiredMilliseconds = 2000L
    private var coolDownMilliseconds = 0L
    private var waitSeconds = 3

    override fun enable() {
        reload()
        commandManager.enableUnstableAPI("help")
        commandManager.registerCommand(Commands)
        messageManager setPlayerPrefix "§7[§6牛腩小镇§7] §f"
    }

    override fun disable() {
        commandManager.unregisterCommands()

    }

    fun reload() {
        // config.yml
        configManager touch "config.yml"
        configManager.parse<ConfigFile>("config.yml").also {
            sessionExpiredMilliseconds = TimeUnit.SECONDS.toMillis(it.expired.toLong())
            coolDownMilliseconds = TimeUnit.SECONDS.toMillis(it.coolDown.toLong())
            waitSeconds = it.delay
            excludeWorlds = it.excludeWorld
        }

        playerBlockSet.clear()
        configManager.touch("blocked.yml", playerBlockSet)
        playerBlockSet = configManager.parse<MutableMap<UUID, MutableSet<UUID>>>("blocked.yml")
    }

    private fun flushSessions() {
        val now = System.currentTimeMillis()
        while (sessionQueue.isNotEmpty() && sessionQueue.first.expired < now) {
            sessions.remove(sessionQueue.pollFirst().id)
        }
    }

    fun block(player: OfflinePlayer, target: OfflinePlayer) {
        if (!playerBlockSet.containsKey(player.uniqueId)) {
            playerBlockSet[player.uniqueId] = mutableSetOf()
        }
        if (playerBlockSet[player.uniqueId]!!.add(target.uniqueId))
            configManager.save(playerBlockSet, "blocked.yml")
    }

    fun unblock(player: OfflinePlayer, target: OfflinePlayer) {
        playerBlockSet[player.uniqueId]?.also {
            val removed = it.remove(target.uniqueId)
            if (it.isEmpty()) playerBlockSet.remove(player.uniqueId)
            if (removed) configManager.save(playerBlockSet, "blocked.yml")
        }
    }

    fun request(requester: Player, target: Player, targetToRequester: Boolean) {
        flushSessions()
        val now = System.currentTimeMillis()
        coolingDownPlayer[requester.uniqueId]?.also {
            if (now <= it && !requester.hasPermission("tpa.no-cool-down")) {
                messageManager.printf(requester, "§c你还在冷却中, 请 §f${TimeUnit.MILLISECONDS.toSeconds(it - now)}§c 秒后再试!")
                return@request
            } else {
                coolingDownPlayer.remove(requester.uniqueId)
            }
        }
        if (playerBlockSet[target.uniqueId]?.contains(requester.uniqueId) == true) {
            messageManager.printf(requester, "§c对方屏蔽了你的请求!")
            return
        }
        if (excludeWorlds.contains(requester.world.name)) {
            messageManager.printf(requester, "§c你不能在这个世界使用TPA!")
            return
        }
        if (excludeWorlds.contains(target.world.name)) {
            messageManager.printf(requester, "§c对方不能在这个世界使用TPA!")
            return
        }
        if (!requester.hasPermission("tpa.no-cool-down")) coolingDownPlayer[requester.uniqueId] = now + coolDownMilliseconds
        val session = Session(requester, target, targetToRequester, now + sessionExpiredMilliseconds, nextSessionId++)
        sessions[session.id] = session
        sessionQueue.addLast(session)
        sendRequest(session)
        messageManager.printf(requester, "§a已向 §f${target.name} §a发送请求!")
    }

    fun responseYes(id: Long): Session? {
        val session = sessions[id]?.also {
            val now = System.currentTimeMillis()
            if (!it.target.isOnline) {
                it.expired = 0
                return@also
            }
            val target = it.target.player!!
            if (!it.requester.isOnline) {
                it.expired = 0
                messageManager.printf(target, "§c对方已离线!")
                return@also
            }
            if (it.expired < now) {
                messageManager.printf(target, "§c请求已过期!")
                return@also
            }
            val requester = it.requester.player!!
            if (excludeWorlds.contains(requester.world.name)) {
                messageManager.printf(requester, "§c你不能在这个世界使用TPA!")
                return@also
            }
            if (excludeWorlds.contains(target.world.name)) {
                messageManager.printf(requester, "§c对方不能在这个世界使用TPA!")
                return@also
            }
            it.expired = 0
            messageManager.printf(requester, "§a对方已接受你的请求!")
            messageManager.printf(target, "§a你已接受对方的请求!")
            val from = if (it.targetToRequester) target else requester
            val to = if (it.targetToRequester) requester else target
            var counter = waitSeconds
            Schedulers.async().runRepeating({ task ->
                from.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("你将在 $counter 秒后传送到对方身边!"))
                to.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent("对方将在 $counter 秒后传送到你身边!"))
                if (counter-- <= 0) {
                    task.close()
                    if (!from.isOnline) {
                        if (to.isOnline) messageManager.printf(to, "§c对方已离线!")
                        return@runRepeating
                    }
                    if (!to.isOnline) {
                        if (from.isOnline) messageManager.printf(from, "§c对方已离线!")
                        return@runRepeating
                    }
                    if (excludeWorlds.contains(from.name)) {
                        messageManager.printf(from, "§c你所在世界使用TPA!")
                        messageManager.printf(to, "§c对方所在世界不能使用TPA!")
                        task.close()
                        return@runRepeating
                    }
                    if (excludeWorlds.contains(to.name)) {
                        messageManager.printf(to, "§c你所在世界使用TPA!")
                        messageManager.printf(from, "§c对方所在世界不能使用TPA!")
                        task.close()
                        return@runRepeating
                    }
                    from.teleport(to)
                    messageManager.printf(from, "§a你已传送到对方身边!")
                    messageManager.printf(to, "§a对方已传送到你身边!")
                    task.close()
                    return@runRepeating
                }
            }, 0, 20)
        }
        flushSessions()
        return session
    }

    fun responseNo(id: Long): Session? {
        val session = sessions[id]?.also {
            if (it.expired == 0L) return@also
            if (it.target.isOnline) messageManager.printf(it.target.player!!, "§c对方拒绝了你的请求!")
            if (it.requester.isOnline) messageManager.printf(it.requester.player!!, "§c你拒绝了对方的请求!")
            coolingDownPlayer.remove(it.requester.uniqueId)
            it.expired = 0
        }
        flushSessions()
        return session
    }
}
