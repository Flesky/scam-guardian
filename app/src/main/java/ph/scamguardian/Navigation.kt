package ph.scamguardian

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import ph.scamguardian.ui.history.HistoryScreen
import ph.scamguardian.ui.main.MainScreen

@Composable
fun MainNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Main)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<Main> {
                    MainScreen(onHistoryClick = { backStack.add(History) })
                }
                entry<History> { HistoryScreen(onBack = { backStack.removeLastOrNull() }) }
            },
    )
}
