package ph.scamguardian.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfusablesTest {
    private val confusables = Confusables()

    @Test
    fun matches_lookAlikeSpellings() {
        val cases =
            listOf(
                "bd0" to "bdo",
                "BPl" to "BPI",
                "BPl" to "bpi",
                "BPI" to "bpi",
                "gcаsh" to "gcash",
                "ВРI" to "BPI",
                "srnart" to "smart",
            )

        cases.forEach { (text, target) ->
            assertTrue(
                "$text should look like $target",
                confusables.matches(text, target),
            )
        }
    }

    @Test
    fun matches_rejectsDifferentWords() {
        val cases = listOf("bdo" to "bpi", "ala" to "aia", "smart" to "start", "globe" to "glove")

        cases.forEach { (text, target) ->
            assertFalse("$text should not look like $target", confusables.matches(text, target))
        }
    }
}
