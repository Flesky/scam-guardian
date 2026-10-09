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
            aliasKeys.any { key ->
                candidates.any { candidate ->
                    // Very short aliases must be a whole part of the label, or they match almost any host.
                    if (key.length < MIN_SUBSTRING_ALIAS) key in candidate.tokens else key in candidate.joined
                }
            }

        private fun looksLike(
            window: List<ConfusableForms>,
            alias: List<ConfusableForms>,
        ): Boolean = window.indices.all { window[it].looksLike(alias[it]) }
    }

    private companion object {
        const val MIN_SUBSTRING_ALIAS = 3
        val words = Regex("[^\\p{L}\\p{N}]+")
    }
}
