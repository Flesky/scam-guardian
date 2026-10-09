package ph.scamguardian.storage

import java.io.File
import java.io.IOException

/** A small text file that is replaced as a whole, so a failed write cannot leave half a file. */
internal class TextFile(
    private val file: File,
) {
    /** The text of the file, or an empty string when there is no file yet. */
    fun read(): String = if (file.isFile) file.readText() else ""

    fun write(text: String) {
        file.parentFile?.mkdirs()
        val temporary = File(file.path + ".tmp")
        temporary.writeText(text)
        if (!temporary.renameTo(file)) throw IOException("Could not replace ${file.name}")
    }
}
