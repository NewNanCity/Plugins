package city.newnan.guardian.model

import org.bukkit.Bukkit
import javax.sql.DataSource
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf

val Database.players get() = sequenceOf(Players)
val Database.towns get() = sequenceOf(Towns)
val Database.servers get() = sequenceOf(Servers)
fun Player.isBan() = banMode > 0
fun Player.notBan() = banMode == 0.toShort()
fun Player.tmpBan() = banMode == 1.toShort()
fun Player.forceBan() = banMode == 2.toShort()
fun String.findPlayer() = DBManager.db.players.find { it.name eq this }
fun Long.findPlayer() = DBManager.db.players.find { it.qq eq this.toString() }
fun String.findTown() = DBManager.db.towns.find { it.name like "%$this%" }
fun String.findServer() = DBManager.db.servers.find { (it.name eq this) or (it.address eq this) }
fun Player.hasJoinGroup() = inQQGroup or inQQGuild or inDiscord
fun Player.inOtherServer() = curServer != null && curServer != DBManager.serverId
fun org.bukkit.entity.Player.lockServer() {
    DBManager.db.update(Players) {
        set(it.curServer, null)
        where {
            (it.name eq name) and
            (it.curServer eq DBManager.serverId)
        }
    }
}

/**
 * 更新服务器锁
 */
fun refreshServerLock() {
    DBManager.db.update(Players) {
        set(it.curServer, null)
        where { it.curServer eq DBManager.serverId }
    }
}

fun lockOnlinePlayers() {
    DBManager.db.batchUpdate(Players) {
        Bukkit.getOnlinePlayers().forEach { player ->
            item {
                set(it.curServer, DBManager.serverId)
                where {
                    it.name eq player.name
                }
            }
        }
    }
}

object DBManager {
    lateinit var db: Database
    var serverId: Int = 0
    fun reload(databaseCP: DataSource, serverName: String, serverAddress: String) {
        // bind db
        db = Database.connect(databaseCP)
        // gen server id
        val server = serverName.findServer() ?: serverAddress.findServer()
        serverId = server?.id
            ?: db.insertAndGenerateKey(Servers) {
                set(it.name, serverName)
                set(it.address, serverAddress)
            } as Int
    }
}