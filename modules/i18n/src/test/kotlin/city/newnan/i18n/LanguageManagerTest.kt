package city.newnan.i18n

import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * LanguageManager 测试类
 *
 * @author Gk0Wk
 * @since 1.0.0
 */
class LanguageManagerTest {

    @Test
    fun `test i18n config creation`() {
        val config = I18nConfig(
            enabled = true,
            defaultLanguage = Locale.US,
            majorLanguage = Locale.SIMPLIFIED_CHINESE
        )

        assertEquals(true, config.enabled)
        assertEquals(Locale.US, config.defaultLanguage)
        assertEquals(Locale.SIMPLIFIED_CHINESE, config.majorLanguage)
    }

    @Test
    fun `test i18n config defaults`() {
        val config = I18nConfig.DEFAULT

        assertTrue(config.enabled)
        assertEquals(64, config.cacheSize)
        assertEquals("<%([\\w\\.]+)%>", config.templatePattern)
    }

    @Test
    fun `test language locale equality`() {
        val locale1 = Locale.SIMPLIFIED_CHINESE
        val locale2 = Locale.SIMPLIFIED_CHINESE
        val locale3 = Locale.US

        assertEquals(locale1, locale2)
        assertTrue(locale1 != locale3)
    }
}
