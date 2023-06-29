package city.newnan.guardian.model

import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

interface Town : Entity<Town> {
    companion object : Entity.Factory<Town>()
    val id: Int
    var name: String
    var level: Int
    var leader: Int?
    var qqGroup: String?
}

object Towns : Table<Town>("towns") {
    val id = int("id").primaryKey().bindTo { it.id }
    var name = varchar("name").bindTo { it.name }
    var level = int("level").bindTo { it.level }
    var leaderID = int("leader").bindTo { it.leader }
    var groupID = varchar("qqgroup").bindTo { it.qqGroup }
}