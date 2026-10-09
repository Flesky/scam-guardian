package ph.scamguardian.core

import kotlin.random.Random

/**
 * Deterministic [Embedder]: the same text always produces the same vector, and different texts
 * produce unrelated vectors (cosine similarity near zero).
 */
class FakeEmbedder(
    private val dimensions: Int = 64,
) : Embedder {
    var calls = 0
        private set

    override fun embed(text: String): FloatArray {
        calls++
        val random = Random(text.hashCode())
        return FloatArray(dimensions) { random.nextFloat() - 0.5f }
    }
}
