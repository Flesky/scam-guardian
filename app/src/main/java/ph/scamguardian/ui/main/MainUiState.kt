package ph.scamguardian.ui.main

import ph.scamguardian.core.Language

/** What the main screen shows. */
data class MainUiState(
    /** The saved state of the on/off button. */
    val isOn: Boolean,
    /** Whether the accessibility service is switched on in the system settings. */
    val serviceEnabled: Boolean,
    val language: Language = Language.DEFAULT,
    /** Demo mode: every warning shows its banner at once, with no wait between banners. */
    val demoMode: Boolean = false,
) {
    /** Messages are only read when the button is ON and the service is running. The mascot and the button show this. */
    val secured: Boolean get() = isOn && serviceEnabled
}
