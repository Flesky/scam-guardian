package ph.scamguardian

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ph.scamguardian.core.PipelineJson
import ph.scamguardian.core.ScamPipeline
import ph.scamguardian.core.WarningCatalog
import ph.scamguardian.core.parseWarnings
import ph.scamguardian.embedding.LiteRtEmbedder
import ph.scamguardian.storage.SafeTextStore
import java.io.File
import java.io.IOException

/** How much of the scam detection is working. */
enum class EngineState {
    /** Nothing was loaded yet. */
    IDLE,

    /** The data or the model is loading. */
    LOADING,

    /** The rules and the AI check both work. */
    READY,

    /** Only the rules work: the model could not be loaded. */
    LIMITED,

    /** Nothing works: the data files could not be read. */
    FAILED,
}

/**
 * The one app-wide owner of the embedder, the data files from assets and the scam pipeline.
 *
 * Nothing is loaded until [start] is first called. Loading happens off the main thread, in two steps.
 * First the data files, which is quick: from then on [loadedPipeline] checks messages with the rules.
 * Then the model and the anchors: from then on the AI check works too. If the second step fails, for
 * example because the model file is missing, the rules keep working and [start] tries again.
 */
class ScamEngine(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val assets = DataFiles(appContext)
    private val embedder = LiteRtEmbedder(File(appContext.getExternalFilesDir(null), MODEL_PATH).path)

    // In the no-backup folder: the messages the user marked "Not a scam" never leave this phone.
    private val safeTexts = SafeTextStore(File(appContext.noBackupFilesDir, SAFE_TEXTS_FILE))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutableState = MutableStateFlow(EngineState.IDLE)
    private var loading: Job? = null
    private var lastStart = 0L

    /** How much of the detection works right now. */
    val state: StateFlow<EngineState> = mutableState.asStateFlow()

    /** Why the state is [EngineState.LIMITED] or [EngineState.FAILED]; null otherwise. */
    @Volatile
    var failure: String? = null
        private set

    /** The pipeline once the data files are read, or null before that. Never waits. */
    @Volatile
    var loadedPipeline: ScamPipeline? = null
        private set

    /** The warning texts. Read from assets on first use, without waiting for the model. */
    val warnings: WarningCatalog by lazy { parseWarnings(assets.text(WARNINGS_FILE)) }

    /** The single thread every model call runs on. */
    val modelDispatcher: CoroutineDispatcher get() = embedder.dispatcher

    /**
     * Starts loading in the background. It does nothing while loading or once everything works; after
     * a failure it tries again, so it also serves as "retry".
     */
    @Synchronized
    fun start() {
        if (loading?.isActive == true || state.value == EngineState.READY) return
        lastStart = SystemClock.elapsedRealtime()
        mutableState.value = EngineState.LOADING
        loading = scope.launch { mutableState.value = load() }
    }

    /**
     * For the accessibility service, each time it reads a screen: loads the engine if nothing was loaded
     * yet, and while only the rules work it tries the model again about once a minute, in case its file
     * has arrived.
     */
    @Synchronized
    fun startIfNeeded() {
        val now = SystemClock.elapsedRealtime()
        val retryDue = state.value == EngineState.LIMITED && now - lastStart >= MODEL_RETRY_MS
        if (state.value == EngineState.IDLE || retryDue) start()
    }

    /**
     * "Not a scam": saves [text] in app storage, then makes it a safe anchor on the model thread. From
     * then on this text gives no warning, and similar messages score as safe in the AI check.
     */
    suspend fun markNotScam(text: String) {
        withContext(Dispatchers.IO) { safeTexts.add(text) }
        val pipeline = loadedPipeline ?: return
        withContext(embedder.dispatcher) { pipeline.markSafe(text) }
    }

    private fun load(): EngineState {
        failure = null
        val pipeline = loadedPipeline ?: rulesPipeline() ?: return EngineState.FAILED
        return try {
            val loadMs = timeMs { embedder.load() }
            val anchorsMs = timeMs { pipeline.loadAi() }
            if (BuildConfig.DEBUG) logTimings(loadMs, anchorsMs)
            EngineState.READY
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Could not load the model; only the rules are working", e)
            failure = e.message
            EngineState.LIMITED
        }
    }

    // The pipeline without the AI check. It needs only the data files, so the rules work at once.
    private fun rulesPipeline(): ScamPipeline? =
        try {
            ScamPipeline(assets.pipelineData(), embedder, loadAi = false).also { pipeline ->
                savedSafeTexts().forEach(pipeline::markSafe)
                loadedPipeline = pipeline
            }
        } catch (e: IOException) {
            failed(e)
        } catch (e: IllegalArgumentException) {
            failed(e)
        }

    private fun failed(e: Exception): ScamPipeline? {
        Log.e(TAG, "Could not read the data files", e)
        failure = e.message
        return null
    }

    // The messages the user marked "Not a scam" earlier. The engine still loads when they cannot be read.
    private fun savedSafeTexts(): List<String> =
        try {
            safeTexts.texts()
        } catch (e: IOException) {
            Log.e(TAG, "Could not read the saved safe messages", e)
            emptyList()
        }

    private fun logTimings(
        loadMs: Long,
        anchorsMs: Long,
    ) {
        var size = 0
        val embedMs = timeMs { size = embedder.embed(DEBUG_TEXT).size }
        Log.d(TAG, "vector size=$size, model load=$loadMs ms, one embedding=$embedMs ms, anchors=$anchorsMs ms")
    }

    private inline fun timeMs(block: () -> Unit): Long {
        val start = SystemClock.elapsedRealtime()
        block()
        return SystemClock.elapsedRealtime() - start
    }

    /** The data files packaged with the app. */
    private class DataFiles(
        private val context: Context,
    ) {
        fun text(name: String): String =
            context.assets
                .open(name)
                .bufferedReader()
                .use { it.readText() }

        fun pipelineData() =
            PipelineJson(
                brands = text("brands.json"),
                keywords = text("keywords.json"),
                shortcuts = text("shortcuts.json"),
                urlRules = text("url_rules.json"),
                warnings = text(WARNINGS_FILE),
                anchors = text("anchors.json"),
            ).parse()
    }

    private companion object {
        const val TAG = "SG"
        const val MODEL_PATH = "models/embeddinggemma-2-740m.litertlm"
        const val DEBUG_TEXT = "Hello world"
        const val WARNINGS_FILE = "warnings.json"
        const val SAFE_TEXTS_FILE = "safe_anchors.json"
        const val MODEL_RETRY_MS = 60_000L
    }
}
