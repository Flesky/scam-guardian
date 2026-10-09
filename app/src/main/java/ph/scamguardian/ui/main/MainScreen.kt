package ph.scamguardian.ui.main

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ph.scamguardian.R
import ph.scamguardian.ScamGuardianApp
import ph.scamguardian.accessibility.GuardPreferences
import ph.scamguardian.accessibility.OpenWarning
import ph.scamguardian.accessibility.ScamAccessibilityService
import ph.scamguardian.core.HistoryEntry
import ph.scamguardian.settings.DemoPreferences
import ph.scamguardian.settings.LanguagePreferences
import ph.scamguardian.storage.HistoryStore
import ph.scamguardian.theme.SecuredGreen
import ph.scamguardian.theme.securedColor
import ph.scamguardian.theme.unsecuredColor
import java.io.IOException

// Well above the 48 dp minimum touch target.
private val ToggleMinWidth = 200.dp
private val ToggleMinHeight = 60.dp
private val ToggleOutline = 2.dp
private const val TOGGLE_OUTLINE_ALPHA = 0.3f

// The history is the list item right after the first screen.
private const val HISTORY_ITEM = 1
private val HintLift = 6.dp
private const val HINT_PERIOD_MS = 900
private const val HINT_MIN_ALPHA = 0.4f
private const val HINT_TEXT_ALPHA = 0.75f

/** What the user can do on the main screen. */
internal data class MainActions(
    val onToggle: () -> Unit = {},
    val onClearHistory: () -> Unit = {},
    val settings: SettingsActions = SettingsActions(),
)

/** The app's one screen: the mascot, the on/off button, the status, and under them the history of warnings. */
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as ScamGuardianApp
    val guard = remember { GuardPreferences(context) }
    val languages = remember { LanguagePreferences(context) }
    val demo = remember { DemoPreferences(context) }
    val scope = rememberCoroutineScope()
    var state by remember {
        mutableStateOf(
            MainUiState(guard.enabled, ScamAccessibilityService.isEnabled(context), languages.language, demo.enabled),
        )
    }
    var entries by remember { mutableStateOf(emptyList<HistoryEntry>()) }
    // While the app was in the background the user may have changed the service in Settings, and new
    // warnings may have been added to the history.
    LifecycleResumeEffect(Unit) {
        // A banner about another app's message must not stay on top of this app.
        OpenWarning.dismiss?.invoke()
        state = state.copy(isOn = guard.enabled, serviceEnabled = ScamAccessibilityService.isEnabled(context))
        scope.launch { entries = withContext(Dispatchers.IO) { app.history.entriesOrNone() } }
        onPauseOrDispose {}
    }
    val actions =
        MainActions(
            // The button shows whether the user is protected. Turning it on while the service is off
            // opens the system settings, where the service is switched on.
            onToggle = {
                val turnOn = !state.secured
                guard.enabled = turnOn
                state = state.copy(isOn = turnOn)
                if (turnOn && !ScamAccessibilityService.isEnabled(context)) openAccessibilitySettings(context)
            },
            onClearHistory = {
                entries = emptyList()
                scope.launch { withContext(Dispatchers.IO) { app.history.clearQuietly() } }
            },
            settings =
                SettingsActions(
                    onDemoModeChange = { enabled ->
                        demo.enabled = enabled
                        state = state.copy(demoMode = enabled)
                    },
                    onLanguageChange = { language ->
                        languages.language = language
                        state = state.copy(language = language)
                    },
                ),
        )
    MainScreen(
        state = state,
        history = HistoryUiState(entries, state.language, app.engine.warnings),
        actions = actions,
        modifier = modifier,
    )
}

private fun openAccessibilitySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

// A history that cannot be read is shown as empty; one that cannot be cleared stays as it is.
private fun HistoryStore.entriesOrNone(): List<HistoryEntry> =
    try {
        entries()
    } catch (_: IOException) {
        emptyList()
    }

private fun HistoryStore.clearQuietly() {
    try {
        clear()
    } catch (_: IOException) {
        // Nothing to do: the entries show again the next time the screen opens.
    }
}

@Composable
internal fun MainScreen(
    state: MainUiState,
    history: HistoryUiState,
    actions: MainActions,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val sounds = rememberMascotSounds()
    Column(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            BasicText(
                text = stringResource(R.string.app_name),
                modifier = Modifier.align(Alignment.Center),
                style = TextStyle(color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Medium),
            )
            SettingsButton(
                state = state,
                actions = actions.settings,
                tint = textColor,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        // One list. The button and the status fill the screen; the history starts below it, so the
        // user scrolls to it on purpose.
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item(key = "protection") {
                Box(modifier = Modifier.fillParentMaxHeight().fillMaxWidth()) {
                    Protection(
                        state = state,
                        onToggle = actions.onToggle,
                        onMascotMove = { sounds?.play(it) },
                        textColor = textColor,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    ScrollHint(
                        onClick = { scope.launch { listState.animateScrollToItem(HISTORY_ITEM) } },
                        textColor = textColor,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            historySection(history = history, textColor = textColor, onClear = actions.onClearHistory)
        }
    }
}

@Composable
private fun Protection(
    state: MainUiState,
    onToggle: () -> Unit,
    onMascotMove: (MascotMove) -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Tapping the mascot toggles too, but screen readers only get the button under it.
        Mascot(
            isOn = state.secured,
            onMove = onMascotMove,
            modifier =
                Modifier
                    .clearAndSetSemantics {}
                    .clickable(interactionSource = null, indication = null, onClick = onToggle),
        )
        ToggleButton(isOn = state.secured, onToggle = onToggle, textColor = textColor)
        BasicText(
            text = stringResource(if (state.secured) R.string.status_secured else R.string.status_not_secured),
            style =
                TextStyle(
                    color = if (state.secured) securedColor(dark) else unsecuredColor(dark),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
        )
        PrivacyLine(textColor = textColor)
    }
}

/** "Scroll up for history" under two chevrons that drift upwards, at the bottom of the first screen. */
@Composable
private fun ScrollHint(
    onClick: () -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "scroll hint")
    val lift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = HINT_PERIOD_MS), RepeatMode.Reverse),
        label = "chevron lift",
    )
    Column(
        modifier =
            modifier
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_chevrons_up),
            contentDescription = null,
            modifier =
                Modifier.size(28.dp).graphicsLayer {
                    translationY = -lift * HintLift.toPx()
                    alpha = HINT_MIN_ALPHA + (1f - HINT_MIN_ALPHA) * lift
                },
            colorFilter = ColorFilter.tint(textColor),
        )
        BasicText(
            text = stringResource(R.string.scroll_for_history),
            style = TextStyle(color = textColor.copy(alpha = HINT_TEXT_ALPHA), fontSize = 14.sp),
        )
    }
}

@Composable
private fun ToggleButton(
    isOn: Boolean,
    onToggle: () -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val state = stringResource(if (isOn) R.string.toggle_state_on else R.string.toggle_state_off)
    val shape = RoundedCornerShape(percent = 50)
    // Green and filled to invite turning protection on; a quiet outline once it is on.
    val look =
        if (isOn) {
            Modifier.border(ToggleOutline, textColor.copy(alpha = TOGGLE_OUTLINE_ALPHA), shape)
        } else {
            Modifier.background(SecuredGreen)
        }
    Box(
        modifier =
            modifier
                .defaultMinSize(minWidth = ToggleMinWidth, minHeight = ToggleMinHeight)
                .clip(shape)
                .then(look)
                .clickable(role = Role.Switch, onClick = onToggle)
                .semantics { stateDescription = state }
                .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = stringResource(if (isOn) R.string.toggle_turn_off else R.string.toggle_turn_on),
            style =
                TextStyle(
                    color = if (isOn) textColor else Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
        )
    }
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
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(textColor),
        )
        BasicText(
            text = stringResource(R.string.privacy_line),
            modifier = Modifier.weight(1f, fill = false),
            style = TextStyle(color = textColor, fontSize = 14.sp),
        )
    }
}
