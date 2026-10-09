package ph.scamguardian

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import ph.scamguardian.core.Embedder
import ph.scamguardian.core.PipelineJson
import ph.scamguardian.core.ScamPipeline
import ph.scamguardian.embedding.LiteRtEmbedder
import java.io.File
import java.io.IOException

/**
 * The one app-wide owner of the embedder, the data files from assets and the scam pipeline.
 *
 * Nothing is loaded until [start] or [pipeline] is first called. Loading happens off the main thread:
 * the model first, then the pipeline, whose anchors are embedded once as it is created.
 */
class ScamEngine(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val embedder = LiteRtEmbedder(File(appContext.getExternalFilesDir(null), MODEL_PATH).path)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val loading: Deferred<ScamPipeline> = scope.async(start = CoroutineStart.LAZY) { load() }

    /** Starts loading in the background, if it has not started yet. */
    fun start() {
        loading.start()
    }

    /** The loaded pipeline. Suspends until loading is done; throws if the model or data could not be loaded. */
    suspend fun pipeline(): ScamPipeline = loading.await()

    private fun load(): ScamPipeline =
        try {
            val loadMs = timeMs { embedder.load() }
            val timed = TimedEmbedder(embedder)
            val pipeline = ScamPipeline(readData(), timed)
            if (BuildConfig.DEBUG) logTimings(loadMs, anchorsMs = timed.totalMs, anchors = timed.calls)
            pipeline
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Could not load the scam engine", e)
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Could not read the data files", e)
            throw e
        }

    private fun readData() =
        PipelineJson(
            brands = asset("brands.json"),
            keywords = asset("keywords.json"),
            shortcuts = asset("shortcuts.json"),
            urlRules = asset("url_rules.json"),
            warnings = asset("warnings.json"),
            anchors = asset("anchors.json"),
        ).parse()

    private fun asset(name: String): String =
        appContext.assets
            .open(name)
            .bufferedReader()
            .use { it.readText() }

    private fun logTimings(
        loadMs: Long,
        anchorsMs: Long,
        anchors: Int,
    ) {
        var size = 0
        val embedMs = timeMs { size = embedder.embed(DEBUG_TEXT).size }
        Log.d(
            TAG,
            "vector size=$size, model load=$loadMs ms, one embedding=$embedMs ms, $anchors anchors=$anchorsMs ms",
        )
    }

    private inline fun timeMs(block: () -> Unit): Long {
        val start = SystemClock.elapsedRealtime()
        block()
        return SystemClock.elapsedRealtime() - start
    }

    /** Adds up the time spent embedding, to report how long the anchors took. */
    private class TimedEmbedder(
        private val delegate: Embedder,
    ) : Embedder {
        var totalMs = 0L
            private set
        var calls = 0
            private set

        override fun embed(text: String): FloatArray {
            val start = SystemClock.elapsedRealtime()
            return delegate.embed(text).also {
                totalMs += SystemClock.elapsedRealtime() - start
                calls++
            }
        }
    }

    private companion object {
        const val TAG = "SG"
        const val MODEL_PATH = "models/embeddinggemma-2-740m.litertlm"
        const val DEBUG_TEXT = "Hello world"
    }
}
