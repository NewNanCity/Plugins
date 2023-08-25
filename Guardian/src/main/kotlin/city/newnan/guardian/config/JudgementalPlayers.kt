package city.newnan.guardian.config

import java.util.UUID

data class JudgementalPlayers(
    val players: Set<UUID> = emptySet()
)