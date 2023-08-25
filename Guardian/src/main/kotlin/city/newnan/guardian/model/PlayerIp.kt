package city.newnan.guardian.model

import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

interface PlayerIp : Entity<PlayerIp> {
    companion object : Entity.Factory<PlayerIp>()
    val id: Int
    var ip: String
}

object PlayerIps : Table<PlayerIp>("player_ips") {
    val id = int("id").primaryKey().bindTo { it.id }
    var ip = varchar("ip").primaryKey().bindTo { it.ip }
}