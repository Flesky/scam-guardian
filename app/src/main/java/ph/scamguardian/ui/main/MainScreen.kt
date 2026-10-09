package ph.scamguardian.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ph.scamguardian.R
import ph.scamguardian.theme.ScamGuardianTheme

private val OnColor = Color(0xFF2E7D32)
private val OffColor = Color(0xFFC62828)

// Well above the 48 dp minimum touch target.
private val ToggleSize = 220.dp

@Composable
fun MainScreen(
    onTestMessageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOn by rememberSaveable { mutableStateOf(false) }
    MainScreen(
        isOn = isOn,
        onToggle = { isOn = !isOn },
        onTestMessageClick = onTestMessageClick,
        modifier = modifier,
    )
}

@Composable
internal fun MainScreen(
    isOn: Boolean,
    onToggle: () -> Unit,
    onTestMessageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PowerToggle(isOn = isOn, onToggle = onToggle)
                Box(
                    modifier =
                        Modifier
                            .padding(top = 24.dp)
                            .clickable(role = Role.Button, onClick = onTestMessageClick)
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.test_message_title), fontSize = 16.sp)
                }
            }
        }
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
        Text(
            text = stringResource(if (isOn) R.string.toggle_label_on else R.string.toggle_label_off),
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenOnPreview() {
    ScamGuardianTheme { MainScreen(isOn = true, onToggle = {}, onTestMessageClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenOffPreview() {
    ScamGuardianTheme { MainScreen(isOn = false, onToggle = {}, onTestMessageClick = {}) }
}
