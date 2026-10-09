package ph.scamguardian.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefilterTest {
    private val cases: PrefilterCases = Fixtures.json.decodeFromString(Fixtures.text("prefilter_cases.json"))
    private val analyzer = Fixtures.analyzer()
    private val prefilter = Prefilter()

    private fun passes(text: String): Boolean = prefilter.passes(analyzer.analyze(Sanitizer.prepare(text)))

    @Test
    fun passes_messagesWithScamSignals() {
        cases.pass.forEach { text -> assertTrue("should pass: $text", passes(text)) }
    }

    @Test
    fun passes_rejectsOrdinaryMessages() {
        cases.fail.forEach { text -> assertFalse("should not pass: $text", passes(text)) }
    }

    @Test
    fun passes_rejectsTextShorterThanTheMinimum() {
        assertFalse(passes("verify"))
        assertFalse(Prefilter(minTokens = 3).passes(analyzer.analyze("gcash verify")))
    }
}
