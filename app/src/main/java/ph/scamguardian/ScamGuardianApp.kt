package ph.scamguardian

import android.app.Application
import ph.scamguardian.storage.HistoryStore
import java.io.File

class ScamGuardianApp : Application() {
    /** The app-wide scam engine. It loads the model and data on first use. */
    val engine: ScamEngine by lazy { ScamEngine(this) }

    /** The past warnings, in a JSON file in app storage. It is in the no-backup folder, so it stays on this phone. */
    val history: HistoryStore by lazy { HistoryStore(File(noBackupFilesDir, HISTORY_FILE)) }

    override fun onCreate() {
        super.onCreate()
        // Debug builds load at app start so the timings appear in the log.
        if (BuildConfig.DEBUG) engine.start()
    }

    private companion object {
        const val HISTORY_FILE = "history.json"
    }
}
