package city.newnan.guardian.gui

internal fun canManageTown(playerId: Int?, leaderId: Int?, hasOverride: Boolean): Boolean =
    hasOverride || (playerId != null && playerId > 0 && playerId == leaderId)
