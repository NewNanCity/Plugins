package city.newnan.mcron

import city.newnan.mcron.config.ConfigFile
import city.newnan.mcron.config.PlayerCron
import city.newnan.mcron.config.PlayerCrons
import city.newnan.mcron.timeiterator.CronExpression
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import me.lucko.helper.Events
import me.lucko.helper.plugin.ExtendedJavaPlugin
import org.bukkit.Bukkit
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.event.server.ServerLoadEvent
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs


class PluginMain : ExtendedJavaPlugin() {
    internal val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.LRU, 4)
        }
    }
    internal val messageManager: MessageManager by lazy { MessageManager(this) }
    internal val cronManager: CronManager by lazy { CronManager() }
    private val commandManager: PaperCommandManager by lazy { PaperCommandManager(this) }
    private val playerCronMap = mutableMapOf<UUID, PlayerCron>()
    companion object {
        lateinit var INSTANCE: PluginMain
            private set
    }
    init { INSTANCE = this }

    override fun enable() {
        // 初始化ConfigManager
        configManager touch "config.yml"

        // 初始化MessageManager
        messageManager setPlayerPrefix "§7[§6牛腩小镇§7] §r"

        // 初始化CommandManager
        commandManager.enableUnstableAPI("help")
        commandManager.registerCommand(Commands)

        // 运行Cron
        cronManager.run()
        cronManager.bindWith(this)
        Events.subscribe(ServerLoadEvent::class.java, EventPriority.MONITOR)
            .handler {
                val now = System.currentTimeMillis()
                configManager.parse<ConfigFile>("config.yml").onServerReady.filterKeys {
                    abs(CronExpression(it).getNextTime(now) - now) < 1000L
                }.forEach { (_, commands) -> executeCommands(commands) }
            }
            .bindWith(this)
        Events.subscribe(PluginEnableEvent::class.java, EventPriority.MONITOR)
            .handler {
                configManager.parse<ConfigFile>("config.yml")
                    .onPluginReady[it.plugin.description.name]?.apply { executeCommands(this) }
            }
            .bindWith(this)
        Events.subscribe(PluginDisableEvent::class.java, EventPriority.MONITOR)
            .handler {
                configManager.parse<ConfigFile>("config.yml")
                    .onPluginDisable[it.plugin.description.name]?.apply { executeCommands(this) }
            }
            .bindWith(this)
        Events.subscribe(PlayerJoinEvent::class.java, EventPriority.MONITOR)
            .filter { playerCronMap.containsKey(it.player.uniqueId) }
            .handler {
                executeCommands(playerCronMap[it.player.uniqueId]!!.onJoin.toTypedArray())
                playerCronMap[it.player.uniqueId]!!.onJoin.clear()
                save()
            }
            .bindWith(this)
    }

    override fun disable() {
        commandManager.unregisterCommands()
    }

    fun reload() {
        configManager.cache?.clear()
        cronManager.reload()

        playerCronMap.clear()
        configManager touch "players.yml"
        playerCronMap.putAll(configManager.parse<PlayerCrons>("players.yml"))
    }

    private fun save() {
        playerCronMap.forEach { (uuid, playerCron) ->
            if (playerCron.onJoin.isNotEmpty()) return@forEach
            playerCronMap.remove(uuid)
        }
        configManager.save(playerCronMap, "players.yml")
    }

    fun pushPlayerJoinTask(uuid: UUID, command: String) {
        playerCronMap.getOrPut(uuid) { PlayerCron() }.onJoin.add(command)
        save()
    }

    fun executeCommands(commands: Array<String>) {
        Bukkit.getConsoleSender().run {
            val nextCommands = mutableListOf<String>()
            var nextTimeDelta = 0L
            commands.forEach { command ->
                if (nextTimeDelta <= 0L) {
                    if (command.startsWith("\$sleep\$ ")) {
                        val sleepTimeArgv = command.substring(8)
                        when {
                            sleepTimeArgv.endsWith("ms") -> {
                                val sleepTime = sleepTimeArgv.substring(0, sleepTimeArgv.length - 2).toLong()
                                nextTimeDelta = sleepTime
                            }
                            sleepTimeArgv.endsWith("s") -> {
                                val sleepTime = sleepTimeArgv.substring(0, sleepTimeArgv.length - 1).toLong()
                                nextTimeDelta = TimeUnit.SECONDS.toMillis(sleepTime)
                            }
                            sleepTimeArgv.endsWith("m") -> {
                                val sleepTime = sleepTimeArgv.substring(0, sleepTimeArgv.length - 1).toLong()
                                nextTimeDelta = TimeUnit.MINUTES.toMillis(sleepTime)
                            }
                            sleepTimeArgv.endsWith("h") -> {
                                val sleepTime = sleepTimeArgv.substring(0, sleepTimeArgv.length - 1).toLong()
                                nextTimeDelta = TimeUnit.HOURS.toMillis(sleepTime)
                            }
                            sleepTimeArgv.endsWith("d") -> {
                                val sleepTime = sleepTimeArgv.substring(0, sleepTimeArgv.length - 1).toLong()
                                nextTimeDelta = TimeUnit.DAYS.toMillis(sleepTime)
                            }
                            else -> {
                                return@forEach
                            }
                        }
                    } else {
                        messageManager.printf("§7> §a$command")
                        Bukkit.dispatchCommand(this, command)
                    }
                } else {
                    nextCommands.add(command)
                }
            }
            if (nextCommands.isNotEmpty()) {
                cronManager.addTask(System.currentTimeMillis() + nextTimeDelta, nextCommands.toTypedArray())
            }
        }
    }
}