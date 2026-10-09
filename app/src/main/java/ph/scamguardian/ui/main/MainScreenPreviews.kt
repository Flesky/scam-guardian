package ph.scamguardian.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import ph.scamguardian.core.parseWarnings
import ph.scamguardian.theme.ScamGuardianTheme

@Composable
private fun PreviewScreen(state: MainUiState) {
    val assets = LocalContext.current.assets
    val catalog = remember { parseWarnings(assets.open("warnings.json").bufferedReader().use { it.readText() }) }
    ScamGuardianTheme { MainScreen(state, HistoryUiState(emptyList(), state.language, catalog), MainActions()) }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenOnPreview() {
    PreviewScreen(MainUiState(isOn = true, serviceEnabled = true))
}

@Preview(showBackground = true)
@Composable
private fun MainScreenOffPreview() {
    PreviewScreen(MainUiState(isOn = false, serviceEnabled = false))
}
