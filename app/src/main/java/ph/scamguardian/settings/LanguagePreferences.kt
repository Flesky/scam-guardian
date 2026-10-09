package ph.scamguardian.settings

import android.content.Context
import androidx.core.content.edit
import ph.scamguardian.core.Language

/** The saved language of the warning messages and history entries. All other app text stays in English. */
class LanguagePreferences(
    context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var language: Language
        get() = Language.fromCode(preferences.getString(KEY_LANGUAGE, null))
        set(value) = preferences.edit { putString(KEY_LANGUAGE, value.code) }

    private companion object {
        const val FILE = "scam_guardian"
        const val KEY_LANGUAGE = "language"
    }
}
