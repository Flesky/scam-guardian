package ph.scamguardian

import android.app.Application
import android.os.SystemClock
import android.util.Log
import ph.scamguardian.core.Embedder
import ph.scamguardian.embedding.LiteRtEmbedder
import java.io.File
import kotlin.concurrent.thread

class ScamGuardianApp : Application() {
    val embedder: Embedder by lazy { LiteRtEmbedder(File(getExternalFilesDir(null), MODEL_PATH).path) }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) logDebugEmbedding()
    }

    // Waits off the main thread; the model itself runs on the embedder's own thread.
    private fun logDebugEmbedding() {
        thread(name = "debug-embedding") {
            try {
                val start = SystemClock.elapsedRealtime()
                val vector = embedder.embed(DEBUG_TEXT)
                val elapsedMs = SystemClock.elapsedRealtime() - start
                Log.d(TAG, "Embedded \"$DEBUG_TEXT\": size=${vector.size}, time=$elapsedMs ms")
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Debug embedding failed", e)
            }
        }
    }

    private companion object {
        const val TAG = "SG"
        const val MODEL_PATH = "models/embeddinggemma-2-740m.litertlm"
        const val DEBUG_TEXT = "Hello world"
    }
}
