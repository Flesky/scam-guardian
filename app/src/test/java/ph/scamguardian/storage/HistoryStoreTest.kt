package ph.scamguardian.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ph.scamguardian.core.HistoryEntry
import ph.scamguardian.core.Severity
import ph.scamguardian.core.WarningType
import java.io.File

class HistoryStoreTest {
    @get:Rule val folder = TemporaryFolder()

    private fun entry(
        id: String,
        text: String,
    ) = HistoryEntry(id, WarningType.OTP_REQUEST, Severity.RED, app = "Messenger", timeMs = 1_000L, text = text)

    @Test
    fun hasText_rememberedAcrossRestartsUntilTheHistoryIsCleared() {
        val file = File(folder.root, "history.json")
        HistoryStore(file).add(entry("1", "Paki-send naman po ng code"))

        // A new store reads the same file, as the app does after a restart.
        val store = HistoryStore(file)
        assertTrue(store.hasText("Paki-send naman po ng code"))
        assertFalse(store.hasText("Ibang message ito"))
        assertEquals(0, store.clears)

        store.clear()

        assertFalse(store.hasText("Paki-send naman po ng code"))
        assertEquals(1, store.clears)
        assertEquals(emptyList<HistoryEntry>(), HistoryStore(file).entries())
    }

    @Test
    fun add_andMarkNotScam_areSavedToTheFile() {
        val file = File(folder.root, "history.json")
        val store = HistoryStore(file)
        store.add(entry("1", "first message"))
        store.add(entry("2", "second message"))
        store.markNotScam("1")

        val saved = HistoryStore(file).entries()

        assertEquals(listOf("2", "1"), saved.map { it.id })
        assertEquals(listOf(false, true), saved.map { it.markedNotScam })
    }
}
