package ph.scamguardian.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ph.scamguardian.R
import ph.scamguardian.ui.testmessage.FlatButton

/**
 * Placeholder destination for the "History" link on the main screen.
 *
 * Not built yet: the list of past warnings, its storage and "Clear history" (docs/ui_spec.md, "History screen").
 */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
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
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            BasicText(
                text = stringResource(R.string.history_empty),
                style = TextStyle(color = textColor, fontSize = 18.sp),
            )
        }
    }
}
