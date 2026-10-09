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
