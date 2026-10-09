package ph.scamguardian.ui.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ph.scamguardian.R
import ph.scamguardian.ScamGuardianApp
import ph.scamguardian.core.HistoryEntry
import ph.scamguardian.core.Language
import ph.scamguardian.core.WarningCatalog
import ph.scamguardian.settings.LanguagePreferences
import ph.scamguardian.storage.HistoryStore
import ph.scamguardian.theme.color
import ph.scamguardian.theme.icon
import ph.scamguardian.ui.testmessage.FlatButton
import java.io.IOException

// Large text for older users.
private val EntrySize = 18.sp
private const val QUOTE_LINES = 4

/** What the history screen shows: the past warnings, newest first, and how to word them. */
internal data class HistoryUiState(
    val entries: List<HistoryEntry>,
    val language: Language,
    val catalog: WarningCatalog,
)

/** The past warnings, newest first. They are read again each time the screen comes back to the front. */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as ScamGuardianApp
    val languages = remember { LanguagePreferences(app) }
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf(emptyList<HistoryEntry>()) }
    LifecycleResumeEffect(Unit) {
        scope.launch { entries = withContext(Dispatchers.IO) { app.history.entriesOrNone() } }
        onPauseOrDispose {}
    }
    HistoryScreen(
        state = HistoryUiState(entries, languages.language, app.engine.warnings),
        onBack = onBack,
        onClear = {
            entries = emptyList()
            scope.launch { withContext(Dispatchers.IO) { app.history.clearQuietly() } }
        },
        modifier = modifier,
    )
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
internal fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    Column(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BasicText(
                text = stringResource(R.string.history_title),
                style = TextStyle(color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Medium),
            )
            FlatButton(
                label = stringResource(R.string.test_message_back),
                onClick = onBack,
                textColor = textColor,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
        if (state.entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                BasicText(
                    text = stringResource(R.string.history_empty),
                    style = TextStyle(color = textColor, fontSize = EntrySize),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    HistoryRow(entry = entry, language = state.language, catalog = state.catalog, textColor = textColor)
                }
            }
        }
        FlatButton(
            label = stringResource(R.string.history_clear),
            onClick = onClear,
            textColor = textColor,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    language: Language,
    catalog: WarningCatalog,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val body = TextStyle(color = textColor, fontSize = EntrySize)
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(
            painter = painterResource(entry.severity.icon),
            // The title next to the icon says the same.
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            colorFilter = ColorFilter.tint(entry.severity.color),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(text = catalog.title(entry.type), style = body.copy(fontWeight = FontWeight.Bold))
            BasicText(text = catalog.message(entry.type, language, entry.brand), style = body)
            if (entry.text.isNotBlank()) {
                // The message that caused the warning, shortened to a few lines.
                BasicText(
                    text = "\u201C${entry.text}\u201D",
                    style = body.copy(fontStyle = FontStyle.Italic),
                    maxLines = QUOTE_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicText(text = "${entry.app} · ${entry.timeLabel()}", style = body)
            if (entry.markedNotScam) {
                BasicText(
                    text = stringResource(R.string.history_marked_not_scam),
                    style = body.copy(fontWeight = FontWeight.Medium),
                )
            }
        }
    }
}
