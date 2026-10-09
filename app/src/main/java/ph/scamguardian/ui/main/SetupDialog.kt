package ph.scamguardian.ui.main

import android.content.Context
import androidx.annotation.AttrRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.use
import ph.scamguardian.R

/** Explains setup before opening Settings, using the same stock Material colors as the settings menu. */
@Composable
internal fun SetupDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors =
        remember(context, dark) {
            val themed = settingsThemeContext(context)
            SetupColors(
                background = themed.themeColor(android.R.attr.colorBackgroundFloating),
                text = themed.themeColor(android.R.attr.textColorPrimary),
                accent = themed.themeColor(android.R.attr.colorAccent),
            )
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = RoundedCornerShape(2.dp),
        containerColor = colors.background,
        titleContentColor = colors.text,
        textContentColor = colors.text,
        tonalElevation = 0.dp,
        title = {
            Text(stringResource(R.string.setup_title), fontSize = 20.sp, fontWeight = FontWeight.Medium)
        },
        text = {
            Text(
                stringResource(R.string.setup_message),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                fontSize = 16.sp,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.setup_decline), color = colors.accent)
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(stringResource(R.string.setup_accept), color = colors.accent)
            }
        },
    )
}

private fun Context.themeColor(
    @AttrRes attribute: Int,
): Color = obtainStyledAttributes(intArrayOf(attribute)).use { Color(it.getColor(0, 0)) }

private data class SetupColors(
    val background: Color,
    val text: Color,
    val accent: Color,
)
