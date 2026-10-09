package ph.scamguardian.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class HistoryTest {
    private fun entry(
        id: String,
        type: WarningType = WarningType.OTP_REQUEST,
        brand: String? = null,
    ) = HistoryEntry(
        id = id,
        type = type,
        severity = Severity.RED,
        brand = brand,
        app = "Messenger",
        timeMs = 1_000L,
    )

    @Test
    fun add_putsTheNewestEntryFirst() {
        val entries = HistoryLog.add(HistoryLog.add(emptyList(), entry("1")), entry("2"))

        assertEquals(listOf("2", "1"), entries.map { it.id })
    }

    @Test
    fun add_keepsTheLast200Entries() {
        val entries = (1..205).fold(emptyList<HistoryEntry>()) { all, id -> HistoryLog.add(all, entry("$id")) }

        assertEquals(200, entries.size)
        assertEquals("205", entries.first().id)
        assertEquals("6", entries.last().id)
    }

    @Test
    fun markNotScam_marksOnlyThatEntry() {
        val entries = HistoryLog.markNotScam(listOf(entry("2"), entry("1")), "1")

        assertFalse(entries[0].markedNotScam)
        assertTrue(entries[1].markedNotScam)
        assertEquals(entries, HistoryLog.markNotScam(entries, "unknown"))
    }

    @Test
    fun encode_thenDecode_givesTheSameEntries() {
        val entries =
            listOf(entry("2", WarningType.FAKE_LINK, brand = "BDO").copy(markedNotScam = true), entry("1"))

        assertEquals(entries, HistoryLog.decode(HistoryLog.encode(entries)))
    }

    @Test
    fun encode_storesTheTypeAndBrandNotTheWarningText() {
        val text = HistoryLog.encode(listOf(entry("1", WarningType.FAKE_LINK, brand = "BDO")))

        assertEquals(
            """[{"id":"1","type":"fake_link","severity":"red","brand":"BDO","app":"Messenger","timeMs":1000}]""",
            text,
        )
    }

    @Test
    fun encode_thenDecode_keepsTheMessageThatCausedTheWarning() {
        val entries = listOf(entry("1").copy(text = "Paki-send naman po ng code"))

        assertEquals(entries, HistoryLog.decode(HistoryLog.encode(entries)))
    }

    @Test
    fun decode_entrySavedWithoutTheMessage_hasEmptyText() {
        val saved = """[{"id":"1","type":"fake_link","severity":"red","brand":"BDO","app":"Messenger","timeMs":1000}]"""

        assertEquals("", HistoryLog.decode(saved).single().text)
    }

    @Test
    fun decode_emptyOrDamagedText_givesNoEntries() {
        assertEquals(emptyList<HistoryEntry>(), HistoryLog.decode(""))
        assertEquals(emptyList<HistoryEntry>(), HistoryLog.decode("[{\"id\":"))
        assertEquals(emptyList<HistoryEntry>(), HistoryLog.decode("""{"id":"1"}"""))
        assertEquals(emptyList<HistoryEntry>(), HistoryLog.decode("""[{"id":"1","type":"new_type"}]"""))
    }

    @Test
    fun timeLabel_isShortEnglishWithAmOrPm() {
        val manila = ZoneId.of("Asia/Manila")
        val evening = ZonedDateTime.of(2026, 10, 9, 22, 24, 0, 0, manila).toInstant().toEpochMilli()
        val morning = ZonedDateTime.of(2026, 1, 15, 9, 5, 0, 0, manila).toInstant().toEpochMilli()

        assertEquals("Oct 9, 10:24 PM", entry("1").copy(timeMs = evening).timeLabel(manila))
        assertEquals("Jan 15, 9:05 AM", entry("1").copy(timeMs = morning).timeLabel(manila))
    }

    @Test
    fun safeTexts_add_keepsEachTextOnceAndTheLast200() {
        assertEquals(listOf("b", "a"), SafeTexts.add(listOf("a", "b"), "a"))

        val texts = (1..205).fold(emptyList<String>()) { all, number -> SafeTexts.add(all, "text $number") }

        assertEquals(200, texts.size)
        assertEquals("text 6", texts.first())
        assertEquals("text 205", texts.last())
    }

    @Test
    fun safeTexts_encodeThenDecode_givesTheSameTexts() {
        val texts = listOf("Pahiram naman \"bukas\"\nbabalik ko", "Kumusta 🙂")

        assertEquals(texts, SafeTexts.decode(SafeTexts.encode(texts)))
        assertEquals(emptyList<String>(), SafeTexts.decode(""))
        assertEquals(emptyList<String>(), SafeTexts.decode("not json"))
    }
}
