package ph.scamguardian.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ph.scamguardian.core.HistoryEntry
import ph.scamguardian.core.Severity
import ph.scamguardian.core.WarningType
import java.io.File

class StoresTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun entry(id: String) =
        HistoryEntry(
            id = id,
            type = WarningType.FAKE_LINK,
            severity = Severity.RED,
            brand = "BDO",
            app = "Messenger",
            timeMs = 1_000L,
        )

    @Test
    fun history_withoutAFile_isEmpty() {
        assertEquals(emptyList<HistoryEntry>(), HistoryStore(File(folder.root, "history.json")).entries())
    }

    @Test
    fun history_isKeptInTheFile_newestFirst() {
        val file = File(folder.root, "history.json")
        val store = HistoryStore(file)

        store.add(entry("1"))
        store.add(entry("2"))
        store.markNotScam("1")

        assertEquals(listOf(entry("2"), entry("1").copy(markedNotScam = true)), HistoryStore(file).entries())
        assertEquals(listOf("history.json"), folder.root.list()?.toList())
    }

    @Test
    fun history_clear_removesEveryEntry() {
        val file = File(folder.root, "history.json")
        val store = HistoryStore(file)
        store.add(entry("1"))

        store.clear()

        assertTrue(HistoryStore(file).entries().isEmpty())
    }

    @Test
    fun history_damagedFile_startsAgainEmpty() {
        val file = File(folder.root, "history.json").apply { writeText("[{") }
        val store = HistoryStore(file)

        store.add(entry("1"))

        assertEquals(listOf(entry("1")), store.entries())
    }

    @Test
    fun safeTexts_areKeptInTheFile_oldestFirst() {
        val file = File(folder.root, "nested/safe_anchors.json")
        val store = SafeTextStore(file)
        assertEquals(emptyList<String>(), store.texts())

        store.add("Pahiram naman\nkailangan ko agad")
        store.add("Kumusta")
        store.add("Kumusta")

        assertEquals(listOf("Pahiram naman\nkailangan ko agad", "Kumusta"), SafeTextStore(file).texts())
    }
}
