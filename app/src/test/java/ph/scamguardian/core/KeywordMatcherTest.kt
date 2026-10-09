package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordMatcherTest {
    private val normalizer = Normalizer()
    private val matcher = KeywordMatcher(normalizer)

    @Test
    fun matches_fuzzyForLongKeywords() {
        val cases = listOf("bonos" to "bonus", "gcazh" to "gcash", "pdala" to "padala", "maswerteng" to "maswerte")

        cases.forEach { (token, keyword) ->
            assertTrue(
                "$token should match $keyword",
                matcher.matches(token, keyword),
            )
        }
    }

    @Test
    fun matches_rejectsDifferentWords() {
        val cases = listOf("pala" to "padala", "plan" to "play", "codes" to "code", "bt" to "bet", "pl" to "play")

        cases.forEach { (token, keyword) ->
            assertFalse("$token should not match $keyword", matcher.matches(token, keyword))
        }
    }

    @Test
    fun match_returnsKeywordsWithTheirTokens() {
        val tokens = normalizer.tokens("GCazh top up bonos, ay ganun pala")

        val matches = matcher.match(tokens, listOf("gcash", "top up", "bonus", "padala", "good luck"))

        assertEquals(
            listOf(
                KeywordMatch("gcash", listOf("gcazh")),
                KeywordMatch("top up", listOf("top", "up")),
                KeywordMatch("bonus", listOf("bonos")),
            ),
            matches,
        )
    }
}
