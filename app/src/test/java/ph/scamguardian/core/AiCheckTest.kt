package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCheckTest {
    private val anchors: Anchors = Fixtures.json.decodeFromString(Fixtures.text("anchors_test.json"))
    private val embedder = FakeEmbedder()

    private fun aiCheck(
        anchors: Anchors = this.anchors,
        threshold: Float = AiCheck.DEFAULT_THRESHOLD,
    ) = AiCheck(embedder, Fixtures.linkAnalyzer(), anchors, threshold)

    @Test
    fun init_embedsEveryAnchorOnce() {
        aiCheck()

        assertEquals(anchors.scam.size + anchors.safe.size, embedder.calls)
    }

    @Test
    fun score_sameTextAsAScamAnchor_isScam() {
        val check = aiCheck()

        val score = checkNotNull(check.score(anchors.scam.first()))

        assertEquals(1f, score.scam, TOLERANCE)
        assertTrue(check.isScam(score))
    }

    @Test
    fun score_sameTextAsASafeAnchor_isNotScam() {
        val check = aiCheck(threshold = -1f)

        val score = checkNotNull(check.score(anchors.safe.first()))

        assertEquals(1f, score.safe, TOLERANCE)
        assertFalse(check.isScam(score))
    }

    @Test
    fun score_unrelatedText_isBelowTheDefaultThreshold() {
        val check = aiCheck()

        assertFalse(check.isScam(checkNotNull(check.score("Kumusta, anong oras ka uuwi mamaya?"))))
    }

    @Test
    fun isScam_usesTheConfiguredThreshold() {
        val score = AiScore(scam = 0.5f, safe = 0.1f)

        assertFalse(aiCheck().isScam(score))
        assertTrue(aiCheck(threshold = 0.4f).isScam(score))
        assertFalse(aiCheck(threshold = 0.4f).isScam(AiScore(scam = 0.5f, safe = 0.7f)))
    }

    @Test
    fun score_withoutScamAnchors_isSkipped() {
        val check = aiCheck(Anchors(scam = emptyList(), safe = anchors.safe))

        assertFalse(check.enabled)
        assertNull(check.score("Nanalo ka ng 50,000 pesos"))
        assertEquals(0, embedder.calls)
    }

    @Test
    fun embeddingText_replacesLinksWithAPlaceholder() {
        assertEquals("Click now: [link]", aiCheck().embeddingText("Click  now:https://bdo-bd0.cc/ph 🙂"))
    }

    @Test
    fun chunks_shortText_isOnePiece() {
        assertEquals(listOf("padala na"), aiCheck().chunks("padala  na"))
    }

    @Test
    fun chunks_longText_overlapAndAlwaysIncludeTheEnd() {
        val check = aiCheck()
        val text = (1..700).joinToString(" ") { "word$it" }
        val full = check.embeddingText(text)

        val chunks = check.chunks(text)

        assertEquals(AiCheck.MAX_CHUNKS, chunks.size)
        assertTrue(chunks.all { it.length == AiCheck.CHUNK_LENGTH })
        assertEquals(full.take(AiCheck.CHUNK_LENGTH), chunks.first())
        assertEquals(full.takeLast(AiCheck.CHUNK_LENGTH), chunks.last())
        assertEquals(chunks[0].takeLast(200), chunks[1].take(200))
    }

    @Test
    fun score_scamAfterLongPadding_isStillFound() {
        val padding = "Kumusta ka na, matagal na tayong hindi nagkita. ".repeat(30)
        val check = AiCheck(MarkerEmbedder("remote access"), Fixtures.linkAnalyzer(), markerAnchors)

        assertTrue(padding.length > AiCheck.CHUNK_LENGTH)
        assertTrue(check.isScam(checkNotNull(check.score(padding + "Install the remote access app now."))))
        assertFalse(check.isScam(checkNotNull(check.score(padding + "Ingat ka palagi."))))
    }

    /** Points one way for text that contains [marker] and the other way for everything else. */
    private class MarkerEmbedder(
        private val marker: String,
    ) : Embedder {
        override fun embed(text: String): FloatArray =
            if (marker in
                text
            ) {
                floatArrayOf(1f, 0f)
            } else {
                floatArrayOf(0f, 1f)
            }
    }

    private companion object {
        const val TOLERANCE = 1e-5f
        val markerAnchors = Anchors(scam = listOf("Install this remote access app"), safe = listOf("Kumusta ka"))
    }
}
