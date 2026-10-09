package ph.scamguardian.theme

import androidx.compose.ui.graphics.Color
import ph.scamguardian.core.Severity

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/** "Scam detected". Dark enough for white text on top. */
val WarningRed = Color(0xFFC62828)

/** "Possible scam detected". Dark enough for white text on top. */
val WarningAmber = Color(0xFFB45309)

/** The warning color of a severity: a card background under white text, or an icon tint. */
val Severity.color: Color
    get() =
        when (this) {
            Severity.RED -> WarningRed
            Severity.AMBER -> WarningAmber
        }

val SecuredGreen = Color(0xFF2E7D32)
private val SecuredGreenOnDark = Color(0xFF81C784)
private val WarningRedOnDark = Color(0xFFEF9A9A)

/**
 * The mascot's colors. [shell] is the bare body; [armor] is the solid plates on it.
 */
data class MascotColors(
    val shell: Color,
    val armor: Color,
    val eye: Color,
)

private val MascotOnLight = MascotColors(shell = Color(0xFFC9D0D6), armor = Color(0xFF111418), eye = Color(0xFF2BD46B))

// White armor, like the logo on a dark background.
private val MascotOnDark = MascotColors(shell = Color(0xFF3A4046), armor = Color(0xFFF1F3F4), eye = Color(0xFF18A14B))

fun mascotColors(darkTheme: Boolean): MascotColors = if (darkTheme) MascotOnDark else MascotOnLight

/** Text color of "Actively protecting you" on the app background. */
fun securedColor(darkTheme: Boolean): Color = if (darkTheme) SecuredGreenOnDark else SecuredGreen

/** Text color of "You are not protected" on the app background. */
fun unsecuredColor(darkTheme: Boolean): Color = if (darkTheme) WarningRedOnDark else WarningRed
