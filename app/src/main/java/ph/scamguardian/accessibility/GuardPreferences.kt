package ph.scamguardian.accessibility

import android.content.Context
import androidx.core.content.edit

/** The saved state of the main on/off button. */
class GuardPreferences(
    context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)
        set(value) = preferences.edit { putBoolean(KEY_ENABLED, value) }

    private companion object {
        const val FILE = "scam_guardian"
        const val KEY_ENABLED = "enabled"
    }
}
