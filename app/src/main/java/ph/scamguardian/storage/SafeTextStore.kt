package ph.scamguardian.storage

import ph.scamguardian.core.SafeTexts
import java.io.File

/**
 * The messages the user marked "Not a scam", kept as JSON in [file]. Every call reads or writes the
 * file, so call it off the main thread. Calls can throw [java.io.IOException].
 */
class SafeTextStore(
    file: File,
) {
    private val file = TextFile(file)

    /** Oldest first. */
    @Synchronized
    fun texts(): List<String> = SafeTexts.decode(file.read())

    @Synchronized
    fun add(text: String) = file.write(SafeTexts.encode(SafeTexts.add(texts(), text)))
}
