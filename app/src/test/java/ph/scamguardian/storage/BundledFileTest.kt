package ph.scamguardian.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.io.InputStream

class BundledFileTest {
    @get:Rule
    val folder = TemporaryFolder()

    private var copies = 0

    private fun target() = File(folder.root, "models/model.bin")

    private fun bundled(content: String) =
        BundledFile(target(), size = { content.length.toLong() }) {
            copies++
            content.byteInputStream()
        }

    @Test
    fun install_withoutAFile_copiesIt() {
        val file = bundled("model").install()

        assertEquals("model", file.readText())
    }

    @Test
    fun install_whenTheFileIsThere_doesNotCopyAgain() {
        bundled("model").install()
        bundled("model").install()

        assertEquals(1, copies)
    }

    @Test
    fun install_whenTheSizeChanged_replacesTheFile() {
        bundled("model").install()

        val file = bundled("a newer model").install()

        assertEquals("a newer model", file.readText())
    }

    @Test
    fun install_whenTheCopyFails_leavesNoFile() {
        val failing =
            BundledFile(target(), size = { 5L }) {
                object : InputStream() {
                    override fun read(): Int = throw IOException("no space")
                }
            }

        assertThrows(IOException::class.java) { failing.install() }

        assertFalse(target().exists())
        assertEquals(emptyList<String>(), target().parentFile?.list()?.toList())
    }
}
