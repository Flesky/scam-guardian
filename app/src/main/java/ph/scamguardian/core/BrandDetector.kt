package ph.scamguardian.core

/** A brand found in a message: named in the text, inside link hosts, or both. */
data class BrandMatch(
    val brand: Brand,
    val inText: Boolean,
    val hosts: List<String>,
)

/** Finds brand names in message text and inside link hosts, including look-alike spellings. */
class BrandDetector(
    brands: List<Brand>,
    private val normalizer: Normalizer,
    private val confusables: Confusables,
) {
    private val entries = brands.map(::Entry)

    fun detect(
        text: String,
        hosts: List<String> = emptyList(),
    ): List<BrandMatch> {
        val named = inText(text)
        return entries.mapNotNull { entry ->
            val inText = entry.brand in named
            val inHosts = hosts.filter { host -> entry.isIn(labelCandidates(host)) }
            if (inText || inHosts.isNotEmpty()) BrandMatch(entry.brand, inText, inHosts) else null
        }
    }

    /** Brands named in [text] as whole words. Brands with context words also need one of those words. */
    fun inText(text: String): List<Brand> {
        val tokens = normalizer.tokens(text)
        val forms = words.split(text).filter(String::isNotEmpty).map(confusables::forms)
        return entries.filter { it.isNamedIn(tokens, forms) }.map { it.brand }
    }

    /** Brands whose alias appears inside a label of [host]. */
    fun inDomain(host: String): List<Brand> {
        val candidates = labelCandidates(host)
        return entries.filter { it.isIn(candidates) }.map { it.brand }
    }

    // Each label as normalized tokens plus the tokens joined, for the plain and the look-alike spelling.
    private fun labelCandidates(host: String): List<LabelCandidate> =
        host.split('.').filter(String::isNotEmpty).flatMap { label ->
            listOf(label, confusables.skeleton(label).lowercase()).distinct().map { spelling ->
                val tokens = normalizer.tokens(spelling)
                LabelCandidate(tokens, tokens.joinToString(""))
            }
        }

    private class LabelCandidate(
        val tokens: List<String>,
        val joined: String,
    )

    private inner class Entry(
        val brand: Brand,
    ) {
        private val aliasTokens = brand.aliases.map(normalizer::tokens).filter { it.isNotEmpty() }
        private val aliasForms =
            brand.aliases
                .map { alias -> words.split(alias).filter(String::isNotEmpty).map(confusables::forms) }
                .filter { it.isNotEmpty() }
        private val aliasKeys = aliasTokens.map { it.joinToString("") }.distinct()
        private val contextPhrases = brand.contextWords.map(normalizer::tokens).filter { it.isNotEmpty() }

        fun isNamedIn(
            tokens: List<String>,
            forms: List<ConfusableForms>,
        ): Boolean {
            val named =
                aliasTokens.any { alias -> tokens.windowed(alias.size).any { it == alias } } ||
                    aliasForms.any { alias -> forms.windowed(alias.size).any { looksLike(it, alias) } }
            val hasContext =
                contextPhrases.isEmpty() ||
                    contextPhrases.any { phrase -> tokens.windowed(phrase.size).any { it == phrase } }
            return named && hasContext
        }

        fun isIn(candidates: List<LabelCandidate>): Boolean =
            aliasKeys.any { key -> candidates.any { candidate -> isIn(key, candidate.tokens + candidate.joined) } }

        // A name inside a longer word is often a coincidence ("sky" in "flesky"), so how freely an alias
        // may sit inside a part of a host label depends on how distinctive it is.
        private fun isIn(
            key: String,
            parts: List<String>,
        ): Boolean =
            when {
                key in parts -> true

                // Two letters match almost any host: only as a whole part.
                key.length < MIN_SUBSTRING_ALIAS -> false

                // A brand named with an ordinary word, the kind that needs context words in text ("sky",
                // "smart", "globe"): only a longer name, and only at the start of a part.
                contextPhrases.isNotEmpty() -> key.length >= DISTINCTIVE_ALIAS && parts.any { it.startsWith(key) }

                // A short name: at the start or the end of a part ("bpivipe", "onlinebdo"), not in the middle.
                key.length < DISTINCTIVE_ALIAS -> parts.any { it.startsWith(key) || it.endsWith(key) }

                else -> parts.any { key in it }
            }

        private fun looksLike(
            window: List<ConfusableForms>,
            alias: List<ConfusableForms>,
        ): Boolean = window.indices.all { window[it].looksLike(alias[it]) }
    }

    private companion object {
        const val MIN_SUBSTRING_ALIAS = 3
        const val DISTINCTIVE_ALIAS = 5
        val words = Regex("[^\\p{L}\\p{N}]+")
    }
}
