package eu.kanade.domain.ui.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThemeModeTest {

    @Test
    fun `amoled option is unavailable in light mode`() {
        assertFalse(ThemeMode.LIGHT.supportsAmoled())
    }

    @Test
    fun `amoled option is available in dark mode`() {
        assertTrue(ThemeMode.DARK.supportsAmoled())
    }

    @Test
    fun `amoled option is available in system mode`() {
        assertTrue(ThemeMode.SYSTEM.supportsAmoled())
    }
}
