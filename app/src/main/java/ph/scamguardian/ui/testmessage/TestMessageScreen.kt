package ph.scamguardian.ui.testmessage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ph.scamguardian.R
import ph.scamguardian.ScamGuardianApp
import ph.scamguardian.core.GateResult
import ph.scamguardian.core.PipelineReport
import ph.scamguardian.theme.ScamGuardianTheme

private val CheckColor = Color(0xFF2E7D32)
private val ErrorColor = Color(0xFFC62828)
private const val DISABLED_ALPHA = 0.4f

@Composable
fun TestMessageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TestMessageViewModel = testMessageViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TestMessageScreen(
        state = state,
        onBack = onBack,
        onTextChange = viewModel::onTextChange,
        onCheck = viewModel::check,
        modifier = modifier,
    )
}

@Composable
private fun testMessageViewModel(): TestMessageViewModel {
    val engine = (LocalContext.current.applicationContext as ScamGuardianApp).engine
    return viewModel { TestMessageViewModel(engine) }
}

@Composable
internal fun TestMessageScreen(
    state: TestMessageState,
    onBack: () -> Unit,
    onTextChange: (String) -> Unit,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    // Safe drawing insets include the keyboard, and are applied before the scroll.
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(onBack = onBack, textColor = textColor)
        QuickFillRow(onPick = onTextChange, textColor = textColor)
        MessageField(text = state.text, onTextChange = onTextChange, textColor = textColor)
        FlatButton(
            label = stringResource(R.string.test_message_check),
            onClick = onCheck,
            textColor = Color.White,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .alpha(if (state.canCheck) 1f else DISABLED_ALPHA)
                    .background(CheckColor),
            enabled = state.canCheck,
        )
        Status(state = state, textColor = textColor)
        state.result?.let { result -> ResultRows(rows = CheckResultFormatter.rows(result), textColor = textColor) }
    }
}

@Composable
private fun Header(
    onBack: () -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BasicText(
            text = stringResource(R.string.test_message_title),
            style = TextStyle(color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Medium),
        )
        FlatButton(
            label = stringResource(R.string.test_message_back),
            onClick = onBack,
            textColor = textColor,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

@Composable
private fun QuickFillRow(
    onPick: (String) -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sampleMessages.forEach { sample ->
            FlatButton(
                label = sample.label,
                onClick = { onPick(sample.text) },
                textColor = textColor,
                modifier = Modifier.border(1.dp, textColor.copy(alpha = DISABLED_ALPHA)),
            )
        }
    }
}

@Composable
private fun MessageField(
    text: String,
    onTextChange: (String) -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp)
                .border(1.dp, textColor.copy(alpha = DISABLED_ALPHA))
                .padding(12.dp),
        textStyle = TextStyle(color = textColor, fontSize = 16.sp),
        cursorBrush = SolidColor(textColor),
        decorationBox = { field ->
            if (text.isEmpty()) {
                BasicText(
                    text = stringResource(R.string.test_message_hint),
                    style = TextStyle(color = textColor.copy(alpha = DISABLED_ALPHA), fontSize = 16.sp),
                )
            }
            field()
        },
    )
}

@Composable
private fun Status(
    state: TestMessageState,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val status =
        when {
            state.error != null -> state.error
            state.loading -> stringResource(R.string.test_message_loading)
            state.checking -> stringResource(R.string.test_message_checking)
            else -> null
        }
    if (status != null) {
        BasicText(
            text = status,
            modifier = modifier,
            style = TextStyle(color = if (state.error != null) ErrorColor else textColor, fontSize = 14.sp),
        )
    }
}

@Composable
private fun ResultRows(
    rows: List<ResultRow>,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Column {
                BasicText(
                    text = row.label,
                    style = TextStyle(color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                )
                BasicText(text = row.value, style = TextStyle(color = textColor, fontSize = 16.sp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TestMessageLoadingPreview() {
    ScamGuardianTheme { TestMessageScreen(TestMessageState(), onBack = {}, onTextChange = {}, onCheck = {}) }
}

@Preview(showBackground = true)
@Composable
private fun TestMessageResultPreview() {
    val report =
        PipelineReport(
            warning = null,
            rule = null,
            gate = GateResult(false, listOf("no link, money or keywords")),
            aiScore = null,
            aiThreshold = 0.6f,
            modelCalls = 0,
        )
    val state = TestMessageState(text = "Kain na tayo", loading = false, result = CheckResult(report, totalMs = 3))
    ScamGuardianTheme { TestMessageScreen(state, onBack = {}, onTextChange = {}, onCheck = {}) }
}
