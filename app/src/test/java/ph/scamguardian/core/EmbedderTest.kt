package ph.scamguardian.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EmbedderTest {
    private val embedder: Embedder = FakeEmbedder(dimensions = 4)

    @Test
    fun embed_returnsVectorWithConfiguredDimensions() {
        assertEquals(4, embedder.embed("You won a prize").size)
    }

    @Test
    fun embed_sameText_returnsSameVector() {
        assertArrayEquals(embedder.embed("You won a prize"), embedder.embed("You won a prize"), 0f)
    }

    @Test
    fun embed_differentText_returnsDifferentVector() {
        assertFalse(embedder.embed("You won a prize").contentEquals(embedder.embed("See you at lunch")))
    }
}
