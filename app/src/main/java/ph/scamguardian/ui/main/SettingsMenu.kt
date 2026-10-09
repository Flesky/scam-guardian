package ph.scamguardian.ui.main

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ph.scamguardian.R
import ph.scamguardian.core.Language
import kotlin.math.roundToInt

private const val DEMO_ITEM = 1
private const val LANGUAGE_ITEM = 2
private const val ICON_PADDING_DP = 12

/** What the settings menu can change. */
internal data class SettingsActions(
    val onDemoModeChange: (Boolean) -> Unit = {},
    val onLanguageChange: (Language) -> Unit = {},
)

/**
 * The gear button of the header. It opens the platform's popup menu, with a checkable "Demo mode" item
 * and an item that switches the language of the warnings.
 */
@Composable
internal fun SettingsButton(
    state: MainUiState,
    actions: SettingsActions,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.settings)
    // The click listener is set once; it must always see the latest state.
    val currentState by rememberUpdatedState(state)
    val currentActions by rememberUpdatedState(actions)
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                setImageResource(R.drawable.ic_settings)
                contentDescription = description
                val padding = (ICON_PADDING_DP * context.resources.displayMetrics.density).roundToInt()
                setPadding(padding, padding, padding, padding)
                setOnClickListener { showSettingsMenu(it, currentState, currentActions) }
            }
        },
        // A 24 dp icon in a 48 dp touch target.
        modifier = modifier.size(48.dp),
        update = { it.imageTintList = ColorStateList.valueOf(tint.toArgb()) },
    )
}

private fun showSettingsMenu(
    anchor: View,
    state: MainUiState,
    actions: SettingsActions,
) {
    val other = if (state.language == Language.FILIPINO) Language.ENGLISH else Language.FILIPINO
    val switchLabel = if (other == Language.ENGLISH) R.string.switch_to_english else R.string.switch_to_filipino
    PopupMenu(settingsThemeContext(anchor.context), anchor, Gravity.END).apply {
        menu.add(Menu.NONE, DEMO_ITEM, Menu.NONE, R.string.demo_mode).apply {
            isCheckable = true
            isChecked = state.demoMode
        }
        menu.add(Menu.NONE, LANGUAGE_ITEM, Menu.NONE, switchLabel)
        setOnMenuItemClickListener { item ->
            when (item.itemId) {
                DEMO_ITEM -> actions.onDemoModeChange(!state.demoMode)
                LANGUAGE_ITEM -> actions.onLanguageChange(other)
            }
            true
        }
        show()
    }
}

/** Shared stock Material theme for the settings menu and setup dialog, following system night mode. */
internal fun settingsThemeContext(context: Context): ContextThemeWrapper {
    val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    val theme =
        if (night == Configuration.UI_MODE_NIGHT_YES) {
            android.R.style.Theme_Material
        } else {
            android.R.style.Theme_Material_Light
        }
    return ContextThemeWrapper(context, theme)
}
