package city.newnan.guardian.gui

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TownPermissionsTest {
    @Test
    fun `town permissions use resolved IDs and never treat two missing IDs as a match`() {
        assertTrue(canManageTown(42, 42, false))
        assertFalse(canManageTown(41, 42, false))
        assertFalse(canManageTown(null, null, false))
        assertFalse(canManageTown(null, 42, false))
        assertFalse(canManageTown(0, 0, false))
        assertTrue(canManageTown(null, null, true))
    }
}
