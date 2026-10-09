package ph.scamguardian.settings

import android.content.Context
import androidx.core.content.edit

/** The saved state of the demo mode switch. In demo mode every warning shows its banner at once. */
class DemoPreferences(
    context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = preferences.getBoolean(KEY_DEMO, false)
        set(value) = preferences.edit { putBoolean(KEY_DEMO, value) }

    private companion object {
        const val FILE = "scam_guardian"
        const val KEY_DEMO = "demo_mode"
    }
}
