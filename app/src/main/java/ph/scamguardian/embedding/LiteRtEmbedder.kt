package ph.scamguardian.embedding

import android.os.Looper
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.EmbeddingEngine
import com.google.ai.edge.litertlm.EmbeddingEngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.LiteRtLmJniException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import ph.scamguardian.core.Embedder
import ph.scamguardian.core.truncatedAndNormalized
import java.io.File
import java.util.concurrent.Executors

/**
 * [Embedder] backed by EmbeddingGemma 2 running on-device through LiteRT-LM.
 *
 * The model is loaded once and kept in memory. Every model call runs on one dedicated thread; [load]
 * and [embed] block the caller until that thread is done, so they refuse to run on the main thread.
 */
class LiteRtEmbedder(
    private val modelPath: String,
) : Embedder,
    AutoCloseable {
    @Volatile
    private var modelThread: Thread? = null

    private val executor =
        Executors.newSingleThreadExecutor { task -> Thread(task, THREAD_NAME).also { modelThread = it } }

    /** The single thread every model call runs on. Work dispatched here may call [embed] directly. */
    val dispatcher: CoroutineDispatcher = executor.asCoroutineDispatcher()

    // Only read and written on the model thread.
    private var engine: EmbeddingEngine? = null

    /** Loads the model if it is not loaded yet. Fails with the expected path when the file is missing. */
    fun load() {
        onModelThread { loadedEngine() }
    }

    override fun embed(text: String): FloatArray = onModelThread { compute(text) }

    override fun close() {
        onModelThread {
            engine?.close()
            engine = null
        }
        executor.shutdown()
    }

    private fun <T> onModelThread(block: () -> T): T {
        check(Looper.myLooper() != Looper.getMainLooper()) { "LiteRtEmbedder must not be called on the main thread" }
        return if (Thread.currentThread() === modelThread) block() else runBlocking(dispatcher) { block() }
    }

    private fun compute(text: String): FloatArray {
        val input = InputData.Text(CLASSIFICATION_PREFIX + text.trim())
        return try {
            loadedEngine().computeEmbedding(listOf(input)).embedding.truncatedAndNormalized(DIMENSIONS)
        } catch (e: LiteRtLmJniException) {
            throw IllegalStateException("Embedding failed: ${e.message}", e)
        }
    }

    private fun loadedEngine(): EmbeddingEngine {
        engine?.let { return it }
        check(File(modelPath).isFile) {
            "EmbeddingGemma model file is missing. Expected it at $modelPath."
        }
        val created = EmbeddingEngine(EmbeddingEngineConfig(modelPath = modelPath, backend = Backend.GPU()))
        try {
            created.initialize()
        } catch (e: LiteRtLmJniException) {
            created.close()
            throw IllegalStateException("Could not load the model at $modelPath: ${e.message}", e)
        }
        engine = created
        return created
    }

    private companion object {
        const val THREAD_NAME = "litert-embedder"

        // From the LiteRT-LM Embedding Models page; applied to every text, anchors and messages alike.
        const val CLASSIFICATION_PREFIX = "task: classification | text: "
        const val DIMENSIONS = 256
    }
}
