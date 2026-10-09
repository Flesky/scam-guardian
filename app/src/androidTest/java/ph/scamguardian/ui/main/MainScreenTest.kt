package ph.scamguardian.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [ph.scamguardian.ui.main.MainScreen]. */
class MainScreenTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {
        composeTestRule.setContent { MainScreen(onTestMessageClick = {}) }
    }

    @Test
    fun toggle_startsOff_andTurnsOnWhenTapped() {
        composeTestRule.onNodeWithText("OFF").assertExists().performClick()
        composeTestRule.onNodeWithText("ON").assertExists()
    }
}
