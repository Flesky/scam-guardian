package ph.scamguardian.core

import java.text.Normalizer as UnicodeNormalizer

/** Cleans raw message text: compatibility forms, invisible characters, emojis and extra whitespace. */
object Sanitizer {
    private val invisible = Regex("[\\u200B-\\u200D\\u2060\\uFEFF\\u00AD\\uFE00-\\uFE0F\\u20D0-\\u20FF]")
    private val singleQuotes = Regex("[\\u2018\\u2019]")
    private val doubleQuotes = Regex("[\\u201C\\u201D]")
    private val dashes = Regex("[\\u2010-\\u2015]")

    // Keeps letters, marks, digits, ASCII punctuation, whitespace and the peso sign.
    private val disallowed = Regex("[^\\p{L}\\p{M}\\p{N}\\p{Punct}\\s₱]")
    private val whitespace = Regex("\\s+")

    fun sanitize(text: String): String =
        UnicodeNormalizer
            .normalize(text, UnicodeNormalizer.Form.NFKC)
            .replace(invisible, "")
            .replace(singleQuotes, "'")
            .replace(doubleQuotes, "\"")
            .replace(dashes, "-")
            .replace(disallowed, "")
            .replace(whitespace, " ")
            .trim()
}
