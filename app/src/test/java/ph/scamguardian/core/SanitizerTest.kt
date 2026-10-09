package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SanitizerTest {
    @Test
    fun sanitize_cleansEachKindOfNoise() {
        val cases =
            mapOf(
                "𝐆𝐂𝐀𝐒𝐇 verify" to "GCASH verify",
                "gc​ash­ ver⁠ify﻿" to "gcash verify",
                "Congrats 🎉🎁 claim ✨ now" to "Congrats claim now",
                "channel：https://a.b/c" to "channel:https://a.b/c",
                "  line one\n\nline   two\t" to "line one line two",
                "₱10,000 or \$5 @juan 50% (promo) a-b" to "₱10,000 or \$5 @juan 50% (promo) a-b",
                "Don’t share “this”" to "Don't share \"this\"",
            )

        cases.forEach { (input, expected) -> assertEquals(input, expected, Sanitizer.sanitize(input)) }
    }
}
