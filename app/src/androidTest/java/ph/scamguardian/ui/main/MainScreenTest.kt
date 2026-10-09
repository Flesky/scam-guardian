package ph.scamguardian.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for [ph.scamguardian.ui.main.MainScreen].
 *
 * They use the stateless screen: toggling the real one saves the state and can open system settings.
 */
class MainScreenTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {
        composeTestRule.setContent {
            var state by remember { mutableStateOf(MainUiState(isOn = false, serviceEnabled = false)) }
            MainScreen(
                state = state,
                actions =
                    MainActions(
                        onToggle = { state = state.copy(isOn = !state.isOn) },
                        onTurnOnProtection = { state = state.copy(isOn = true, serviceEnabled = true) },
                        onLanguageChange = { state = state.copy(language = it) },
                    ),
            )
        }
    }

    @Test
    fun toggle_startsOff_andTurnsOnWhenTapped() {
        composeTestRule.onNodeWithText("OFF").assertExists().performClick()
        composeTestRule.onNodeWithText("ON").assertExists()
    }

    @Test
    fun status_isNotSecuredUntilProtectionIsTurnedOn() {
        composeTestRule.onNodeWithText("You are not secured").assertExists()
        composeTestRule.onNodeWithText("Turn on protection").performClick()

        composeTestRule.onNodeWithText("You are secured").assertExists()
        composeTestRule.onNodeWithText("Turn on protection").assertDoesNotExist()
    }

    @Test
    fun status_buttonOnButServiceOff_isNotSecured() {
        composeTestRule.onNodeWithText("OFF").performClick()

        composeTestRule.onNodeWithText("You are not secured").assertExists()
        composeTestRule.onNodeWithText("Turn on protection").assertExists()
    }

    @Test
    fun languageToggle_startsOnFilipino_andSwitchesToEnglish() {
        composeTestRule.onNodeWithText("FIL").assertIsSelected()
        composeTestRule.onNodeWithText("EN").performClick().assertIsSelected()
        composeTestRule.onNodeWithText("FIL").assertIsNotSelected()
    }

    @Test
    fun screen_showsThePrivacyLineAndTheLinks() {
        composeTestRule.onNodeWithText("Uses local engine only. Not connected to the internet.").assertExists()
        composeTestRule.onNodeWithText("Test a message").assertExists()
        composeTestRule.onNodeWithText("History").assertExists()
    }
}
