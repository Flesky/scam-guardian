package ph.scamguardian.core

import java.security.MessageDigest

/**
 * A warning without its text: the UI takes the title and the message in the selected language from the
 * [WarningCatalog], using [type] and [brand].
 */
data class ScamWarning(
    val type: WarningType,
    val severity: Severity,
    /** The brand a fake link imitates, or null for the other warning types. */
    val brand: String?,
    val evidence: String,
)

/** What the pipeline did with one message, step by step. */
data class PipelineReport(
    val warning: ScamWarning?,
    /** The rule that matched, or null. */
    val rule: RuleMatch?,
    /** Whether the message qualifies for the AI check. The AI check is skipped anyway when a rule matched. */
    val gate: GateResult,
    /** The AI scores, or null when the AI check did not run. */
    val aiScore: AiScore?,
    val aiThreshold: Float,
    /** How many times the model was called for this message. */
    val modelCalls: Int,
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
    private val warnings = data.warnings
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

    // Texts the user marked "Not a scam". Unlike the cache, these are never dropped.
    private val markedSafe = HashSet<String>()

    @Synchronized
    fun check(text: String): ScamWarning? {
        val key = keyOf(text)
        return when (key) {
            in markedSafe -> null
            in cache -> cache[key]
            else -> inspect(text).warning.also { cache[key] = it }
        }
    }

    /**
     * Checks [text] only if it is new, and reports each step like [inspect]. Returns null when the text
     * is already in the cache or was marked "Not a scam", so a text is reported at most once.
     */
    @Synchronized
    fun inspectNew(text: String): PipelineReport? {
        val key = keyOf(text)
        if (key in markedSafe || key in cache) return null
        return inspect(text).also { cache[key] = it.warning }
    }

    /** Removes [text] from the cache, so [inspectNew] reports it again the next time it is seen. */
    @Synchronized
    fun forget(text: String) {
        cache.remove(keyOf(text))
    }

    /** Empties the cache, so every text is new to [inspectNew] again. Texts marked safe stay safe. */
    @Synchronized
    fun forgetAll() {
        cache.clear()
    }

    /**
     * The user said [text] is not a scam: it never gives a warning again, and it becomes a safe anchor
     * so similar messages score as safe in the AI check. This calls the model once.
     */
    @Synchronized
    fun markSafe(text: String) {
        markedSafe += keyOf(text)
        aiCheck.addSafeAnchor(text)
    }

    /** Checks [text] without the cache and reports each step: the rule, the AI gate and the AI scores. */
    @Synchronized
    fun inspect(text: String): PipelineReport {
        val prepared = Sanitizer.prepare(text)
        val analysis = analyzer.analyze(prepared)
        val rule = rules.evaluate(analysis)
        val gate = prefilter.explain(analysis)
        val runsAi = rule == null && gate.passes && aiCheck.enabled
        val score = if (runsAi) aiCheck.score(prepared) else null
        val match = rule ?: score?.takeIf(aiCheck::isScam)?.let(::aiMatch)
        return PipelineReport(
            warning = match?.let(::toWarning),
            rule = rule,
            gate = gate,
            aiScore = score,
            aiThreshold = aiCheck.threshold,
            modelCalls = if (runsAi) aiCheck.chunks(prepared).size else 0,
        )
    }

    private fun aiMatch(score: AiScore): RuleMatch =
        RuleMatch(WarningType.AI_SCAM, "Similar to known scam messages (score %.2f)".format(score.scam))

    private fun toWarning(match: RuleMatch): ScamWarning =
        ScamWarning(
            type = match.type,
            severity = warnings.severity(match.type),
            brand = match.brand,
            evidence = match.evidence,
        )

    private fun keyOf(text: String): String {
        val prepared = Sanitizer.prepare(text)
        // The hosts are part of the key: two messages can clean to the same words yet link elsewhere.
        val hosts = linkAnalyzer.analyze(prepared).joinToString(" ") { it.host }
        return hash("${Sanitizer.clean(prepared)}\n$hosts")
    }

    private fun hash(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val CACHE_SIZE = 256
        const val LOAD_FACTOR = 0.75f
    }
}
