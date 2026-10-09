package ph.scamguardian.theme

import androidx.annotation.DrawableRes
import ph.scamguardian.R
import ph.scamguardian.core.Severity

/** The warning icon of a severity: the alert icon for red, the caution icon for amber. */
@get:DrawableRes
val Severity.icon: Int
    get() =
        when (this) {
            Severity.RED -> R.drawable.ic_alert
            Severity.AMBER -> R.drawable.ic_caution
        }
