package city.newnan.guardian.modules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HeartbeatManagerTest {
    @Test
    fun `normalizes tps to the API range`() {
        assertEquals(20.0, normalizeTps(20.01))
        assertEquals(0.0, normalizeTps(-1.0))
        assertEquals(19.5, normalizeTps(19.5))
    }

    @Test
    fun `omits non finite tps`() {
        assertNull(normalizeTps(Double.NaN))
        assertNull(normalizeTps(Double.POSITIVE_INFINITY))
        assertNull(normalizeTps(Double.NEGATIVE_INFINITY))
    }
}
