package ph.scamguardian.core

import org.apache.commons.text.similarity.LevenshteinDistance

data class KeywordMatch(
    val keyword: String,
    val tokens: List<String>,
)

/** Finds keywords and phrases in normalized tokens, tolerating dropped vowels and small typos in longer words. */
class KeywordMatcher(
    private val normalizer: Normalizer,
) {
    private val levenshtein = LevenshteinDistance(LONG_KEYWORD_DISTANCE)
    private val phrases = HashMap<String, List<String>>()

    /** Returns each keyword found in [tokens] with the tokens that matched it. */
    fun match(
        tokens: List<String>,
        keywords: Collection<String>,
    ): List<KeywordMatch> =
        keywords.mapNotNull { keyword ->
            val phrase = phrases.getOrPut(keyword) { normalizer.tokens(keyword) }
            find(tokens, phrase)?.let { KeywordMatch(keyword, it) }
        }

    fun matches(
        token: String,
        keyword: String,
    ): Boolean =
        when {
            token == keyword -> true
            keyword.length < FUZZY_MIN_LENGTH -> false
            else -> sameConsonants(token, keyword) || withinDistance(token, keyword)
        }

    private fun find(
        tokens: List<String>,
        phrase: List<String>,
    ): List<String>? {
        if (phrase.isEmpty()) return null
        return tokens.windowed(phrase.size).firstOrNull { window ->
            window.indices.all { matches(window[it], phrase[it]) }
        }
    }

    private fun sameConsonants(
        token: String,
        keyword: String,
    ): Boolean {
        val skeleton = token.filterNot { it in VOWELS }
        return skeleton.length >= SKELETON_MIN_LENGTH && skeleton == keyword.filterNot { it in VOWELS }
    }

    private fun withinDistance(
        token: String,
        keyword: String,
    ): Boolean {
        val allowed = if (keyword.length >= LONG_KEYWORD_LENGTH) LONG_KEYWORD_DISTANCE else 1
        // LevenshteinDistance returns -1 when the distance is over its threshold.
        return levenshtein.apply(token, keyword) in 0..allowed
    }

    private companion object {
        const val VOWELS = "aeiou"
        const val FUZZY_MIN_LENGTH = 5
        const val SKELETON_MIN_LENGTH = 3
        const val LONG_KEYWORD_LENGTH = 8
        const val LONG_KEYWORD_DISTANCE = 2
    }
}
