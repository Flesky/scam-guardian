package ph.scamguardian.core

import kotlin.math.sqrt

/** Keeps the leading [dimensions] values and rescales them to unit length (L2 norm of 1). */
fun FloatArray.truncatedAndNormalized(dimensions: Int): FloatArray {
    require(dimensions in 1..size) { "dimensions must be in 1..$size but was $dimensions" }
    val truncated = copyOf(dimensions)
    val norm = sqrt(truncated.sumOf { (it * it).toDouble() }).toFloat()
    if (norm == 0f) return truncated
    return FloatArray(dimensions) { truncated[it] / norm }
}

/** Cosine similarity of two vectors of the same size; 0 when either is all zeros. */
fun cosineSimilarity(
    first: FloatArray,
    second: FloatArray,
): Float {
    require(first.size == second.size) { "Vector sizes differ: ${first.size} and ${second.size}" }
    var dot = 0.0
    var firstNorm = 0.0
    var secondNorm = 0.0
    for (index in first.indices) {
        dot += first[index] * second[index]
        firstNorm += first[index] * first[index]
        secondNorm += second[index] * second[index]
    }
    val norms = sqrt(firstNorm) * sqrt(secondNorm)
    return if (norms == 0.0) 0f else (dot / norms).toFloat()
}
