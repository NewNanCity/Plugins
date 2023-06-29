package city.newnan.guardian

import city.newnan.guardian.model.DBManager
import city.newnan.guardian.model.Servers
import city.newnan.guardian.model.lockOnlinePlayers
import city.newnan.guardian.model.refreshServerLock
import city.newnan.violet.config.ConfigManager
import city.newnan.violet.sql.SQLConnectionPool
import city.newnan.violet.sql.buildSQLCP
import me.lucko.helper.plugin.ExtendedJavaPlugin
import org.anjocaido.groupmanager.GroupManager
import org.anjocaido.groupmanager.data.Group
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder
import org.bukkit.Bukkit
import org.ktorm.dsl.eq
import org.ktorm.dsl.update


class Guardian : ExtendedJavaPlugin() {
    companion object {
        lateinit var INSTANCE: Guardian
            private set
    }
    init { INSTANCE = this }

    private val configManager: ConfigManager by lazy { ConfigManager(this) }
    private var databaseCP: SQLConnectionPool? = null
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
        checkGroup = configManager["config.yml"]?.getNode("check-group")?.getBoolean(true) ?: true
        serverAddress = configManager["config.yml"]?.getNode("server-address")?.getString("") ?: ""
        serverName = configManager["config.yml"]?.getNode("server-name")?.getString("") ?: ""
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
        configManager["config.yml"]?.getNode("mysql")?.also {
            databaseCP?.close()
            databaseCP = buildSQLCP {
                host = it.getNode("host").getString("localhost") ?: return@buildSQLCP
                port = it.getNode("post").getInt(3306)
                database = it.getNode("database").getString("") ?: return@buildSQLCP
                username = it.getNode("username").getString("") ?: return@buildSQLCP
                password = it.getNode("password").getString("") ?: return@buildSQLCP
                it.getNode("params").childrenMap.forEach { (key, node) ->
                    set(key.toString() to node.getString(""))
                }
            }
            DBManager.reload(databaseCP!!.hikari, serverName, serverAddress)
        }
    }

    override fun disable() {
        refreshServerLock()
        DBManager.db.update(Servers) {
            set(it.online, false)
            where { it.id eq DBManager.serverId }
        }
    }
}