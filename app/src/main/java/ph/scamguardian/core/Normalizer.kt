package ph.scamguardian.core

import java.text.Normalizer as UnicodeNormalizer

/** Turns text into comparable tokens: lowercase, no accents, leetspeak and texting shortcuts undone. */
class Normalizer(
    shortcuts: Map<String, String> = emptyMap(),
) {
    private val shortcuts = shortcuts.mapKeys { it.key.lowercase() }

    fun tokens(text: String): List<String> {
        val folded = UnicodeNormalizer.normalize(text.lowercase(), UnicodeNormalizer.Form.NFD).replace(marks, "")
        val joined = spacedCharacters.replace(folded) { match -> match.value.filter(Char::isLetterOrDigit) }
        val letters = symbolsInsideWords.replace(joined) { match -> LEET_SYMBOLS.getValue(match.value[0]).toString() }
        return separators.split(letters).filter(String::isNotEmpty).flatMap(::normalizeToken)
    }

    private fun normalizeToken(token: String): List<String> {
        val leet = if (token.any(Char::isLetter)) token.map { LEET_DIGITS[it] ?: it }.joinToString("") else token
        // A token of one repeated letter ("sss", "www") is a name, not a stretched word.
        val collapsed = if (leet.toSet().size == 1) leet else repeatedLetters.replace(leet, "$1")
        return (shortcuts[collapsed] ?: collapsed).split(' ').filter(String::isNotEmpty)
    }

    private companion object {
        val marks = Regex("\\p{M}+")
        val separators = Regex("[^\\p{L}\\p{N}]+")
        val repeatedLetters = Regex("(\\p{L})\\1{2,}")

        // Three or more single characters separated by spaces, dots or dashes: "p a d a l a".
        val spacedCharacters =
            Regex("(?<![\\p{L}\\p{N}])[\\p{L}\\p{N}](?:[ .\\-][\\p{L}\\p{N}]){2,}(?![\\p{L}\\p{N}])")
        val symbolsInsideWords = Regex("(?<=\\p{L})[@!$](?=\\p{L})")

        val LEET_SYMBOLS = mapOf('@' to 'a', '!' to 'i', '$' to 's')
        val LEET_DIGITS =
            mapOf('4' to 'a', '3' to 'e', '1' to 'i', '0' to 'o', '5' to 's', '6' to 'g', '9' to 'g', '7' to 't')
    }
}
