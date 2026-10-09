package ph.scamguardian.core

import java.security.MessageDigest

data class ScamWarning(
    val type: WarningType,
    val title: String,
    val message: String,
    val evidence: String,
)

/**
 * Checks one message: the rules first, then the AI check for messages that pass the prefilter.
 * Returns null when nothing is wrong.
 */
class ScamPipeline(
    data: PipelineData,
    embedder: Embedder,
    aiThreshold: Float = AiCheck.DEFAULT_THRESHOLD,
    private val prefilter: Prefilter = Prefilter(),
) {
    private val warnings =
        WarningType.entries.associateWith { type ->
            requireNotNull(data.warnings[type.key]) { "No warning text for ${type.key}" }
        }
    private val confusables = Confusables()
    private val normalizer = Normalizer(data.shortcuts, confusables)
    private val matcher = KeywordMatcher(normalizer)
    private val linkAnalyzer = LinkAnalyzer(data.urlRules, data.brands)
    private val analyzer = MessageAnalyzer(data, normalizer, matcher, linkAnalyzer, confusables)
    private val rules = Rules(normalizer, matcher)
    private val aiCheck = AiCheck(embedder, linkAnalyzer, data.anchors, aiThreshold)

    // Results by hash of the cleaned text and its link hosts, so a repeated message is not checked again.
    private val cache =
        object : LinkedHashMap<String, ScamWarning?>(CACHE_SIZE, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ScamWarning?>): Boolean =
                size > CACHE_SIZE
        }

    @Synchronized
    fun check(text: String): ScamWarning? {
        val prepared = Sanitizer.prepare(text)
        // The hosts are part of the key: two messages can clean to the same words yet link elsewhere.
        val hosts = linkAnalyzer.analyze(prepared).joinToString(" ") { it.host }
        val key = hash("${Sanitizer.clean(prepared)}\n$hosts")
        if (key in cache) return cache[key]
        return evaluate(prepared).also { cache[key] = it }
    }

    private fun evaluate(prepared: String): ScamWarning? {
        val analysis = analyzer.analyze(prepared)
        val match = rules.evaluate(analysis) ?: if (prefilter.passes(analysis)) aiMatch(prepared) else null
        return match?.let(::toWarning)
    }

    private fun aiMatch(prepared: String): RuleMatch? =
        aiCheck.score(prepared)?.takeIf(aiCheck::isScam)?.let { score ->
            RuleMatch(WarningType.AI_SCAM, "Similar to known scam messages (score %.2f)".format(score.scam))
        }

    private fun toWarning(match: RuleMatch): ScamWarning {
        val text = warnings.getValue(match.type)
        return ScamWarning(
            type = match.type,
            title = text.title,
            message = text.message.replace(BRAND_PLACEHOLDER, match.brand.orEmpty()),
            evidence = match.evidence,
        )
    }

    private fun hash(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val CACHE_SIZE = 256
        const val LOAD_FACTOR = 0.75f
        const val BRAND_PLACEHOLDER = "{brand}"
    }
}
