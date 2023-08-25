package city.newnan.guardian

import city.newnan.guardian.config.ConfigFile
import city.newnan.guardian.config.JudgementalPlayers
import city.newnan.guardian.model.*
import city.newnan.violet.config.ConfigManager2
import city.newnan.violet.gui.GuiManager
import city.newnan.violet.message.MessageManager
import co.aikar.commands.PaperCommandManager
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.lucko.helper.Events
import me.lucko.helper.event.filter.EventFilters
import me.lucko.helper.plugin.ExtendedJavaPlugin
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.permission.Permission
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.ktorm.dsl.eq
import org.ktorm.dsl.update
import java.util.*
import java.util.concurrent.TimeUnit


class PluginMain : ExtendedJavaPlugin() {
    companion object {
        lateinit var INSTANCE: PluginMain
            private set
    }
    init { INSTANCE = this }

    val configManager: ConfigManager2 by lazy {
        ConfigManager2(this).apply {
            setCache(ConfigManager2.CacheType.None)
        }
    }
    val command: PaperCommandManager by lazy { PaperCommandManager(this) }
    val gui: GuiManager by lazy { GuiManager(this) }
    val message: MessageManager by lazy { MessageManager(this) }
    private var database: HikariDataSource? = null
    lateinit var permission: Permission
    lateinit var economy: Economy

    internal var checkGroup: Boolean = true
    private var serverAddress: String = ""
    private var serverName: String = ""
    var groupWorld: String = ""
        private set
    var newbieGroup: String = ""
        private set
    var playersGroup: String = ""
        private set

    var judgementalGroup: String = ""
        private set

    val judgementalPlayers: MutableSet<UUID> = mutableSetOf()

    override fun enable() {
        reload()
        message setPlayerPrefix "§7[§6牛腩小镇§7] §f"
        command.enableUnstableAPI("help")
        command.locales.setDefaultLocale(Locale.SIMPLIFIED_CHINESE)
        command.registerCommand(Commands)

        refreshServerLock()
        lockOnlinePlayers()
        Bukkit.getOnlinePlayers().forEach { addPlayerIp(it) }
        DBManager.db.update(Servers) {
            set(it.online, true)
            where { it.id eq DBManager.serverId }
        }
        enableTrigger()

        // 玩家切换世界时触发，用于让风纪委员与其状态一致
        Events.subscribe(PlayerChangedWorldEvent::class.java, EventPriority.MONITOR)
            .handler { event ->
                val player = event.player
                val from = permission.playerInGroup(event.from.name, player, judgementalGroup)
                val to = permission.playerInGroup(player.world.name, player, judgementalGroup)
                if (from && !to) {
                    permission.playerAddGroup(player.world.name, player, judgementalGroup)
                    player.gameMode = GameMode.SPECTATOR
                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().consoleSender, "vanish ${player.name} enable")
                } else if (!from && to) {
                    permission.playerRemoveGroup(event.from.name, player, judgementalGroup)
                    player.gameMode = GameMode.SURVIVAL
                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().consoleSender, "vanish ${player.name} disable")
                }
            }
    }

    private fun reload() {
        configManager touch "config.yml"
        val config = configManager.parse<ConfigFile>("config.yml")
        checkGroup = config.checkGroup
        serverAddress = config.serverAddress
        serverName = config.serverName
        if (checkGroup) {
            if (server.pluginManager.getPlugin("Vault") == null) throw Exception("Vault not found!")
            permission = server.servicesManager.getRegistration(Permission::class.java)?.provider
                ?: throw Exception("Vault permission service not found!")
            economy = server.servicesManager.getRegistration(Economy::class.java)?.provider
                ?: throw Exception("Vault economy service not found!")
            groupWorld = config.groupWorld
            newbieGroup = config.newbieGroup
            playersGroup = config.playersGroup
        }

        judgementalPlayers.clear()
        configManager.touch("judgemental-players.yml", { JudgementalPlayers() })
        judgementalGroup = config.judgementalGroup
        judgementalPlayers.addAll(configManager.parse<JudgementalPlayers>("judgemental-players.yml").players)

        try { database?.close() } catch (e: Exception) { e.printStackTrace() }
        database = HikariDataSource(HikariConfig().apply {
            poolName = "Guardian"
            driverClassName = "com.mysql.jdbc.Driver"
            username = config.mysql.username
            password = config.mysql.password
            jdbcUrl = "jdbc:mysql://${config.mysql.host}:${config.mysql.port}/${config.mysql.database}"
            config.mysql.params.forEach { (key, value) -> addDataSourceProperty(key, value) }
            maximumPoolSize = 2
            minimumIdle = 2
            maxLifetime = TimeUnit.MINUTES.toMillis(1)
            connectionTimeout = TimeUnit.SECONDS.toMillis(1)
            leakDetectionThreshold = TimeUnit.SECONDS.toMillis(10)
            keepaliveTime = TimeUnit.MINUTES.toMillis(1)
        })
        DBManager.reload(database!!, serverName, serverAddress)
    }

    override fun disable() {
        gui.close()
        configManager.close()
        message.close()
        try { command.unregisterCommands() } catch (e: Exception) { e.printStackTrace() }
        refreshServerLock()
        DBManager.db.update(Servers) {
            set(it.online, false)
            where { it.id eq DBManager.serverId }
        }
        database?.close()
    }

    fun save() {
        configManager.save(JudgementalPlayers(judgementalPlayers), "judgemental-players.yml")
    }
}