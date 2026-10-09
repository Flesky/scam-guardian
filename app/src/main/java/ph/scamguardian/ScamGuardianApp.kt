package ph.scamguardian

import android.app.Application

class ScamGuardianApp : Application() {
    /** The app-wide scam engine. It loads the model and data on first use. */
    val engine: ScamEngine by lazy { ScamEngine(this) }

    override fun onCreate() {
        super.onCreate()
        // Debug builds load at app start so the timings appear in the log.
        if (BuildConfig.DEBUG) engine.start()
    }
}
