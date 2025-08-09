package city.newnan.guardian.model

import org.jetbrains.exposed.v1.core.ResultRow
import java.time.LocalDateTime

enum class BanMode(val value: Int) {
    NOT_BANNED(0),
    TEMP_BANNED(1),
    PERMANENT_BANNED(2)
}

data class Player(
    val id: Int,
    val name: String,
    val qq: String?,
    val qqguild: String?,
    val discord: String?,
    val inqqgroup: Boolean,
    val town: Int?,
    val banMode: Int,
    val banExpire: LocalDateTime?,
    val curServer: Int?,
    val inqqguild: Boolean,
    val indiscord: Boolean
) {
     companion object {
         fun fromRow(row: ResultRow) = Player(
            row[PlayerTable.id].value,
            row[PlayerTable.name],
            row[PlayerTable.qq],
            row[PlayerTable.qqguild],
            row[PlayerTable.discord],
            row[PlayerTable.inqqgroup],
            row[PlayerTable.town]?.value,
            row[PlayerTable.banMode],
            row[PlayerTable.banExpire]?.run { LocalDateTime.of(this.year, this.monthNumber, this.dayOfMonth,
                      this.hour, this.minute, this.second, this.nanosecond) },
            row[PlayerTable.curServer],
            row[PlayerTable.inqqguild],
            row[PlayerTable.indiscord]
        )
     }

    val banned: Boolean get() = banMode != 0
    val tempBanned: Boolean get() = banMode == 1
    val permanentBanned: Boolean get() = banMode == 2
    val hasJoinGroup: Boolean get() = inqqgroup || inqqguild || indiscord
}

data class Town(
    val id: Int,
    val name: String,
    val level: Int,
    val leader: Int?,
    val qqgroup: String?
) {
    companion object {
        fun fromRow(row: ResultRow) = Town(
            row[TownTable.id].value,
            row[TownTable.name],
            row[TownTable.level],
            row[TownTable.leader]?.value,
            row[TownTable.qqgroup]
        )
    }
}

data class Server(
    val id: Int,
    val name: String,
    val address: String
) {
    companion object {
        fun fromRow(row: ResultRow) = Server(
            row[ServerTable.id].value,
            row[ServerTable.name],
            row[ServerTable.address]
        )
    }
}

data class PlayerIp(
    val id: Int,
    val playerId: Int,
    val ip: String
) {
    companion object {
        fun fromRow(row: ResultRow) = PlayerIp(
            row[PlayerIpTable.id],
            row[PlayerIpTable.id],
            row[PlayerIpTable.ip]
        )
    }
}
