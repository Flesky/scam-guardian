package ph.scamguardian.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormat = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)

/**
 * One warning the user was shown. The warning text is not stored: the UI takes it from the
 * [WarningCatalog] with [type] and [brand], so an old entry follows the language setting.
 */
@Serializable
data class HistoryEntry(
    val id: String,
    val type: WarningType,
    val severity: Severity,
    /** The brand a fake link imitates, or null for the other warning types. */
    val brand: String? = null,
    /** The name of the app where the warning was seen, for example "Messenger". */
    val app: String,
    val timeMs: Long,
    /** True after the user pressed "Not a scam" on the banner. */
    val markedNotScam: Boolean = false,
    /** The on-screen text that caused the warning. Empty in entries saved before it was stored. */
    val text: String = "",
) {
    /** The time in the form "Oct 9, 10:24 PM". It is always in English, like all text but the messages. */
    fun timeLabel(zone: ZoneId = ZoneId.systemDefault()): String =
        timeFormat.format(Instant.ofEpochMilli(timeMs).atZone(zone))
}

/** The list of past warnings, newest first, and its JSON form. */
object HistoryLog {
    const val MAX_ENTRIES = 200

    private val json = Json { ignoreUnknownKeys = true }

    /** [entries] with [entry] at the top. The oldest entries past [MAX_ENTRIES] are dropped. */
    fun add(
        entries: List<HistoryEntry>,
        entry: HistoryEntry,
    ): List<HistoryEntry> = (listOf(entry) + entries).take(MAX_ENTRIES)

    fun markNotScam(
        entries: List<HistoryEntry>,
        id: String,
    ): List<HistoryEntry> = entries.map { if (it.id == id) it.copy(markedNotScam = true) else it }

    /**
     * True when exactly this [text] already caused a warning in [entries]. Such a message does not warn
     * again, until the history is cleared.
     */
    fun hasText(
        entries: List<HistoryEntry>,
        text: String,
    ): Boolean = text.isNotBlank() && entries.any { it.text == text }

    fun encode(entries: List<HistoryEntry>): String = json.encodeToString(entries)

    /** The entries in [text]. Empty or damaged text gives an empty list. */
    fun decode(text: String): List<HistoryEntry> = decodeListOrEmpty(json, text)
}

/** The messages the user marked "Not a scam", oldest first, and their JSON form. */
object SafeTexts {
    const val MAX_TEXTS = 200

    private val json = Json

    /** [texts] with [text] at the end, without a second copy. The oldest texts past [MAX_TEXTS] are dropped. */
    fun add(
        texts: List<String>,
        text: String,
    ): List<String> = (texts - text + text).takeLast(MAX_TEXTS)

    fun encode(texts: List<String>): String = json.encodeToString(texts)

    /** The texts in [text]. Empty or damaged text gives an empty list. */
    fun decode(text: String): List<String> = decodeListOrEmpty(json, text)
}

private inline fun <reified T> decodeListOrEmpty(
    json: Json,
    text: String,
): List<T> =
    try {
        if (text.isBlank()) emptyList() else json.decodeFromString<List<T>>(text)
    } catch (_: SerializationException) {
        emptyList()
    }
