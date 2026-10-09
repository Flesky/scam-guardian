package ph.scamguardian.core

import kotlin.random.Random

/** Deterministic [Embedder]: the same text always produces the same vector. */
class FakeEmbedder(
    private val dimensions: Int = 8,
) : Embedder {
    override fun embed(text: String): FloatArray {
        val random = Random(text.hashCode())
        return FloatArray(dimensions) { random.nextFloat() }
    }
}
