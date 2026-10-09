package ph.scamguardian.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VectorsTest {
    @Test
    fun truncatedAndNormalized_keepsLeadingDimensionsAtUnitLength() {
        val vector = floatArrayOf(3f, 4f, 12f, 99f)

        assertArrayEquals(floatArrayOf(0.6f, 0.8f), vector.truncatedAndNormalized(2), TOLERANCE)
    }

    @Test
    fun truncatedAndNormalized_fullSize_onlyNormalizes() {
        assertArrayEquals(floatArrayOf(0f, 1f), floatArrayOf(0f, 5f).truncatedAndNormalized(2), TOLERANCE)
    }

    @Test
    fun truncatedAndNormalized_zeroVector_staysZero() {
        assertArrayEquals(floatArrayOf(0f, 0f), floatArrayOf(0f, 0f, 1f).truncatedAndNormalized(2), TOLERANCE)
    }

    @Test
    fun truncatedAndNormalized_rejectsDimensionsOutsideTheVector() {
        assertThrows(IllegalArgumentException::class.java) { floatArrayOf(1f).truncatedAndNormalized(2) }
        assertThrows(IllegalArgumentException::class.java) { floatArrayOf(1f).truncatedAndNormalized(0) }
    }

    @Test
    fun cosineSimilarity_comparesDirectionNotLength() {
        assertEquals(1f, cosineSimilarity(floatArrayOf(1f, 2f), floatArrayOf(2f, 4f)), TOLERANCE)
        assertEquals(0f, cosineSimilarity(floatArrayOf(1f, 0f), floatArrayOf(0f, 3f)), TOLERANCE)
        assertEquals(-1f, cosineSimilarity(floatArrayOf(1f, 1f), floatArrayOf(-1f, -1f)), TOLERANCE)
        assertEquals(0f, cosineSimilarity(floatArrayOf(0f, 0f), floatArrayOf(1f, 1f)), TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1e-6f
    }
}
