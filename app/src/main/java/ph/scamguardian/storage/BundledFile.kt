package ph.scamguardian.storage

import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * A file that is packaged with the app and needed as a real file in app storage. The model is one:
 * LiteRT-LM opens it by path, and an asset inside the APK has none.
 */
class BundledFile(
    private val target: File,
    private val size: () -> Long,
    private val open: () -> InputStream,
) {
    /**
     * Returns the file in app storage. It is copied there when it is missing or has another size, as
     * after an app update with a new model. A failed copy leaves no half file behind.
     */
    fun install(): File {
        if (target.isFile && target.length() == size()) return target
        target.parentFile?.mkdirs()
        val temporary = File(target.path + ".tmp")
        try {
            open().use { input -> temporary.outputStream().use { input.copyTo(it) } }
            if (!temporary.renameTo(target)) throw IOException("Could not replace ${target.name}")
        } finally {
            temporary.delete()
        }
        return target
    }
}
