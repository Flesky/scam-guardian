package ph.scamguardian.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ph.scamguardian.core.Language

class MainUiStateTest {
    @Test
    fun secured_onlyWhenTheButtonIsOnAndTheServiceIsEnabled() {
        assertTrue(MainUiState(isOn = true, serviceEnabled = true).secured)
        assertFalse(MainUiState(isOn = true, serviceEnabled = false).secured)
        assertFalse(MainUiState(isOn = false, serviceEnabled = true).secured)
        assertFalse(MainUiState(isOn = false, serviceEnabled = false).secured)
    }

    @Test
    fun language_defaultsToFilipino() {
        assertEquals(Language.FILIPINO, MainUiState(isOn = false, serviceEnabled = false).language)
    }
}
