package ph.scamguardian.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ph.scamguardian.R

/** The small "Demo" switch in the header: filled when demo mode is on. */
@Composable
internal fun DemoSwitch(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.demo_mode_description)
    val state = stringResource(if (enabled) R.string.toggle_state_on else R.string.toggle_state_off)
    Box(
        modifier =
            modifier
                .border(1.dp, textColor)
                .background(if (enabled) textColor else Color.Transparent)
                .clickable(role = Role.Switch, onClick = { onChange(!enabled) })
                .semantics {
                    contentDescription = description
                    stateDescription = state
                }.defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = stringResource(R.string.demo_mode),
            style =
                TextStyle(
                    color = if (enabled) MaterialTheme.colorScheme.background else textColor,
                    fontSize = 14.sp,
                    fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal,
                ),
        )
    }
}
