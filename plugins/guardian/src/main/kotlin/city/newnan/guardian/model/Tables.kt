package city.newnan.guardian.model

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.datetime

object PlayerTable : IntIdTable("players") {
    val name = varchar("name", 100).uniqueIndex(customIndexName = "player_name_un")
    val qq = varchar("qq", 15).nullable()
    val qqguild = varchar("qqguild", 23).nullable()
    val discord = varchar("discord", 18).nullable()
    val inqqgroup = bool("inqqgroup").default(false)
    val town = optReference("town", TownTable, onDelete = ReferenceOption.SET_NULL, onUpdate = ReferenceOption.CASCADE, fkName = "players_FK")
    val banMode = integer("ban_mode").default(0)
    val banExpire = datetime("ban_expire").nullable()
    val curServer = integer("cur_server").nullable()
    val inqqguild = bool("inqqguild").default(false)
    val indiscord = bool("indiscord").default(false)
}

object PlayerIpTable : Table("player_ips") {
    val id = integer("id").references(PlayerTable.id, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE, fkName = "player_ips_FK")
    val ip = varchar("ip", 100)

    override val primaryKey = PrimaryKey(id, ip, name = "player_ips_id_IDX")
}

object ServerTable : IntIdTable("servers") {
    val address = varchar("address", 100).uniqueIndex(customIndexName = "server_address_un")
    val name = varchar("name", 100).uniqueIndex(customIndexName = "server_name_un")
    val online = bool("online").default(false)
}

object TownTable : IntIdTable("towns") {
    val name = varchar("name", 100).uniqueIndex(customIndexName = "town_name_un")
    val level = integer("level").default(0)
    val leader = optReference("leader", PlayerTable.id, onDelete = ReferenceOption.SET_NULL, onUpdate = ReferenceOption.CASCADE, fkName = "towns_FK")
    val qqgroup = varchar("qqgroup", 15).nullable()
}