package city.newnan.guardian.modules

import city.newnan.core.base.BaseModule
import city.newnan.guardian.GuardianPlugin
import city.newnan.guardian.model.Player
import city.newnan.guardian.model.PlayerIpTable
import city.newnan.guardian.model.PlayerTable
import city.newnan.guardian.model.ServerTable
import city.newnan.guardian.model.Town
import city.newnan.guardian.model.TownTable
import com.zaxxer.hikari.HikariDataSource
import kotlinx.datetime.LocalDateTime
import org.bukkit.Bukkit
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert

class PlayerDB(val plugin: GuardianPlugin) : BaseModule("PlayerDB", plugin) {
    var serverAddress: String = ""
    var serverName: String = ""

    var datasource: HikariDataSource? = null
    lateinit var db: Database
    var serverId: Int = -1

    init { init() }

    override fun onReload() {
        val config = plugin.getPluginConfig()
        serverAddress = config.serverAddress
        serverName = config.serverName

        datasource?.close()
        datasource = null
        val newDatasource = HikariDataSource(config.database.toHikariConfig())

        val newDb = Database.connect(newDatasource)

        // 更新服务器信息
        val playerNames = Bukkit.getOnlinePlayers().map { it.name }
        transaction(newDb) {
            serverId = ServerTable.select(ServerTable.id).withDistinct().
            where { (ServerTable.name eq serverName) or (ServerTable.address eq serverAddress) }.
            map { it[ServerTable.id].value }.firstOrNull() ?: run {
                ServerTable.insertAndGetId {
                    it[name] = serverName
                    it[address] = serverAddress
                    it[online] = true
                }.value
            }
            PlayerTable.update({ PlayerTable.curServer eq serverId }) {
                it[curServer] = null
            }
            PlayerTable.update({ PlayerTable.name inList playerNames }) {
                it[curServer] = serverId
            }
        }

        datasource = newDatasource
        db = newDb
    }

    override fun onInit() {

    }

    override fun onClose() {
        if (datasource != null) {
            transaction(db) {
                ServerTable.update({ ServerTable.id eq serverId }) {
                    it[online] = false
                }
            }

            datasource!!.close()
            datasource = null
        }
    }

    // DAO Methods

    fun getPlayerById(id: Int) = transaction(db) {
        PlayerTable.selectAll().withDistinct().where { PlayerTable.id eq id }.map {
            Player.fromRow(it)
        }.firstOrNull()
    }

    fun getPlayerByName(name: String) = transaction(db) {
        PlayerTable.selectAll().withDistinct().where { PlayerTable.name eq name }.map {
            Player.fromRow(it)
        }.firstOrNull()
    }

    fun getPlayerByQQ(qq: String) = transaction(db) {
        PlayerTable.selectAll().withDistinct().where { PlayerTable.qq eq qq }.map {
            Player.fromRow(it)
        }.firstOrNull()
    }

    fun getPlayerByQQGuild(qqguild: String) = transaction(db) {
        PlayerTable.selectAll().withDistinct().where { PlayerTable.qqguild eq qqguild }.map {
            Player.fromRow(it)
        }.firstOrNull()
    }

    fun getPlayerByDiscord(discord: String) = transaction(db) {
        PlayerTable.selectAll().withDistinct().where { PlayerTable.discord eq discord }.map {
            Player.fromRow(it)
        }.firstOrNull()
    }

    fun findTownByName(name: String) = transaction(db) {
        TownTable.selectAll().withDistinct().where { TownTable.name like "%$name%" }.map {
            Town.fromRow(it)
        }.firstOrNull()
    }

    fun getTownById(id: Int) = transaction(db) {
        TownTable.selectAll().withDistinct().where { TownTable.id eq id }.map {
            Town.fromRow(it)
        }.firstOrNull()
    }

    fun onPlayerLogin(player: org.bukkit.entity.Player): Player? = transaction(db) {
        val playerRecord = getPlayerByName(player.name) ?: return@transaction null

        player.address?.hostString?.run {
            PlayerIpTable.upsert {
                it[id] = playerRecord.id
                it[PlayerIpTable.ip] = ip
            }
        }

        playerRecord
    }

    fun updatePlayerStatus(playerId: Int, banMode: Int, banExpire: LocalDateTime?, curServer: Boolean) = transaction(db) {
        PlayerTable.update({ PlayerTable.id eq playerId }) {
            it[PlayerTable.banMode] = banMode
            it[PlayerTable.banExpire] = banExpire
            if (curServer) it[PlayerTable.curServer] = serverId
        }
    }

    fun onPlayerLogout(player: org.bukkit.entity.Player) = transaction(db) {
        val playerRecord = getPlayerByName(player.name) ?: return@transaction null

        PlayerTable.update({ PlayerTable.name eq player.name }) {
            it[curServer] = null
        }

        playerRecord
    }

    // 获取小镇成员列表
    fun getTownMembers(townId: Int, offset: Int, limit: Int) = transaction(db) {
        PlayerTable.selectAll()
            .withDistinct()
            .where { PlayerTable.town eq townId }
            .limit(limit)
            .offset(offset.toLong())
            .orderBy(PlayerTable.id to SortOrder.ASC)
            .map { Player.fromRow(it) }
    }

    fun getTownMembersCount(townId: Int) = transaction(db) {
        PlayerTable.selectAll().withDistinct().where { PlayerTable.town eq townId }.count().toInt()
    }

    // 获取玩家的历史IP列表
    fun getPlayerIps(playerId: Int) = transaction(db) {
        PlayerIpTable.select(PlayerIpTable.ip).withDistinct().where { PlayerIpTable.id eq playerId }.map {
            it[PlayerIpTable.ip]
        }
    }

    // 更新玩家的小镇
    fun updatePlayerTown(playerId: Int, townId: Int?) = transaction(db) {
        PlayerTable.update({ PlayerTable.id eq playerId }) {
            it[town] = townId
        }
    }
}