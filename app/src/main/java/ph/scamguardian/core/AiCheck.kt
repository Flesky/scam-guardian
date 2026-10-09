package ph.scamguardian.core

data class AiScore(
    val scam: Float,
    val safe: Float,
)

/** Compares a message with known scam and safe messages by embedding similarity. */
class AiCheck(
    private val embedder: Embedder,
    private val linkAnalyzer: LinkAnalyzer,
    anchors: Anchors,
    private val threshold: Float = DEFAULT_THRESHOLD,
) {
    // Anchors are embedded once, here.
    private val scamAnchors = anchors.scam.map(::embed)
    private val safeAnchors = if (scamAnchors.isEmpty()) emptyList() else anchors.safe.map(::embed)

    /** False when there are no scam anchors; the AI check is then skipped. */
    val enabled: Boolean get() = scamAnchors.isNotEmpty()

    /** The text that is embedded: sanitized, links replaced with "[link]", at most about 1000 characters. */
    fun embeddingText(text: String): String =
        linkAnalyzer.replaceLinks(Sanitizer.sanitize(text), LINK_PLACEHOLDER).take(MAX_LENGTH)

    /** Best similarity to a scam anchor and to a safe anchor, or null when the check is skipped. */
    fun score(text: String): AiScore? {
        if (!enabled) return null
        val vector = embed(text)
        return AiScore(
            scam = scamAnchors.maxOf { cosineSimilarity(vector, it) },
            safe = safeAnchors.maxOfOrNull { cosineSimilarity(vector, it) } ?: NO_MATCH,
        )
    }

    fun isScam(score: AiScore): Boolean = score.scam >= threshold && score.scam > score.safe

    private fun embed(text: String): FloatArray = embedder.embed(embeddingText(text))

    companion object {
        const val DEFAULT_THRESHOLD = 0.6f
        private const val MAX_LENGTH = 1000
        private const val LINK_PLACEHOLDER = "[link]"
        private const val NO_MATCH = -1f
    }
}
