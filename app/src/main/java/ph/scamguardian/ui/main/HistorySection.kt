package ph.scamguardian.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ph.scamguardian.R
import ph.scamguardian.core.HistoryEntry
import ph.scamguardian.core.Language
import ph.scamguardian.core.WarningCatalog
import ph.scamguardian.theme.color
import ph.scamguardian.theme.icon
import ph.scamguardian.ui.FlatButton

private val EntrySize = 18.sp
private val DetailSize = 15.sp
private const val QUOTE_LINES = 4
private const val DIVIDER_ALPHA = 0.15f
private const val OUTLINE_ALPHA = 0.4f
private const val DETAIL_ALPHA = 0.75f

/** The past warnings, newest first, and how to word them. */
internal data class HistoryUiState(
    val entries: List<HistoryEntry>,
    val language: Language,
    val catalog: WarningCatalog,
)

/** The history part of the main screen's list: a title, then the entries or the empty state. */
internal fun LazyListScope.historySection(
    history: HistoryUiState,
    textColor: Color,
    onClear: () -> Unit,
) {
    item(key = "history-title") {
        BasicText(
            text = stringResource(R.string.history_title),
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            style = TextStyle(color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Medium),
        )
    }
    if (history.entries.isEmpty()) {
        item(key = "history-empty") {
            BasicText(
                text = stringResource(R.string.history_empty),
                modifier = Modifier.padding(vertical = 16.dp),
                style = TextStyle(color = textColor.copy(alpha = DETAIL_ALPHA), fontSize = EntrySize),
            )
        }
    } else {
        items(history.entries, key = { it.id }) { entry ->
            HistoryRow(entry = entry, history = history, textColor = textColor)
            Box(Modifier.fillMaxWidth().height(1.dp).background(textColor.copy(alpha = DIVIDER_ALPHA)))
        }
        item(key = "history-clear") {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                FlatButton(
                    label = stringResource(R.string.history_clear),
                    onClick = onClear,
                    textColor = textColor,
                    modifier = Modifier.border(1.dp, textColor.copy(alpha = OUTLINE_ALPHA)),
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    history: HistoryUiState,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val body = TextStyle(color = textColor, fontSize = EntrySize)
    val detail = TextStyle(color = textColor.copy(alpha = DETAIL_ALPHA), fontSize = DetailSize)
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(entry.severity.icon),
            // The title next to the icon says the same.
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            colorFilter = ColorFilter.tint(entry.severity.color),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(text = history.catalog.title(entry.type), style = body.copy(fontWeight = FontWeight.Bold))
            BasicText(text = history.catalog.message(entry.type, history.language, entry.brand), style = body)
            if (entry.text.isNotBlank()) {
                // The message that caused the warning, shortened to a few lines.
                BasicText(
                    text = "“${entry.text}”",
                    style = detail.copy(fontStyle = FontStyle.Italic),
                    maxLines = QUOTE_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicText(text = "${entry.app} · ${entry.timeLabel()}", style = detail)
            if (entry.markedNotScam) {
                BasicText(
                    text = stringResource(R.string.history_marked_not_scam),
                    style = detail.copy(fontWeight = FontWeight.Medium),
                )
            }
        }
    }
}
