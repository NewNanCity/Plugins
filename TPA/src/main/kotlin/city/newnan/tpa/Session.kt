package city.newnan.tpa

import org.bukkit.OfflinePlayer

data class Session(
    val requester: OfflinePlayer,
    val target: OfflinePlayer,
    val targetToRequester: Boolean,
    var expired: Long,
    val id: Long,
)