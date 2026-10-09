package ph.scamguardian.core

/** Turns text into an embedding vector. */
interface Embedder {
    fun embed(text: String): FloatArray
}
