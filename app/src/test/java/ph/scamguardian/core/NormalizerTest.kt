package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizerTest {
    private val normalizer = Fixtures.normalizer()

    @Test
    fun tokens_normalizesEachCase() {
        val cases =
            mapOf(
                "Padala NA" to listOf("padala", "na"),
                "regaló piña" to listOf("regalo", "pina"),
                "p a d a l a ka muna" to listOf("padala", "ka", "muna"),
                "P.A.D.A.L.A" to listOf("padala"),
                "g-c-a-s-h" to listOf("gcash"),
                "a b" to listOf("a", "b"),
                "p4dala b0nu5 p@dala" to listOf("padala", "bonus", "padala"),
                "send 500 to 0917" to listOf("send", "500", "to", "0917"),
                "pahiraaaam" to listOf("pahiram"),
                "SSS www" to listOf("sss", "www"),
                "aq pls d2 nmn" to listOf("ako", "please", "dito", "naman"),
                "mag-login, now!" to listOf("mag", "login", "now"),
            )

        cases.forEach { (input, expected) -> assertEquals(input, expected, normalizer.tokens(input)) }
    }
}
