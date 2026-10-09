package ph.scamguardian.storage

import ph.scamguardian.core.HistoryEntry
import ph.scamguardian.core.HistoryLog
import java.io.File

/**
 * The past warnings, kept as JSON in [file]. Every call reads or writes the file, so call it off the
 * main thread. Calls can throw [java.io.IOException].
 */
class HistoryStore(
    file: File,
) {
    private val file = TextFile(file)

    /** Newest first. */
    @Synchronized
    fun entries(): List<HistoryEntry> = HistoryLog.decode(file.read())

    @Synchronized
    fun add(entry: HistoryEntry) = file.write(HistoryLog.encode(HistoryLog.add(entries(), entry)))

    @Synchronized
    fun markNotScam(id: String) = file.write(HistoryLog.encode(HistoryLog.markNotScam(entries(), id)))

    @Synchronized
    fun clear() = file.write(HistoryLog.encode(emptyList()))
}
