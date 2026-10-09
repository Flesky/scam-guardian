package ph.scamguardian

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import ph.scamguardian.ui.main.MainScreen
import ph.scamguardian.ui.testmessage.TestMessageScreen

@Composable
fun MainNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Main)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<Main> { MainScreen(onTestMessageClick = { backStack.add(TestMessage) }) }
                entry<TestMessage> { TestMessageScreen(onBack = { backStack.removeLastOrNull() }) }
            },
    )
}
