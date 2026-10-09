package ph.scamguardian.storage

import ph.scamguardian.core.HistoryEntry
import ph.scamguardian.core.HistoryLog
import java.io.File

/**
 * The past warnings, kept as JSON in [file] and in memory after the first read. A call may read or
 * write the file, so call it off the main thread. Calls can throw [java.io.IOException].
 */
class HistoryStore(
    file: File,
) {
    private val file = TextFile(file)
    private var cached: List<HistoryEntry>? = null

    /** How many times the history was cleared since the app started. */
    @Volatile
    var clears = 0
        private set

    /** Newest first. */
    @Synchronized
    fun entries(): List<HistoryEntry> = cached ?: HistoryLog.decode(file.read()).also { cached = it }

    /** True when exactly this [text] already has an entry, so it should not warn again. */
    @Synchronized
    fun hasText(text: String): Boolean = HistoryLog.hasText(entries(), text)

    @Synchronized
    fun add(entry: HistoryEntry) = save(HistoryLog.add(entries(), entry))

    @Synchronized
    fun markNotScam(id: String) = save(HistoryLog.markNotScam(entries(), id))

    /** Removes every entry. Messages that warned before may then warn again. */
    @Synchronized
    fun clear() {
        save(emptyList())
        clears++
    }

    private fun save(entries: List<HistoryEntry>) {
        file.write(HistoryLog.encode(entries))
        cached = entries
    }
}
