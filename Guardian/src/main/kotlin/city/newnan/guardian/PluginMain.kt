package city.newnan.guardian

import city.newnan.guardian.config.ConfigFile
import city.newnan.guardian.model.DBManager
import city.newnan.guardian.model.Servers
import city.newnan.guardian.model.lockOnlinePlayers
import city.newnan.guardian.model.refreshServerLock
import city.newnan.violet.config.ConfigManager2
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.lucko.helper.plugin.ExtendedJavaPlugin
import org.anjocaido.groupmanager.GroupManager
import org.anjocaido.groupmanager.data.Group
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder
import org.bukkit.Bukkit
import org.ktorm.dsl.eq
import org.ktorm.dsl.update
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
    private var database: HikariDataSource? = null
    internal lateinit var workWorldsPermissionHandler: OverloadedWorldHolder
    internal lateinit var newbiesGroup: Group
    internal lateinit var playersGroup: Group

    internal var checkGroup: Boolean = true
    private var serverAddress: String = ""
    private var serverName: String = ""

    override fun enable() {
        configManager touch "config.yml"
        reload()
        refreshServerLock()
        lockOnlinePlayers()
        DBManager.db.update(Servers) {
            set(it.online, true)
            where { it.id eq DBManager.serverId }
        }
        EventTrigger.on()
    }

    private fun reload() {
        configManager.cache?.clear()
        configManager touch "config.yml"
        val config = configManager.parse<ConfigFile>("config.yml")
        checkGroup = config.checkGroup
        serverAddress = config.serverAddress
        serverName = config.serverName
        if (checkGroup) {
            val gmPlugin = Bukkit.getServer().pluginManager.getPlugin("GroupManager")
            if (gmPlugin == null || !gmPlugin.isEnabled) {
                throw Exception("Guardian needs GroupManager plugin!")
            }
            val groupManager = gmPlugin as GroupManager
            workWorldsPermissionHandler = groupManager.worldsHolder.getWorldData("world")
            newbiesGroup = workWorldsPermissionHandler.getGroup("Newbie")
            playersGroup = workWorldsPermissionHandler.getGroup("Player")
        }

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
            maxLifetime = TimeUnit.MINUTES.toMillis(10)
            connectionTimeout = TimeUnit.SECONDS.toMillis(10)
            leakDetectionThreshold = TimeUnit.SECONDS.toMillis(10)
            keepaliveTime = TimeUnit.MINUTES.toMillis(10)
        })
        DBManager.reload(database!!, serverName, serverAddress)
    }

    override fun disable() {
        refreshServerLock()
        DBManager.db.update(Servers) {
            set(it.online, false)
            where { it.id eq DBManager.serverId }
        }
        database?.close()
    }
}