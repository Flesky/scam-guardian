package ph.scamguardian.ui.main

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import ph.scamguardian.R
import ph.scamguardian.accessibility.GuardPreferences
import ph.scamguardian.accessibility.ScamAccessibilityService
import ph.scamguardian.core.Language
import ph.scamguardian.settings.LanguagePreferences
import ph.scamguardian.theme.ScamGuardianTheme
import ph.scamguardian.theme.SecuredGreen
import ph.scamguardian.theme.WarningRed
import ph.scamguardian.theme.securedColor
import ph.scamguardian.theme.unsecuredColor
import ph.scamguardian.ui.testmessage.FlatButton

private val OnColor = SecuredGreen
private val OffColor = WarningRed

// Well above the 48 dp minimum touch target.
private val ToggleSize = 220.dp
private val BodySize = 18.sp

/** What the user can do on the main screen. */
internal data class MainActions(
    val onToggle: () -> Unit = {},
    val onTurnOnProtection: () -> Unit = {},
    val onLanguageChange: (Language) -> Unit = {},
    val onTestMessageClick: () -> Unit = {},
    val onHistoryClick: () -> Unit = {},
)

@Composable
fun MainScreen(
    onTestMessageClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val guard = remember { GuardPreferences(context) }
    val languages = remember { LanguagePreferences(context) }
    var state by remember {
        mutableStateOf(MainUiState(guard.enabled, ScamAccessibilityService.isEnabled(context), languages.language))
    }
    // The user may have switched the service on or off in Settings while the app was in the background.
    LifecycleResumeEffect(Unit) {
        state = state.copy(isOn = guard.enabled, serviceEnabled = ScamAccessibilityService.isEnabled(context))
        onPauseOrDispose {}
    }

    fun setOn(isOn: Boolean) {
        guard.enabled = isOn
        state = state.copy(isOn = isOn)
        // The service does the reading; send the user to switch it on if it is off.
        if (isOn && !ScamAccessibilityService.isEnabled(context)) openAccessibilitySettings(context)
    }
    val actions =
        MainActions(
            onToggle = { setOn(!state.isOn) },
            onTurnOnProtection = { setOn(true) },
            onLanguageChange = { language ->
                languages.language = language
                state = state.copy(language = language)
            },
            onTestMessageClick = onTestMessageClick,
            onHistoryClick = onHistoryClick,
        )
    MainScreen(state = state, actions = actions, modifier = modifier)
}

private fun openAccessibilitySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
internal fun MainScreen(
    state: MainUiState,
    actions: MainActions,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    Column(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        BasicText(
            text = stringResource(R.string.app_name),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            style =
                TextStyle(
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
        )
        // Scrolls on small screens and with large fonts; the privacy line stays at the bottom.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PowerToggle(isOn = state.isOn, onToggle = actions.onToggle)
            ProtectionStatus(secured = state.secured, onTurnOnProtection = actions.onTurnOnProtection)
            LanguageToggle(selected = state.language, onSelect = actions.onLanguageChange, textColor = textColor)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlatButton(
                    label = stringResource(R.string.test_message_title),
                    onClick = actions.onTestMessageClick,
                    textColor = textColor,
                )
                FlatButton(
                    label = stringResource(R.string.history_title),
                    onClick = actions.onHistoryClick,
                    textColor = textColor,
                )
            }
        }
        PrivacyLine(textColor = textColor, modifier = Modifier.fillMaxWidth().padding(16.dp))
    }
}

@Composable
private fun PowerToggle(
    isOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = stringResource(if (isOn) R.string.toggle_state_on else R.string.toggle_state_off)
    Box(
        modifier =
            modifier
                .size(ToggleSize)
                .clip(CircleShape)
                .background(if (isOn) OnColor else OffColor)
                .clickable(role = Role.Switch, onClick = onToggle)
                .semantics { stateDescription = state },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = stringResource(if (isOn) R.string.toggle_label_on else R.string.toggle_label_off),
            style = TextStyle(color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun ProtectionStatus(
    secured: Boolean,
    onTurnOnProtection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            text = stringResource(if (secured) R.string.status_secured else R.string.status_not_secured),
            style =
                TextStyle(
                    color = if (secured) securedColor(dark) else unsecuredColor(dark),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
        )
        if (!secured) {
            Box(
                modifier =
                    Modifier
                        .background(OnColor)
                        .clickable(role = Role.Button, onClick = onTurnOnProtection)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = stringResource(R.string.turn_on_protection),
                    style = TextStyle(color = Color.White, fontSize = BodySize, fontWeight = FontWeight.Medium),
                )
            }
        }
    }
}

/** The two-option "EN | FIL" toggle for the language of the warning messages. */
@Composable
private fun LanguageToggle(
    selected: Language,
    onSelect: (Language) -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.language_toggle_description)
    Row(
        modifier =
            modifier
                .border(1.dp, textColor)
                .selectableGroup()
                .semantics { contentDescription = description },
    ) {
        listOf(Language.ENGLISH, Language.FILIPINO).forEach { language ->
            val isSelected = language == selected
            Box(
                modifier =
                    Modifier
                        .background(if (isSelected) textColor else Color.Transparent)
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(language) })
                        .defaultMinSize(minWidth = 72.dp, minHeight = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = stringResource(language.labelRes()),
                    style =
                        TextStyle(
                            color = if (isSelected) MaterialTheme.colorScheme.background else textColor,
                            fontSize = BodySize,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                )
            }
        }
    }
}

private fun Language.labelRes(): Int =
    when (this) {
        Language.ENGLISH -> R.string.language_en
        Language.FILIPINO -> R.string.language_fil
    }

@Composable
private fun PrivacyLine(
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_shield),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(textColor),
        )
        BasicText(
            text = stringResource(R.string.privacy_line),
            modifier = Modifier.weight(1f, fill = false),
            style = TextStyle(color = textColor, fontSize = 14.sp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenSecuredPreview() {
    ScamGuardianTheme { MainScreen(MainUiState(isOn = true, serviceEnabled = true), MainActions()) }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenNotSecuredPreview() {
    ScamGuardianTheme {
        MainScreen(MainUiState(isOn = true, serviceEnabled = false, language = Language.ENGLISH), MainActions())
    }
}
