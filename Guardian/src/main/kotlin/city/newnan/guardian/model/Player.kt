package city.newnan.guardian.model

import org.ktorm.entity.Entity
import org.ktorm.schema.*
import java.time.LocalDateTime

interface Player : Entity<Player> {
    companion object : Entity.Factory<Player>()
    val id: Int
    var name: String
    var qq: String?
    var qqguild: String?
    var discord: String?
    var inQQGroup: Boolean
    var inQQGuild: Boolean
    var inDiscord: Boolean
    var town: Town?
    var banMode: Short
    var banExpire: LocalDateTime?
    var curServer: Int?
}

object Players : Table<Player>("players") {
    val id = int("id").primaryKey().bindTo { it.id }
    var name = varchar("name").bindTo { it.name }
    var qq = varchar("qq").bindTo { it.qq }
    var qqguild = varchar("qqguild").bindTo { it.qqguild }
    var discord = varchar("discord").bindTo { it.discord }
    var inQQGroup = boolean("inqqgroup").bindTo { it.inQQGroup }
    var inQQGuild = boolean("inqqguild").bindTo { it.inQQGuild }
    var inDiscord = boolean("indiscord").bindTo { it.inDiscord }
    var town = int("town").references(Towns) { it.town }
    var banMode = short("ban_mode").bindTo { it.banMode }
    var banExpire = datetime("ban_expire").bindTo { it.banExpire }
    var curServer = int("cur_server").bindTo { it.curServer }
}