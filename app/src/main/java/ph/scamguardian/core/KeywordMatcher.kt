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
    ): List<KeywordMatch> = match(tokens, keywords, ::matches)

    /**
     * Like [match], but every word must be spelled exactly. For everyday words such as "promise", where a
     * near miss ("promos") is a different word, not a typo.
     */
    fun matchExact(
        tokens: List<String>,
        keywords: Collection<String>,
    ): List<KeywordMatch> = match(tokens, keywords) { token, keyword -> token == keyword }

    private fun match(
        tokens: List<String>,
        keywords: Collection<String>,
        sameWord: (String, String) -> Boolean,
    ): List<KeywordMatch> =
        keywords.mapNotNull { keyword ->
            val phrase = phrases.getOrPut(keyword) { normalizer.tokens(keyword) }
            find(tokens, phrase, sameWord)?.let { KeywordMatch(keyword, it) }
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
        sameWord: (String, String) -> Boolean,
    ): List<String>? {
        if (phrase.isEmpty()) return null
        return tokens.windowed(phrase.size).firstOrNull { window ->
            window.indices.all { sameWord(window[it], phrase[it]) }
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
