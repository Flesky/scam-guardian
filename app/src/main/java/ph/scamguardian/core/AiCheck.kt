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
    val threshold: Float = DEFAULT_THRESHOLD,
) {
    // Anchors are embedded once, here.
    private val scamAnchors = anchors.scam.map(::embedAnchor)
    private val safeAnchors =
        (if (scamAnchors.isEmpty()) emptyList() else anchors.safe.map(::embedAnchor)).toMutableList()

    /** False when there are no scam anchors; the AI check is then skipped. */
    val enabled: Boolean get() = scamAnchors.isNotEmpty()

    /**
     * Adds [text] to the safe anchors, for a message the user marked "Not a scam". This calls the model
     * once. It does nothing when the AI check is skipped.
     */
    fun addSafeAnchor(text: String) {
        if (enabled) safeAnchors += embedAnchor(text)
    }

    /** The text that is embedded: cleaned, with every link replaced by "[link]". */
    fun embeddingText(text: String): String =
        Sanitizer.clean(linkAnalyzer.replaceLinks(Sanitizer.prepare(text), LINK_PLACEHOLDER))

    /**
     * The pieces of [text] that are embedded, each at most about 1000 characters. A long message is cut
     * into overlapping pieces so that padding cannot push a scam out of view. The last piece is always
     * the end of the message; past [MAX_CHUNKS] pieces, text between the early pieces and the end is skipped.
     */
    fun chunks(text: String): List<String> {
        val full = embeddingText(text)
        if (full.length <= CHUNK_LENGTH) return listOf(full)
        val starts = (0..full.length - CHUNK_LENGTH step CHUNK_STEP).take(MAX_CHUNKS - 1) + (full.length - CHUNK_LENGTH)
        return starts.distinct().map { full.substring(it, it + CHUNK_LENGTH) }
    }

    /**
     * Similarity to the closest scam anchor and the closest safe anchor, or null when the check is
     * skipped. For a long message this is the score of its most scam-like piece.
     */
    fun score(text: String): AiScore? {
        if (!enabled) return null
        val scores = chunks(text).map(::scoreChunk)
        return scores.filter(::isScam).maxByOrNull { it.scam } ?: scores.maxBy { it.scam }
    }

    fun isScam(score: AiScore): Boolean = score.scam >= threshold && score.scam > score.safe

    private fun scoreChunk(chunk: String): AiScore {
        val vector = embedder.embed(chunk)
        return AiScore(
            scam = scamAnchors.maxOf { cosineSimilarity(vector, it) },
            safe = safeAnchors.maxOfOrNull { cosineSimilarity(vector, it) } ?: NO_MATCH,
        )
    }

    private fun embedAnchor(text: String): FloatArray = embedder.embed(embeddingText(text).take(CHUNK_LENGTH))

    companion object {
        const val DEFAULT_THRESHOLD = 0.6f

        /** Messages with fewer words than this are not given to the AI check. */
        const val MIN_WORDS = 6
        const val CHUNK_LENGTH = 1000
        const val MAX_CHUNKS = 4
        private const val CHUNK_STEP = 800
        private const val LINK_PLACEHOLDER = "[link]"
        private const val NO_MATCH = -1f
    }
}
