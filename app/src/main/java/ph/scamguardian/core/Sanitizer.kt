package ph.scamguardian.core

import java.text.Normalizer as UnicodeNormalizer

/**
 * Cleans raw message text in two steps. [prepare] only applies changes that a browser would also apply
 * to a link, so links are read from prepared text. [clean] then removes invisible characters, emojis
 * and extra whitespace from the words around the links.
 */
object Sanitizer {
    // Unicode treats the ideographic full stop as a domain separator, like ".".
    private val dotVariants = Regex("[\\u3002\\uFF61]")
    private val singleQuotes = Regex("[\\u2018\\u2019]")
    private val doubleQuotes = Regex("[\\u201C\\u201D]")
    private val dashes = Regex("[\\u2010-\\u2015]")

    private val invisible = Regex("[\\u200B-\\u200D\\u2060\\uFEFF\\u00AD\\uFE00-\\uFE0F\\u20D0-\\u20FF]")

    // Keeps letters, marks, digits, ASCII punctuation, whitespace and the peso sign.
    private val disallowed = Regex("[^\\p{L}\\p{M}\\p{N}\\p{Punct}\\s₱]+")
    private val whitespace = Regex("\\s+")

    /** Compatibility forms (NFKC), dot variants, quotes and dashes. Never joins two pieces of text. */
    fun prepare(text: String): String =
        UnicodeNormalizer
            .normalize(text, UnicodeNormalizer.Form.NFKC)
            .replace(dotVariants, ".")
            .replace(singleQuotes, "'")
            .replace(doubleQuotes, "\"")
            .replace(dashes, "-")

    /**
     * Removes invisible characters, emojis and decorative symbols, and collapses whitespace. Take the
     * links out first: removing a symbol from inside a host would turn it into a different host.
     */
    fun clean(prepared: String): String =
        prepared
            .replace(invisible, "")
            .replace(disallowed, "")
            .replace(whitespace, " ")
            .trim()

    fun sanitize(text: String): String = clean(prepare(text))
}
