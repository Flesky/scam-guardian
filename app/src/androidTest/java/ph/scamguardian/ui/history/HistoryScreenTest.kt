package ph.scamguardian.ui.history

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import ph.scamguardian.core.HistoryEntry
import ph.scamguardian.core.Language
import ph.scamguardian.core.Severity
import ph.scamguardian.core.WarningType
import ph.scamguardian.core.parseWarnings

/** UI tests for the stateless [HistoryScreen]; the real one reads the history file of the app. */
class HistoryScreenTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val catalog =
        parseWarnings(
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext.assets
                .open("warnings.json")
                .bufferedReader()
                .use { it.readText() },
        )

    private val entries =
        listOf(
            HistoryEntry(
                id = "2",
                type = WarningType.FAKE_LINK,
                severity = Severity.RED,
                brand = "BDO",
                app = "Messenger",
                timeMs = 1_000L,
            ),
            HistoryEntry(
                id = "1",
                type = WarningType.RISKY_LINK,
                severity = Severity.AMBER,
                app = "Viber",
                timeMs = 500L,
                markedNotScam = true,
            ),
        )

    private fun show(
        entries: List<HistoryEntry>,
        language: Language = Language.ENGLISH,
    ) {
        composeTestRule.setContent {
            var shown by remember { mutableStateOf(entries) }
            HistoryScreen(
                state = HistoryUiState(shown, language, catalog),
                onBack = {},
                onClear = { shown = emptyList() },
            )
        }
    }

    @Test
    fun noEntries_showsTheEmptyState() {
        show(emptyList())

        composeTestRule.onNodeWithText("No warnings yet.").assertExists()
    }

    @Test
    fun entries_showTitleMessageAppAndMark() {
        show(entries)

        composeTestRule.onNodeWithText("Scam detected").assertExists()
        composeTestRule
            .onNodeWithText("Not a real link of BDO. Do not give your OTP or personal info.")
            .assertExists()
        composeTestRule.onNodeWithText("Messenger", substring = true).assertExists()
        composeTestRule.onNodeWithText("Possible scam detected").assertExists()
        composeTestRule.onNodeWithText("Marked not a scam").assertExists()
    }

    @Test
    fun entries_followTheSelectedLanguage() {
        show(entries, Language.FILIPINO)

        composeTestRule.onNodeWithText("Mag-ingat. Kahina-hinala ang link na ito.").assertExists()
    }

    @Test
    fun clearHistory_removesTheEntries() {
        show(entries)

        composeTestRule.onNodeWithText("Clear history").performClick()

        composeTestRule.onNodeWithText("No warnings yet.").assertExists()
    }
}
