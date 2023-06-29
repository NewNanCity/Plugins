package city.newnan.guardian.model

import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.int
import org.ktorm.schema.varchar

interface Server : Entity<Server> {
    companion object : Entity.Factory<Server>()
    val id: Int
    var address: String
    var name: String
    var online: Boolean
}

object Servers : Table<Server>("servers") {
    val id = int("id").primaryKey().bindTo { it.id }
    var address = varchar("address").bindTo { it.address }
    var name = varchar("name").bindTo { it.name }
    var online = boolean("online").bindTo { it.online }
}