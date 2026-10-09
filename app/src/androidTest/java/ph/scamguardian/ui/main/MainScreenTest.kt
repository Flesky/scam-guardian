package ph.scamguardian.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Rule
import org.junit.Test
import ph.scamguardian.core.HistoryEntry
import ph.scamguardian.core.Language
import ph.scamguardian.core.Severity
import ph.scamguardian.core.WarningType
import ph.scamguardian.core.parseWarnings

/**
 * UI tests for [ph.scamguardian.ui.main.MainScreen].
 *
 * They use the stateless screen: toggling the real one saves the state and can open system settings.
 */
class MainScreenTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val catalog by lazy {
        parseWarnings(
            composeTestRule.activity.assets
                .open("warnings.json")
                .bufferedReader()
                .use { it.readText() },
        )
    }

    private val entries =
        listOf(
            HistoryEntry("2", WarningType.FAKE_LINK, Severity.RED, brand = "BDO", app = "Messenger", timeMs = 1_000L),
            HistoryEntry(
                "1",
                WarningType.RISKY_LINK,
                Severity.AMBER,
                app = "Viber",
                timeMs = 500L,
                markedNotScam = true,
            ),
        )

    // Turning the button on switches the service on too, as if the user had finished the setup.
    private fun show(
        initial: MainUiState = MainUiState(isOn = false, serviceEnabled = false, language = Language.ENGLISH),
        history: List<HistoryEntry> = emptyList(),
    ) {
        composeTestRule.setContent {
            var state by remember { mutableStateOf(initial) }
            var shown by remember { mutableStateOf(history) }
            MainScreen(
                state = state,
                history = HistoryUiState(shown, state.language, catalog),
                actions =
                    MainActions(
                        onToggle = { state = state.copy(isOn = !state.secured, serviceEnabled = true) },
                        onClearHistory = { shown = emptyList() },
                        settings =
                            SettingsActions(
                                onDemoModeChange = { state = state.copy(demoMode = it) },
                                onLanguageChange = { state = state.copy(language = it) },
                            ),
                    ),
            )
        }
    }

    private fun scrollTo(text: String) {
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText(text, substring = true))
    }

    @Test
    fun toggle_startsOff_andTurnsOnWhenTapped() {
        show()

        composeTestRule.onNodeWithText("You are not secured").assertExists()
        composeTestRule.onNodeWithText("Scroll up for history").assertExists()
        composeTestRule.onNodeWithText("OFF").assertExists().performClick()

        composeTestRule.onNodeWithText("ON").assertExists()
        composeTestRule.onNodeWithText("You are secured").assertExists()
    }

    @Test
    fun toggle_staysOffUntilTheServiceIsEnabled() {
        show(MainUiState(isOn = true, serviceEnabled = false))

        composeTestRule.onNodeWithText("OFF").assertExists()
        composeTestRule.onNodeWithText("You are not secured").assertExists()
        composeTestRule.onNodeWithText("Turn on protection").assertDoesNotExist()
    }

    @Test
    fun history_empty_showsTheEmptyStateOnTheSameScreen() {
        show()

        scrollTo("No warnings yet.")
        composeTestRule.onNodeWithText("No warnings yet.").assertExists()
        composeTestRule.onNodeWithText("Clear history").assertDoesNotExist()
    }

    @Test
    fun history_entries_areListedAndCanBeCleared() {
        show(history = entries)

        scrollTo("Not a real link of BDO")
        composeTestRule.onNodeWithText("Not a real link of BDO. Do not give your OTP or personal info.").assertExists()
        scrollTo("Marked not a scam")
        composeTestRule.onNodeWithText("Marked not a scam").assertExists()
        scrollTo("Clear history")
        composeTestRule.onNodeWithText("Clear history").performClick()

        composeTestRule.onNodeWithText("No warnings yet.").assertExists()
    }

    @Test
    fun settings_gearOpensAMenuWithDemoModeAndTheOtherLanguage() {
        show(history = entries)

        onView(withContentDescription("Settings")).perform(click())
        onView(withText("Demo mode")).inRoot(isPlatformPopup()).check(matches(isDisplayed()))
        onView(withText("Switch to Filipino")).inRoot(isPlatformPopup()).perform(click())

        scrollTo("Hindi ito ang tunay na link ng BDO")
        composeTestRule.onNodeWithText("Hindi ito ang tunay na link ng BDO", substring = true).assertExists()
    }
}
