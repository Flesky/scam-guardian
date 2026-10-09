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
    fun embeddingText_replacesLinksAndLimitsLength() {
        val check = aiCheck()

        assertEquals("Click now: [link]", check.embeddingText("Click  now:https://bdo-bd0.cc/ph 🙂"))
        assertEquals(1000, check.embeddingText("padala ".repeat(500)).length)
    }

    private companion object {
        const val TOLERANCE = 1e-5f
    }
}
