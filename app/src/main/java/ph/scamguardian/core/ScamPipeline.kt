package ph.scamguardian.core

import java.security.MessageDigest

data class ScamWarning(
    val type: WarningType,
    val title: String,
    val message: String,
    val evidence: String,
)

/** Checks one message: sanitize, prefilter, rules, then the AI check. Returns null when nothing is wrong. */
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
    private val normalizer = Normalizer(data.shortcuts)
    private val matcher = KeywordMatcher(normalizer)
    private val linkAnalyzer = LinkAnalyzer(data.urlRules)
    private val analyzer = MessageAnalyzer(data, normalizer, matcher, linkAnalyzer)
    private val rules = Rules(matcher)
    private val aiCheck = AiCheck(embedder, linkAnalyzer, data.anchors, aiThreshold)

    // Results by hash of the sanitized text, so a repeated message is not checked again.
    private val cache =
        object : LinkedHashMap<String, ScamWarning?>(CACHE_SIZE, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ScamWarning?>): Boolean =
                size > CACHE_SIZE
        }

    @Synchronized
    fun check(text: String): ScamWarning? {
        val sanitized = Sanitizer.sanitize(text)
        val key = hash(sanitized)
        if (key in cache) return cache[key]
        return evaluate(sanitized).also { cache[key] = it }
    }

    private fun evaluate(sanitized: String): ScamWarning? {
        val analysis = analyzer.analyze(sanitized)
        if (!prefilter.passes(analysis)) return null
        val match = rules.evaluate(analysis) ?: aiMatch(sanitized)
        return match?.let(::toWarning)
    }

    private fun aiMatch(sanitized: String): RuleMatch? =
        aiCheck.score(sanitized)?.takeIf(aiCheck::isScam)?.let { score ->
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
